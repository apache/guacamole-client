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

package org.glyptodon.guacamole.net.basic.extension;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.glyptodon.guacamole.net.basic.resource.Resource;
import org.glyptodon.guacamole.net.basic.resource.WebApplicationResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service which provides access to all built-in languages as resources, and
 * allows other resources to be added or overlaid against existing resources.
 *
 * @author Michael Jumper
 */
public class LanguageResourceService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(LanguageResourceService.class);
    
    /**
     * The path to the translation folder within the webapp.
     */
    private static final String TRANSLATION_PATH = "/translations";
    
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
     * Map of all language resources by language key. Language keys are
     * language and country code pairs, separated by an underscore, like
     * "en_US".
     */
    private final Map<String, Resource> resources = new HashMap<String, Resource>();

    /**
     * Derives a language key from the filename within the given path, if
     * possible. If the filename is not a valid language key, null is returned.
     *
     * @param path
     *     The path containing the filename to derive the language key from.
     *
     * @return
     *     The derived language key, or null if the filename is not a valid
     *     language key.
     */
    public String getLanguageKey(String path) {

        // Parse language key from filename
        Matcher languageKeyMatcher = LANGUAGE_KEY_PATTERN.matcher(path);
        if (!languageKeyMatcher.matches())
            return null;

        // Return parsed key
        return languageKeyMatcher.group(1);

    }

    /**
     * Adds or overlays the given language resource, which need not exist in
     * the ServletContext. If a language resource is already defined for the
     * given language key, the strings from the given resource will be overlaid
     * on top of the existing strings, augmenting or overriding the available
     * strings for that language.
     *
     * @param key
     *     The language key of the resource being added. Language keys are
     *     pairs consisting of a language code followed by an underscore and
     *     country code, such as "en_US".
     *
     * @param resource
     *     The language resource to add. This resource must have the mimetype
     *     "application/json".
     */
    public void addLanguageResource(String key, Resource resource) {

        // Merge language resources if already defined
        Resource existing = resources.get(key);
        if (existing != null) {
            // TODO: Merge
            logger.debug("Merged strings with existing language: \"{}\"", key);
        }

        // Otherwise, add new language resource
        else {
            resources.put(key, resource);
            logger.debug("Added language: \"{}\"", key);
        }

    }

    /**
     * Adds or overlays all languages defined within the /translations
     * directory of the given ServletContext. If no such language files exist,
     * nothing is done. If a language is already defined, the strings from the
     * will be overlaid on top of the existing strings, augmenting or
     * overriding the available strings for that language. The language key
     * for each language file is derived from the filename.
     *
     * @param context
     *     The ServletContext from which language files should be loaded.
     */
    public void addLanguageResources(ServletContext context) {

        // Get the paths of all the translation files
        Set<?> resourcePaths = context.getResourcePaths(TRANSLATION_PATH);
        
        // If no translation files found, nothing to add
        if (resourcePaths == null)
            return;
        
        // Iterate through all the found language files and add them to the map
        for (Object resourcePathObject : resourcePaths) {

            // Each resource path is guaranteed to be a string
            String resourcePath = (String) resourcePathObject;

            // Parse language key from path
            String languageKey = getLanguageKey(resourcePath);
            if (languageKey == null) {
                logger.warn("Invalid language file name: \"{}\"", resourcePath);
                continue;
            }

            // Add/overlay new resource
            addLanguageResource(
                languageKey,
                new WebApplicationResource(context, "application/json", resourcePath)
            );

        }

    }

    /**
     * Returns a set of all unique language keys currently associated with
     * language resources stored in this service. The returned set cannot be
     * modified.
     *
     * @return
     *     A set of all unique language keys currently associated with this
     *     service.
     */
    public Set<String> getLanguageKeys() {
        return Collections.unmodifiableSet(resources.keySet());
    }

    /**
     * Returns a map of all languages currently associated with this service,
     * where the key of each map entry is the language key. The returned map
     * cannot be modified.
     *
     * @return
     *     A map of all languages currently associated with this service.
     */
    public Map<String, Resource> getLanguageResources() {
        return Collections.unmodifiableMap(resources);
    }

    /**
     * Returns a mapping of all language keys to their corresponding human-
     * readable language names. If an error occurs while parsing a language
     * resource, its key/name pair will simply be omitted. The returned map
     * cannot be modified.
     *
     * @return
     *     A map of all language keys and their corresponding human-readable
     *     names.
     */
    public Map<String, String> getLanguageNames() {

        Map<String, String> languageNames = new HashMap<String, String>();

        // For each language key/resource pair
        for (Map.Entry<String, Resource> entry : resources.entrySet()) {

            // Get language key and resource
            String languageKey = entry.getKey();
            Resource resource = entry.getValue();

            // Get stream for resource
            InputStream resourceStream = resource.asStream();
            if (resourceStream == null) {
                logger.warn("Expected language resource does not exist: \"{}\".", languageKey);
                continue;
            }
            
            // Get name node of language
            try {
                JsonNode tree = mapper.readTree(resourceStream);
                JsonNode nameNode = tree.get(LANGUAGE_DISPLAY_NAME_KEY);
                
                // Attempt to read language name from node
                String languageName;
                if (nameNode == null || (languageName = nameNode.getTextValue()) == null) {
                    logger.warn("Root-level \"" + LANGUAGE_DISPLAY_NAME_KEY + "\" string missing or invalid in language \"{}\"", languageKey);
                    languageName = languageKey;
                }
                
                // Add language key/name pair to map
                languageNames.put(languageKey, languageName);

            }

            // Continue with next language if unable to read
            catch (IOException e) {
                logger.warn("Unable to read language resource \"{}\".", languageKey);
                logger.debug("Error reading language resource.", e);
            }

        }
        
        return Collections.unmodifiableMap(languageNames);
        
    }
    
}
