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

# The DB password is generated once and kept in .env (not committed).
# Docker Compose reads .env automatically, so no export is needed.
# It must stay the same while the DB data exists, so it is not regenerated.
if [ ! -f .env ]; then
  echo "POSTGRES_PASSWORD=$(openssl rand -hex 16)" > .env
  echo "Created onboarding-progress-system/.env with a random DB password"
fi

docker compose -f docker-compose.yml -f docker-compose.codespaces.yml up -d --build

echo
echo "The app is starting. Open port 8080 from the Ports tab in a minute."
echo "Logs: cd onboarding-progress-system && docker compose logs -f app"
