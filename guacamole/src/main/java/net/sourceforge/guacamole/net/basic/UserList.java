package net.sourceforge.guacamole.net.basic;

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

import java.io.IOException;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.GuacamoleSecurityException;
import net.sourceforge.guacamole.net.auth.PermissionDirectory;
import net.sourceforge.guacamole.net.auth.User;
import net.sourceforge.guacamole.net.auth.UserContext;
import net.sourceforge.guacamole.net.auth.UserDirectory;
import net.sourceforge.guacamole.net.auth.permission.ObjectPermission;
import net.sourceforge.guacamole.net.auth.permission.Permission;
import net.sourceforge.guacamole.net.auth.permission.SystemPermission;
import net.sourceforge.guacamole.net.auth.permission.UserDirectoryPermission;
import net.sourceforge.guacamole.net.auth.permission.UserPermission;

/**
 * Simple HttpServlet which outputs XML containing a list of all visible users.
 *
 * @author Michael Jumper
 */
public class UserList extends AuthenticatingHttpServlet {

    /**
     * Checks whether the given user has permission to perform the given
     * system operation. Security exceptions are handled appropriately - only
     * non-security exceptions pass through.
     * 
     * @param permissions The PermissionsDirectory to check.
     * @param user The user whose permissions should be verified.
     * @param type The type of operation to check for permission for.
     * @return true if permission is granted, false otherwise.
     * 
     * @throws GuacamoleException If an error occurs while checking permissions.
     */
    private boolean hasUserPermission(PermissionDirectory permissions,
            String user, SystemPermission.Type type)
    throws GuacamoleException {

        // Build permission
        Permission permission = new UserDirectoryPermission(type);

        try {
            // Return result of permission check, if possible
            return permissions.hasPermission(user, permission);
        }
        catch (GuacamoleSecurityException e) {
            // If cannot check due to security restrictions, no permission
            return false;
        }

    }

    /**
     * Checks whether the given user has permission to perform the given
     * object operation. Security exceptions are handled appropriately - only
     * non-security exceptions pass through.
     * 
     * @param permissions The PermissionsDirectory to check.
     * @param user The user whose permissions should be verified.
     * @param type The type of operation to check for permission for.
     * @param identifier The identifier of the user the operation would be
     *                   performed upon.
     * @return true if permission is granted, false otherwise.
     * 
     * @throws GuacamoleException If an error occurs while checking permissions.
     */
    private boolean hasUserPermission(PermissionDirectory permissions,
            String user, ObjectPermission.Type type, String identifier)
    throws GuacamoleException {

        // Build permission
        Permission permission = new UserPermission(type, identifier);

        try {
            // Return result of permission check, if possible
            return permissions.hasPermission(user, permission);
        }
        catch (GuacamoleSecurityException e) {
            // If cannot check due to security restrictions, no permission
            return false;
        }

    }
    
    @Override
    protected void authenticatedService(
            UserContext context,
            HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException {

        // Do not cache
        response.setHeader("Cache-Control", "no-cache");

        // Write XML content type
        response.setHeader("Content-Type", "text/xml");

        // Try to get permission directory
        PermissionDirectory permissions = null;
        try {
            permissions = context.getPermissionDirectory();
        }
        catch (GuacamoleSecurityException e) {
            // Soft fail - can't check permissions ... assume have READ and
            // nothing else
        }
        catch (GuacamoleException e) {
            throw new ServletException("Unable to retrieve permissions.", e);
        }

        // Write actual XML
        try {

            // Get user directory
            UserDirectory directory = context.getUserDirectory();
            
            // Get users
            Set<User> users = directory.getUsers();

            // Get username
            String username = context.self().getUsername();

            XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter xml = outputFactory.createXMLStreamWriter(response.getWriter());

            // Begin document
            xml.writeStartDocument();
            xml.writeStartElement("users");
            
            // Save user create permission attribute
            if (hasUserPermission(permissions, username,
                    SystemPermission.Type.CREATE))
                xml.writeAttribute("create", "yes");
            
            // For each entry, write corresponding user element
            for (User user : users) {

                // Write user 
                xml.writeEmptyElement("user");
                xml.writeAttribute("name", user.getUsername());

                // Check permissions and set attributes appropriately
                if (permissions != null) {

                    // Save update permission attribute
                    if (hasUserPermission(permissions, username,
                            ObjectPermission.Type.UPDATE, user.getUsername()))
                        xml.writeAttribute("update", "yes");
                    
                    // Save admin permission attribute
                    if (hasUserPermission(permissions, username,
                            ObjectPermission.Type.ADMINISTER, user.getUsername()))
                        xml.writeAttribute("admin", "yes");
                    
                    // Save delete permission attribute
                    if (hasUserPermission(permissions, username,
                            ObjectPermission.Type.DELETE, user.getUsername()))
                        xml.writeAttribute("delete", "yes");
                    
                }
                
            }

            // End document
            xml.writeEndElement();
            xml.writeEndDocument();

        }
        catch (XMLStreamException e) {
            throw new IOException("Unable to write user list XML.", e);
        }
        catch (GuacamoleSecurityException e) {

            // If cannot read permissions, return error
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Permission denied.");

        }
        catch (GuacamoleException e) {
            throw new ServletException("Unable to read users.", e);
        }

    }

}

