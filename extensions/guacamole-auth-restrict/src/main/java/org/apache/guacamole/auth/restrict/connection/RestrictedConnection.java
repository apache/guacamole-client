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

package org.apache.guacamole.auth.restrict.connection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.restrict.Restrictable;
import org.apache.guacamole.auth.restrict.RestrictionVerificationService;
import org.apache.guacamole.auth.restrict.form.DateTimeRestrictionField;
import org.apache.guacamole.auth.restrict.form.HostRestrictionField;
import org.apache.guacamole.auth.restrict.form.TimeRestrictionField;
import org.apache.guacamole.calendar.RestrictionType;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.DelegatingConnection;
import org.apache.guacamole.protocol.GuacamoleClientInformation;

/**
 * A Connection implementation that wraps another connection, providing additional
 * ability to control access to the connection.
 */
public class RestrictedConnection extends DelegatingConnection implements Restrictable {

    /**
     * The remote address of the client from which the user logged in.
     */
    private final String remoteAddress;
    
    /**
     * The list of all connection attributes provided by this Connection implementation.
     */
    public static final List<String> RESTRICT_CONNECTION_ATTRIBUTES = Arrays.asList(
            RESTRICT_TIME_AFTER_ATTRIBUTE_NAME,
            RESTRICT_TIME_BEFORE_ATTRIBUTE_NAME,
            RESTRICT_TIME_ALLOWED_ATTRIBUTE_NAME,
            RESTRICT_TIME_DENIED_ATTRIBUTE_NAME,
            RESTRICT_HOSTS_ALLOWED_ATTRIBUTE_NAME,
            RESTRICT_HOSTS_DENIED_ATTRIBUTE_NAME
    );
    
    /**
     * The form containing the list of fields for the attributes provided
     * by this module.
     */
    public static final Form RESTRICT_CONNECTION_FORM = new Form("restrict-login-form",
            Arrays.asList(
                    new DateTimeRestrictionField(RESTRICT_TIME_AFTER_ATTRIBUTE_NAME),
                    new DateTimeRestrictionField(RESTRICT_TIME_BEFORE_ATTRIBUTE_NAME),
                    new TimeRestrictionField(RESTRICT_TIME_ALLOWED_ATTRIBUTE_NAME),
                    new TimeRestrictionField(RESTRICT_TIME_DENIED_ATTRIBUTE_NAME),
                    new HostRestrictionField(RESTRICT_HOSTS_ALLOWED_ATTRIBUTE_NAME),
                    new HostRestrictionField(RESTRICT_HOSTS_DENIED_ATTRIBUTE_NAME)
            )
    );
    
    /**
     * Wraps the given Connection object, providing capability of further
     * restricting connection access beyond the default access control provided
     * by other modules.
     *
     * @param connection
     *     The Connection object to wrap.
     * 
     * @param remoteAddress
     *     The remote address of the client from which the current user logged
     *     in.
     */
    public RestrictedConnection(Connection connection, String remoteAddress) {
        super(connection);
        this.remoteAddress = remoteAddress;
    }
    
    /**
     * Returns the original Connection object wrapped by this RestrictConnection.
     *
     * @return
     *     The wrapped Connection object.
     */
    public Connection getUndecorated() {
        return getDelegateConnection();
    }
    
    @Override
    public Map<String, String> getAttributes() {

        // Create independent, mutable copy of attributes
        Map<String, String> attributes = new HashMap<>(super.getAttributes());
        
        // Loop through extension-specific attributes and add them where no
        // values exist, so that they show up in the web UI.
        for (String attribute : RESTRICT_CONNECTION_ATTRIBUTES) {
            String value = attributes.get(attribute);
            if (value == null || value.isEmpty())
                attributes.put(attribute,  null);
        }

        return attributes;

    }

    @Override
    public void setAttributes(Map<String, String> attributes) {

        // Create independent, mutable copy of attributes
        attributes = new HashMap<>(attributes);

        // Loop through extension-specific attributes, only sending ones
        // that are non-null and non-empty to the underlying storage mechanism.
        for (String attribute : RESTRICT_CONNECTION_ATTRIBUTES) {
            String value = attributes.get(attribute);
            if (value != null && value.isEmpty())
                attributes.put(attribute, null);
        }

        super.setAttributes(attributes);

    }
    
    @Override
    public GuacamoleTunnel connect(GuacamoleClientInformation info,
            Map<String, String> tokens) throws GuacamoleException {
   
        // Verify the restrictions for this connection.
        RestrictionVerificationService.verifyConnectionRestrictions(this, remoteAddress);
        
        // Connect
        return super.connect(info, tokens);
        
    }
    
    @Override
    public RestrictionType getCurrentTimeRestriction() {
        String allowedTimeString = getAttributes().get(RESTRICT_TIME_ALLOWED_ATTRIBUTE_NAME);
        String deniedTimeString = getAttributes().get(RESTRICT_TIME_DENIED_ATTRIBUTE_NAME);
        return RestrictionVerificationService.allowedByTimeRestrictions(allowedTimeString, deniedTimeString);
    }
    
    @Override
    public RestrictionType getCurrentHostRestriction() {
        String allowedHostString = getAttributes().get(RESTRICT_HOSTS_ALLOWED_ATTRIBUTE_NAME);
        String deniedHostString = getAttributes().get(RESTRICT_HOSTS_DENIED_ATTRIBUTE_NAME);
        return RestrictionVerificationService.allowedByHostRestrictions(allowedHostString, deniedHostString, remoteAddress);
    }
    
}
