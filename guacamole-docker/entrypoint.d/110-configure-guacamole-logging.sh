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
## @fn 030-configure-guacamole-logging.sh
##
## Checks the value of the LOGBACK_LEVEL environment variable, producing a
## corresponding logback.xml file within GUACAMOLE_HOME if a log level has been
## explicitly specified.
##

# Set logback level if specified
if [ -n "$LOGBACK_LEVEL" ]; then
    unzip -o -j /opt/guacamole/guacamole.war WEB-INF/classes/logback.xml -d $GUACAMOLE_HOME
    sed -i "s/level=\"info\"/level=\"$LOGBACK_LEVEL\"/" $GUACAMOLE_HOME/logback.xml
fi

