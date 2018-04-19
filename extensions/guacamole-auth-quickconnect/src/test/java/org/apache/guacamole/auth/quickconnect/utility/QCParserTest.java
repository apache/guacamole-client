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

package org.apache.guacamole.auth.quickconnect.utility;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Class to test methods in the QCParser utility class.
 */
public class QCParserTest {

    /**
     * Verify that the parseQueryString() method functions as designed.
     */
    @Test
    public void testParseQueryString() throws UnsupportedEncodingException {

        final String queryString = "param1=value1&param2=value2=3&param3=value%3D3&param4=value%264";
        Map<String, String> queryMap = QCParser.parseQueryString(queryString);

        assertEquals("value1", queryMap.get("param1"));
        assertEquals("value2=3", queryMap.get("param2"));
        assertEquals("value=3", queryMap.get("param3"));
        assertEquals("value&4", queryMap.get("param4"));

    }

}
