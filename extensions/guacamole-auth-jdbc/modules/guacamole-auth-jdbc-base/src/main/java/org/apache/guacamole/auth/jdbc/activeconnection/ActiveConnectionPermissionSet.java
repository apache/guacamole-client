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

package org.apache.guacamole.auth.jdbc.activeconnection;

import com.google.inject.Inject;
import org.apache.guacamole.auth.jdbc.permission.ObjectPermissionService;
import org.apache.guacamole.auth.jdbc.permission.ObjectPermissionSet;

/**
 * An implementation of ObjectPermissionSet which uses an injected service to
 * query and manipulate the permissions associated with active connections.
 *
 * @author Michael Jumper
 */
public class ActiveConnectionPermissionSet extends ObjectPermissionSet {

    /**
     * Service for querying and manipulating active connection permissions.
     */
    @Inject
    private ActiveConnectionPermissionService activeConnectionPermissionService;
    
    @Override
    protected ObjectPermissionService getObjectPermissionService() {
        return activeConnectionPermissionService;
    }
 
}
