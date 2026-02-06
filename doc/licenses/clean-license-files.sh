#!/bin/sh
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
# clean-license-files.sh
# ----------------------
#
# Removes all stored license content for all files with at least one "Source"
# header, so that the build will automatically pull down the latest license
# content when run. The license content downloaded may need to be manually
# edited, particularly if dependency NOTICE files contain extra information
# that need not be kept in Guacamole's NOTICE files.
#
# This script is expected to be manually invoked when updating the license data
# of dependencies, particularly when dependencies are updated.
#
# USAGE:
#
#     path/to/clean-license-files.sh
#

##
## The directory containing this script and all license information. This
## should be "doc/licenses/" within the guacamole-client source tree.
##
LICENSES_DIR="`dirname "$0"`"

find "$LICENSES_DIR" -type f \
    | xargs grep -l '^# --- BEGIN LICENSE FILE \(\[[^]]*\] \)\?---$' \
    | xargs grep -l '^#[[:space:]]*Source:' \
    | while read FILENAME; do

    sed -i -n \
        -e '/^# --- BEGIN LICENSE FILE \(\[[^]]*\] \)\?---$/q' \
        -e 'p' \
        "$FILENAME"

done
