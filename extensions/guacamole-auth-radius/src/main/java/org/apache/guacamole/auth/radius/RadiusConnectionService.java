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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.jradius.client.RadiusClient;
import net.jradius.dictionary.Attr_CleartextPassword;
import net.jradius.dictionary.Attr_ReplyMessage;
import net.jradius.dictionary.Attr_State;
import net.jradius.dictionary.Attr_UserName;
import net.jradius.dictionary.Attr_UserPassword;
import net.jradius.exception.RadiusException;
import net.jradius.packet.RadiusPacket;
import net.jradius.packet.AccessRequest;
import net.jradius.packet.attribute.AttributeList;
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
     * @return
     *     A RadiusClient instance, configured with server, shared secret,
     *     ports, and timeout, as configured in guacamole.properties.
     *
     * @throws GuacamoleException
     *     If an error occurs while parsing guacamole.properties, or if the
     *     configuration of RadiusClient fails.
     */
    private RadiusClient createRadiusConnection() throws GuacamoleException {

        // Create the RADIUS client with the configuration parameters
        try {
            return new RadiusClient(InetAddress.getByName(confService.getRadiusServer()),
                                            confService.getRadiusSharedSecret(),
                                            confService.getRadiusAuthPort(),
                                            confService.getRadiusAcctPort(),
                                            confService.getRadiusTimeout());
        }
        catch (UnknownHostException e) {
            logger.debug("Failed to resolve host.", e);
            throw new GuacamoleServerException("Unable to resolve RADIUS server host.", e);
        }
        catch (IOException e) {
            logger.debug("Failed to communicate with host.", e);
            throw new GuacamoleServerException("Failed to communicate with RADIUS server.", e);
        }

    }

    /**
     * Creates a new instance of RadiusAuthentictor, configured with
     * parameters specified within guacamole.properties.
     *
     * @param radiusClient
     *     A RadiusClient instance that has been initialized to
     *     communicate with a RADIUS server.
     *
     * @return
     *     A new RadiusAuthenticator instance which has been configured
     *     with parameters from guacamole.properties, or null if
     *     configuration fails.
     *
     * @throws GuacamoleException
     *     If the configuration cannot be read or the inner protocol is
     *     not configured when the client is set up for a tunneled
     *     RADIUS connection.
     */
    private RadiusAuthenticator setupRadiusAuthenticator(RadiusClient radiusClient)
            throws GuacamoleException {

        // If we don't have a radiusClient object, yet, don't go any further.
        if (radiusClient == null) {
            logger.error("RADIUS client hasn't been set up, yet.");
            logger.debug("We can't run this method until the RADIUS client has been set up.");
            return null;
        }

        RadiusAuthenticator radAuth = radiusClient.getAuthProtocol(confService.getRadiusAuthProtocol());
        if (radAuth == null)
            throw new GuacamoleException("Could not get a valid RadiusAuthenticator for specified protocol: " + confService.getRadiusAuthProtocol());

        // If we're using any of the TLS protocols, we need to configure them
        if (radAuth instanceof PEAPAuthenticator || 
            radAuth instanceof EAPTLSAuthenticator || 
            radAuth instanceof EAPTTLSAuthenticator) {

            // Pull TLS configuration parameters from guacamole.properties
            File caFile = confService.getRadiusCAFile();
            String caPassword = confService.getRadiusCAPassword();
            File keyFile = confService.getRadiusKeyFile();
            String keyPassword = confService.getRadiusKeyPassword();

            if (caFile != null) {
                ((EAPTLSAuthenticator)radAuth).setCaFile(caFile.toString());
                ((EAPTLSAuthenticator)radAuth).setCaFileType(confService.getRadiusCAType());
                if (caPassword != null)
                    ((EAPTLSAuthenticator)radAuth).setCaPassword(caPassword);
            }

            if (keyPassword != null)
                ((EAPTLSAuthenticator)radAuth).setKeyPassword(keyPassword);

            ((EAPTLSAuthenticator)radAuth).setKeyFile(keyFile.toString());
            ((EAPTLSAuthenticator)radAuth).setKeyFileType(confService.getRadiusKeyType());
            ((EAPTLSAuthenticator)radAuth).setTrustAll(confService.getRadiusTrustAll());
        }

        // If we're using EAP-TTLS, we need to define tunneled protocol
        if (radAuth instanceof EAPTTLSAuthenticator) {
            String innerProtocol = confService.getRadiusEAPTTLSInnerProtocol();
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
     *
     * @param secret
     *     The secret, usually a password or challenge response, to send
     *     to authenticate to the RADIUS server.
     *
     * @param state
     *     The previous state of the RADIUS connection
     *
     * @return
     *     A RadiusPacket with the response of the server.
     *
     * @throws GuacamoleException
     *     If an error occurs while talking to the server.
     */
    public RadiusPacket authenticate(String username, String secret, byte[] state)
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
            if (state != null && state.length > 0)
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
            RadiusResponse reply = radiusClient.sendReceive(radAcc, confService.getRadiusMaxRetries());

            // We receive a Challenge not asking for user input, so silently process the challenge
            while((reply instanceof AccessChallenge) && (reply.findAttribute(Attr_ReplyMessage.TYPE) == null)) {
                radAuth.processChallenge(radAcc, reply);
                reply = radiusClient.sendReceive(radAcc, confService.getRadiusMaxRetries());
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

    /**
     * Send a challenge response to the RADIUS server by validating the input and
     * then sending it along to the authenticate method.
     *
     * @param username
     *     The username to send to the RADIUS server for authentication.
     *
     * @param response
     *     The response phrase to send to the RADIUS server in response to the
     *     challenge previously provided.
     *
     * @param state
     *     The state data provided by the RADIUS server in order to continue
     *     the RADIUS conversation.
     *
     * @return
     *     A RadiusPacket containing the server's response to the authentication
     *     attempt.
     *
     * @throws GuacamoleException
     *     If an error is encountered trying to talk to the RADIUS server.
     */
    public RadiusPacket sendChallengeResponse(String username, String response, byte[] state)
            throws GuacamoleException {

        if (username == null || username.isEmpty()) {
            logger.error("Challenge/response to RADIUS requires a username.");
            return null;
        }

        if (state == null || state.length == 0) {
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
