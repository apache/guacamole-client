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
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * A resource which is the logical concatenation of other resources.
 *
 * @author Michael Jumper
 */
public class SequenceResource extends AbstractResource {

    /**
     * The resources to be concatenated.
     */
    private final Iterable<Resource> resources;

    /**
     * Returns the mimetype of the first resource in the given Iterable, or
     * "application/octet-stream" if no resources are provided.
     *
     * @param resources
     *     The resources from which the mimetype should be retrieved.
     *
     * @return
     *     The mimetype of the first resource, or "application/octet-stream"
     *     if no resources were provided.
     */
    private static String getMimeType(Iterable<Resource> resources) {

        // If no resources, just assume application/octet-stream
        Iterator<Resource> resourceIterator = resources.iterator();
        if (!resourceIterator.hasNext())
            return "application/octet-stream";

        // Return mimetype of first resource
        return resourceIterator.next().getMimeType();

    }

    /**
     * Creates a new SequenceResource as the logical concatenation of the
     * given resources. Each resource is concatenated in iteration order as
     * needed when reading from the input stream of the SequenceResource.
     *
     * @param mimetype
     *     The mimetype of the resource.
     *
     * @param resources
     *     The resources to concatenate within the InputStream of this
     *     SequenceResource.
     */
    public SequenceResource(String mimetype, Iterable<Resource> resources) {
        super(mimetype);
        this.resources = resources;
    }

    /**
     * Creates a new SequenceResource as the logical concatenation of the
     * given resources. Each resource is concatenated in iteration order as
     * needed when reading from the input stream of the SequenceResource. The
     * mimetype of the resulting concatenation is derived from the first
     * resource.
     *
     * @param resources
     *     The resources to concatenate within the InputStream of this
     *     SequenceResource.
     */
    public SequenceResource(Iterable<Resource> resources) {
        super(getMimeType(resources));
        this.resources = resources;
    }

    /**
     * Creates a new SequenceResource as the logical concatenation of the
     * given resources. Each resource is concatenated in iteration order as
     * needed when reading from the input stream of the SequenceResource.
     *
     * @param mimetype
     *     The mimetype of the resource.
     *
     * @param resources
     *     The resources to concatenate within the InputStream of this
     *     SequenceResource.
     */
    public SequenceResource(String mimetype, Resource... resources) {
        this(mimetype, Arrays.asList(resources));
    }

    /**
     * Creates a new SequenceResource as the logical concatenation of the
     * given resources. Each resource is concatenated in iteration order as
     * needed when reading from the input stream of the SequenceResource. The
     * mimetype of the resulting concatenation is derived from the first
     * resource.
     *
     * @param resources
     *     The resources to concatenate within the InputStream of this
     *     SequenceResource.
     */
    public SequenceResource(Resource... resources) {
        this(Arrays.asList(resources));
    }

    @Override
    public InputStream asStream() {
        return new SequenceInputStream(new Enumeration<InputStream>() {

            /**
             * Iterator over all resources associated with this
             * SequenceResource.
             */
            private final Iterator<Resource> resourceIterator = resources.iterator();

            @Override
            public boolean hasMoreElements() {
                return resourceIterator.hasNext();
            }

            @Override
            public InputStream nextElement() {
                return resourceIterator.next().asStream();
            }

        });
    }

}
