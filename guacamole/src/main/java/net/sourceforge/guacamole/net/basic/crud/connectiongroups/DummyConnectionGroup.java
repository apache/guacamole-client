
package net.sourceforge.guacamole.net.basic.crud.connectiongroups;

import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.GuacamoleSocket;
import net.sourceforge.guacamole.net.auth.AbstractConnectionGroup;
import net.sourceforge.guacamole.net.auth.Connection;
import net.sourceforge.guacamole.net.auth.ConnectionGroup;
import net.sourceforge.guacamole.net.auth.Directory;
import net.sourceforge.guacamole.protocol.GuacamoleClientInformation;

/**
 * Basic ConnectionGroup skeleton, providing a means of storing Connection data
 * prior to CRUD operations. This ConnectionGroup has no functionality for actually
 * performing a connection operation, and does not promote any of the
 * semantics that would otherwise be present because of the authentication
 * provider. It is up to the authentication provider to create a new
 * ConnectionGroup based on the information contained herein.
 *
 * @author James Muehlner
 */
public class DummyConnectionGroup extends AbstractConnectionGroup {

    @Override
    public GuacamoleSocket connect(GuacamoleClientInformation info) throws GuacamoleException {
        throw new UnsupportedOperationException("Connection unsupported in DummyConnectionGroup.");
    }

    @Override
    public Directory<String, Connection> getConnectionDirectory() throws GuacamoleException {
        throw new UnsupportedOperationException("Connection directory unsupported in DummyConnectionGroup.");
    }

    @Override
    public Directory<String, ConnectionGroup> getConnectionGroupDirectory() throws GuacamoleException {
        throw new UnsupportedOperationException("Connection group directory unsuppprted in DummyConnectionGroup.");
    }

}
