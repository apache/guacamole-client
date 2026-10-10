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


package org.apache.guacamole.auth.jdbc.reset;

import java.util.Date;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for password reset tokens. Every query is keyed by the token's
 * SHA-256 hash, never by the token itself.
 */
public interface PasswordResetTokenMapper {

    /**
     * Stores the given token.
     *
     * @param token
     *     The token to store.
     *
     * @return
     *     The number of rows inserted.
     */
    int insert(@Param("token") PasswordResetTokenModel token);

    /**
     * Returns the token having the given hash if it is unused and has not
     * expired as of the given time.
     *
     * @param tokenHash
     *     The SHA-256 hash of the token.
     *
     * @param now
     *     The current time.
     *
     * @return
     *     The matching valid token, or null if there is none.
     */
    PasswordResetTokenModel selectValid(@Param("tokenHash") byte[] tokenHash,
            @Param("now") Date now);

    /**
     * Marks the token having the given hash as used, only if it is still
     * unused and unexpired. This is the single-use guarantee: of two
     * concurrent redemptions, exactly one sees a count of 1.
     *
     * @param tokenHash
     *     The SHA-256 hash of the token.
     *
     * @param now
     *     The current time.
     *
     * @return
     *     1 if the token was consumed, 0 if it was not valid.
     */
    int consume(@Param("tokenHash") byte[] tokenHash, @Param("now") Date now);

    /**
     * Marks every unused token of the given user as used, so that issuing a
     * new token invalidates any older one.
     *
     * @param userID
     *     The ID of the user.
     *
     * @param now
     *     The current time.
     *
     * @return
     *     The number of tokens invalidated.
     */
    int invalidateUnused(@Param("userID") Integer userID, @Param("now") Date now);

    /**
     * Counts the tokens issued to the given user since the given time,
     * whether or not they were used. This is the per-user rate limit.
     *
     * @param userID
     *     The ID of the user.
     *
     * @param since
     *     The start of the counting window.
     *
     * @return
     *     The number of tokens issued within the window.
     */
    int countIssuedSince(@Param("userID") Integer userID, @Param("since") Date since);

    /**
     * Deletes every token issued before the given time, keeping the table
     * small. Tokens this old are expired and outside the rate limit window.
     *
     * @param before
     *     The cutoff time.
     *
     * @return
     *     The number of tokens deleted.
     */
    int deleteIssuedBefore(@Param("before") Date before);

}
