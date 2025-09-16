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

package org.apache.guacamole.vault.hv.user;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.DelegatingUser;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.vault.hv.conf.HvAttributeService;
import org.apache.guacamole.vault.hv.conf.HvConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A HV-specific user implementation that exposes the
 * HV_CONFIGURATION_ATTRIBUTE attribute even if no value is set. but only
 * if user-specific HV configuration is enabled. The value of the attribute
 * will be sanitized if non-empty. This ensures that the attribute will always
 * show up in the UI when the feature is enabled, even for users that don't
 * already have it set, and that any sensitive information in the attribute
 * value will not be exposed.
 */
public class HvUser extends DelegatingUser {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(HvUser.class);

    /**
     * Service for retrieving HV configuration details.
     */
    @Inject
    private HvConfigurationService configurationService;

    /**
     * Create a new Hvuser wrapping the provided User record.
     *
     * @param user
     *     The User record to wrap.
     */
    @AssistedInject
    HvUser(@Assisted User user) {
        super(user);
    }

    /**
     * Return the underlying wrapped user record.
     *
     * @return
     *     The wrapped user record.
     */
    User getUnderlyingUser() {
        return getDelegateUser();
    }

    @Override
    public Map<String, String> getAttributes() {

        // Make a copy of the existing map
        Map<String, String> attributes = Maps.newHashMap(super.getAttributes());

        // Figure out if user-level HV config is enabled
        boolean userHvConfigEnabled = false;
        try {
            userHvConfigEnabled = configurationService.getAllowUserConfig();
        } catch (GuacamoleException e) {

            logger.warn(
                    "Disabling user HV config due to exception: {}"
                    , e.getMessage());
            logger.debug("Error looking up if user HV config is enabled.", e);

        }

        // If user-specific HV configuration is not enabled, do not expose the
        // attribute at all
        if (!userHvConfigEnabled)
            attributes.remove(HvAttributeService.HV_CONFIGURATION_ATTRIBUTE);

        else
            // Sanitize the HV configuration attribute, and ensure the attribute
            // is always present
            attributes.put(
                    HvAttributeService.HV_CONFIGURATION_ATTRIBUTE,
                    HvAttributeService.sanitizeHvAttributeValue(
                        attributes.get(HvAttributeService.HV_CONFIGURATION_ATTRIBUTE)));

        return attributes;
    }

    @Override
    public void setAttributes(Map<String, String> attributes) {
        try {
            super.setAttributes(
                HvAttributeService.processAttributes(attributes)
            );
        } catch (GuacamoleException e) {
            logger.warn("HvUser setAttributes failed");
        }
    }

}