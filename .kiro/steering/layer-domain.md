---
inclusion: fileMatch
fileMatchPattern: "domain/**"
---

# Domain 层编码规则

网关服务作为无状态路由层，领域层几乎不使用。大部分 domain 子模块为空壳。

## 网关的特殊情况
- Gateway 的核心逻辑（认证验证、路由过滤）位于 `infrastructure/gateway/gateway-impl/`，而非 domain 层
- domain-model、domain-api、domain-impl 中通常没有业务实体或服务
- repository-api、cache-api、mq-api、security-api 端口接口在网关中不使用

## 如需扩展
如果未来网关需要领域逻辑（如速率限制规则、黑名单管理），仍应遵循：
- 实体命名：`{Name}Entity`，纯 Java POJO
- 服务命名：`{Name}DomainService` / `{Name}DomainServiceImpl`
- 通过 Port 接口访问基础设施

## 禁止事项
- 不要在 domain 层直接依赖 WebFlux 或 Spring Cloud Gateway 的类
- 不要在 domain 层处理 HTTP 请求/响应
