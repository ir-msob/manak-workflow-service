package ir.msob.manak.workflow.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.CamundaClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import ir.msob.jima.core.commons.logger.Logger;
import ir.msob.jima.core.commons.logger.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.*;

/**
 * Reactive generic worker that executes REST requests described by job variables.
 * <p>
 * Job input variables:
 * - url (String)                     : required
 * - method (String)                  : required (GET, POST, PUT, DELETE, PATCH, ...)
 * - headers (Map<String,String>)     : optional
 * - params (Map<String,String>)      : optional (query params)
 * - payload (Object)                 : optional (Map, JsonNode, String, byte[] (base64 string))
 * - contentType (String)             : optional, default = application/json
 * - timeoutSeconds (Integer)         : optional, default = 10
 * - retry (Integer)                  : optional retry attempts, default = 0
 * <p>
 * Job output variables:
 * - response   : JsonNode | String | base64String (depending on returned content)
 * - status     : Integer
 * - success    : Boolean
 */
@Component
@RequiredArgsConstructor
public class RestRequestWorker {

    // job var names
    public static final String VAR_URL = "url";
    public static final String VAR_METHOD = "method";
    public static final String VAR_HEADERS = "headers";
    public static final String VAR_QUERY_PARAMS = "params"; // query params variable name
    public static final String VAR_PAYLOAD = "payload";
    public static final String VAR_CONTENT_TYPE = "Content-Type";
    public static final String VAR_TIMEOUT_SECONDS = "timeoutSeconds";
    public static final String VAR_RETRY = "retry";

    // output var names
    public static final String OUT_RESPONSE = "response";
    public static final String OUT_STATUS = "status";
    public static final String OUT_SUCCESS = "success";

    private static final String DEFAULT_CONTENT_TYPE = MediaType.APPLICATION_JSON_VALUE;
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    private static final int DEFAULT_RETRIES = 0;
    private static final int DEFAULT_FAIL_RETRIES_LEFT = 3;

    private static final Logger log = LoggerFactory.getLogger(RestRequestWorker.class);

    private final WebClient webClient;
    private final CamundaClient camundaClient;
    private final ObjectMapper objectMapper;

    // ---------------- helpers ----------------

