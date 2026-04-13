/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.auth.openid.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.jose4j.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
 * Utility class to open a http connection to a URL, send a body
 * and receive a response in the form of a parsed JSON
 */

public final class JsonUrlReader {
    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(JsonUrlReader.class);

    /**
     * Class to GET and POST to a URL and read the returned JSON. This class should
     * not be instantiated.
     */
    private JsonUrlReader() {}

    /**
     * Method to POST or GET to a URL and recover the JSON in the form of a Map
     *
     * @param String method
     *      The http method to use. Should be "GET", "POST" or "PATCH".
     *
     * @param URI uri
     *      A URI value giving the address where to recover the JSON
     *
     * @param String body
     *      A pre-encoded body string to be sent to the address if the method is
     *      "POST" or "PATCH". Ignored if the method is "GET".
     *
     * @return
     *      A Map<String,Object> containing the decoded json values.
     */
    public static Map<String, Object> fetch(String method, URI uri, String body) throws IOException {
        if (uri == null || uri.toString().isEmpty()) {
            throw new IOException("JsonUrlReader: Missing URL");
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(uri);
            if (method != "GET") {
                // FIXME: If this function is ever used to post json bodies this header
                // will need to be configurable
                requestBuilder.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .method(method, HttpRequest.BodyPublishers.ofString(body == null ? "" : body,
                                                                    StandardCharsets.UTF_8));
            }
            else {
                requestBuilder.GET();
            }

            // Asynchronous, non-blocking send, so that tomcat servlets are not blocked by outbound connection
            CompletableFuture<HttpResponse<String>> future = client.sendAsync(requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            HttpResponse<String> response = future.join();
            int status = response.statusCode();
            logger.debug("Response body: {}", response.body());
            Map<String, Object> json = JsonUtil.parseJson(response.body());

            if (status < 200 || status >= 300) {
                throw new IOException("(status: " + status + "): " + json.toString());
            }

            return json;
        }
        catch (Exception e) {
            throw new IOException("JsonUrlReader error: " + e.getMessage());
        }
    }
}
