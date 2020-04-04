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

package org.apache.guacamole.auth.totp.user;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.guacamole.form.BooleanField;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.net.auth.DelegatingUser;
import org.apache.guacamole.net.auth.User;

/**
 * TOTP-specific User implementation which wraps a User from another extension,
 * hiding and blocking access to the core attributes used by TOTP.
 */
public class TOTPUser extends DelegatingUser {

    /**
     * The name of the user attribute which stores the TOTP key.
     */
    public static final String TOTP_KEY_SECRET_ATTRIBUTE_NAME = "guac-totp-key-secret";

    /**
     * The name of the user attribute defines whether the TOTP key has been
     * confirmed by the user, and the user is thus fully enrolled.
     */
    public static final String TOTP_KEY_CONFIRMED_ATTRIBUTE_NAME = "guac-totp-key-confirmed";
    
    /**
     * The name of the field used to trigger a reset of the TOTP data.
     */
    public static final String TOTP_KEY_SECRET_RESET_FIELD = "guac-totp-reset";

    /**
     * The form which contains all configurable properties for this user.
     */
    public static final Form TOTP_CONFIG_FORM = new Form("totp-config-form",
            Arrays.asList(
                    new BooleanField(TOTP_KEY_SECRET_RESET_FIELD, "true"),
                    new BooleanField(TOTP_KEY_CONFIRMED_ATTRIBUTE_NAME, "true")
            )
    );
    
    
    /**
     * Wraps the given User object, hiding and blocking access to the core
     * attributes used by TOTP.
     *
     * @param user
     *     The User object to wrap.
     */
    public TOTPUser(User user) {
        super(user);
    }

    /**
     * Returns the User object wrapped by this TOTPUser.
     *
     * @return
     *     The wrapped User object.
     */
    public User getUndecorated() {
        return getDelegateUser();
    }

    @Override
    public Map<String, String> getAttributes() {

        // Create independent, mutable copy of attributes
        Map<String, String> attributes = new HashMap<>(super.getAttributes());
        
        // Protect the secret value by removing it
        String secret = attributes.remove(TOTP_KEY_SECRET_ATTRIBUTE_NAME);
        
        // If secret is null or empty, mark the reset as true.
        if (secret == null || secret.isEmpty())
            attributes.put(TOTP_KEY_SECRET_RESET_FIELD, "true");
            
        // If secret has a value, mark the reset as false.
        else
            attributes.put(TOTP_KEY_SECRET_RESET_FIELD, "false");

        return attributes;

    }

    @Override
    public void setAttributes(Map<String, String> attributes) {

        // Create independent, mutable copy of attributes
        attributes = new HashMap<>(attributes);
        
        // Do not expose any TOTP secret attribute outside this extension
        attributes.remove(TOTP_KEY_SECRET_ATTRIBUTE_NAME);
        
        // Pull off the boolean reset field
        String reset = attributes.remove(TOTP_KEY_SECRET_RESET_FIELD);
        
        // If reset has been set to true, clear the secret.
        if (reset != null && reset.equals("true")) {
            attributes.put(TOTP_KEY_SECRET_ATTRIBUTE_NAME, null);
            attributes.put(TOTP_KEY_CONFIRMED_ATTRIBUTE_NAME, null);
        }

        super.setAttributes(attributes);

    }

}