    private static String safeString(Object o) {
        if (o == null) return null;
        return Objects.toString(o, null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> safeMapStringString(Object o) {
        if (o == null) return Collections.emptyMap();
        try {
            return (Map<String, String>) o;
        } catch (ClassCastException ex) {
            return Collections.emptyMap();
        }
    }

    private static int safeInt(Object o, int defaultValue) {
        if (o == null) return defaultValue;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Read retries from job safely (handles Integer or int or absence).
     */
    private int getRetriesOrDefault(ActivatedJob job, int defaultValue) {
        try {
            Object value = ActivatedJob.class.getMethod("getRetries").invoke(job);
            if (value == null) return defaultValue;
            if (value instanceof Number) return ((Number) value).intValue();
            return Integer.parseInt(value.toString());
        } catch (Exception ex) {
            log.debug("Couldn't read retries from job, using default {}. reason={}", defaultValue, ex.getMessage());
            return defaultValue;
        }
    }

    // ---------------- worker entry point ----------------

    @JobWorker(type = "rest-request", autoComplete = false)
    public void executeRestRequest(final ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();

        String url = safeString(vars.get(VAR_URL));
        String methodStr = safeString(vars.get(VAR_METHOD));
        Map<String, String> headers = safeMapStringString(vars.get(VAR_HEADERS));
        Map<String, String> queryParams = safeMapStringString(vars.get(VAR_QUERY_PARAMS));
        Object payload = vars.get(VAR_PAYLOAD);
        String contentType = safeString(headers.get(VAR_CONTENT_TYPE));
        int timeoutSeconds = safeInt(vars.get(VAR_TIMEOUT_SECONDS), DEFAULT_TIMEOUT_SECONDS);
        int retryAttempts = safeInt(vars.get(VAR_RETRY), DEFAULT_RETRIES);

        if (contentType == null) {
            headers = new HashMap<>(headers);
            contentType = DEFAULT_CONTENT_TYPE;
            headers.put(VAR_CONTENT_TYPE, contentType);
        }

        if (url == null || methodStr == null) {
            String msg = "Missing required job variables: '" + VAR_URL + "' and/or '" + VAR_METHOD + "'";
            log.error("[jobKey={}] {}", job.getKey(), msg);
            failJobReactive(job, msg).subscribe(); // report failure and return
            return;
        }

        HttpMethod httpMethod;
        try {
            httpMethod = HttpMethod.valueOf(methodStr.trim().toUpperCase());
        } catch (Exception ex) {
            String msg = "Invalid HTTP method: " + methodStr;
            log.error("[jobKey={}] {}", job.getKey(), msg);
            failJobReactive(job, msg).subscribe();
            return;
        }

        log.info("[jobKey={}] Preparing REST request: {} {}", job.getKey(), httpMethod, url);

        // Build URI with query params (if any)
        URI targetUri;
        try {
            targetUri = buildUriWithQueryParams(url, queryParams);
        } catch (Exception ex) {
            String msg = "Invalid URL or query params: " + ex.getMessage();
            log.error("[jobKey={}] {}", job.getKey(), msg);
            failJobReactive(job, msg).subscribe();
            return;
        }

        WebClient.RequestBodySpec requestSpec = webClient
                .method(httpMethod)
                .uri(targetUri)
                .accept(MediaType.ALL);

        // set headers
        if (!headers.isEmpty()) {
            headers.forEach(requestSpec::header);
        }


        // build response mono (ClientResponse) — handle body when appropriate
        Mono<ClientResponse> responseMono;
        if (payload != null && supportsRequestBody(httpMethod)) {
            Object requestBody = prepareRequestBodyForContentType(payload, contentType);
            requestSpec.contentType(MediaType.parseMediaType(contentType));
            responseMono = requestSpec.body(BodyInserters.fromValue(requestBody)).exchangeToMono(Mono::just);
        } else {
            responseMono = requestSpec.exchangeToMono(Mono::just);
        }

        // map response -> result map (handles Mono and Flux responses internally)
        Mono<Map<String, Object>> resultMono = responseMono
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .flatMap(this::mapResponseToResult)
                .doOnNext(r -> log.debug("[jobKey={}] Response mapped to result", job.getKey()))
                .doOnError(e -> log.error("[jobKey={}] REST call error: {}", job.getKey(), e.getMessage(), e));

        // retry if requested (exponential backoff)
        if (retryAttempts > 0) {
            resultMono = resultMono.retryWhen(
                    Retry.backoff(retryAttempts, Duration.ofMillis(200))
                            .maxBackoff(Duration.ofSeconds(5))
            );
        }

        // chain completion: after result produced, send complete to Camunda (non-blocking)
        resultMono
                .flatMap(result -> completeJobReactive(job, result))
                .onErrorResume(err -> {
                    String msg = "REST request failed: " + err.getMessage();
                    log.error("[jobKey={}] {}", job.getKey(), msg, err);
                    return failJobReactive(job, msg);
                })
                .subscribe(); // subscribe to start the chain (non-blocking)
    }

    // ---------------- reactive Camunda interactions ----------------

    // reactive completion using CamundaFuture.whenComplete -> Mono<Void>
    private Mono<Void> completeJobReactive(ActivatedJob job, Map<String, Object> resultVars) {
        log.info("[jobKey={}] Completing job. status={}, success={}",
                job.getKey(), resultVars.get(OUT_STATUS), resultVars.get(OUT_SUCCESS));

        // send() returns CamundaFuture<CompleteJobResponse> (not a CompletableFuture<Void>)
        var camundaFuture = camundaClient
                .newCompleteCommand(job.getKey())
                .variables(resultVars)
                .send();

        return Mono.create(sink -> camundaFuture.whenComplete((resp, ex) -> {
            if (ex != null) {
                log.error("[jobKey={}] Failed to complete job in Camunda: {}", job.getKey(), ex.getMessage(), ex);
                sink.error(ex);
            } else {
                log.info("[jobKey={}] Job completed successfully in Camunda", job.getKey());
                sink.success();
            }
        }));
    }

    // reactive fail using CamundaFuture.whenComplete -> Mono<Void>
    private Mono<Void> failJobReactive(ActivatedJob job, String errorMessage) {
        int currentRetries = getRetriesOrDefault(job, DEFAULT_FAIL_RETRIES_LEFT);
        int retriesLeft = Math.max(currentRetries - 1, 0);

        log.warn("[jobKey={}] Failing job. retriesLeft={}, message={}", job.getKey(), retriesLeft, errorMessage);

        var camundaFuture = camundaClient
                .newFailCommand(job)
                .retries(retriesLeft)
                .errorMessage(errorMessage)
                .send();

        return Mono.create(sink -> camundaFuture.whenComplete((resp, ex) -> {
            if (ex != null) {
                log.error("[jobKey={}] Error reporting failure to Camunda: {}", job.getKey(), ex.getMessage(), ex);
                sink.error(ex);
            } else {
                log.info("[jobKey={}] Reported failure to Camunda", job.getKey());
                sink.success();
            }
        }));
    }


    // ---------------- response handling ----------------

    /**
     * Build URI with query params (keeps original url if no params)
     */
    private URI buildUriWithQueryParams(String url, Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return URI.create(url);
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        queryParams.forEach(builder::queryParam);
        return builder.build(true).toUri();
    }

    /**
     * Determine whether this HTTP method supports a request body
     */
    private boolean supportsRequestBody(HttpMethod method) {
        return (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH || method == HttpMethod.DELETE);
    }

    /**
     * Prepare request body according to declared content type.
     */
    private Object prepareRequestBodyForContentType(Object payload, String contentType) {
        try {
            String lower = contentType == null ? "" : contentType.toLowerCase();
            if (lower.contains("json")) {
                if (payload instanceof String) {
                    String s = ((String) payload).trim();
                    if (s.startsWith("{") || s.startsWith("[")) {
                        return objectMapper.readTree(s);
                    } else {
                        return s;
                    }
                }
                return payload;
            } else if (lower.contains("octet-stream") || lower.contains("application/octet-stream")) {
                if (payload instanceof String) {
                    try {
                        return Base64.getDecoder().decode((String) payload);
                    } catch (IllegalArgumentException ex) {
                        return ((String) payload).getBytes();
                    }
                } else if (payload instanceof byte[]) {
                    return payload;
                } else {
                    return payload.toString().getBytes();
                }
            } else {
                return payload;
            }
        } catch (Exception e) {
            log.debug("Could not convert payload to requested content type {}, sending raw payload. reason={}", contentType, e.getMessage());
            return payload;
        }
    }

    /**
     * Map ClientResponse to a result map containing:
     * - status (Integer)
     * - response (JsonNode | String | base64String)
     * - success (Boolean)
     * <p>
     * This method handles both single-value bodies (Mono) and streaming bodies (Flux).
     */
    private Mono<Map<String, Object>> mapResponseToResult(ClientResponse resp) {
        int status = resp.statusCode().value();

        List<String> contentTypeHeaders = resp.headers().header("Content-Type");
        String respContentType = contentTypeHeaders.isEmpty() ? "" : contentTypeHeaders.getFirst().toLowerCase();

        // Heuristics: if content-type indicates streaming or chunked transfer, treat as Flux
        boolean isStream =
                respContentType.contains("stream") ||
                        respContentType.contains("event-stream") ||
                        respContentType.contains("ndjson") ||
                        // if Transfer-Encoding header indicates chunked, consider it streaming
                        resp.headers().header("Transfer-Encoding").stream().anyMatch(h -> h.toLowerCase().contains("chunked"));

        // JSON (object or array) — if not streaming, read as single JsonNode
        if (!isStream && (respContentType.contains("json") || respContentType.contains("application/json"))) {
            return resp.bodyToMono(JsonNode.class)
                    .onErrorResume(e -> Mono.just(objectMapper.nullNode()))
                    .map(bodyNode -> Map.of(
                            OUT_STATUS, status,
                            OUT_RESPONSE, bodyNode,
                            OUT_SUCCESS, status >= 200 && status < 300
                    ));
        }

        // JSON stream or streaming-JSON -> collect all JsonNodes into an ArrayNode
        if (isStream && (respContentType.contains("json") || respContentType.contains("ndjson") || respContentType.contains("stream"))) {
            Flux<JsonNode> flux = resp.bodyToFlux(JsonNode.class)
                    .onErrorContinue((err, itm) -> log.debug("Error parsing stream item: {}", err.getMessage()));
            return flux.collectList()
                    .map(list -> {
                        // convert list to a Json array node (ObjectMapper will convert)
                        JsonNode arrayNode = objectMapper.valueToTree(list);
                        return Map.of(
                                OUT_STATUS, status,
                                OUT_RESPONSE, arrayNode,
                                OUT_SUCCESS, status >= 200 && status < 300
                        );
                    });
        }

        // Text-like response (including event-stream text lines)
        if (respContentType.startsWith("text/") || respContentType.contains("xml") || respContentType.contains("html")) {
            if (isStream) {
                // streaming text -> collect lines
                return resp.bodyToFlux(String.class)
                        .collectList()
                        .map(list -> Map.of(
                                OUT_STATUS, status,
                                OUT_RESPONSE, list, // list of strings
                                OUT_SUCCESS, status >= 200 && status < 300
                        ));
            } else {
                return resp.bodyToMono(String.class)
                        .onErrorResume(e -> Mono.just(""))
                        .map(bodyStr -> Map.of(
                                OUT_STATUS, status,
                                OUT_RESPONSE, bodyStr,
                                OUT_SUCCESS, status >= 200 && status < 300
                        ));
            }
        }

        // Binary or unknown content-type
        if (isStream) {
            // collect chunks of byte[] and combine
            return resp.bodyToFlux(byte[].class)
                    .collectList()
                    .map(list -> {
                        int total = list.stream().mapToInt(b -> b.length).sum();
                        byte[] all = new byte[total];
                        int pos = 0;
                        for (byte[] chunk : list) {
                            System.arraycopy(chunk, 0, all, pos, chunk.length);
                            pos += chunk.length;
                        }
                        String base64 = Base64.getEncoder().encodeToString(all);
                        return Map.<String, Object>of(
                                OUT_STATUS, status,
                                OUT_RESPONSE, base64,
                                OUT_SUCCESS, status >= 200 && status < 300
                        );
                    })
                    .onErrorResume(e -> Mono.just(Map.of(
                            OUT_STATUS, status,
                            OUT_RESPONSE, "",
                            OUT_SUCCESS, status >= 200 && status < 300
                    )));
        } else {
            return resp.bodyToMono(byte[].class)
                    .onErrorResume(e -> Mono.just(new byte[0]))
                    .map(bytes -> {
                        String base64 = Base64.getEncoder().encodeToString(bytes);
                        return Map.of(
                                OUT_STATUS, status,
                                OUT_RESPONSE, base64,
                                OUT_SUCCESS, status >= 200 && status < 300
                        );
                    });
        }
    }
}
