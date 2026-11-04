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

package org.apache.guacamole.cluster.redis;

import io.lettuce.core.RedisNoScriptException;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.sync.RedisCommands;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * A Lua script, bundled with this module as a classpath resource, which is
 * executed inside the Redis server rather than as a series of individual
 * commands sent from Java.
 *
 * Some cluster operations must read and modify several keys as one indivisible
 * step. Acquiring connection seats, for example, must check every applicable
 * limit and then claim a seat under each of them; if those were separate
 * commands, two replicas could both pass the check before either claimed its
 * seat, exceeding the limit. Redis executes a script atomically, with no other
 * command interleaved, so performing such a check-and-set as a script makes it
 * safe across every replica in the cluster.
 *
 * The script text is read once, when the script is loaded, and is sent to the
 * server with SCRIPT LOAD the first time it is executed. The server returns the
 * script's SHA1 digest, which is cached and used to run the script with
 * EVALSHA thereafter, so the script body is not resent on every call. If the
 * server reports that it no longer knows the digest (NOSCRIPT, which happens
 * after a SCRIPT FLUSH, a server restart, or a failover to a server which never
 * loaded the script), the script is loaded again and the call is retried once.
 */
public class LuaScript {

    /**
     * The full source text of the Lua script.
     */
    private final String script;

    /**
     * The SHA1 digest under which the Redis server has cached this script, as
     * returned by SCRIPT LOAD, or null if the script has not yet been loaded
     * into the server. Volatile so that a digest obtained by one thread is
     * reused by others; a race merely loads the same script twice, which is
     * harmless as SCRIPT LOAD is idempotent.
     */
    private volatile String sha;

    /**
     * Creates a new LuaScript having the given source text. Instances are
     * obtained through load().
     *
     * @param script
     *     The full source text of the Lua script.
     */
    private LuaScript(String script) {
        this.script = script;
    }

    /**
     * Reads the Lua script at the given classpath location, returning a
     * LuaScript which can then be executed against Redis with eval(). This
     * only reads the script text into memory; nothing is sent to Redis until
     * the script is first executed. Scripts are loaded once, when the store
     * using them is created, so a missing or unreadable script is reported
     * immediately rather than on first use.
     *
     * @param resourcePath
     *     The absolute classpath location of the script, such as
     *     "/org/apache/guacamole/cluster/redis/acquire-seats.lua".
     *
     * @return
     *     A LuaScript containing the text of the script at the given
     *     location.
     *
     * @throws IllegalStateException
     *     If no resource exists at the given location.
     *
     * @throws UncheckedIOException
     *     If the resource exists but cannot be read.
     */
    public static LuaScript load(String resourcePath) {

        InputStream input = LuaScript.class.getResourceAsStream(resourcePath);
        if (input == null)
            throw new IllegalStateException("Missing Lua script: " + resourcePath);

        // Read the entire resource, decoding it as UTF-8
        try {

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];

            int read;
            while ((read = input.read(chunk)) != -1)
                buffer.write(chunk, 0, read);

            return new LuaScript(new String(buffer.toByteArray(), StandardCharsets.UTF_8));

        }
        catch (IOException e) {
            throw new UncheckedIOException("Unable to read Lua script: " + resourcePath, e);
        }
        finally {
            try {
                input.close();
            }
            catch (IOException e) {
                // Nothing useful can be done if closing fails
            }
        }

    }

    /**
     * Executes this script atomically within the Redis server, returning its
     * integer result. The script is loaded into the server with SCRIPT LOAD
     * if it has not been loaded already, then run with EVALSHA. If the server
     * has since discarded the script, it is loaded again and run once more.
     *
     * @param commands
     *     The synchronous Redis command interface to execute the script
     *     through.
     *
     * @param keys
     *     The Redis keys the script operates on, available to the script as
     *     its KEYS array.
     *
     * @param args
     *     The additional arguments to pass to the script, available to the
     *     script as its ARGV array.
     *
     * @return
     *     The integer returned by the script.
     *
     * @throws io.lettuce.core.RedisException
     *     If the script cannot be loaded or executed, including if Redis is
     *     unreachable.
     */
    public long eval(RedisCommands<String, String> commands, String[] keys, String[] args) {

        // Load the script into the server on first use, caching its digest
        String currentSha = sha;
        if (currentSha == null) {
            currentSha = commands.scriptLoad(script);
            sha = currentSha;
        }

        // Run the cached script by digest. NOSCRIPT means the server has lost
        // it (flush, restart, or failover), so load it again and retry once
        try {
            return commands.evalsha(currentSha, ScriptOutputType.INTEGER, keys, args);
        }
        catch (RedisNoScriptException e) {
            sha = commands.scriptLoad(script);
            return commands.evalsha(sha, ScriptOutputType.INTEGER, keys, args);
        }

    }

}
