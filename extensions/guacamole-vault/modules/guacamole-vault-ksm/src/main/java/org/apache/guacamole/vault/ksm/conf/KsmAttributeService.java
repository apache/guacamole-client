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

package org.apache.guacamole.vault.ksm.conf;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.form.BooleanField;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.form.TextField;
import org.apache.guacamole.vault.conf.VaultAttributeService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * A service that exposes KSM-specific attributes, allowing setting KSM
 * configuration through the admin interface.
 */
@Singleton
public class KsmAttributeService implements VaultAttributeService {


    @Inject
    private KsmConfigurationService configurationService;

    /**
     * The name of the attribute which can contain a KSM configuration blob
     * associated with either a connection group or user.
     */
    public static final String KSM_CONFIGURATION_ATTRIBUTE = "ksm-config";

    /**
     * All attributes related to configuring the KSM vault on a
     * per-connection-group or per-user basis.
     */
    public static final Form KSM_CONFIGURATION_FORM = new Form("ksm-config",
            Arrays.asList(new TextField(KSM_CONFIGURATION_ATTRIBUTE)));

    /**
     * All KSM-specific attributes for users or connection groups, organized by form.
     */
    public static final Collection<Form> KSM_ATTRIBUTES =
            Collections.unmodifiableCollection(Arrays.asList(KSM_CONFIGURATION_FORM));

    /**
     * The name of the attribute which can controls whether a KSM user configuration
     * is enabled on a connection-by-connection basis.
     */
    public static final String KSM_USER_CONFIG_ENABLED_ATTRIBUTE = "ksm-user-config-enabled";

    /**
     * The string value used by KSM attributes to represent the boolean value "true".
     */
    public static final String TRUTH_VALUE = "true";

    /**
     * All attributes related to configuring the KSM vault on a per-connection basis.
     */
    public static final Form KSM_CONNECTION_FORM = new Form("ksm-config",
            Arrays.asList(new BooleanField(KSM_USER_CONFIG_ENABLED_ATTRIBUTE, TRUTH_VALUE)));

    /**
     * All KSM-specific attributes for connections, organized by form.
     */
    public static final Collection<Form> KSM_CONNECTION_ATTRIBUTES =
            Collections.unmodifiableCollection(Arrays.asList(KSM_CONNECTION_FORM));

    @Override
    public Collection<Form> getConnectionAttributes() {
        return KSM_CONNECTION_ATTRIBUTES;
    }

    @Override
    public Collection<Form> getConnectionGroupAttributes() {
        return KSM_ATTRIBUTES;
    }

    @Override
    public Collection<Form> getUserAttributes() {
        return KSM_ATTRIBUTES;
    }

    @Override
    public Collection<Form> getUserPreferenceAttributes() {

        try {

            // Expose the user attributes IFF user-level KSM configuration is enabled
            return configurationService.getAllowUserConfig() ? KSM_ATTRIBUTES : Collections.emptyList();

        } catch (GuacamoleException e) {

            // If the configuration can't be parsed, default to not exposing the attribute
            return Collections.emptyList();
        }
    }


}
