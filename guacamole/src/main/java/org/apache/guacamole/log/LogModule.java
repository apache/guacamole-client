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

package org.apache.guacamole.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.google.inject.AbstractModule;
import java.io.File;
import org.apache.guacamole.environment.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes the logging subsystem.
 */
public class LogModule extends AbstractModule {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(LogModule.class);

    /**
     * The Guacamole server environment.
     */
    private final Environment environment;

    /**
     * Creates a new LogModule which uses the given environment to determine
     * the logging configuration.
     *
     * @param environment
     *     The environment to use when configuring logging.
     */
    public LogModule(Environment environment) {
        this.environment = environment;
    }
    
    @Override
    protected void configure() {

        // Only load logback configuration if GUACAMOLE_HOME exists
        File guacamoleHome = environment.getGuacamoleHome();
        if (!guacamoleHome.isDirectory())
            return;

        // Check for custom logback.xml
        File logbackConfiguration = new File(guacamoleHome, "logback.xml");
        if (!logbackConfiguration.exists())
            return;

        logger.info("Loading logback configuration from \"{}\".", logbackConfiguration);

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();

        try {

            // Initialize logback
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            configurator.doConfigure(logbackConfiguration);

            // Dump any errors that occur during logback init
            StatusPrinter.printInCaseOfErrorsOrWarnings(context);

        }
        catch (JoranException e) {
            logger.error("Initialization of logback failed: {}", e.getMessage());
            logger.debug("Unable to load logback configuration..", e);
        }

    }

}
