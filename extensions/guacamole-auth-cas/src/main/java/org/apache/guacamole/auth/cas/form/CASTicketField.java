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

import java.net.URI;
import javax.ws.rs.core.UriBuilder;
import org.apache.guacamole.form.RedirectField;
import org.apache.guacamole.language.TranslatableMessage;


/**
 * Field definition which represents the ticket returned by an CAS service.
 * This is processed transparently - the user is redirected to CAS, authenticates
 * and then is returned to Guacamole where the ticket field is
 * processed.
 */
public class CASTicketField extends RedirectField {

    /**
     * The parameter that will be present upon successful CAS authentication.
     */
    public static final String PARAMETER_NAME = "ticket";
    
    /**
     * The standard URI name for the CAS login resource.
     */
    private static final String CAS_LOGIN_URI = "login";

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
     * @param redirectURI
     *     The URI that the CAS service should redirect to upon successful
     *     authentication.
     * 
     * @param redirectMessage
     *     The message that will be displayed for the user while the redirect
     *     is processed.  This will be processed through Guacamole's translation
     *     system.
     */
    public CASTicketField(URI authorizationEndpoint, URI redirectURI,
            TranslatableMessage redirectMessage) {
        
        super(PARAMETER_NAME, UriBuilder.fromUri(authorizationEndpoint)
                .path(CAS_LOGIN_URI)
                .queryParam("service", redirectURI)
                .build(),
                redirectMessage);

    }

}
