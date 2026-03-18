package com.awsome.shop.gateway.facade.http.request.common;

/**
 * 网关可注入请求接口
 *
 * <p>定义网关注入到 JSON 请求体中的标准字段</p>
 *
 * <p>网关注入字段：</p>
 * <ul>
 *   <li>operatorId - 操作人ID（从 JWT 验证结果中提取）</li>
 * </ul>
 */
public interface GatewayInjectableRequest {

    /**
     * 获取操作人ID
     *
     * @return 操作人ID
     */
    String getOperatorId();

    /**
     * 设置操作人ID
     *
     * @param operatorId 操作人ID
     */
    void setOperatorId(String operatorId);
}
