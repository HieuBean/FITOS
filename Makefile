# ==============================================================================
# FITOS Ecosystem - Core Automation Makefile
# Task #142 - Person D (Layer 5: DevOps)
# ==============================================================================

.PHONY: help up down restart build logs ps migrate seed test-ai test-be test-postgres test-all clean

# Default shell
SHELL := /bin/bash

help: ## Hiển thị hướng dẫn các lệnh khả dụng
	@echo "========================================================================"
	@echo "  FITOS FITNESS OPERATING SYSTEM - DEVOPS COMMAND SUITE"
	@echo "========================================================================"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'
	@echo "========================================================================"

up: ## Khởi động toàn bộ hệ thống bằng Docker Compose ở chế độ nền (-d)
	docker compose up -d

down: ## Dừng và gỡ bỏ toàn bộ containers, networks của hệ thống
	docker compose down

restart: ## Khởi động lại toàn bộ containers
	docker compose restart

build: ## Đóng gói và build lại toàn bộ Docker images (Backend, Frontend, AI Services)
	docker compose build

ps: ## Xem trạng thái các containers đang hoạt động
	docker compose ps

logs: ## Xem stream logs tổng hợp thời gian thực của toàn bộ hệ sinh thái
	docker compose logs -f

logs-ai: ## Xem stream logs riêng của microservice AI Workout (port 8004)
	docker compose logs -f ai-workout

migrate: ## Chạy Flyway migrations trên Spring Boot Backend
	docker compose up -d --wait postgres backend

seed: ## Nạp dữ liệu mẫu ban đầu (Initial seed data) vào PostgreSQL
	@echo "Exercise seed is applied by Flyway V4_1. Verifying it..."
	@test $$(docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U fitos_user -d fitos -tAc "SELECT count(*) FROM exercises") -ge 30

test-ai: ## Chạy bộ kiểm thử tự động pytest (22 test cases) của ai-workout
	@echo "Executing Pytest test suite for ai-workout..."
	cd ai-workout && python -m pytest tests/ -v

test-be: ## Chạy bộ kiểm thử tự động JUnit 5 của Spring Boot Backend
	@echo "Executing Maven test suite for backend..."
	cd backend && mvn test -B

test-postgres: ## Kiểm thử luồng booking/check-in/PT trên database fitos_test
	@test -n "$$FITOS_TEST_DATABASE_URL" || (echo "Set FITOS_TEST_DATABASE_URL to a PostgreSQL database ending in _test"; exit 1)
	cd backend && mvn -B -Dtest=PostgresWorkflowIntegrationTest test

test-all: test-ai test-be ## Chạy toàn bộ test suites của cả Backend và AI Service

clean: ## Dọn dẹp cache tạm (.pytest_cache, target, .next, dangling docker containers)
	@echo "Cleaning up temporary files and caches..."
	rm -rf ai-workout/.pytest_cache ai-workout/__pycache__ ai-workout/tests/__pycache__
	rm -rf backend/target
	@echo "Clean completed."
