#!/bin/sh
# Blocks secret and signing files from entering a commit or a push, even when staged with git add -f.
# Reads file names from stdin, one per line.
bad=$(grep -E '(^|/)(release-signing/|secrets/|key[-_]text[^/]*\.txt$|key-backup[^/]*\.txt$|secret[^/]*\.txt$|[^/]*\.b64$|keystore\.properties$|\.env($|\.)|.*\.(jks|keystore|p12|pfx|pem|key|idsig)$)' || true)
if [ -n "$bad" ]; then
  echo "BLOCKED: secret or signing files must never be committed:"
  echo "$bad"
  exit 1
fi
