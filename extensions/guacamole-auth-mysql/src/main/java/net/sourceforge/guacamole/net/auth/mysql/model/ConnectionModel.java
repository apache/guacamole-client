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

package net.sourceforge.guacamole.net.auth.mysql.model;

/**
 * Object representation of a Guacamole connection, as represented in the
 * database.
 *
 * @author Michael Jumper
 */
public class ConnectionModel {

    /**
     * The identifier of this connection in the database, if any.
     */
    private String identifier;

    /**
     * The identifier of the parent connection group in the database, or null
     * if the parent connection group is the root group.
     */
    private String parentIdentifier;
    
    /**
     * The human-readable name associated with this connection.
     */
    private String name;

    /**
     * The name of the protocol to use when connecting to this connection.
     */
    private String protocol;
    
    /**
     * Creates a new, empty connection.
     */
    public ConnectionModel() {
    }

    /**
     * Returns the name associated with this connection.
     *
     * @return
     *     The name associated with this connection.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name associated with this connection.
     *
     * @param name
     *     The name to associate with this connection.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the protocol to use when connecting to this
     * connection.
     *
     * @return
     *     The name of the protocol to use when connecting to this connection.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Sets the name of the protocol to use when connecting to this connection.
     *
     * @param protocol
     *     The name of the protocol to use when connecting to this connection.
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
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
    public void setParentID(String parentIdentifier) {
        this.parentIdentifier = parentIdentifier;
    }

    /**
     * Returns the identifier of this connection in the database, if it exists.
     *
     * @return
     *     The identifier of this connection in the database, or null if this
     *     connection was not retrieved from the database.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Sets the identifier of this connection to the given value.
     *
     * @param identifier 
     *     The identifier to assign to this connection.
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

}
