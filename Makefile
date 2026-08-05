SHELL := /bin/bash
.DEFAULT_GOAL := help

ENV_FILE := infrastructure/docker/.env
PORTS_FILE := .run/ports.env
COMPOSE_FILE := infrastructure/docker/docker-compose.yml
COMPOSE = docker compose -f $(COMPOSE_FILE) --env-file $(ENV_FILE) $(if $(wildcard $(PORTS_FILE)),--env-file $(PORTS_FILE),)
MVNW := ./mvnw
RUN_DIR := .run
PID_DIR := $(RUN_DIR)/pids
LOG_DIR := $(RUN_DIR)/logs

# Spring Boot 3.2 does not support the JDK 25 that ships as this machine's default.
JAVA_HOME ?= $(shell /usr/libexec/java_home -v 21 2>/dev/null)

# Secrets from .env, then dynamic ports from .run/ports.env (ports win on clash).
LOAD_ENV = set -a; \
	[ -f $(ENV_FILE) ] && source $(ENV_FILE); \
	[ -f $(PORTS_FILE) ] && source $(PORTS_FILE); \
	set +a; \
	export JAVA_HOME=$(JAVA_HOME); \
	export PATH=$$JAVA_HOME/bin:$$PATH;

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

.PHONY: ports
ports: ## Allocate free host ports into .run/ports.env
	@./scripts/allocate-ports.sh
	@echo "active ports:"
	@grep -E '^(POSTGRES_PORT|REDIS_PORT|KAFKA_EXTERNAL_PORT|MINIO_API_PORT|EUREKA_SERVER_PORT|TENNIS_DATA_SERVER_PORT|MATCH_SERVER_PORT|REPLAY_SERVER_PORT|WEB_PORT)=' $(PORTS_FILE)

.PHONY: infra-up
infra-up: ports ## Start postgres, redis, kafka and minio on allocated ports
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
tools-up: ports ## Start pgadmin, redis-commander and kafka-ui
	$(COMPOSE) --profile tools up -d

# --- one-command boot -------------------------------------------------------

.PHONY: up
up: check-env ports ## Start infra + every app service in the background
	@mkdir -p $(PID_DIR) $(LOG_DIR)
	$(COMPOSE) --profile infra up -d postgres redis kafka minio
	@echo "waiting for postgres/redis/kafka"
	@$(COMPOSE) up -d --wait postgres redis kafka 2>/dev/null || true
	@$(LOAD_ENV) \
		$(MAKE) --no-print-directory _start-bg NAME=eureka \
			CMD='SERVER_PORT=$$EUREKA_SERVER_PORT $(MVNW) -pl services/eureka-server spring-boot:run'; \
		$(MAKE) --no-print-directory _wait-http PORT=$$EUREKA_SERVER_PORT LABEL=eureka; \
		$(MAKE) --no-print-directory _start-bg NAME=tennis-data \
			CMD='SERVER_PORT=$$TENNIS_DATA_SERVER_PORT $(MVNW) -pl services/tennis-data-service spring-boot:run'; \
		$(MAKE) --no-print-directory _wait-http PORT=$$TENNIS_DATA_SERVER_PORT LABEL=tennis-data; \
		$(MAKE) --no-print-directory _start-bg NAME=match \
			CMD='SERVER_PORT=$$MATCH_SERVER_PORT $(MVNW) -pl services/match-service spring-boot:run'; \
		$(MAKE) --no-print-directory _start-bg NAME=replay \
			CMD='SERVER_PORT=$$REPLAY_SERVER_PORT $(MVNW) -pl services/replay-service spring-boot:run'; \
		$(MAKE) --no-print-directory _start-bg NAME=web \
			CMD='pnpm --filter @tennisly/web dev'
	@echo ""
	@echo "stack starting — first JVM boot is slow (~1–2 min)"
	@$(MAKE) --no-print-directory ports-print
	@echo "  make status   # who's up"
	@echo "  make logs     # follow all app logs"
	@echo "  make health   # probe health endpoints"
	@echo "  make down     # stop everything"

.PHONY: ports-print
ports-print: ## Print the currently allocated app URLs
	@$(LOAD_ENV) \
		echo "  web          http://localhost:$$WEB_PORT"; \
		echo "  eureka       http://localhost:$$EUREKA_SERVER_PORT"; \
		echo "  tennis-data  http://localhost:$$TENNIS_DATA_SERVER_PORT"; \
		echo "  match        http://localhost:$$MATCH_SERVER_PORT"; \
		echo "  replay       http://localhost:$$REPLAY_SERVER_PORT"; \
		echo "  postgres     localhost:$$POSTGRES_PORT"; \
		echo "  redis        localhost:$$REDIS_PORT"; \
		echo "  kafka        localhost:$$KAFKA_EXTERNAL_PORT"; \
		echo "  minio        http://localhost:$$MINIO_API_PORT";

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
eureka: ports ## Run eureka in the foreground
	@$(LOAD_ENV) SERVER_PORT=$$EUREKA_SERVER_PORT $(MVNW) -pl services/eureka-server spring-boot:run

.PHONY: tennis-data
tennis-data: check-env ports ## Run tennis-data-service in the foreground
	@$(LOAD_ENV) SERVER_PORT=$$TENNIS_DATA_SERVER_PORT $(MVNW) -pl services/tennis-data-service spring-boot:run

.PHONY: match
match: ports ## Run match-service in the foreground
	@$(LOAD_ENV) SERVER_PORT=$$MATCH_SERVER_PORT $(MVNW) -pl services/match-service spring-boot:run

.PHONY: replay
replay: ports ## Run replay-service in the foreground
	@$(LOAD_ENV) SERVER_PORT=$$REPLAY_SERVER_PORT $(MVNW) -pl services/replay-service spring-boot:run

.PHONY: web
web: ports ## Run the Next.js app in the foreground
	@$(LOAD_ENV) pnpm --filter @tennisly/web dev

.PHONY: test
test: ## Run the JVM test suites
	@$(LOAD_ENV) $(MVNW) -pl services/tennis-data-service,services/match-service,services/replay-service -am test

.PHONY: health
health: ## Probe every local service health endpoint using allocated ports
	@$(LOAD_ENV) \
	for pair in "$$EUREKA_SERVER_PORT:eureka" "$$TENNIS_DATA_SERVER_PORT:tennis-data" "$$MATCH_SERVER_PORT:match" "$$REPLAY_SERVER_PORT:replay" "$$WEB_PORT:web"; do \
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
