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

package org.apache.guacamole.properties;

import org.apache.guacamole.properties.EnumGuacamoleProperty.PropertyValue;

/**
 * An enum that supports configuring various user and group case sensitivity
 * settings.
 */
public enum CaseSensitivity {
    
    /**
     * Case sensitivity enabled for both usernames and group names.
     */
    @PropertyValue("enabled")
    ENABLED(true, true),
    
    /**
     * Case sensitivity enabled for usernames but disabled for group names.
     */
    @PropertyValue("usernames")
    USERS(true, false),
    
    /**
     * Case sensitivity disabled for usernames but enabled for group names.
     */
    @PropertyValue("group-names")
    GROUPS(false, true),
    
    /**
     * Case sensitivity disabled for both usernames and group names.
     */
    @PropertyValue("disabled")
    DISABLED(false, false);
    
    /**
     * Whether or not case sensitivity should be enabled for usernames.
     */
    private final boolean usernames;
    
    /**
     * Whether or not case sensitivity should be enabled for group names.
     */
    private final boolean groupNames;
    
    CaseSensitivity(boolean usernames, boolean groupNames) {
        this.usernames = usernames;
        this.groupNames = groupNames;
    }
    
    /**
     * Return "true" if case sensitivity is enabled for usernames, otherwise
     * "false".
     * 
     * @return 
     *     "true" if case sensitivity is enabled for usernames, otherwise "false".
     */
    public boolean caseSensitiveUsernames() {
        return usernames;
    }
    
    /**
     * Return "true" if case sensitivity is enabled group names, otherwise
     * "false".
     * 
     * @return 
     *     "true" if case sensitivity is enabled for group names, otherwise
     *     "false".
     */
    public boolean caseSensitiveGroupNames() {
        return groupNames;
    }
    
}
