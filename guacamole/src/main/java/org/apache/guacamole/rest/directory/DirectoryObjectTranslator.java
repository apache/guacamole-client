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

package org.apache.guacamole.rest.directory;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.Identifiable;

/**
 * Provides bidirectional conversion between REST-specific objects and the
 * internal objects defined by the Guacamole extension API.
 *
 * @param <InternalType>
 *     The type of object converted by this DirectoryObjectTranslator which is
 *     not necessarily intended for use in interchange.
 *
 * @param <ExternalType>
 *     The type of object used in interchange (ie: serialized or
 *     deserialized as JSON) between REST clients and resource implementations
 *     when representing the InternalType.
 */
public interface DirectoryObjectTranslator<InternalType extends Identifiable, ExternalType> {

    /**
     * Converts the given object to an object which is intended to be used in
     * interchange.
     *
     * @param object
     *     The object to convert for the sake of interchange.
     *
     * @return
     *     A new object containing the same data as the given internal object,
     *     but intended for use in interchange.
     *
     * @throws GuacamoleException
     *     If the provided object cannot be converted for any reason.
     */
    ExternalType toExternalObject(InternalType object)
            throws GuacamoleException;

    /**
     * Converts the given object to an object which is intended to be used
     * within the Guacamole extension API.
     *
     * @param object
     *     An object of the type intended for use in interchange, such as that
     *     produced by toExternalObject() or received from a user via REST.
     *
     * @return
     *     A new object containing the same data as the given external object,
     *     but intended for use within the Guacamole extension API.
     *
     * @throws GuacamoleException
     *     If the provided object cannot be converted for any reason.
     */
    InternalType toInternalObject(ExternalType object)
            throws GuacamoleException;

    /**
     * Overlays the changes indicated by the given external object, modifying
     * the given existing object from the Guacamole extension API.
     *
     * @param existingObject
     *     The existing object from the Guacamole extension API which should be
     *     modified.
     *
     * @param object
     *     The external object representing the modifications to the existing
     *     internal object.
     *
     * @throws GuacamoleException
     *     If the provided modifications cannot be applied for any reason.
     */
    void applyExternalChanges(InternalType existingObject, ExternalType object)
            throws GuacamoleException;

}
