#!/bin/sh

VERSION=blobee-0.1

(cd target 
jar  cvf bundle.jar  \
 ${VERSION}.pom \
 ${VERSION}.pom.asc \
 ${VERSION}.jar \
 ${VERSION}.jar.asc \
 ${VERSION}-javadoc.jar \
 ${VERSION}-javadoc.jar.asc \
 ${VERSION}-sources.jar \
 ${VERSION}-sources.jar.asc )
