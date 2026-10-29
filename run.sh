#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

echo "============================================================"
echo "  VoteSecure - Online Voting System"
echo "============================================================"

if ! command -v java >/dev/null 2>&1; then
    echo "[ERROR] Java was not found. Install Java 17 or newer and try again."
    exit 1
fi

if [ ! -f "target/VoteSecure.jar" ]; then
    if ! command -v mvn >/dev/null 2>&1; then
        echo "[ERROR] The app is not built and Maven is not installed."
        echo "Install Maven, then run: mvn -DskipTests package"
        exit 1
    fi
    echo "First launch detected. Building the application..."
    mvn -q -DskipTests package
fi

echo "Starting VoteSecure..."
java -jar target/VoteSecure.jar
