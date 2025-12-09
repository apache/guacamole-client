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

package org.apache.guacamole.log;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.slf4j.spi.MDCAdapter;

/**
 * MDCAdapter implementation that allows individual string context values to be
 * inherited by child threads. Deque context values (as manipulated with
 * {@link #pushByKey(java.lang.String, java.lang.String)}, etc.) are not
 * inherited.
 */
public class InheritableMDCAdapter implements MDCAdapter {

    /**
     * MDC context map that is automatically inherited by child threads.
     */
    private final InheritableThreadLocal<ForkableHashMap<String, String>> context =
            new InheritableThreadLocal<ForkableHashMap<String, String>>() {

        @Override
        protected ForkableHashMap<String, String> initialValue() {
            return new ForkableHashMap<>();
        }

        @Override
        protected ForkableHashMap<String, String> childValue(ForkableHashMap<String, String> parentValue) {
            // This function is invoked by the JVM for every thread created,
            // so simply creating a new copy is expensive. We instead create a
            // copy-on-write reference.
            return parentValue.fork();
        }

    };

    /**
     * MDC deque map that is NOT automatically inherited by child threads. We
     * choose not to inherit the deque map as that would result in pushed values
     * possibly never getting popped.
     * <p>
     * NOTE: The Deques have no relation to the single values maintained using
     * {@link #put(java.lang.String, java.lang.String)} and retrieved using
     * {@link #get(java.lang.String)} which are intentionally stored
     * separately in {@link #context}. This matches the behavior of the Logback
     * implementation of MDC.
     */
    private final ThreadLocal<Map<String, Deque<String>>> contextDeques =
            new ThreadLocal<Map<String, Deque<String>>>() {

        @Override
        protected Map<String, Deque<String>> initialValue() {
            return new HashMap<>();
        }

    };

    @Override
    public void put(String key, String value) {
        context.get().put(key, value);
    }

    @Override
    public String get(String key) {
        return context.get().get(key);
    }

    @Override
    public void remove(String key) {
        context.get().remove(key);
    }

    @Override
    public void clear() {
        context.get().clear();
    }

    @Override
    public Map<String, String> getCopyOfContextMap() {
        // This function is invoked by Logback for every log message produced,
        // so simply copying the context map is expensive. We instead create a
        // copy-on-write reference to the context map.
        return context.get().fork();
    }

    @Override
    public void setContextMap(Map<String, String> contextMap) {
        context.set(new ForkableHashMap<>(contextMap));
    }

    /**
     * Returns the Deque of pushed values associated with the given key, as
     * required for {@link #pushByKey(java.lang.String, java.lang.String)} and
     * {@link #popByKey(java.lang.String)}. If no Deque yet exists for the
     * given key, it is first created.
     *
     * @param key
     *     The key to retrieve the Deque for.
     *
     * @return
     *     The Deque of pushed values for the given key.
     */
    private Deque<String> getDeque(String key) {

        Deque<String> values = contextDeques.get().get(key);
        if (values == null) {
            values = new LinkedList<>();
            contextDeques.get().put(key, values);
        }

        return values;

    }

    @Override
    public void pushByKey(String key, String value) {
        getDeque(key).push(value);
    }

    @Override
    public String popByKey(String key) {
        return getDeque(key).pop();
    }

    @Override
    public Deque<String> getCopyOfDequeByKey(String key) {
        return new LinkedList<>(getDeque(key));
    }

    @Override
    public void clearDequeByKey(String key) {
        getDeque(key).clear();
    }

}
