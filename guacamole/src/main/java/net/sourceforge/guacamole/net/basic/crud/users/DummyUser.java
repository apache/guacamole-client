
package net.sourceforge.guacamole.net.basic.crud.users;

import java.util.HashSet;
import java.util.Set;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.auth.AbstractUser;
import net.sourceforge.guacamole.net.auth.permission.Permission;

/**
 * Basic User skeleton, providing a means of storing User data prior to CRUD
 * operations. This User does not promote any of the semantics that would
 * otherwise be present because of the authentication provider. It is up to the
 * authentication provider to create a new User based on the information
 * contained herein.
 *
 * @author Michael Jumper
 */
public class DummyUser extends AbstractUser {

    /**
     * Set of all available permissions.
     */
    private Set<Permission> permissions = new HashSet<Permission>();

    @Override
    public Set<Permission> getPermissions() throws GuacamoleException {
        return permissions;
    }

    @Override
    public boolean hasPermission(Permission permission) throws GuacamoleException {
        return permissions.contains(permission);
    }

    @Override
    public void addPermission(Permission permission) throws GuacamoleException {
        permissions.add(permission);
    }

    @Override
    public void removePermission(Permission permission) throws GuacamoleException {
        permissions.remove(permission);
    }

}
