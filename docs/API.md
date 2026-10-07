# API 使用说明

API 前缀为 `/api/v1`，Swagger 开发地址为 `http://localhost:8080/api/swagger-ui.html`。响应统一包含 `code`、`message`、`data` 和 `requestId`。

## 演示登录

仅 `demo` Profile 开放：

```http
POST /api/v1/auth/dev-login
Content-Type: application/json

{"email":"customer@example.test"}
```

后续请求携带 `Authorization: Bearer <accessToken>`。Access Token 有效 2 小时，Refresh Token 有效 7 天且每次刷新后旧令牌立即失效。

## 主要资源

- 公开菜单：`GET /public/merchants`、`GET /public/stores`、`GET /public/stores/{id}/menu`
- 顾客订单：`POST/GET /customer/orders`、`GET /customer/orders/{id}`、`POST /customer/orders/{id}/cancel`
- 商家经营：`/merchant/stores`、`/merchant/categories`、`/merchant/products`、`/merchant/spec-groups`
- 商家订单：`/merchant/orders` 及 `accept/reject/ready/complete` 状态操作
- 平台审核：`/platform/merchants` 及 `approve/reject/suspend`
- 图片上传：`POST /uploads/images`，字段名为 `file`

创建订单必须携带不超过 80 字符的 `X-Idempotency-Key`。客户端金额不会被采信，商品和规格价格全部由服务端重新计算。
