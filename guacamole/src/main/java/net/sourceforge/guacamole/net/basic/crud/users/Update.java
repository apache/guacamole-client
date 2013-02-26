package net.sourceforge.guacamole.net.basic.crud.users;

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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.Directory;
import net.sourceforge.guacamole.net.auth.User;
import net.sourceforge.guacamole.net.auth.UserContext;
import net.sourceforge.guacamole.net.auth.permission.ConnectionDirectoryPermission;
import net.sourceforge.guacamole.net.auth.permission.ConnectionPermission;
import net.sourceforge.guacamole.net.auth.permission.ObjectPermission;
import net.sourceforge.guacamole.net.auth.permission.Permission;
import net.sourceforge.guacamole.net.auth.permission.SystemPermission;
import net.sourceforge.guacamole.net.auth.permission.UserDirectoryPermission;
import net.sourceforge.guacamole.net.auth.permission.UserPermission;
import net.sourceforge.guacamole.net.basic.AuthenticatingHttpServlet;

/**
 * Simple HttpServlet which handles user update.
 *
 * @author Michael Jumper
 */
public class Update extends AuthenticatingHttpServlet {

    /**
     * String given for directory creation permission.
     */
    private static final String CREATE_PERMISSION = "create";

    /**
     * Prefix given before an object identifier for read permission.
     */
    private static final String READ_PREFIX   = "read:";

    /**
     * Prefix given before an object identifier for delete permission.
     */
    private static final String DELETE_PREFIX = "delete:";

    /**
     * Prefix given before an object identifier for update permission.
     */
    private static final String UPDATE_PREFIX = "update:";

    /**
     * Prefix given before an object identifier for administration permission.
     */
    private static final String ADMIN_PREFIX  = "admin:";

    /**
     * Given a permission string, returns the corresponding user permission.
     *
     * @param str The permission string to parse.
     * @return The parsed user permission.
     * @throws GuacamoleException If the given string could not be parsed.
     */
    private Permission parseUserPermission(String str)
            throws GuacamoleException {

        // Create permission
        if (str.equals(CREATE_PERMISSION))
            return new UserDirectoryPermission(SystemPermission.Type.CREATE);

        // Read
        if (str.startsWith(READ_PREFIX))
            return new UserPermission(ObjectPermission.Type.READ,
                    str.substring(READ_PREFIX.length()));

        // Update
        if (str.startsWith(UPDATE_PREFIX))
            return new UserPermission(ObjectPermission.Type.UPDATE,
                    str.substring(UPDATE_PREFIX.length()));

        // Delete
        if (str.startsWith(DELETE_PREFIX))
            return new UserPermission(ObjectPermission.Type.DELETE,
                    str.substring(DELETE_PREFIX.length()));

        // Administration
        if (str.startsWith(ADMIN_PREFIX))
            return new UserPermission(ObjectPermission.Type.ADMINISTER,
                    str.substring(ADMIN_PREFIX.length()));

        throw new GuacamoleException("Invalid permission string.");

    }

    /**
     * Given a permission string, returns the corresponding connection
     * permission.
     *
     * @param str The permission string to parse.
     * @return The parsed connection permission.
     * @throws GuacamoleException If the given string could not be parsed.
     */
    private Permission parseConnectionPermission(String str)
            throws GuacamoleException {

        // Create permission
        if (str.equals(CREATE_PERMISSION))
            return new ConnectionDirectoryPermission(SystemPermission.Type.CREATE);

        // Read
        if (str.startsWith(READ_PREFIX))
            return new ConnectionPermission(ObjectPermission.Type.READ,
                    str.substring(READ_PREFIX.length()));

        // Update
        if (str.startsWith(UPDATE_PREFIX))
            return new ConnectionPermission(ObjectPermission.Type.UPDATE,
                    str.substring(UPDATE_PREFIX.length()));

        // Delete
        if (str.startsWith(DELETE_PREFIX))
            return new ConnectionPermission(ObjectPermission.Type.DELETE,
                    str.substring(DELETE_PREFIX.length()));

        // Administration
        if (str.startsWith(ADMIN_PREFIX))
            return new ConnectionPermission(ObjectPermission.Type.ADMINISTER,
                    str.substring(ADMIN_PREFIX.length()));

        throw new GuacamoleException("Invalid permission string.");

    }

    @Override
    protected void authenticatedService(
            UserContext context,
            HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException {

        // Create user as specified
        String username = request.getParameter("name");
        String password = request.getParameter("password");

        try {

            // Attempt to get user directory
            Directory<String, User> directory =
                    context.getUserDirectory();

            // Get user data, setting password if given
            User user = directory.get(username);
            user.setUsername(username);
            if (password != null)
                user.setPassword(password);

            // Set user permissions
            String[] user_permission = request.getParameterValues("user");
            if (user_permission != null) {
                for (String str : user_permission)
                    user.addPermission(parseUserPermission(str));
            }

            // Set connection permissions
            String[] connection_permission = request.getParameterValues("connection");
            if (connection_permission != null) {
                for (String str : connection_permission)
                    user.addPermission(parseConnectionPermission(str));
            }

            // Update user
            directory.update(user);

        }
        catch (GuacamoleException e) {
            throw new ServletException("Unable to update user.", e);
        }

    }

}

