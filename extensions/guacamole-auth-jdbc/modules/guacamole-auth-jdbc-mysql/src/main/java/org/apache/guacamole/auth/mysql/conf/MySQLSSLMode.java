/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.auth.mysql.conf;

import org.apache.guacamole.properties.EnumGuacamoleProperty.PropertyValue;

/**
 * Possible values for enabling SSL within the MySQL Driver.
 */
public enum MySQLSSLMode {
    
    /**
     * Do not use SSL at all.
     */
    @PropertyValue("disabled")
    DISABLED,
    
    /**
     * Prefer SSL, but fall back to unencrypted.
     */
    @PropertyValue("preferred")
    PREFERRED,
    
    /**
     * Require SSL, but perform no certificate validation.
     */
    @PropertyValue("required")
    REQUIRED,
    
    /**
     * Require SSL, and validate server certificate issuer.
     */
    @PropertyValue("verify-ca")
    VERIFY_CA,
    
    /**
     * Require SSL and validate both server certificate issuer and server
     * identity.
     */
    @PropertyValue("verify-identity")
    VERIFY_IDENTITY;
    
}
