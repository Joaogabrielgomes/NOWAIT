package com.nowait.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Order(1)
public class RateLimitGlobalFilter extends OncePerRequestFilter {

    private static final int LIMITE_GERAL    = 300;

    private static final int LIMITE_SENSIVEL =  5;

    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(50_000)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        String ip        = resolverIp(req);
        String uri       = req.getRequestURI();
        String categoria = resolverCategoria(uri);
        int    limite    = categoria.equals(CATEGORIA_GERAL) ? LIMITE_GERAL : LIMITE_SENSIVEL;

        Bucket bucket = cache.get(ip + ":" + categoria, k -> criarBucket(limite));

        if (bucket.tryConsume(1)) {
            chain.doFilter(req, res);
        } else {
            log.warn("Rate limit global atingido: ip={} uri={}", ip, uri);
            escreverRespostaLimitada(res);
        }
    }

    private static final String CATEGORIA_GERAL = "geral";

    private static final List<String> ROTAS_SENSIVEIS = List.of(
            "/auth/login", "/auth/register", "/auth/forgot-password", "/auth/reset-password"
    );

    private String resolverCategoria(String uri) {
        return ROTAS_SENSIVEIS.stream()
                .filter(uri::contains)
                .findFirst()
                .orElse(CATEGORIA_GERAL);
    }

    private Bucket criarBucket(int limite) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(limite,
                        Refill.greedy(limite, Duration.ofMinutes(1))))
                .build();
    }

    private String resolverIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private void escreverRespostaLimitada(HttpServletResponse res) throws IOException {
        res.setStatus(429);
        res.setHeader("Retry-After", "60");
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(
                "{\"status\":429,\"error\":\"Too Many Requests\"," +
                "\"message\":\"Limite de requisições atingido. Tente novamente em instantes.\"}");
    }
}
