package com.awsome.shop.gateway.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Gateway-specific error codes (aligned with design doc)
 */
@Getter
@AllArgsConstructor
public enum GatewayErrorCode implements ErrorCode {

    AUTHZ_001("AUTHZ_001", "未授权，请先登录"),
    FORBIDDEN_001("FORBIDDEN_001", "权限不足"),
    BAD_GATEWAY_001("BAD_GATEWAY_001", "服务暂时不可用"),
    GATEWAY_TIMEOUT_001("GATEWAY_TIMEOUT_001", "请求超时");

    private final String code;
    private final String message;
}
