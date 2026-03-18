---
inclusion: fileMatch
fileMatchPattern: "interface/**"
---

# Interface 层编码规则

网关服务不使用传统的 REST Controller。路由规则在 YAML 配置文件中定义，由 Spring Cloud Gateway 自动处理。

## 网关的特殊情况
- **没有 `@RestController`**：网关不暴露自己的 API，只转发请求到后端服务
- **路由定义在 YAML 中**：`bootstrap/src/main/resources/application-{profile}.yml` 的 `spring.cloud.gateway.server.webflux.routes`
- **过滤器在 infrastructure 层**：`AuthGatewayFilter` 位于 `infrastructure/gateway/gateway-impl/`
- **全局异常处理在 infrastructure 层**：`GlobalExceptionHandler` 实现 `ErrorWebExceptionHandler`

## interface-http / interface-consumer
- 这两个子模块在网关中为空壳
- 不需要在此创建 Controller 或 Consumer

## 路由配置格式
```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: route-name
              uri: http://service-host:port
              predicates:
                - Path=/api/v1/scope/module/**
              metadata:
                auth-required: true/false
```

## 禁止事项
- 不要在 interface 层创建 MVC Controller（网关是 WebFlux）
- 不要在 interface 层定义路由（路由在 YAML 配置中）
- 不要在 interface 层处理认证逻辑（认证在 gateway-impl 的过滤器中）
