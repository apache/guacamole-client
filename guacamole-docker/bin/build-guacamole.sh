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
## @fn build-guacamole.sh
##
## Builds Guacamole, saving "guacamole.war" and all applicable extension .jars
## using the guacamole-client source contained within the given directory.
## Extension files will be grouped by their associated type, with all MySQL
## files being placed within the "mysql/" subdirectory of the destination, all
## PostgreSQL files being placed within the "postgresql/" subdirectory of the
## destination, etc.
##
## @param BUILD_DIR
##     The directory which currently contains the guacamole-client source and
##     in which the build should be performed.
##
## @param DESTINATION
##     The directory to save guacamole.war within, along with all extension
##     .jars.  Note that this script will create extension-specific
##     subdirectories within this directory, and files will thus be grouped by
##     extension type.
##
## @param BUILD_PROFILE
##     The build profile that will be passed to Maven build process. Defaults
##     to empty string. Can be set to "lgpl-extensions" to e.g. include
##     RADIUS authentication extension.
##

BUILD_DIR="$1"
DESTINATION="$2"
BUILD_PROFILE="$3"

#
# Create destination, if it does not yet exist
#

mkdir -p "$DESTINATION"

#
# Build guacamole.war and all extensions
#

cd "$BUILD_DIR"

# Required for build leveraging PhantomJS for unit testing (without this, the
# build fails with "libssl_conf.so: cannot open shared object file: No such
# file or directory")
export OPENSSL_CONF=/etc/ssl

if [ -z "$BUILD_PROFILE" ]; then
    mvn package
else
    mvn -P "$BUILD_PROFILE" package
fi

#
# Copy guacamole.war to destination
#

cp guacamole/target/*.war "$DESTINATION/guacamole.war"

#
# Copy JDBC auth extensions and SQL scripts
#

tar -xzf extensions/guacamole-auth-jdbc/modules/guacamole-auth-jdbc-dist/target/*.tar.gz \
    -C "$DESTINATION"                                   \
    --wildcards                                         \
    --no-anchored                                       \
    --strip-components=1                                \
    "*.jar"                                             \
    "*.sql"

#
# Download MySQL JDBC driver
#

echo "Downloading MySQL Connector/J ..."
curl -L "https://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-5.1.46.tar.gz" | \
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

#
# Copy LDAP auth extension and schema modifications
#

mkdir -p "$DESTINATION/ldap"
tar -xzf extensions/guacamole-auth-ldap/target/*.tar.gz \
    -C "$DESTINATION/ldap"                              \
    --wildcards                                         \
    --no-anchored                                       \
    --xform="s#.*/##"                                   \
    "*.jar"                                             \
    "*.ldif"

#
# Copy Radius auth extension if it was build
#

if [ -f extensions/guacamole-auth-radius/target/guacamole-auth-radius*.jar ]; then
    mkdir -p "$DESTINATION/radius"
    cp extensions/guacamole-auth-radius/target/guacamole-auth-radius*.jar "$DESTINATION/radius"
fi

#
# Copy OPENID auth extension and schema modifications
#

if [ -f extensions/guacamole-auth-openid/target/guacamole-auth-openid*.jar ]; then
    mkdir -p "$DESTINATION/openid"
    cp extensions/guacamole-auth-openid/target/guacamole-auth-openid*.jar "$DESTINATION/openid"
fi

#
# Copy TOTP auth extension if it was built
#

if [ -f extensions/guacamole-auth-totp/target/guacamole-auth-totp*.jar ]; then
    mkdir -p "$DESTINATION/totp"
    cp extensions/guacamole-auth-totp/target/guacamole-auth-totp*.jar "$DESTINATION/totp"
fi

#
# Copy Duo auth extension if it was built
#

if [ -f extensions/guacamole-auth-duo/target/*.tar.gz ]; then
    mkdir -p "$DESTINATION/duo"
    tar -xzf extensions/guacamole-auth-duo/target/*.tar.gz \
        -C "$DESTINATION/duo/"                             \
        --wildcards                                        \
        --no-anchored                                      \
        --no-wildcards-match-slash                         \
        --strip-components=1                               \
        "*.jar"
fi

#
# Copy header auth extension if it was built
#

if [ -f extensions/guacamole-auth-header/target/guacamole-auth-header*.jar ]; then
    mkdir -p "$DESTINATION/header"
    cp extensions/guacamole-auth-header/target/guacamole-auth-header*.jar "$DESTINATION/header"
fi

#
# Copy CAS auth extension if it was built
#

if [ -f extensions/guacamole-auth-cas/target/*.tar.gz ]; then
    mkdir -p "$DESTINATION/cas"
    tar -xzf extensions/guacamole-auth-cas/target/*.tar.gz  \
    -C "$DESTINATION/cas/"                                  \
    --wildcards                                             \
    --no-anchored                                           \
    --no-wildcards-match-slash                              \
    --strip-components=1                                    \
    "*.jar"
fi

#
# Copy json auth extension if it was built
#

if [ -f extensions/guacamole-auth-json/target/guacamole-auth-json*.jar ]; then
    mkdir -p "$DESTINATION/json"
    cp extensions/guacamole-auth-json/target/guacamole-auth-json*.jar "$DESTINATION/json"
fi
