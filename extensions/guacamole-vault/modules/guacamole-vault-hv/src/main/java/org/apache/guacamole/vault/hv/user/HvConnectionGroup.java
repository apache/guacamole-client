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
import java.util.Map;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.DelegatingConnectionGroup;
import org.apache.guacamole.vault.hv.conf.HvAttributeService;

/**
 * A HV-specific connection group implementation that always exposes
 * the HV_URI_ATTRIBUTE, HV_TOKEN_ATTRIBUTE, HV_USERNAME_ATTRIBUTE and
 * HV_PASSORD_ATTRIBUTE attributes, even when no value is set.
 */
public class HvConnectionGroup extends DelegatingConnectionGroup {

    /**
     * Create a new HvConnectionGroup wrapping the provided ConnectionGroup record.
     *
     * @param connectionGroup
     *     The ConnectionGroup record to wrap.
     */
    public HvConnectionGroup(final ConnectionGroup connectionGroup) {
        super(connectionGroup);
    }

    /**
     * Return the underlying wrapped connection group record.
     *
     * @return
     *     The wrapped connection group record.
     */
    public ConnectionGroup getUnderlyingConnectionGroup() {
        return getDelegateConnectionGroup();
    }

    @Override
    public Map<String, String> getAttributes() {

        // Make a copy of the existing map
        final Map<String, String> attributes = Maps.newHashMap(super.getAttributes());

        attributes.put(
            HvAttributeService.HV_URI_ATTRIBUTE,
            attributes.get(HvAttributeService.HV_URI_ATTRIBUTE)
        );
        attributes.put(
            HvAttributeService.HV_TOKEN_ATTRIBUTE,
                attributes.get(HvAttributeService.HV_TOKEN_ATTRIBUTE)
        );
        attributes.put(
            HvAttributeService.HV_USERNAME_ATTRIBUTE,
            attributes.get(HvAttributeService.HV_USERNAME_ATTRIBUTE)
        );
        attributes.put(
            HvAttributeService.HV_PASSWORD_ATTRIBUTE,
                attributes.get(HvAttributeService.HV_PASSWORD_ATTRIBUTE)
        );        
        return attributes;
    }   
}
