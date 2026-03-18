package com.awsome.shop.gateway.infrastructure.auth.client;

import com.awsome.shop.gateway.application.auth.dto.AuthValidateRequest;
import com.awsome.shop.gateway.application.auth.dto.AuthValidateResponse;
import com.awsome.shop.gateway.common.enums.GatewayErrorCode;
import com.awsome.shop.gateway.common.exception.GatewayException;
import com.awsome.shop.gateway.common.exception.ServiceUnavailableException;
import com.awsome.shop.gateway.domain.auth.model.AuthenticationResult;
import com.awsome.shop.gateway.domain.auth.service.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.springframework.web.reactive.function.client.WebClientRequestException;

/**
 * Auth service client that validates tokens via external auth service using WebClient.
 *
 * <p>Error handling per design doc:</p>
 * <ul>
 *   <li>Auth service unreachable → BAD_GATEWAY_001 (502)</li>
 *   <li>Auth service timeout → GATEWAY_TIMEOUT_001 (504)</li>
 * </ul>
 */
@Slf4j
@Component
public class AuthServiceClient implements AuthenticationService {

    private final WebClient webClient;
    private final String authValidateUrl;
    private final Duration timeout;

    public AuthServiceClient(
            WebClient.Builder webClientBuilder,
            @Value("${gateway.auth.validate-url}") String authValidateUrl,
            @Value("${gateway.auth.timeout:5s}") Duration timeout) {
        this.webClient = webClientBuilder.build();
        this.authValidateUrl = authValidateUrl;
        this.timeout = timeout;
    }

    @Override
    public Mono<AuthenticationResult> validate(String token) {
        AuthValidateRequest request = AuthValidateRequest.builder()
                .token(token)
                .build();

        return webClient.post()
                .uri(authValidateUrl)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AuthValidateResponse.class)
                .timeout(timeout)
                .map(response -> {
                    if (response.isSuccess()) {
                        return AuthenticationResult.success(
                                response.getOperatorId(), response.getRole());
                    }
                    return AuthenticationResult.failure(response.getMessage());
                })
                .onErrorResume(ex -> {
                    if (ex instanceof GatewayException) {
                        return Mono.error(ex);
                    }
                    if (ex instanceof TimeoutException) {
                        log.error("Auth service call timed out: {}", ex.getMessage());
                        return Mono.error(new GatewayException(
                                GatewayErrorCode.GATEWAY_TIMEOUT_001));
                    }
                    if (ex instanceof WebClientRequestException) {
                        log.error("Auth service unreachable: {}", ex.getMessage());
                        return Mono.error(new ServiceUnavailableException(
                                GatewayErrorCode.BAD_GATEWAY_001));
                    }
                    log.error("Auth service call failed: {}", ex.getMessage(), ex);
                    return Mono.error(new ServiceUnavailableException(
                            GatewayErrorCode.BAD_GATEWAY_001));
                });
    }
}
