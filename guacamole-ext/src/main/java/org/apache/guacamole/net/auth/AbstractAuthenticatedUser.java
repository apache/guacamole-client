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

package org.apache.guacamole.net.auth;

import java.util.Collections;
import java.util.Set;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.environment.LocalEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic implementation of an AuthenticatedUser which uses the username to
 * determine equality. Username comparison is case-sensitive.
 */
public abstract class AbstractAuthenticatedUser extends AbstractIdentifiable
        implements AuthenticatedUser {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAuthenticatedUser.class);
    
    /**
     * The server environment in which this Guacamole Client instance is
     * running.
     */
    private final Environment environment = LocalEnvironment.getInstance();
    
    // Prior functionality now resides within AbstractIdentifiable

    @Override
    public Set<String> getEffectiveUserGroups() {
        return Collections.<String>emptySet();
    }

    @Override
    public boolean isCaseSensitive() {
        try {
            return environment.getCaseSensitivity().caseSensitiveUsernames();
        }
        catch (GuacamoleException e) {
            LOGGER.error("Failed to retrieve the configuration for case sensitivity: {}. "
                       + "Username comparisons will be case-sensitive.",
                       e.getMessage());
            LOGGER.debug("An exception was caught when attempting to retrieve the "
                       + "case sensitivity configuration.", e);
            return true;
        }
    }
    
    @Override
    public void invalidate() {
        // Nothing to invalidate
    }

}
