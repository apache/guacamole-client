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
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.guacamole.extension.LoggerContextProvider;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.spi.MDCAdapter;

/**
 * {@link ILoggerFactory} implementation that automatically includes additional
 * {@link Marker} context based on the origin of each log message. The origin
 * of each message is determined by investigating the caller classes, with
 * the actual context provided by the associated classloader if it implements
 * {@link LoggerContextProvider}.
 */
public class ContextAwareLoggerFactory implements ReconfigurableLoggerFactory {

    /**
     * The name of the LoggerContext maintained by this logger factory.
     */
    private static final String LOGGER_CONTEXT_NAME = "guacamole";

    /**
     * The default text that should be displayed if no other context can be
     * derived from any caller.
     */
    private static final String DEFAULT_CONTEXT = "app";

    /**
     * Regular expression that matches one or more newlines, regardless of style
     * (Windows, Linux, etc.), preserving only the first such newline. This
     * expression is intended to reliably match newlines that may be interpreted
     * by the shell/viewer, regardless of the platform producing the log
     * message.
     * <p>
     * This specific regular expression is intended for use with content that is
     * being included inline (concatenated with an existing line of output).
     */
    private static final String INLINE_NEWLINE_REGEX =

              // Locate the right edge of the first newline sequence, whether
              // that is CR, LF, CRLF, or LFCR
              "(?<="

                // Do not match newline sequences that are not the first
                // newline in the sequence
                + "(?<![\\n\\r])"

                // Match either CR, LF, CRLF, or LFCR, ensuring that CRLF
                // does not get matched as just CR (same for LFCR vs. LF)
                + "("
                    +  "\\n(?!\\r)" // LF (with no following CR)
                    + "|\\r(?!\\n)" // CR (with no following LF)
                    + "|\\n\\r"     // LFCR
                    + "|\\r\\n"     // CRLF
                + ")"

            + ")"

               // Ignore (and strip) and remaining newline characters
            + "[\\n\\r]*";

    /**
     * Identical in purpose and behavior to {@link #INLINE_NEWLINE_REGEX}, but
     * intended for output that stands on its own as a block of text. This
     * variation preserves formatting of a block by matching the beginning of
     * all lines (regardless of whether preceded by a newline) and not matching
     * the final newline.
     */
    private static final String BLOCK_NEWLINE_REGEX = "(^|" + INLINE_NEWLINE_REGEX + ")(?=[^\\r\\n])";

    /**
     * Logback encoder pattern fragment that produces a local timestamp (with
     * timezone information) in roughly ISO 8601 format.
     */
    private static final String LOG_PATTERN_FRAGMENT_TIMESTAMP = "%d{yyyy-MM-dd HH:mm:ss.SSS xxx}";

    /**
     * The text that should be prepended to log messages when they span multiple
     * lines.
     */
    private static final String CONTINUATION_PREFIX = "+ ";

    /**
     * Logback encoder pattern fragment that produces the logged message. This
     * automatically prepends any newlines within the message with a "+ " prefix
     * to clearly represent when a message has been split across multiple lines.
     */
    private static final String LOG_PATTERN_FRAGMENT_MESSAGE_BODY = "%replace(%msg){'" + INLINE_NEWLINE_REGEX + "','" + CONTINUATION_PREFIX + "'}";

    /**
     * Logback encoder pattern that should be used for messages when the highest
     * level of verbosity is not needed.
     */
    private static final String LOG_PATTERN_DEFAULT = LOG_PATTERN_FRAGMENT_TIMESTAMP
            + " guacamole [%marker] %level: "
            + LOG_PATTERN_FRAGMENT_MESSAGE_BODY + "%n"
            + "%nopex";

    /**
     * Logback encoder pattern that should be used for messages with the highest
     * level of verbosity.
     */
    private static final String LOG_PATTERN_VERBOSE = LOG_PATTERN_FRAGMENT_TIMESTAMP
            + " guacamole [%marker, thread:%thread] %level: %logger{36} - "
            + LOG_PATTERN_FRAGMENT_MESSAGE_BODY + "%n"
            + "%replace(%xThrowable){'" + BLOCK_NEWLINE_REGEX + "','" + CONTINUATION_PREFIX + "'}";

    /**
     * The top-level Logback object, the LoggerContext, which is also Logback's
     * LoggerFactory implementation.
     */
    private final LoggerContext context;

