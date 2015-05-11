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

import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.environment.Environment;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
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
     * Creates a module which loads all extensions within the
     * GUACAMOLE_HOME/extensions directory.
     *
     * @param environment
     *     The environment to use when configuring authentication.
     */
    public ExtensionModule(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected void configureServlets() {

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

        // Init JavaScript resources with base guacamole.min.js
        Collection<Resource> javaScriptResources = new ArrayList<Resource>();
        javaScriptResources.add(new WebApplicationResource(getServletContext(), "/guacamole.min.js"));

        // Init CSS resources with base guacamole.min.css
        Collection<Resource> cssResources = new ArrayList<Resource>();
        cssResources.add(new WebApplicationResource(getServletContext(), "/guacamole.min.css"));

        // Load each extension within the extension directory
        for (File extensionFile : extensionFiles) {

            logger.debug("Loading extension: \"{}\"", extensionFile.getName());

            try {

                // FIXME: Use class loader which reads from the lib directory
                // Load extension from file
                Extension extension = new Extension(ExtensionModule.class.getClassLoader(), extensionFile);

                // Add any JavaScript / CSS resources
                javaScriptResources.addAll(extension.getJavaScriptResources());
                cssResources.addAll(extension.getCSSResources());

                // Load all authentication providers as singletons
                Collection<Class<AuthenticationProvider>> authenticationProviders = extension.getAuthenticationProviderClasses();
                for (Class<AuthenticationProvider> authenticationProvider : authenticationProviders) {
                    logger.debug("Binding AuthenticationProvider \"{}\".", authenticationProvider);
                    bind(AuthenticationProvider.class).to(authenticationProvider).in(Singleton.class);
                }

                // Log successful loading of extension by name
                logger.info("Extension \"{}\" loaded.", extension.getName());

            }
            catch (GuacamoleException e) {
                logger.error("Extension \"{}\" could not be loaded: {}", extensionFile.getName(), e.getMessage());
                logger.debug("Unable to load extension.", e);
            }

        }

        // Dynamically generate app.js and app.css from extensions
        serve("/app.js").with(new ResourceServlet(new SequenceResource(javaScriptResources)));
        serve("/app.css").with(new ResourceServlet(new SequenceResource(cssResources)));

    }

}
