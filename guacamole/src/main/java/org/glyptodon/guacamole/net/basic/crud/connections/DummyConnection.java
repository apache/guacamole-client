
package org.glyptodon.guacamole.net.basic.crud.connections;

import java.util.List;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.GuacamoleSocket;
import org.glyptodon.guacamole.net.auth.AbstractConnection;
import org.glyptodon.guacamole.net.auth.ConnectionRecord;
import org.glyptodon.guacamole.protocol.GuacamoleClientInformation;

/**
 * Basic Connection skeleton, providing a means of storing Connection data
 * prior to CRUD operations. This Connection has no functionality for actually
 * performing a connection operation, and does not promote any of the
 * semantics that would otherwise be present because of the authentication
 * provider. It is up to the authentication provider to create a new
 * Connection based on the information contained herein.
 *
 * @author Michael Jumper
 */
public class DummyConnection extends AbstractConnection {

    @Override
    public GuacamoleSocket connect(GuacamoleClientInformation info) throws GuacamoleException {
        throw new UnsupportedOperationException("Connection unsupported in DummyConnection.");
    }

    @Override
    public List<ConnectionRecord> getHistory() throws GuacamoleException {
        throw new UnsupportedOperationException("History unsupported in DummyConnection.");
    }

}
