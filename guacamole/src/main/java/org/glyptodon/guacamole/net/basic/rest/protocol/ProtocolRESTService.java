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

package org.glyptodon.guacamole.net.basic.rest.protocol;

import com.google.inject.Inject;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.basic.ProtocolInfo;
import org.glyptodon.guacamole.net.basic.rest.AuthProviderRESTExposure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A REST Service for handling the listing of protocols.
 * 
 * @author James Muehlner
 */
@Path("/protocol")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProtocolRESTService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(ProtocolRESTService.class);
    
    /**
     * Service for retrieving protocol definitions.
     */
    @Inject
    private ProtocolRetrievalService protocolRetrievalservice;

    /**
     * Gets a map of protocols defined in the system - protocol name to protocol.
     * 
     * @return The protocol map.
     * @throws GuacamoleException If a problem is encountered while listing protocols.
     */
    @GET
    @AuthProviderRESTExposure
    public Map<String, ProtocolInfo> getProtocols() throws GuacamoleException {
        
        // Get and return a map of all protocols.
        return protocolRetrievalservice.getProtocolMap();
    }

}
