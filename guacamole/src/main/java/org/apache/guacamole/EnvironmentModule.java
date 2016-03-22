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

package org.apache.guacamole;

import com.google.inject.AbstractModule;
import org.apache.guacamole.environment.Environment;

/**
 * Guice module which binds the base Guacamole server environment.
 *
 * @author Michael Jumper
 */
public class EnvironmentModule extends AbstractModule {

    /**
     * The Guacamole server environment.
     */
    private final Environment environment;

    /**
     * Creates a new EnvironmentModule which will bind the given environment
     * for future injection.
     *
     * @param environment
     *     The environment to bind.
     */
    public EnvironmentModule(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected void configure() {

        // Bind environment
        bind(Environment.class).toInstance(environment);

    }

}

