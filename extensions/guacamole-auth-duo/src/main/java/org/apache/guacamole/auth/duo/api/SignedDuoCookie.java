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

package org.apache.guacamole.auth.duo.api;

import com.google.common.io.BaseEncoding;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;

/**
 * A DuoCookie which is cryptographically signed with a provided key using
 * HMAC-SHA1.
 */
public class SignedDuoCookie extends DuoCookie {

    /**
     * Pattern which matches valid signed cookies. Like unsigned cookies, each
     * signed cookie is made up of three sections, separated from each other by
     * pipe symbols ("|").
     */
    private static final Pattern SIGNED_COOKIE_FORMAT = Pattern.compile("([^|]+)\\|([^|]+)\\|([0-9a-f]+)");

    /**
     * The index of the capturing group within SIGNED_COOKIE_FORMAT which
     * contains the cookie type prefix.
     */
    private static final int PREFIX_GROUP = 1;

    /**
     * The index of the capturing group within SIGNED_COOKIE_FORMAT which
     * contains the cookie's base64-encoded data.
     */
    private static final int DATA_GROUP = 2;

    /**
     * The index of the capturing group within SIGNED_COOKIE_FORMAT which
     * contains the signature.
     */
    private static final int SIGNATURE_GROUP = 3;

    /**
     * The signature algorithm that should be used to sign the cookie, as
     * defined by:
     * http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Mac
     */
    private static final String SIGNATURE_ALGORITHM = "HmacSHA1";

    /**
     * The type of a signed Duo cookie. Each signed Duo cookie has an
     * associated type which determines the prefix included in the string
     * representation of that cookie. As that type is included in the data
     * that is signed, different types will result in different signatures,
     * even if the data portion of the cookie is otherwise identical.
     */
    public enum Type {

        /**
         * A Duo cookie which has been signed with the secret key for inclusion
         * in a Duo request.
         */
        DUO_REQUEST("TX"),

        /**
         * A Duo cookie which has been signed with the secret key by Duo and
         * was included in a Duo response.
         */
        DUO_RESPONSE("AUTH"),

        /**
         * A Duo cookie which has been signed with the application key for
         * inclusion in a Duo request. Such cookies are also included in Duo
         * responses, for verification by the application.
         */
        APPLICATION("APP");

        /**
         * The prefix associated with the Duo cookie type. This prefix will
         * be included in the string representation of the cookie.
         */
        private final String prefix;

        /**
         * Creates a new Duo cookie type associated with the given string
         * prefix. This prefix will be included in the string representation of
         * the cookie.
         *
         * @param prefix
         *     The prefix to associated with the Duo cookie type.
         */
        Type(String prefix) {
            this.prefix = prefix;
        }

        /**
         * Returns the prefix associated with the Duo cookie type.
         *
         * @return
         *     The prefix to associated with this Duo cookie type.
         */
        public String getPrefix() {
            return prefix;
        }

        /**
         * Returns the cookie type associated with the given prefix. If no such
         * cookie type exists, null is returned.
         *
         * @param prefix
         *     The prefix of the cookie type to search for.
         *
         * @return
         *     The cookie type associated with the given prefix, or null if no
         *     such cookie type exists.
         */
        public static Type fromPrefix(String prefix) {

            // Search through all defined cookie types for the given prefix
            for (Type type : Type.values()) {
                if (type.getPrefix().equals(prefix))
                    return type;
            }

            // No such cookie type exists
            return null;

        }

    }

    /**
     * The type of this Duo cookie.
     */
    private final Type type;

    /**
     * The signature produced when the cookie was signed with HMAC-SHA1. The
     * signature covers the prefix of the type and the cookie's base64-encoded
     * data, separated by a pipe symbol.
     */
    private final String signature;

    /**
     * Creates a new SignedDuoCookie which describes the identity of a user
     * being verified and is cryptographically signed with HMAC-SHA1 by a given
     * key.
     *
     * @param cookie
     *     The cookie defining the identity being verified.
     *
     * @param type
     *     The type of the cookie being created.
     *
     * @param key
     *     The key to use to generate the cryptographic signature. This key
     *     will not be stored within the cookie.
     *
     * @throws GuacamoleException
     *     If the given signing key is invalid.
     */
    public SignedDuoCookie(DuoCookie cookie, Type type, String key)
            throws GuacamoleException {

        // Init underlying cookie
        super(cookie.getUsername(), cookie.getIntegrationKey(),
                cookie.getExpirationTimestamp());

        // Store cookie type and signature
        this.type = type;
        this.signature = sign(key, type.getPrefix() + "|" + cookie.toString());

    }

