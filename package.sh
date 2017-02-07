#!/bin/bash

if [ -z "$1" ];
then
    echo "pulling down latest version on current branch..."
    git pull
else
    echo "checking out $1..."
    git checkout $1
fi

if [ -z "$JAVA_HOME" ]; then
  JAVA_HOME=/usr/local/jdk
fi

if [ -z "$M2_HOME" ]; then
  M2_HOME=/usr/local/maven
fi

export PATH=$M2_HOME/bin:$JAVA_HOME/bin:$PATH
mvn -Dmaven.test.skip=true -Dautoconfig.skip clean package -U
