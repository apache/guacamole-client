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

package org.glyptodon.guacamole.net.auth.attribute;

import java.util.Set;
import org.glyptodon.guacamole.GuacamoleException;

/**
 * An arbitrary set of attributes.
 *
 * @author Michael Jumper
 * @param <AttributeType>
 *     The type of attribute stored within this AttributeSet.
 */
public interface AttributeSet<AttributeType extends Attribute> {

    /**
     * Returns a Set which contains all attributes stored or storable within
     * this attribute set. The value of each attribute, if any, is associated
     * with the attribute object within the set. The set itself may not be
     * mutable.
     *
     * @return
     *     A Set containing all attributes stored or storable within this
     *     attribute set, which may not be mutable.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the attributes, or if attributes
     *     cannot be retrieved due to lack of permissions to do so.
     */
    Set<AttributeType> getAttributes() throws GuacamoleException;

    /**
     * Changes each of the given attributes. If a specified attribute is
     * already set, the previous value will be overwritten with the new value.
     * If the new value is null, the previous value will be unset.
     *
     * @param attributes
     *     The attributes to update.
     *
     * @throws GuacamoleException
     *     If an error occurs while updating the attributes, if permission to
     *     update attributes is denied, or if any one of the given attributes
     *     is invalid or malformed.
     */
    void updateAttributes(Set<AttributeType> attributes)
            throws GuacamoleException;

}
