package com.example.myapp_backend;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(CorrelationFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest req && response instanceof HttpServletResponse res) {
            String correlationId = req.getHeader("X-Correlation-Id");
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = UUID.randomUUID().toString();
            }

            MDC.put("correlationId", correlationId);
            res.setHeader("X-Correlation-Id", correlationId);

            log.info("{} {}", req.getMethod(), req.getRequestURI());

            try {
                chain.doFilter(request, response);
                log.info("{} {} {}", res.getStatus(), req.getMethod(), req.getRequestURI());
            } finally {
                MDC.remove("correlationId");
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
