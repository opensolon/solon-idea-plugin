package org.noear.solon.idea.plugin.api;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * 接口快速调用的 HTTP 发送服务。全部使用异步调用，避免阻塞 EDT。
 */
public final class HttpRequestService {

    /**
     * HttpClient 禁止手动设置的受限请求头，提前过滤避免抛异常。
     */
    private static final Set<String> RESTRICTED_HEADERS = Set.of(
            "host", "content-length", "connection", "upgrade",
            "transfer-encoding", "expect", "keep-alive", "proxy-connection");

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public record Request(@NotNull String method,
                          @NotNull String url,
                          @NotNull Map<String, String> headers,
                          @Nullable String body) {
    }

    public record Response(int statusCode,
                           String reasonPhrase,
                           long elapsedMillis,
                           String body,
                           Map<String, List<String>> headers,
                           @Nullable String error) {
    }

    private HttpRequestService() {
    }

    public static @NotNull CompletableFuture<Response> send(@NotNull Request request) {
        HttpRequest httpRequest;
        try {
            httpRequest = buildRequest(request);
        } catch (RuntimeException e) {
            return CompletableFuture.completedFuture(
                    new Response(-1, "", 0, "", Map.of(), message(e)));
        }

        long start = System.currentTimeMillis();
        return CLIENT.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> new Response(response.statusCode(),
                        reasonPhrase(response.statusCode()),
                        System.currentTimeMillis() - start,
                        response.body(),
                        response.headers().map(),
                        null))
                .exceptionally(throwable -> new Response(-1, "", System.currentTimeMillis() - start,
                        "", Map.of(), message(throwable)));
    }

    /**
     * HttpResponse 不提供 reason phrase，这里补齐常见状态码的描述。
     */
    private static @NotNull String reasonPhrase(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 304 -> "Not Modified";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 409 -> "Conflict";
            case 415 -> "Unsupported Media Type";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            default -> "";
        };
    }

    private static @NotNull HttpRequest buildRequest(@NotNull Request request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(request.url()))
                .timeout(Duration.ofSeconds(30));

        request.headers().forEach((name, value) -> {
            if (name == null || value == null || name.isBlank()) {
                return;
            }
            if (RESTRICTED_HEADERS.contains(name.trim().toLowerCase(Locale.ROOT))) {
                return;
            }
            builder.header(name.trim(), value);
        });

        String method = request.method() == null || request.method().isBlank()
                ? "GET" : request.method().trim().toUpperCase(Locale.ROOT);
        String body = request.body();
        boolean hasBody = body != null && !body.isBlank() && !"GET".equals(method) && !"HEAD".equals(method);
        builder.method(method, hasBody
                ? HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)
                : HttpRequest.BodyPublishers.noBody());
        return builder.build();
    }

    /**
     * JSON 响应体格式化，非 JSON 内容原样返回。
     */
    public static @NotNull String prettyPrint(@Nullable String body) {
        if (body == null || body.isBlank()) {
            return body == null ? "" : body;
        }
        String trimmed = body.trim();
        char first = trimmed.charAt(0);
        if (first != '{' && first != '[') {
            return body;
        }
        try {
            Object parsed = JSON.parse(trimmed);
            return JSON.toJSONString(parsed, JSONWriter.Feature.PrettyFormat);
        } catch (RuntimeException ignored) {
            return body;
        }
    }

    private static @NotNull String message(@NotNull Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }
}
