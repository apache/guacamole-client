
package net.sourceforge.guacamole.net.basic;

/*
 *  Guacamole - Clientless Remote Desktop
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.basic.properties.BasicGuacamoleProperties;
import net.sourceforge.guacamole.properties.GuacamoleProperties;

/**
 * A ClassLoader implementation which finds classes within a configurable
 * directory. This directory is set within guacamole.properties.
 *
 * @author Michael Jumper
 */
public class GuacamoleClassLoader extends ClassLoader {

    private URLClassLoader classLoader = null;

    private static GuacamoleException exception = null;
    private static GuacamoleClassLoader instance = null;

    static {

        try {
            // Attempt to create singleton classloader which loads classes from
            // all .jar's in the lib directory defined in guacamole.properties
            instance = AccessController.doPrivileged(new PrivilegedExceptionAction<GuacamoleClassLoader>() {

                @Override
                public GuacamoleClassLoader run() throws GuacamoleException {
                    return new GuacamoleClassLoader(
                        GuacamoleProperties.getProperty(BasicGuacamoleProperties.LIB_DIRECTORY)
                    );
                }

            });
        }

        catch (PrivilegedActionException e) {
            // On error, record exception
            exception = (GuacamoleException) e.getException();
        }

    }

    private GuacamoleClassLoader(File libDirectory) throws GuacamoleException {

        // If no directory provided, just direct requests to parent classloader
        if (libDirectory == null)
            return;

        // Validate directory is indeed a directory
        if (!libDirectory.isDirectory())
            throw new GuacamoleException(libDirectory + " is not a directory.");

        // Get list of URLs for all .jar's in the lib directory
        Collection<URL> jarURLs = new ArrayList<URL>();
        for (File file : libDirectory.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {

                // If it ends with .jar, accept the file
                return name.endsWith(".jar");

            }

        })) {

            try {

                // Add URL for the .jar to the jar URL list
                jarURLs.add(file.toURI().toURL());

            }
            catch (MalformedURLException e) {
                throw new GuacamoleException(e);
            }

        }

        // Set delegate classloader to new URLClassLoader which loads from the
        // .jars found above.

        URL[] urls = new URL[jarURLs.size()];
        classLoader = new URLClassLoader(
            jarURLs.toArray(urls),
            getClass().getClassLoader()
        );

    }

    /**
     * Returns an instance of a GuacamoleClassLoader which finds classes
     * within the directory configured in guacamole.properties.
     *
     * @return An instance of a GuacamoleClassLoader.
     * @throws GuacamoleException If no instance could be returned due to an
     *                            error.
     */
    public static GuacamoleClassLoader getInstance() throws GuacamoleException {

        // If instance could not be created, rethrow original exception
        if (exception != null) throw exception;

        return instance;

    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {

        // If no classloader, use default loader
        if (classLoader == null)
            return Class.forName(name);

        // Otherwise, delegate
        return classLoader.loadClass(name);

    }

}
