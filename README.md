# Awsome Shop API Gateway

API 网关 — AWSomeShop 平台的统一流量入口，负责认证鉴权与路由转发。

- **Java 21** / **Spring Boot 3.4.1** / **Spring Cloud Gateway (WebFlux)**
- 基于 Netty 的响应式非阻塞架构
- 远程 Token 验证（调用 auth-service）
- 无数据库依赖，纯无状态代理

---

## 核心功能

| 功能 | 说明 |
|------|------|
| 路由转发 | 按路径前缀分发到 auth/product/points/order 四个服务 |
| 远程认证 | 调用 auth-service 验证 Bearer Token |
| 角色鉴权 | `/api/v1/admin/**` 路径要求 ADMIN 角色 |
| 安全头清除 | 转发前清除 X-Operator-Id / X-User-Role 防伪造 |
| 用户信息注入 | 认证后注入 X-Operator-Id / X-User-Role 头 + operatorId 到请求体 |
| 错误处理 | 502 (服务不可达) / 504 (超时) / 401 (认证失败) / 403 (权限不足) |

---

## 过滤器链

```
Request → AccessLogFilter (HIGHEST_PRECEDENCE)
        → AuthenticationGatewayFilter (order=100)
            ├── 清除伪造安全头
            ├── 公开路径 → 放行
            ├── 提取 Bearer Token
            ├── 远程验证 (auth-service)
            ├── ADMIN 路径角色检查
            └── 注入 X-Operator-Id, X-User-Role
        → OperatorIdInjectionFilter (order=200)
            └── 注入 operatorId 到 JSON Body
        → Route → Downstream Service
```

---

## 路由配置

| 路径 | 目标 | 认证 |
|------|------|------|
| `/api/v1/public/auth/**` | auth:8001 | 无 |
| `/api/v1/public/product/**` | product:8002 | 无 |
| `/api/v1/private/user/**` | auth:8001 | Token |
| `/api/v1/admin/user/**` | auth:8001 | Token + ADMIN |
| `/api/v1/product/**` | product:8002 | Token |
| `/api/v1/point/**` | point:8003 | Token |
| `/api/v1/order/**` | order:8004 | Token |

---

## 模块结构

```
awsome-shop-gateway-service/
├── common/                          # 异常、错误码、常量
│   ├── enums/GatewayErrorCode       # AUTHZ_001, FORBIDDEN_001, BAD_GATEWAY_001, GATEWAY_TIMEOUT_001
│   ├── constants/RouteConstants     # 路径常量、Header 名
│   └── exception/                   # GatewayException, ForbiddenException, ServiceUnavailableException
├── domain/
│   └── domain-model/                # AuthenticationResult (认证结果领域模型)
├── infrastructure/
│   └── gateway/gateway-impl/
│       ├── filter/                   # AuthenticationGatewayFilter, OperatorIdInjectionFilter
│       ├── client/AuthServiceClient  # WebClient 调用 auth-service
│       └── config/                   # GlobalExceptionHandler
├── application/
│   └── application-api/             # AuthValidateResponse DTO
├── interface/
│   └── interface-http/              # GatewayInjectableRequest, PageableRequest
└── bootstrap/                       # 启动 + 路由配置 (application-local.yml)
```

---

## 快速开始

```bash
# 1. 确保 auth-service 已启动在 8001 端口

# 2. 编译
mvn clean install -DskipTests

# 3. 启动
mvn spring-boot:run -pl bootstrap -Dspring-boot.run.profiles=local

# 4. 访问聚合 Swagger
open http://localhost:8080/swagger-ui.html

# 5. 测试公开接口
curl -X POST http://localhost:8080/api/v1/public/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

---

## 架构文档

详见 [docs/architecture.md](docs/architecture.md)

---

## 错误码

| 错误码 | HTTP | 场景 |
|--------|------|------|
| AUTHZ_001 | 401 | Token 缺失/无效/过期 |
| FORBIDDEN_001 | 403 | 非 ADMIN 访问管理员端点 |
| BAD_GATEWAY_001 | 502 | 下游服务不可达 |
| GATEWAY_TIMEOUT_001 | 504 | 下游服务超时 |

---

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 运行时 |
| Spring Boot | 3.4.1 | 应用框架 |
| Spring Cloud Gateway | - | 响应式网关 (WebFlux/Netty) |
| WebClient | - | 非阻塞 HTTP 客户端 (调用 auth-service) |
| SpringDoc | 2.7.0 | 聚合 Swagger 文档 |
| Lombok | - | 代码简化 |
