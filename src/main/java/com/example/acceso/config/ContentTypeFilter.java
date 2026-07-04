package com.example.acceso.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

@Component
public class ContentTypeFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String contentType = request.getContentType();
        
        // Si el Content-Type es application/json con charset, lo normalizamos
        if (contentType != null && contentType.startsWith("application/json;")) {
            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getContentType() {
                    return "application/json";
                }
                
                @Override
                public String getHeader(String name) {
                    if ("content-type".equalsIgnoreCase(name)) {
                        return "application/json";
                    }
                    return super.getHeader(name);
                }
                
                @Override
                public Enumeration<String> getHeaders(String name) {
                    if ("content-type".equalsIgnoreCase(name)) {
                        return Collections.enumeration(Collections.singletonList("application/json"));
                    }
                    return super.getHeaders(name);
                }
            };
            filterChain.doFilter(wrappedRequest, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}