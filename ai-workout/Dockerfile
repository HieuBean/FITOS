# ==============================================================================
# FITOS AI Workout Coach Microservice Dockerfile
# Task #141 - Person D (Layer 5: DevOps)
# Port: 8004
# ==============================================================================

FROM python:3.11-slim AS runner

WORKDIR /app

# Prevent Python from writing .pyc files & enable unbuffered standard IO
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PORT=8004

# Security: Create non-root application user
RUN useradd -m -u 1000 appuser

# Install Python dependencies in cached layer
COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir -r requirements.txt

# Copy microservice source code
COPY . .

# Set permissions for non-root user
RUN chown -R appuser:appuser /app

USER appuser

# Expose internal microservice port
EXPOSE 8004

# Container healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8004/health')" || exit 1

# Launch FastAPI Uvicorn ASGI server
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8004"]
