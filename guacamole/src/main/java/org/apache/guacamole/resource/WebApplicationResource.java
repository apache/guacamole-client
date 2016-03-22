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

package org.apache.guacamole.resource;

import java.io.InputStream;
import javax.servlet.ServletContext;

/**
 * A resource which is located within the classpath associated with another
 * class.
 *
 * @author Michael Jumper
 */
public class WebApplicationResource extends AbstractResource {

    /**
     * The servlet context to use when reading the resource and, if necessary,
     * when determining the mimetype of the resource.
     */
    private final ServletContext context;

    /**
     * The path of this resource relative to the ServletContext.
     */
    private final String path;

    /**
     * Derives a mimetype from the filename within the given path using the
     * given ServletContext, if possible.
     *
     * @param context
     *     The ServletContext to use to derive the mimetype.
     *
     * @param path
     *     The path to derive the mimetype from.
     *
     * @return
     *     An appropriate mimetype based on the name of the file in the path,
     *     or "application/octet-stream" if no mimetype could be determined.
     */
    private static String getMimeType(ServletContext context, String path) {

        // If mimetype is known, use defined mimetype
        String mimetype = context.getMimeType(path);
        if (mimetype != null)
            return mimetype;

        // Otherwise, default to application/octet-stream
        return "application/octet-stream";

    }

    /**
     * Creates a new WebApplicationResource which serves the resource at the
     * given path relative to the given ServletContext. Rather than deriving
     * the mimetype of the resource from the filename within the path, the
     * mimetype given is used.
     *
     * @param context
     *     The ServletContext to use when reading the resource.
     *
     * @param mimetype
     *     The mimetype of the resource.
     *
     * @param path
     *     The path of the resource relative to the given ServletContext.
     */
    public WebApplicationResource(ServletContext context, String mimetype, String path) {
        super(mimetype);
        this.context = context;
        this.path = path;
    }

    /**
     * Creates a new WebApplicationResource which serves the resource at the
     * given path relative to the given ServletContext. The mimetype of the
     * resource is automatically determined based on the filename within the
     * path.
     *
     * @param context
     *     The ServletContext to use when reading the resource and deriving the
     *     mimetype.
     *
     * @param path
     *     The path of the resource relative to the given ServletContext.
     */
    public WebApplicationResource(ServletContext context, String path) {
        this(context, getMimeType(context, path), path);
    }

    @Override
    public InputStream asStream() {
        return context.getResourceAsStream(path);
    }

}
