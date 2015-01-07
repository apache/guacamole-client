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

package org.glyptodon.guacamole.token;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test which verifies the filtering functionality of TokenFilter.
 *
 * @author Michael Jumper
 */
public class TokenFilterTest {

    /**
     * Verifies that token replacement via filter() functions as specified.
     */
    @Test
    public void testFilter() {

        // Create token filter
        TokenFilter tokenFilter = new TokenFilter();
        tokenFilter.setToken("TOKEN_A", "value-of-a");
        tokenFilter.setToken("TOKEN_B", "value-of-b");

        // Test basic substitution and escaping
        assertEquals(
            "$${NOPE}hellovalue-of-aworldvalue-of-b${NOT_A_TOKEN}",
            tokenFilter.filter("$$${NOPE}hello${TOKEN_A}world${TOKEN_B}$${NOT_A_TOKEN}")
        );
        
        // Unknown tokens must be interpreted as literals
        assertEquals(
            "${NOPE}hellovalue-of-aworld${TOKEN_C}",
            tokenFilter.filter("${NOPE}hello${TOKEN_A}world${TOKEN_C}")
        );
        
    }
    
    /**
     * Verifies that token replacement via filterValues() functions as
     * specified.
     */
    @Test
    public void testFilterValues() {

        // Create token filter
        TokenFilter tokenFilter = new TokenFilter();
        tokenFilter.setToken("TOKEN_A", "value-of-a");
        tokenFilter.setToken("TOKEN_B", "value-of-b");

        // Create test map
        Map<Integer, String> map = new HashMap<Integer, String>();
        map.put(1, "$$${NOPE}hello${TOKEN_A}world${TOKEN_B}$${NOT_A_TOKEN}");
        map.put(2, "${NOPE}hello${TOKEN_A}world${TOKEN_C}");
        map.put(3, null);

        // Filter map values
        tokenFilter.filterValues(map);

        // Filter should not affect size of map
        assertEquals(3, map.size());

        // Filtered value 1
        assertEquals(
            "$${NOPE}hellovalue-of-aworldvalue-of-b${NOT_A_TOKEN}",
            map.get(1)
        );
        
        // Filtered value 2
        assertEquals(
            "${NOPE}hellovalue-of-aworld${TOKEN_C}",
            map.get(2)
        );

        // Null values are not filtered
        assertNull(map.get(3));
        
    }
    
}
