
package net.sourceforge.guacamole.net.auth.simple;

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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.GuacamoleSecurityException;
import net.sourceforge.guacamole.net.auth.PermissionDirectory;
import net.sourceforge.guacamole.net.auth.User;
import net.sourceforge.guacamole.net.auth.permission.GuacamoleConfigurationPermission;
import net.sourceforge.guacamole.net.auth.permission.ObjectPermission;
import net.sourceforge.guacamole.net.auth.permission.Permission;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;


/**
 * A simple read-only PermissionDirectory which manages the permissions for a
 * single user.
 * 
 * @author Michael Jumper
 */
public class SimplePermissionDirectory implements PermissionDirectory {

    /**
     * The username of the user that has access to all given configs.
     */
    private String user;

    /**
     * The identifiers of all available configs.
     */
    private Set<String> configIdentifiers;
    
    /**
     * Creates a new SimplePermissionDirectory which manages the permissions of
     * the given user and the given Map of GuacamoleConfigurations, which must
     * contain only those GuacamoleConfigurations the given user has access to.
     * 
     * @param user The user to manage permissions for.
     * @param configs All available configurations for the user given.
     */
    public SimplePermissionDirectory(User user,
            Map<String, GuacamoleConfiguration> configs) {

        this.user = user.getUsername();
        configIdentifiers = configs.keySet();
        
    }
    
    @Override
    public Set<Permission> getPermissions(String user) throws GuacamoleException {

        // No permssion to check permissions of other users
        if (!this.user.equals(user))
            throw new GuacamoleSecurityException("Permission denied.");
        
        // If correct user, build list all permissions
        Set<Permission> permissions = new HashSet<Permission>();
        for (String identifier : configIdentifiers) {
           
            // Add permission to set
            permissions.add(
                new GuacamoleConfigurationPermission(
                    ObjectPermission.Type.READ,
                    identifier
                )
            );
            
        }

        return permissions;
        
    }

    @Override
    public boolean hasPermission(String user, Permission permission) throws GuacamoleException {

        // No permssion to check permissions of other users
        if (!this.user.equals(user))
            throw new GuacamoleSecurityException("Permission denied.");
        
        // If correct user, validate config permission
        if (permission instanceof GuacamoleConfigurationPermission) {

            // Get permission
            GuacamoleConfigurationPermission guacConfigPerm =
                    (GuacamoleConfigurationPermission) permission;

            // If type is READ, permission given if the config exists in the set
            if (guacConfigPerm.getType() == ObjectPermission.Type.READ)
                return configIdentifiers.contains(guacConfigPerm.getObjectIdentifier());
            
        }

        // No permission by default
        return false;

    }

    @Override
    public void addPermission(String user, Permission permission) throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

    @Override
    public void removePermission(String user, Permission permission) throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

}
