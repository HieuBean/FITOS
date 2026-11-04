import fs from 'node:fs';
import path from 'node:path';
import { describe, it } from 'node:test';
import assert from 'node:assert';

describe('Layer 5 OPS & DevOps Comprehensive Unit Test Suite', () => {
  const rootDir = path.resolve('..');

  describe('ai-crm/Dockerfile Tests', () => {
    const dockerfilePath = path.join(rootDir, 'ai-crm', 'Dockerfile');

    it('Dockerfile must exist and not be empty', () => {
      assert.ok(fs.existsSync(dockerfilePath), 'ai-crm/Dockerfile should exist');
      const content = fs.readFileSync(dockerfilePath, 'utf8');
      assert.ok(content.length > 50, 'Dockerfile should have substantial content');
    });

    it('Dockerfile should use official slim python base image', () => {
      const content = fs.readFileSync(dockerfilePath, 'utf8');
      assert.match(content, /FROM\s+python:3\.\d+(-slim)?/i, 'Should use python 3.x slim base image');
    });

    it('Dockerfile should expose port 8001 and have healthcheck', () => {
      const content = fs.readFileSync(dockerfilePath, 'utf8');
      assert.match(content, /EXPOSE\s+8001/, 'Should expose port 8001');
      assert.match(content, /HEALTHCHECK/i, 'Should define HEALTHCHECK');
      assert.match(content, /http:\/\/localhost:8001\/health/, 'Healthcheck must target /health');
    });

    it('Dockerfile should execute uvicorn on port 8001', () => {
      const content = fs.readFileSync(dockerfilePath, 'utf8');
      assert.match(content, /CMD\s*\[.*"uvicorn".*"8001".*\]/s, 'CMD must run uvicorn on port 8001');
    });
  });

  describe('.env.example Completeness Tests', () => {
    const envPath = path.join(rootDir, '.env.example');

    it('.env.example must exist', () => {
      assert.ok(fs.existsSync(envPath), '.env.example must exist');
    });

    it('.env.example should contain all required database variables', () => {
      const content = fs.readFileSync(envPath, 'utf8');
      assert.match(content, /DB_HOST=/, 'Should define DB_HOST');
      assert.match(content, /DB_PORT=5432/, 'Should define standard DB_PORT');
      assert.match(content, /DB_NAME=/, 'Should define DB_NAME');
      assert.match(content, /SPRING_DATASOURCE_URL=/, 'Should define SPRING_DATASOURCE_URL');
    });

    it('.env.example should contain JWT Secret and Redis variables', () => {
      const content = fs.readFileSync(envPath, 'utf8');
      assert.match(content, /JWT_SECRET=/, 'Should define JWT_SECRET');
      assert.match(content, /REDIS_HOST=/, 'Should define REDIS_HOST');
      assert.match(content, /REDIS_PORT=6379/, 'Should define REDIS_PORT');
    });

    it('.env.example should define all 4 AI microservice gateways and API keys', () => {
      const content = fs.readFileSync(envPath, 'utf8');
      assert.match(content, /AI_CRM_URL=http:\/\/localhost:8001/, 'Should define AI_CRM_URL on 8001');
      assert.match(content, /AI_CRM_API_KEY=/, 'Should define AI_CRM_API_KEY');
      assert.match(content, /AI_COMMERCE_URL=http:\/\/localhost:8002/, 'Should define AI_COMMERCE_URL on 8002');
      assert.match(content, /AI_OPERATIONS_URL=http:\/\/localhost:8003/, 'Should define AI_OPERATIONS_URL on 8003');
      assert.match(content, /AI_WORKOUT_URL=http:\/\/localhost:8004/, 'Should define AI_WORKOUT_URL on 8004');
    });
  });

  describe('GitHub Actions CI Workflows Tests', () => {
    const ciBackendPath = path.join(rootDir, '.github', 'workflows', 'ci-backend.yml');
    const ciAiCrmPath = path.join(rootDir, '.github', 'workflows', 'ci-ai-crm.yml');

    it('.github/workflows/ci-backend.yml must be valid and test backend with postgres service', () => {
      assert.ok(fs.existsSync(ciBackendPath), 'ci-backend.yml must exist');
      const content = fs.readFileSync(ciBackendPath, 'utf8');
      assert.match(content, /name:\s*Backend CI/i);
      assert.match(content, /services:\s*postgres:/s, 'Must declare postgres container service');
      assert.match(content, /java-version:\s*'17'/, 'Must use JDK 17');
      assert.match(content, /mvn\s+clean\s+test/, 'Must run maven test command');
    });

    it('.github/workflows/ci-ai-crm.yml must be valid and run pytest on Python 3.11', () => {
      assert.ok(fs.existsSync(ciAiCrmPath), 'ci-ai-crm.yml must exist');
      const content = fs.readFileSync(ciAiCrmPath, 'utf8');
      assert.match(content, /name:\s*AI CRM CI/i);
      assert.match(content, /python-version:\s*'3\.11'/, 'Must use Python 3.11');
      assert.match(content, /pytest\s+tests/, 'Must run pytest suite');
      assert.match(content, /AI_CRM_API_KEY:\s*crm-secret-ai-token-2026/, 'Must inject AI_CRM_API_KEY');
    });
  });

  describe('Smoke Test & Runbook Verification', () => {
    const runbookPath = path.join(rootDir, 'deploy', 'DEPLOY_RUNBOOK.md');

    it('DEPLOY_RUNBOOK.md must exist and contain post-deployment smoke test curl script', () => {
      assert.ok(fs.existsSync(runbookPath), 'DEPLOY_RUNBOOK.md should exist');
      const content = fs.readFileSync(runbookPath, 'utf8');
      assert.match(content, /curl.*\/health/, 'Must include curl health check');
      assert.match(content, /curl.*\/score-lead/, 'Must include curl score-lead check');
      assert.match(content, /curl.*\/api\/exercises/, 'Must include backend exercise check');
    });
  });
});

