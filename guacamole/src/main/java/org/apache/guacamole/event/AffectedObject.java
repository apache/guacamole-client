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

package org.apache.guacamole.event;

import org.apache.guacamole.net.auth.Nameable;
import org.apache.guacamole.net.event.DirectoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loggable representation of the object affected by an operation.
 */
public class AffectedObject implements LoggableDetail {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(AffectedObject.class);
    
    /**
     * The event representing the requested operation.
     */
    private final DirectoryEvent<?> event;

    /**
     * Creates a new AffectedObject representing the object affected by the
     * operation described by the given event.
     *
     * @param event
     *     The event representing the operation.
     */
    public AffectedObject(DirectoryEvent<?> event) {
        this.event = event;
    }

    @Override
    public String toString() {

        Object object = event.getObject();
        String identifier = event.getObjectIdentifier();

        String objectType;
        String name = null; // Not all objects have names

        // Obtain name of object (if applicable and available)
        if (object instanceof Nameable) {
            try {
                name = ((Nameable) object).getName();
            }
            catch (RuntimeException | Error e) {
                logger.debug("Name of object \"{}\" could not be retrieved.", identifier, e);
            }
        }

        // Determine type of object
        switch (event.getDirectoryType()) {

            // Active connections
            case ACTIVE_CONNECTION:
                objectType = "active connection";
                break;

            // Connections
            case CONNECTION:
                objectType = "connection";
                break;

            // Connection groups
            case CONNECTION_GROUP:
                objectType = "connection group";
                break;

            // Sharing profiles
            case SHARING_PROFILE:
                objectType = "sharing profile";
                break;

            // Users
            case USER:
                objectType = "user";
                break;

            // User groups
            case USER_GROUP:
                objectType = "user group";
                break;

            // Unknown
            default:
                objectType = (object != null) ? object.getClass().toString() : "an unknown object";
                
        }

        // Describe at least the type of the object and its identifier,
        // including the name of the object, as well, if available
        if (identifier != null) {
            if (name != null)
                return objectType + " \"" + identifier + "\" (currently named \"" + name + "\")";
            else
                return objectType + " \"" + identifier + "\"";
        }
        else
            return objectType;

    }
    
}
