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

import java.util.HashMap;
import java.util.Map;
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
        Map<String, String> attributes =
                new HashMap<String, String>(super.getAttributes());

        // Do not expose any TOTP-related attributes outside this extension
        attributes.remove(TOTP_KEY_SECRET_ATTRIBUTE_NAME);
        attributes.remove(TOTP_KEY_CONFIRMED_ATTRIBUTE_NAME);

        // Expose only non-TOTP attributes
        return attributes;

    }

    @Override
    public void setAttributes(Map<String, String> attributes) {

        // Create independent, mutable copy of attributes
        attributes = new HashMap<String, String>(attributes);

        // Do not expose any TOTP-related attributes outside this extension
        attributes.remove(TOTP_KEY_SECRET_ATTRIBUTE_NAME);
        attributes.remove(TOTP_KEY_CONFIRMED_ATTRIBUTE_NAME);

        // Set only non-TOTP attributes
        super.setAttributes(attributes);

    }

}
