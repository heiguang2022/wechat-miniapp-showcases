# 测试报告

- 测试基线日期：2026-10-07
- 后端：JUnit 5、MockMvc、Testcontainers MySQL 8.4
- 管理后台：Vue TypeScript 类型检查和 Vite 生产构建
- 小程序：仓库结构测试及微信开发者工具人工验收

## 自动化覆盖与结果

- 开发登录、JWT 刷新令牌轮换和生产禁用开关。
- 平台、商家、顾客角色授权。
- 已审核商家和公开菜单。
- 服务端价格计算、幂等下单及非法状态跳转。
- 跨顾客订单隔离和商家接口角色隔离。
- 图片内容与 MIME 不匹配拒绝。
- Flyway 在真实 MySQL 容器上从空库建表和初始化演示数据。

| 验证 | 结果 |
|---|---|
| Maven + JUnit/Testcontainers | 5 个测试全部通过 |
| Vue TypeScript + Vite 生产构建 | 通过；Element Plus 单入口包有体积提示 |
| 原有样例及新小程序结构测试 | 6 组全部通过 |
| Docker Compose 配置解析 | 通过 |
| API 与管理后台镜像构建 | 通过 |
| 四服务健康检查 | MySQL、API、Admin、Nginx 全部通过 |
| 部署后下单冒烟 | 顾客下单 26 元，商家接单进入 `PREPARING`，通过 |
| Swagger/OpenAPI | HTTP 200，通过 |

尚未执行微信开发者工具和 Android/iPhone 真机人工验收，因此相关项目保持未勾选。
