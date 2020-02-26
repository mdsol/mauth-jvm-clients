#!/usr/bin/env bash

set -u -f -e

# Determine if `scalafmt` command is available and exit if not.
set +e
hash scalafmt
SCALAFMT_CHECK=$?
set -e

if [ "${SCALAFMT_CHECK}" -ne 0 ]; then
  echo "Required command 'scalafmt' not found. Please install."
  echo "See http://scalameta.org/scalafmt/"
  exit 1
fi

# Determine OpenWhisk base directory
ROOT_DIR="$(git rev-parse --show-toplevel)"

# Run `scalafmt` iff there are staged .scala source files
set +e
STAGED_SCALA_FILES=$(git diff --cached --name-only --no-color --diff-filter=d --exit-code -- "${ROOT_DIR}/*.scala" "${ROOT_DIR}/*.sbt")
STAGED_SCALA_FILES_DETECTED=$?
set -e

if [ "${STAGED_SCALA_FILES_DETECTED}" -eq 1 ]; then
    # Re-format and re-add all staged .scala files
    for SCALA_FILE in ${STAGED_SCALA_FILES}
    do
      scalafmt --config "${ROOT_DIR}/.scalafmt.conf" "${ROOT_DIR}/${SCALA_FILE}"
      git add -- "${ROOT_DIR}/${SCALA_FILE}"
    done
fi

exit 0
