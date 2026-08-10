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

package org.apache.guacamole.token;

import org.apache.guacamole.GuacamoleException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests which characterize the existing parameter token grammar accepted by
 * TokenFilter. These tests are intended to pin down current behavior so that
 * any change to the token pattern can be evaluated for backwards
 * compatibility, particularly with respect to token names containing
 * characters outside [A-Za-z0-9_], unrecognized modifiers, and the behavior
 * of strict mode when a candidate token cannot be resolved.
 */
public class TokenFilterGrammarTest {

    /**
     * Returns a TokenFilter with a single known token defined, for use in
     * verifying how inputs surrounding that token are interpreted.
     *
     * @return
     *     A TokenFilter with the token "TOKEN_C" defined.
     */
    private TokenFilter createFilter() {
        TokenFilter filter = new TokenFilter();
        filter.setToken("TOKEN_C", "Value-of-C");
        return filter;
    }

    /**
     * Verifies that the documented LOWER and UPPER modifiers are applied to
     * the value of a defined token.
     */
    @Test
    public void testDocumentedModifiers() {

        TokenFilter filter = createFilter();

        assertEquals("Value-of-C", filter.filter("${TOKEN_C}"));
        assertEquals("value-of-c", filter.filter("${TOKEN_C:LOWER}"));
        assertEquals("VALUE-OF-C", filter.filter("${TOKEN_C:UPPER}"));

    }

    /**
     * Verifies that a modifier which is not recognized is ignored, leaving the
     * value of the token substituted unmodified. The value is substituted
     * rather than the token being left as a literal.
     */
    @Test
    public void testUnrecognizedModifierYieldsRawValue() {

        TokenFilter filter = createFilter();

        assertEquals("Value-of-C", filter.filter("${TOKEN_C:BOGUS}"));
        assertEquals("Value-of-C", filter.filter("${TOKEN_C:lower}"));

    }

    /**
     * Verifies that a defined token bearing an unrecognized modifier resolves
     * successfully in strict mode, rather than being reported as undefined.
     */
    @Test
    public void testUnrecognizedModifierResolvesUnderStrictMode()
            throws GuacamoleException {

        TokenFilter filter = createFilter();

        assertEquals("Value-of-C", filter.filterStrict("${TOKEN_C:BOGUS}"));

    }

    /**
     * Verifies that text which resembles a token but contains characters not
     * permitted within a token name is not treated as a token at all. Such
     * text is preserved literally and, critically, does not cause strict mode
     * to report an undefined token.
     */
    @Test
    public void testNonTokenTextIsNotTreatedAsToken()
            throws GuacamoleException {

        TokenFilter filter = createFilter();

        // Not tokens, as spaces, slashes, and dots are outside the set of
        // characters permitted within a token name
        assertEquals("${NOT A TOKEN}", filter.filter("${NOT A TOKEN}"));
        assertEquals("${a/b/c}", filter.filter("${a/b/c}"));
        assertEquals("${some.value}", filter.filter("${some.value}"));

        // The same inputs must not be reported as undefined tokens under
        // strict mode, as they were never tokens to begin with
        assertEquals("${NOT A TOKEN}", filter.filterStrict("${NOT A TOKEN}"));
        assertEquals("${a/b/c}", filter.filterStrict("${a/b/c}"));
        assertEquals("${some.value}", filter.filterStrict("${some.value}"));

    }

    /**
     * Verifies that literal braces appearing within a value, whether balanced
     * or unbalanced, do not affect the interpretation of surrounding tokens.
     */
    @Test
    public void testLiteralBracesAreNotConsumed() {

        TokenFilter filter = createFilter();

        assertEquals("{Value-of-C}", filter.filter("{${TOKEN_C}}"));
        assertEquals("{{Value-of-C", filter.filter("{{${TOKEN_C}"));
        assertEquals("Value-of-C}}", filter.filter("${TOKEN_C}}}"));
        assertEquals("a{b}cValue-of-C", filter.filter("a{b}c${TOKEN_C}"));

    }

