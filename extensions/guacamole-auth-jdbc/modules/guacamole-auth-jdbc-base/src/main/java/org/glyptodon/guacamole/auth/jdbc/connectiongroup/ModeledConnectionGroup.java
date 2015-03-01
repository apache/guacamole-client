/*
 * Copyright (C) 2013 Glyptodon LLC
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

package org.glyptodon.guacamole.auth.jdbc.connectiongroup;

import com.google.inject.Inject;
import java.util.Set;
import org.glyptodon.guacamole.auth.jdbc.base.DirectoryObject;
import org.glyptodon.guacamole.auth.jdbc.connection.ConnectionService;
import org.glyptodon.guacamole.auth.jdbc.socket.GuacamoleSocketService;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.GuacamoleSocket;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.protocol.GuacamoleClientInformation;

/**
 * An implementation of the ConnectionGroup object which is backed by a
 * database model.
 *
 * @author James Muehlner
 */
public class ModeledConnectionGroup extends DirectoryObject<ConnectionGroupModel>
    implements ConnectionGroup {

    /**
     * Service for managing connections.
     */
    @Inject
    private ConnectionService connectionService;

    /**
     * Service for managing connection groups.
     */
    @Inject
    private ConnectionGroupService connectionGroupService;

    /**
     * Service for creating and tracking sockets.
     */
    @Inject
    private GuacamoleSocketService socketService;

    /**
     * Creates a new, empty ModeledConnectionGroup.
     */
    public ModeledConnectionGroup() {
    }

    @Override
    public String getName() {
        return getModel().getName();
    }

    @Override
    public void setName(String name) {
        getModel().setName(name);
    }

    @Override
    public String getParentIdentifier() {

        // Translate null parent to proper identifier
        String parentIdentifier = getModel().getParentIdentifier();
        if (parentIdentifier == null)
            return RootConnectionGroup.IDENTIFIER;

        return parentIdentifier;
        
    }

    @Override
    public void setParentIdentifier(String parentIdentifier) {

        // Translate root identifier back into null
        if (parentIdentifier != null
                && parentIdentifier.equals(RootConnectionGroup.IDENTIFIER))
            parentIdentifier = null;

        getModel().setParentIdentifier(parentIdentifier);

    }

    @Override
    public GuacamoleSocket connect(GuacamoleClientInformation info)
            throws GuacamoleException {
        return connectionGroupService.connect(getCurrentUser(), this, info);
    }

    @Override
    public int getActiveConnections() {
        return socketService.getActiveConnections(this).size();
    }

    @Override
    public void setType(Type type) {
        getModel().setType(type);
    }

    @Override
    public Type getType() {
        return getModel().getType();
    }

    @Override
    public Set<String> getConnectionIdentifiers()
            throws GuacamoleException {
        return connectionService.getIdentifiersWithin(getCurrentUser(), getIdentifier());
    }

    @Override
    public Set<String> getConnectionGroupIdentifiers()
            throws GuacamoleException {
        return connectionGroupService.getIdentifiersWithin(getCurrentUser(), getIdentifier());
    }

}
