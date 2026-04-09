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

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
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

    private JsonUrlReader() {}

    public static Map<String,Object> fetch(String method, URL url, String body) throws GuacamoleException {
        if (url == null) {
            throw new GuacamoleException("JsonUrlReader : Missing URL");
        }
        
        try {
            // Open connection, using HttpURLConnection
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            if (! method.equalsIgnoreCase("GET")) {
                conn.setDoOutput(true);
                try (OutputStream out = conn.getOutputStream()) {
                    byte [] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
                    out.write(bodyBytes, 0, bodyBytes.length);
                }
            }

            // Read response
            int status = conn.getResponseCode();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            status >= 200 && status < 300
                                    ? conn.getInputStream()
                                    : conn.getErrorStream(),
                            StandardCharsets.UTF_8
                    )
            );

            StringBuilder responseBody = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBody.append(line);
            }
            reader.close();

            logger.debug("Response body : {}", responseBody.toString());

            Map<String,Object> json = JsonUtil.parseJson(responseBody.toString());

            if (status < 200 || status >= 300) {
                throw new GuacamoleException("(status: " + status + "): " + json.toString());
            }

            return json;
        } catch (Exception e) {
            throw new GuacamoleException("JsonUrlreader error : " + method + " : " + e.getMessage());
        }
    }
}
