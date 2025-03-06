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
## @fn EXTRA_CACERT_/configure.sh
##
## Configures Tomcat to import additional CA certificates if
## the EXTRA_CACERT_ENABLED environment variable is set to "true".
##

##
## Search pach for additional CA certificates MUST be specified in
## EXTRA_CACERT_SEARCH_PATH environment variables. 
## CA certificates MUST have an extension of .crt
##
CERT_PATH="EXTRA_CACERT_$(echo "searchPath" | sed 's/\([a-z]\)\([A-Z]\)/\1_\2/g' | tr 'a-z' 'A-Z')"

for cert_fn in `find ${!CERT_PATH} -name "*.crt"`; do
    keytool -importcert -file $cert_fn -alias $(basename $cert_fn) -storepass changeit -noprompt -keystore $JAVA_HOME/lib/security/cacerts || true
done