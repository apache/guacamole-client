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
FROM tomcat:8.0.20-jre7
MAINTAINER Michael Jumper <mike.jumper@guac-dev.org>

# Version info
ENV \
    GUAC_VERSION=0.9.9      \
    GUAC_JDBC_VERSION=0.9.9 \
    GUAC_LDAP_VERSION=0.9.9

# Add configuration scripts
COPY bin /opt/guacamole/bin/

# Download and install latest guacamole-client and authentication
RUN \
    /opt/guacamole/bin/download-guacamole.sh "$GUAC_VERSION" /usr/local/tomcat/webapps && \
    /opt/guacamole/bin/download-jdbc-auth.sh "$GUAC_JDBC_VERSION" /opt/guacamole       && \
    /opt/guacamole/bin/download-ldap-auth.sh "$GUAC_LDAP_VERSION" /opt/guacamole

# Start Guacamole under Tomcat, listening on 0.0.0.0:8080
EXPOSE 8080
CMD ["/opt/guacamole/bin/start.sh" ]

