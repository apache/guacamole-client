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
import java.util.HashSet;
import java.util.Set;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.glyptodon.guacamole.net.auth.AbstractConnectionGroup;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.protocol.GuacamoleClientInformation;

/**
 * An extremely simple read-only implementation of a ConnectionGroup which
 * returns the connection and connection group identifiers it was constructed
 * with. Load balancing across this connection group is not allowed.
 * 
 * @author James Muehlner
 */
public class SimpleConnectionGroup extends AbstractConnectionGroup {

    /**
     * The identifiers of all connections in this group.
     */
    private final Set<String> connectionIdentifiers;

    /**
     * The identifiers of all connection groups in this group.
     */
    private final Set<String> connectionGroupIdentifiers;
    
    /**
     * Creates a new SimpleConnectionGroup having the given name and identifier
     * which will expose the given contents.
     * 
     * @param name
     *     The name to associate with this connection group.
     *
     * @param identifier
     *     The identifier to associate with this connection group.
     *
     * @param connectionIdentifiers
     *     The connection identifiers to expose when requested.
     *
     * @param connectionGroupIdentifiers
     *     The connection group identifiers to expose when requested.
     */
    public SimpleConnectionGroup(String name, String identifier,
            Collection<String> connectionIdentifiers, 
            Collection<String> connectionGroupIdentifiers) {

        // Set name
        setName(name);

        // Set identifier
        setIdentifier(identifier);
        
        // Set group type
        setType(ConnectionGroup.Type.ORGANIZATIONAL);

        // Populate contents
        this.connectionIdentifiers = new HashSet<String>(connectionIdentifiers);
        this.connectionGroupIdentifiers = new HashSet<String>(connectionGroupIdentifiers);

    }

    @Override
    public int getActiveConnections() {
        return 0;
    }

    @Override
    public Set<String> getConnectionIdentifiers() {
        return connectionIdentifiers;
    }

    @Override
    public Set<String> getConnectionGroupIdentifiers() {
        return connectionGroupIdentifiers;
    }

    @Override
    public GuacamoleTunnel connect(GuacamoleClientInformation info) 
            throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

}