    /**
     * Signs the given arbitrary string data with the given key using the
     * algorithm defined by SIGNATURE_ALGORITHM. Both the data and the key will
     * be interpreted as UTF-8 bytes.
     *
     * @param key
     *     The key which should be used to sign the given data.
     *
     * @param data
     *     The data being signed.
     *
     * @return
     *     The signature produced by signing the given data with the given key,
     *     encoded as lowercase hexadecimal.
     *
     * @throws GuacamoleException
     *     If the given signing key is invalid.
     */
    private static String sign(String key, String data) throws GuacamoleException {

        try {

            // Attempt to sign UTF-8 bytes of provided data
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            mac.init(new SecretKeySpec(key.getBytes("UTF-8"), SIGNATURE_ALGORITHM));

            // Return signature as hex
            return BaseEncoding.base16().lowerCase().encode(mac.doFinal(data.getBytes("UTF-8")));

        }

        // Re-throw any errors which prevent signature
        catch (InvalidKeyException e){
            throw new GuacamoleServerException("Signing key is invalid.", e);
        }

        // Throw hard errors if standard pieces of Java are missing
        catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException("Unexpected lack of UTF-8 support.", e);
        }
        catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException("Unexpected lack of support "
                    + "for required signature algorithm "
                    + "\"" + SIGNATURE_ALGORITHM + "\".", e);
        }

    }

    /**
     * Returns the type of this Duo cookie. The Duo cookie type is dictated
     * by the context of the cookie's use, and is included with the cookie's
     * underlying data when generating the signature.
     *
     * @return
     *     The type of this Duo cookie.
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the signature produced when the cookie was signed with HMAC-SHA1.
     * The signature covers the prefix of the cookie's type and the cookie's
     * base64-encoded data, separated by a pipe symbol.
     *
     * @return
     *     The signature produced when the cookie was signed with HMAC-SHA1.
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Parses a signed Duo cookie string, such as that produced by the
     * toString() function or received from the Duo service, producing a new
     * SignedDuoCookie object containing the associated cookie data and
     * signature. If the given string is not a valid Duo cookie, or if the
     * signature is incorrect, an exception is thrown. Note that the cookie may
     * be expired, and must be checked for expiration prior to actual use.
     *
     * @param key
     *     The key that was used to sign the Duo cookie.
     *
     * @param str
     *     The Duo cookie string to parse.
     *
     * @return
     *     A new SignedDuoCookie object containing the same data and signature
     *     as the given Duo cookie string.
     *
     * @throws GuacamoleException
     *     If the given string is not a valid Duo cookie string, or if the
     *     signature of the cookie is invalid.
     */
    public static SignedDuoCookie parseSignedDuoCookie(String key, String str)
            throws GuacamoleException {

        // Verify format of provided data
        Matcher matcher = SIGNED_COOKIE_FORMAT.matcher(str);
        if (!matcher.matches())
            throw new GuacamoleClientException("Format of signed Duo cookie "
                    + "is invalid.");

        // Parse type from prefix
        Type type = Type.fromPrefix(matcher.group(PREFIX_GROUP));
        if (type == null)
            throw new GuacamoleClientException("Invalid Duo cookie prefix.");

        // Parse cookie from base64-encoded data
        DuoCookie cookie = DuoCookie.parseDuoCookie(matcher.group(DATA_GROUP));

        // Verify signature of cookie
        SignedDuoCookie signedCookie = new SignedDuoCookie(cookie, type, key);
        if (!signedCookie.getSignature().equals(matcher.group(SIGNATURE_GROUP)))
            throw new GuacamoleClientException("Duo cookie has incorrect signature.");

        // Cookie has valid signature and has parsed successfully
        return signedCookie;

    }

    /**
     * Returns the string representation of this SignedDuoCookie. The format
     * used is identical to that required by the Duo service: the type prefix,
     * base64-encoded cookie data, and HMAC-SHA1 signature separated by pipe
     * symbols ("|").
     *
     * @return
     *     The string representation of this SignedDuoCookie.
     */
    @Override
    public String toString() {
        return type.getPrefix() + "|" + super.toString() + "|" + signature;
    }

}
