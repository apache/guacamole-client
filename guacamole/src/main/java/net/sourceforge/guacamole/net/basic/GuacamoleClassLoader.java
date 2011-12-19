
package net.sourceforge.guacamole.net.basic;

/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is guacamole-common.
 *
 * The Initial Developer of the Original Code is
 * Michael Jumper.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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
            instance = new GuacamoleClassLoader(
                GuacamoleProperties.getProperty(BasicGuacamoleProperties.LIB_DIRECTORY)
            );
        }
        
        catch (GuacamoleException e) {
            // On error, record exception
            exception = e;
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

        // If no classloader, use super
        if (classLoader == null)
            return super.findClass(name);
        
        // Otherwise, delegate
        return classLoader.loadClass(name);

    }

}
