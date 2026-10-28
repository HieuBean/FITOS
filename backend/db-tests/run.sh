#!/usr/bin/env bash
# Kiểm tra migration V2 (Người B) trên một PostgreSQL tạm thời.
# Cách chạy (từ thư mục FITOS):   bash backend/db-tests/run.sh
# Yêu cầu: Docker Desktop đang chạy.
set -euo pipefail

cd "$(dirname "$0")/../.."          # về thư mục gốc FITOS
MIG=backend/src/main/resources/db/migration
TESTS=backend/db-tests
NAME=fitos-db-test

cleanup() { docker rm -f "$NAME" >/dev/null 2>&1 || true; }
trap cleanup EXIT

echo "▶ Khởi động PostgreSQL tạm thời ($NAME)..."
cleanup
docker run -d --name "$NAME" -e POSTGRES_PASSWORD=test -e POSTGRES_DB=fitos postgres:16-alpine >/dev/null
for _ in $(seq 1 30); do
  docker exec "$NAME" pg_isready -U postgres -d fitos >/dev/null 2>&1 && break
  sleep 1
done
sleep 2

PSQL="docker exec -i $NAME psql -q -U postgres -d fitos"

# Nếu đã có V1 thật của Người A thì dùng, không thì dùng stub
if ls $MIG/V1*.sql >/dev/null 2>&1; then
  echo "▶ Dùng V1 thật của Người A"
  V1_FILES=$(ls $MIG/V1*.sql | sort -V)
else
  echo "▶ Chưa có V1 → dùng stub users/customers"
  V1_FILES=$TESTS/00_v1_stub.sql
fi

echo "▶ Chạy migration V1 + V2..."
# shellcheck disable=SC2086
cat $V1_FILES $(ls $MIG/V2*.sql | sort -V) | $PSQL -v ON_ERROR_STOP=1
echo "✅ Migration chạy thành công"

# Stub chỉ chèn sẵn 1 customer; với V1 thật cần đảm bảo có customer id=1
if [ "$V1_FILES" != "$TESTS/00_v1_stub.sql" ]; then
  echo "⚠ Đang dùng V1 thật: test giả định đã có customers.id = 1"
fi

echo "▶ Chạy test ràng buộc..."
$PSQL < $TESTS/10_v2_constraint_tests.sql 2>&1 | sed 's/^psql:<stdin>:[0-9]*: //; s/^NOTICE:  //'
