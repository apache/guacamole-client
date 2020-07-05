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

package org.apache.guacamole.auth.saml;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.onelogin.saml2.authn.AuthnRequest;
import com.onelogin.saml2.authn.SamlResponse;
import com.onelogin.saml2.exception.SettingsException;
import com.onelogin.saml2.exception.ValidationError;
import com.onelogin.saml2.settings.Saml2Settings;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.guacamole.auth.saml.conf.ConfigurationService;
import org.apache.guacamole.auth.saml.user.SAMLAuthenticatedUser;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.form.Field;
import org.apache.guacamole.form.RedirectField;
import org.apache.guacamole.language.TranslatableMessage;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInsufficientCredentialsException;
import org.apache.guacamole.token.TokenName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Class that provides services for use by the SAMLAuthenticationProvider class.
 */
public class AuthenticationProviderService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationProviderService.class);

    /**
     * Service for retrieving SAML configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Provider for AuthenticatedUser objects.
     */
    @Inject
    private Provider<SAMLAuthenticatedUser> authenticatedUserProvider;
    
    /**
     * The map used to track active SAML responses.
     */
    @Inject
    private SAMLResponseMap samlResponseMap;
    
    private static final String SAML_ATTRIBUTE_TOKEN_PREFIX = "SAML_";

    /**
     * Returns an AuthenticatedUser representing the user authenticated by the
     * given credentials.
     *
     * @param credentials
     *     The credentials to use for authentication.
     *
     * @return
     *     An AuthenticatedUser representing the user authenticated by the
     *     given credentials.
     *
     * @throws GuacamoleException
     *     If an error occurs while authenticating the user, or if access is
     *     denied.
     */
    public AuthenticatedUser authenticateUser(Credentials credentials)
            throws GuacamoleException {
        
        HttpServletRequest request = credentials.getRequest();

        // Initialize and configure SAML client.
        Saml2Settings samlSettings = confService.getSamlSettings();

        if (request != null) {
            
            // Look for the SAML Response parameter.
            String responseHash = request.getParameter("responseHash");

            if (responseHash != null && samlResponseMap.hasSamlResponse(responseHash)) {

                try {
                        
                    SamlResponse samlResponse = samlResponseMap.getSamlResponse(responseHash);

                    if (!samlResponse.validateNumAssertions()) {
                        logger.warn("SAML response contained other than single assertion.");
                        logger.debug("validateNumAssertions returned false.");
                        throw new GuacamoleServerException("Unable to validate SAML assertions.");
                    }
                    
                    // Validate timestamps, generating ValidationException if this fails.
                    samlResponse.validateTimestamps();

                    // Grab the username, and, if present, finish authentication.
                    String username = samlResponse.getNameId();
                    if (username != null) {

                        // Canonicalize username as lowercase
                        username = username.toLowerCase();

                        // Retrieve any provided attributes
                        Map<String, List<String>> attributes =
                                samlResponse.getAttributes();
                        
                        // Back-port the username to the credentials
                        credentials.setUsername(username);
                        
                        // Configure the AuthenticatedUser and return it
                        SAMLAuthenticatedUser authenticatedUser =
                                authenticatedUserProvider.get();
                        
                        authenticatedUser.init(username, credentials,
                                parseTokens(attributes),
                                parseGroups(attributes, confService.getGroupAttribute()));
                        
                        return authenticatedUser;
                    }
                }

                // Catch errors and convert to a GuacamoleExcetion.
                catch (IOException e) {
                    logger.warn("Error during I/O while parsing SAML response: {}", e.getMessage());
                    logger.debug("Received IOException when trying to parse SAML response.", e);
                    throw new GuacamoleServerException("IOException received while processing SAML response.", e);
                }
                catch (ParserConfigurationException e) {
                    logger.warn("Error configuring XML parser: {}", e.getMessage());
                    logger.debug("Received ParserConfigurationException when trying to parse SAML response.", e);
                    throw new GuacamoleServerException("XML ParserConfigurationException received while processing SAML response.", e);
                }
                catch (SAXException e) {
                    logger.warn("Bad XML when parsing SAML response: {}", e.getMessage());
                    logger.debug("Received SAXException while parsing SAML response.", e);
                    throw new GuacamoleServerException("XML SAXException received while processing SAML response.", e);
                }
                catch (SettingsException e) {
                    logger.warn("Error with SAML settings while parsing response: {}", e.getMessage());
                    logger.debug("Received SettingsException while parsing SAML response.", e);
                    throw new GuacamoleServerException("SAML SettingsException received while process SAML response.", e);
                }
                catch (ValidationError e) {
                    logger.warn("Error validating SAML response: {}", e.getMessage());
                    logger.debug("Received ValidationError while parsing SAML response.", e);
                    throw new GuacamoleServerException("SAML ValidationError received while processing SAML response.", e);
                }
                catch (XPathExpressionException e) {
                    logger.warn("Problem with XML parsing response: {}", e.getMessage());
                    logger.debug("Received XPathExpressionException while processing SAML response.", e);
                    throw new GuacamoleServerException("XML XPathExpressionExcetion received while processing SAML response.", e);
                }
                catch (Exception e) {
                    logger.warn("Exception while getting name from SAML response: {}", e.getMessage());
                    logger.debug("Received Exception while retrieving name from SAML response.", e);
                    throw new GuacamoleServerException("Generic Exception received processing SAML response.", e);
                }
            }
        }

        // No SAML Response is present, or hash is not present in map.
        AuthnRequest samlReq = new AuthnRequest(samlSettings);
        URI authUri;
        try {
            authUri = UriBuilder.fromUri(samlSettings.getIdpSingleSignOnServiceUrl().toURI())
                    .queryParam("SAMLRequest", samlReq.getEncodedAuthnRequest())
                    .build();
        }
        catch (IOException e) {
            logger.error("Error encoding authentication request to string: {}", e.getMessage());
            logger.debug("Got IOException encoding authentication request.", e);
            throw new GuacamoleServerException("IOException received while generating SAML authentication URI.", e);
        }
        catch(URISyntaxException e) {
            logger.error("Error generating URI for authentication redirect: {}", e.getMessage());
            logger.debug("Got URISyntaxException generating authentication URI", e);
            throw new GuacamoleServerException("URISyntaxException received while generating SAML authentication URI.", e);
        }

        // Redirect to SAML Identity Provider (IdP)
        throw new GuacamoleInsufficientCredentialsException("Redirecting to SAML IdP.",
                new CredentialsInfo(Arrays.asList(new Field[] {
                    new RedirectField("samlRedirect", authUri, new TranslatableMessage("LOGIN.INFO_SAML_REDIRECT_PENDING"))
                }))
        );

    }
    
    /**
     * Generates Map of tokens that can be substituted within Guacamole
     * parameters given a Map containing a List of attributes from the SAML IdP. 
     * Attributes that have multiple values will be reduced to a single value,
     * taking the first available value and discarding the remaining values.
     * 
     * @param attributes
     *     The Map containing the attributes retrieved from the SAML IdP.
     * 
     * @return
     *     A Map of key and single value pairs that can be used as parameter
     *     tokens.
     */
    private Map<String, String> parseTokens(Map<String,
            List<String>> attributes) {
        
        Map<String, String> tokens = new HashMap<>();
        for (Entry<String, List<String>> entry : attributes.entrySet()) {
            
            List<String> values = entry.getValue();
            tokens.put(TokenName.canonicalize(
                    entry.getKey(), SAML_ATTRIBUTE_TOKEN_PREFIX),
                    values.get(0));
            
        }
        
        return tokens;
        
    }
    
    /**
     * Returns a list of groups found in the provided Map of attributes returned
     * by the SAML IdP by searching the map for the provided group attribute.
     * 
     * @param attributes
     *     The Map of attributes provided by the SAML IdP.
     * 
     * @param groupAttribute
     *     The name of the attribute that may be present in the Map that
     *     will be used to parse group membership for the authenticated user.
     * 
     * @return
     *     A Set of groups of which the user is a member.
     */
    private Set<String> parseGroups(Map<String, List<String>> attributes,
            String groupAttribute) {
        
        List<String> samlGroups = attributes.get(groupAttribute);
        if (samlGroups != null && !samlGroups.isEmpty())
            return Collections.unmodifiableSet(new HashSet<>(samlGroups));
        
        return Collections.emptySet();
    }

}
