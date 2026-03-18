# 技术栈与构建

## 运行时与语言
- Java 21
- Spring Boot 3.4.1（**WebFlux 响应式**，非 Servlet/MVC）
- Spring Cloud Gateway 2025.0.0

## 关键区别：Gateway 是响应式服务
- 使用 `spring-cloud-starter-gateway`（基于 Netty + WebFlux）
- **不使用** Spring MVC、Servlet、MyBatis、MySQL、Redis、SQS
- **不使用** `@RestController`、`@RequestMapping` 等 MVC 注解（全局异常处理除外）
- HTTP 客户端使用 `WebClient`（响应式），非 RestTemplate
- 异常类型：`WebClientRequestException`（非 ConnectException）

## 构建系统
- Maven（多模块 POM）
- Lombok 1.18.36（注解处理器）

## 网关特有依赖
- Spring Cloud Gateway（路由、过滤器、断言）
- WebClient（调用 Auth Service 验证令牌）
- Jackson（JSON 序列化/反序列化）

## 容器化
- 多阶段 Docker 构建：`maven:3.9-eclipse-temurin-21` → `eclipse-temurin:21-jre`
- 容器端口：8080
- 默认 profile：`docker`

## 常用命令

```bash
# 全量构建（跳过测试）
mvn clean install -DskipTests

# 启动应用（local 配置，端口 8080）
mvn spring-boot:run -pl bootstrap

# Docker 构建
docker build -t awsome-shop-gateway-service .
```

## 环境配置
- `local`（默认）— 本地开发（后端服务 localhost）
- `docker` — Docker 部署（后端服务使用 Docker DNS）

## 配置项
- `gateway.services.auth/product/point/order` — 后端服务 URL
- `gateway.auth.validate-url` — Auth 验证接口完整 URL
- `gateway.auth.timeout` — Auth 调用超时时间（默认 5s）
- 路由定义在 `spring.cloud.gateway.server.webflux.routes` 中
