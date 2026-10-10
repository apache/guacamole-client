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
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts the parameter encryption sweep once, in the background, when the
 * extension loads. When encryption is not configured, only counts encrypted
 * rows and reports them at ERROR. Never delays startup. Bound
 * as an eager singleton, so an invalid encryption configuration fails the
 * extension at load rather than on first use.
 */
public class SweepStarter {

    private static final Logger logger = LoggerFactory.getLogger(SweepStarter.class);

    @Inject
    public SweepStarter(ParameterEncryptionService service,
            ParameterEncryptionSweep sweep, SqlSessionManager sessions) {

        Thread thread = new Thread(() -> {

            // A SIMPLE executor makes each update return its real row count
            // (the default BATCH executor does not); auto-commit per statement.
            try {
                sessions.startManagedSession(ExecutorType.SIMPLE, true);
                try {
                    ParameterEncryptionSweep.SweepResult result = sweep.run();
                    if (!service.isEnabled()) {
                        if (result.remainingEncrypted > 0)
                            logger.error("{} stored parameters are encrypted but "
                                    + "parameter encryption is not configured; those "
                                    + "connections cannot be used.", result.remainingEncrypted);
                    }
                    else if (service.isDecryptMode())
                        logger.warn("Parameter encryption sweep finished: {}. "
                                + "Downgrading to a version without parameter "
                                + "encryption is safe only when remaining encrypted "
                                + "is 0 and no other node runs in encrypt mode.", result);
                    else
                        logger.info("Parameter encryption sweep finished: {}.", result);
                }
                finally {
                    sessions.close();
                }
            }
            catch (RuntimeException e) {
                // Class only: a driver message can quote the failing row.
                logger.error("Parameter encryption sweep failed: {}", e.getClass().getName());
            }

        }, "parameter-encryption-sweep");
        thread.setDaemon(true);
        thread.start();

    }

}
