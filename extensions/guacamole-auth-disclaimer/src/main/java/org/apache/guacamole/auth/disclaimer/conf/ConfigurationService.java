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

package org.apache.guacamole.auth.disclaimer.conf;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.environment.LocalEnvironment;
import org.apache.guacamole.properties.BooleanGuacamoleProperty;
import org.apache.guacamole.properties.FileGuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;

/**
 * Configuration service for the Disclaimer authentication module.
 */
public class ConfigurationService {
    
    /**
     * The Guacamole server environment.
     */
    private final Environment environment = LocalEnvironment.getInstance();
    
    /**
     * The Guacamole property that contains the title of the disclaimer field.
     */
    private static final StringGuacamoleProperty DISCLAIMER_TITLE = new StringGuacamoleProperty() {
        
        @Override
        public String getName() { return "disclaimer-title"; }
        
    };
    
    /**
     * The Guacamole property that contains the text of the disclaimer, if an
     * external file is not referenced.
     */
    private static final StringGuacamoleProperty DISCLAIMER_TEXT = new StringGuacamoleProperty() {
        
        @Override
        public String getName() { return "disclaimer-text"; }
        
    };
    
    /**
     * The Guacamole property that contains the name of the file from which the
     * disclaimer text will be read, if not specified in the disclaimer-text
     * property.
     */
    private static final FileGuacamoleProperty DISCLAIMER_FILE = new FileGuacamoleProperty() {
    
        @Override
        public String getName() { return "disclaimer-file"; }
        
    };
    
    /**
     * A boolean property that indicates whether or not the disclaimer form
     * will show the last login time of the user.
     */
    private static final BooleanGuacamoleProperty DISCLAIMER_LAST_LOGIN = new BooleanGuacamoleProperty() {
        
        @Override
        public String getName() { return "disclaimer-show-last-login"; }
        
    };
    
    /**
     * Return the text to be displayed in the title of the disclaimer message.
     * 
     * @return
     *     The text to be displayed in the title of the disclaimer message.
     * 
     * @throws GuacamoleException 
     *     If guacamole.properties cannot be parsed or the property is missing.
     */
    public String getTitle() throws GuacamoleException {
        return environment.getRequiredProperty(DISCLAIMER_TITLE);
    }
    
    /**
     * Return the text that should be displayed in the body of the disclaimer
     * field, reading either the file specified in the configuration or the
     * text specified in the guacamole.properties file.
     * 
     * @return
     *     The text that should be displayed in the body of the disclaimer.
     * 
     * @throws GuacamoleException 
     *     If guacamole.properties cannot be parsed or the specified disclaimer
     *     file cannot be read and the disclaimer text property is not specified.
     */
    public String getDisclaimerText() throws GuacamoleException {
        File disclaimerFile = environment.getProperty(DISCLAIMER_FILE);
        if (disclaimerFile != null && disclaimerFile.exists()) {
            try {
                // Attempt to read the file and return the contents as a string.
                return Files.readString(disclaimerFile.toPath());
            }
            catch (IOException e) {
                throw new GuacamoleServerException("Failed to read the disclaimer file.", e);
            }
            // Read text from file and return.
        }
        
        // If a file is not specified, does not exist, or is not readable, then
        // the disclaimer-text property is required.
        return environment.getRequiredProperty(DISCLAIMER_TEXT);
        
    }
    
    /**
     * Returns true if the last login of the current user should be shown,
     * otherwise false. The default is true.
     * 
     * @return
     *     true if the last login of the user should be shown, otherwise
     *     false.
     * 
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public boolean getLastLogin() throws GuacamoleException {
        return environment.getProperty(DISCLAIMER_LAST_LOGIN, true);
    }
    
}
