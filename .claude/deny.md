# 禁止操作列表

以下命令/操作在任何情况下都不允许执行，除非用户明确书面授权。

## 数据销毁类

- `docker compose down -v` — 会删除数据卷导致所有业务数据永久丢失
- `docker volume rm` — 删除 Docker 卷
- `rm -rf` 涉及数据库数据目录 — 如 `/var/lib/postgresql/data`
- `DROP DATABASE` — 删除整个数据库
- `DROP TABLE` 不带备份确认 — 删除业务表

## 规则

1. 任何会删除/清空业务数据的命令，必须先向用户明确说明影响范围，获得确认后方可执行。
2. 修改数据库结构优先使用 `ALTER TABLE`，而不是删库重建。
3. 如果必须重建数据，先备份（`pg_dump` 或导出），再操作。
