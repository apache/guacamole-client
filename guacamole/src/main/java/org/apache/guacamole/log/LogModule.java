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
 *
 * @author Michael Jumper
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
