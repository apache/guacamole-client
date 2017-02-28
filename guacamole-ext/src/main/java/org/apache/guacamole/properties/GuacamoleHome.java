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

package org.apache.guacamole.properties;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract representation of the Guacamole configuration directory.
 *
 * @deprecated
 */
public class GuacamoleHome {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(GuacamoleHome.class);

    static {
        // Warn about deprecation
        logger.warn("GuacamoleHome is deprecated. Please use Environment instead.");
    }
    
    /**
     * GuacamoleHome is a utility class and cannot be instantiated.
     */
    private GuacamoleHome() {}

    /**
     * Returns the Guacamole home directory by checking, in order:
     * the guacamole.home system property, the GUACAMOLE_HOME environment
     * variable, and finally the .guacamole directory in the home directory of
     * the user running the servlet container.
     *
     * @return The File representing the Guacamole home directory, which may
     *         or may not exist, and may turn out to not be a directory.
     */
    public static File getDirectory() {

        // Attempt to find Guacamole home
        File guacHome;

        // Use system property by default
        String desiredDir = System.getProperty("guacamole.home");

        // Failing that, try the GUACAMOLE_HOME environment variable
        if (desiredDir == null) desiredDir = System.getenv("GUACAMOLE_HOME");

        // If successful, use explicitly specified directory
        if (desiredDir != null)
            guacHome = new File(desiredDir);

        // If not explicitly specified, use ~/.guacamole
        else
            guacHome = new File(System.getProperty("user.home"), ".guacamole");

        // Return discovered directory
        return guacHome;

    }

}
