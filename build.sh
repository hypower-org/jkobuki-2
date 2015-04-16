#!/bin/bash

# Make build dir if not existing
mkdir -p build/

# Target JVM 7, place class files in build/, all .java located in src/
javac -cp libs/jssc-2.8.0.jar -d build/ -sourcepath src/ src/edu/ycp/robotics/*.java

