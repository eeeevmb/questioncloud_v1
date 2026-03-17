# MySQL 初始化说明

- `docker/mysql/init/` 中的脚本仅会在 **首次** 创建数据卷 `qc_mysql_data` 时由 MySQL 容器自动执行。
- Docker 部署会额外挂载以下 SQL 到初始化目录并按文件名字典序执行：
  - `02_agent_domain.sql`（来源：`src/main/resources/sql/agent_domain.sql`）
  - `03_init_agent.sql`（来源：`src/main/resources/sql/init_agent.sql`）
- 若要重新初始化数据库，请执行 `docker compose down -v` 删除数据卷后再启动。

## 验证步骤

```bash
docker compose down -v
docker compose up -d mysql
docker compose exec mysql mysql -uroot -p"$QC_MYSQL_ROOT_PASSWORD" -e "SHOW TABLES FROM questioncloud_db;"
```
