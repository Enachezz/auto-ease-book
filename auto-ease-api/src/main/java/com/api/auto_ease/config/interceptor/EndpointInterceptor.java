package com.api.auto_ease.config.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class EndpointInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String requestUri = request.getRequestURI();
        if (requestUri != null && isSkipLogging(requestUri)) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        Long startTime = (Long) request.getAttribute("startTime");
        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;

        append(sb, "STATUS", String.valueOf(response.getStatus()));
        append(sb, "METHOD", request.getMethod());
        append(sb, "ENDPOINT", request.getRequestURI() + ", DURATION=" + duration + "ms");
        if (request.getQueryString() != null) {
            append(sb, "QUERY_STRING", request.getQueryString());
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null) {
            append(sb, "IP", forwarded);
        }

        log.info(sb.toString());
    }

    private static boolean isSkipLogging(String requestUri) {
        // Add paths to skip if they are polled frequently and would spam logs
        return false;
    }

    private void append(StringBuilder sb, String key, String value) {
        sb.append("[").append(key).append("->").append(value).append("] ");
    }
}
