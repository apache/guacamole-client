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

package org.glyptodon.guacamole.net.basic.rest;

import com.google.inject.matcher.AbstractMatcher;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import javax.ws.rs.HttpMethod;
import org.glyptodon.guacamole.GuacamoleException;

/**
 * A Guice Matcher which matches only methods which throw GuacamoleException
 * (or a subclass thereof) and are explicitly annotated as with an HTTP method
 * annotation like <code>@GET</code> or <code>@POST</code>. Any method which
 * throws GuacamoleException and is annotated with an annotation that is
 * annotated with <code>@HttpMethod</code> will match.
 *
 * @author Michael Jumper
 */
public class RESTMethodMatcher extends AbstractMatcher<Method> {

    /**
     * Returns whether the given method throws the specified exception type,
     * including any subclasses of that type.
     *
     * @param method
     *     The method to test.
     *
     * @param exceptionType
     *     The exception type to test for.
     *
     * @return
     *     true if the given method throws an exception of the specified type,
     *     false otherwise.
     */
    private boolean methodThrowsException(Method method,
            Class<? extends Exception> exceptionType) {

        // Check whether the method throws an exception of the specified type
        for (Class<?> thrownType : method.getExceptionTypes()) {
            if (exceptionType.isAssignableFrom(thrownType))
                return true;
        }

        // No such exception is declared to be thrown
        return false;
        
    }

    /**
     * Returns whether the given method is annotated as a REST method. A REST
     * method is annotated with an annotation which is annotated with
     * <code>@HttpMethod</code>.
     *
     * @param method
     *     The method to test.
     *
     * @return
     *     true if the given method is annotated as a REST method, false
     *     otherwise.
     */
    private boolean isRESTMethod(Method method) {

        // Check whether the required REST annotations are present
        for (Annotation annotation : method.getAnnotations()) {

            // A method is a REST method if it is annotated with @HttpMethod
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.isAnnotationPresent(HttpMethod.class))
                return true;

        }

        // The method is not an HTTP method
        return false;

    }

    @Override
    public boolean matches(Method method) {

        // Guacamole REST methods are REST methods which throw
        // GuacamoleExceptions
        return isRESTMethod(method)
            && methodThrowsException(method, GuacamoleException.class);

    }

}
