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

package org.glyptodon.guacamole.net.basic.rest;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Wrapper for HttpServletRequest which uses a given MultivaluedMap to provide
 * the values of all request parameters.
 * 
 * @author Michael Jumper
 */
public class APIRequest extends HttpServletRequestWrapper {

    /**
     * Map of all request parameter names to their corresponding values.
     */
    private final Map<String, String[]> parameters;

    /**
     * Wraps the given HttpServletRequest, using the given MultivaluedMap to
     * provide all request parameters. All HttpServletRequest functions which
     * do not deal with parameter names and values are delegated to the wrapped
     * request.
     *
     * @param request
     *     The HttpServletRequest to wrap.
     *
     * @param parameters
     *     All request parameters.
     */
    public APIRequest(HttpServletRequest request,
            MultivaluedMap<String, String> parameters) {

        super(request);

        // Copy parameters from given MultivaluedMap 
        this.parameters = new HashMap<String, String[]>(parameters.size());
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {

            // Get parameter name and all corresponding values
            String name = entry.getKey();
            List<String> values = entry.getValue();

            // Add parameters to map
            this.parameters.put(name, values.toArray(new String[values.size()]));
            
        }
        
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameters.get(name);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public String getParameter(String name) {

        // If no such parameter exists, just return null
        String[] values = getParameterValues(name);
        if (values == null)
            return null;

        // Otherwise, return first value
        return values[0];

    }

}
