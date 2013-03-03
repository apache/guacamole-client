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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.guacamole.GuacamoleClientException;
import net.sourceforge.guacamole.GuacamoleException;
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
 * Simple HttpServlet which handles user update.
 *
 * @author Michael Jumper
 */
public class Update extends AuthenticatingHttpServlet {

    /**
     * String given for user creation permission.
     */
    private static final String CREATE_USER_PERMISSION = "create-user";

    /**
     * String given for connection creation permission.
     */
    private static final String CREATE_CONNECTION_PERMISSION = "create-connection";

    /**
     * String given for system administration permission.
     */
    private static final String ADMIN_PERMISSION = "admin";

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
     * Given a permission string, returns the corresponding system permission.
     *
     * @param str The permission string to parse.
     * @return The parsed system permission.
     * @throws GuacamoleException If the given string could not be parsed.
     */
    private Permission parseSystemPermission(String str)
            throws GuacamoleException {

        // Create user
        if (str.startsWith(CREATE_USER_PERMISSION))
            return new SystemPermission(SystemPermission.Type.CREATE_USER);

        // Create connection
        if (str.startsWith(CREATE_CONNECTION_PERMISSION))
            return new SystemPermission(SystemPermission.Type.CREATE_CONNECTION);

        // Administration
        if (str.startsWith(ADMIN_PERMISSION))
            return new SystemPermission(SystemPermission.Type.ADMINISTER);

        throw new GuacamoleException("Invalid permission string.");

    }

    /**
     * Given a permission string, returns the corresponding user permission.
     *
     * @param str The permission string to parse.
     * @return The parsed user permission.
     * @throws GuacamoleException If the given string could not be parsed.
     */
    private Permission parseUserPermission(String str)
            throws GuacamoleException {

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

        throw new GuacamoleClientException("Invalid permission string.");

    }

    @Override
    protected void authenticatedService(
            UserContext context,
            HttpServletRequest request, HttpServletResponse response)
    throws GuacamoleException {

        // Create user as specified
        String username = request.getParameter("name");
        String password = request.getParameter("password");

        // Attempt to get user directory
        Directory<String, User> directory =
                context.getUserDirectory();

        // Get user data, setting password if given
        User user = directory.get(username);
        user.setUsername(username);
        if (password != null)
            user.setPassword(password);

        /*
         * NEW PERMISSIONS
         */

        // Set added system permissions
        String[] add_sys_permission = request.getParameterValues("+sys");
        if (add_sys_permission != null) {
            for (String str : add_sys_permission)
                user.addPermission(parseSystemPermission(str));
        }

        // Set added user permissions
        String[] add_user_permission = request.getParameterValues("+user");
        if (add_user_permission != null) {
            for (String str : add_user_permission)
                user.addPermission(parseUserPermission(str));
        }

        // Set added connection permissions
        String[] add_connection_permission = request.getParameterValues("+connection");
        if (add_connection_permission != null) {
            for (String str : add_connection_permission)
                user.addPermission(parseConnectionPermission(str));
        }

        /*
         * REMOVED PERMISSIONS
         */

        // Unset removed system permissions
        String[] remove_sys_permission = request.getParameterValues("-sys");
        if (remove_sys_permission != null) {
            for (String str : remove_sys_permission)
                user.removePermission(parseSystemPermission(str));
        }

        // Unset removed user permissions
        String[] remove_user_permission = request.getParameterValues("-user");
        if (remove_user_permission != null) {
            for (String str : remove_user_permission)
                user.removePermission(parseUserPermission(str));
        }

        // Unset removed connection permissions
        String[] remove_connection_permission = request.getParameterValues("-connection");
        if (remove_connection_permission != null) {
            for (String str : remove_connection_permission)
                user.removePermission(parseConnectionPermission(str));
        }

        // Update user
        directory.update(user);

    }

}

