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
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleUnsupportedException;
import org.apache.guacamole.environment.LocalEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.jradius.client.RadiusClient;
import net.jradius.exception.RadiusException;
import net.jradius.packet.RadiusPacket;
import net.jradius.packet.AccessRequest;
import net.jradius.dictionary.*;
import net.jradius.packet.attribute.AttributeList;
import net.jradius.packet.attribute.RadiusAttribute;
import net.jradius.client.auth.EAPTLSAuthenticator;
import net.jradius.client.auth.EAPTTLSAuthenticator;
import net.jradius.client.auth.RadiusAuthenticator;
import net.jradius.client.auth.PEAPAuthenticator;
import net.jradius.packet.attribute.AttributeFactory;
import net.jradius.packet.AccessChallenge;
import net.jradius.packet.RadiusResponse;

/**
 * Service for creating and managing connections to RADIUS servers.
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
     * Creates a new instance of RadiusClient, configured with parameters
     * from guacamole.properties.
     *
     * @throws GuacamoleException
     *     If an error occurs while parsing guacamole.properties, or if the
     *     configuration of RadiusClient fails.
     */
    private RadiusClient createRadiusConnection() {

        // Create the RADIUS client with the configuration parameters
        try {
            return new RadiusClient(InetAddress.getByName(confService.getRadiusServer()),
                                            confService.getRadiusSharedSecret(),
                                            confService.getRadiusAuthPort(),
                                            confService.getRadiusAcctPort(),
                                            confService.getRadiusTimeout());
        }
        catch (GuacamoleException e) {
            logger.error("Unable to initialize RADIUS client: {}", e.getMessage());
            logger.debug("Failed to init RADIUS client.", e);
        }
        catch (UnknownHostException e) {
            logger.error("Unable to resolve host: {}", e.getMessage());
            logger.debug("Failed to resolve host.", e);
        }
        catch (IOException e) {
            logger.error("Unable to communicate with host: {}", e.getMessage());
            logger.debug("Failed to communicate with host.", e);
        }

        return null;

    }

    /**
     * Creates a new instance of RadiusAuthentictor, configured with
     * parameters specified within guacamole.properties.
     *
     * @return
     *     A new RadiusAuthenticator instance which has been configured
     *     with parameters from guacamole.properties, or null if
     *     configuration fails.
     */
    private RadiusAuthenticator setupRadiusAuthenticator(RadiusClient radiusClient)
            throws GuacamoleException {

        // If we don't have a radiusClient object, yet, don't go any further.
        if (radiusClient == null) {
            logger.error("RADIUS client hasn't been set up, yet.");
            logger.debug("We can't run this method until the RADIUS client has been set up.");
            return null;
        }

        // Pull configuration parameters from guacamole.properties
        LocalEnvironment guacEnv = new LocalEnvironment();
        String guacHome = guacEnv.getGuacamoleHome().getAbsolutePath();
        String caFile = confService.getRadiusCAFile();
        String caPassword = confService.getRadiusCAPassword();
        String keyFile = confService.getRadiusKeyFile();
        String keyPassword = confService.getRadiusKeyPassword();
        String innerProtocol = confService.getRadiusEAPTTLSInnerProtocol();
            
        RadiusAuthenticator radAuth = radiusClient.getAuthProtocol(confService.getRadiusAuthProtocol());
        if (radAuth == null)
            throw new GuacamoleException("Could not get a valid RadiusAuthenticator for specified protocol: " + confService.getRadiusAuthProtocol());

        // If we're using any of the TLS protocols, we need to configure them
        if (radAuth instanceof PEAPAuthenticator || 
            radAuth instanceof EAPTLSAuthenticator || 
            radAuth instanceof EAPTTLSAuthenticator) {

            if (caFile != null) {
                ((EAPTLSAuthenticator)radAuth).setCaFile((new File(guacHome, caFile)).toString());
                ((EAPTLSAuthenticator)radAuth).setCaFileType(confService.getRadiusCAType());
                if (caPassword != null)
                    ((EAPTLSAuthenticator)radAuth).setCaPassword(caPassword);
            }

            if (keyPassword != null)
                ((EAPTLSAuthenticator)radAuth).setKeyPassword(keyPassword);

            ((EAPTLSAuthenticator)radAuth).setKeyFile((new File(guacHome, keyFile)).toString());
            ((EAPTLSAuthenticator)radAuth).setKeyFileType(confService.getRadiusKeyType());
            ((EAPTLSAuthenticator)radAuth).setTrustAll(confService.getRadiusTrustAll());
        }

        // If we're using EAP-TTLS, we need to define tunneled protocol
        if (radAuth instanceof EAPTTLSAuthenticator) {
            if (innerProtocol == null)
                throw new GuacamoleException("Trying to use EAP-TTLS, but no inner protocol specified.");

            ((EAPTTLSAuthenticator)radAuth).setInnerProtocol(innerProtocol);
        }

        return radAuth;

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
    public RadiusPacket authenticate(String username, String secret, String state)
            throws GuacamoleException {

        // If a username wasn't passed, we quit
        if (username == null || username.isEmpty()) {
            logger.warn("Anonymous access not allowed with RADIUS client.");
            return null;
        }

        // If secret wasn't passed, we quit
        if (secret == null || secret.isEmpty()) {
            logger.warn("Password/secret required for RADIUS authentication.");
            return null;
        }

        // Create the RADIUS connection and set up the dictionary
        RadiusClient radiusClient = createRadiusConnection();
        AttributeFactory.loadAttributeDictionary("net.jradius.dictionary.AttributeDictionaryImpl");

        // Client failed to set up, so we return null
        if (radiusClient == null)
            return null;

        // Set up the RadiusAuthenticator
        RadiusAuthenticator radAuth = setupRadiusAuthenticator(radiusClient);
        if (radAuth == null)
            throw new GuacamoleException("Unknown RADIUS authentication protocol.");

        // Add attributes to the connection and send the packet
        try {
            AttributeList radAttrs = new AttributeList();
            radAttrs.add(new Attr_UserName(username));
            if (state != null && !state.isEmpty())
                radAttrs.add(new Attr_State(state));
            radAttrs.add(new Attr_UserPassword(secret));
            radAttrs.add(new Attr_CleartextPassword(secret));

            AccessRequest radAcc = new AccessRequest(radiusClient);

            // EAP-TTLS tunnels protected attributes inside the TLS layer
            if (radAuth instanceof EAPTTLSAuthenticator) {
                radAuth.setUsername(new Attr_UserName(username));
                ((EAPTTLSAuthenticator)radAuth).setTunneledAttributes(radAttrs);
            }
            else
                radAcc.addAttributes(radAttrs);

            radAuth.setupRequest(radiusClient, radAcc);
            radAuth.processRequest(radAcc);
            RadiusResponse reply = radiusClient.sendReceive(radAcc, confService.getRadiusRetries());

            // We receive a Challenge not asking for user input, so silently process the challenge
            while((reply instanceof AccessChallenge) && (reply.findAttribute(Attr_ReplyMessage.TYPE) == null)) {
                radAuth.processChallenge(radAcc, reply);
                reply = radiusClient.sendReceive(radAcc, confService.getRadiusRetries());
            }
            return reply;
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
        finally {
            radiusClient.close();
        }
    }

    public RadiusPacket sendChallengeResponse(String username, String response, String state)
            throws GuacamoleException {

        if (username == null || username.isEmpty()) {
            logger.error("Challenge/response to RADIUS requires a username.");
            return null;
        }

        if (state == null || state.isEmpty()) {
            logger.error("Challenge/response to RADIUS requires a prior state.");
            return null;
        }

        if (response == null || response.isEmpty()) {
            logger.error("Challenge/response to RADIUS requires a response.");
            return null;
        }

        return authenticate(username,response,state);

    }

}
