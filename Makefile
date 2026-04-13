# Hify 项目 Makefile
# 包含：启动、停止、重启、构建、清理、打包

.PHONY: help start stop restart build clean package

PROJECT_ROOT := $(shell pwd)
VERSION      := $(shell sed -n 's/.*<version>\([^<]*\)<\/version>.*/\1/p' pom.xml | head -n 1)
SERVER_JAR   := $(wildcard hify-server/target/hify-server-*.jar)
BACKEND_PID  := hify-server/hify-server.pid
BACKEND_LOG  := hify-server/backend.log
FRONTEND_PID := hify-web/hify-web.pid
FRONTEND_LOG := hify-web/frontend.log
HEALTH_URL   := http://localhost:8080/api/actuator/health
PACKAGE_NAME := hify-$(VERSION)
PACKAGE_DIR  := dist/$(PACKAGE_NAME)

help:
	@echo "Hify 项目命令："
	@echo "  make start   - 启动 PostgreSQL/Redis、后端和前端开发服务器"
	@echo "  make stop    - 停止前端开发服务器、后端和 PostgreSQL/Redis"
	@echo "  make restart - 停止后重新启动所有服务"
	@echo "  make build   - 构建后端 (Maven) + 前端 (npm run build)"
	@echo "  make clean   - 清理 Maven target 和前端 dist/node_modules"
	@echo "  make package - 打成可分发 tar.gz（后端 jar + 前端 dist + 部署文件）"

# =============================================================================
# 启动 / 停止 / 重启
# =============================================================================
start: infra backend frontend

infra:
	@echo ">>> 启动基础设施 (PostgreSQL & Redis)..."
	docker compose up -d postgres redis
	@echo ">>> 等待服务就绪..."
	@for port in 5432 6379; do \
		elapsed=0; \
		while ! nc -z localhost $$port >/dev/null 2>&1; do \
			if [ $$elapsed -ge 30 ]; then echo ""; echo "错误：端口 $$port 未在 30 秒内就绪"; exit 1; fi; \
			sleep 1; elapsed=$$((elapsed + 1)); \
		done; \
		echo "端口 $$port 已就绪"; \
	done

backend:
	@if [ -z "$(SERVER_JAR)" ]; then \
		echo ">>> 未找到后端 jar，先执行构建..."; \
		$(MAKE) build-backend; \
	fi
	@echo ">>> 启动后端服务 (profile: local)..."
	@if [ -f "$(BACKEND_PID)" ] && kill -0 $$(cat $(BACKEND_PID)) 2>/dev/null; then \
		echo "后端已在运行 (PID: $$(cat $(BACKEND_PID)))"; \
	else \
		nohup java -jar "$(SERVER_JAR)" --spring.profiles.active=local > "$(BACKEND_LOG)" 2>&1 & \
		echo $$! > "$(BACKEND_PID)"; \
		echo "后端进程 PID: $$!，等待健康检查..."; \
		elapsed=0; \
		while ! curl -sf "$(HEALTH_URL)" >/dev/null 2>&1; do \
			if ! kill -0 $$(cat $(BACKEND_PID)) 2>/dev/null; then \
				echo ""; echo "错误：后端进程异常退出，查看日志 $(BACKEND_LOG)"; exit 1; \
			fi; \
			if [ $$elapsed -ge 120 ]; then echo ""; echo "错误：后端健康检查超时"; exit 1; fi; \
			sleep 2; elapsed=$$((elapsed + 2)); \
			echo -n "."; \
		done; \
		echo ""; \
		echo "后端健康检查通过: $(HEALTH_URL)"; \
	fi

frontend:
	@echo ">>> 启动前端开发服务器..."
	@if [ -f "$(FRONTEND_PID)" ] && kill -0 $$(cat $(FRONTEND_PID)) 2>/dev/null; then \
		echo "前端已在运行 (PID: $$(cat $(FRONTEND_PID)))"; \
	else \
		cd hify-web && nohup npm run dev > "$(PROJECT_ROOT)/$(FRONTEND_LOG)" 2>&1 & \
		echo $$! > "$(PROJECT_ROOT)/$(FRONTEND_PID)"; \
		echo "前端进程 PID: $$!"; \
	fi

stop:
	@echo ">>> 停止前后端..."
	@./stop.sh || true

restart: stop start

# =============================================================================
# 构建 / 清理
# =============================================================================
build: build-backend build-frontend

build-backend:
	@echo ">>> 构建后端 (mvn clean install -DskipTests)..."
	mvn clean install -DskipTests

build-frontend:
	@echo ">>> 构建前端 (npm install && npm run build)..."
	cd hify-web && npm install && npm run build

clean:
	@echo ">>> 清理后端构建产物..."
	mvn clean || true
	@echo ">>> 清理前端构建产物..."
	rm -rf hify-web/dist hify-web/node_modules

# =============================================================================
# 打包
# =============================================================================
package:
	$(MAKE) clean
	$(MAKE) build
	@echo ">>> 正在打包 $(PACKAGE_NAME).tar.gz ..."
	@mkdir -p $(PACKAGE_DIR)
	@cp "$(SERVER_JAR)" $(PACKAGE_DIR)/hify-server.jar
	@cp -r hify-web/dist $(PACKAGE_DIR)/web-dist
	@cp docker-compose.yml $(PACKAGE_DIR)/
	@mkdir -p $(PACKAGE_DIR)/sql-init
	@cp -r docs/sql/init/* $(PACKAGE_DIR)/sql-init/ 2>/dev/null || true
	@tar -czf dist/$(PACKAGE_NAME).tar.gz -C dist $(PACKAGE_NAME)
	@rm -rf $(PACKAGE_DIR)
	@echo "打包完成: dist/$(PACKAGE_NAME).tar.gz"
