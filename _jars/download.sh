#!/bin/bash
# Download minimal jars to compile & run JsonToTsGenerator standalone tests
set -e

MVN_BASE="https://repo1.maven.org/maven2"

# Jackson (for JsonParser)
wget -q -c "$MVN_BASE/com/fasterxml/jackson/core/jackson-core/2.17.2/jackson-core-2.17.2.jar"
wget -q -c "$MVN_BASE/com/fasterxml/jackson/core/jackson-databind/2.17.2/jackson-databind-2.17.2.jar"
wget -q -c "$MVN_BASE/com/fasterxml/jackson/core/jackson-annotations/2.17.2/jackson-annotations-2.17.2.jar"

# Kotlin stdlib (for running kotlin classes)
wget -q -c "$MVN_BASE/org/jetbrains/kotlin/kotlin-stdlib/2.1.0/kotlin-stdlib-2.1.0.jar"
wget -q -c "$MVN_BASE/org/jetbrains/kotlin/kotlin-stdlib-jdk8/2.1.0/kotlin-stdlib-jdk8-2.1.0.jar"
wget -q -c "$MVN_BASE/org/jetbrains/kotlin/kotlin-stdlib-jdk7/2.1.0/kotlin-stdlib-jdk7-2.1.0.jar"

# JUnit Jupiter (tests) + JUnit Platform Console (run tests standalone)
wget -q -c "$MVN_BASE/org/junit/jupiter/junit-jupiter-api/5.11.4/junit-jupiter-api-5.11.4.jar"
wget -q -c "$MVN_BASE/org/junit/jupiter/junit-jupiter-engine/5.11.4/junit-jupiter-engine-5.11.4.jar"
wget -q -c "$MVN_BASE/org/junit/jupiter/junit-jupiter-params/5.11.4/junit-jupiter-params-5.11.4.jar"
wget -q -c "$MVN_BASE/org/junit/platform/junit-platform-commons/1.11.4/junit-platform-commons-1.11.4.jar"
wget -q -c "$MVN_BASE/org/junit/platform/junit-platform-engine/1.11.4/junit-platform-engine-1.11.4.jar"
wget -q -c "$MVN_BASE/org/junit/platform/junit-platform-launcher/1.11.4/junit-platform-launcher-1.11.4.jar"
wget -q -c "$MVN_BASE/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar"

# Opentest4j (assertion failure types)
wget -q -c "$MVN_BASE/org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar"

# Apiguardian API (required by junit-jupiter-api)
wget -q -c "$MVN_BASE/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar"

echo "All jars downloaded."
