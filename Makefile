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
	@grep -E '^(POSTGRES_PORT|REDIS_PORT|KAFKA_EXTERNAL_PORT|MINIO_API_PORT|ELASTICSEARCH_PORT|EUREKA_SERVER_PORT|TENNIS_DATA_SERVER_PORT|MATCH_SERVER_PORT|REPLAY_SERVER_PORT|ANALYTICS_SERVER_PORT|NOTIFICATION_SERVER_PORT|WEB_PORT)=' $(PORTS_FILE)

.PHONY: infra-up
infra-up: ports ## Start postgres, redis, kafka, minio and elasticsearch on allocated ports
	$(COMPOSE) --profile infra up -d postgres redis kafka minio elasticsearch
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
	@./scripts/dev-up.sh

.PHONY: ports-print
ports-print: ## Print the currently allocated app URLs
	@$(LOAD_ENV) \
		echo "  web          http://localhost:$$WEB_PORT"; \
		echo "  eureka       http://localhost:$$EUREKA_SERVER_PORT"; \
		echo "  tennis-data  http://localhost:$$TENNIS_DATA_SERVER_PORT"; \
		echo "  match        http://localhost:$$MATCH_SERVER_PORT"; \
		echo "  replay       http://localhost:$$REPLAY_SERVER_PORT"; \
		echo "  analytics    http://localhost:$$ANALYTICS_SERVER_PORT"; \
		echo "  notification  http://localhost:$$NOTIFICATION_SERVER_PORT"; \
		echo "  elasticsearch http://localhost:$$ELASTICSEARCH_PORT"; \
		echo "  postgres     localhost:$$POSTGRES_PORT"; \
		echo "  redis        localhost:$$REDIS_PORT"; \
		echo "  kafka        localhost:$$KAFKA_EXTERNAL_PORT"; \
		echo "  minio        http://localhost:$$MINIO_API_PORT";

.PHONY: down
down: ## Stop app services and infrastructure
	@./scripts/dev-down.sh

.PHONY: status
status: ## Show which app processes are still running
	@mkdir -p $(PID_DIR)
	@for name in eureka tennis-data match replay analytics notification web; do \
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
	@touch $(LOG_DIR)/eureka.log $(LOG_DIR)/tennis-data.log $(LOG_DIR)/match.log $(LOG_DIR)/replay.log $(LOG_DIR)/analytics.log $(LOG_DIR)/notification.log $(LOG_DIR)/web.log
	@tail -F $(LOG_DIR)/eureka.log $(LOG_DIR)/tennis-data.log $(LOG_DIR)/match.log $(LOG_DIR)/replay.log $(LOG_DIR)/analytics.log $(LOG_DIR)/notification.log $(LOG_DIR)/web.log

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

.PHONY: analytics
analytics: ports ## Run analytics-service in the foreground
	@$(LOAD_ENV) SERVER_PORT=$$ANALYTICS_SERVER_PORT $(MVNW) -pl services/analytics-service spring-boot:run

.PHONY: notification
notification: ports ## Run notification-service in the foreground
	@$(LOAD_ENV) SERVER_PORT=$$NOTIFICATION_SERVER_PORT $(MVNW) -pl services/notification-service spring-boot:run

.PHONY: web
web: ports ## Run the Next.js app in the foreground
	@$(LOAD_ENV) pnpm --filter @tennisly/web dev

.PHONY: test
test: ## Run the JVM test suites
	@$(LOAD_ENV) $(MVNW) -pl services/tennisly-common,services/api-gateway,services/user-service,services/tennis-data-service,services/match-service,services/replay-service,services/analytics-service,services/notification-service -am test

.PHONY: test-it
test-it: ## Run Testcontainers ITs (Docker required)
	@$(LOAD_ENV) $(MVNW) -pl services/user-service,services/notification-service -am test -Dtest=PublicWebhookApiIT,WebhookDeliveryWorkerIT -Dsurefire.failIfNoSpecifiedTests=false

.PHONY: e2e
e2e: ## Playwright smoke (production next start; never turbopack/dev)
	pnpm --filter @tennisly/web test:e2e

.PHONY: test-coverage
test-coverage: ## Run tests and write Jacoco HTML reports
	@$(LOAD_ENV) $(MVNW) test jacoco:report -pl services/tennisly-common,services/api-gateway,services/user-service,services/notification-service -am

.PHONY: load-smoke
load-smoke: ## k6 smoke against public API (needs BASE_URL + API_KEY)
	@test -n "$$API_KEY" || { echo "set API_KEY=tly_live_..."; exit 1; }
	k6 run -e BASE_URL=$${BASE_URL:-http://localhost:8080} -e API_KEY=$$API_KEY tests/load/public-api-smoke.js

.PHONY: zap-api
zap-api: ## OWASP ZAP api-scan of /api/v1 (needs Docker + API_KEY + running gateway)
	@test -n "$$API_KEY" || { echo "set API_KEY=tly_live_... (disposable key)"; exit 1; }
	@./scripts/zap-api-scan.sh

.PHONY: zap-rules-gen
zap-rules-gen: ## Write a full ZAP api-scan rules template into .run/zap/
	@./scripts/zap-api-scan.sh --gen-rules

.PHONY: test-pact
test-pact: ## Generate consumer pacts then verify tennis-data provider
	@$(LOAD_ENV) $(MVNW) -pl services/contract-tests -am test -Dtest=ApiGatewayPlayersPactTest -Dsurefire.failIfNoSpecifiedTests=false
	@$(LOAD_ENV) $(MVNW) -pl services/tennis-data-service -am test -Dtest=PlayerControllerProviderPactTest -Dsurefire.failIfNoSpecifiedTests=false -Dpact_do_not_track=true

.PHONY: health
health: ## Probe every local service health endpoint using allocated ports
	@$(LOAD_ENV) \
	for pair in "$$EUREKA_SERVER_PORT:eureka" "$$TENNIS_DATA_SERVER_PORT:tennis-data" "$$MATCH_SERVER_PORT:match" "$$REPLAY_SERVER_PORT:replay" "$$ANALYTICS_SERVER_PORT:analytics" "$$NOTIFICATION_SERVER_PORT:notification" "$$ELASTICSEARCH_PORT:elasticsearch" "$$WEB_PORT:web"; do \
		port=$${pair%%:*}; name=$${pair##*:}; \
		if [ "$$name" = "elasticsearch" ]; then \
			code=$$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$$port/_cluster/health 2>/dev/null || echo down); \
		else \
			code=$$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$$port/actuator/health 2>/dev/null \
				|| curl -s -o /dev/null -w "%{http_code}" http://localhost:$$port 2>/dev/null \
				|| echo down); \
		fi; \
		printf "  %-14s :%s  %s\n" "$$name" "$$port" "$$code"; \
	done
