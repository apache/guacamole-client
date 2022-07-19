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

import java.util.List;
import java.util.Map;

import org.apache.guacamole.net.auth.DelegatingUser;
import org.apache.guacamole.net.auth.User;

import com.google.common.collect.Maps;

/**
 * A User that explicitly adds a blank entry for any defined
 * KSM user attributes.
 */
public class KsmUser extends DelegatingUser {

    /**
     * The names of all user attributes defined for the vault.
     */
    private List<String> userAttributeNames;

    /**
     * Create a new Vault user wrapping the provided User record. Any
     * attributes defined in the provided user attribute forms will have empty
     * values automatically populated when getAttributes() is called.
     *
     * @param user
     *     The user record to wrap.
     *
     * @param userAttributeNames
     *     The names of all user attributes to automatically expose.
     */
    KsmUser(User user, List<String> userAttributeNames) {

        super(user);
        this.userAttributeNames = userAttributeNames;

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
        Map<String, String> attributeMap = Maps.newHashMap(super.getAttributes());

        // Add every defined attribute
        userAttributeNames.forEach(
                attributeName -> attributeMap.putIfAbsent(attributeName, null));

        return attributeMap;
    }

}
