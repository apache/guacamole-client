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
import org.glyptodon.guacamole.environment.Environment;
import org.glyptodon.guacamole.net.basic.resource.ResourceServlet;
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
        File[] extensions = extensionsDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.isFile() && file.getName().endsWith(EXTENSION_SUFFIX);
            }

        });

        // Load each extension
        for (File extension : extensions) {
            // TODO: Actually load extension
            logger.info("Loading extension: \"{}\"", extension.getName());
        }
        
        // TODO: Pull these from extensions, dynamically concatenated
        serve("/app.js").with(new ResourceServlet(new WebApplicationResource(getServletContext(), "/guacamole.min.js")));
        serve("/app.css").with(new ResourceServlet(new WebApplicationResource(getServletContext(), "/guacamole.min.css")));

    }

}
