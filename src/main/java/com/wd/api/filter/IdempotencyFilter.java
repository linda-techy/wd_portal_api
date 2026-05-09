package com.wd.api.filter;

import com.wd.api.model.IdempotencyResponse;
import com.wd.api.repository.IdempotencyResponseRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * S5 PR1: caches 2xx responses keyed by the {@code Idempotency-Key} header for
 * 24 hours, scoped to the three S5-queued mutation paths. Absent header is a
 * no-op (pre-S5 clients keep working).
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

    // /api/site-reports          (POST + nested)
    // /api/projects/{id}/delays  (POST)
    // /api/tasks/{id}/mark-complete
    private static final Pattern SCOPED_PATHS = Pattern.compile(
            "^/api/site-reports(/.*)?$"
                    + "|^/api/projects/[^/]+/delays(/.*)?$"
                    + "|^/api/tasks/[^/]+/mark-complete$");

    private final IdempotencyResponseRepository repo;

    public IdempotencyFilter(IdempotencyResponseRepository repo) {
        this.repo = repo;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        String key = request.getHeader(HEADER);
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (key == null || key.isBlank() || !SCOPED_PATHS.matcher(path).matches()) {
            chain.doFilter(request, response);
            return;
        }

        Optional<IdempotencyResponse> hit =
                repo.findByIdempotencyKeyAndRequestMethodAndRequestPath(key, method, path);
        if (hit.isPresent() && hit.get().getExpiresAt().isAfter(LocalDateTime.now())) {
            IdempotencyResponse cached = hit.get();
            response.setStatus(cached.getResponseStatus());
            response.setContentType(cached.getResponseContentType());
            byte[] body = cached.getResponseBody().getBytes(StandardCharsets.UTF_8);
            response.setContentLength(body.length);
            response.getOutputStream().write(body);
            response.getOutputStream().flush();
            return;
        }

        // Cache miss (or expired): wrap response, capture body, persist on 2xx.
        CachingResponseWrapper wrapper = new CachingResponseWrapper(response);
        chain.doFilter(request, wrapper);
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
                    .cachedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusHours(CACHE_TTL_HOURS))
                    .build();
            repo.save(row);
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
