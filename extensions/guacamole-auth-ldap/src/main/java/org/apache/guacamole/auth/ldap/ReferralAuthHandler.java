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

package org.apache.guacamole.auth.ldap;

import com.google.inject.Inject;
import com.novell.ldap.LDAPAuthHandler;
import com.novell.ldap.LDAPAuthProvider;
import com.novell.ldap.LDAPConnection;
import java.io.UnsupportedEncodingException;
import org.apache.guacamole.GuacamoleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that implements the necessary authentication handling
 * for following referrals in LDAP connections.
 */
public class ReferralAuthHandler implements LDAPAuthHandler {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(ReferralAuthHandler.class);

    /**
     * The LDAPAuthProvider object that will be set and returned to the referral handler.
     */
    private final LDAPAuthProvider ldapAuth;

    /**
     * Creates a ReferralAuthHandler object to handle authentication when
     * following referrals in a LDAP connection, using the provided dn and
     * password.
     */
    public ReferralAuthHandler(String dn, String password) {
        byte[] passwordBytes;
        try {

            // Convert password into corresponding byte array
            if (password != null)
                passwordBytes = password.getBytes("UTF-8");
            else
                passwordBytes = null;

        }   
        catch (UnsupportedEncodingException e) {
            logger.error("Unexpected lack of support for UTF-8: {}", e.getMessage());
            logger.debug("Support for UTF-8 (as required by Java spec) not found.", e); 
            throw new UnsupportedOperationException("Unexpected lack of UTF-8 support.", e);
        }
        ldapAuth = new LDAPAuthProvider(dn, passwordBytes);
    }

    @Override
    public LDAPAuthProvider getAuthProvider(String host, int port) {
        return ldapAuth;
    }

}
