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

package org.glyptodon.guacamole.net.basic.rest.language;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.basic.rest.AuthProviderRESTExposure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A REST Service for handling the listing of languages.
 * 
 * @author James Muehlner
 */
@Path("/languages")
@Produces(MediaType.APPLICATION_JSON)
public class LanguageRESTService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(LanguageRESTService.class);
    
    /**
     * The path to the translation folder within the webapp.
     */
    private static final String TRANSLATION_PATHS = "/translations";
    
    /**
     * The JSON property for the human readable display name.
     */
    private static final String LANGUAGE_DISPLAY_NAME_KEY = "NAME";
    
    /**
     * The Jackson parser for parsing the language JSON files.
     */
    private static final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * The regular expression to use for parsing the language key from the
     * filename.
     */
    private static final Pattern LANGUAGE_KEY_PATTERN = Pattern.compile(".*/([a-z]+_[A-Z]+)\\.json");
    
    /**
     * Returns a map of all available language keys to their corresponding
     * human-readable names.
     * 
     * @param authToken
     *     The authentication token that is used to authenticate the user
     *     performing the operation.
     *
     * @param servletContext
     *     The ServletContext associated with the request.
     *
     * @return
     *     A list of languages defined in the system, consisting of 
     *     language display name and key.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the available languages.
     */
    @GET
    @AuthProviderRESTExposure
    public Map<String, String> getLanguages(@QueryParam("token") String authToken,
            @Context ServletContext servletContext) throws GuacamoleException {
        
        // Get the paths of all the translation files
        Set<String> resourcePaths = servletContext.getResourcePaths(TRANSLATION_PATHS);
        
        // If no translation files found, return an empty map
        if (resourcePaths == null)
            return Collections.EMPTY_MAP;
        
        Map<String, String> languageMap = new HashMap<String, String>();
        
        // Iterate through all the found language files and add them to the return map
        for(String resourcePath : resourcePaths) {
            
            // Get input stream for language file
            InputStream languageFileStream = servletContext.getResourceAsStream(resourcePath); 
            if (languageFileStream == null) {
                logger.warn("Unable to read language resource \"{}\"", resourcePath);
                continue;
            }
            
            try {

                // Parse language key from filename
                String languageKey;
                Matcher languageKeyMatcher = LANGUAGE_KEY_PATTERN.matcher(resourcePath);
                if (!languageKeyMatcher.matches() || (languageKey = languageKeyMatcher.group(1)) == null) {
                    logger.warn("Invalid language file name: \"{}\"", resourcePath);
                    continue;
                }
                
                // Get name node of language
                JsonNode tree = mapper.readTree(languageFileStream);
                JsonNode nameNode = tree.get(LANGUAGE_DISPLAY_NAME_KEY);
                
                // Attempt to read language name from node
                String languageName;
                if (nameNode == null || (languageName = nameNode.getTextValue()) == null) {
                    logger.warn("Root-level \"" + LANGUAGE_DISPLAY_NAME_KEY + "\" string missing or invalid in language file \"{}\"", resourcePath);
                    languageName = languageKey;
                }
                
                // Add language key/name pair to map
                languageMap.put(languageKey, languageName);
                
            }
            catch (IOException e) {
                logger.warn("Unable to read language resource \"{}\": {}", resourcePath, e.getMessage());
                logger.debug("Error while reading language resource.", e);
            }
        }
        
        return languageMap;
    }
    
}