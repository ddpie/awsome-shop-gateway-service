# API Gateway 架构方案

## 1. 服务定位

API 网关（api-gateway）是 AWSomeShop 平台的统一入口，负责认证鉴权、路由转发、安全防护。

| 属性 | 值 |
|------|-----|
| 端口 | 8080 |
| 框架 | Spring Cloud Gateway (WebFlux 响应式) |
| 运行时 | Netty (非阻塞 I/O) |
| 数据库 | 无（无状态服务） |

---

## 2. 核心职责

```
Client ──HTTP──▶ ┌─────────────────────────────────┐ ──▶ Downstream Services
                 │           api-gateway            │
                 ├─────────────────────────────────┤
                 │  1. 安全头清除 (防伪造)           │      auth-service  :8001
                 │  2. 令牌远程验证 (调 auth-service)│      product-service:8002
                 │  3. 角色权限检查 (ADMIN 路径)     │      points-service :8003
                 │  4. 用户信息注入 (X-Operator-Id)  │      order-service  :8004
                 │  5. 路由转发 (保留完整路径)       │
                 └─────────────────────────────────┘
```

---

## 3. 过滤器链

```
Request
  │
  ▼
AccessLogFilter          (HIGHEST_PRECEDENCE)  记录请求日志
  │
  ▼
AuthenticationGatewayFilter  (order=100)       认证 + 鉴权
  │  1. 安全头清除 (X-Operator-Id, X-User-Role)
  │  2. 公开路径检查 → 直接放行
  │  3. 提取 Bearer Token
  │  4. 调用 auth-service 验证 Token
  │  5. ADMIN 路径角色检查
  │  6. 注入 X-Operator-Id 和 X-User-Role
  │
  ▼
OperatorIdInjectionFilter   (order=200)        注入 operatorId 到 JSON Body
  │
  ▼
Route → Downstream Service
```

---

## 4. 认证设计

### 令牌验证方式：远程调用

```
Gateway ──POST──▶ auth-service:/api/v1/internal/auth/validate
          Body: {"token": "eyJhbG..."}

          ◀── {"success": true, "operatorId": 1, "role": "ADMIN"}
          ◀── {"success": false, "message": "Token无效或已过期"}
```

- gateway **不持有 JWT 密钥**，完全依赖 auth-service 做验证
- 每个需认证的请求都会调用一次 auth-service
- 验证超时：5 秒

### 为什么不在网关本地验证 JWT？

| 考量 | 说明 |
|------|------|
| 密钥管理 | 不需要在多处同步 JWT_SECRET |
| 令牌撤销 | auth-service 未来可加黑名单，网关无需改动 |
| 职责分离 | 认证逻辑集中在 auth-service |
| 代价 | 每次请求多一次内部 HTTP 调用（~10ms） |

---

## 5. 路由配置

### 路径 → 服务映射

| 路径模式 | 目标服务 | 认证 | 说明 |
|---------|---------|------|------|
| `/api/v1/public/auth/**` | auth:8001 | 否 | 登录、注册 |
| `/api/v1/public/product/**` | product:8002 | 否 | 商品浏览 |
| `/api/v1/public/point/**` | point:8003 | 否 | 积分公开接口 |
| `/api/v1/public/order/**` | order:8004 | 否 | 兑换公开接口 |
| `/api/v1/private/user/**` | auth:8001 | 是 | 当前用户信息 |
| `/api/v1/admin/user/**` | auth:8001 | 是 + ADMIN | 用户管理 |
| `/api/v1/auth/**` | auth:8001 | 是 | 认证其他接口 |
| `/api/v1/product/**` | product:8002 | 是 | 商品管理 |
| `/api/v1/point/**` | point:8003 | 是 | 积分操作 |
| `/api/v1/order/**` | order:8004 | 是 | 兑换操作 |

### Scope 含义

| Scope | 认证 | 角色 | 用途 |
|-------|------|------|------|
| `public` | 无 | 无 | 未登录可访问 |
| `private` | Bearer Token | 任意角色 | 已登录用户 |
| `admin` | Bearer Token | ADMIN | 管理员专属 |
| `internal` | 无（仅内部网络） | 无 | 服务间通信 |

---

## 6. 安全设计

### BR-GW-012: 安全头清除

转发前**必须**清除客户端伪造的安全头：

```java
request.mutate().headers(headers -> {
    headers.remove("X-Operator-Id");
    headers.remove("X-User-Role");
}).build();
```

无论路径是否需要认证，都执行清除。

### BR-GW-006: ADMIN 路径检查

```java
private boolean isAdminOnly(String path) {
    return path.startsWith("/api/v1/admin/")
            || path.equals("/api/v1/files/upload");
}
```

非 ADMIN 角色访问 → 返回 403 FORBIDDEN_001。

### BR-GW-013: 用户信息注入

认证成功后注入两个头：
- `X-Operator-Id`: 用户 ID (Long)
- `X-User-Role`: 角色 (EMPLOYEE / ADMIN)

下游服务从这两个头获取当前用户身份。

---

## 7. 错误码

| 错误码 | HTTP | 触发场景 |
|--------|------|---------|
| AUTHZ_001 | 401 | Token 缺失/无效/过期/格式错误 |
| FORBIDDEN_001 | 403 | 非 ADMIN 访问管理员端点 |
| BAD_GATEWAY_001 | 502 | 下游服务不可达 |
| GATEWAY_TIMEOUT_001 | 504 | 下游服务响应超时 |

**错误响应格式**：

```json
{
  "code": "AUTHZ_001",
  "message": "未授权，请先登录",
  "data": null
}
```

---

## 8. OperatorId 注入机制

对于需要知道"谁在操作"的下游接口，gateway 除了注入 Header 外，还会将 `operatorId` 写入 JSON 请求体：

```
原始请求体: {"name": "张三", "status": "ACTIVE"}
注入后:     {"name": "张三", "status": "ACTIVE", "operatorId": "1"}
```

下游服务的请求 DTO 实现 `GatewayInjectableRequest` 接口即可自动接收。

---

## 9. 异常处理

```
异常类型                    HTTP     处理方式
────────────────────────── ──────── ──────────────────────
ServiceUnavailableException  502    auth-service 不可达
GatewayException(TIMEOUT)    504    auth-service 超时
ForbiddenException           403    角色权限不足
GatewayException(AUTHZ)      401    认证失败

下游服务返回的错误           原样透传  不修改状态码和响应体
```

---

## 10. 运行时依赖

```
                    ┌── auth-service:8001  (关键依赖 — Token 验证)
                    │
api-gateway:8080 ───┼── product-service:8002
                    │
                    ├── points-service:8003
                    │
                    └── order-service:8004
```

| 依赖 | 类型 | 影响 |
|------|------|------|
| auth-service | 关键 | 不可用则所有认证请求失败 (502) |
| 其他服务 | 路由目标 | 单个服务不可用只影响该服务的请求 |
| 数据库 | 无 | 网关无数据库依赖 |

---

## 11. 配置项

```yaml
gateway:
  services:
    auth: http://localhost:8001        # auth 服务地址
    product: http://localhost:8002     # product 服务地址
    point: http://localhost:8003       # points 服务地址
    order: http://localhost:8004       # order 服务地址
  auth:
    validate-url: http://localhost:8001/api/v1/internal/auth/validate
```

生产环境通过 Docker DNS 解析服务名（如 `http://auth-service:8001`）。
