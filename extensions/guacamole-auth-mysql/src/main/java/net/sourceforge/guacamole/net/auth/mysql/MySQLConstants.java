
package net.sourceforge.guacamole.net.auth.mysql;

import net.sourceforge.guacamole.net.auth.ConnectionGroup;
import net.sourceforge.guacamole.net.auth.permission.ObjectPermission;
import net.sourceforge.guacamole.net.auth.permission.SystemPermission;

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
 * The Original Code is guacamole-auth-mysql.
 *
 * The Initial Developer of the Original Code is
 * James Muehlner.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
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

/**
 * A set of constants that are useful for the MySQL-based authentication provider.
 * @author James Muehlner
 */
public final class MySQLConstants {

    /**
     * This class should not be instantiated.
     */
    private MySQLConstants() {}

    /**
     * The string stored in the database to represent READ access to a user.
     */
    public static final String USER_READ = "READ";

    /**
     * The string stored in the database to represent UPDATE access to a user.
     */
    public static final String USER_UPDATE = "UPDATE";

    /**
     * The string stored in the database to represent DELETE access to a user.
     */
    public static final String USER_DELETE = "DELETE";

    /**
     * The string stored in the database to represent ADMINISTER access to a
     * user.
     */
    public static final String USER_ADMINISTER = "ADMINISTER";

    /**
     * The string stored in the database to represent READ access to a
     * connection.
     */
    public static final String CONNECTION_READ = "READ";

    /**
     * The string stored in the database to represent UPDATE access to a
     * connection.
     */
    public static final String CONNECTION_UPDATE = "UPDATE";

    /**
     * The string stored in the database to represent DELETE access to a
     * connection.
     */
    public static final String CONNECTION_DELETE = "DELETE";

    /**
     * The string stored in the database to represent ADMINISTER access to a
     * connection.
     */
    public static final String CONNECTION_ADMINISTER = "ADMINISTER";

    /**
     * The string stored in the database to represent READ access to a
     * connection.
     */
    public static final String CONNECTION_GROUP_READ = "READ";

    /**
     * The string stored in the database to represent UPDATE access to a
     * connection group.
     */
    public static final String CONNECTION_GROUP_UPDATE = "UPDATE";

    /**
     * The string stored in the database to represent DELETE access to a
     * connection group.
     */
    public static final String CONNECTION_GROUP_DELETE = "DELETE";

    /**
     * The string stored in the database to represent ADMINISTER access to a
     * connection group.
     */
    public static final String CONNECTION_GROUP_ADMINISTER = "ADMINISTER";

    /**
     * The string stored in the database to represent a BALANCING
     * connection group.
     */
    public static final String CONNECTION_GROUP_BALANCING = "BALANCING";

    /**
     * The string stored in the database to represent an ORGANIZATIONAL
     * connection group.
     */
    public static final String CONNECTION_GROUP_ORGANIZATIONAL = 
            "ORGANIZATIONAL";
    
    /**
     * The identifier used to mark the root connection group.
     */
    public static final String CONNECTION_GROUP_ROOT_IDENTIFIER = "ROOT";

    /**
     * The string stored in the database to represent permission to create
     * users.
     */
    public static final String SYSTEM_USER_CREATE = "CREATE_USER";

    /**
     * The string stored in the database to represent permission to create
     * connections.
     */
    public static final String SYSTEM_CONNECTION_CREATE = "CREATE_CONNECTION";

    /**
     * The string stored in the database to represent permission to create
     * connection groups.
     */
    public static final String SYSTEM_CONNECTION_GROUP_CREATE = "CREATE_CONNECTION_GROUP";

    /**
     * The string stored in the database to represent permission to administer
     * the system as a whole.
     */
    public static final String SYSTEM_ADMINISTER = "ADMINISTER";

    /**
     * Given the type of a permission affecting a user, returns the MySQL
     * constant representing that permission type.
     *
     * @param type The type of permission to look up.
     * @return The MySQL constant corresponding to the given permission type.
     */
    public static String getUserConstant(ObjectPermission.Type type) {

        // Convert permission type to MySQL constant
        switch (type) {
            case READ:       return USER_READ;
            case UPDATE:     return USER_UPDATE;
            case ADMINISTER: return USER_ADMINISTER;
            case DELETE:     return USER_DELETE;
        }

        // If we get here, permission support was not properly implemented
        throw new UnsupportedOperationException(
            "Unsupported permission type: " + type);

    }

    /**
     * Given the type of a permission affecting a connection, returns the MySQL
     * constant representing that permission type.
     *
     * @param type The type of permission to look up.
     * @return The MySQL constant corresponding to the given permission type.
     */
    public static String getConnectionConstant(ObjectPermission.Type type) {

        // Convert permission type to MySQL constant
        switch (type) {
            case READ:       return CONNECTION_READ;
            case UPDATE:     return CONNECTION_UPDATE;
            case ADMINISTER: return CONNECTION_ADMINISTER;
            case DELETE:     return CONNECTION_DELETE;
        }

        // If we get here, permission support was not properly implemented
        throw new UnsupportedOperationException(
            "Unsupported permission type: " + type);

    }

    /**
     * Given the type of a permission affecting a connection group, 
     * returns the MySQL constant representing that permission type.
     *
     * @param type The type of permission to look up.
     * @return The MySQL constant corresponding to the given permission type.
     */
    public static String getConnectionGroupConstant(ObjectPermission.Type type) {

        // Convert permission type to MySQL constant
        switch (type) {
            case READ:       return CONNECTION_GROUP_READ;
            case UPDATE:     return CONNECTION_GROUP_UPDATE;
            case ADMINISTER: return CONNECTION_GROUP_ADMINISTER;
            case DELETE:     return CONNECTION_GROUP_DELETE;
        }

        // If we get here, permission support was not properly implemented
        throw new UnsupportedOperationException(
            "Unsupported permission type: " + type);

    }

    /**
     * Given the type of a connection group, returns the MySQL constant
     * representing that type.
     *
     * @param type The connection group type to look up.
     * @return The MySQL constant corresponding to the given type.
     */
    public static String getConnectionGroupTypeConstant(ConnectionGroup.Type type) {

        // Convert permission type to MySQL constant
        switch (type) {
            case ORGANIZATIONAL: return CONNECTION_GROUP_ORGANIZATIONAL;
            case BALANCING:      return CONNECTION_GROUP_BALANCING;
        }

        // If we get here, permission support was not properly implemented
        throw new UnsupportedOperationException(
            "Unsupported connection group type: " + type);

    }

    /**
     * Given the type of a permission affecting the system, returns the MySQL
     * constant representing that permission type.
     *
     * @param type The type of permission to look up.
     * @return The MySQL constant corresponding to the given permission type.
     */
    public static String getSystemConstant(SystemPermission.Type type) {

        // Convert permission type to MySQL constant
        switch (type) {
            case CREATE_USER:             return SYSTEM_USER_CREATE;
            case CREATE_CONNECTION:       return SYSTEM_CONNECTION_CREATE;
            case CREATE_CONNECTION_GROUP: return SYSTEM_CONNECTION_GROUP_CREATE;
            case ADMINISTER:              return SYSTEM_ADMINISTER;
        }

        // If we get here, permission support was not properly implemented
        throw new UnsupportedOperationException(
            "Unsupported permission type: " + type);

    }

}
