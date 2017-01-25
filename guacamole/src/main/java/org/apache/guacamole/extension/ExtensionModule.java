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

package org.apache.guacamole.extension;

import com.google.inject.Provides;
import com.google.inject.servlet.ServletModule;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.guacamole.auth.file.FileAuthenticationProvider;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.resource.Resource;
import org.apache.guacamole.resource.ResourceServlet;
import org.apache.guacamole.resource.SequenceResource;
import org.apache.guacamole.resource.WebApplicationResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Guice Module which loads all extensions within the
 * GUACAMOLE_HOME/extensions directory, if any.
 *
 * @author Michael Jumper
 */
public class ExtensionModule extends ServletModule {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(ExtensionModule.class);

    /**
     * The version strings of all Guacamole versions whose extensions are
     * compatible with this release.
     */
    private static final List<String> ALLOWED_GUACAMOLE_VERSIONS =
        Collections.unmodifiableList(Arrays.asList(
            "*",
            "0.9.11-incubating"
        ));

    /**
     * The name of the directory within GUACAMOLE_HOME containing any .jars
     * which should be included in the classpath of all extensions.
     */
    private static final String LIB_DIRECTORY = "lib";

    /**
     * The name of the directory within GUACAMOLE_HOME containing all
     * extensions.
     */
    private static final String EXTENSIONS_DIRECTORY = "extensions";

    /**
     * The string that the filenames of all extensions must end with to be
     * recognized as extensions.
     */
    private static final String EXTENSION_SUFFIX = ".jar";

    /**
     * The Guacamole server environment.
     */
    private final Environment environment;

    /**
     * All currently-bound authentication providers, if any.
     */
    private final List<AuthenticationProvider> boundAuthenticationProviders =
            new ArrayList<AuthenticationProvider>();

    /**
     * Service for adding and retrieving language resources.
     */
    private final LanguageResourceService languageResourceService;

    /**
     * Service for adding and retrieving HTML patch resources.
     */
    private final PatchResourceService patchResourceService;
    
    /**
     * Returns the classloader that should be used as the parent classloader
     * for all extensions. If the GUACAMOLE_HOME/lib directory exists, this
     * will be a classloader that loads classes from within the .jar files in
     * that directory. Lacking the GUACAMOLE_HOME/lib directory, this will
     * simply be the classloader associated with the ExtensionModule class.
     *
     * @return
     *     The classloader that should be used as the parent classloader for
     *     all extensions.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the classloader.
     */
    private ClassLoader getParentClassLoader() throws GuacamoleException {

        // Retrieve lib directory
        File libDir = new File(environment.getGuacamoleHome(), LIB_DIRECTORY);

        // If lib directory does not exist, use default class loader
        if (!libDir.isDirectory())
            return ExtensionModule.class.getClassLoader();

        // Return classloader which loads classes from all .jars within the lib directory
        return DirectoryClassLoader.getInstance(libDir);

    }

    /**
     * Creates a module which loads all extensions within the
     * GUACAMOLE_HOME/extensions directory.
     *
     * @param environment
     *     The environment to use when configuring authentication.
     */
    public ExtensionModule(Environment environment) {
        this.environment = environment;
        this.languageResourceService = new LanguageResourceService(environment);
        this.patchResourceService = new PatchResourceService();
    }

    /**
     * Binds the given AuthenticationProvider class such that any service
     * requiring access to the AuthenticationProvider can obtain it via
     * injection, along with any other bound AuthenticationProviders.
     *
     * @param authenticationProvider
     *     The AuthenticationProvider class to bind.
     */
    private void bindAuthenticationProvider(Class<? extends AuthenticationProvider> authenticationProvider) {

        // Bind authentication provider
        logger.debug("[{}] Binding AuthenticationProvider \"{}\".",
                boundAuthenticationProviders.size(), authenticationProvider.getName());
        boundAuthenticationProviders.add(new AuthenticationProviderFacade(authenticationProvider));

    }

