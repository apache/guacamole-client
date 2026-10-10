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

package org.apache.guacamole.auth.jdbc.encryption;

import com.google.inject.Inject;
import java.util.List;
import java.util.Set;
import org.apache.guacamole.GuacamoleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Brings stored parameters in line with the configured encryption: encrypts
 * plain-text secrets, re-keys rows on non-current keys, or (decrypt mode)
 * restores plain text. Each row is written with a compare-and-set, so
 * concurrent sweeps on several nodes, or an administrator saving mid-sweep,
 * are safe without a database lock.
 */
public class ParameterEncryptionSweep {

    private static final Logger logger = LoggerFactory.getLogger(ParameterEncryptionSweep.class);

    /** Rows handled between progress log lines. */
    public static final int BATCH_SIZE = 100;

    /** Counts produced by one sweep. */
    public static class SweepResult {
        public int encrypted, rekeyed, decrypted, skipped, failed, remaining, remainingEncrypted;
        @Override
        public String toString() {
            return "encrypted " + encrypted + ", re-keyed " + rekeyed
                    + ", decrypted " + decrypted + ", skipped " + skipped
                    + ", failed " + failed + ", remaining plain-text secrets "
                    + remaining;
        }
    }

    private final ParameterEncryptionService service;
    private final ParameterEncryptionMapper mapper;

    @Inject
    public ParameterEncryptionSweep(ParameterEncryptionService service,
            ParameterEncryptionMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    public SweepResult run() {
        SweepResult result = new SweepResult();
        if (!service.isEnabled()) {
            result.remainingEncrypted = countEncrypted(null);
            return result;
        }
        String kid = service.currentKeyId();
        boolean decrypt = service.isDecryptMode();
        Set<String> names = service.getPolicy().getSecretNames();
        sweep(ParameterOwner.CONNECTION,
                mapper.selectConnectionCandidates(names, kid, decrypt), result);
        sweep(ParameterOwner.SHARING_PROFILE,
                mapper.selectSharingProfileCandidates(names, kid, decrypt), result);
        if (!decrypt)
            result.remaining = countPlain(mapper.selectConnectionCandidates(names, kid, false))
                    + countPlain(mapper.selectSharingProfileCandidates(names, kid, false));
        return result;
    }

    /** Counts encrypted rows in both tables, optionally excluding one key ID. */
    private int countEncrypted(String otherThanKeyId) {
        return mapper.countConnectionEncrypted(otherThanKeyId)
                + mapper.countSharingProfileEncrypted(otherThanKeyId);
    }

    /** Counts the rows still stored as plain text. */
    private static int countPlain(List<ParameterRow> rows) {
        int plain = 0;
        for (ParameterRow row : rows)
            if (row.getEncryption() == null)
                plain++;
        return plain;
    }

    private void sweep(ParameterOwner owner, List<ParameterRow> rows, SweepResult result) {
        int handled = 0;
        for (ParameterRow row : rows) {
            // Empty values are never encrypted (see shouldEncrypt); the query already excludes them.
            if (!service.isDecryptMode() && row.getValue().isEmpty())
                continue;
            try {
                String plain = service.reveal(owner, row.getOwnerIdentifier(),
                        row.getName(), row.getValue(), row.getEncryption());
                String newValue;
                String newKid;
                if (service.isDecryptMode()) {
                    newValue = plain;
                    newKid = null;
                }
                else {
                    newValue = service.encryptValue(owner, row.getOwnerIdentifier(), row.getName(), plain);
                    newKid = service.currentKeyId();
                }
                int updated = owner == ParameterOwner.CONNECTION
                        ? mapper.updateConnectionParameter(row, newValue, newKid)
                        : mapper.updateSharingProfileParameter(row, newValue, newKid);
                if (updated == 0)
                    result.skipped++;
                else if (service.isDecryptMode())
                    result.decrypted++;
                else if (row.getEncryption() == null)
                    result.encrypted++;
                else
                    result.rekeyed++;
            }
            catch (GuacamoleException e) {
                result.failed++;
                logger.warn("Parameter encryption sweep skipped a row: {}", e.getMessage());
            }
            if (++handled % BATCH_SIZE == 0)
                logger.info("Parameter encryption sweep: {} {} rows processed.", handled, owner.getType());
        }
    }

}
