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

# Start from Tomcat image
FROM tomcat:8.0.20-jre8

# Environment variables
ENV \
    BUILD_DIR=/tmp/guacamole-docker-BUILD \
    BUILD_DEPENDENCIES="                  \
        maven                             \
        openjdk-8-jdk-headless"

# Add configuration scripts
COPY guacamole-docker/bin /opt/guacamole/bin/

# Copy source to container for sake of build
COPY . "$BUILD_DIR"

# Build latest guacamole-client and authentication
RUN apt-get update                                                    && \
    apt-get install -y --no-install-recommends $BUILD_DEPENDENCIES    && \
    /opt/guacamole/bin/build-guacamole.sh "$BUILD_DIR" /opt/guacamole && \
    rm -Rf "$BUILD_DIR"                                               && \
    rm -Rf /var/lib/apt/lists/*                                       && \
    apt-get purge -y --auto-remove $BUILD_DEPENDENCIES

# Start Guacamole under Tomcat, listening on 0.0.0.0:8080
EXPOSE 8080
CMD ["/opt/guacamole/bin/start.sh" ]

