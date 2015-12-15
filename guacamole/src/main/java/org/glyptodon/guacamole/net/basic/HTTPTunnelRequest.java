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

package org.glyptodon.guacamole.net.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * HTTP-specific implementation of TunnelRequest.
 *
 * @author Michael Jumper
 */
public class HTTPTunnelRequest extends TunnelRequest {

    /**
     * A copy of the parameters obtained from the HttpServletRequest used to
     * construct the HTTPTunnelRequest.
     */
    private final Map<String, List<String>> parameterMap =
            new HashMap<String, List<String>>();

    /**
     * Creates a HTTPTunnelRequest which copies and exposes the parameters
     * from the given HttpServletRequest.
     *
     * @param request
     *     The HttpServletRequest to copy parameter values from.
     */
    @SuppressWarnings("unchecked") // getParameterMap() is defined as returning Map<String, String[]>
    public HTTPTunnelRequest(HttpServletRequest request) {

        // For each parameter
        for (Map.Entry<String, String[]> mapEntry : ((Map<String, String[]>)
                request.getParameterMap()).entrySet()) {

            // Get parameter name and corresponding values
            String parameterName = mapEntry.getKey();
            List<String> parameterValues = Arrays.asList(mapEntry.getValue());

            // Store copy of all values in our own map
            parameterMap.put(
                parameterName,
                new ArrayList<String>(parameterValues)
            );

        }

    }

    @Override
    public String getParameter(String name) {
        List<String> values = getParameterValues(name);

        // Return the first value from the list if available
        if (values != null && !values.isEmpty())
            return values.get(0);

        return null;
    }

    @Override
    public List<String> getParameterValues(String name) {
        return parameterMap.get(name);
    }
    
}
