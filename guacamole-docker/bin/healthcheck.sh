#!/bin/bash -e
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
## @fn healthcheck.sh
##
## Performs a health check for the Guacamole container. If HEALTH_CHECK_VALVE_ENABLED
## is set to "true", this script will check the actual health endpoint. Otherwise,
## it will simply return success and log that health checks are disabled.
##


# Check if health check valve is enabled
if [ "$HEALTH_CHECK_VALVE_ENABLED" = "true" ]; then
    # Default health check path (/health)
    HEALTH_CHECK_PATH="${HEALTH_CHECK_VALVE_PATH:-/health}"
    # Perform actual health check via curl
    curl --fail --silent --show-error "http://localhost:8080${HEALTH_CHECK_PATH}" || exit 1
else
    # Health check valve is disabled - just return OK
    echo "Health check valve is disabled. Reporting healthy without actual check."
    exit 0
fi
