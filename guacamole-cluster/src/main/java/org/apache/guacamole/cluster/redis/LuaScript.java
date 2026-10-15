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
 * A Lua script loaded from the classpath and executed by SHA, falling back to
 * a full EVAL if the server has forgotten the script (which happens after a
 * SCRIPT FLUSH or a failover to a replica that never cached it).
 */
public class LuaScript {

    private final String script;
    private volatile String sha;

    private LuaScript(String script) {
        this.script = script;
    }

    /**
     * Loads a script from the classpath.
     *
     * @param resourcePath
     *     Absolute classpath location of the script.
     *
     * @return
     *     The loaded script.
     */
    public static LuaScript load(String resourcePath) {

        InputStream input = LuaScript.class.getResourceAsStream(resourcePath);
        if (input == null)
            throw new IllegalStateException("Missing Lua script: " + resourcePath);

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
     * Executes this script, returning its integer result.
     *
     * @param commands
     *     Synchronous Redis command interface.
     *
     * @param keys
     *     The KEYS array.
     *
     * @param args
     *     The ARGV array.
     *
     * @return
     *     The integer returned by the script.
     */
    public long eval(RedisCommands<String, String> commands, String[] keys, String[] args) {

        String currentSha = sha;
        if (currentSha == null) {
            currentSha = commands.scriptLoad(script);
            sha = currentSha;
        }

        try {
            return commands.evalsha(currentSha, ScriptOutputType.INTEGER, keys, args);
        }
        catch (RedisNoScriptException e) {
            sha = commands.scriptLoad(script);
            return commands.evalsha(sha, ScriptOutputType.INTEGER, keys, args);
        }

    }

}
