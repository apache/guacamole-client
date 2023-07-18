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

package org.apache.guacamole.auth.sso.conf;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.DelegatingEnvironment;
import org.apache.guacamole.environment.LocalEnvironment;

/**
 * An SSO-specific environment that defines generic properties that can be used
 * with any of the implemented SSO providers.
 */
public abstract class SSOEnvironment extends DelegatingEnvironment {
    
    /**
     * Create a new instance of the SSOEnvironment using the underlying
     * LocalEnvironment to read configured properties.
     */
    public SSOEnvironment() {
        super(LocalEnvironment.getInstance());
    }
    
    /**
     * Returns true if the usernames provided to the SSO authentication
     * module should be treated as case-sensitive, or false if usernames
     * should be treated as case-insensitive. The default is true, usernames
     * will be case-sensitive in keeping with the past behavior of Guacamole
     * prior to the addition of this option.
     * 
     * @return
     *     true if usernames should be treated as case-sensitive, otherwise
     *     false.
     * 
     * @throws GuacamoleException 
     *     If guacamole.properties cannot be parsed.
     */
    public boolean getCaseSensitiveUsernames() throws GuacamoleException {
        
        // While most SSO systems do not use case to differentiate between
        // usernames, this currently defaults to true to avoid suddenly
        // breaking any extensions that rely on case-sensitivity.
        return true;
    }
    
}
