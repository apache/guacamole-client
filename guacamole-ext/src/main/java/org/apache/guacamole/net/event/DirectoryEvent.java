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

package org.apache.guacamole.net.event;

import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.Identifiable;

/**
 * Abstract basis for events which involve a modification made to the objects
 * within a {@link Directory}.
 *
 * @param <ObjectType>
 *     The type of object stored within the {@link Directory}.
 */
public interface DirectoryEvent<ObjectType extends Identifiable>
        extends AuthenticationProviderEvent, UserEvent {

    /**
     * The types of directory operations that may be represented by a
     * DirectoryEvent.
     */
    public enum Operation {

        /**
         * An object was added to the {@link Directory}. The object added can
         * be accessed with {@link #getObject()}, and its identifier may be
         * obtained from {@link #getObjectIdentifier()}.
         */
        ADD,

        /**
         * An object was retrieved from a {@link Directory}. The object
         * retrieved can be accessed with {@link #getObject()}, and its
         * identifier may be obtained from {@link #getObjectIdentifier()}.
         */
        GET,

        /**
         * An existing object within a {@link Directory} was modified. The
         * modified object can be accessed with {@link #getObject()}, and its
         * identifier may be obtained from {@link #getObjectIdentifier()}.
         */
        UPDATE,

        /**
         * An existing object within a {@link Directory} was deleted/removed.
         * The identifier of the object that was deleted may be obtained from
         * {@link #getObjectIdentifier()}. The full object that was deleted
         * will be made available via {@link #getObject()} if possible, but
         * this is not guaranteed for deletions.
         */
        REMOVE

    }

    /**
     * Returns the type of objects stored within the {@link Directory}
     * affected by the operation.
     *
     * @return
     *     The type of objects stored within the {@link Directory}.
     */
    Directory.Type getDirectoryType();

    /**
     * Returns the operation that was performed/attempted.
     *
     * @return
     *     The operation that was performed or attempted.
     */
    Operation getOperation();

    /**
     * Returns the identifier of the object affected by the operation. If the
     * object was just created, this will be the identifier of the new object.
     *
     * @return
     *     The identifier of the object affected by the operation.
     */
    String getObjectIdentifier();

    /**
     * Returns the object affected by the operation, if available. For
     * deletions, there is no guarantee that the affected object will be
     * available within this event. If the object is not available, null is
     * returned.
     *
     * @return
     *     The object affected by the operation performed, or null if that
     *     object is not available in the context of this event.
     */
    ObjectType getObject();

}
