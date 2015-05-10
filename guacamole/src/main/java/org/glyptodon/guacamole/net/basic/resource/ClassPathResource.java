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

package org.glyptodon.guacamole.net.basic.resource;

import java.io.InputStream;

/**
 * A resource which is located within the classpath of an arbitrary
 * ClassLoader.
 *
 * @author Michael Jumper
 */
public class ClassPathResource extends AbstractResource {

    /**
     * The classloader to use when reading this resource.
     */
    private final ClassLoader classLoader;

    /**
     * The path of this resource relative to the classloader.
     */
    private final String path;

    /**
     * Creates a new ClassPathResource which uses the given ClassLoader to
     * read the resource having the given path.
     *
     * @param classLoader
     *     The ClassLoader to use when reading the resource.
     *
     * @param mimetype
     *     The mimetype of the resource.
     *
     * @param path
     *     The path of the resource relative to the given ClassLoader.
     */
    public ClassPathResource(ClassLoader classLoader, String mimetype, String path) {
        super(mimetype);
        this.classLoader = classLoader;
        this.path = path;
    }

    /**
     * Creates a new ClassPathResource which uses the ClassLoader associated
     * with the ClassPathResource class to read the resource having the given
     * path.
     *
     * @param mimetype
     *     The mimetype of the resource.
     *
     * @param path
     *     The path of the resource relative to the ClassLoader associated
     *     with the ClassPathResource class.
     */
    public ClassPathResource(String mimetype, String path) {
        this(ClassPathResource.class.getClassLoader(), mimetype, path);
    }

    @Override
    public InputStream asStream() {
        return classLoader.getResourceAsStream(path);
    }

}
