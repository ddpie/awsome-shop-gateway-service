---
inclusion: fileMatch
fileMatchPattern: "infrastructure/**"
---

# Infrastructure 层编码规则

网关的核心逻辑集中在 `infrastructure/gateway/gateway-impl/` 模块。其他 infrastructure 子模块（mysql、redis、sqs、jwt）在网关中为空壳。

## gateway-impl 模块（网关核心）

### AuthServiceClient（认证服务客户端）
- 包路径：`infrastructure.gateway.client`
- 使用 `WebClient` 调用 Auth Service 的 `/api/v1/internal/auth/validate`
- 请求：`{ "token": "..." }`
- 响应：解析 `success`、`operatorId`、`role`、`message` 字段
- 超时：`gateway.auth.timeout` 配置（默认 5s）
- 异常处理：
  - `WebClientRequestException` — Auth 服务连接失败（非 ConnectException，因为 WebFlux 包装了底层异常）
  - `TimeoutException` — 响应超时

### AuthGatewayFilter（认证网关过滤器）
- 包路径：`infrastructure.gateway.filter`
- 实现 `GlobalFilter` + `Ordered`
- 流程：
  1. 检查路由 metadata 的 `auth-required` 标记
  2. 从 `Authorization: Bearer xxx` 头提取 token
  3. 调用 `AuthServiceClient.validateToken(token)`
  4. 成功：注入 `X-Operator-Id`、`X-User-Role` 头 + JSON body 中的 `operatorId`
  5. 失败：返回对应错误码

### GlobalExceptionHandler（全局异常处理）
- 包路径：`infrastructure.gateway.config`
- 实现 WebFlux 的 `ErrorWebExceptionHandler`（**非** MVC 的 `@RestControllerAdvice`）
- 处理 `WebClientRequestException`（502）、`TimeoutException`（504）等
- 响应格式使用 `Result` 包装器

### GatewayConfig（网关配置）
- 包路径：`infrastructure.gateway.config`
- 定义 `WebClient` Bean

## 重要注意事项
- 网关是 **WebFlux 响应式**，所有操作返回 `Mono<Void>` 或 `Mono<T>`
- 不要使用阻塞 API（如 `block()`、`Thread.sleep()`）
- HTTP 客户端必须用 `WebClient`，不能用 `RestTemplate`
- 连接异常类型是 `WebClientRequestException`，不是 `ConnectException`

## 空壳模块（网关不使用）
- `mysql-impl` — 网关无数据库
- `redis-impl` — 网关无缓存
- `sqs-impl` — 网关无消息队列
- `jwt-impl` — 网关不本地验证 JWT（委托给 Auth Service）

## 禁止事项
- 不允许在网关中编写业务逻辑（网关只负责路由和认证转发）
- 不允许使用阻塞操作
- 不允许依赖 application 层或 interface 层
