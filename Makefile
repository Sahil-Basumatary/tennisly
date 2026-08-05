SHELL := /bin/bash
.DEFAULT_GOAL := help

ENV_FILE := infrastructure/docker/.env
COMPOSE := docker compose -f infrastructure/docker/docker-compose.yml --env-file $(ENV_FILE)
MVNW := ./mvnw
RUN_DIR := .run
PID_DIR := $(RUN_DIR)/pids
LOG_DIR := $(RUN_DIR)/logs

# Spring Boot 3.2 does not support the JDK 25 that ships as this machine's default.
JAVA_HOME ?= $(shell /usr/libexec/java_home -v 21 2>/dev/null)

# Java services run outside Docker, so secrets have to be loaded into their JVM.
LOAD_ENV = set -a; [ -f $(ENV_FILE) ] && source $(ENV_FILE); set +a; export JAVA_HOME=$(JAVA_HOME); export PATH=$$JAVA_HOME/bin:$$PATH;

.PHONY: help
help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

.PHONY: check-env
check-env: ## Verify the API keys are present before booting anything
	@test -f $(ENV_FILE) || { echo "missing $(ENV_FILE) — copy it from .env.example"; exit 1; }
	@$(LOAD_ENV) \
		for key in TENNIS_BALLDONTLIE_API_KEY TENNIS_LIVETENNIS_API_KEY; do \
			if [ -z "$${!key}" ]; then echo "$$key is empty in $(ENV_FILE)"; exit 1; fi; \
		done; \
		echo "API keys present"
	@test -n "$(JAVA_HOME)" || { echo "no JDK 21 found — install temurin-21"; exit 1; }
	@echo "JDK 21 at $(JAVA_HOME)"

.PHONY: infra-up
infra-up: ## Start postgres, redis, kafka and minio
	$(COMPOSE) --profile infra up -d postgres redis kafka minio
	@$(COMPOSE) ps

.PHONY: infra-down
infra-down: ## Stop infrastructure, keep volumes
	$(COMPOSE) --profile infra --profile tools down

.PHONY: infra-nuke
infra-nuke: ## Stop infrastructure and delete all data volumes
	$(COMPOSE) --profile infra --profile tools down -v

.PHONY: infra-ps
infra-ps: ## Show container status
	$(COMPOSE) ps

.PHONY: infra-logs
infra-logs: ## Tail infrastructure logs
	$(COMPOSE) logs -f

.PHONY: tools-up
tools-up: ## Start pgadmin, redis-commander and kafka-ui
	$(COMPOSE) --profile tools up -d

# --- one-command boot -------------------------------------------------------

.PHONY: up
up: check-env infra-up ## Start infra + every app service in the background
	@mkdir -p $(PID_DIR) $(LOG_DIR)
	@echo "waiting for postgres/redis/kafka"
	@$(COMPOSE) up -d --wait postgres redis kafka 2>/dev/null || true
	@$(MAKE) --no-print-directory _start-bg NAME=eureka CMD='$(MVNW) -pl services/eureka-server spring-boot:run'
	@$(MAKE) --no-print-directory _wait-http PORT=8761 LABEL=eureka
	@$(MAKE) --no-print-directory _start-bg NAME=tennis-data CMD='$(MVNW) -pl services/tennis-data-service spring-boot:run'
	@$(MAKE) --no-print-directory _wait-http PORT=8083 LABEL=tennis-data
	@$(MAKE) --no-print-directory _start-bg NAME=match CMD='$(MVNW) -pl services/match-service spring-boot:run'
	@$(MAKE) --no-print-directory _start-bg NAME=replay CMD='$(MVNW) -pl services/replay-service spring-boot:run'
	@$(MAKE) --no-print-directory _start-bg NAME=web CMD='pnpm --filter @tennisly/web dev'
	@echo ""
	@echo "stack starting — first JVM boot is slow (~1–2 min)"
	@echo "  make status   # who's up"
	@echo "  make logs     # follow all app logs"
	@echo "  make health   # probe health endpoints"
	@echo "  make down     # stop everything"

.PHONY: down
down: ## Stop app services and infrastructure
	@for name in web replay match tennis-data eureka; do \
		pid_file="$(PID_DIR)/$$name.pid"; \
		if [ -f "$$pid_file" ]; then \
			pid=$$(cat "$$pid_file"); \
			echo "stopping $$name (pid $$pid)"; \
			kill -- -$$pid 2>/dev/null || kill $$pid 2>/dev/null || true; \
			rm -f "$$pid_file"; \
		fi; \
	done
	@$(MAKE) --no-print-directory infra-down
	@echo "down"

