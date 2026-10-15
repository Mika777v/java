#!/usr/bin/env bash
# Starts the DB and the app with Docker Compose in GitHub Codespaces.
# Run automatically when the codespace starts; can also be run by hand:
#   bash .devcontainer/start-app.sh
set -euo pipefail

cd "$(dirname "$0")/../onboarding-progress-system"

# Docker may still be starting right after the codespace boots
for _ in $(seq 1 60); do
  docker info >/dev/null 2>&1 && break
  sleep 2
done

# Passwords are generated once and kept in .env (not committed).
# Docker Compose reads .env automatically, so no export is needed.
# It must stay the same while the DB data exists, so it is not regenerated.
touch .env
if ! grep -q '^POSTGRES_PASSWORD=' .env; then
  echo "POSTGRES_PASSWORD=$(openssl rand -hex 16)" >> .env
  echo "Created a random DB password in onboarding-progress-system/.env"
fi
# Password of the first administrator (admin@example.com), also kept in .env
if ! grep -q '^APP_ADMIN_PASSWORD=' .env; then
  echo "APP_ADMIN_PASSWORD=$(openssl rand -hex 8)" >> .env
  echo "Created a random administrator password in onboarding-progress-system/.env"
fi

docker compose -f docker-compose.yml -f docker-compose.codespaces.yml up -d --build

# Wait until the app answers, so that "ready" really means ready (up to 5 minutes)
echo "Waiting for the app to start..."
for i in $(seq 1 60); do
  if curl -sf -o /dev/null http://localhost:8080/; then
    echo
    echo "Ready: the app is running on port 8080."
    echo "The browser opens automatically; otherwise open port 8080 from the Ports tab."
    echo
    admin_email=$(grep '^APP_ADMIN_EMAIL=' .env | cut -d= -f2- || true)
    echo "Log in as administrator:"
    echo "  email:    ${admin_email:-admin@example.com}"
    echo "  password: $(grep '^APP_ADMIN_PASSWORD=' .env | cut -d= -f2-)"
    echo "(shown again with: cat onboarding-progress-system/.env)"
    exit 0
  fi
  sleep 5
done

echo
echo "The app did not answer within 5 minutes. Check the logs:"
echo "  cd onboarding-progress-system && docker compose logs --tail 50 app"
exit 1
