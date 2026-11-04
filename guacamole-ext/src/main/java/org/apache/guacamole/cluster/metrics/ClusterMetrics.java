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


package org.apache.guacamole.cluster.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A registry of every metric the cluster implementation publishes, rendered
 * on request in the Prometheus text exposition format. Counters only ever
 * increase; gauges hold the most recently recorded value.
 *
 * Static rather than injected on purpose: the web application and the JDBC
 * extension build separate ClusterStore instances, and an injected registry
 * would give each of them a copy reporting half the truth. This class lives in
 * guacamole-ext, which every extension takes as "provided" and which therefore
 * exists exactly once in the deployed WAR, defined by a single classloader.
 */
public class ClusterMetrics {

    /**
     * Counter series, keyed by their fully rendered identity, for example
     * "guacamole_cluster_operation_failures_total{operation=\"getToken\"}".
     */
    private static final ConcurrentMap<String, AtomicLong> counters =
            new ConcurrentHashMap<String, AtomicLong>();

    /**
     * Gauge values, keyed the same way.
     */
    private static final ConcurrentMap<String, Double> gauges =
            new ConcurrentHashMap<String, Double>();

    /**
     * The metric name of each series, keyed by that series' identity, so that
     * the TYPE line can be emitted once per name.
     */
    private static final ConcurrentMap<String, String> names =
            new ConcurrentHashMap<String, String>();

    /**
     * Increments an unlabelled counter.
     *
     * @param name
     *     The metric name.
     */
    public static void counter(String name) {
        increment(name, name);
    }

    /**
     * Increments a counter carrying one label.
     *
     * @param name
     *     The metric name.
     *
     * @param labelName
     *     The label's name.
     *
     * @param labelValue
     *     The label's value, which may contain characters needing escaping.
     */
    public static void counter(String name, String labelName, String labelValue) {
        increment(name, name + "{" + labelName + "=\"" + escape(labelValue) + "\"}");
    }

    /**
     * Records the current value of an unlabelled gauge.
     *
     * @param name
     *     The metric name.
     *
     * @param value
     *     The value as of now.
     */
    public static void gauge(String name, double value) {
        names.put(name, name);
        gauges.put(name, Double.valueOf(value));
    }

    /**
     * Discards every recorded series. Intended only for tests.
     */
    public static void reset() {
        counters.clear();
        gauges.clear();
        names.clear();
    }

    /**
     * Renders every recorded series in the Prometheus text exposition format.
     *
     * @return
     *     The whole registry, or the empty string when nothing has been
     *     recorded.
     */
    public static String render() {

        StringBuilder text = new StringBuilder();

        // Sort the series so that output is stable and series sharing a
        // metric name are rendered together under one TYPE line
        List<String> counterKeys = new ArrayList<String>(counters.keySet());
        Collections.sort(counterKeys);

        List<String> gaugeKeys = new ArrayList<String>(gauges.keySet());
        Collections.sort(gaugeKeys);

        // Render counters, preceding each new metric name with its TYPE line
        String lastName = null;
        for (String key : counterKeys) {
            String name = names.get(key);
            if (!name.equals(lastName)) {
                text.append("# TYPE ").append(name).append(" counter\n");
                lastName = name;
            }
            text.append(key).append(' ').append(counters.get(key).get()).append('\n');
        }

        // Render gauges in the same way
        lastName = null;
        for (String key : gaugeKeys) {
            String name = names.get(key);
            if (!name.equals(lastName)) {
                text.append("# TYPE ").append(name).append(" gauge\n");
                lastName = name;
            }
            text.append(key).append(' ')
                    .append(format(gauges.get(key).doubleValue())).append('\n');
        }

        return text.toString();

    }

    /**
     * Increments the counter with the given identity, creating it if this is
     * its first occurrence.
     *
     * @param name
     *     The metric name, used for the TYPE line.
     *
     * @param key
     *     The fully rendered series identity.
     */
    private static void increment(String name, String key) {

        // Create the series on first use. putIfAbsent() guarantees that two
        // threads racing to create the same series end up sharing one
        // counter, so no increment is lost
        AtomicLong counter = counters.get(key);
        if (counter == null) {
            counters.putIfAbsent(key, new AtomicLong());
            names.put(key, name);
            counter = counters.get(key);
        }

        counter.incrementAndGet();

    }

    /**
     * Escapes a label value for the text exposition format.
     *
     * @param value
     *     The raw label value.
     *
     * @return
     *     The value with backslashes, quotes and newlines escaped.
     */
    private static String escape(String value) {
        if (value == null)
            return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    /**
     * Renders a gauge value without a trailing ".0" for whole numbers, which
     * keeps a 0/1 gauge readable.
     *
     * @param value
     *     The value to render.
     *
     * @return
     *     The value as text.
     */
    private static String format(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value))
            return Long.toString((long) value);
        return Double.toString(value);
    }

}
