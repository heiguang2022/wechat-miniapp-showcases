# 微信小程序接单样例交付中心

这里汇总三个可独立展示、报价和二次开发的原生微信小程序样例。展示素材与源码分离存放，不会进入小程序主包。

| 样例 | 适用客户 | 核心流程 | 交付目录 |
| --- | --- | --- | --- |
| 一刻咖啡 | 咖啡、茶饮、烘焙、轻餐饮 | 选品、规格、购物袋、结算、订单 | [brew-coffee](./brew-coffee/) |
| 漫游城市 | 活动平台、文旅、本地生活、展览机构 | 筛选、收藏、详情、报名、票夹 | [city-discovery](./city-discovery/) |
| 微光习惯 | 健康、教育、自律工具、企业关怀 | 打卡、新建习惯、统计、个人管理 | [habit-tracker](./habit-tracker/) |

每个交付包包含：

- `cover.png`：项目封面
- `screenshots/`：4 张关键流程截图
- `demo.mp4`：33 秒竖屏演示视频
- `README.md`：客户版项目说明与运行方法
- `feature-spec.md`：功能边界和二次开发方向
- `demo-script.md`：视频讲解与销售演示话术
- `test-report.md`：已经执行的测试与结果
- `acceptance-checklist.md`：正式交付前的验收清单
- `manifest.md`：视频与源码包的 SHA-256 校验信息
- `source.zip`：可直接导入微信开发者工具的源码包

## 可直接发送的完整压缩包

- [一刻咖啡完整交付包](./packages/brew-coffee-delivery.zip)
- [漫游城市完整交付包](./packages/city-discovery-delivery.zip)
- [微光习惯完整交付包](./packages/habit-tracker-delivery.zip)

## 使用建议

对外发样例时，可先发送封面、演示视频和项目说明；客户确认方向后，再提供源码包或在线演示。正式上线项目需要替换为客户自己的 AppID，并按实际业务接入后端、登录、支付或内容管理系统。
