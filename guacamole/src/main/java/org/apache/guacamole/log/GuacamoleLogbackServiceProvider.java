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
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * SLF4JServiceProvider that overrides the default behavior of Logback's
 * service provider, forcing the use of a custom MDCAdapter.
 */
public class GuacamoleLogbackServiceProvider implements SLF4JServiceProvider {

    /**
     * The highest version of SLF4J that this service provider can support.
     * This is intentionally not actually a released version, but a reasonable
     * upper bound. This value is identical to the value used by Logback.
     */
    private static final String SLF4J_API_MAX_VERSION = "2.0.99";

    /**
     * The name of the LoggerContext maintained by this service provider.
     */
    private static final String LOGGER_CONTEXT_NAME = "guacamole";

    /**
     * The singleton instance of the LoggerContext maintained by this service
     * provider. The LoggerContext is actually the LoggerFactory.
     */
    private LoggerContext context;

    /**
     * The singleton instance of the MDCAdapter implementation that should be
     * used by Logback and SLF4J. The custom implementation provided here
     * allows the context map to be inherited by child threads.
     */
    private final MDCAdapter mdcAdapter = new InheritableMDCAdapter();

    /**
     * The singleton MarkerFactory instance maintained by this service
     * provider.
     */
    private final IMarkerFactory markerFactory = new BasicMarkerFactory();

    @Override
    public void initialize() {

        context = new LoggerContext();
        context.setName(LOGGER_CONTEXT_NAME);
        context.setMDCAdapter(mdcAdapter);

        // Perform basic init (prior to loading any Guacamole-specific
        // configuration, which is loaded by the LogModule)
        try {
            new ContextInitializer(context).autoConfig();
        }
        catch (JoranException | RuntimeException | Error e) {
            System.err.println("Logging system could not be initialized: " + e.getMessage());
            e.printStackTrace(System.err);
        }

        context.start();

    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return mdcAdapter;
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return context;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return markerFactory;
    }

    @Override
    public String getRequestedApiVersion() {
        return SLF4J_API_MAX_VERSION;
    }

}
