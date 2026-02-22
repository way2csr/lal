#!/bin/bash
JAR_PATH="target/lal-0.0.2-SNAPSHOT.jar"
if [ -f "$JAR_PATH" ]; then
    echo "Starting LingoLearn..."
    java -jar "$JAR_PATH"
else
    echo "Error: JAR file not found at $JAR_PATH"
    echo "Please run 'mvn package' first to build the application."
fi
