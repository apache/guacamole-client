/*
 * Copyright (C) 2016 Glyptodon, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.glyptodon.guacamole.auth.json.user;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.ConnectionRecord;
import org.apache.guacamole.protocol.GuacamoleClientInformation;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.glyptodon.guacamole.auth.json.connection.ConnectionService;

/**
 * Connection implementation which automatically manages related UserData if
 * the connection is used. Connections which are marked as single-use will
 * be removed from the given UserData such that only the first connection
 * attempt can succeed.
 *
 * @author Michael Jumper
 */
public class UserDataConnection implements Connection {

    /**
     * Service for establishing and managing connections.
     */
    @Inject
    private ConnectionService connectionService;

    /**
     * A human-readable value which both uniquely identifies this connection
     * and serves as the connection display name.
     */
    private String identifier;

    /**
     * The UserData associated with this connection. This UserData will be
     * automatically updated as this connection is used.
     */
    private UserData data;

    /**
     * The connection entry for this connection within the associated UserData.
     */
    private UserData.Connection connection;

    /**
     * Initializes this UserDataConnection with the given data, unique
     * identifier, and connection information. This function MUST be invoked
     * before any particular UserDataConnection is actually used.
     *
     * @param data
     *     The UserData that this connection should manage.
     *
     * @param identifier
     *     The identifier associated with this connection within the given
     *     UserData.
     *
     * @param connection
     *     The connection data associated with this connection within the given
     *     UserData.
     *
     * @return
     *     A reference to this UserDataConnection.
     */
    public UserDataConnection init(UserData data, String identifier,
            UserData.Connection connection) {

        this.identifier = identifier;
        this.data = data;
        this.connection = connection;

        return this;

    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(String identifier) {
        throw new UnsupportedOperationException("UserDataConnection is immutable.");
    }

    @Override
    public String getName() {
        return identifier;
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException("UserDataConnection is immutable.");
    }

    @Override
    public String getParentIdentifier() {
        return UserContext.ROOT_CONNECTION_GROUP;
    }

    @Override
    public void setParentIdentifier(String parentIdentifier) {
        throw new UnsupportedOperationException("UserDataConnection is immutable.");
    }

    @Override
    public GuacamoleConfiguration getConfiguration() {

        // Generate configuration, using a skeleton configuration if generation
        // fails
        GuacamoleConfiguration config = connectionService.getConfiguration(connection);
        if (config == null)
            config = new GuacamoleConfiguration();

        return config;

    }

    @Override
    public void setConfiguration(GuacamoleConfiguration config) {
        throw new UnsupportedOperationException("UserDataConnection is immutable.");
    }

    @Override
    public Map<String, String> getAttributes() {
        return Collections.<String, String>emptyMap();
    }

    @Override
    public void setAttributes(Map<String, String> attributes) {
        throw new UnsupportedOperationException("UserDataConnection is immutable.");
    }

    @Override
    public Date getLastActive() {
        return null;
    }

    @Override
    public List<? extends ConnectionRecord> getHistory() throws GuacamoleException {
        return Collections.<ConnectionRecord>emptyList();
    }

    @Override
    public Set<String> getSharingProfileIdentifiers() throws GuacamoleException {
        return Collections.<String>emptySet();
    }

    @Override
    public int getActiveConnections() {
        return 0;
    }

    @Override
    public GuacamoleTunnel connect(GuacamoleClientInformation info)
            throws GuacamoleException {

        // Prevent future use immediately upon connect
        if (connection.isSingleUse()) {

            // Deny access if another user already used the connection
            if (data.removeConnection(getIdentifier()) == null)
                throw new GuacamoleSecurityException("Permission denied");

        }

        // Perform connection operation
        return connectionService.connect(connection, info);

    }

}
