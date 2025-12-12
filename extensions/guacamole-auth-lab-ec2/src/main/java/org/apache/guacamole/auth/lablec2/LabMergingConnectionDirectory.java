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
package org.apache.guacamole.auth.lablec2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.Directory;

/**
 * Directory wrapper which merges a dynamically-generated lab connection with
 * an underlying directory.
 */
public class LabMergingConnectionDirectory implements Directory<Connection> {

    /**
     * The directory being wrapped.
     */
    private final Directory<Connection> base;

    /**
     * The lab connection to expose alongside the wrapped directory.
     */
    private final Connection labConnection;

    /**
     * Create a new merging directory.
     *
     * @param base
     *     The original connection directory.
     *
     * @param labConnection
     *     The per-user lab connection to add.
     */
    public LabMergingConnectionDirectory(Directory<Connection> base,
            Connection labConnection) {
        this.base = base;
        this.labConnection = labConnection;
    }

    @Override
    public Connection get(String identifier) throws GuacamoleException {
        if (labConnection.getIdentifier().equals(identifier))
            return labConnection;

        return base.get(identifier);
    }

    @Override
    public Collection<Connection> getAll(Collection<String> identifiers)
            throws GuacamoleException {

        // Filter out lab connection ID before passing to base directory
        // to avoid potential errors if base directory is strict
        Set<String> baseIdentifiers = new HashSet<>(identifiers);
        boolean includeLab = baseIdentifiers.remove(labConnection.getIdentifier());

        List<Connection> connections = new ArrayList<>(
                base.getAll(baseIdentifiers));

        if (includeLab)
            connections.add(labConnection);

        return connections;
    }

    @Override
    public Set<String> getIdentifiers() throws GuacamoleException {
        Set<String> ids = new HashSet<>(base.getIdentifiers());
        ids.add(labConnection.getIdentifier());
        return ids;
    }

    @Override
    public void add(Connection object) throws GuacamoleException {
        base.add(object);
    }

    @Override
    public void update(Connection object) throws GuacamoleException {
        base.update(object);
    }

    @Override
    public void remove(String identifier) throws GuacamoleException {
        base.remove(identifier);
    }

}
