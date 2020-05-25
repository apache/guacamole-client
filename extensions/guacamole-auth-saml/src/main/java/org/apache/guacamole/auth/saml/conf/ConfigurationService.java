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

package org.apache.guacamole.auth.saml.conf;

import com.google.inject.Inject;
import com.onelogin.saml2.settings.IdPMetadataParser;
import com.onelogin.saml2.settings.Saml2Settings;
import com.onelogin.saml2.settings.SettingsBuilder;
import com.onelogin.saml2.util.Constants;
import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.properties.BooleanGuacamoleProperty;
import org.apache.guacamole.properties.FileGuacamoleProperty;
import org.apache.guacamole.properties.URIGuacamoleProperty;

/**
 * Service for retrieving configuration information regarding the SAML
 * authentication module.
 */
public class ConfigurationService {

    /**
     * The file containing the XML Metadata associated with the SAML IdP.
     */
    private static final FileGuacamoleProperty SAML_IDP_METADATA =
            new FileGuacamoleProperty() {

        @Override
        public String getName() { return "saml-idp-metadata"; }

    };

    /**
     * The URL of the SAML IdP.
     */
    private static final URIGuacamoleProperty SAML_IDP_URL =
            new URIGuacamoleProperty() {

        @Override
        public String getName() { return "saml-idp-url"; }

    };

    /**
     * The URL identifier for this SAML client.
     */
    private static final URIGuacamoleProperty SAML_ENTITY_ID =
            new URIGuacamoleProperty() {

        @Override
        public String getName() { return "saml-entity-id"; }

    };

    /**
     * The callback URL to use for SAML IdP, normally the base
     * of the Guacamole install.
     */
    private static final URIGuacamoleProperty SAML_CALLBACK_URL =
            new URIGuacamoleProperty() {

        @Override
        public String getName() { return "saml-callback-url"; }

    };

    /**
     * The single logout redirect URL.
     */
    private static final URIGuacamoleProperty SAML_LOGOUT_URL =
            new URIGuacamoleProperty() {

        @Override
        public String getName() { return "saml-logout-url"; }

    };
    
    /**
     * Whether or not debugging should be enabled in the SAML library to help
     * track down errors.
     */
    private static final BooleanGuacamoleProperty SAML_DEBUG =
            new BooleanGuacamoleProperty() {
    
        @Override
        public String getName() { return "saml-debug"; }
                
    };
    
    /**
     * Whether or not to enabled compression for the SAML request.
     */
    private static final BooleanGuacamoleProperty SAML_COMPRESS_REQUEST =
            new BooleanGuacamoleProperty() {
            
        @Override
        public String getName() { return "saml-compress-request"; }
                
    };
    
    /**
     * Whether or not to enabled compression for the SAML response.
     */
    private static final BooleanGuacamoleProperty SAML_COMPRESS_RESPONSE =
            new BooleanGuacamoleProperty() {
            
        @Override
        public String getName() { return "saml-compress-response"; }
                
    };
    
    /**
     * Whether or not to enforce strict SAML security during processing.
     */
    private static final BooleanGuacamoleProperty SAML_STRICT =
            new BooleanGuacamoleProperty() {
            
        @Override
        public String getName() { return "saml-strict"; }
        
    };

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * Returns the URL to be used as the client ID which will be
     * submitted to the SAML IdP as configured in
     * guacamole.properties.
     *
     * @return
     *     The URL to be used as the client ID sent to the
     *     SAML IdP.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed, or if the
     *     property is missing.
     */
    private URI getEntityId() throws GuacamoleException {
        return environment.getRequiredProperty(SAML_ENTITY_ID);
    }

    /**
     * The file that contains the metadata that the SAML client should
     * use to communicate with the SAML IdP.  This is generated by the
     * SAML IdP and should be uploaded to the system where the Guacamole
     * client is running.
     *
     * @return
     *     The file containing the metadata used by the SAML client
     *     when it communicates with the SAML IdP.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed, or if the client
     *     metadata is missing.
     */
    private File getIdpMetadata() throws GuacamoleException {
        return environment.getProperty(SAML_IDP_METADATA);
    }

