#!/bin/sh

VERSION=blobee-1.2

(cd target 
jar  cvf ${VERSION}-bundle.jar  \
 ${VERSION}.pom \
 ${VERSION}.pom.asc \
 ${VERSION}.jar \
 ${VERSION}.jar.asc \
 ${VERSION}-javadoc.jar \
 ${VERSION}-javadoc.jar.asc \
 ${VERSION}-sources.jar \
 ${VERSION}-sources.jar.asc )
