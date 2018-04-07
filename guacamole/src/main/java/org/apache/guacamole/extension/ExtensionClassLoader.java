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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;

/**
 * ClassLoader implementation which prioritizes the classes defined within a
 * given extension .jar file. Unlike the standard URLClassLoader, classes
 * within the parent ClassLoader are only used if they are not defined within
 * the given .jar. If classes are defined in both the parent and the extension
 * .jar, the versions defined within the extension .jar are used.
 */
public class ExtensionClassLoader extends URLClassLoader {

    /**
     * The ClassLoader to use if class resolution through the extension .jar
     * fails.
     */
    private final ClassLoader parent;

    /**
     * Returns an instance of ExtensionClassLoader configured to load classes
     * from the given extension .jar. If a necessary class cannot be found
     * within the .jar, the given parent ClassLoader is used. Calling this
     * function multiple times will not affect previously-returned instances of
     * ExtensionClassLoader.
     *
     * @param extension
     *     The extension .jar file from which classes should be loaded.
     *
     * @param parent
     *     The ClassLoader to use if class resolution through the extension
     *     .jar fails.
     *
     * @return
     *     A ExtensionClassLoader instance which loads classes from the
     *     given extension .jar file.
     *
     * @throws GuacamoleException
     *     If the given file is not actually a file, or the contents of the
     *     file cannot be read.
     */
    public static ExtensionClassLoader getInstance(final File extension,
            final ClassLoader parent) throws GuacamoleException {

        try {
            // Attempt to create classloader which loads classes from the given
            // .jar file
            return AccessController.doPrivileged(new PrivilegedExceptionAction<ExtensionClassLoader>() {

                @Override
                public ExtensionClassLoader run() throws GuacamoleException {
                    return new ExtensionClassLoader(extension, parent);
                }

            });
        }

        catch (PrivilegedActionException e) {
            throw (GuacamoleException) e.getException();
        }

    }

    /**
     * Returns a URL which points to the given extension .jar file.
     *
     * @param extension
     *     The extension .jar file to generate a URL for.
     *
     * @return
     *     A URL which points to the given extension .jar.
     *
     * @throws GuacamoleException
     *     If the given file is not actually a file, or the contents of the
     *     file cannot be read.
     */
    private static URL getExtensionURL(File extension)
            throws GuacamoleException {

        // Validate extension file is indeed a file
        if (!extension.isFile())
            throw new GuacamoleException(extension + " is not a file.");

        try {
            return extension.toURI().toURL();
        }
        catch (MalformedURLException e) {
            throw new GuacamoleServerException(e);
        }

    }

    /**
     * Creates a new ExtensionClassLoader configured to load classes from the
     * given extension .jar. If a necessary class cannot be found within the
     * .jar, the given parent ClassLoader is used. Calling this function
     * multiple times will not affect previously-returned instances of
     * ExtensionClassLoader.
     *
     * @param extension
     *     The extension .jar file from which classes should be loaded.
     *
     * @param parent
     *     The ClassLoader to use if class resolution through the extension
     *     .jar fails.
     *
     * @throws GuacamoleException
     *     If the given file is not actually a file, or the contents of the
     *     file cannot be read.
     */
    private ExtensionClassLoader(File extension, ClassLoader parent)
            throws GuacamoleException {
        super(new URL[]{ getExtensionURL(extension) }, null);
        this.parent = parent;
    }

    @Override
    protected Class<?> findClass(String string) throws ClassNotFoundException {

        // Search only within the given URLs
        try {
            return super.findClass(string);
        }

        // Search parent classloader ONLY if not found within given URLs
        catch (ClassNotFoundException e) {
            return parent.loadClass(string);
        }

    }

}
