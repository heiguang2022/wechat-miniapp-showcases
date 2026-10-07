# 部署与本地运行

## 一键启动演示环境

1. 复制根目录 `.env.example` 为 `.env`，修改数据库密码和至少 32 字节的 `JWT_SECRET`。
2. 在仓库根目录运行：

```powershell
docker compose --env-file .env -f deploy/docker-compose.yml up --build -d
```

3. 等待服务健康后访问：

- 管理后台：`http://localhost/admin/`
- API 健康检查：`http://localhost/actuator/health`
- Swagger：`http://localhost/api/swagger-ui.html`

Compose 使用 `demo` Profile，首次启动由 Flyway 建表并显式加载演示数据。

## 微信开发者工具

导入 `apps/miniapp`。本机开发可关闭“校验合法域名”；真机或正式环境必须把 `config.js` 改为备案且已加入小程序后台白名单的 HTTPS 域名。

## 生产模式

```powershell
docker compose --env-file .env -f deploy/docker-compose.yml -f deploy/docker-compose.prod.yml up --build -d
```

生产覆盖配置启用 `prod` Profile：关闭开发登录、测试账号初始化和 Swagger UI。正式部署应在 Nginx 前配置 HTTPS，并将 CORS 域名改为实际后台域名。

## 数据备份与恢复

```powershell
docker compose -f deploy/docker-compose.yml exec mysql mysqldump -uroot -p yike_coffee > yike-coffee.sql
docker compose -f deploy/docker-compose.yml exec -T mysql mysql -uroot -p yike_coffee < yike-coffee.sql
```

上传图片位于 `uploads` Docker 卷；数据库备份不包含图片，需同时备份该卷。升级前先备份，Flyway 迁移只能向前执行，不应手工修改已应用的迁移文件。

## 本机 Java 构建

当前机器全局 `JAVA_HOME` 指向 JDK 8。运行 Maven 前应临时选择 JDK 17：

```powershell
$env:JAVA_HOME='C:\Users\DJL\.jdks\ms-17.0.17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd apps/api
.\mvnw.cmd test
```
