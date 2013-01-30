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
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.GuacamoleSecurityException;
import net.sourceforge.guacamole.net.auth.GuacamoleConfigurationDirectory;
import net.sourceforge.guacamole.net.auth.PermissionDirectory;
import net.sourceforge.guacamole.net.auth.UserContext;
import net.sourceforge.guacamole.net.auth.permission.GuacamoleConfigurationDirectoryPermission;
import net.sourceforge.guacamole.net.auth.permission.GuacamoleConfigurationPermission;
import net.sourceforge.guacamole.net.auth.permission.ObjectPermission;
import net.sourceforge.guacamole.net.auth.permission.Permission;
import net.sourceforge.guacamole.net.auth.permission.SystemPermission;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;

/**
 * Simple HttpServlet which outputs XML containing a list of all authorized
 * configurations for the current user.
 *
 * @author Michael Jumper
 */
public class ConfigurationList extends AuthenticatingHttpServlet {

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
    private boolean hasConfigPermission(PermissionDirectory permissions,
            String user, SystemPermission.Type type)
    throws GuacamoleException {

        // Build permission
        Permission permission =
                new GuacamoleConfigurationDirectoryPermission(type);

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
     * @param identifier The identifier of the configuration the operation
     *                   would be performed upon.
     * @return true if permission is granted, false otherwise.
     * 
     * @throws GuacamoleException If an error occurs while checking permissions.
     */
    private boolean hasConfigPermission(PermissionDirectory permissions,
            String user, ObjectPermission.Type type, String identifier)
    throws GuacamoleException {

        // Build permission
        Permission permission = new GuacamoleConfigurationPermission(
            type,
            identifier
        );

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

        // Attempt to get configurations
        Map<String, GuacamoleConfiguration> configs;
        try {

            // Get configuration directory
            GuacamoleConfigurationDirectory directory =
                context.getGuacamoleConfigurationDirectory();
            
            // Get configurations
            configs = directory.getConfigurations();

        }
        catch (GuacamoleException e) {
            throw new ServletException("Unable to retrieve configurations.", e);
        }
        
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

            // Get username
            String username = context.self().getUsername();
            
            XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter xml = outputFactory.createXMLStreamWriter(response.getWriter());

            // Begin document
            xml.writeStartDocument();
            xml.writeStartElement("configs");
            
            // Save config create permission attribute
            if (hasConfigPermission(permissions, username,
                    SystemPermission.Type.CREATE))
                xml.writeAttribute("create", "yes");
            
            // For each entry, write corresponding config element
            for (Entry<String, GuacamoleConfiguration> entry : configs.entrySet()) {

                // Get config
                GuacamoleConfiguration config = entry.getValue();

                // Write config
                xml.writeEmptyElement("config");
                xml.writeAttribute("id", entry.getKey());
                xml.writeAttribute("protocol", config.getProtocol());

                // Check permissions and set attributes appropriately
                if (permissions != null) {

                    // Save update permission attribute
                    if (hasConfigPermission(permissions, username,
                            ObjectPermission.Type.UPDATE, entry.getKey()))
                        xml.writeAttribute("update", "yes");
                    
                    // Save admin permission attribute
                    if (hasConfigPermission(permissions, username,
                            ObjectPermission.Type.ADMINISTER, entry.getKey()))
                        xml.writeAttribute("admin", "yes");
                    
                    // Save delete permission attribute
                    if (hasConfigPermission(permissions, username,
                            ObjectPermission.Type.DELETE, entry.getKey()))
                        xml.writeAttribute("delete", "yes");
                    
                }
                
            }

            // End document
            xml.writeEndElement();
            xml.writeEndDocument();

        }
        catch (XMLStreamException e) {
            throw new IOException("Unable to write configuration list XML.", e);
        }
        catch (GuacamoleException e) {
            throw new ServletException("Unable to read configurations.", e);
        }

    }

}

