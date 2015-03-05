/*
 * Copyright (C) 2013 Glyptodon LLC
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

package org.glyptodon.guacamole.auth.jdbc.security;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.DatatypeConverter;

/**
 * Provides a SHA-256 based implementation of the password encryption functionality.
 * @author James Muehlner
 */
public class SHA256PasswordEncryptionService implements PasswordEncryptionService {

    @Override
    public byte[] createPasswordHash(String password, byte[] salt) {

        try {

            // Build salted password, if a salt was provided
            StringBuilder builder = new StringBuilder();
            builder.append(password);

            if (salt != null)
                builder.append(DatatypeConverter.printHexBinary(salt));

            // Hash UTF-8 bytes of possibly-salted password
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(builder.toString().getBytes("UTF-8"));
            return md.digest();

        }

        // Throw hard errors if standard pieces of Java are missing
        catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException("Unexpected lack of UTF-8 support.", e);
        }
        catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException("Unexpected lack of SHA-256 support.", e);
        }

    }

}
