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

package org.apache.guacamole.net.basic.extension;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.guacamole.GuacamoleException;

/**
 * A ClassLoader implementation which finds classes within .jar files within a
 * given directory.
 *
 * @author Michael Jumper
 */
public class DirectoryClassLoader extends URLClassLoader {

    /**
     * Returns an instance of DirectoryClassLoader configured to load .jar
     * files from the given directory. Calling this function multiple times
     * will not affect previously-returned instances of DirectoryClassLoader.
     *
     * @param dir
     *     The directory from which .jar files should be read.
     *
     * @return
     *     A DirectoryClassLoader instance which loads classes from the .jar
     *     files in the given directory.
     *
     * @throws GuacamoleException
     *     If the given file is not a directory, or the contents of the given
     *     directory cannot be read.
     */
    public static DirectoryClassLoader getInstance(final File dir)
            throws GuacamoleException {

        try {
            // Attempt to create singleton classloader which loads classes from
            // all .jar's in the lib directory defined in guacamole.properties
            return AccessController.doPrivileged(new PrivilegedExceptionAction<DirectoryClassLoader>() {

                @Override
                public DirectoryClassLoader run() throws GuacamoleException {
                    return new DirectoryClassLoader(dir);
                }

            });
        }

        catch (PrivilegedActionException e) {
            throw (GuacamoleException) e.getException();
        }

    }

    /**
     * Returns all .jar files within the given directory as an array of URLs.
     *
     * @param dir
     *     The directory to retrieve all .jar files from.
     *
     * @return
     *     An array of the URLs of all .jar files within the given directory.
     *
     * @throws GuacamoleException
     *     If the given file is not a directory, or the contents of the given
     *     directory cannot be read.
     */
    private static URL[] getJarURLs(File dir) throws GuacamoleException {

        // Validate directory is indeed a directory
        if (!dir.isDirectory())
            throw new GuacamoleException(dir + " is not a directory.");

        // Get list of URLs for all .jar's in the lib directory
        Collection<URL> jarURLs = new ArrayList<URL>();
        File[] files = dir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {

                // If it ends with .jar, accept the file
                return name.endsWith(".jar");

            }

        });

        // Verify directory was successfully read
        if (files == null)
            throw new GuacamoleException("Unable to read contents of directory " + dir);

        // Add the URL for each .jar to the jar URL list
        for (File file : files) {

            try {
                jarURLs.add(file.toURI().toURL());
            }
            catch (MalformedURLException e) {
                throw new GuacamoleException(e);
            }

        }

        // Set delegate classloader to new URLClassLoader which loads from the .jars found above.
        URL[] urls = new URL[jarURLs.size()];
        return jarURLs.toArray(urls);

    }

    /**
     * Creates a new DirectoryClassLoader configured to load .jar files from
     * the given directory.
     *
     * @param dir
     *     The directory from which .jar files should be read.
     *
     * @throws GuacamoleException
     *     If the given file is not a directory, or the contents of the given
     *     directory cannot be read.
     */

    private DirectoryClassLoader(File dir) throws GuacamoleException {
        super(getJarURLs(dir), DirectoryClassLoader.class.getClassLoader());
    }

}
