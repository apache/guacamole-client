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

package org.apache.guacamole.auth.saml.form;

import org.apache.guacamole.form.Field;

/**
 * Field definition which represents the data used to do redirects
 * during SAML authentication.
 */
public class SAMLRedirectField extends Field {

    /**
     * The name of the parameter containing the redirect.
     */
    public static final String PARAMETER_NAME = "samlRedirect";

    /**
     * The encoded URI of the redirect.
     */
    private final String samlRedirect;

    /**
     * Creates a new field which facilitates redirection of the user
     * during SAML SSO authentication.
     *
     * @param samlRedirect
     *     The URI to which the user should be redirected.
     */
    public SAMLRedirectField(String samlRedirect) {

        // Init base field properties
        super(PARAMETER_NAME, "GUAC_SAML_REDIRECT");

        this.samlRedirect = samlRedirect;

    }

    /**
     * Returns the URI of the redirect.
     * 
     * @return
     *     The URI of the redirect.
     */
    public String getSamlRedirect() {
        return samlRedirect;
    }

}
