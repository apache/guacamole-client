package net.sourceforge.guacamole.net.basic.crud.connections;

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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.GuacamoleSecurityException;
import net.sourceforge.guacamole.GuacamoleServerException;
import net.sourceforge.guacamole.net.auth.Connection;
import net.sourceforge.guacamole.net.auth.ConnectionRecord;
import net.sourceforge.guacamole.net.auth.Directory;
import net.sourceforge.guacamole.net.auth.User;
import net.sourceforge.guacamole.net.auth.UserContext;
import net.sourceforge.guacamole.net.auth.permission.ConnectionPermission;
import net.sourceforge.guacamole.net.auth.permission.ObjectPermission;
import net.sourceforge.guacamole.net.auth.permission.Permission;
import net.sourceforge.guacamole.net.auth.permission.SystemPermission;
import net.sourceforge.guacamole.net.basic.AuthenticatingHttpServlet;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;

/**
 * Simple HttpServlet which outputs XML containing a list of all authorized
 * configurations for the current user.
 *
 * @author Michael Jumper
 */
public class List extends AuthenticatingHttpServlet {

    /**
     * Checks whether the given user has permission to perform the given
     * object operation. Security exceptions are handled appropriately - only
     * non-security exceptions pass through.
     *
     * @param user The user whose permissions should be verified.
     * @param type The type of operation to check for permission for.
     * @param identifier The identifier of the connection the operation
     *                   would be performed upon.
     * @return true if permission is granted, false otherwise.
     *
     * @throws GuacamoleException If an error occurs while checking permissions.
     */
    private boolean hasConfigPermission(User user, ObjectPermission.Type type,
            String identifier)
    throws GuacamoleException {

        // Build permission
        Permission permission = new ConnectionPermission(
            type,
            identifier
        );

        try {
            // Return result of permission check, if possible
            return user.hasPermission(permission);
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
    throws GuacamoleException {

        // Do not cache
        response.setHeader("Cache-Control", "no-cache");

        // Write XML content type
        response.setHeader("Content-Type", "text/xml");
        
        // Set encoding
        response.setCharacterEncoding("UTF-8");

        // Get connection directory
        Directory<String, Connection> directory =
                context.getRootConnectionGroup().getConnectionDirectory();

        // Sys-admin permission
        Permission systemPermission =
                new SystemPermission(SystemPermission.Type.ADMINISTER);

        // Write actual XML
        try {

            // Get self
            User self = context.self();

            XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter xml = outputFactory.createXMLStreamWriter(response.getWriter());

            // Begin document
            xml.writeStartDocument();
            xml.writeStartElement("connections");

            // For each entry, write corresponding connection element
            for (String identifier : directory.getIdentifiers()) {

                // Get connection
                Connection connection = directory.get(identifier);

                // Write connection
                xml.writeStartElement("connection");
                xml.writeAttribute("id", identifier);
                xml.writeAttribute("protocol",
                        connection.getConfiguration().getProtocol());

                // If update permission available, include parameters
                if (self.hasPermission(systemPermission) ||
                        hasConfigPermission(self, ObjectPermission.Type.UPDATE,
                        identifier)) {

                    // As update permission is present, also list parameters
                    GuacamoleConfiguration config = connection.getConfiguration();
                    for (String name : config.getParameterNames()) {

                        String value = connection.getConfiguration().getParameter(name);
                        xml.writeStartElement("param");
                        xml.writeAttribute("name", name);

                        if (value != null)
                            xml.writeCharacters(value);

                        xml.writeEndElement();
                    }

                }

                // Write history
                xml.writeStartElement("history");
                for (ConnectionRecord record : connection.getHistory()) {
                    xml.writeStartElement("record");

                    // Start date
                    xml.writeAttribute("start",
                        Long.toString(record.getStartDate().getTime()));

                    // End date
                    if (record.getEndDate() != null)
                        xml.writeAttribute("end",
                            Long.toString(record.getEndDate().getTime()));

                    // Whether connection currently active
                    if (record.isActive())
                        xml.writeAttribute("active", "yes");

                    // User involved
                    xml.writeCharacters(record.getUsername());

                    xml.writeEndElement();
                }
                xml.writeEndElement();

                xml.writeEndElement();

            }

            // End document
            xml.writeEndElement();
            xml.writeEndDocument();

        }
        catch (XMLStreamException e) {
            throw new GuacamoleServerException(
                    "Unable to write configuration list XML.", e);
        }
        catch (IOException e) {
            throw new GuacamoleServerException(
                    "I/O error writing configuration list XML.", e);
        }

    }

}

