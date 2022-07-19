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

import org.apache.guacamole.net.auth.DelegatingConnection;
import org.apache.guacamole.net.auth.Connection;

import com.google.common.collect.Maps;

/**
 * A Connection that explicitly adds a blank entry for any defined
 * KSM connection attributes.
 */
public class KsmConnection extends DelegatingConnection {

    /**
     * The names of all connection attributes defined for the vault.
     */
    private List<String> connectionAttributeNames;

    /**
     * Create a new Vault connection wrapping the provided Connection record. Any
     * attributes defined in the provided connection attribute forms will have empty
     * values automatically populated when getAttributes() is called.
     *
     * @param connection
     *     The connection record to wrap.
     *
     * @param connectionAttributeNames
     *     The names of all connection attributes to automatically expose.
     */
    KsmConnection(Connection connection, List<String> connectionAttributeNames) {

        super(connection);
        this.connectionAttributeNames = connectionAttributeNames;

    }

    /**
     * Return the underlying wrapped connection record.
     *
     * @return
     *     The wrapped connection record.
     */
    Connection getUnderlyingConnection() {
        return getDelegateConnection();
    }

    @Override
    public Map<String, String> getAttributes() {

        // Make a copy of the existing map
        Map<String, String> attributeMap = Maps.newHashMap(super.getAttributes());

        // Add every defined attribute
        connectionAttributeNames.forEach(
                attributeName -> attributeMap.putIfAbsent(attributeName, null));

        return attributeMap;
    }

}
