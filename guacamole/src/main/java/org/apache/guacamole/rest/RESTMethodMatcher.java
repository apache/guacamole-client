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

package org.apache.guacamole.rest;

import com.google.inject.matcher.AbstractMatcher;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import org.apache.guacamole.GuacamoleException;

/**
 * A Guice Matcher which matches only methods which throw GuacamoleException
 * (or a subclass thereof) and are explicitly annotated as with an HTTP method
 * annotation like <code>@GET</code> or <code>@POST</code>. Any method which
 * throws GuacamoleException and is annotated with an annotation that is
 * annotated with <code>@HttpMethod</code> will match.
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
     * <code>@HttpMethod</code> or <code>@Path</code>.
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

            // A method is a REST method if it is annotated with @Path
            if (Path.class.isAssignableFrom(annotationType))
                return true;

        }

        // A method is also REST method if it overrides a REST method within
        // the superclass
        Class<?> superclass = method.getDeclaringClass().getSuperclass();
        if (superclass != null) {

            // Recheck against identical method within superclass
            try {
                return isRESTMethod(superclass.getMethod(method.getName(),
                        method.getParameterTypes()));
            }

            // If there is no such method, then this method cannot possibly be
            // a REST method
            catch (NoSuchMethodException e) {
                return false;
            }

        }

        // Lacking a superclass, the search stops here - it's not a REST method
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
