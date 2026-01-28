## 🚀 快速开始

### 1. 拉取项目
```bash
   git clone -b dev https://github.com/eeeevmb/questioncloud_v1.git
```

### 2. 编辑环境变量
```bash
    cd questioncloud_v1 &&
    cp .env.example .env &&
    vim .env
```
将 .env 中的 change_me 替换为你自己的密码/配置（至少包含 MySQL 与 RabbitMQ）。

提示：.env 位于 docker-compose.yml 同级目录（项目根目录）。

### 3. 启动容器
```bash
    docker compose up -d --build
```

### 4. 访问服务
- 前端：http://localhost:10081
- 后端：http://localhost:18081

> 如果部署在服务器上，请把 localhost 换成服务器 IP 或域名，并确保安全组/防火墙放行端口 10081（以及 18081 如需对外开放）。