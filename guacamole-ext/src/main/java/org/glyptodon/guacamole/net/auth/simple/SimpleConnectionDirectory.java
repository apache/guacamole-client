/*
 * Copyright (C) 2013 Glyptodon LLC
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

package org.glyptodon.guacamole.net.auth.simple;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.glyptodon.guacamole.net.auth.Connection;

/**
 * An extremely simple read-only implementation of a Directory of
 * GuacamoleConfigurations which provides access to a pre-defined Map of
 * GuacamoleConfigurations.
 *
 * @author Michael Jumper
 */
public class SimpleConnectionDirectory extends SimpleDirectory<Connection> {

    /**
     * The Map of Connections to provide access to.
     */
    private final Map<String, Connection> connections =
            new HashMap<String, Connection>();

    /**
     * Creates a new SimpleConnectionDirectory which provides access to the
     * connections contained within the given Map.
     *
     * @param connections
     *     A Collection of all connections that should be present in this
     *     connection directory.
     */
    public SimpleConnectionDirectory(Collection<Connection> connections) {

        // Add all given connections
        for (Connection connection : connections)
            this.connections.put(connection.getIdentifier(), connection);

        // Use the connection map to back the underlying directory 
        super.setObjects(this.connections);

    }

    /**
     * An internal method for modifying the Connections in this Directory.
     * Returns the previous connection for the given identifier, if found.
     *
     * @param connection The connection to add or update the Directory with.
     * @return The previous connection for the connection identifier, if found.
     */
    public Connection putConnection(Connection connection) {
        return connections.put(connection.getIdentifier(), connection);
    }

    /**
     * An internal method for removing a Connection from this Directory.
     * @param identifier The identifier of the Connection to remove.
     * @return The previous connection for the given identifier, if found.
     */
    public Connection removeConnection(String identifier) {
        return connections.remove(identifier);
    }

}
