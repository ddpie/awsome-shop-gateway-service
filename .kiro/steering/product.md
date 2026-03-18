# 网关服务概述

Awsome Shop Gateway Service — 电商平台 API 网关微服务，基于 Spring Cloud Gateway（WebFlux 响应式）构建。

## 核心职责
- 统一入口：所有前端请求经由网关路由到后端微服务
- 认证鉴权：拦截需要认证的请求，调用 Auth Service 验证 JWT 令牌
- 请求增强：验证通过后注入 X-Operator-Id、X-User-Role 头及 JSON Body 中的 operatorId
- 路由转发：按 URL 路径规则将请求转发到对应微服务

## 路由规则

| Route ID | URI | 路径匹配 | 认证 |
|---|---|---|---|
| auth-public | auth-service:8001 | /api/v1/public/auth/** | 否 |
| product-public | product-service:8002 | /api/v1/public/product/** | 否 |
| point-public | points-service:8003 | /api/v1/public/point/** | 否 |
| order-public | order-service:8004 | /api/v1/public/order/** | 否 |
| auth-private | auth-service:8001 | /api/v1/private/user/** | 是 |
| auth-admin | auth-service:8001 | /api/v1/admin/user/** | 是 |
| auth-protected | auth-service:8001 | /api/v1/auth/** | 是 |
| product-protected | product-service:8002 | /api/v1/product/** | 是 |
| point-protected | points-service:8003 | /api/v1/point/** | 是 |
| order-protected | order-service:8004 | /api/v1/order/** | 是 |

## 认证流程
1. 请求到达 Gateway → `AuthGatewayFilter` 检查路由 metadata `auth-required`
2. 如需认证，从 Authorization 头提取 Bearer token
3. 调用 Auth Service：POST `http://auth-service:8001/api/v1/internal/auth/validate`
4. 验证成功 → 注入 X-Operator-Id、X-User-Role 头 + body 中的 operatorId
5. 验证失败 → 返回 401/403 错误

## 错误码（GatewayErrorCode）
- `AUTHZ_001` — 未提供认证令牌（401）
- `FORBIDDEN_001` — 令牌验证失败（403）
- `BAD_GATEWAY_001` — Auth 服务不可用（502）
- `GATEWAY_TIMEOUT_001` — Auth 服务响应超时（504）

## 后端服务地址（Docker DNS）
- auth-service:8001、product-service:8002、points-service:8003、order-service:8004
