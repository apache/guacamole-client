/*
 * Copyright (C) 2016 Glyptodon LLC
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

package org.glyptodon.guacamole.net.basic.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.glyptodon.guacamole.net.basic.resource.Resource;

/**
 * Service which provides access to all HTML patches as resources, and allows
 * other patch resources to be added.
 *
 * @author Michael Jumper
 */
public class PatchResourceService {

    /**
     * A list of all HTML patch resources currently defined, in the order they
     * should be applied.
     */
    private final List<Resource> resources = new ArrayList<Resource>();

    /**
     * Adds the given HTML patch resource such that it will apply to the
     * Guacamole UI. The patch will be applied by the JavaScript side of the
     * web application in the order that addPatchResource() is invoked.
     *
     * @param resource
     *     The HTML patch resource to add. This resource must have the mimetype
     *     "text/html".
     */
    public void addPatchResource(Resource resource) {
        resources.add(resource);
    }

    /**
     * Adds the given HTML patch resources such that they will apply to the
     * Guacamole UI. The patches will be applied by the JavaScript side of the
     * web application in the order provided.
     *
     * @param resources
     *     The HTML patch resources to add. Each resource must have the
     *     mimetype "text/html".
     */
    public void addPatchResources(Collection<Resource> resources) {
        for (Resource resource : resources)
            addPatchResource(resource);
    }

    /**
     * Returns a list of all HTML patches currently associated with this
     * service, in the order they should be applied. The returned list cannot
     * be modified.
     *
     * @return
     *     A list of all HTML patches currently associated with this service.
     */
    public List<Resource> getPatchResources() {
        return Collections.unmodifiableList(resources);
    }

}
