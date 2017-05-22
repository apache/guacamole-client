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

package org.apache.guacamole.auth.cas.form;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.SecureRandom;
import org.apache.guacamole.form.Field;


/**
 * Field definition which represents the ticket returned by an CAS service.
 * This is processed transparently - the user is redirected to CAS, authenticates
 * and then is returned to Guacamole where the ticket field is
 * processed.
 */
public class CASTicketField extends Field {

    /**
     * The standard HTTP parameter which will be included within the URL by all
     * CAS services upon successful authentication and redirect.
     */
    public static final String PARAMETER_NAME = "ticket";

    /**
     * The full URI which the field should link to.
     */
    private final String authorizationURI;

    /**
     * Creates a new CAS "ticket" field which links to the given CAS
     * service using the provided client ID. Successful authentication at the
     * CAS service will result in the client being redirected to the specified
     * redirect URI. The CAS ticket will be embedded in the fragment (the part
     * following the hash symbol) of that URI, which the JavaScript side of
     * this extension will move to the query parameters.
     *
     * @param authorizationEndpoint
     *     The full URL of the endpoint accepting CAS authentication
     *     requests.
     *
     * @param clientID
     *     The ID of the CAS client. This is normally determined ahead of
     *     time by the CAS service through some manual credential request
     *     procedure.
     *
     * @param redirectURI
     *     The URI that the CAS service should redirect to upon successful
     *     authentication.
     */
    public CASTicketField(String authorizationEndpoint, String redirectURI) {

        // Init base field properties
        super(PARAMETER_NAME, "GUAC_CAS_TICKET");

        // Build authorization URI from given values
        try {
            this.authorizationURI = authorizationEndpoint
                    + "?service=" + URLEncoder.encode(redirectURI, "UTF-8");
        }

        // Java is required to provide UTF-8 support
        catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException("Unexpected lack of UTF-8 support.", e);
        }

    }

    /**
     * Returns the full URI that this field should link to when a new ticket
     * needs to be obtained from the CAS service.
     *
     * @return
     *     The full URI that this field should link to.
     */
    public String getAuthorizationURI() {
        return authorizationURI;
    }

}
