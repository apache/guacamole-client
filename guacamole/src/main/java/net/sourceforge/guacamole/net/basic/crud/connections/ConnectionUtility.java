
/*
 *  Guacamole - Clientless Remote Desktop
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.guacamole.net.basic.crud.connections;

import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.Connection;
import net.sourceforge.guacamole.net.auth.ConnectionGroup;
import net.sourceforge.guacamole.net.auth.Directory;
import net.sourceforge.guacamole.net.auth.UserContext;

/**
 * A class that provides helper methods for the Connection CRUD servlets.
 * 
 * @author James Muehlner
 */
class ConnectionUtility {
    
    // This class should not be instantiated
    private ConnectionUtility() {}
    
    /**
     * Get the ConnectionDirectory with the parent connection group specified by
     * parentID.
     * 
     * @param context The UserContext to search for the connection directory.
     * @param parentID The ID of the parent connection group to search for.
     * 
     * @return The ConnectionDirectory with the parent connection group,
     *         if found.
     * @throws GuacamoleException If an error is encountered while getting the
     *                            connection directory.
     */
    static Directory<String, Connection> findConnectionDirectory(
            UserContext context, String parentID) throws GuacamoleException {
        
        // Find the correct connection directory
        ConnectionGroup rootGroup = context.getRootConnectionGroup();
        Directory<String, Connection> directory;
        
        Directory<String, ConnectionGroup> connectionGroupDirectory = 
            rootGroup.getConnectionGroupDirectory();

        ConnectionGroup parentGroup = connectionGroupDirectory.get(parentID);

        if(parentGroup == null)
            return null;

        directory = parentGroup.getConnectionDirectory();
        
        return directory;
    }
}
