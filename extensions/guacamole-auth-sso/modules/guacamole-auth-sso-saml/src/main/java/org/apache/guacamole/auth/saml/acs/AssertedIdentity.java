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

package org.apache.guacamole.auth.saml.acs;

import com.onelogin.saml2.authn.SamlResponse;
import com.onelogin.saml2.exception.ValidationError;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.xml.xpath.XPathExpressionException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.auth.saml.conf.SAMLConfiguration;

/**
 * Representation of a user's identity as asserted by a SAML IdP.
 */
public class AssertedIdentity {

    /**
     * The original SAML response received from the SAML IdP that asserted
     * the user's identity.
     */
    private final SamlResponse response;

    /**
     * The unique identifier of the SAML identity provider that asserted this
     * identity.
     */
    private final String identityProviderId;

    /**
     * The name of the SAML attribute containing group memberships for the
     * identity provider that asserted this identity, or null if the default
     * configured attribute should be used.
     */
    private final String groupAttribute;

    /**
     * The user's Guacamole username.
     */
    private final String username;

    /**
     * All attributes included in the original SAML response. Attributes may
     * possibly be associated with multiple values.
     */
    private final Map<String, List<String>> attributes;

    /**
     * Creates a new AssertedIdentity representing the identity asserted by the
     * default SAML IdP in the given response.
     *
     * @param response
     *     The response received from the SAML IdP.
     *
     * @throws GuacamoleException
     *     If the given SAML response cannot be parsed or is missing required
     *     information.
     */
    public AssertedIdentity(SamlResponse response) throws GuacamoleException {
        this(response, SAMLConfiguration.DEFAULT_ID, null);
    }

    /**
     * Creates a new AssertedIdentity representing the identity asserted by a
     * specific SAML IdP in the given response.
     *
     * @param response
     *     The response received from the SAML IdP.
     *
     * @param identityProviderId
     *     The unique identifier of the SAML IdP that asserted this identity.
     *
     * @param groupAttribute
     *     The name of the SAML attribute containing group memberships for this
     *     IdP, or null if the default configured attribute should be used.
     *
     * @throws GuacamoleException
     *     If the given SAML response cannot be parsed or is missing required
     *     information.
     */
    public AssertedIdentity(SamlResponse response, String identityProviderId,
            String groupAttribute) throws GuacamoleException {

        // Parse user identity from SAML response
        String nameId;
        try {
            nameId = response.getNameId();
            if (nameId == null)
                throw new GuacamoleSecurityException("SAML response did not "
                        + "include the relevant user's identity (no name ID).");
        }

        // Unfortunately, getNameId() is declared as "throws Exception", so
        // this error handling has to be pretty generic
        catch (Exception e) {
            throw new GuacamoleSecurityException("User identity (name ID) "
                    + "could not be retrieved from the SAML response: " + e.getMessage(), e);
        }

        // Retrieve any provided attributes
        Map<String, List<String>> responseAttributes;
        try {
            responseAttributes = Collections.unmodifiableMap(response.getAttributes());
        }
        catch (XPathExpressionException | ValidationError e) {
            throw new GuacamoleSecurityException("SAML attributes could not "
                    + "be parsed from the SAML response: " + e.getMessage(), e);
        }

        this.response = response;
        this.identityProviderId = identityProviderId;
        this.groupAttribute = groupAttribute;
        this.username = nameId.toLowerCase(); // Canonicalize username as lowercase
        this.attributes = responseAttributes;

    }

    /**
     * Returns the unique identifier of the SAML identity provider that
     * asserted this identity.
     *
     * @return
     *     The unique identifier of the SAML identity provider.
     */
    public String getIdentityProviderId() {
        return identityProviderId;
    }

    /**
     * Returns the name of the SAML attribute containing group memberships for
     * the identity provider that asserted this identity, or null if not
     * explicitly recorded.
     *
     * @return
     *     The name of the SAML group attribute, or null if not recorded.
     */
    public String getGroupAttribute() {
        return groupAttribute;
    }

    /**
     * Returns the username of the Guacamole user whose identity was asserted
     * by the SAML IdP.
     *
     * @return
     *     The username of the Guacamole user whose identity was asserted by
     *     the SAML IdP.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns a Map containing all attributes included in the original SAML
     * response that asserted this user's identity. Attributes may possibly be
     * associated with multiple values.
     *
     * @return
     *     A Map of all attributes included in the original SAML response.
     */
    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    /**
     * Returns whether the identity asserted by the original SAML response is
     * still valid. An asserted identity may cease to be valid after creation
     * if it has expired according to the timestamps included in the response.
     *
     * @return
     *     true if the original SAML response is still valid, false otherwise.
     */
    public boolean isValid() {
        return response.isValid();
    }

}
