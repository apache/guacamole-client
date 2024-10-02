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

package org.apache.guacamole.auth.header;

import com.google.inject.Inject;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;

/**
 * Service for retrieving configuration information for HTTP header-based 
 * authentication.
 */
public class ConfigurationService {

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * Returns the header of the HTTP server as configured with
     * guacamole.properties used for HTTP authentication.
     * By default, this will be "REMOTE_USER".
     *
     * @return
     *     The header used for HTTP authentication, as configured with
     *     guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public String getHttpAuthHeader() throws GuacamoleException {
        return environment.getProperty(
            HTTPHeaderGuacamoleProperties.HTTP_AUTH_HEADER,
            "REMOTE_USER"
        );
    }
    
    /**
     * Returns true if the usernames provided to the header authentication
     * module should be treated as case-sensitive, or false if usernames
     * should be treated as case-insensitive. This will default to the global
     * Guacamole configuration for case-sensitivity, which defaults to true, but
     * can be overridden for this extension, if desired.
     * 
     * @return
     *     true if usernames should be treated as case-sensitive, otherwise
     *     false.
     * 
     * @throws GuacamoleException 
     *     If guacamole.properties cannot be parsed.
     */
    public boolean getCaseSensitiveUsernames() throws GuacamoleException {
        return environment.getProperty(
            HTTPHeaderGuacamoleProperties.HTTP_AUTH_CASE_SENSITIVE_USERNAMES,
            environment.getCaseSensitiveUsernames()
        );
    }

}