    /**
     * Verifies that escaping a token by preceding it with an additional '$'
     * prevents substitution, and that the escape itself is consumed.
     */
    @Test
    public void testEscapedTokensAreNotSubstituted() {

        TokenFilter filter = createFilter();

        assertEquals("${TOKEN_C}", filter.filter("$${TOKEN_C}"));
        assertEquals("${TOKEN_C:UPPER}", filter.filter("$${TOKEN_C:UPPER}"));
        assertEquals("Value-of-C${TOKEN_C}",
                filter.filter("${TOKEN_C}$${TOKEN_C}"));

    }

    /**
     * Verifies that the OPTIONAL modifier causes an undefined token to be
     * replaced with a blank value rather than being preserved literally or
     * reported as undefined.
     */
    @Test
    public void testOptionalModifier() throws GuacamoleException {

        TokenFilter filter = createFilter();

        assertEquals("", filter.filter("${UNDEFINED:OPTIONAL}"));
        assertEquals("", filter.filterStrict("${UNDEFINED:OPTIONAL}"));
        assertEquals("a-b", filter.filter("a-${UNDEFINED:OPTIONAL}b"));

    }

    /**
     * Verifies that an undefined token with no modifier is preserved literally
     * in non-strict mode, and reported in strict mode.
     */
    @Test
    public void testUndefinedTokenHandling() {

        TokenFilter filter = createFilter();

        assertEquals("${UNDEFINED}", filter.filter("${UNDEFINED}"));

        try {
            filter.filterStrict("${UNDEFINED}");
            fail("An undefined token must be reported under strict mode.");
        }
        catch (GuacamoleTokenUndefinedException e) {
            assertEquals("UNDEFINED", e.getTokenName());
        }
        catch (GuacamoleException e) {
            fail("Unexpected exception type: " + e);
        }

    }

    /**
     * Verifies that repeated tokens separated by at least one character are
     * each substituted.
     *
     * Note that two directly adjacent tokens are NOT both substituted. The
     * token pattern matches a single character preceding each token in order
     * to detect escaping, and for adjacent tokens that character is consumed
     * from the preceding match, leaving the second token unmatched and
     * therefore literal. This is existing behavior, recorded here so that it
     * is not altered unintentionally.
     */
    @Test
    public void testAdjacentAndRepeatedTokens() {

        TokenFilter filter = createFilter();

        assertEquals("Value-of-C/Value-of-C",
                filter.filter("${TOKEN_C}/${TOKEN_C}"));
        assertEquals("xValue-of-Cyvalue-of-cz",
                filter.filter("x${TOKEN_C}y${TOKEN_C:LOWER}z"));

        // Directly adjacent tokens: only the first is substituted
        assertEquals("Value-of-C${TOKEN_C}",
                filter.filter("${TOKEN_C}${TOKEN_C}"));

    }

    /**
     * Verifies that an unterminated token is preserved literally, and that
     * matching such input completes promptly. Token patterns which admit
     * nested quantifiers can exhibit severe backtracking on input of this
     * shape, so this test bounds the time taken.
     */
    @Test(timeout = 5000)
    public void testUnterminatedTokenDoesNotBacktrackExcessively() {

        TokenFilter filter = createFilter();

        StringBuilder input = new StringBuilder("${");
        for (int i = 0; i < 32; i++)
            input.append("{a}");

        String value = input.toString();
        assertEquals(value, filter.filter(value));

    }

    /**
     * Verifies that a long run of opening braces following a token
     * introduction is handled promptly and preserved literally.
     */
    @Test(timeout = 5000)
    public void testUnbalancedBracesDoNotBacktrackExcessively() {

        TokenFilter filter = createFilter();

        StringBuilder input = new StringBuilder("${");
        for (int i = 0; i < 32; i++)
            input.append('{');

        String value = input.toString();
        assertEquals(value, filter.filter(value));

    }

}
