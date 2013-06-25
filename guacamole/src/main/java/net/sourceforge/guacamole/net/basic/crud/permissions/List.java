package net.sourceforge.guacamole.net.basic.crud.permissions;

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
import net.sourceforge.guacamole.GuacamoleClientException;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.GuacamoleSecurityException;
import net.sourceforge.guacamole.GuacamoleServerException;
import net.sourceforge.guacamole.net.auth.Directory;
import net.sourceforge.guacamole.net.auth.User;
import net.sourceforge.guacamole.net.auth.UserContext;
import net.sourceforge.guacamole.net.auth.permission.ConnectionPermission;
import net.sourceforge.guacamole.net.auth.permission.ObjectPermission;
import net.sourceforge.guacamole.net.auth.permission.Permission;
import net.sourceforge.guacamole.net.auth.permission.SystemPermission;
import net.sourceforge.guacamole.net.auth.permission.UserPermission;
import net.sourceforge.guacamole.net.basic.AuthenticatingHttpServlet;

/**
 * Simple HttpServlet which outputs XML containing a list of all visible
 * permissions of a given user.
 *
 * @author Michael Jumper
 */
public class List extends AuthenticatingHttpServlet {

    /**
     * Returns the XML attribute value representation of the given
     * SystemPermission.Type.
     *
     * @param type The SystemPermission.Type to translate into a String.
     * @return The XML attribute value representation of the given
     *         SystemPermission.Type.
     *
     * @throws GuacamoleException If the type given is not implemented.
     */
    private String toString(SystemPermission.Type type)
        throws GuacamoleException {

        switch (type) {
            case CREATE_USER:       return "create-user";
            case CREATE_CONNECTION: return "create-connection";
            case ADMINISTER:        return "admin";
        }

        throw new GuacamoleException("Unknown permission type: " + type);

    }

    /**
     * Returns the XML attribute value representation of the given
     * ObjectPermission.Type.
     *
     * @param type The ObjectPermission.Type to translate into a String.
     * @return The XML attribute value representation of the given
     *         ObjectPermission.Type.
     *
     * @throws GuacamoleException If the type given is not implemented.
     */
    private String toString(ObjectPermission.Type type)
        throws GuacamoleException {

        switch (type) {
            case READ:       return "read";
            case UPDATE:     return "update";
            case DELETE:     return "delete";
            case ADMINISTER: return "admin";
        }

        throw new GuacamoleException("Unknown permission type: " + type);

    }

    @Override
    protected void authenticatedService(
            UserContext context,
            HttpServletRequest request, HttpServletResponse response)
    throws GuacamoleException {

        // Do not cache
        response.setHeader("Cache-Control", "no-cache");

        // Write actual XML
        try {

            User user;

            // Get username
            String username = request.getParameter("user");
            if (username != null) {

                // Get user directory
                Directory<String, User> users = context.getUserDirectory();

                // Get specific user
                user = users.get(username);
            }
            else
                user = context.self();
            
            if (user == null)
                throw new GuacamoleSecurityException("No such user.");

            // Write XML content type
            response.setHeader("Content-Type", "text/xml");

            XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter xml = outputFactory.createXMLStreamWriter(response.getWriter());

            // Begin document
            xml.writeStartDocument();
            xml.writeStartElement("permissions");
            xml.writeAttribute("user", user.getUsername());

            // For each entry, write corresponding user element
            for (Permission permission : user.getPermissions()) {

                // System permission
                if (permission instanceof SystemPermission) {

                    // Get permission
                    SystemPermission sp = (SystemPermission) permission;

                    // Write permission
                    xml.writeEmptyElement("system");
                    xml.writeAttribute("type", toString(sp.getType()));

                }

                // Config permission
                else if (permission instanceof ConnectionPermission) {

                    // Get permission
                    ConnectionPermission cp =
                            (ConnectionPermission) permission;

                    // Write permission
                    xml.writeEmptyElement("connection");
                    xml.writeAttribute("type", toString(cp.getType()));
                    xml.writeAttribute("name", cp.getObjectIdentifier());

                }

                // User permission
                else if (permission instanceof UserPermission) {

                    // Get permission
                    UserPermission up = (UserPermission) permission;

                    // Write permission
                    xml.writeEmptyElement("user");
                    xml.writeAttribute("type", toString(up.getType()));
                    xml.writeAttribute("name", up.getObjectIdentifier());

                }

                else
                    throw new GuacamoleClientException(
                            "Unsupported permission type.");

            }

            // End document
            xml.writeEndElement();
            xml.writeEndDocument();

        }
        catch (XMLStreamException e) {
            throw new GuacamoleServerException(
                    "Unable to write permission list XML.", e);
        }
        catch (IOException e) {
            throw new GuacamoleServerException(
                    "I/O error writing permission list XML.", e);
        }

    }

}
