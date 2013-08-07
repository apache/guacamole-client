
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
 * The Original Code is guacamole-auth.
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

import java.util.Collections;
import java.util.List;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.GuacamoleSocket;
import net.sourceforge.guacamole.net.InetGuacamoleSocket;
import net.sourceforge.guacamole.net.SSLGuacamoleSocket;
import net.sourceforge.guacamole.net.auth.AbstractConnection;
import net.sourceforge.guacamole.net.auth.ConnectionRecord;
import net.sourceforge.guacamole.properties.GuacamoleProperties;
import net.sourceforge.guacamole.protocol.ConfiguredGuacamoleSocket;
import net.sourceforge.guacamole.protocol.GuacamoleClientInformation;
import net.sourceforge.guacamole.protocol.GuacamoleConfiguration;


/**
 * An extremely basic Connection implementation.
 *
 * @author Michael Jumper
 */
public class SimpleConnection extends AbstractConnection {

    /**
     * Backing configuration, containing all sensitive information.
     */
    private GuacamoleConfiguration config;

    /**
     * Creates a completely uninitialized SimpleConnection.
     */
    public SimpleConnection() {
    }

    /**
     * Creates a new SimpleConnection having the given identifier and
     * GuacamoleConfiguration.
     *
     * @param name The name to associate with this connection.
     * @param identifier The identifier to associate with this connection.
     * @param config The configuration describing how to connect to this
     *               connection.
     */
    public SimpleConnection(String name, String identifier,
            GuacamoleConfiguration config) {
        
        // Set name
        setName(name);

        // Set identifier
        setIdentifier(identifier);

        // Set config
        setConfiguration(config);
        this.config = config;

    }

    @Override
    public GuacamoleSocket connect(GuacamoleClientInformation info)
            throws GuacamoleException {

        // Get guacd connection parameters
        String hostname = GuacamoleProperties.getProperty(GuacamoleProperties.GUACD_HOSTNAME);
        int port = GuacamoleProperties.getProperty(GuacamoleProperties.GUACD_PORT);

        // If guacd requires SSL, use it
        if (GuacamoleProperties.getProperty(GuacamoleProperties.GUACD_SSL, false))
            return new ConfiguredGuacamoleSocket(
                new SSLGuacamoleSocket(hostname, port),
                config, info
            );

        // Return connected socket
        return new ConfiguredGuacamoleSocket(
            new InetGuacamoleSocket(hostname, port),
            config, info
        );

    }

    @Override
    public List<ConnectionRecord> getHistory() throws GuacamoleException {
        return Collections.EMPTY_LIST;
    }

}
