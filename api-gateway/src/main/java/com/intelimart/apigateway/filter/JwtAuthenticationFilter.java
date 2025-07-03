package com.intelimart.apigateway.filter; // Or your preferred package

import com.intelimart.apigateway.util.JwtUtil; // Adjust import based on where you put JwtUtil
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Autowired
    private JwtUtil jwtUtil;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    // Define routes that should NOT be authenticated (public endpoints)
    // This is a simple list, consider more robust solutions for larger apps
    public static final String[] EXCLUDED_URLS = {
            "/api/auth/register",
            "/api/auth/login",
            "/eureka/**" // Allow Eureka dashboard and registration
            // Add other public routes as needed
    };

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 1. Check if the request path is in the excluded URLs
            String path = request.getURI().getPath();
            for (String excludedUrl : EXCLUDED_URLS) {
                if (path.startsWith(excludedUrl.replace("/**", ""))) { // Simple startsWith check
                    return chain.filter(exchange); // Continue without authentication
                }
            }

            // 2. Extract JWT from Authorization header
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Missing or invalid Bearer token", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7); // Remove "Bearer " prefix

            // 3. Validate JWT
            try {
                // This method now only validates and extracts claims
                jwtUtil.extractUsername(token); // Just to validate the token's signature and expiration
                // If validation successful, extract user info and add to headers
                String username = jwtUtil.extractUsername(token);
                // You can add more claims as needed, e.g., roles from claims.get("roles")
                // If you store user ID in JWT, extract it here as well

                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-Auth-User", username) // Add username to header for downstream services
                        // .header("X-Auth-Roles", roles) // Add roles if extracted
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (Exception e) {
                // Log the exception for debugging
                System.err.println("JWT Validation Error: " + e.getMessage());
                return onError(exchange, "Invalid or expired token: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        // Optionally set a response body for more detailed error messages
        // return response.writeWith(Mono.just(response.bufferFactory().wrap(err.getBytes())));
        return response.setComplete(); // Just complete the response with status
    }

    public static class Config {
        // Put the configuration properties for your filter here
    }
}