package com.blog.blogsystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory rate limiter cho các endpoint nhạy cảm.
 * Sử dụng Token Bucket algorithm đơn giản với ConcurrentHashMap.
 *
 * Giới hạn:
 * - /api/auth/refresh : 10 request / phút / IP
 * - /api/auth/login   :  5 request / phút / IP
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int REFRESH_LIMIT = 10;
    private static final int LOGIN_LIMIT = 5;
    private static final long WINDOW_MS = 60_000; // 1 phút

    private final ConcurrentHashMap<String, RateBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        int limit = getLimit(path);

        if (limit > 0) {
            String clientIp = getClientIp(request);
            String key = path + ":" + clientIp;

            RateBucket bucket = buckets.computeIfAbsent(key, k -> new RateBucket(limit));

            if (!bucket.tryConsume()) {
                log.warn("Rate limit exceeded: IP={} path={}", clientIp, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"code\":429,\"message\":\"Quá nhiều yêu cầu. Vui lòng thử lại sau.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Trả về giới hạn request cho path, hoặc 0 nếu không giới hạn.
     */
    private int getLimit(String path) {
        if (path.equals("/api/auth/refresh")) {
            return REFRESH_LIMIT;
        } else if (path.equals("/api/auth/login")) {
            return LOGIN_LIMIT;
        }
        return 0;
    }

    /**
     * Lấy IP thực của client, hỗ trợ proxy (X-Forwarded-For).
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Dọn dẹp bucket hết hạn mỗi 5 phút để tránh memory leak.
     */
    @Scheduled(fixedRate = 300_000) // 5 phút
    public void cleanupExpiredBuckets() {
        long now = System.currentTimeMillis();
        int removed = 0;
        var iterator = buckets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, RateBucket> entry = iterator.next();
            if (now - entry.getValue().windowStart.get() > WINDOW_MS * 2) {
                iterator.remove();
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Đã dọn dẹp {} rate limit bucket hết hạn", removed);
        }
    }

    /**
     * Token bucket đơn giản với sliding window.
     * Thread-safe nhờ sử dụng AtomicInteger và AtomicLong.
     */
    static class RateBucket {
        final int limit;
        final AtomicInteger count = new AtomicInteger(0);
        final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());

        RateBucket(int limit) {
            this.limit = limit;
        }

        boolean tryConsume() {
            long now = System.currentTimeMillis();
            // Reset window nếu đã qua 1 phút
            if (now - windowStart.get() > WINDOW_MS) {
                windowStart.set(now);
                count.set(0);
            }
            return count.incrementAndGet() <= limit;
        }
    }
}
