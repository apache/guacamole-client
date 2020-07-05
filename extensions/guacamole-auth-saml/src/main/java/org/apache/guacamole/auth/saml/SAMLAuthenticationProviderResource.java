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
import com.onelogin.saml2.authn.SamlResponse;
import com.onelogin.saml2.exception.SettingsException;
import com.onelogin.saml2.exception.ValidationError;
import com.onelogin.saml2.http.HttpRequest;
import com.onelogin.saml2.servlet.ServletUtils;
import com.onelogin.saml2.settings.Saml2Settings;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.FormParam;
import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.auth.saml.conf.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * A class that implements the REST API necessary for the
 * SAML Idp to POST back its response to Guacamole.
 */
public class SAMLAuthenticationProviderResource {

    /**
     * Logger for this class.
     */
    private final Logger logger =
            LoggerFactory.getLogger(SAMLAuthenticationProviderResource.class);
    
    /**
     * The configuration service for this module.
     */
    @Inject
    private ConfigurationService confService;
    
    /**
     * The map used to track active responses.
     */
    @Inject
    private SAMLResponseMap samlResponseMap;

    /**
     * A REST endpoint that is POSTed to by the SAML IdP
     * with the results of the SAML SSO Authentication.
     * 
     * @param samlResponseString
     *     The encoded response returned by the SAML IdP.
     * 
     * @param consumedRequest
     *     The HttpServletRequest associated with the SAML response. The
     *     parameters of this request may not be accessible, as the request may
     *     have been fully consumed by JAX-RS.
     * 
     * @return
     *     A HTTP Response that will redirect the user back to the
     *     Guacamole home page, with the SAMLResponse encoded in the
     *     return URL.
     * 
     * @throws GuacamoleException
     *     If the Guacamole configuration cannot be read or an error occurs
     *     parsing a URI.
     */
    @POST
    @Path("callback")
    public Response processSamlResponse(
            @FormParam("SAMLResponse") String samlResponseString,
            @Context HttpServletRequest consumedRequest)
            throws GuacamoleException {
        
        URI guacBase = confService.getCallbackUrl();
        Saml2Settings samlSettings = confService.getSamlSettings();
        try {
            HttpRequest request = ServletUtils
                    .makeHttpRequest(consumedRequest)
                    .addParameter("SAMLResponse", samlResponseString);
            SamlResponse samlResponse = new SamlResponse(samlSettings, request);
            
            String responseHash = hashSamlResponse(samlResponseString);
            samlResponseMap.putSamlResponse(responseHash, samlResponse);
            return Response.seeOther(UriBuilder.fromUri(guacBase)
                    .queryParam("responseHash", responseHash)
                    .build()
            ).build();

        }
        catch (IOException e) {
            throw new GuacamoleServerException("I/O exception processing SAML response.", e);
        }
        catch (NoSuchAlgorithmException e) {
            throw new GuacamoleServerException("Unexpected missing SHA-256 support while generating SAML response hash.", e);
        }
        catch (ParserConfigurationException e) {
            throw new GuacamoleServerException("Parser exception processing SAML response.", e);
        }
        catch (SAXException e) {
            throw new GuacamoleServerException("SAX exception processing SAML response.", e);
        }
        catch (SettingsException e) {
            throw new GuacamoleServerException("Settings exception processing SAML response.", e);
        }
        catch (ValidationError e) {
            throw new GuacamoleServerException("Exception validating SAML response.", e);
        }
        catch (XPathExpressionException e) {
            throw new GuacamoleServerException("XML Xpath exception validating SAML response.", e);
        }

    }
    
    /**
     * This is a utility method designed to generate a SHA-256 hash for the
     * given string representation of the SAMLResponse, throwing an exception
     * if, for some reason, the Java implementation in use doesn't support
     * SHA-256, and returning a hex-formatted hash value.
     * 
     * @param samlResponse
     *     The String representation of the SAML response.
     * 
     * @return
     *     A hex-formatted string of the SHA-256 hash.
     * 
     * @throws NoSuchAlgorithmException 
     *     If Java does not support SHA-256.
     */
    private String hashSamlResponse(String samlResponse)
            throws NoSuchAlgorithmException {
        
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return DatatypeConverter.printHexBinary(
                digest.digest(samlResponse.getBytes(StandardCharsets.UTF_8)));
    }

}
