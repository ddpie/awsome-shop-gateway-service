---
inclusion: fileMatch
fileMatchPattern: "application/**"
---

# Application 层编码规则

网关服务作为无状态路由层，application 层几乎不使用。网关的核心逻辑在 `infrastructure/gateway/gateway-impl/` 中。

## 网关的特殊情况
- 网关没有传统的"用例编排"需求
- 认证验证逻辑直接在 Gateway Filter 中完成（调用 AuthServiceClient）
- application-api 和 application-impl 中通常没有业务服务

## 如需扩展
如果未来需要应用层逻辑（如聚合多个服务的响应），仍应遵循：
- 接口命名：`{Name}ApplicationService`
- 实现命名：`{Name}ApplicationServiceImpl`
- 只依赖 Domain Service 接口
- 使用 `@Service` + `@RequiredArgsConstructor`

## 禁止事项
- 不允许直接依赖 infrastructure 实现类
- 不允许在应用层处理 WebFlux 的 `ServerWebExchange`
