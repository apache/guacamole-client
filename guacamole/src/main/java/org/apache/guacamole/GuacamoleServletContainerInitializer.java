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

package org.apache.guacamole;

import ch.qos.logback.core.joran.spi.JoranException;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.apache.guacamole.log.GuacamoleLogbackServiceProvider;
import org.apache.guacamole.log.LogLevel;
import org.apache.guacamole.log.ReconfigurableLoggerFactory;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.Reporter;

/**
 * ServletContainerInitializer which forces the SLF4J service provider to be
 * Guacamole's built-in logging system. This is necessary as Logback also
 * includes an SLF4J service provider, and there is no guarantee which provider
 * will be chosen by SLF4J in the case that multiple are present.
 * <p>
 * SLF4J uses a "slf4j.provider" system property to allow a specific provider
 * to be chosen, which this initializer sets automatically.
 */
public class GuacamoleServletContainerInitializer implements ServletContainerInitializer {

    /**
     * The log level that should be used when we have not yet loaded any
     * configuration dictating otherwise.
     */
    private static final LogLevel EARLY_LOG_LEVEL = LogLevel.INFO;

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {

        // Ideally, we want to use Guacamole's built-in logging facility, but
        // we shouldn't override the administrator if they have their own SLF4J
        // provider that needs to be used instead (ie: for some log aggregation
        // service)
        if (System.getProperty(LoggerFactory.PROVIDER_PROPERTY_KEY) == null) {

            System.setProperty(LoggerFactory.PROVIDER_PROPERTY_KEY,
                    GuacamoleLogbackServiceProvider.class.getCanonicalName());

            // Avoid excessive verbosity from SLF4J unless the administrator
            // has explicitly chosen SLF4J's verbosity level (the SLF4J provider
            // class will otherwise be logged at startup, which is an internal
            // detail and not typically needed)
            if (System.getProperty(Reporter.SLF4J_INTERNAL_VERBOSITY_KEY) == null)
                System.setProperty(Reporter.SLF4J_INTERNAL_VERBOSITY_KEY, "WARN");

        }

        // Attempt early initialization of Logback (before we begin parsing
        // Guacamole's configuration), such that even startup messages follow
        // a consistent format
        try (InputStream logbackConfiguration = EARLY_LOG_LEVEL.getLogbackConfiguration()) {
            ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
            if (loggerFactory instanceof ReconfigurableLoggerFactory)
                ((ReconfigurableLoggerFactory) loggerFactory).reconfigure(logbackConfiguration);
        }
        catch (JoranException | IOException e) {
            System.err.println("Early initialization of Logback failed - "
                    + "logging format may not match until the web application "
                    + "finishes starting up: " + e.getMessage());
        }

    }

}
