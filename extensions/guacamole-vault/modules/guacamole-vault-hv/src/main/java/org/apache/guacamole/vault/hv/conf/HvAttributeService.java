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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.form.BooleanField;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.form.TextField;
import org.apache.guacamole.language.TranslatableGuacamoleClientException;
import org.apache.guacamole.vault.conf.VaultAttributeService;

@Singleton
public class HvAttributeService implements VaultAttributeService {

    /**
     * The name of the attribute which can contain a HV configuration blob
     * associated with either a connection group or user.
     */
    public static final String HV_CONFIGURATION_ATTRIBUTE = "hv-config";

    /**
     * The HV configuration attribute contains sensitive information, so it
     * should not be exposed through the directory. Instead, if a value is
     * set on the attributes of an object, the following value will be exposed
     * in its place, and correspondingly the underlying value will not be
     * changed if this value is provided to an update call.
     */
    public static final String HV_ATTRIBUTE_PLACEHOLDER_VALUE = "**********";

    /**
     * All attributes related to configuring the HV vault on a
     * per-connection-group or per-user basis.
     */
    public static final Form HV_CONFIGURATION_FORM = new Form(
        "hv-config",
        Arrays.asList(new TextField(HV_CONFIGURATION_ATTRIBUTE))
    );

    /**
     * All HV-specific attributes for users, connections, or connection groups, organized by form.
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

    /**
     * Sanitize the value of the provided HV config attribute. If the provided
     * config value is non-empty, it will be replaced with the placeholder
     * value to avoid leaking sensitive information. If the value is empty, it
     * will be replaced by `null`.
     *
     * @param hvAttributeValue
     *    The HV configuration attribute value to sanitize.
     *
     * @return
     *    The sanitized HV configuration attribute value, stripped of any
     *    sensitive information.
     */
    public static String sanitizeHvAttributeValue(String hvAttributeValue) {

        // Any non-empty values may contain sensitive information, and should
        // be replaced by the safe placeholder value
        if (hvAttributeValue != null && !hvAttributeValue.trim().isEmpty())
            return HV_ATTRIBUTE_PLACEHOLDER_VALUE;

        // If the configuration value is empty, expose a null value
        else
            return null;

    }

    public static Map<String, String> processAttributes(
        Map<String, String> attributes
    ) throws GuacamoleException {
        // Get the value of the HV config attribute in the provided map
        String hvConfigValue = attributes.get(HvAttributeService.HV_CONFIGURATION_ATTRIBUTE);

        // If the placeholder value was provided, do not update the attribute
        if (HvAttributeService.HV_ATTRIBUTE_PLACEHOLDER_VALUE.equals(hvConfigValue)) {
            // Remove the attribute from the map so it won't be updated
            attributes = new HashMap<>(attributes);
            attributes.remove(HvAttributeService.HV_CONFIGURATION_ATTRIBUTE);
        }

        return attributes;
    }
}
