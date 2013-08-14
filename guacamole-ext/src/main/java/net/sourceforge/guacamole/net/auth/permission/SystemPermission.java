
package net.sourceforge.guacamole.net.auth.permission;

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
 * A permission which affects the system as a whole, rather than an individual
 * object.
 *
 * @author Michael Jumper
 */
public class SystemPermission implements Permission<SystemPermission.Type> {

    /**
     * Specific types of system-level permissions. Each permission type is
     * related to a specific class of system-level operation.
     */
    public enum Type {

        /**
         * Create users.
         */
        CREATE_USER,

        /**
         * Create connections.
         */
        CREATE_CONNECTION,

        /**
         * Create connection groups.
         */
        CREATE_CONNECTION_GROUP,

        /**
         * Administer the system in general, including adding permissions
         * which affect the system (like user creation, connection creation,
         * and system administration).
         */
        ADMINISTER

    }

    /**
     * The type of operation affected by this permission.
     */
    private Type type;

    /**
     * Creates a new SystemPermission with the given
     * type.
     *
     * @param type The type of operation controlled by this permission.
     */
    public SystemPermission(Type type) {
        this.type = type;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public boolean equals(Object obj) {

        // Not equal if null or wrong type
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final SystemPermission other = (SystemPermission) obj;

        // Compare types
        if (type != other.type)
            return false;

        return true;
    }


}
