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

package org.apache.guacamole.auth.radius;

import com.google.inject.Inject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleUnsupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.jradius.client.RadiusClient;
import net.jradius.exception.RadiusException;
import net.jradius.packet.RadiusPacket;
import net.jradius.packet.AccessRequest;
import net.jradius.dictionary.*;
import net.jradius.packet.attribute.AttributeList;
import net.jradius.client.auth.EAPTLSAuthenticator;
import net.jradius.client.auth.EAPTTLSAuthenticator;
import net.jradius.client.auth.RadiusAuthenticator;
import net.jradius.client.auth.PEAPAuthenticator;
import net.jradius.packet.attribute.AttributeFactory;

/**
 * Service for creating and managing connections to RADIUS servers.
 *
 * @author Nick Couchman
 */
public class RadiusConnectionService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(RadiusConnectionService.class);

    /**
     * Service for retrieving RADIUS server configuration information.
     */
    @Inject
    private ConfigurationService confService;


    /**
     * The RADIUS client;
     */
    private RadiusClient radiusClient;

    /**
     * Creates a new instance of RadiusConnection, configured with parameters
     * from guacamole.properties.
     *
     * @return
     *     A new RadiusConnection instance which has already been configured 
     *     with parameters from guacamole.properties.
     *
     * @throws GuacamoleException
     *     If an error occurs while parsing guacamole.properties, or if the
     *     requested encryption method is actually not implemented (a bug).
     */
    private void createRadiusConnection() {

        // Create the RADIUS client with the configuration parameters
        try {
            radiusClient = new RadiusClient(InetAddress.getByName(confService.getRadiusServer()),
                                            confService.getRadiusSharedSecret(),
                                            confService.getRadiusAuthPort(),
                                            confService.getRadiusAcctPort(),
                                            confService.getRadiusTimeout());
        }
        catch (GuacamoleException e) {
            logger.error("Unable to initialize RADIUS client: {}", e.getMessage());
            logger.debug("Failed to init RADIUS client.", e);
            return;
        }
        catch (UnknownHostException e) {
            logger.error("Unable to resolve host: {}", e.getMessage());
            logger.debug("Failed to resolve host.", e);
            return;
        }
        catch (IOException e) {
            logger.error("Unable to communicate with host: {}", e.getMessage());
            logger.debug("Failed to communicate with host.", e);
            return;
        }

    }

    /**
     * Creates a new instance of RadiusAuthentictor, configured with
     * parameters specified within guacamole.properties.
     *
     * @return
     *     A new RadiusAuthenticator instance which has been configured
     *     with parameters from guacamole.properties, or null if
     *     configuration fails.
     *
     */
    private RadiusAuthenticator setupRadiusAuthenticator() {

        if(radiusClient == null) {
            logger.error("RADIUS client hasn't been set up, yet.");
            logger.debug("We can't run this method until the RADIUS client has been set up.");
            return null;
        }

        String radAuthName;
        String caFile;
        String caPassword;
        String caType;
        String keyFile;
        String keyPassword;
        String keyType;
        Boolean trustAll;
        String innerProtocol;

        // Pull configuration parameters from guacamole.properties
        try {
            radAuthName = confService.getRadiusAuthProtocol();
            caFile = confService.getRadiusCAFile();
            caPassword = confService.getRadiusCAPassword();
            caType = confService.getRadiusCAType();
            keyFile = confService.getRadiusKeyFile();
            keyPassword = confService.getRadiusKeyPassword();
            keyType = confService.getRadiusKeyType();
            trustAll = confService.getRadiusTrustAll();
            innerProtocol = confService.getRadiusEAPTTLSInnerProtocol();
            
        }
        catch (GuacamoleException e) {
            logger.error("Error retrieving configuration.");
            logger.debug("Error getting config parameters from file.");
            return null;
        }

        RadiusAuthenticator radAuth = radiusClient.getAuthProtocol(radAuthName);
        if(radAuth == null)
            return null;

        // If we're using any of the TLS protocols, we need to configure them
        if (radAuth instanceof PEAPAuthenticator || 
            radAuth instanceof EAPTLSAuthenticator || 
            radAuth instanceof EAPTTLSAuthenticator) {

            if (caFile != null && !caFile.isEmpty())
                ((EAPTLSAuthenticator)radAuth).setCaFile(caFile);

            if (caType != null && !caType.isEmpty())
                ((EAPTLSAuthenticator)radAuth).setCaFileType(caType);

            if (caPassword != null && !caPassword.isEmpty())
                ((EAPTLSAuthenticator)radAuth).setCaPassword(caPassword);

            if (keyFile != null && !keyFile.isEmpty())
                ((EAPTLSAuthenticator)radAuth).setKeyFile(keyFile);

            if (keyType != null && !keyType.isEmpty())
                ((EAPTLSAuthenticator)radAuth).setKeyFileType(keyType);

            if (keyPassword != null && !keyPassword.isEmpty())
                ((EAPTLSAuthenticator)radAuth).setKeyPassword(keyPassword);

            ((EAPTLSAuthenticator)radAuth).setTrustAll(trustAll);

        }

        // If we're using EAP-TTLS, we need to define tunneled protocol
        if (radAuth instanceof EAPTTLSAuthenticator) {

            if (innerProtocol == null || innerProtocol.isEmpty())
                return null;

            ((EAPTTLSAuthenticator)radAuth).setInnerProtocol(innerProtocol);

        }

        return radAuth;

    }

    /**
     * Authenticate to the RADIUS server and return the response from the
     * server.
     *
     * @param username
     *     The username for authentication.
     * @param password
     *     The password for authentication.
     *
     * @return
     *     A RadiusPacket with the response of the server.
     *
     * @throws GuacamoleException
     *     If an error occurs while talking to the server. 
     */
    public RadiusPacket authenticate(String username, String password) 
            throws GuacamoleException {

        // Create the connection and load the attribute dictionary
	createRadiusConnection();
        AttributeFactory.loadAttributeDictionary("net.jradius.dictionary.AttributeDictionaryImpl");

        // If the client is null, we return null - something has gone wrong
        if (radiusClient == null)
            return null;

        // If a username hasn't been provided, stop
        if (username == null || username.isEmpty()) {
            logger.warn("Anonymous access not allowed with RADIUS client.");
            return null;
        }

        // If a password hasn't been provided, stop
        if (password == null || password.isEmpty()) {
            logger.warn("Password required for RADIUS authentication.");
            return null;
        }

        RadiusAuthenticator radAuth = setupRadiusAuthenticator();

        if (radAuth == null)
            throw new GuacamoleException("Unknown RADIUS authentication protocol.");

        // Set up attributes, create the access request, and send the packet
        try { 
            AttributeList radAttrs = new AttributeList();
            radAttrs.add(new Attr_UserName(username));
            radAttrs.add(new Attr_UserPassword(password));
            AccessRequest radAcc = new AccessRequest(radiusClient);

            if (radAuth instanceof EAPTTLSAuthenticator)
                ((EAPTTLSAuthenticator)radAuth).setTunneledAttributes(radAttrs);
            else
                radAcc.addAttributes(radAttrs);

            radAuth.setupRequest(radiusClient, radAcc);
            radAuth.processRequest(radAcc);
            return radiusClient.sendReceive(radAcc, confService.getRadiusRetries());
        }

        catch (RadiusException e) {
            logger.error("Unable to complete authentication.", e.getMessage());
            logger.debug("Authentication with RADIUS failed.", e);
            return null;
        }

        catch (NoSuchAlgorithmException e) {
            logger.error("No such RADIUS algorithm: {}", e.getMessage());
            logger.debug("Unknown RADIUS algorithm.", e);
            return null;
        }
    }

    /**
     * Authenticate to the RADIUS server using existing state and a response
     *
     * @param username
     *     The username for the authentication
     * @param state
     *     The previous state of the RADIUS connection
     * @param response
     *     The response to the RADIUS challenge
     *
     * @return
     *     A RadiusPacket with the response of the server.
     *
     * @throws GuacamoleException
     *     If an error occurs while talking to the server.
     */
    public RadiusPacket authenticate(String username, String state, String response)
            throws GuacamoleException {

        // Create the RADIUS connection and set up the dictionary
        createRadiusConnection();
        AttributeFactory.loadAttributeDictionary("net.jradius.dictionary.AttributeDictionaryImpl");

        // Client failed to set up, so we return null
        if (radiusClient == null)
            return null;

        // If a username wasn't passed, we quit
        if (username == null || username.isEmpty()) {
            logger.warn("Anonymous access not allowed with RADIUS client.");
            return null;
        }

        // If the state wasn't passed, we quit
        if (state == null || state.isEmpty()) {
            logger.warn("This method needs a previous RADIUS state to respond to.");
            return null;
        }

        // If the response wasn't passed, we quit
        if (response == null || response.isEmpty()) {
            logger.warn("Response required for RADIUS authentication.");
            return null;
        }

        // Set up the RadiusAuthenticator
        RadiusAuthenticator radAuth = setupRadiusAuthenticator();
        if (radAuth == null)
            throw new GuacamoleException("Unknown RADIUS authentication protocol.");

        // Add attributes to the connection and send the packet
        try {
            AttributeList radAttrs = new AttributeList();
            radAttrs.add(new Attr_UserName(username));
            radAttrs.add(new Attr_State(state));
            radAttrs.add(new Attr_UserPassword(response));
            AccessRequest radAcc = new AccessRequest(radiusClient);

            if (radAuth instanceof EAPTTLSAuthenticator)
                ((EAPTTLSAuthenticator)radAuth).setTunneledAttributes(radAttrs);
            else
                radAcc.addAttributes(radAttrs);

            radAuth.setupRequest(radiusClient, radAcc);
            radAuth.processRequest(radAcc);
            return radiusClient.sendReceive(radAcc, confService.getRadiusRetries());
        }
        catch (RadiusException e) {
            logger.error("Unable to complete authentication.", e.getMessage());
            logger.debug("Authentication with RADIUS failed.", e);
            return null;
        }
        catch (NoSuchAlgorithmException e) {
            logger.error("No such RADIUS algorithm: {}", e.getMessage());
            logger.debug("Unknown RADIUS algorithm.", e);
            return null;
        }
    }

    /**
     * Disconnects the given RADIUS connection, logging any failure to do so
     * appropriately.
     *
     */
    public void disconnect() {

        radiusClient.close();

    }

}
