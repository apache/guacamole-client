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

import java.util.Collection;
import java.util.Collections;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.net.auth.simple.SimpleActivityRecordSet;
import org.apache.guacamole.net.auth.simple.SimpleConnectionGroup;
import org.apache.guacamole.net.auth.simple.SimpleDirectory;

/**
 * Base implementation of UserContext which provides default implementations of
 * most functions. Implementations must provide their own {@link self()} and
 * {@link getAuthenticationProvider()}, but otherwise need only override an
 * implemented function if they wish to actually implement the functionality
 * defined for that function by the UserContext interface.
 */
public abstract class AbstractUserContext implements UserContext {

    /**
     * The unique identifier that will be used for the root connection group if
     * {@link #getRootConnectionGroup()} is not overridden.
     */
    protected static final String DEFAULT_ROOT_CONNECTION_GROUP = "ROOT";

    @Override
    public Object getResource() throws GuacamoleException {
        return null;
    }

    @Override
    public Directory<User> getUserDirectory() throws GuacamoleException {
        return new SimpleDirectory<User>(self());
    }

    @Override
    public Directory<Connection> getConnectionDirectory()
            throws GuacamoleException {
        return new SimpleDirectory<Connection>();
    }

    @Override
    public Directory<ConnectionGroup> getConnectionGroupDirectory()
            throws GuacamoleException {
        return new SimpleDirectory<ConnectionGroup>(getRootConnectionGroup());
    }

    @Override
    public Directory<ActiveConnection> getActiveConnectionDirectory()
            throws GuacamoleException {
        return new SimpleDirectory<ActiveConnection>();
    }

    @Override
    public Directory<SharingProfile> getSharingProfileDirectory()
            throws GuacamoleException {
        return new SimpleDirectory<SharingProfile>();
    }

    @Override
    public ActivityRecordSet<ConnectionRecord> getConnectionHistory()
            throws GuacamoleException {
        return new SimpleActivityRecordSet<ConnectionRecord>();
    }

    @Override
    public ActivityRecordSet<ActivityRecord> getUserHistory()
            throws GuacamoleException {
        return new SimpleActivityRecordSet<ActivityRecord>();
    }

    @Override
    public ConnectionGroup getRootConnectionGroup()
            throws GuacamoleException {
        return new SimpleConnectionGroup(
            DEFAULT_ROOT_CONNECTION_GROUP,
            DEFAULT_ROOT_CONNECTION_GROUP,
            getConnectionDirectory().getIdentifiers(),
            Collections.<String>emptySet()
        );
    }

    @Override
    public Collection<Form> getUserAttributes() {
        return Collections.<Form>emptyList();
    }

    @Override
    public Collection<Form> getConnectionAttributes() {
        return Collections.<Form>emptyList();
    }

    @Override
    public Collection<Form> getConnectionGroupAttributes() {
        return Collections.<Form>emptyList();
    }

    @Override
    public Collection<Form> getSharingProfileAttributes() {
        return Collections.<Form>emptyList();
    }

    @Override
    public void invalidate() {
    }

}
