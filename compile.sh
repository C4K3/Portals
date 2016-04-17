#!/usr/bin/env bash
RT_JAR=/usr/lib/jvm/java-8-openjdk/jre/lib/rt.jar
INCLUDES_DIR=../
BUKKIT_TARGET=bukkit-1.8.8B1.jar
DST_DIR=.

javac -Xlint:all -bootclasspath "$RT_JAR:$INCLUDES_DIR/$BUKKIT_TARGET" -d ./ src/*
jar -cfe $DST_DIR/Portals.jar net/simpvp/Portals/Portals ./*
rm -rf net/
