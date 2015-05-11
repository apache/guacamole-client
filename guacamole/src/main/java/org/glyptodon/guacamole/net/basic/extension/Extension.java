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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleServerException;
import org.glyptodon.guacamole.net.basic.resource.ClassPathResource;
import org.glyptodon.guacamole.net.basic.resource.Resource;

/**
 * A Guacamole extension, which may provide custom authentication, static
 * files, theming/branding, etc.
 *
 * @author Michael Jumper
 */
public class Extension {

    /**
     * The Jackson parser for parsing the language JSON files.
     */
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * The name of the manifest file that describes the contents of a
     * Guacamole extension.
     */
    private static final String MANIFEST_NAME = "guac-manifest.json";

    /**
     * The parsed manifest file of this extension, describing the location of
     * resources within the extension.
     */
    private final ExtensionManifest manifest;

    /**
     * The classloader to use when reading resources from this extension,
     * including classes and static files.
     */
    private final ClassLoader classLoader;

    /**
     * Loads the given file as an extension, which must be a .jar containing
     * a guac-manifest.json file describing its contents.
     *
     * @param parent
     *     The classloader to use as the parent for the isolated classloader of
     *     this extension.
     *
     * @param file
     *     The file to load as an extension.
     *
     * @throws GuacamoleException
     *     If the provided file is not a .jar file, does not contain the
     *     guac-manifest.json, or if guac-manifest.json is invalid and cannot
     *     be parsed.
     */
    public Extension(final ClassLoader parent, final File file) throws GuacamoleException {

        try {

            // Open extension
            ZipFile extension = new ZipFile(file);

            try {

                // Retrieve extension manifest
                ZipEntry manifestEntry = extension.getEntry(MANIFEST_NAME);
                if (manifestEntry == null)
                    throw new GuacamoleServerException("Extension " + file.getName() + " is missing " + MANIFEST_NAME);

                // Parse manifest
                manifest = mapper.readValue(extension.getInputStream(manifestEntry), ExtensionManifest.class);

            }

            // Always close zip file, if possible
            finally {
                extension.close();
            }

            try {

                // Create isolated classloader for this extension
                classLoader = AccessController.doPrivileged(new PrivilegedExceptionAction<ClassLoader>() {

                    @Override
                    public ClassLoader run() throws GuacamoleException {

                        try {

                            // Classloader must contain only the extension itself
                            return new URLClassLoader(new URL[]{file.toURI().toURL()}, parent);

                        }
                        catch (MalformedURLException e) {
                            throw new GuacamoleException(e);
                        }

                    }

                });

            }

            // Rethrow any GuacamoleException
            catch (PrivilegedActionException e) {
                throw (GuacamoleException) e.getException();
            }

        }

        // Abort load if not a valid zip file
        catch (ZipException e) {
            throw new GuacamoleServerException("Extension is not a valid zip file: " + file.getName(), e);
        }

        // Abort if manifest cannot be parsed (invalid JSON)
        catch (JsonParseException e) {
            throw new GuacamoleServerException(MANIFEST_NAME + " is not valid JSON: " + file.getName(), e);
        }

        // Abort if zip file cannot be read at all due to I/O errors
        catch (IOException e) {
            throw new GuacamoleServerException("Unable to read extension: " + file.getName(), e);
        }

    }

    /**
     * Returns the name of this extension, as declared in the extension's
     * manifest.
     *
     * @return
     *     The name of this extension.
     */
    public String getName() {
        return manifest.getName();
    }

    /**
     * Returns the namespace of this extension, as declared in the extension's
     * manifest.
     *
     * @return
     *     The namespace of this extension.
     */
    public String getNamespace() {
        return manifest.getNamespace();
    }

    /**
     * Returns a new collection of resources corresponding to the collection of
     * paths provided. Each resource will be associated with the given
     * mimetype.
     *
     * @param mimetype
     *     The mimetype to associate with each resource.
     *
     * @param paths
     *     The paths corresponding to the resources desired.
     *
     * @return
     *     A new, unmodifiable collection of resources corresponding to the
     *     collection of paths provided.
     */
    private Collection<Resource> getClassPathResources(String mimetype, Collection<String> paths) {

        // If no paths are provided, just return an empty list
        if (paths == null)
            return Collections.<Resource>emptyList();

        // Add classpath resource for each path provided
        Collection<Resource> resources = new ArrayList<Resource>(paths.size());
        for (String path : paths)
            resources.add(new ClassPathResource(classLoader, mimetype, path));

        // Callers should not rely on modifying the result
        return Collections.unmodifiableCollection(resources);

    }

    /**
     * Returns all declared JavaScript resources associated with this
     * extension. JavaScript resources are declared within the extension
     * manifest.
     *
     * @return
     *     All declared JavaScript resources associated with this extension.
     */
    public Collection<Resource> getJavaScriptResources() {
        return getClassPathResources("text/javascript", manifest.getJavaScriptPaths());
    }

    /**
     * Returns all declared CSS resources associated with this extension. CSS
     * resources are declared within the extension manifest.
     *
     * @return
     *     All declared CSS resources associated with this extension.
     */
    public Collection<Resource> getCSSResources() {
        return getClassPathResources("text/css", manifest.getCSSPaths());
    }

}