    /**
     * Retrieve the URL used to log in to the SAML IdP.
     *
     * @return
     *     The URL used to log in to the SAML IdP.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    private URI getIdpUrl() throws GuacamoleException {
        return environment.getProperty(SAML_IDP_URL);
    }

    /**
     * The callback URL used for the SAML IdP to POST a response
     * to upon completion of authentication, normally the base
     * of the Guacamole install.
     *
     * @return
     *     The callback URL to be sent to the SAML IdP that will
     *     be POSTed to upon completion of SAML authentication.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed, or if the
     *     callback parameter is missing.
     */
    public URI getCallbackUrl() throws GuacamoleException {
        return environment.getRequiredProperty(SAML_CALLBACK_URL);
    }

    /**
     * Return the URL used to log out from the SAML IdP.
     *
     * @return
     *     The URL used to log out from the SAML IdP.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    private URI getLogoutUrl() throws GuacamoleException {
        return environment.getProperty(SAML_LOGOUT_URL);
    }
    
    /**
     * Return true if SAML debugging should be enabled, otherwise false.  The
     * default is false.
     * 
     * @return
     *     True if debugging should be enabled in the SAML library, otherwise
     *     false.
     * 
     * @throws GuacamoleException 
     *     If guacamole.properties cannot be parsed.
     */
    private Boolean getDebug() throws GuacamoleException {
        return environment.getProperty(SAML_DEBUG, false);
    }
    
    /**
     * Return true if compression should be enabled when sending the SAML
     * request, otherwise false.  The default is to enable compression.
     * 
     * @return
     *     True if compression should be enabled when sending the SAML request,
     *     otherwise false.
     * 
     * @throws GuacamoleException 
     *     If guacamole.properties cannot be parsed.
     */
    private Boolean getCompressRequest() throws GuacamoleException {
        return environment.getProperty(SAML_COMPRESS_REQUEST, true);
    }
    
    /**
     * Returns whether or not the SAML login should enforce strict security
     * controls.  By default this is true, and should be set to true in any
     * production environment.
     * 
     * @return
     *     True if the SAML login should enforce strict security checks,
     *     otherwise false.
     * 
     * @throws GuacamoleException 
     *     If guacamole.properties cannot be parsed.
     */
    private Boolean getStrict() throws GuacamoleException {
        return environment.getProperty(SAML_STRICT, true);
    }
    
    /**
     * Return true if compression should be requested from the server when the
     * SAML response is returned, otherwise false.  The default is to request
     * that the response be compressed.
     * 
     * @return
     *     True if compression should be requested from the server for the SAML
     *     response.
     * 
     * @throws GuacamoleException 
     *     If guacamole.properties cannot be parsed.
     */
    private Boolean getCompressResponse() throws GuacamoleException {
        return environment.getProperty(SAML_COMPRESS_RESPONSE, true);
    }

    /**
     * Returns the collection of SAML settings used to
     * initialize the client.
     *
     * @return
     *     The collection of SAML settings used to 
     *     initialize the SAML client.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed or
     *     if parameters are missing.
     */
    public Saml2Settings getSamlSettings() throws GuacamoleException {

        File idpMetadata = getIdpMetadata();
        Map<String, Object> samlMap;
        if (idpMetadata != null) {
            try {
                samlMap = IdPMetadataParser.parseFileXML(idpMetadata.getAbsolutePath());
            }
            catch (Exception e) {
                throw new GuacamoleServerException(
                        "Could not parse SAML IdP Metadata file.", e);
            }
        }

        else {
            samlMap = new HashMap<>();
            samlMap.put(SettingsBuilder.SP_ENTITYID_PROPERTY_KEY,
                    getEntityId().toString());
            samlMap.put(SettingsBuilder.SP_ASSERTION_CONSUMER_SERVICE_URL_PROPERTY_KEY,
                    getCallbackUrl().toString() + "/api/ext/saml/callback");
            samlMap.put(SettingsBuilder.IDP_ENTITYID_PROPERTY_KEY
                    , getIdpUrl().toString());
            samlMap.put(SettingsBuilder.IDP_SINGLE_SIGN_ON_SERVICE_URL_PROPERTY_KEY,
                    getIdpUrl().toString());
            samlMap.put(SettingsBuilder.IDP_SINGLE_SIGN_ON_SERVICE_BINDING_PROPERTY_KEY,
                    Constants.BINDING_HTTP_REDIRECT);
        }
        
        SettingsBuilder samlBuilder = new SettingsBuilder();
        Saml2Settings samlSettings = samlBuilder.fromValues(samlMap).build();
        samlSettings.setStrict(getStrict());
        samlSettings.setDebug(getDebug());
        samlSettings.setCompressRequest(getCompressRequest());
        samlSettings.setCompressResponse(getCompressResponse());
    
        return samlSettings;
    }


}
