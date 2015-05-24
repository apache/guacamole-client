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

package org.glyptodon.guacamole.net.auth.simple;

import java.util.Collections;
import java.util.Set;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.auth.attribute.ObjectAttribute;
import org.glyptodon.guacamole.net.auth.attribute.ObjectAttributeSet;

/**
 * A read-only implementation of ObjectAttributeSet which uses a backing Set
 * of Attribute for attribute/value storage.
 *
 * @author Michael Jumper
 */
public class SimpleObjectAttributeSet implements ObjectAttributeSet {

    /**
     * The set of all attributes currently set.
     */
    private Set<ObjectAttribute> attributes = Collections.<ObjectAttribute>emptySet();

    /**
     * Creates a new empty SimpleObjectAttributeSet.
     */
    public SimpleObjectAttributeSet() {
    }

    /**
     * Creates a new SimpleObjectAttributeSet which contains the attributes
     * within the given Set.
     *
     * @param attributes
     *     The Set of attributes this SimpleObjectAttributeSet should
     *     contain.
     */
    public SimpleObjectAttributeSet(Set<ObjectAttribute> attributes) {
        this.attributes = attributes;
    }

    /**
     * Sets the Set which backs this SimpleObjectAttributeSet. Future function
     * calls on this SimpleObjectAttributeSet will use the provided Set.
     *
     * @param attributes
     *     The Set of attributes this SimpleObjectAttributeSet should
     *     contain.
     */
    protected void setAttributes(Set<ObjectAttribute> attributes) {
        this.attributes = attributes;
    }

    @Override
    public Set<ObjectAttribute> getAttributes() throws GuacamoleException {
        return attributes;
    }

    @Override
    public void updateAttributes(Set<ObjectAttribute> attributes) throws GuacamoleException {
        throw new GuacamoleSecurityException("Permission denied.");
    }

}
