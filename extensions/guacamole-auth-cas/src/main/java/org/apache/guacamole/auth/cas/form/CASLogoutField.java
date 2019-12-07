/* * Licensed to the Apache Software Foundation (ASF) under one
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
import org.apache.guacamole.form.Field;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.cas.conf.ConfigurationService;


/**
 * Field definition used to redirect a user to CAS logout
 */
public class CASLogoutField extends Field {

   /**
    *  A parameter name for the field
    */
    public static final String PARAMETER_NAME = "logout";

    /**
     * The full URI which the field should link to.
     */
    private final URI logoutURI;

    /**
     * Mimics the CAS ticket routine but calls logout...
     *
     * @param authorizationEndpoint
     *     The full URL of the CAS logout URI
     *
     * @param logoutURI
     *     The URI that Guacamole should redirect the user's browser to 
     *     in order to logout
     */
    public CASLogoutField(URI logoutURI) {

        // Init base field properties
        super(PARAMETER_NAME, "GUAC_CAS_LOGOUT");
        this.logoutURI = logoutURI;

    }


    /**
     * Returns the full URI that this field should link to logging out
     *
     * @return
     *     The full URI that this field should link to.
     */
    public String getlogoutURI() {
        return logoutURI.toString();
    }

}
