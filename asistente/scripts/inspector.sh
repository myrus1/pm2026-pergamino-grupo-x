#!/bin/sh
# Abre el MCP Inspector conectado al /mcp del asistente, con tu token.
# Uso: PM2026_TOKEN=<accessToken> sh asistente/scripts/inspector.sh [url]
# La url por defecto es la local; para Railway pasá https://<dominio>/mcp.
set -eu
URL="${1:-http://localhost:8000/mcp}"
: "${PM2026_TOKEN:?Exportá PM2026_TOKEN con el accessToken de POST /api/auth/token}"
TOKEN="${PM2026_TOKEN#Bearer }"

umask 077
DIR="$(mktemp -d)"
trap 'rm -rf "$DIR"' EXIT INT TERM
printf '{"mcpServers":{"pm2026":{"type":"streamable-http","url":"%s","headers":{"Authorization":"Bearer %s"}}}}\n' \
  "$URL" "$TOKEN" > "$DIR/inspector.json"

npx -y @modelcontextprotocol/inspector@2.13.0 --config "$DIR/inspector.json"
