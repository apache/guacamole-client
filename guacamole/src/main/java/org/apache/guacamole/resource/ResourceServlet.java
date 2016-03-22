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

package org.apache.guacamole.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet which serves a given resource for all HTTP GET requests. The HEAD
 * method is correctly supported, and HTTP 304 ("Not Modified") responses will
 * be properly returned for GET requests depending on the last time the
 * resource was modified.
 *
 * @author Michael Jumper
 */
public class ResourceServlet extends HttpServlet {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(ResourceServlet.class);

    /**
     * The size of the buffer to use when transferring data from the input
     * stream of a resource to the output stream of a request.
     */
    private static final int BUFFER_SIZE = 10240;

    /**
     * The resource to serve for every GET request.
     */
    private final Resource resource;

    /**
     * Creates a new ResourceServlet which serves the given Resource for all
     * HTTP GET requests.
     *
     * @param resource
     *     The Resource to serve.
     */
    public ResourceServlet(Resource resource) {
        this.resource = resource;
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Set last modified and content type headers
        response.addDateHeader("Last-Modified", resource.getLastModified());
        response.setContentType(resource.getMimeType());

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Get input stream from resource
        InputStream input = resource.asStream();

        // If resource does not exist, return not found
        if (input == null) {
            logger.debug("Resource does not exist: \"{}\"", request.getServletPath());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {

            // Write headers
            doHead(request, response);

            // If not modified since "If-Modified-Since" header, return not modified
            long ifModifiedSince = request.getDateHeader("If-Modified-Since");
            if (resource.getLastModified() - ifModifiedSince < 1000) {
                logger.debug("Resource not modified: \"{}\"", request.getServletPath());
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }

            int length;
            byte[] buffer = new byte[BUFFER_SIZE];

            // Write resource to response body
            OutputStream output = response.getOutputStream();
            while ((length = input.read(buffer)) != -1)
                output.write(buffer, 0, length);

        }

        // Ensure input stream is always closed
        finally {
            input.close();
        }

    }

}
