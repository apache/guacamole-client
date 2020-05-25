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

package org.apache.guacamole.form;

import java.net.URI;

/**
 * A Guacamole field that redirects a user to another page.
 */
public class RedirectField extends Field {

    /**
     * The encoded URL of the redirect.
     */
    private final URI redirectUrl;
    
    /**
     * The message that will be displayed for the user during the redirect
     * process.  This will be processed through Guacamole's translation system.
     */
    private final String redirectMsg;

    /**
     * Creates a new field which facilitates redirection of the user
     * to another page.
     *
     * @param name
     *     The name of this field.
     * 
     * @param redirectUrl
     *     The URL to which the user should be redirected.
     * 
     * @param redirectMsg
     *     The message to display during the redirect, which will be processed
     *     through Guacamole's translation system.
     */
    public RedirectField(String name, URI redirectUrl, String redirectMsg) {

        // Init base field properties
        super(name, Field.Type.REDIRECT);

        // Store the URL to which the user will be redirected.
        this.redirectUrl = redirectUrl;
        this.redirectMsg = redirectMsg;

    }

    /**
     * Returns the URL of the redirect.
     * 
     * @return
     *     The URL of the redirect.
     */
    public String getRedirectUrl() {
        return redirectUrl.toString();
    }
    
    /**
     * Returns the message that will be displayed for the user while the
     * redirect takes place.
     * 
     * @return
     *     The message to display for the user.
     */
    public String getRedirectMsg() {
        return redirectMsg;
    }

}
