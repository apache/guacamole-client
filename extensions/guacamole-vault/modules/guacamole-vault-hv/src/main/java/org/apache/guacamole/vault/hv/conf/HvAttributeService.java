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

package org.apache.guacamole.vault.hv.conf;

import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.apache.guacamole.form.BooleanField;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.form.TextField;
import org.apache.guacamole.form.PasswordField;
import org.apache.guacamole.vault.conf.VaultAttributeService;

/**
 * A service that exposes attributes for the admin UI, Four attributes are
 * exposed: hv-uri, hv-token, hv-username and hv-password
 *
 */
@Singleton
public class HvAttributeService implements VaultAttributeService {

    /**
     * The name of the attribute which can contain a HV URI
     * associated with either a connection group or user.
     */
    public static final String HV_URI_ATTRIBUTE = "hv-uri";

    /**
     * The name of the attribute which can contain a HV token
     * associated with either a connection group or user.
     */
    public static final String HV_TOKEN_ATTRIBUTE = "hv-token";

    /**
     * The name of the attribute which can contain a HV username
     * associated with either a connection group or user.
     */
    public static final String HV_USERNAME_ATTRIBUTE = "hv-username";

    /**
     * The name of the attribute which can contain a HV password
     * associated with either a connection group or user.
     */
    public static final String HV_PASSWORD_ATTRIBUTE = "hv-password";

    /**
     * All attributes related to configuring the HV vault on a
     * per-connection-group or per-user basis.
     */
    public static final Form HV_CONFIGURATION_FORM = new Form(
        "hv-config",
        Arrays.asList(new TextField(HV_URI_ATTRIBUTE),
                      new PasswordField(HV_TOKEN_ATTRIBUTE),
                      new TextField(HV_USERNAME_ATTRIBUTE),
                      new PasswordField(HV_PASSWORD_ATTRIBUTE)
        )
    );

    /**
     * All HV-specific attributes for users, connections, or connection groups,
     * organized by form.
     */
    public static final Collection<Form> HV_ATTRIBUTES = Collections.unmodifiableCollection(Arrays.asList(HV_CONFIGURATION_FORM));

    /**
     * The name of the attribute which can controls whether a HV user configuration
     * is enabled on a connection-by-connection basis.
     */
    public static final String HV_USER_CONFIG_ENABLED_ATTRIBUTE = "hv-user-config-enabled";

    /**
     * The string value used by HV attributes to represent the boolean value "true".
     */
    public static final String TRUTH_VALUE = "true";

    /**
     * All attributes related to configuring the HV vault on a per-connection basis.
     */
    public static final Form HV_CONNECTION_FORM = new Form(
        "hv-config",
        Arrays.asList(new BooleanField(HV_USER_CONFIG_ENABLED_ATTRIBUTE, TRUTH_VALUE))
    );

    /**
     * All HV-specific attributes for connections, organized by form.
     */
    public static final Collection<Form> HV_CONNECTION_ATTRIBUTES = Collections.unmodifiableCollection(Arrays.asList(HV_CONNECTION_FORM));

    @Override
    public Collection<Form> getConnectionAttributes() {
        return HV_CONNECTION_ATTRIBUTES;
    }

    @Override
    public Collection<Form> getUserAttributes() {
        return HV_ATTRIBUTES;
    }

    @Override
    public Collection<Form> getUserPreferenceAttributes() {
        return getUserAttributes();
    }

    @Override
    public Collection<Form> getConnectionGroupAttributes() {
        return HV_ATTRIBUTES;
    }
}
