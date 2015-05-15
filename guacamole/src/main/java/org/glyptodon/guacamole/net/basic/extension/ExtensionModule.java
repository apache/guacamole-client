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

import com.google.inject.servlet.ServletModule;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.sourceforge.guacamole.net.basic.BasicFileAuthenticationProvider;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleServerException;
import org.glyptodon.guacamole.environment.Environment;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
import org.glyptodon.guacamole.net.basic.properties.BasicGuacamoleProperties;
import org.glyptodon.guacamole.net.basic.resource.Resource;
import org.glyptodon.guacamole.net.basic.resource.ResourceServlet;
import org.glyptodon.guacamole.net.basic.resource.SequenceResource;
import org.glyptodon.guacamole.net.basic.resource.WebApplicationResource;
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
            "0.9.6"
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
     * The currently-bound authentication provider, if any. At the moment, we
     * only support one authentication provider loaded at any one time.
     */
    private Class<? extends AuthenticationProvider> boundAuthenticationProvider = null;

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
    }

    /**
     * Reads the value of the now-deprecated "auth-provider" property from
     * guacamole.properties, returning the corresponding AuthenticationProvider
     * class. If no authentication provider could be read, or the property is
     * not present, null is returned.
     *
     * As this property is deprecated, this function will also log warning
     * messages if the property is actually specified.
     *
     * @return
     *     The value of the deprecated "auth-provider" property, or null if the
     *     property is not present.
     */
    @SuppressWarnings("deprecation") // We must continue to use this property until it is truly no longer supported
    private Class<AuthenticationProvider> getAuthProviderProperty() {

        // Get and bind auth provider instance, if defined via property
        try {

            // Use "auth-provider" property if present, but warn about deprecation
            Class<AuthenticationProvider> authenticationProvider = environment.getProperty(BasicGuacamoleProperties.AUTH_PROVIDER);
            if (authenticationProvider != null)
                logger.warn("The \"auth-provider\" and \"lib-directory\" properties are now deprecated. Please use the \"extensions\" and \"lib\" directories within GUACAMOLE_HOME instead.");

            return authenticationProvider;

        }
        catch (GuacamoleException e) {
            logger.warn("Value of deprecated \"auth-provider\" property within guacamole.properties is not valid: {}", e.getMessage());
            logger.debug("Error reading authentication provider from guacamole.properties.", e);
        }

        return null;

    }

    /**
     * Binds the given AuthenticationProvider class such that any service
     * requiring access to the AuthenticationProvider can obtain it via
     * injection.
     *
     * @param authenticationProvider
     *     The AuthenticationProvider class to bind.
     */
    private void bindAuthenticationProvider(Class<? extends AuthenticationProvider> authenticationProvider) {

        // Choose auth provider for binding if not already chosen
        if (boundAuthenticationProvider == null)
            boundAuthenticationProvider = authenticationProvider;

        // If an auth provider is already chosen, skip and warn
        else {
            logger.debug("Ignoring AuthenticationProvider \"{}\".", authenticationProvider);
            logger.warn("Only one authentication extension may be used at a time. Please "
                      + "make sure that only one authentication extension is present "
                      + "within the GUACAMOLE_HOME/" + EXTENSIONS_DIRECTORY + " "
                      + "directory, and that you are not also specifying the deprecated "
                      + "\"auth-provider\" property within guacamole.properties.");
            return;
        }

        // Bind authentication provider
        logger.debug("Binding AuthenticationProvider \"{}\".", authenticationProvider);
        bind(AuthenticationProvider.class).toInstance(new AuthenticationProviderFacade(authenticationProvider));

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
                javaScriptResources.addAll(extension.getJavaScriptResources());
                cssResources.addAll(extension.getCSSResources());

                // Attempt to load all authentication providers
                Collection<Class<AuthenticationProvider>> authenticationProviders = extension.getAuthenticationProviderClasses();
                for (Class<AuthenticationProvider> authenticationProvider : authenticationProviders)
                    bindAuthenticationProvider(authenticationProvider);

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

        // Load authentication provider from guacamole.properties for sake of backwards compatibility
        Class<AuthenticationProvider> authProviderProperty = getAuthProviderProperty();
        if (authProviderProperty != null)
            bindAuthenticationProvider(authProviderProperty);

        // Init JavaScript resources with base guacamole.min.js
        Collection<Resource> javaScriptResources = new ArrayList<Resource>();
        javaScriptResources.add(new WebApplicationResource(getServletContext(), "/guacamole.min.js"));

        // Init CSS resources with base guacamole.min.css
        Collection<Resource> cssResources = new ArrayList<Resource>();
        cssResources.add(new WebApplicationResource(getServletContext(), "/guacamole.min.css"));

        // Load all extensions
        loadExtensions(javaScriptResources, cssResources);

        // Bind basic auth if nothing else chosen/provided
        if (boundAuthenticationProvider == null) {
            logger.info("Using default, \"basic\", XML-driven authentication.");
            bindAuthenticationProvider(BasicFileAuthenticationProvider.class);
        }

        // Dynamically generate app.js and app.css from extensions
        serve("/app.js").with(new ResourceServlet(new SequenceResource(javaScriptResources)));
        serve("/app.css").with(new ResourceServlet(new SequenceResource(cssResources)));

    }

}
