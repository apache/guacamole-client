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

import java.util.List;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.net.auth.permission.ObjectPermission;

/**
 * Provides access to the entire tree of connection groups and their
 * connections.
 *
 * @author Michael Jumper
 */
public class ConnectionGroupTree {

    /**
     * The root connection group.
     */
    private final APIConnectionGroup root;

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
        this.root = new APIConnectionGroup(root);

        // STUB
        
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
        return root;
    }

}
