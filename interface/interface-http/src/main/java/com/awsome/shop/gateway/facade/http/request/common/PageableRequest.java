package com.awsome.shop.gateway.facade.http.request.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 分页请求基类
 *
 * <p>提供标准分页参数，所有需要分页的请求 DTO 应继承此类</p>
 *
 * <p>网关注入支持：</p>
 * <ul>
 *   <li>operatorId - 操作人ID（网关从 JWT 验证结果中提取并注入）</li>
 * </ul>
 */
@Schema(description = "分页请求基类")
public abstract class PageableRequest implements GatewayInjectableRequest {

    @Schema(description = "页码（从 1 开始）", example = "1", minimum = "1")
    @Min(value = 1, message = "页码最小为 1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "20", minimum = "1", maximum = "100")
    @Min(value = 1, message = "每页大小最小为 1")
    @Max(value = 100, message = "每页大小最大为 100")
    private Integer size = 20;

    // ==================== 网关注入字段 ====================

    @Schema(description = "操作人ID（网关注入）", hidden = true)
    private String operatorId;

    // ==================== Getters and Setters ====================

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    @Override
    public String getOperatorId() {
        return operatorId;
    }

    @Override
    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }
}
