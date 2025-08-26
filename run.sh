#!/bin/bash
# SafeRoom V2 Run Script

echo "SafeRoom V2 - Starting Application..."
echo "======================================="

# Build if needed
echo "Building project..."
./gradlew build

if [ $? -eq 0 ]; then
    echo "Build successful. Starting application..."
    echo "======================================="
    ./gradlew run
else
    echo "Build failed. Please check the errors above."
    exit 1
fi
