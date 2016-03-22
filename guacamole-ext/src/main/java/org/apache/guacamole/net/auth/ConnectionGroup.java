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

package org.apache.guacamole.net.auth;

import java.util.Map;
import java.util.Set;
import org.apache.guacamole.GuacamoleException;

/**
 * Represents a connection group, which can contain both other connection groups
 * as well as connections.
 *
 * @author James Muehlner
 */
public interface ConnectionGroup extends Identifiable, Connectable {
  
    /**
     * All legal types of connection group.
     */
    public enum Type {

        /**
         * A connection group that purely organizes other connections or
         * connection groups, serving only as a container. An organizational
         * connection group is analogous to a directory or folder in a
         * filesystem.
         */
        ORGANIZATIONAL,

        /**
         * A connection group that acts as a load balancer. A balancing
         * connection group can be connected to in the same manner as a
         * connection, and will transparently route to the least-used
         * underlying connection.
         */
        BALANCING

    };

    /**
     * Returns the name assigned to this ConnectionGroup.
     * @return The name assigned to this ConnectionGroup.
     */
    public String getName();

    /**
     * Sets the name assigned to this ConnectionGroup.
     *
     * @param name The name to assign.
     */
    public void setName(String name);

    /**
     * Returns the unique identifier of the parent ConnectionGroup for
     * this ConnectionGroup.
     * 
     * @return The unique identifier of the parent ConnectionGroup for
     * this ConnectionGroup.
     */
    public String getParentIdentifier();

    /**
     * Sets the unique identifier of the parent ConnectionGroup for
     * this ConnectionGroup.
     * 
     * @param parentIdentifier The unique identifier of the parent 
     * ConnectionGroup for this ConnectionGroup.
     */
    public void setParentIdentifier(String parentIdentifier);
    
    /**
     * Set the type of this ConnectionGroup.
     *
     * @param type The type of this ConnectionGroup.
     */
    public void setType(Type type);
    
    /**
     * Returns the type of this connection.
     * @return the type of this connection.
     */
    public Type getType();

    /**
     * Returns the identifiers of all readable connections that are children
     * of this connection group.
     *
     * @return
     *     The set of identifiers of all readable connections that are children
     *     of this connection group.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the identifiers.
     */
    public Set<String> getConnectionIdentifiers() throws GuacamoleException;

    /**
     * Returns the identifiers of all readable connection groups that are
     * children of this connection group.
     *
     * @return
     *     The set of identifiers of all readable connection groups that are
     *     children of this connection group.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the identifiers.
     */

    public Set<String> getConnectionGroupIdentifiers()
            throws GuacamoleException;

    /**
     * Returns all attributes associated with this connection group. The
     * returned map may not be modifiable.
     *
     * @return
     *     A map of all attribute identifiers to their corresponding values,
     *     for all attributes associated with this connection group, which may
     *     not be modifiable.
     */
    Map<String, String> getAttributes();

    /**
     * Sets the given attributes. If an attribute within the map is not
     * supported, it will simply be dropped. Any attributes not within the
     * given map will be left untouched.
     *
     * @param attributes
     *     A map of all attribute identifiers to their corresponding values.
     */
    void setAttributes(Map<String, String> attributes);

}
