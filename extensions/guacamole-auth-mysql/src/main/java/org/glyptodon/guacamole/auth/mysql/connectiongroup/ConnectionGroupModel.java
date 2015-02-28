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

package org.glyptodon.guacamole.auth.mysql.connectiongroup;

import org.glyptodon.guacamole.auth.mysql.base.ObjectModel;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;

/**
 * Object representation of a Guacamole connection group, as represented in the
 * database.
 *
 * @author Michael Jumper
 */
public class ConnectionGroupModel extends ObjectModel {

    /**
     * The identifier of the parent connection group in the database, or null
     * if the parent connection group is the root group.
     */
    private String parentIdentifier;
    
    /**
     * The human-readable name associated with this connection group.
     */
    private String name;

    /**
     * The type of this connection group, such as organizational or balancing.
     */
    private ConnectionGroup.Type type;
    
    /**
     * Creates a new, empty connection group.
     */
    public ConnectionGroupModel() {
    }

    /**
     * Returns the name associated with this connection group.
     *
     * @return
     *     The name associated with this connection group.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name associated with this connection group.
     *
     * @param name
     *     The name to associate with this connection group.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the identifier of the parent connection group, or null if the
     * parent connection group is the root connection group.
     *
     * @return 
     *     The identifier of the parent connection group, or null if the parent
     *     connection group is the root connection group.
     */
    public String getParentIdentifier() {
        return parentIdentifier;
    }

    /**
     * Sets the identifier of the parent connection group.
     *
     * @param parentIdentifier
     *     The identifier of the parent connection group, or null if the parent
     *     connection group is the root connection group.
     */
    public void setParentIdentifier(String parentIdentifier) {
        this.parentIdentifier = parentIdentifier;
    }

    /**
     * Returns the type of this connection group, such as organizational or
     * balancing.
     *
     * @return
     *     The type of this connection group.
     */
    public ConnectionGroup.Type getType() {
        return type;
    }

    /**
     * Sets the type of this connection group, such as organizational or
     * balancing.
     *
     * @param type
     *     The type of this connection group.
     */
    public void setType(ConnectionGroup.Type type) {
        this.type = type;
    }

    @Override
    public String getIdentifier() {

        // If no associated ID, then no associated identifier
        Integer id = getObjectID();
        if (id == null)
            return null;

        // Otherwise, the identifier is the ID as a string
        return id.toString();

    }

    @Override
    public void setIdentifier(String identifier) {
        throw new UnsupportedOperationException("Connection group identifiers are derived from IDs. They cannot be set.");
    }

}