    /**
     * Binds each of the the given AuthenticationProvider classes such that any
     * service requiring access to the AuthenticationProvider can obtain it via
     * injection.
     *
     * @param authProviders
     *     The AuthenticationProvider classes to bind.
     */
    private void bindAuthenticationProviders(Collection<Class<AuthenticationProvider>> authProviders) {

        // Bind each authentication provider within extension
        for (Class<AuthenticationProvider> authenticationProvider : authProviders)
            bindAuthenticationProvider(authenticationProvider);

    }

    /**
     * Returns a list of all currently-bound AuthenticationProvider instances.
     *
     * @return
     *     A List of all currently-bound AuthenticationProvider. The List is
     *     not modifiable.
     */
    @Provides
    public List<AuthenticationProvider> getAuthenticationProviders() {
        return Collections.unmodifiableList(boundAuthenticationProviders);
    }

    /**
     * Serves each of the given resources as a language resource. Language
     * resources are served from within the "/translations" directory as JSON
     * files, where the name of each JSON file is the language key.
     *
     * @param resources
     *     A map of all language resources to serve, where the key of each
     *     entry in the language key from which the name of the JSON file will
     *     be derived.
     */
    private void serveLanguageResources(Map<String, Resource> resources) {

        // Add all resources to language resource service
        for (Map.Entry<String, Resource> translationResource : resources.entrySet()) {

            // Get path and resource from path/resource pair
            String path = translationResource.getKey();
            Resource resource = translationResource.getValue();

            // Derive key from path
            String languageKey = languageResourceService.getLanguageKey(path);
            if (languageKey == null) {
                logger.warn("Invalid language file name: \"{}\"", path);
                continue;
            }

            // Add language resource
            languageResourceService.addLanguageResource(languageKey, resource);

        }

    }

    /**
     * Serves each of the given resources under the given prefix. The path of
     * each resource relative to the prefix is the key of its entry within the
     * map.
     *
     * @param prefix
     *     The prefix under which each resource should be served.
     *
     * @param resources
     *     A map of all resources to serve, where the key of each entry in the
     *     map is the desired path of that resource relative to the prefix.
     */
    private void serveStaticResources(String prefix, Map<String, Resource> resources) {

        // Add all resources under given prefix
        for (Map.Entry<String, Resource> staticResource : resources.entrySet()) {

            // Get path and resource from path/resource pair
            String path = staticResource.getKey();
            Resource resource = staticResource.getValue();

            // Serve within namespace-derived path
            serve(prefix + path).with(new ResourceServlet(resource));

        }

    }

    /**
     * Returns whether the given version of Guacamole is compatible with this
     * version of Guacamole as far as extensions are concerned.
     *
     * @param guacamoleVersion
     *     The version of Guacamole the extension was built for.
     *
     * @return
     *     true if the given version of Guacamole is compatible with this
     *     version of Guacamole, false otherwise.
     */
    private boolean isCompatible(String guacamoleVersion) {
        return ALLOWED_GUACAMOLE_VERSIONS.contains(guacamoleVersion);
    }

