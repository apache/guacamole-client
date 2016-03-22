/*
 * Copyright (C) 2014 Glyptodon LLC
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

package org.apache.guacamole.net.basic.rest;

/**
 * An object for representing the body of a HTTP PATCH method.
 * See https://tools.ietf.org/html/rfc6902
 * 
 * @author James Muehlner
 * @param <T> The type of object being patched.
 */
public class APIPatch<T> {
    
    /**
     * The possible operations for a PATCH request.
     */
    public enum Operation {
        add, remove, test, copy, replace, move
    }
    
    /**
     * The operation to perform for this patch.
     */
    private Operation op;
    
    /**
     * The value for this patch.
     */
    private T value;
    
    /**
     * The path for this patch.
     */
    private String path;

    /**
     * Returns the operation for this patch.
     * @return the operation for this patch. 
     */
    public Operation getOp() {
        return op;
    }

    /**
     * Set the operation for this patch.
     * @param op The operation for this patch.
     */
    public void setOp(Operation op) {
        this.op = op;
    }

    /**
     * Returns the value of this patch.
     * @return The value of this patch.
     */
    public T getValue() {
        return value;
    }

    /**
     * Sets the value of this patch.
     * @param value The value of this patch.
     */
    public void setValue(T value) {
        this.value = value;
    }

    /**
     * Returns the path for this patch.
     * @return The path for this patch.
     */
    public String getPath() {
        return path;
    }

    /**
     * Set the path for this patch.
     * @param path The path for this patch.
     */
    public void setPath(String path) {
        this.path = path;
    }

}
