/*
 * Copyright (C) 2015 Glyptodon LLC
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

package org.glyptodon.guacamole.auth.jdbc.connection;

import com.google.inject.Inject;
import java.util.Map;
import org.glyptodon.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;

/**
 * Implementation of GuacamoleConfiguration which loads parameter values only
 * if necessary, and only if allowed.
 *
 * @author Michael Jumper
 */
public class MySQLGuacamoleConfiguration extends GuacamoleConfiguration {

    /**
     * The user this configuration belongs to. Access is based on his/her
     * permission settings.
     */
    private AuthenticatedUser currentUser;

    /**
     * The internal model object containing the values which represent the
     * connection associated with this configuration.
     */
    private ConnectionModel connectionModel;

    /**
     * Service for managing connection parameters.
     */
    @Inject
    private ConnectionService connectionService;

    /**
     * The manually-set parameter map, if any.
     */
    private Map<String, String> parameters = null;
    
    /**
     * Creates a new, empty MySQLGuacamoleConfiguration.
     */
    public MySQLGuacamoleConfiguration() {
    }

    /**
     * Initializes this configuration, associating it with the current
     * authenticated user and populating it with data from the given model
     * object.
     *
     * @param currentUser
     *     The user that created or retrieved this configuration.
     *
     * @param connectionModel 
     *     The model object backing this configuration.
     */
    public void init(AuthenticatedUser currentUser, ConnectionModel connectionModel) {
        this.currentUser = currentUser;
        this.connectionModel = connectionModel;
    }

    @Override
    public String getProtocol() {
        return connectionModel.getProtocol();
    }

    @Override
    public void setProtocol(String protocol) {
        super.setProtocol(protocol);
        connectionModel.setProtocol(protocol);
    }


    @Override
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
        super.setParameters(parameters);
    }

    @Override
    public Map<String, String> getParameters() {

        // Retrieve visible parameters, if not overridden by setParameters()
        if (parameters == null) {

            // Retrieve all visible parameters
            Map<String, String> visibleParameters =
                    connectionService.retrieveParameters(currentUser, connectionModel.getIdentifier());

            // Use retrieved parameters to back future operations
            super.setParameters(visibleParameters);

        }

        return super.getParameters();

    }

}
