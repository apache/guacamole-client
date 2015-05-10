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
 * An arbitrary resource that can be served to a user via HTTP. Resources are
 * anonymous but have a defined mimetype and corresponding input stream.
 *
 * @author Michael Jumper
 */
public interface Resource {

    /**
     * Returns the mimetype of this resource. This function MUST always return
     * a value. If the type is unknown, return "application/octet-stream".
     *
     * @return
     *     The mimetype of this resource.
     */
    String getMimeType();

    /**
     * Returns the time the resource was last modified in milliseconds since
     * midnight of January 1, 1970 UTC.
     *
     * @return
     *      The time the resource was last modified, in milliseconds.
     */
    long getLastModified();

    /**
     * Returns an InputStream which reads the contents of this resource,
     * starting with the first byte. Reading from the returned InputStream will
     * not affect reads from other InputStreams returned by other calls to
     * asStream(). The returned InputStream must be manually closed when no
     * longer needed. If the resource is unexpectedly unavailable, this will
     * return null.
     *
     * @return
     *     An InputStream which reads the contents of this resource, starting
     *     with the first byte, or null if the resource is unavailable.
     */
    InputStream asStream();

}
