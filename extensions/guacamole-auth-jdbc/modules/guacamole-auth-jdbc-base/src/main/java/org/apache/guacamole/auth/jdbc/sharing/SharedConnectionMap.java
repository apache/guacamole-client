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

package org.apache.guacamole.auth.jdbc.sharing;

/**
 * Represents a mapping between share keys and the Guacamole connection being
 * shared.
 * 
 * @author Michael Jumper
 */
public interface SharedConnectionMap {

    /**
     * Associates the given share key with a SharedConnectionDefinition,
     * allowing the connection it describes to be accessed by users having the
     * share key.
     *
     * @param key
     *     The share key to use to share the connection described by the given
     *     SharedConnectionDefinition.
     *
     * @param definition
     *     The SharedConnectionDefinition describing the connection being
     *     shared via the given share key.
     */
    public void put(String key, SharedConnectionDefinition definition);

    /**
     * Retrieves the connection definition associated with the given share key.
     * If no such share key exists, null is returned.
     *
     * @param key
     *     The share key associated with the connection definition to be
     *     returned.
     *
     * @return
     *     The connection definition associated with the given share key, or
     *     null if no such share key exists.
     */
    public SharedConnectionDefinition get(String key);

    /**
     * Invalidates given share key, if it exists, returning the connection
     * definition previously associated with that key. If no such share key
     * exists, this function has no effect, and null is returned.
     *
     * @param key
     *     The share key associated with the connection definition to be
     *     removed.
     *
     * @return
     *     The connection definition previously associated with the given
     *     share key, or null if no such share key exists and no connection was
     *     removed.
     */
    public SharedConnectionDefinition remove(String key);

}
