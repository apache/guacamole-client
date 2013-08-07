
package net.sourceforge.guacamole.net.auth;

/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is guacamole-ext.
 *
 * The Initial Developer of the Original Code is
 * Michael Jumper.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s): James Muehlner
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.GuacamoleSocket;
import net.sourceforge.guacamole.protocol.GuacamoleClientInformation;


/**
 * Represents a connection group, which can contain both other connection groups
 * as well as connections.
 *
 * @author James Muehlner
 */
public interface ConnectionGroup {

    /**
     * Returns the name assigned to this ConnectionGroup.
     * @return The name assigned to this ConnectionGroup.
     */
    public String getName();

    /**
     * Sets the name assigned to this ConnectionGroup.
     *
     * @param identifier The name to assign.
     */
    public void setName(String name);

    /**
     * Returns the unique identifier assigned to this ConnectionGroup.
     * @return The unique identifier assigned to this ConnectionGroup.
     */
    public String getIdentifier();

    /**
     * Sets the identifier assigned to this ConnectionGroup.
     *
     * @param identifier The identifier to assign.
     */
    public void setIdentifier(String identifier);
    
    /**
     * Sets whether this is a balancing ConnectionGroup.
     *
     * @param balancing whether this is a balancing ConnectionGroup.
     */
    public void setBalancing(boolean balancing);
    
    /**
     * Returns true if this is a balancing ConnectionGroup, false otherwise.
     * @return true if this is a balancing ConnectionGroup, false otherwise.
     */
    public boolean isBalancing();

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
    Directory<String, Connection> getConnectionDirectory()
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
    Directory<String, ConnectionGroup> getConnectionGroupDirectory()
            throws GuacamoleException;
    
    /**
     * Establishes a connection to guacd using a connection chosen from among
     * the connections in this ConnectionGroup, and returns the resulting, 
     * connected GuacamoleSocket.
     *
     * @param info Information associated with the connecting client.
     * @return A fully-established GuacamoleSocket.
     *
     * @throws GuacamoleException If an error occurs while connecting to guacd,
     *                            or if permission to connect is denied.
     */
    public GuacamoleSocket connect(GuacamoleClientInformation info)
            throws GuacamoleException;

}
