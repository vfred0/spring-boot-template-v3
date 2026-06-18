#!/bin/sh
sed \
  -e "s|\${GOOGLE_CLIENT_ID}|$GOOGLE_CLIENT_ID|g" \
  -e "s|\${GOOGLE_CLIENT_SECRET}|$GOOGLE_CLIENT_SECRET|g" \
  /tmp/realm-export.template.json > /import/realm-export.json
