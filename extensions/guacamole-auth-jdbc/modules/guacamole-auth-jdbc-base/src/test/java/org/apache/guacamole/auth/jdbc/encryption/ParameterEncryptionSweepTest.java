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

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

public class ParameterEncryptionSweepTest {

    static ParameterEncryptionService svc(String current, boolean decrypt, String... kids) throws Exception {
        return new ParameterEncryptionService(
            ParameterEncryptionServiceTest.provider(current, kids),
            ParameterEncryptionServiceTest.policy(), decrypt);
    }

    @Test
    public void encryptsPlainSecretsOnly() throws Exception {
        InMemoryParameterEncryptionMapper db = new InMemoryParameterEncryptionMapper();
        db.rows.add(InMemoryParameterEncryptionMapper.row("1", "password", "a", null));
        db.rows.add(InMemoryParameterEncryptionMapper.row("1", "hostname", "h", null));
        ParameterEncryptionService s = svc("k1", false, "k1");
        ParameterEncryptionSweep.SweepResult r = new ParameterEncryptionSweep(s, db).run();
        assertEquals(1, r.encrypted);
        assertEquals(0, r.remaining);
        assertEquals("k1", db.rows.get(0).getEncryption());
        assertEquals("a", s.reveal(ParameterOwner.CONNECTION, "1", "password", db.rows.get(0).getValue(), "k1"));
        assertNull(db.rows.get(1).getEncryption());
        assertEquals("h", db.rows.get(1).getValue());
    }

    @Test
    public void rekeysOnRotation() throws Exception {
        InMemoryParameterEncryptionMapper db = new InMemoryParameterEncryptionMapper();
        db.rows.add(InMemoryParameterEncryptionMapper.row("1", "password", "a", null));
        new ParameterEncryptionSweep(svc("k1", false, "k1"), db).run();
        ParameterEncryptionService rotated = svc("k2", false, "k1", "k2");
        ParameterEncryptionSweep.SweepResult r = new ParameterEncryptionSweep(rotated, db).run();
        assertEquals(1, r.rekeyed);
        assertEquals("k2", db.rows.get(0).getEncryption());
        assertEquals("a", rotated.reveal(ParameterOwner.CONNECTION, "1", "password", db.rows.get(0).getValue(), "k2"));
    }

    @Test
    public void decryptModeRestoresPlainText() throws Exception {
        InMemoryParameterEncryptionMapper db = new InMemoryParameterEncryptionMapper();
        db.rows.add(InMemoryParameterEncryptionMapper.row("1", "password", "a", null));
        new ParameterEncryptionSweep(svc("k1", false, "k1"), db).run();
        ParameterEncryptionSweep.SweepResult r = new ParameterEncryptionSweep(svc("k1", true, "k1"), db).run();
        assertEquals(1, r.decrypted);
        assertNull(db.rows.get(0).getEncryption());
        assertEquals("a", db.rows.get(0).getValue());
    }

    @Test
    public void undecryptableRowIsCountedAndSkipped() throws Exception {
        InMemoryParameterEncryptionMapper db = new InMemoryParameterEncryptionMapper();
        db.rows.add(InMemoryParameterEncryptionMapper.row("1", "password", "garbage", "gone"));
        db.rows.add(InMemoryParameterEncryptionMapper.row("2", "password", "b", null));
        ParameterEncryptionSweep.SweepResult r = new ParameterEncryptionSweep(svc("k1", false, "k1"), db).run();
        assertEquals(1, r.failed);
        assertEquals(1, r.encrypted);
        assertEquals("garbage", db.rows.get(0).getValue());
    }

    @Test
    public void concurrentSweepsEncryptEachRowOnce() throws Exception {
        InMemoryParameterEncryptionMapper db = new InMemoryParameterEncryptionMapper();
        for (int i = 0; i < 500; i++)
            db.rows.add(InMemoryParameterEncryptionMapper.row(Integer.toString(i), "password", "v" + i, null));
        ParameterEncryptionService s = svc("k1", false, "k1");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<ParameterEncryptionSweep.SweepResult> a = pool.submit(() -> { start.await(); return new ParameterEncryptionSweep(s, db).run(); });
        Future<ParameterEncryptionSweep.SweepResult> b = pool.submit(() -> { start.await(); return new ParameterEncryptionSweep(s, db).run(); });
        start.countDown();
        int total = a.get().encrypted + b.get().encrypted;
        pool.shutdown();
        assertEquals(500, total, "each row encrypted exactly once across both sweeps");
        for (int i = 0; i < 500; i++)
            assertEquals("v" + i, s.reveal(ParameterOwner.CONNECTION, Integer.toString(i), "password",
                db.rows.get(i).getValue(), "k1"));
    }

    @Test
    public void adminWriteDuringSweepWins() throws Exception {
        InMemoryParameterEncryptionMapper db = new InMemoryParameterEncryptionMapper();
        db.rows.add(InMemoryParameterEncryptionMapper.row("1", "password", "old", null));
        ParameterEncryptionService s = svc("k1", false, "k1");
        String adminValue = s.encryptValue(ParameterOwner.CONNECTION, "1", "password", "new");
        db.beforeUpdate = () -> { db.rows.get(0).setValue(adminValue); db.rows.get(0).setEncryption("k1"); };
        ParameterEncryptionSweep.SweepResult r = new ParameterEncryptionSweep(s, db).run();
        assertEquals(1, r.skipped);
        assertEquals("new", s.reveal(ParameterOwner.CONNECTION, "1", "password", db.rows.get(0).getValue(), "k1"));
    }

}
