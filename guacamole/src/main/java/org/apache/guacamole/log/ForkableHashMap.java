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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * HashMap implementation that uses copy-on-write to allow efficient cloning.
 * When an instance of ForkableHashMap has been copied using {@link #fork()},
 * the parent and child both continue to refer to the same copy of their
 * underlying data. If the parent (or child) is modified, only then does the
 * parent (or child) create an independent copy of that underlying data.
 * <p>
 * For simplicity, this implementation DOES NOT permit modification via the
 * collections/sets returned by {@link #values()}, {@link #keySet()}, or
 * {@link #entrySet()}.
 * <p>
 * As with the standard HashMap, this implementation IS NOT threadsafe.
 *
 * @param <K>
 *     The type of key used to store/retrieve values within this map.
 *
 * @param <V>
 *     The type of values stored by this map.
 */
public class ForkableHashMap<K, V> implements Map<K, V> {

    /**
     * The underlying content of this ForkableHashMap. This content is shared
     * with at least one other ForkableHashMap if {@link #shared} is true, and
     * is an independent copy that may safely be modified if {@link #shared} is
     * false.
     */
    private Map<K, V> content;

    /**
     * Whether the Map referenced by {@link #content} is shared with at least
     * one other ForkableHashMap.
     */
    private boolean shared;

    /**
     * Creates a new, empty ForkableHashMap.
     */
    public ForkableHashMap() {
        this.content = new HashMap<>();
        this.shared = false;
    }

    /**
     * Creates a new ForkableHashMap that contains an independent copy of the
     * entries in the provided map.
     * <p>
     * This is not a deep copy; only the references to keys and values are
     * copied.
     *
     * @param map
     *     The map to copy.
     */
    public ForkableHashMap(Map<K, V> map) {
        this.content = new HashMap<>(map);
        this.shared = false;
    }

    /**
     * Creates a new ForkableHashMap that shares the content of the given
     * ForkableHashMap. It is the responsibility of the caller to ensure that
     * the given ForkableHashMap has its content marked as shared.
     *
     * @param parent
     *     The ForkableHashMap to lazily copy.
     */
    private ForkableHashMap(ForkableHashMap<K, V> parent) {
        this.content = parent.content;
        this.shared = true;
    }

    /**
     * Creates a new ForkableHashMap that shares the content of this map. No
     * actual copy operation will be performed until this map or the new map
     * are modified.
     *
     * @return
     *     A new ForkableHashMap that lazily shares the content of this map.
     */
    public ForkableHashMap<K, V> fork() {
        shared = true;
        return new ForkableHashMap<>(this);
    }

    /**
     * Returns the map providing content for this ForkableHashMap while
     * ensuring that map is safe to modify. If the map is shared with another
     * ForkableHashMap, the map is first replaced with an independent copy.
     *
     * @return
     *     The map providing content for this ForkableHashMap, which shall be
     *     safe to modify after this function returns.
     */
    private Map<K, V> writableContent() {

        // Transform into independent copy if other instances of ForkableHashMap
        // share the same underlying data
        if (shared) {
            content = new HashMap<>(content);
            shared = false;
        }

        return content;

    }

    @Override
    public int size() {
        return content.size();
    }

    @Override
    public boolean isEmpty() {
        return content.isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
        return content.containsKey(o);
    }

    @Override
    public boolean containsValue(Object o) {
        return content.containsValue(o);
    }

    @Override
    public V get(Object o) {
        return content.get(o);
    }

    @Override
    public V put(K k, V v) {
        return writableContent().put(k, v);
    }

    @Override
    public V remove(Object o) {
        return writableContent().remove(o);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        writableContent().putAll(map);
    }

    @Override
    public void clear() {
        writableContent().clear();
    }

    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(content.keySet());
    }

    @Override
    public Collection<V> values() {
        return Collections.unmodifiableCollection(content.values());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(content.entrySet());
    }

    @Override
    public boolean equals(Object o) {
        return content.equals(o);
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return content.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        content.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        writableContent().replaceAll(function);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return writableContent().putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return writableContent().remove(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return writableContent().replace(key, oldValue, newValue);
    }

    @Override
    public V replace(K key, V value) {
        return writableContent().replace(key, value);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return content.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return content.computeIfPresent(key, remappingFunction);
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return content.compute(key, remappingFunction);
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return writableContent().merge(key, value, remappingFunction);
    }

    @Override
    public String toString() {
        return content.toString();
    }

}
