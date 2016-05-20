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
## @fn download-ldap-auth.sh
##
## Downloads LDAP authentication support. The LDAP authentication .jar file
## will be placed within the specified destination directory.
##
## @param VERSION
##     The version of guacamole-auth-ldap to download, such as "0.9.6".
##
## @param DESTINATION
##     The directory to save downloaded files within.
##

VERSION="$1"
DESTINATION="$2"

#
# Use ldap/ subdirectory within DESTINATION.
#

DESTINATION="$DESTINATION/ldap"

#
# Create destination, if it does not yet exist
#

mkdir -p "$DESTINATION"

#
# Download Guacamole LDAP auth
#

echo "Downloading LDAP auth version $VERSION ..."
curl -L "http://sourceforge.net/projects/guacamole/files/current/extensions/guacamole-auth-ldap-$VERSION.tar.gz" | \
tar -xz               \
    -C "$DESTINATION" \
    --wildcards       \
    --no-anchored     \
    --xform="s#.*/##" \
    "*.jar"           \
    "*.ldif"

