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

package org.glyptodon.guacamole.net.auth;

import java.util.Collection;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.form.Form;

/**
 * The context of an active user. The functions of this class enforce all
 * permissions and act only within the rights of the associated user.
 *
 * @author Michael Jumper
 */
public interface UserContext {

    /**
     * Returns the User whose access rights control the operations of this
     * UserContext.
     *
     * @return The User whose access rights control the operations of this
     *         UserContext.
     */
    User self();

    /**
     * Returns the AuthenticationProvider which created this UserContext, which
     * may not be the same AuthenticationProvider that authenticated the user
     * associated with this UserContext.
     *
     * @return
     *     The AuthenticationProvider that created this UserContext.
     */
    AuthenticationProvider getAuthenticationProvider();

    /**
     * Retrieves a Directory which can be used to view and manipulate other
     * users, but only as allowed by the permissions given to the user of this
     * UserContext.
     *
     * @return A Directory whose operations are bound by the restrictions
     *         of this UserContext.
     *
     * @throws GuacamoleException If an error occurs while creating the
     *                            Directory.
     */
    Directory<User> getUserDirectory() throws GuacamoleException;

    /**
     * Retrieves a Directory which can be used to view and manipulate
     * connections and their configurations, but only as allowed by the
     * permissions given to the user.
     *
     * @return A Directory whose operations are bound by the permissions of 
     *         the user.
     *
     * @throws GuacamoleException If an error occurs while creating the
     *                            Directory.
     */
    Directory<Connection> getConnectionDirectory()
            throws GuacamoleException;

    /**
     * Retrieves a Directory which can be used to view and manipulate
     * connection groups and their members, but only as allowed by the
     * permissions given to the user.
     *
     * @return A Directory whose operations are bound by the permissions of
     *         the user.
     *
     * @throws GuacamoleException If an error occurs while creating the
     *                            Directory.
     */
    Directory<ConnectionGroup> getConnectionGroupDirectory()
            throws GuacamoleException;

    /**
     * Retrieves a Directory which can be used to view and manipulate
     * active connections, but only as allowed by the permissions given to the
     * user.
     *
     * @return
     *     A Directory whose operations are bound by the permissions of the
     *     user.
     *
     * @throws GuacamoleException
     *     If an error occurs while creating the Directory.
     */
    Directory<ActiveConnection> getActiveConnectionDirectory()
            throws GuacamoleException;

    /**
     * Retrieves a connection group which can be used to view and manipulate
     * connections, but only as allowed by the permissions given to the user of 
     * this UserContext.
     *
     * @return A connection group whose operations are bound by the restrictions
     *         of this UserContext.
     *
     * @throws GuacamoleException If an error occurs while creating the
     *                            Directory.
     */
    ConnectionGroup getRootConnectionGroup() throws GuacamoleException;

    /**
     * Retrieves a collection of all attributes applicable to users. This
     * collection will contain only those attributes which the current user has
     * general permission to view or modify. If there are no such attributes,
     * this collection will be empty.
     *
     * @return
     *     A collection of all attributes applicable to users.
     */
    Collection<Form> getUserAttributes();

    /**
     * Retrieves a collection of all attributes applicable to connections. This
     * collection will contain only those attributes which the current user has
     * general permission to view or modify. If there are no such attributes,
     * this collection will be empty.
     *
     * @return
     *     A collection of all attributes applicable to connections.
     */
    Collection<Form> getConnectionAttributes();

    /**
     * Retrieves a collection of all attributes applicable to connection
     * groups. This collection will contain only those attributes which the
     * current user has general permission to view or modify. If there are no
     * such attributes, this collection will be empty.
     *
     * @return
     *     A collection of all attributes applicable to connection groups.
     */
    Collection<Form> getConnectionGroupAttributes();

}
