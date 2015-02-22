/*
 * Copyright (C) 2015 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.glyptodon.guacamole.net.basic.rest.connectiongroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;
import org.glyptodon.guacamole.net.basic.rest.connection.APIConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides access to the entire tree of connection groups and their
 * connections.
 *
 * @author Michael Jumper
 */
public class ConnectionGroupTree {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(ConnectionGroupTree.class);
    
    /**
     * The root connection group.
     */
    private final ConnectionGroup root;
    
    /**
     * The root connection group as an APIConnectionGroup.
     */
    private final APIConnectionGroup rootAPIGroup;

    /**
     * All connection groups that have been retrieved, stored by their
     * identifiers.
     */
    private final Map<String, APIConnectionGroup> retrievedGroups =
            new HashMap<String, APIConnectionGroup>();

    /**
     * Adds each of the provided connections to the current tree as children
     * of their respective parents. The parent connection groups must already
     * be added.
     *
     * @param connections
     *     The connections to add to the tree.
     * 
     * @throws GuacamoleException
     *     If an error occurs while adding the connection to the tree.
     */
    private void addConnections(Collection<Connection> connections)
        throws GuacamoleException {

        // Add each connection to the tree
        for (Connection connection : connections) {

            // Retrieve the connection's parent group
            APIConnectionGroup parent = retrievedGroups.get(connection.getParentIdentifier());
            if (parent != null) {

                Collection<APIConnection> children = parent.getChildConnections();
                
                // Create child collection if it does not yet exist
                if (children == null) {
                    children = new ArrayList<APIConnection>();
                    parent.setChildConnections(children);
                }

                // Add child
                children.add(new APIConnection(connection));
                
            }

            // Warn of internal consistency issues
            else
                logger.debug("Connection \"{}\" cannot be added to the tree: parent \"{}\" does not actually exist.",
                        connection.getIdentifier(),
                        connection.getParentIdentifier());

        } // end for each connection
        
    }
    
    /**
     * Adds each of the provided connection groups to the current tree as
     * children of their respective parents. The parent connection groups must
     * already be added.
     *
     * @param connectionGroups
     *     The connection groups to add to the tree.
     */
    private void addConnectionGroups(Collection<ConnectionGroup> connectionGroups) {

        // Add each connection group to the tree
        for (ConnectionGroup connectionGroup : connectionGroups) {

            // Retrieve the connection group's parent group
            APIConnectionGroup parent = retrievedGroups.get(connectionGroup.getParentIdentifier());
            if (parent != null) {

                Collection<APIConnectionGroup> children = parent.getChildConnectionGroups();
                
                // Create child collection if it does not yet exist
                if (children == null) {
                    children = new ArrayList<APIConnectionGroup>();
                    parent.setChildConnectionGroups(children);
                }

                // Add child
                APIConnectionGroup apiConnectionGroup = new APIConnectionGroup(connectionGroup);
                retrievedGroups.put(connectionGroup.getIdentifier(), apiConnectionGroup);
                children.add(apiConnectionGroup);
                
            }

            // Warn of internal consistency issues
            else
                logger.debug("Connection group \"{}\" cannot be added to the tree: parent \"{}\" does not actually exist.",
                        connectionGroup.getIdentifier(),
                        connectionGroup.getParentIdentifier());

        } // end for each connection group
        
    }
    
    /**
     * Adds all descendants of the given parent groups to their corresponding
     * parents already stored under root.
     *
     * @param parents
     *     The parents whose descendants should be added to the tree.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the descendants.
     */
    private void addDescendants(Collection<ConnectionGroup> parents)
        throws GuacamoleException {

        // If no parents, nothing to do
        if (parents.isEmpty())
            return;

        Collection<String> childConnectionIdentifiers = new ArrayList<String>();
        Collection<String> childConnectionGroupIdentifiers = new ArrayList<String>();
        
        // Build lists of identifiers for retrieval
        for (ConnectionGroup parent : parents) {
            childConnectionIdentifiers.addAll(parent.getConnectionDirectory().getIdentifiers());
            childConnectionGroupIdentifiers.addAll(parent.getConnectionGroupDirectory().getIdentifiers());
        }

        // Retrieve child connections
        if (!childConnectionIdentifiers.isEmpty()) {
            Collection<Connection> childConnections = root.getConnectionDirectory().getAll(childConnectionIdentifiers);
            addConnections(childConnections);
        }

        // Retrieve child connection groups
        if (!childConnectionGroupIdentifiers.isEmpty()) {
            Collection<ConnectionGroup> childConnectionGroups = root.getConnectionGroupDirectory().getAll(childConnectionGroupIdentifiers);
            addConnectionGroups(childConnectionGroups);
            addDescendants(childConnectionGroups);
        }

    }
    
    /**
     * Creates a new connection group tree using the given connection group as
     * the tree root.
     *
     * @param root
     *     The connection group to use as the root of this connection group
     *     tree.
     * 
     * @param permissions
     *     If specified and non-empty, limit the contents of the tree to only
     *     those connections for which the current user has any of the given
     *     permissions. Otherwise, all visible connections are returned.
     *     Connection groups are unaffected by this parameter.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the tree of connection groups
     *     and their descendants.
     */
    public ConnectionGroupTree(ConnectionGroup root,
            List<ObjectPermission.Type> permissions) throws GuacamoleException {

        // Store root of tree
        this.root = root;
        this.rootAPIGroup = new APIConnectionGroup(root);
        retrievedGroups.put(root.getIdentifier(), this.rootAPIGroup);

        // Add all descendants
        addDescendants(Collections.singleton(root));
        
    }

    /**
     * Returns the entire connection group tree as an APIConnectionGroup. The
     * returned APIConnectionGroup is the root group and will contain all
     * descendant connection groups and connections, arranged hierarchically.
     *
     * @return
     *     The root connection group, containing the entire connection group
     *     tree and all connections.
     */
    public APIConnectionGroup getRootAPIConnectionGroup() {
        return rootAPIGroup;
    }

}
