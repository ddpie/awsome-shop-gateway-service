package com.awsome.shop.gateway.application.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Auth validation response DTO from external auth service
 *
 * <p>Response format from auth-service /api/v1/internal/auth/validate:</p>
 * <pre>
 * { "success": true, "operatorId": 1, "role": "EMPLOYEE", "message": "验证成功" }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthValidateResponse {

    private boolean success;

    private Long operatorId;

    private String role;

    private String message;
}