    /**
     * Loads all extensions within the GUACAMOLE_HOME/extensions directory, if
     * any, adding their static resource to the given resoure collections.
     *
     * @param javaScriptResources
     *     A modifiable collection of static JavaScript resources which may
     *     receive new JavaScript resources from extensions.
     *
     * @param cssResources
     *     A modifiable collection of static CSS resources which may receive
     *     new CSS resources from extensions.
     */
    private void loadExtensions(Collection<Resource> javaScriptResources,
            Collection<Resource> cssResources) {

        // Retrieve and validate extensions directory
        File extensionsDir = new File(environment.getGuacamoleHome(), EXTENSIONS_DIRECTORY);
        if (!extensionsDir.isDirectory())
            return;

        // Retrieve list of all extension files within extensions directory
        File[] extensionFiles = extensionsDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.isFile() && file.getName().endsWith(EXTENSION_SUFFIX);
            }

        });

        // Verify contents are accessible
        if (extensionFiles == null) {
            logger.warn("Although GUACAMOLE_HOME/" + EXTENSIONS_DIRECTORY + " exists, its contents cannot be read.");
            return;
        }

        // Sort files lexicographically
        Arrays.sort(extensionFiles);

        // Load each extension within the extension directory
        for (File extensionFile : extensionFiles) {

            logger.debug("Loading extension: \"{}\"", extensionFile.getName());

            try {

                // Load extension from file
                Extension extension = new Extension(getParentClassLoader(), extensionFile);

                // Validate Guacamole version of extension
                if (!isCompatible(extension.getGuacamoleVersion())) {
                    logger.debug("Declared Guacamole version \"{}\" of extension \"{}\" is not compatible with this version of Guacamole.",
                            extension.getGuacamoleVersion(), extensionFile.getName());
                    throw new GuacamoleServerException("Extension \"" + extension.getName() + "\" is not "
                            + "compatible with this version of Guacamole.");
                }

                // Add any JavaScript / CSS resources
                javaScriptResources.addAll(extension.getJavaScriptResources().values());
                cssResources.addAll(extension.getCSSResources().values());

                // Attempt to load all authentication providers
                bindAuthenticationProviders(extension.getAuthenticationProviderClasses());

                // Add any translation resources
                serveLanguageResources(extension.getTranslationResources());

                // Add all HTML patch resources
                patchResourceService.addPatchResources(extension.getHTMLResources().values());

                // Add all static resources under namespace-derived prefix
                String staticResourcePrefix = "/app/ext/" + extension.getNamespace() + "/";
                serveStaticResources(staticResourcePrefix, extension.getStaticResources());

                // Serve up the small favicon if provided
                if(extension.getSmallIcon() != null)
                    serve("/images/logo-64.png").with(new ResourceServlet(extension.getSmallIcon()));

                // Serve up the large favicon if provided
                if(extension.getLargeIcon()!= null)
                    serve("/images/logo-144.png").with(new ResourceServlet(extension.getLargeIcon()));

                // Log successful loading of extension by name
                logger.info("Extension \"{}\" loaded.", extension.getName());

            }
            catch (GuacamoleException e) {
                logger.error("Extension \"{}\" could not be loaded: {}", extensionFile.getName(), e.getMessage());
                logger.debug("Unable to load extension.", e);
            }

        }

    }
    
    @Override
    protected void configureServlets() {

        // Bind resource services
        bind(LanguageResourceService.class).toInstance(languageResourceService);
        bind(PatchResourceService.class).toInstance(patchResourceService);

        // Load initial language resources from servlet context
        languageResourceService.addLanguageResources(getServletContext());

        // Init JavaScript resources with base guacamole.min.js
        Collection<Resource> javaScriptResources = new ArrayList<Resource>();
        javaScriptResources.add(new WebApplicationResource(getServletContext(), "/guacamole.min.js"));

        // Init CSS resources with base guacamole.min.css
        Collection<Resource> cssResources = new ArrayList<Resource>();
        cssResources.add(new WebApplicationResource(getServletContext(), "/guacamole.min.css"));

        // Load all extensions
        loadExtensions(javaScriptResources, cssResources);

        // Always bind default file-driven auth last
        bindAuthenticationProvider(FileAuthenticationProvider.class);

        // Dynamically generate app.js and app.css from extensions
        serve("/app.js").with(new ResourceServlet(new SequenceResource(javaScriptResources)));
        serve("/app.css").with(new ResourceServlet(new SequenceResource(cssResources)));

        // Dynamically serve all language resources
        for (Map.Entry<String, Resource> entry : languageResourceService.getLanguageResources().entrySet()) {

            // Get language key/resource pair
            String languageKey = entry.getKey();
            Resource resource = entry.getValue();

            // Serve resource within /translations
            serve("/translations/" + languageKey + ".json").with(new ResourceServlet(resource));
            
        }
        
    }

}
