#!/usr/bin/env bash

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVA_HOME_CANDIDATES=(
  "${JAVA_HOME:-}"
  "/opt/homebrew/opt/openjdk@17"
  "/opt/homebrew/opt/openjdk"
  "/usr/local/opt/openjdk@17"
  "/usr/local/opt/openjdk"
)

for candidate in "${JAVA_HOME_CANDIDATES[@]}"; do
  if [[ -n "${candidate}" && -x "${candidate}/bin/java" ]]; then
    export JAVA_HOME="${candidate}"
    export PATH="${JAVA_HOME}/bin:${PATH}"
    break
  fi
done

if ! command -v java >/dev/null 2>&1; then
  echo "Java 17 not found. Install OpenJDK 17 or set JAVA_HOME." >&2
  exit 1
fi

cd "${PROJECT_DIR}"
mvn -q -DskipTests package dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
java -cp "target/classes:$(cat target/classpath.txt)" org.example.Main "$@"