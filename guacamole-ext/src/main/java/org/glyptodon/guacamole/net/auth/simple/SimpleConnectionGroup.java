
package org.glyptodon.guacamole.net.auth.simple;

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

import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.GuacamoleSocket;
import org.glyptodon.guacamole.net.auth.AbstractConnectionGroup;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.net.auth.Directory;
import org.glyptodon.guacamole.protocol.GuacamoleClientInformation;


/**
 * An extremely simple read-only implementation of a ConnectionGroup which
 * returns the connection and connection group directories it was constructed
 * with. Load balancing across this connection group is not allowed.
 * 
 * @author James Muehlner
 */
public class SimpleConnectionGroup extends AbstractConnectionGroup {

    /**
     * Underlying connection directory, containing all connections within this
     * group.
     */
    private final Directory<String, Connection> connectionDirectory;

    /**
     * Underlying connection group directory, containing all connections within
     * this group.
     */
    private final Directory<String, ConnectionGroup> connectionGroupDirectory;
    
    /**
     * Creates a new SimpleConnectionGroup having the given name and identifier
     * which will expose the given directories as its contents.
     * 
     * @param name The name to associate with this connection.
     * @param identifier The identifier to associate with this connection.
     * @param connectionDirectory The connection directory to expose when
     *                            requested.
     * @param connectionGroupDirectory The connection group directory to expose
     *                                 when requested.
     */
    public SimpleConnectionGroup(String name, String identifier,
            Directory<String, Connection> connectionDirectory, 
            Directory<String, ConnectionGroup> connectionGroupDirectory) {

        // Set name
        setName(name);

        // Set identifier
        setIdentifier(identifier);
        
        // Set group type
        setType(ConnectionGroup.Type.ORGANIZATIONAL);

        // Assign directories
        this.connectionDirectory = connectionDirectory;
        this.connectionGroupDirectory = connectionGroupDirectory;

    }
    
    @Override
    public Directory<String, Connection> getConnectionDirectory() 
            throws GuacamoleException {
        return connectionDirectory;
    }

    @Override
    public Directory<String, ConnectionGroup> getConnectionGroupDirectory() 
            throws GuacamoleException {
        return connectionGroupDirectory;
    }

    @Override
    public GuacamoleSocket connect(GuacamoleClientInformation info) 
            throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

}
