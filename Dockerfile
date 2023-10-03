#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

#
# Dockerfile for guacamole-client
#

# Use args for Tomcat image label to allow image builder to choose alternatives
# such as `--build-arg TOMCAT_JRE=jre8-alpine`
#
ARG TOMCAT_VERSION=8.5
ARG TOMCAT_JRE=jdk8

# Use official maven image for the build
FROM maven:3-eclipse-temurin-8-focal AS builder

# Use Mozilla's Firefox PPA (newer Ubuntu lacks a "firefox-esr" package and
# provides only a transitional "firefox" package that actually requires Snap
# and thus can't be used within Docker)
RUN    apt-get update                                \
    && apt-get upgrade -y                            \
    && apt-get install -y software-properties-common \
    && add-apt-repository -y ppa:mozillateam/ppa

# Explicitly prefer packages from the Firefox PPA
COPY guacamole-docker/mozilla-firefox.pref /etc/apt/preferences.d/

# Install firefox browser for sake of JavaScript unit tests
RUN apt-get update && apt-get install -y firefox

# Arbitrary arguments that can be passed to the maven build. By default, an
# argument will be provided to explicitly unskip any skipped tests. To, for
# example, allow the building of the RADIUS auth extension, pass a build profile
# as well: `--build-arg MAVEN_ARGUMENTS="-P lgpl-extensions -DskipTests=false"`.
ARG MAVEN_ARGUMENTS="-DskipTests=false"

# Versions of JDBC drivers to bundle within image
ARG MSSQL_JDBC_VERSION=12.2.0
ARG MYSQL_JDBC_VERSION=8.0.33
ARG PGSQL_JDBC_VERSION=42.6.0

# Build environment variables
ENV \
    BUILD_DIR=/tmp/guacamole-docker-BUILD

# Add configuration scripts
COPY guacamole-docker/bin/ /opt/guacamole/bin/

# Copy source to container for sake of build
COPY . "$BUILD_DIR"

ENV IGNORE_LICENSE_ERRORS=true

# Run the build itself
RUN /opt/guacamole/bin/build-guacamole.sh "$BUILD_DIR" /opt/guacamole

# For the runtime image, we start with the official Tomcat distribution
FROM tomcat:${TOMCAT_VERSION}-${TOMCAT_JRE}

# Install XMLStarlet for server.xml alterations and unzip for LOGBACK_LEVEL case
RUN apt-get update -qq \
    && apt-get install -y xmlstarlet unzip\
    && rm -rf /var/lib/apt/lists/* 

# This is where the build artifacts go in the runtime image
WORKDIR /opt/guacamole

# Copy artifacts from builder image into this image
COPY --from=builder /opt/guacamole/ .

# Create a new user guacamole
ARG UID=1001
ARG GID=1001
RUN groupadd --gid $GID guacamole
RUN useradd --system --create-home --shell /usr/sbin/nologin --uid $UID --gid $GID guacamole

# Run with user guacamole
USER guacamole

# Start Guacamole under Tomcat, listening on 0.0.0.0:8080
EXPOSE 8080
CMD ["/opt/guacamole/bin/start.sh" ]
