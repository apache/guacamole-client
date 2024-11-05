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

package org.apache.guacamole.net.auth;

import java.util.Collections;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.environment.LocalEnvironment;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;
import org.apache.guacamole.net.auth.permission.SystemPermissionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation of UserGroup which provides default implementations of
 * most functions.
 */
public abstract class AbstractUserGroup extends AbstractIdentifiable implements UserGroup {

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUserGroup.class);
    
    /**
     * The server environment in which this Guacamole Client instance is
     * running.
     */
    private final Environment environment = LocalEnvironment.getInstance();
    
    /**
     * {@inheritDoc}
     *
     * <p>This implementation simply an immutable, empty map. Implementations
     * that wish to expose custom attributes should override this function.
     */
    @Override
    public Map<String, String> getAttributes() {
        return Collections.emptyMap();
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation simply ignores all attributes given.
     * Implementations that wish to support modification of custom attributes
     * should override this function.
     */
    @Override
    public void setAttributes(Map<String, String> attributes) {
        // Ignore all attributes by default
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation simply an immutable, empty permission set.
     * Implementations that wish to expose permissions should override this
     * function.
     */
    @Override
    public SystemPermissionSet getSystemPermissions()
            throws GuacamoleException {
        return SystemPermissionSet.EMPTY_SET;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation simply an immutable, empty permission set.
     * Implementations that wish to expose permissions should override this
     * function.
     */
    @Override
    public ObjectPermissionSet getConnectionPermissions()
            throws GuacamoleException {
        return ObjectPermissionSet.EMPTY_SET;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation simply an immutable, empty permission set.
     * Implementations that wish to expose permissions should override this
     * function.
     */
    @Override
    public ObjectPermissionSet getConnectionGroupPermissions()
            throws GuacamoleException {
        return ObjectPermissionSet.EMPTY_SET;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation simply an immutable, empty permission set.
     * Implementations that wish to expose permissions should override this
     * function.
     */
    @Override
    public ObjectPermissionSet getUserPermissions()
            throws GuacamoleException {
        return ObjectPermissionSet.EMPTY_SET;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation simply an immutable, empty permission set.
     * Implementations that wish to expose permissions should override this
     * function.
     */
    @Override
    public ObjectPermissionSet getUserGroupPermissions()
            throws GuacamoleException {
        return ObjectPermissionSet.EMPTY_SET;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation simply an immutable, empty permission set.
     * Implementations that wish to expose permissions should override this
     * function.
     */
    @Override
    public ObjectPermissionSet getActiveConnectionPermissions()
            throws GuacamoleException {
        return ObjectPermissionSet.EMPTY_SET;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation simply an immutable, empty permission set.
     * Implementations that wish to expose permissions should override this
     * function.
     */
    @Override
    public ObjectPermissionSet getSharingProfilePermissions() {
        return ObjectPermissionSet.EMPTY_SET;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation simply an immutable, empty related object set.
     * Implementations that wish to expose group membership should override
     * this function.
     */
    @Override
    public RelatedObjectSet getUserGroups() throws GuacamoleException {
        return RelatedObjectSet.EMPTY_SET;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation simply an immutable, empty related object set.
     * Implementations that wish to expose group membership should override
     * this function.
     */
    @Override
    public RelatedObjectSet getMemberUsers() throws GuacamoleException {
        return RelatedObjectSet.EMPTY_SET;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation simply an immutable, empty related object set.
     * Implementations that wish to expose group membership should override
     * this function.
     */
    @Override
    public RelatedObjectSet getMemberUserGroups() throws GuacamoleException {
        return RelatedObjectSet.EMPTY_SET;
    }
    
    @Override
    public boolean isCaseSensitive() {
        try {
            return environment.getCaseSensitivity().caseSensitiveGroupNames();
        }
        catch (GuacamoleException e) {
            LOGGER.warn("Unable to retrieve server configuration, group names "
                      + "will default to case-sensitive.");
            LOGGER.debug("Received an exception attempting to retrieve the "
                       + "property for group name case sensitivity, group names"
                       + "will be treated as case-sensitive.", e);
            return true;
        }
    }

}
