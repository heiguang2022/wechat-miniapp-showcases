# 微信小程序精品样例与一刻咖啡多商户 MVP

仓库在保留三个原生微信小程序样例的基础上，新增了可部署的“一刻咖啡”多商户餐饮 MVP：

- `apps/miniapp`：顾客端原生微信小程序。
- `apps/api`：Java 17、Spring Boot、MyBatis-Plus、MySQL API。
- `apps/admin`：Vue 3 + TypeScript + Element Plus 运营后台。
- `deploy`：MySQL、API、后台和 Nginx 的 Docker Compose 部署。
- `docs`：接口、部署、演示、测试和验收文档。

完整本地演示请从 [`docs/DEPLOYMENT.md`](./docs/DEPLOYMENT.md) 开始。旧版离线样例仍保留在原目录中，便于回归与素材复用。

## 原始离线样例

三个零依赖、可直接运行的原生微信小程序。每个目录都是独立项目，使用测试 AppID 导入微信开发者工具即可。

| 项目 | 目录 | 主要体验 |
| --- | --- | --- |
| 一刻咖啡 | `brew-miniapp` | 分类点单、规格选择、购物袋、结算、订单状态 |
| 漫游城市 | `city-miniapp` | 活动筛选、收藏、报名、个人票夹 |
| 微光习惯 | `habit-miniapp` | 每日打卡、连续记录、趋势统计、习惯管理 |

所有业务数据都保存在本机 `Storage` 中。开发者工具中选择对应目录导入，编译后即可体验；如需重置演示数据，可在开发者工具清除缓存。

## 本地测试

仓库自带零依赖测试套件，安装 Node.js 后在根目录运行：

```powershell
npm test
```

## 接单展示素材

三个项目的封面、关键截图、33 秒演示视频、功能规格、测试报告、验收清单和独立源码包统一放在 [`deliverables`](./deliverables/) 目录。

测试覆盖项目配置、WXML/WXSS 结构、事件绑定、图片与主包体积，以及咖啡下单、活动收藏报名、习惯打卡管理等核心流程。真实渲染、设备兼容和扫码预览仍需微信开发者工具。
