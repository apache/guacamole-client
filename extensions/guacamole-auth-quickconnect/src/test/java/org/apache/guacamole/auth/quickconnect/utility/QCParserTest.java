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
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Class to test methods in the QCParser utility class.
 */
public class QCParserTest {

    /**
     * Verify that the parseQueryString() method functions as designed.
     * 
     * @throws UnsupportedEncodingException
     *     If Java lacks UTF-8 support.
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

    /**
     * Verify that the parseUserInfo() method functions as designed.
     * 
     * @throws UnsupportedEncodingException
     *     If Java lacks UTF-8 support.
     */
    @Test
    public void testParseUserInfo() throws UnsupportedEncodingException {

        Map<String, String> userInfoMap;

        GuacamoleConfiguration config1 = new GuacamoleConfiguration();
        QCParser.parseUserInfo("guacuser:secretpw", config1);
        assertEquals("guacuser", config1.getParameter("username"));
        assertEquals("secretpw", config1.getParameter("password"));

        GuacamoleConfiguration config2 = new GuacamoleConfiguration();
        QCParser.parseUserInfo("guacuser", config2);
        assertEquals("guacuser", config2.getParameter("username"));
        assertNull(config2.getParameter("password"));

        GuacamoleConfiguration config3 = new GuacamoleConfiguration();
        QCParser.parseUserInfo("guacuser:P%40ssw0rd%21", config3);
        assertEquals("guacuser", config3.getParameter("username"));
        assertEquals("P@ssw0rd!", config3.getParameter("password"));

        GuacamoleConfiguration config4 = new GuacamoleConfiguration();
        QCParser.parseUserInfo("domain%5cguacuser:domain%2fpassword", config4);
        assertEquals("domain\\guacuser", config4.getParameter("username"));
        assertEquals("domain/password", config4.getParameter("password"));

    }

    /**
     * Verify that the getConfiguration() method returns the expected
     * GuacamoleConfiguration object.
     * 
     * @throws GuacamoleException
     *     If the configuration cannot be parsed from the given URI.
     */
    @Test
    public void testGetConfiguration() throws GuacamoleException {

        String uri1 = "ssh://guacuser:guacpassword@hostname1.domain.local/?param1=value1&param2=value2";
        GuacamoleConfiguration config1 = QCParser.getConfiguration(uri1);
        assertEquals("ssh", config1.getProtocol());
        assertEquals("hostname1.domain.local", config1.getParameter("hostname"));
        assertEquals("guacuser", config1.getParameter("username"));
        assertEquals("guacpassword", config1.getParameter("password"));
        assertEquals("value1", config1.getParameter("param1"));
        assertEquals("value2", config1.getParameter("param2"));

        String uri2 = "rdp://domain%5cguacuser:adPassword123@windows1.domain.tld/?enable-sftp=true";
        GuacamoleConfiguration config2 = QCParser.getConfiguration(uri2);
        assertEquals("rdp", config2.getProtocol());
        assertEquals("windows1.domain.tld", config2.getParameter("hostname"));
        assertEquals("domain\\guacuser", config2.getParameter("username"));
        assertEquals("adPassword123", config2.getParameter("password"));
        assertEquals("true", config2.getParameter("enable-sftp"));

        String uri3 = "vnc://mirror1.example.com:5910/";
        GuacamoleConfiguration config3 = QCParser.getConfiguration(uri3);
        assertEquals("vnc", config3.getProtocol());
        assertEquals("mirror1.example.com", config3.getParameter("hostname"));
        assertEquals("5910", config3.getParameter("port"));

    }

}
