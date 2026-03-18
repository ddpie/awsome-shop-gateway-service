# 项目结构

DDD + 六边形架构，以 Maven 多模块项目组织。Gateway 作为无状态网关，模块使用与其他服务不同。

## 模块布局

```
├── common/                              # 公共工具（异常、错误码、Result）
├── domain/
│   ├── domain-model/                    # 领域模型（网关几乎不使用）
│   ├── domain-api/                      # 领域服务接口（网关几乎不使用）
│   ├── domain-impl/                     # 领域服务实现（网关几乎不使用）
│   ├── repository-api/                  # 仓储端口（网关不使用）
│   ├── cache-api/                       # 缓存端口（网关不使用）
│   ├── mq-api/                          # 消息队列端口（网关不使用）
│   └── security-api/                    # 安全端口（网关不使用）
├── infrastructure/
│   ├── repository/
│   │   └── mysql-impl/                  # 网关不使用数据库
│   ├── cache/
│   │   └── redis-impl/                  # 网关不使用缓存
│   ├── mq/
│   │   └── sqs-impl/                    # 网关不使用消息队列
│   ├── security/
│   │   └── jwt-impl/                    # 网关不本地验证 JWT
│   └── gateway/
│       └── gateway-impl/               # ★ 网关核心：AuthServiceClient、AuthGatewayFilter、GlobalExceptionHandler
├── application/
│   ├── application-api/                 # 网关几乎不使用
│   └── application-impl/               # 网关几乎不使用
├── interface/
│   ├── interface-http/                  # 网关不使用传统 Controller（路由在 YAML 定义）
│   └── interface-consumer/              # 网关不使用消息消费者
└── bootstrap/                           # Spring Boot 启动入口、路由配置、application.yml
```

## 网关核心代码位置
所有网关逻辑集中在 `infrastructure/gateway/gateway-impl/` 模块：

| 类 | 包路径 | 职责 |
|---|---|---|
| `AuthServiceClient` | `infrastructure.gateway.client` | WebClient 调用 Auth Service 验证令牌 |
| `AuthGatewayFilter` | `infrastructure.gateway.filter` | Gateway 过滤器，拦截请求、验证认证、注入头信息 |
| `GlobalExceptionHandler` | `infrastructure.gateway.config` | WebFlux 全局异常处理（非 MVC @RestControllerAdvice） |
| `GatewayConfig` | `infrastructure.gateway.config` | WebClient Bean 配置 |

## 依赖规则
- `gateway-impl` 依赖 `common`（使用错误码、Result）
- `bootstrap` 聚合 gateway-impl，用于 Spring DI 装配
- **其他模块（domain、repository、cache、mq、jwt）在网关中基本为空壳**
- **禁止修改 pom.xml**：未经确认不得修改

## 包命名约定
基础包名：`com.awsome.shop.gateway`

| 组件 | 包路径 |
|---|---|
| 网关客户端 | `infrastructure.gateway.client` |
| 网关过滤器 | `infrastructure.gateway.filter` |
| 网关配置 | `infrastructure.gateway.config` |
| 错误码 | `common.error`（GatewayErrorCode） |

## 与 Auth Service 的区别
- **网关是 WebFlux 响应式**，Auth 是 Servlet/MVC
- 网关没有数据库、没有 MyBatis、没有 Redis
- 网关没有传统 Controller，路由在 `application-docker.yml` / `application-local.yml` 中 YAML 定义
- 全局异常处理使用 WebFlux 的 `ErrorWebExceptionHandler`，非 MVC 的 `@RestControllerAdvice`
- HTTP 调用使用 `WebClient`，异常类型为 `WebClientRequestException`
