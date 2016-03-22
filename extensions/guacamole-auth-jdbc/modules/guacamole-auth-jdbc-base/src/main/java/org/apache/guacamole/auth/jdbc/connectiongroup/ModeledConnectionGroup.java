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

package org.apache.guacamole.auth.jdbc.connectiongroup;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.guacamole.auth.jdbc.connection.ConnectionService;
import org.apache.guacamole.auth.jdbc.tunnel.GuacamoleTunnelService;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.jdbc.JDBCEnvironment;
import org.apache.guacamole.auth.jdbc.base.ModeledGroupedDirectoryObject;
import org.apache.guacamole.form.Field;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.form.NumericField;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.protocol.GuacamoleClientInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the ConnectionGroup object which is backed by a
 * database model.
 *
 * @author James Muehlner
 * @author Michael Jumper
 */
public class ModeledConnectionGroup extends ModeledGroupedDirectoryObject<ConnectionGroupModel>
    implements ConnectionGroup {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(ModeledConnectionGroup.class);

    /**
     * The name of the attribute which controls the maximum number of
     * concurrent connections.
     */
    public static final String MAX_CONNECTIONS_NAME = "max-connections";

    /**
     * The name of the attribute which controls the maximum number of
     * concurrent connections per user.
     */
    public static final String MAX_CONNECTIONS_PER_USER_NAME = "max-connections-per-user";

    /**
     * All attributes related to restricting user accounts, within a logical
     * form.
     */
    public static final Form CONCURRENCY_LIMITS = new Form("concurrency", Arrays.<Field>asList(
        new NumericField(MAX_CONNECTIONS_NAME),
        new NumericField(MAX_CONNECTIONS_PER_USER_NAME)
    ));

    /**
     * All possible attributes of connection group objects organized as
     * individual, logical forms.
     */
    public static final Collection<Form> ATTRIBUTES = Collections.unmodifiableCollection(Arrays.asList(
        CONCURRENCY_LIMITS
    ));

    /**
     * The environment of the Guacamole server.
     */
    @Inject
    private JDBCEnvironment environment;

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
     * Service for creating and tracking tunnels.
     */
    @Inject
    private GuacamoleTunnelService tunnelService;

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
    public GuacamoleTunnel connect(GuacamoleClientInformation info)
            throws GuacamoleException {
        return connectionGroupService.connect(getCurrentUser(), this, info);
    }

    @Override
    public int getActiveConnections() {
        return tunnelService.getActiveConnections(this).size();
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

    @Override
    public Map<String, String> getAttributes() {

        Map<String, String> attributes = new HashMap<String, String>();

        // Set connection limit attribute
        attributes.put(MAX_CONNECTIONS_NAME, NumericField.format(getModel().getMaxConnections()));

        // Set per-user connection limit attribute
        attributes.put(MAX_CONNECTIONS_PER_USER_NAME, NumericField.format(getModel().getMaxConnectionsPerUser()));

        return attributes;
    }

    @Override
    public void setAttributes(Map<String, String> attributes) {

        // Translate connection limit attribute
        try { getModel().setMaxConnections(NumericField.parse(attributes.get(MAX_CONNECTIONS_NAME))); }
        catch (NumberFormatException e) {
            logger.warn("Not setting maximum connections: {}", e.getMessage());
            logger.debug("Unable to parse numeric attribute.", e);
        }

        // Translate per-user connection limit attribute
        try { getModel().setMaxConnectionsPerUser(NumericField.parse(attributes.get(MAX_CONNECTIONS_PER_USER_NAME))); }
        catch (NumberFormatException e) {
            logger.warn("Not setting maximum connections per user: {}", e.getMessage());
            logger.debug("Unable to parse numeric attribute.", e);
        }

    }

    /**
     * Returns the maximum number of connections that should be allowed to this
     * connection group overall. If no limit applies, zero is returned.
     *
     * @return
     *     The maximum number of connections that should be allowed to this
     *     connection group overall, or zero if no limit applies.
     *
     * @throws GuacamoleException
     *     If an error occurs while parsing the concurrency limit properties
     *     specified within guacamole.properties.
     */
    public int getMaxConnections() throws GuacamoleException {

        // Pull default from environment if connection limit is unset
        Integer value = getModel().getMaxConnections();
        if (value == null)
            return environment.getDefaultMaxGroupConnections();

        // Otherwise use defined value
        return value;

    }

    /**
     * Returns the maximum number of connections that should be allowed to this
     * connection group for any individual user. If no limit applies, zero is
     * returned.
     *
     * @return
     *     The maximum number of connections that should be allowed to this
     *     connection group for any individual user, or zero if no limit
     *     applies.
     *
     * @throws GuacamoleException
     *     If an error occurs while parsing the concurrency limit properties
     *     specified within guacamole.properties.
     */
    public int getMaxConnectionsPerUser() throws GuacamoleException {

        // Pull default from environment if per-user connection limit is unset
        Integer value = getModel().getMaxConnectionsPerUser();
        if (value == null)
            return environment.getDefaultMaxGroupConnectionsPerUser();

        // Otherwise use defined value
        return value;

    }


}
