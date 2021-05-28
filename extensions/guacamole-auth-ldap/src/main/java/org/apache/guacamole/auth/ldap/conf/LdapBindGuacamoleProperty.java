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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.properties.GuacamoleProperty;

/**
 * A GuacamoleProperty that checks a string to a verify that it is a valid
 * LDAP bind format - either an Active Directory UPN or a LDAP DN.  An exception
 * is thrown if the provided value is invalid.
 */
public abstract class LdapBindGuacamoleProperty implements GuacamoleProperty<String> {

    /**
     * An expression that matches valid Active Directory UPN values, in one of
     * two formats - either <DOMAIN>\<USERNAME> or <USERNAME>@<DOMAIN>.
     */
    private static final Pattern LDAP_UPN_PATTERN = Pattern.compile("^(?:(?<adusername>[^@]+)@(?<addomain>.+)|(?<nbdomain>[^\\\\]+)\\\\(?<nbusername>.+))$");
    
    @Override
    public String parseValue(String value) throws GuacamoleException {

        if (value == null)
            return null;

        // Check input value against UPN regex matcher
        Matcher upnMatcher = LDAP_UPN_PATTERN.matcher(value);
        if (upnMatcher.matches())
            return value;
        
        // If UPN matcher doesn't work, verify the DN
        if (Dn.isValid(value))
            return value;
        
        throw new GuacamoleServerException("The DN \"" + value + "\" is invalid.");

    }

}