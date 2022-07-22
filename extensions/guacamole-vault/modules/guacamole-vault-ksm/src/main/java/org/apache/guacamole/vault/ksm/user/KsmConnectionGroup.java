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

package org.apache.guacamole.vault.ksm.user;

import java.util.HashMap;
import java.util.Map;

import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.DelegatingConnectionGroup;
import org.apache.guacamole.vault.ksm.conf.KsmAttributeService;

 /**
  * A KSM-specific connection group implementation that always exposes
  * the KSM_CONFIGURATION_ATTRIBUTE attribute, even when no value is set.
  * This ensures that the attribute will always show up in the UI, even
  * for connection groups that don't already have it set.
  */
 public class KsmConnectionGroup extends DelegatingConnectionGroup {

    /**
     * Create a new KsmConnectionGroup instance, wrapping the provided
     * ConnectionGroup.
     *
     * @param connectionGroup
     *     The ConnectionGroup instance to wrap.
     */
    public KsmConnectionGroup(ConnectionGroup connectionGroup) {

        // Wrap the provided connection group
        super(connectionGroup);
    }

    @Override
    public Map<String, String> getAttributes() {

        // All attributes defined on the underlying connection group
        Map<String, String> attributes = super.getAttributes();

        // If the attribute is already present, there's no need to add it - return
        // the existing attributes as they are
        if (attributes.containsKey(KsmAttributeService.KSM_CONFIGURATION_ATTRIBUTE))
            return attributes;

        // Make a copy of the existing attributes and add KSM_CONFIGURATION_ATTRIBUTE
        attributes = new HashMap<>(attributes);
        attributes.put(KsmAttributeService.KSM_CONFIGURATION_ATTRIBUTE, null);
        return attributes;

    }

    /**
     * Return the underlying ConnectionGroup that's wrapped by this KsmConnectionGroup.
     *
     * @return
     *     The underlying ConnectionGroup that's wrapped by this KsmConnectionGroup.
     */
    ConnectionGroup getUnderlyConnectionGroup() {
        return getDelegateConnectionGroup();
    }

 }