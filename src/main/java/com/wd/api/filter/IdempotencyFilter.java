package com.wd.api.filter;

import com.wd.api.model.IdempotencyResponse;
import com.wd.api.repository.IdempotencyResponseRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * S5 PR1: caches 2xx responses keyed by the {@code Idempotency-Key} header for
 * 24 hours, scoped to a configurable set of mutation paths. Absent header is a
 * no-op (pre-S5 clients keep working).
 *
 * <p>G-57 hardening: stores SHA-256 of the request body alongside the cached
 * response and returns 409 Conflict when the same key is replayed with a
 * different payload. Previously, key-only matching would silently replay the
 * original response — risky for financial mutations.
 *
 * <p>G-58 hardening: scope path-pattern is loaded from
 * {@code wd.idempotency.scoped-paths} (regex) so new mutation endpoints can be
 * brought under the filter without code edits. Default preserves the original
 * S5 PR1 scope.
 *
 * <p>Mounted late enough to run after auth (Spring Security default order is
 * {@code SecurityProperties.DEFAULT_FILTER_ORDER == -100}); we sit just before
 * the dispatcher servlet at {@code LOWEST_PRECEDENCE - 10}.
 */
@Component("offlineIdempotencyFilter")
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String HEADER = "Idempotency-Key";
    private static final long CACHE_TTL_HOURS = 24;

    static final String DEFAULT_SCOPED_PATHS =
            "^/api/site-reports(/.*)?$"
                    + "|^/api/projects/[^/]+/delays(/.*)?$"
                    + "|^/api/tasks/[^/]+/mark-complete$";

    private final IdempotencyResponseRepository repo;
    private final Pattern scopedPaths;

    public IdempotencyFilter(IdempotencyResponseRepository repo,
                             @Value("${wd.idempotency.scoped-paths:" + DEFAULT_SCOPED_PATHS + "}")
                             String scopedPathsRegex) {
        this.repo = repo;
        this.scopedPaths = Pattern.compile(scopedPathsRegex);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        String key = request.getHeader(HEADER);
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (key == null || key.isBlank() || !scopedPaths.matcher(path).matches()) {
            chain.doFilter(request, response);
            return;
        }

        // Buffer the body once so we can hash it AND let the controller read
        // it normally via getInputStream(). A naive readAllBytes() on the
        // original request would consume the stream and break the controller.
        BufferedRequestWrapper wrappedRequest = new BufferedRequestWrapper(request);
        String bodyHash = sha256Hex(wrappedRequest.getBufferedBody());

        Optional<IdempotencyResponse> hit =
                repo.findByIdempotencyKeyAndRequestMethodAndRequestPath(key, method, path);
        if (hit.isPresent() && hit.get().getExpiresAt().isAfter(LocalDateTime.now())) {
            IdempotencyResponse cached = hit.get();

            // G-57: replays with the same key but a different body must NOT
            // silently return the original 2xx. Treat NULL legacy hashes as
            // "skip check" — rows expire within 24h so the gap closes itself.
            String cachedHash = cached.getRequestBodyHash();
            if (cachedHash != null && !cachedHash.equals(bodyHash)) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"success\":false,\"code\":\"IDEMPOTENCY_KEY_REPLAYED_WITH_DIFFERENT_BODY\","
                                + "\"message\":\"Idempotency-Key matches a prior request with a "
                                + "different payload — refusing to replay.\"}");
                return;
            }

            response.setStatus(cached.getResponseStatus());
            response.setContentType(cached.getResponseContentType());
            byte[] body = cached.getResponseBody().getBytes(StandardCharsets.UTF_8);
            response.setContentLength(body.length);
            response.addHeader("Idempotency-Replayed", "true");
            response.getOutputStream().write(body);
            response.getOutputStream().flush();
            return;
        }

        // Cache miss (or expired): wrap response, capture body, persist on 2xx.
        CachingResponseWrapper wrapper = new CachingResponseWrapper(response);
        chain.doFilter(wrappedRequest, wrapper);
        wrapper.flushBuffer();

        int status = wrapper.getStatus();
        if (status >= 200 && status < 300) {
            String body = wrapper.getCapturedBody();
            String contentType = response.getContentType() != null
                    ? response.getContentType() : "application/json";

            IdempotencyResponse row = IdempotencyResponse.builder()
                    .idempotencyKey(key)
                    .requestMethod(method)
                    .requestPath(path)
                    .responseStatus(status)
                    .responseBody(body)
                    .responseContentType(contentType)
                    .requestBodyHash(bodyHash)
                    .cachedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusHours(CACHE_TTL_HOURS))
                    .build();
            repo.save(row);
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // ---- request body buffering ----

    /** Reads the body once into memory and re-serves it on every
     *  getInputStream / getReader call so downstream handlers see an
     *  untouched stream. */
    private static final class BufferedRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] body;

        BufferedRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            try (var in = request.getInputStream();
                 var out = new ByteArrayOutputStream()) {
                in.transferTo(out);
                this.body = out.toByteArray();
            }
        }

        byte[] getBufferedBody() { return body; }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return bais.read(); }
                @Override public boolean isFinished() { return bais.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener l) { }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }

    // ---- response capture helpers ----

    private static final class CachingResponseWrapper extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final ServletOutputStream outputStream = new BufferedServletOutputStream();
        private final PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true);

        CachingResponseWrapper(HttpServletResponse delegate) { super(delegate); }

        @Override public ServletOutputStream getOutputStream() { return outputStream; }
        @Override public PrintWriter getWriter() { return writer; }

        String getCapturedBody() { writer.flush(); return buffer.toString(StandardCharsets.UTF_8); }

        @Override public void flushBuffer() throws IOException {
            writer.flush();
            byte[] bytes = buffer.toByteArray();
            getResponse().getOutputStream().write(bytes);
            getResponse().getOutputStream().flush();
        }

        private final class BufferedServletOutputStream extends ServletOutputStream {
            @Override public void write(int b) { buffer.write(b); }
            @Override public boolean isReady() { return true; }
            @Override public void setWriteListener(WriteListener l) { }
        }
    }
}
