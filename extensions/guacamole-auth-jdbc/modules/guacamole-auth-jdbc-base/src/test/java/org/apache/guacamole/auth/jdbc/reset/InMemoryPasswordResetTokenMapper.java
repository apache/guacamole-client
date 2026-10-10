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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * PasswordResetTokenMapper which mirrors the SQL of the real mappers over an
 * in-memory list.
 */
class InMemoryPasswordResetTokenMapper implements PasswordResetTokenMapper {

    final List<PasswordResetTokenModel> rows = new ArrayList<>();

    private PasswordResetTokenModel validRow(byte[] tokenHash, Date now) {
        for (PasswordResetTokenModel row : rows)
            if (Arrays.equals(row.getTokenHash(), tokenHash)
                    && row.getUsed() == null && row.getExpires().after(now))
                return row;
        return null;
    }

    @Override
    public int insert(PasswordResetTokenModel token) {
        rows.add(token);
        return 1;
    }

    @Override
    public PasswordResetTokenModel selectValid(byte[] tokenHash, Date now) {
        PasswordResetTokenModel row = validRow(tokenHash, now);
        if (row != null)
            row.setUsername("user" + row.getUserID());
        return row;
    }

    @Override
    public int consume(byte[] tokenHash, Date now) {
        PasswordResetTokenModel row = validRow(tokenHash, now);
        if (row == null)
            return 0;
        row.setUsed(now);
        return 1;
    }

    @Override
    public int invalidateUnused(Integer userID, Date now) {
        int count = 0;
        for (PasswordResetTokenModel row : rows)
            if (row.getUserID().equals(userID) && row.getUsed() == null) {
                row.setUsed(now);
                count++;
            }
        return count;
    }

    @Override
    public int countIssuedSince(Integer userID, Date since) {
        int count = 0;
        for (PasswordResetTokenModel row : rows)
            if (row.getUserID().equals(userID) && row.getCreated().after(since))
                count++;
        return count;
    }

    @Override
    public int deleteIssuedBefore(Date before) {
        int count = 0;
        for (Iterator<PasswordResetTokenModel> i = rows.iterator(); i.hasNext();)
            if (i.next().getCreated().before(before)) {
                i.remove();
                count++;
            }
        return count;
    }

}
