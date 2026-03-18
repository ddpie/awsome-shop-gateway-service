---
inclusion: fileMatch
fileMatchPattern: "bootstrap/**"
---

# Bootstrap 层编码规则

启动模块，负责 Spring Boot 应用组装和路由配置。网关没有数据库，不使用 Flyway。

## 职责
- Spring Boot 启动入口（`Application.java`）
- `@ComponentScan(basePackages = "com.awsome.shop.gateway")` 扫描所有模块
- 聚合 gateway-impl 模块的 Maven 依赖，由 Spring DI 完成绑定
- 多环境配置文件（`application-{profile}.yml`）
- **路由规则定义**（核心配置）

## 配置文件
- `application.yml` — 基础配置（端口 8080）
- `application-local.yml` — 本地开发（后端服务 localhost）
- `application-docker.yml` — Docker 部署（后端服务 Docker DNS）

## 关键配置项

### 后端服务地址
```yaml
gateway:
  services:
    auth: http://auth-service:8001
    product: http://product-service:8002
    point: http://points-service:8003
    order: http://order-service:8004
  auth:
    validate-url: http://auth-service:8001/api/v1/internal/auth/validate
    timeout: 5s
```

### 路由规则
- 定义在 `spring.cloud.gateway.server.webflux.routes` 下
- 每条路由：id、uri、predicates（Path）、metadata（auth-required）
- public 路由：`auth-required: false`
- protected/private/admin 路由：`auth-required: true`

## 与 Auth Service Bootstrap 的区别
- **没有 Flyway**：网关无数据库
- **没有 MyBatis 配置**：网关无 ORM
- **没有 Redis 配置**：网关无缓存
- **核心是路由配置**：YAML 中的 routes 定义是网关的"业务逻辑"

## 禁止事项
- 不在 bootstrap 中编写业务逻辑
- 不添加数据库相关配置（网关是无状态服务）
- 不使用 Servlet/MVC 相关配置
