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

package org.apache.guacamole.auth.json;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import org.apache.guacamole.GuacamoleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

/**
 * Service for testing the validity of received HTTP requests.
 *
 * @author Michael Jumper
 */
public class RequestValidationService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(RequestValidationService.class);

    /**
     * Service for retrieving configuration information regarding the
     * JSONAuthenticationProvider.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Returns whether the given request can be used for authentication, taking
     * into account restrictions specified within guacamole.properties.
     *
     * @param request
     *     The HTTP request to test.
     *
     * @return
     *     true if the given request comes from a trusted source and can be
     *     used for authentication, false otherwise.
     */
    public boolean isAuthenticationAllowed(HttpServletRequest request) {

        // Pull list of all trusted networks
        Collection<String> trustedNetworks;
        try {
            trustedNetworks = confService.getTrustedNetworks();
        }

        // Deny all requests if restrictions cannot be parsed
        catch (GuacamoleException e) {
            logger.warn("Authentication request from \"{}\" is DENIED due to parse error: {}", request.getRemoteAddr(), e.getMessage());
            logger.debug("Error parsing authentication request restrictions from guacamole.properties.", e);
            return false;
        }

        // All requests are allowed if no restrictions are defined
        if (trustedNetworks.isEmpty()) {
            logger.debug("Authentication request from \"{}\" is ALLOWED (no restrictions).", request.getRemoteAddr());
            return true;
        }

        // Build matchers for each trusted network
        Collection<IpAddressMatcher> matchers = new ArrayList<IpAddressMatcher>(trustedNetworks.size());
        for (String network : trustedNetworks)
            matchers.add(new IpAddressMatcher(network));

        // Otherwise ensure at least one subnet matches
        for (IpAddressMatcher matcher : matchers) {

            // Request is allowed if any subnet matches
            if (matcher.matches(request)) {
                logger.debug("Authentication request from \"{}\" is ALLOWED (matched subnet).", request.getRemoteAddr());
                return true;
            }

        }

        // Otherwise request is denied - no subnets matched
        logger.debug("Authentication request from \"{}\" is DENIED (did not match subnet).", request.getRemoteAddr());
        return false;

    }

}
