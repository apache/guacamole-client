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
import java.util.HashMap;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.DelegatingConnectionGroup;
import org.apache.guacamole.vault.hv.conf.HvAttributeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A HV-specific connection group implementation that always exposes
 * the HV_CONFIGURATION_ATTRIBUTE attribute, even when no value is set.
 * The value of the attribute will be sanitized if non-empty. This ensures
 * that the attribute will always show up in the UI, even for connection
 * groups that don't already have it set, and that any sensitive information
 * in the attribute value will not be exposed.
 */
public class HvConnectionGroup extends DelegatingConnectionGroup {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(HvConnectionGroup.class);

    /**
     * Create a new HvConnectionGroup wrapping the provided ConnectionGroup record.
     *
     * @param connectionGroup
     *     The ConnectionGroup record to wrap.
     */
    HvConnectionGroup(ConnectionGroup connectionGroup) {
        super(connectionGroup);
    }

    /**
     * Return the underlying wrapped connection group record.
     *
     * @return
     *     The wrapped connection group record.
     */
    ConnectionGroup getUnderlyingConnectionGroup() {
        return getDelegateConnectionGroup();
    }

    /**
     * Return the underlying ConnectionGroup that's wrapped by this HvConnectionGroup.
     *
     * @return
     *     The underlying ConnectionGroup that's wrapped by this HvConnectionGroup.
     */
    ConnectionGroup getUnderlyConnectionGroup() {
        return getDelegateConnectionGroup();
    }

    @Override
    public Map<String, String> getAttributes() {

        // Make a copy of the existing map
        Map<String, String> attributes = Maps.newHashMap(super.getAttributes());

        // Sanitize the HV configuration attribute, and ensure the attribute
        // is always present
        attributes.put(
            HvAttributeService.HV_CONFIGURATION_ATTRIBUTE,
            HvAttributeService.sanitizeHvAttributeValue(
                attributes.get(HvAttributeService.HV_CONFIGURATION_ATTRIBUTE)
            )
        );

        return attributes;
    }

    @Override
    public void setAttributes(Map<String, String> attributes) {
        try {
            super.setAttributes(
                HvAttributeService.processAttributes(attributes)
            );
        }
        catch (GuacamoleException e) {
            logger.warn("HvConnectionGroup setAttributes failed");
        }
    }

}