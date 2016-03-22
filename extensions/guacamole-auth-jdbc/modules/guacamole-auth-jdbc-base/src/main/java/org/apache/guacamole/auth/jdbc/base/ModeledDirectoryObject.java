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

package org.apache.guacamole.auth.jdbc.base;

import org.apache.guacamole.net.auth.Identifiable;

/**
 * Common base class for objects that will ultimately be made available through
 * the Directory class and are persisted to an underlying database model. All
 * such objects will need the same base set of queries to fulfill the needs of
 * the Directory class.
 *
 * @author Michael Jumper
 * @param <ModelType>
 *     The type of model object that corresponds to this object.
 */
public abstract class ModeledDirectoryObject<ModelType extends ObjectModel>
    extends ModeledObject<ModelType> implements Identifiable {

    @Override
    public String getIdentifier() {
        return getModel().getIdentifier();
    }

    @Override
    public void setIdentifier(String identifier) {
        getModel().setIdentifier(identifier);
    }

}
