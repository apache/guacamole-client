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

package org.apache.guacamole.auth.ldap.conf;

/**
 * A data type for tracking the type of binding used to initially contact
 * the LDAP directory.
 */
public enum BindType {
    
    /**
     * Bind anonymously to the LDAP directory to search for the user logging in.
     */
    ANONYMOUS,
    
    /**
     * Derive the username from the base DN and username attribute configured
     * in guacamole.properties.
     */
    DERIVED,
    
    /**
     * User the username entered in the logon box to bind directly to the
     * LDAP directory.
     */
    DIRECT,
    
    /**
     * Use the values for search user and password configured in
     * guacamole.properties to locate the user logging in.
     */
    SEARCH;
    
}
