#!/bin/sh

vault server -dev -dev-listen-address=0.0.0.0:8200 &
VAULT_PID=$!

until vault status > /dev/null 2>&1; do
  sleep 1
done

vault kv put secret/forex/one-frame token="${ONE_FRAME_TOKEN}"
vault kv put secret/forex/redis token="${REDIS_TOKEN}"

wait $VAULT_PID
