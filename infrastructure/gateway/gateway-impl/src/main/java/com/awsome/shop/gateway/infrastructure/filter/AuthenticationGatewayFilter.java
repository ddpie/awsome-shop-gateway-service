package com.awsome.shop.gateway.infrastructure.filter;

import com.awsome.shop.gateway.common.constants.RouteConstants;
import com.awsome.shop.gateway.common.enums.GatewayErrorCode;
import com.awsome.shop.gateway.common.exception.AuthenticationException;
import com.awsome.shop.gateway.common.exception.ForbiddenException;
import com.awsome.shop.gateway.domain.auth.model.AuthenticationResult;
import com.awsome.shop.gateway.domain.auth.model.TokenInfo;
import com.awsome.shop.gateway.domain.auth.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Global filter for JWT token authentication and role-based access control.
 *
 * <p>Order: +100 - executes after AccessLogFilter.</p>
 *
 * <p>Implements the following design doc rules:</p>
 * <ul>
 *   <li>BR-GW-012: Strip X-Operator-Id and X-User-Role from all requests (prevent forgery)</li>
 *   <li>BR-GW-001~004: Token extraction and remote validation via auth-service</li>
 *   <li>BR-GW-005: Public endpoint whitelist (skip auth)</li>
 *   <li>BR-GW-006: Admin endpoint role check (ADMIN_ONLY → 403)</li>
 *   <li>BR-GW-013: Inject X-Operator-Id and X-User-Role for authenticated requests</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationGatewayFilter implements GlobalFilter, Ordered {

    private static final String ROLE_ADMIN = "ADMIN";

    private final AuthenticationService authenticationService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // BR-GW-012: Always strip security headers to prevent forgery
        ServerHttpRequest strippedRequest = stripSecurityHeaders(exchange.getRequest());
        ServerWebExchange strippedExchange = exchange.mutate().request(strippedRequest).build();

        // Skip auth for public and docs paths
        if (isPublicPath(path)) {
            return chain.filter(strippedExchange);
        }

        // Check route metadata for auth-required flag
        if (!isAuthRequired(exchange)) {
            return chain.filter(strippedExchange);
        }

        // Extract token (BR-GW-001)
        String authHeader = exchange.getRequest().getHeaders().getFirst(RouteConstants.HEADER_AUTHORIZATION);
        TokenInfo tokenInfo = TokenInfo.fromAuthorizationHeader(authHeader);
        if (tokenInfo == null) {
            return Mono.error(new AuthenticationException(GatewayErrorCode.AUTHZ_001));
        }

        String requestId = exchange.getAttribute(RouteConstants.ATTR_REQUEST_ID);

        // Validate token via auth service (BR-GW-002)
        return authenticationService.validate(tokenInfo.getToken())
                .flatMap(result -> {
                    if (!result.isAuthenticated()) {
                        log.warn("[{}] Authentication failed: {}", requestId, result.getMessage());
                        return Mono.error(new AuthenticationException(GatewayErrorCode.AUTHZ_001));
                    }

                    // BR-GW-006: Admin endpoint role check
                    if (isAdminOnly(path) && !ROLE_ADMIN.equals(result.getRole())) {
                        log.warn("[{}] Forbidden: operatorId={} role={} attempted admin path {}",
                                requestId, result.getOperatorId(), result.getRole(), path);
                        return Mono.error(new ForbiddenException(GatewayErrorCode.FORBIDDEN_001));
                    }

                    String operatorIdStr = String.valueOf(result.getOperatorId());
                    String role = result.getRole();

                    log.debug("[{}] Authenticated operatorId={} role={}", requestId, operatorIdStr, role);

                    // Store for downstream filters
                    strippedExchange.getAttributes().put(RouteConstants.ATTR_OPERATOR_ID, operatorIdStr);
                    strippedExchange.getAttributes().put(RouteConstants.ATTR_USER_ROLE, role);

                    // BR-GW-013: Inject user info headers
                    ServerHttpRequest mutatedRequest = strippedExchange.getRequest().mutate()
                            .header(RouteConstants.HEADER_OPERATOR_ID, operatorIdStr)
                            .header(RouteConstants.HEADER_USER_ROLE, role)
                            .build();

                    return chain.filter(strippedExchange.mutate().request(mutatedRequest).build());
                });
    }

    @Override
    public int getOrder() {
        return 100;
    }

    /**
     * Strip X-Operator-Id and X-User-Role headers from client request (BR-GW-012)
     */
    private ServerHttpRequest stripSecurityHeaders(ServerHttpRequest request) {
        return request.mutate()
                .headers(headers -> {
                    headers.remove(RouteConstants.HEADER_OPERATOR_ID);
                    headers.remove(RouteConstants.HEADER_USER_ROLE);
                })
                .build();
    }

    private boolean isPublicPath(String path) {
        return path.startsWith(RouteConstants.PATH_PREFIX_PUBLIC)
                || path.startsWith(RouteConstants.PATH_PREFIX_DOCS)
                || path.startsWith("/swagger-ui")
                || path.startsWith("/actuator");
    }

    /**
     * Check if path requires ADMIN role (BR-GW-006)
     */
    private boolean isAdminOnly(String path) {
        return path.startsWith(RouteConstants.PATH_PREFIX_ADMIN)
                || path.equals(RouteConstants.PATH_FILES_UPLOAD);
    }

    private boolean isAuthRequired(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route == null) {
            return true;
        }
        Map<String, Object> metadata = route.getMetadata();
        Object authRequired = metadata.get(RouteConstants.METADATA_AUTH_REQUIRED);
        if (authRequired instanceof Boolean) {
            return (Boolean) authRequired;
        }
        if (authRequired instanceof String) {
            return Boolean.parseBoolean((String) authRequired);
        }
        return true;
    }
}
