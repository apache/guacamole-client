#!/bin/sh -e
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

##
## @fn download-jdbc-auth.sh
##
## Downloads JDBC authentication support, including any required JDBC drivers.
## The downloaded files will be grouped by their associated database type, with
## all MySQL files being placed within the "mysql/" subdirectory of the
## destination, and all PostgreSQL files being placed within the "postgresql/"
## subdirectory of the destination.
##
## @param VERSION
##     The version of guacamole-auth-jdbc to download, such as "0.9.6".
##
## @param DESTINATION
##     The directory to save downloaded files within. Note that this script
##     will create database-specific subdirectories within this directory,
##     and downloaded files will be thus grouped by their respected database
##     types.
##

VERSION="$1"
DESTINATION="$2"

#
# Create destination, if it does not yet exist
#

mkdir -p "$DESTINATION"

#
# Download Guacamole JDBC auth
#

echo "Downloading JDBC auth version $VERSION ..."
curl -L "http://sourceforge.net/projects/guacamole/files/current/extensions/guacamole-auth-jdbc-$VERSION.tar.gz" | \
tar -xz                  \
    -C "$DESTINATION"    \
    --wildcards          \
    --no-anchored        \
    --strip-components=1 \
    "*.jar"              \
    "*.sql"

#
# Download MySQL JDBC driver
#

echo "Downloading MySQL Connector/J ..."
curl -L "http://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-5.1.35.tar.gz" | \
tar -xz                        \
    -C "$DESTINATION/mysql/"   \
    --wildcards                \
    --no-anchored              \
    --no-wildcards-match-slash \
    --strip-components=1       \
    "mysql-connector-*.jar"

#
# Download PostgreSQL JDBC driver
#

echo "Downloading PostgreSQL JDBC driver ..."
curl -L "https://jdbc.postgresql.org/download/postgresql-9.4-1201.jdbc41.jar" > "$DESTINATION/postgresql/postgresql-9.4-1201.jdbc41.jar"

