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

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

/**
 * {@link Logger} that automatically includes arbitrary context. The additional
 * context is included as a {@link Marker}, and all logging functions are
 * overridden to include at least this Marker. If a logging function is given a
 * Marker by the caller, it is wrapped as a reference of Marker created by the
 * ContextAwareLogger's Marker.
 */
public class ContextAwareLogger implements Logger {

    /**
     * The wrapped Logger.
     */
    private final Logger logger;

    /**
     * The arbitrary context that should be included as a Marker in all log
     * messages.
     */
    private final String context;

    /**
     * Creates a new ContextAwareLogger that wraps given Logger, automatically
     * including the provided context as a Marker for each logged message.
     *
     * @param context
     *     The arbitrary context to include as a Marker with each
     *     logged message.
     *
     * @param logger
     *     The Logger to wrap.
     */
    public ContextAwareLogger(String context, Logger logger) {
        this.logger = logger;
        this.context = context;
    }

    /**
     * Returns a Marker containing the arbitrary context provided when this
     * ContextAwareLogger was created. If Markers are provided to this function,
     * they are included as references within the returned Marker.
     *
     * @param refs
     *     The Markers to include in the returned Marker as references, if any.
     *
     * @return
     *     A Marker containing the context provided when this
     *     ContextAwareLogger was created, as well as any provided Marker
     *     references.
     */
    private Marker getContextMarker(Marker... refs) {
        Marker marker = MarkerFactory.getMarker(context);
        Arrays.stream(refs).forEach(marker::add);
        return marker;
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public LoggingEventBuilder makeLoggingEventBuilder(Level level) {
        return logger.makeLoggingEventBuilder(level);
    }

    @Override
    public LoggingEventBuilder atLevel(Level level) {
        return logger.atLevel(level);
    }

    @Override
    public boolean isEnabledForLevel(Level level) {
        return logger.isEnabledForLevel(level);
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        logger.trace(getContextMarker(), msg);
    }

    @Override
    public void trace(String format, Object arg) {
        logger.trace(getContextMarker(), format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        logger.trace(getContextMarker(), format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        logger.trace(getContextMarker(), format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        logger.trace(getContextMarker(), msg, t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return logger.isTraceEnabled(marker);
    }

    @Override
    public LoggingEventBuilder atTrace() {
        return logger.atTrace();
    }

    @Override
    public void trace(Marker marker, String msg) {
        logger.trace(getContextMarker(marker), msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        logger.trace(getContextMarker(marker), format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        logger.trace(getContextMarker(marker), format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        logger.trace(getContextMarker(marker), format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        logger.trace(getContextMarker(marker), msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        logger.debug(getContextMarker(), msg);
    }

    @Override
    public void debug(String format, Object arg) {
        logger.debug(getContextMarker(), format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        logger.debug(getContextMarker(), format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        logger.debug(getContextMarker(), format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        logger.debug(getContextMarker(), msg, t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return logger.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg) {
        logger.debug(getContextMarker(marker), msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        logger.debug(getContextMarker(marker), format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        logger.debug(getContextMarker(marker), format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        logger.debug(getContextMarker(marker), format, arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        logger.debug(getContextMarker(marker), msg, t);
    }

    @Override
    public LoggingEventBuilder atDebug() {
        return logger.atDebug();
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        logger.info(getContextMarker(), msg);
    }

    @Override
    public void info(String format, Object arg) {
        logger.info(getContextMarker(), format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        logger.info(getContextMarker(), format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        logger.info(getContextMarker(), format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        logger.info(getContextMarker(), msg, t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return logger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        logger.info(getContextMarker(marker), msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        logger.info(getContextMarker(marker), format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        logger.info(getContextMarker(marker), format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        logger.info(getContextMarker(marker), format, arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        logger.info(getContextMarker(marker), msg, t);
    }

    @Override
    public LoggingEventBuilder atInfo() {
        return logger.atInfo();
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        logger.warn(getContextMarker(), msg);
    }

    @Override
    public void warn(String format, Object arg) {
        logger.warn(getContextMarker(), format, arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        logger.warn(getContextMarker(), format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        logger.warn(getContextMarker(), format, arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        logger.warn(getContextMarker(), msg, t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return logger.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        logger.warn(getContextMarker(marker), msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        logger.warn(getContextMarker(marker), format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        logger.warn(getContextMarker(marker), format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        logger.warn(getContextMarker(marker), format, arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        logger.warn(getContextMarker(marker), msg, t);
    }

    @Override
    public LoggingEventBuilder atWarn() {
        return logger.atWarn();
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        logger.error(getContextMarker(), msg);
    }

    @Override
    public void error(String format, Object arg) {
        logger.error(getContextMarker(), format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        logger.error(getContextMarker(), format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        logger.error(getContextMarker(), format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        logger.error(getContextMarker(), msg, t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return logger.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        logger.error(getContextMarker(marker), msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        logger.error(getContextMarker(marker), format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        logger.error(getContextMarker(marker), format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        logger.error(getContextMarker(marker), format, arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        logger.error(getContextMarker(marker), msg, t);
    }

    @Override
    public LoggingEventBuilder atError() {
        return logger.atError();
    }

}
