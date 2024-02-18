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
## @fn REMOTE_IP_VALVE_/configure.sh
##
## Configures Tomcat to forward the IP addresses of clients behind a proxy if
## the REMOTE_IP_VALVE_ENABLED environment variable is set to "true".
##

# Add <Valve> element
xmlstarlet edit --inplace \
    --insert '/Server/Service/Engine/Host/*' --type elem -n Valve \
    --insert '/Server/Service/Engine/Host/Valve[not(@className)]' --type attr -n className -v org.apache.catalina.valves.RemoteIpValve \
    $CATALINA_BASE/conf/server.xml

# Allowed IPs
if [ -z "$PROXY_ALLOWED_IPS_REGEX" ]; then
    echo "Using default Tomcat allowed IPs regex"
else
    xmlstarlet edit --inplace \
        --insert '/Server/Service/Engine/Host/Valve[@className="org.apache.catalina.valves.RemoteIpValve"]' \
        --type attr -n internalProxies -v "$PROXY_ALLOWED_IPS_REGEX" \
        $CATALINA_BASE/conf/server.xml
fi

# X-Forwarded-For
if [ -z "$PROXY_IP_HEADER" ]; then
    echo "Using default Tomcat proxy IP header"
else
    xmlstarlet edit --inplace \
        --insert "/Server/Service/Engine/Host/Valve[@className='org.apache.catalina.valves.RemoteIpValve']" \
        --type attr -n remoteIpHeader -v "$PROXY_IP_HEADER" \
        $CATALINA_BASE/conf/server.xml
fi

# X-Forwarded-Proto
if [ -z "$PROXY_PROTOCOL_HEADER" ]; then
    echo "Using default Tomcat proxy protocol header"
else
    xmlstarlet edit --inplace \
        --insert "/Server/Service/Engine/Host/Valve[@className='org.apache.catalina.valves.RemoteIpValve']" \
        --type attr -n protocolHeader -v "$PROXY_PROTOCOL_HEADER" \
        $CATALINA_BASE/conf/server.xml
fi

# X-Forwarded-By
if [ -z "$PROXY_BY_HEADER" ]; then
    echo "Using default Tomcat proxy forwarded by header"
else
    xmlstarlet edit --inplace \
        --insert "/Server/Service/Engine/Host/Valve[@className='org.apache.catalina.valves.RemoteIpValve']" \
        --type attr -n remoteIpProxiesHeader -v "$PROXY_BY_HEADER" \
        $CATALINA_BASE/conf/server.xml
fi