    /**
     * Creates a new ContextAwareLoggerFactory that uses the given MDCAdapter
     * for the {@link MDC} implementation.
     *
     * @param mdcAdapter
     *     The MDCAdapter that should be used to provide the MDC
     *     implementation.
     */
    public ContextAwareLoggerFactory(MDCAdapter mdcAdapter) {

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

    /**
     * Uses the {@link StackWalker} class provided by Java9+ to retrieve all
     * classes currently on the call stack for the current function, in order of
     * decreasing depth. The current function (the deepest function in terms of
     * call depth) is the first item in the returned list.
     * <p>
     * NOTE: This method is only supported by Java 9 and late and will throw a
     * ClassNotFoundException otherwise..
     *
     * @return
     *     A List of all classes currently on the call stack, in order of
     *     decreasing depth.
     *
     * @throws UnsupportedOperationException
     *     If support for StackWalker is not present (Java 8).
     */
    @SuppressWarnings("unchecked")
    private List<Class<?>> getCallersFromStackWalker()
            throws UnsupportedOperationException {

        //
        // NOTE: The following is the reflection equivalent of:
        //
        // return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk((stack) ->
        //    stack.map((frame) -> frame.getDeclaringClass()).collect(Collectors.toList())
        // );
        //

        try {

            // Retrieve StackWalker definition and related classes
            Class<?> stackWalkerClass = Class.forName("java.lang.StackWalker");
            Class<?> stackFrameClass = Class.forName("java.lang.StackWalker$StackFrame");
            Class<Enum> optionEnum = (Class<Enum>) Class.forName("java.lang.StackWalker$Option");

            // Retrieve references to functions we will be needing
            Method stackWalkerGetInstanceMethod = stackWalkerClass.getMethod("getInstance", optionEnum);
            Method walkMethod = stackWalkerClass.getMethod("walk", Function.class);
            Method getDeclaringClassMethod = stackFrameClass.getMethod("getDeclaringClass");

            // Equivalent to:
            // StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
            Object stackWalker = stackWalkerGetInstanceMethod.invoke(null,
                    Enum.valueOf(optionEnum, "RETAIN_CLASS_REFERENCE"));

            // Equivalent to:
            // (stack) -> stack.map((frame) -> frame.getDeclaringClass()).collect(Collectors.toList())
            Function<Stream<?>, List<Class<?>>> walker =
                    (stack) -> stack.map((frame) -> {
                        try {
                            return (Class<?>) getDeclaringClassMethod.invoke(frame);
                        }
                        catch (ReflectiveOperationException e) {
                            throw new UnsupportedOperationException("getDeclaringClass() "
                                    + "could not be invoked for stack frame ", e);
                        }
                    }).collect(Collectors.toList());

            // Equivalent to: stackWalker.walk(...)
            return (List<Class<?>>) walkMethod.invoke(stackWalker, walker);

        }
        catch (ReflectiveOperationException e) {
            throw new UnsupportedOperationException("StackWalker is not "
                    + "supported by this version of Java", e);
        }

    }

    /**
     * Uses the deprecated {@link SecurityManager} to retrieve all classes
     * currently on the call stack for the current function, in order of
     * decreasing depth. The current function (the deepest function in terms of
     * call depth) is the first item in the returned list.
     * <p>
     * NOTE: This is the only method that is supported by Java 8 that includes
     * direct Class references.
     *
     * @return
     *     A List of all classes currently on the call stack, in order of
     *     decreasing depth.
     *
     * @throws UnsupportedOperationException
     *     If support for SecurityManager is not present (expected for future
     *     Java versions).
     */
    @SuppressWarnings("removal")
    private List<Class<?>> getCallersFromSecurityManager() {
        try {

            // Atempt to locate SecurityManager class and grab its
            // getClassContext() method (which returns a stack trace)
            Class<?> securityManagerClass = Class.forName("java.lang.SecurityManager");
            Object securityManager = securityManagerClass.getDeclaredConstructor().newInstance();
            Method getClassContextMethod = securityManagerClass.getDeclaredMethod("getClassContext");

            // getClassContext() is protected
            getClassContextMethod.setAccessible(true);

            return Arrays.asList((Class<?>[]) getClassContextMethod.invoke(securityManager));

        }
        catch (ReflectiveOperationException e) {
            throw new UnsupportedOperationException("SecurityManager is not "
                    + "supported by this version of Java", e);
        }
    }

    /**
     * Returns all classes currently on the call stack for the current function,
     * in order of decreasing depth. The current function (the deepest function
     * in terms of call depth) is the first item in the returned list.
     *
     * @return
     *     A List of all classes currently on the call stack, in order of
     *     decreasing depth.
     *
     * @throws UnsupportedOperationException
     *     If no supported mechanism exists for retrieving the call stack.
     */
    private List<Class<?>> getCallers() throws UnsupportedOperationException {
        try {
            return getCallersFromStackWalker();
        }
        catch (UnsupportedOperationException e) {
            return getCallersFromSecurityManager();
        }
    }

    /**
     * Derives the context of the message currently being logged based on the
     * content of the call stack.
     *
     * @return
     *     The context of the message currently being logged.
     */
    private String getCallerLogContext() {

        // Derive context from the most recent caller that explicitly provides
        // their own logging context, if any
        for (Class<?> caller : getCallers()) {

            ClassLoader classloader = caller.getClassLoader();
            if (classloader instanceof LoggerContextProvider)
                return ((LoggerContextProvider) classloader).getLoggerContext();

        }

        // Use default context if no callers provide their own
        return DEFAULT_CONTEXT;

    }

    @Override
    public Logger getLogger(String name) {
        return new ContextAwareLogger(getCallerLogContext(), context.getLogger(name));
    }

    @Override
    public void reconfigure(InputStream logbackConfiguration) throws JoranException {

        context.reset();

        // Include convenience properties for building off Guacamole's
        // built-in configuration with a custom logback.xml
        context.putProperty("guac_timestamp_pattern", LOG_PATTERN_FRAGMENT_TIMESTAMP);
        context.putProperty("guac_message_pattern", LOG_PATTERN_FRAGMENT_MESSAGE_BODY);
        context.putProperty("guac_pattern", LOG_PATTERN_DEFAULT);
        context.putProperty("guac_verbose_pattern", LOG_PATTERN_VERBOSE);

        // Initialize logback
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(logbackConfiguration);

        // Dump any errors that occur during logback init
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);

    }

}
