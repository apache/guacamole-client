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

/**
 * Base abstract resource implementation which provides an associated mimetype,
 * and modification time. Classes which extend AbstractResource must provide
 * their own InputStream, however.
 *
 * @author Michael Jumper
 */
public abstract class AbstractResource implements Resource {

    /**
     * The mimetype of this resource.
     */
    private final String mimetype;

    /**
     * The time this resource was last modified, in milliseconds since midnight
     * of January 1, 1970 UTC.
     */
    private final long lastModified;

    /**
     * Initializes this AbstractResource with the given mimetype and
     * modification time.
     *
     * @param mimetype
     *     The mimetype of this resource.
     *
     * @param lastModified
     *     The time this resource was last modified, in milliseconds since
     *     midnight of January 1, 1970 UTC.
     */
    public AbstractResource(String mimetype, long lastModified) {
        this.mimetype = mimetype;
        this.lastModified = lastModified;
    }

    /**
     * Initializes this AbstractResource with the given mimetype. The
     * modification time of the resource is set to the current system time.
     *
     * @param mimetype
     *     The mimetype of this resource.
     */
    public AbstractResource(String mimetype) {
        this(mimetype, System.currentTimeMillis());
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public String getMimeType() {
        return mimetype;
    }

}
