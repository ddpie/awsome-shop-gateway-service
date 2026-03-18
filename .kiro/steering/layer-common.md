---
inclusion: fileMatch
fileMatchPattern: "common/**"
---

# Common 层编码规则

本模块是全局共享基础设施。网关服务中主要定义错误码和响应包装器。

## 职责范围
- 异常体系定义（BaseException 等）
- 错误码接口（ErrorCode）与枚举（GatewayErrorCode）
- 统一响应包装器（Result<T>）

## 网关错误码（GatewayErrorCode）
- `AUTHZ_001` — 未提供认证令牌（映射 401）
- `FORBIDDEN_001` — 令牌验证失败 / 权限不足（映射 403）
- `BAD_GATEWAY_001` — 后端 Auth 服务不可用（映射 502）
- `GATEWAY_TIMEOUT_001` — 后端 Auth 服务响应超时（映射 504）

## 错误码规范
- 实现 `ErrorCode` 接口，提供 `getCode()` 和 `getMessage()`
- 格式：`{CATEGORY}_{SEQ}`，类别前缀决定 HTTP 状态码映射：
  - `AUTH_` → 401, `AUTHZ_` → 401, `FORBIDDEN_` → 403
  - `BAD_GATEWAY_` → 502, `GATEWAY_TIMEOUT_` → 504
  - `PARAM_` → 400, `SYS_` → 500

## 编码约定
- 使用 Lombok `@Data`、`@Getter` 简化代码
- 不依赖 Spring 框架
- 不依赖任何业务模块
