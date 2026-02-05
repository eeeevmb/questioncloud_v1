# MySQL 初始化说明

- `docker/mysql/init/` 下脚本仅会在 **第一次** 创建 `qc_mysql_data` 数据卷时执行；之后复用旧卷不会再次建表。
- 若需要重建数据库，请执行 `docker compose down -v` 删除卷后重新启动。

## 验证命令

```bash
docker compose down -v
docker compose up -d mysql
docker compose exec mysql mysql -uroot -p"$QC_MYSQL_ROOT_PASSWORD" -e "SHOW TABLES FROM questioncloud_db;"
```
