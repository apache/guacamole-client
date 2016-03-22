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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * A resource which contains a defined byte array.
 *
 * @author Michael Jumper
 */
public class ByteArrayResource extends AbstractResource {

    /**
     * The bytes contained by this resource.
     */
    private final byte[] bytes;

    /**
     * Creates a new ByteArrayResource which provides access to the given byte
     * array. Changes to the given byte array will affect this resource even
     * after the resource is created. Changing the byte array while an input
     * stream from this resource is in use has undefined behavior.
     *
     * @param mimetype
     *     The mimetype of the resource.
     *
     * @param bytes
     *     The bytes that this resource should contain.
     */
    public ByteArrayResource(String mimetype, byte[] bytes) {
        super(mimetype);
        this.bytes = bytes;
    }

    @Override
    public InputStream asStream() {
        return new ByteArrayInputStream(bytes);
    }

}
