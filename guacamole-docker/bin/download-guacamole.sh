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
## @fn download-guacamole.sh
##
## Downloads Guacamole, saving the specified version to "guacamole.war" within
## the given directory.
##
## @param VERSION
##     The version of guacamole.war to download, such as "0.9.6".
##
## @param DESTINATION
##     The directory to save guacamole.war within.
##

VERSION="$1"
DESTINATION="$2"

#
# Create destination, if it does not yet exist
#

mkdir -p "$DESTINATION"

#
# Download guacamole.war, placing in specified destination
#

echo "Downloading Guacamole version $VERSION to $DESTINATION ..."
curl -L "http://sourceforge.net/projects/guacamole/files/current/binary/guacamole-${VERSION}.war" > "$DESTINATION/guacamole.war"

