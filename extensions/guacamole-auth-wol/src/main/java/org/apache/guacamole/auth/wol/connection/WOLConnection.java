/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.guacamole.auth.wol.connection;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.form.TextField;
import org.apache.guacamole.wol.WOLException;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.DelegatingConnection;
import org.apache.guacamole.wol.WakeOnLAN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Connection which delegates functionality to an underlying connection, but
 * which supplies attributes specific to Wake-on-LAN functionality.
 */
public class WOLConnection extends DelegatingConnection {
    
    /**
     * The logger for this class.
    */
    private static final Logger logger = LoggerFactory.getLogger(WOLConnection.class);

    /**
     * The name of the attribute for the MAC address of this host.
     */
    public static final String WOL_ATTRIBUTE_MAC_ADDRESS = "wol-mac-address";

    /**
     * The name of the attribute that stores the network address to use
     * to broadcast the WOL packet.
     */
    public static final String WOL_ATTRIBUTE_NET_BROADCAST = "wol-net-broadcast";

    /**
     * The default broadcast address to use if none is configured.
     */
    public static final String WOL_DEFAULT_BROADCAST = "255.255.255.255";
    
    /**
     * The form containing Wake-on-LAN attributes.
     */
    public static final Form WOL_ATTRIBUTE_FORM = new Form("wol-attributes",
        Arrays.asList(
                new TextField(WOL_ATTRIBUTE_MAC_ADDRESS),
                new TextField(WOL_ATTRIBUTE_NET_BROADCAST)
        )
    );
    
    /**
     * The collection of forms for Wake-on-LAN configuration.
     */
    public static final Collection<Form> ATTRIBUTES =
            Collections.unmodifiableCollection(Arrays.asList(
                    WOL_ATTRIBUTE_FORM
        )
    );

    /**
     * The array containing all of the WOL-specific attributes.
     */
    public static final List<String> WOL_ATTRIBUTES = Arrays.asList(
            WOL_ATTRIBUTE_MAC_ADDRESS,
            WOL_ATTRIBUTE_NET_BROADCAST
    );
    
    /**
     * Whether or not the user has access to update the data for this connection.
     */
    private final Boolean canUpdate;

    /**
     * Create a WOLConnection with the specified Connection object as the
     * base, and indicate whether the user has privileges to update this
     * connection.
     *
     * @param connection
     *     The Connection object to decorate.
     * 
     * @param canUpdate
     *     Whether or not the user can update this connection.
     */
    public WOLConnection(Connection connection, Boolean canUpdate) {

        super(connection);
        this.canUpdate = canUpdate;

    }

    /**
     * Get the undecorated version of the object represented by this
     * WOLConnection.
     *
     * @return
     *     The undecorated version of the Connection represented by this
     *     WOLConnection.
     */
    public Connection getUndecorated() {
        return getDelegateConnection();
    }

    @Override
    public Map<String, String> getAttributes() {
        
        Map<String, String> attributes = super.getAttributes();
        
        /* If user lacks update privilegs, just return what we have. */
        if (!canUpdate)
            return attributes;

        // Create a mutable copy of the attributes
        Map<String, String> effectiveAttributes = new HashMap<>(attributes);

        // Check to see if any need to be added or removed
        for (String attr : WOL_ATTRIBUTES)
            effectiveAttributes.putIfAbsent(attr, null);

        return effectiveAttributes;

    }

    @Override
    public void setAttributes(Map<String, String> setAttributes) {

        // Create a mutable copy of the attributes
        setAttributes =  new HashMap<>(setAttributes);

        if (!canUpdate)
            for (String attr : WOL_ATTRIBUTES)
                setAttributes.remove(attr);

        // Pass attributes on up
        super.setAttributes(setAttributes);

    }

    /**
     * Wake up the host associated with this connection.  This retrieves
     * the configured MAC address and broadcast address, if any, and triggers
     * the WakeOnLAN utility class to send the magic Wake-on-LAN packet to the
     * broadcast address with the specified MAC address payload.  If no MAC
     * address is configured an exception is triggered.  If no broadcast
     * address is specified the special "this network" address is used.
     *
     * @throws GuacamoleException
     *     If the MAC address is not configured or an error occurs sending
     *     the magic WOL packet.
     */
    public void wakeUpHost() throws GuacamoleException {

        // Retrieve the broadcast address, or use the default.
        Map<String, String> attributes = getAttributes();
        String strBroadcast = attributes.get(WOL_ATTRIBUTE_NET_BROADCAST);
        if (strBroadcast == null || strBroadcast.isEmpty())
            strBroadcast = WOL_DEFAULT_BROADCAST;

        try {

            InetAddress broadcast = InetAddress.getByName(strBroadcast);

            // Retrieve the MAC address, or throw exception if not configured.
            String mac = attributes.get(WOL_ATTRIBUTE_MAC_ADDRESS);
            if (mac == null || mac.isEmpty())
                throw new WOLException("MAC address not found for this connection.");

            // Send the magic wakeup packet.
            WakeOnLAN.sendMagicPacket(mac, broadcast);
        }
        catch (UnknownHostException e) {
            throw new WOLException(
                    "Could not resolve broadcast address.", e);
        }
    }

}
