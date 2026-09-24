#!/bin/bash

set -e

cd "$(dirname "$0")"

echo "Starting Book Sharing App..."

mvn spring-boot:run
