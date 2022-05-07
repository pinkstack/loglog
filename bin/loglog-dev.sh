#!/usr/bin/env bash
set -e

if [[ -z "${LOGLOG_HOME}" ]]; then
  echo "LOGLOG_HOME environment variable is not set!" && exit 255
fi

cd "$LOGLOG_HOME" && \
  docker-compose \
    -f .docker/docker-compose.yml \
    --project-name loglog \
    --project-directory . $@
