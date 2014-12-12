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

package org.glyptodon.guacamole.net.basic.rest;

import com.google.inject.AbstractModule;
import org.glyptodon.guacamole.net.basic.rest.connection.ConnectionService;
import org.glyptodon.guacamole.net.basic.rest.connectiongroup.ConnectionGroupService;
import org.glyptodon.guacamole.net.basic.rest.permission.PermissionService;
import org.glyptodon.guacamole.net.basic.rest.protocol.ProtocolRetrievalService;

/**
 * A Guice Module for setting up dependency injection for the 
 * Guacamole REST API.
 * 
 * @author James Muehlner
 */
public class RESTModule extends AbstractModule {

    @Override
    protected void configure() {

        // Bind generic low-level services
        bind(ConnectionService.class);
        bind(ConnectionGroupService.class);
        bind(PermissionService.class);
        bind(ProtocolRetrievalService.class);
        
    }
    
}
