#!/bin/bash

# If no build dir, build
if [ ! -d "build/" ]; then
    echo "Building..."
    ./build.sh
fi 

# Change dir to build dir
cd build/

# Run, and include jssc on classpath
java -cp ../libs/jssc-2.8.0.jar:. edu.ycp.robotics.KobukiRobot

cd ..