.PHONY: status
status: ## Show which app processes are still running
	@mkdir -p $(PID_DIR)
	@for name in eureka tennis-data match replay web; do \
		pid_file="$(PID_DIR)/$$name.pid"; \
		if [ -f "$$pid_file" ] && kill -0 $$(cat "$$pid_file") 2>/dev/null; then \
			printf "  %-14s running  pid=%s\n" "$$name" "$$(cat $$pid_file)"; \
		else \
			printf "  %-14s stopped\n" "$$name"; \
			rm -f "$$pid_file"; \
		fi; \
	done
	@$(COMPOSE) ps --format 'table {{.Name}}\t{{.Status}}\t{{.Ports}}' 2>/dev/null || $(COMPOSE) ps

.PHONY: logs
logs: ## Tail app service logs (Ctrl-C to stop)
	@mkdir -p $(LOG_DIR)
	@touch $(LOG_DIR)/eureka.log $(LOG_DIR)/tennis-data.log $(LOG_DIR)/match.log $(LOG_DIR)/replay.log $(LOG_DIR)/web.log
	@tail -F $(LOG_DIR)/eureka.log $(LOG_DIR)/tennis-data.log $(LOG_DIR)/match.log $(LOG_DIR)/replay.log $(LOG_DIR)/web.log

# --- single-service foreground (debug) --------------------------------------

.PHONY: eureka
eureka: ## Run the service registry in the foreground (port 8761)
	@$(LOAD_ENV) $(MVNW) -pl services/eureka-server spring-boot:run

.PHONY: tennis-data
tennis-data: check-env ## Run tennis-data-service in the foreground (port 8083)
	@$(LOAD_ENV) $(MVNW) -pl services/tennis-data-service spring-boot:run

.PHONY: match
match: ## Run match-service in the foreground (port 8084)
	@$(LOAD_ENV) $(MVNW) -pl services/match-service spring-boot:run

.PHONY: replay
replay: ## Run replay-service in the foreground (port 8085)
	@$(LOAD_ENV) $(MVNW) -pl services/replay-service spring-boot:run

.PHONY: web
web: ## Run the Next.js app in the foreground (port 3000)
	pnpm --filter @tennisly/web dev

.PHONY: test
test: ## Run the JVM test suites
	@$(LOAD_ENV) $(MVNW) -pl services/tennis-data-service,services/match-service,services/replay-service -am test

.PHONY: health
health: ## Probe every local service health endpoint
	@for pair in "8761:eureka" "8083:tennis-data" "8084:match" "8085:replay" "3000:web"; do \
		port=$${pair%%:*}; name=$${pair##*:}; \
		code=$$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$$port/actuator/health 2>/dev/null \
			|| curl -s -o /dev/null -w "%{http_code}" http://localhost:$$port 2>/dev/null \
			|| echo down); \
		printf "  %-14s :%s  %s\n" "$$name" "$$port" "$$code"; \
	done

# --- internals --------------------------------------------------------------

.PHONY: _start-bg
_start-bg:
	@mkdir -p $(PID_DIR) $(LOG_DIR)
	@if [ -f "$(PID_DIR)/$(NAME).pid" ] && kill -0 $$(cat "$(PID_DIR)/$(NAME).pid") 2>/dev/null; then \
		echo "$(NAME) already running (pid $$(cat $(PID_DIR)/$(NAME).pid))"; \
	else \
		echo "starting $(NAME) → $(LOG_DIR)/$(NAME).log"; \
		$(LOAD_ENV) \
		setsid bash -lc '$(CMD)' > "$(LOG_DIR)/$(NAME).log" 2>&1 & \
		echo $$! > "$(PID_DIR)/$(NAME).pid"; \
	fi

.PHONY: _wait-http
_wait-http:
	@echo "waiting for $(LABEL) on :$(PORT)"
	@for i in $$(seq 1 90); do \
		if curl -sf http://localhost:$(PORT)/actuator/health >/dev/null 2>&1 \
			|| curl -sf http://localhost:$(PORT)/ >/dev/null 2>&1; then \
			echo "$(LABEL) is up"; \
			exit 0; \
		fi; \
		sleep 2; \
	done; \
	echo "$(LABEL) did not become healthy in time — check $(LOG_DIR)/$(LABEL).log"; \
	exit 1
