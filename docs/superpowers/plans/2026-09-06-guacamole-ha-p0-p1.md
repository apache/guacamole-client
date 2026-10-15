# Guacamole HA Clustering — P0 + P1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the `guacamole-cluster` coordination module and wire it into `guacd` endpoint selection, so one Guacamole deployment can run multiple web application replicas against a pool of `guacd` instances with working cross-replica session join.

**Architecture:** A new `guacamole-cluster` Maven module holds a `ClusterStore` interface with a Lettuce-backed Redis implementation. Cluster state lives in Redis sorted sets scored by heartbeat timestamp, so entries belonging to a dead replica age out automatically. Kubernetes owns `guacd` membership via a headless Service; Redis owns state. P0 builds and tests the store in isolation with nothing wired in. P1 wires `guacd` selection and the route map into `AbstractGuacamoleTunnelService`, which is the first shippable outcome.

**Tech Stack:** Java 8, Maven, Guice 5.1.0, Lettuce 6.3.2.RELEASE (Redis client), Redis 7.x, JUnit 5.14.4, Testcontainers 1.19.8, Docker.

**Spec:** `docs/superpowers/specs/2026-09-06-guacamole-ha-clustering-design.md`

## Global Constraints

- **Java 8 source and target.** `pom.xml:259-260` sets `<source>1.8</source>` / `<target>1.8</target>`. No `var`, no records, no text blocks, no `List.of`, no `Map.of`. Use `Arrays.asList`, `new HashMap<>()`, and explicit types.
- **Apache RAT plugin is active** (`pom.xml:111`). Every new file — `.java`, `.xml`, `.lua`, `.yaml`, `.md` — must carry the Apache Software Foundation license header or the build fails. Copy the header verbatim from `guacamole-ext/pom.xml:2-19` (XML form) or from the top of any existing `.java` file in the repository.
- **Redis 7.0 or later is required.** The seat script calls `redis.call('TIME')` and then writes. Redis 5+ uses effects replication, which makes this safe; Redis 7 is the tested floor.
- **Module version is `${revision}`**, inherited from the parent (`pom.xml:38`, currently `1.6.1`). Never hardcode a version in a new `pom.xml`.
- **`cluster-enabled` defaults to `false`.** Every patched code path must behave identically to upstream when clustering is off. This is verified by an explicit test in Task 8.
- **No new state may be added to `guacamole-server`.** That repository is not touched by this plan.
- **Naming:** all Redis keys are produced by `ClusterKeys`. Never build a key string inline.

---

## File Structure

**New module — `guacamole-cluster/`**

| File | Responsibility |
|---|---|
| `pom.xml` | Module build; depends on `guacamole-ext`, Guice, Lettuce |
| `src/main/java/org/apache/guacamole/cluster/ClusterStore.java` | The whole cluster-state interface. Single seam every consumer uses |
| `src/main/java/org/apache/guacamole/cluster/NoOpClusterStore.java` | Used when `cluster-enabled=false`; preserves upstream behavior |
| `src/main/java/org/apache/guacamole/cluster/ClusterKeys.java` | Redis key construction and segment escaping. The only place key strings are built |
| `src/main/java/org/apache/guacamole/cluster/SeatResult.java` | Enum of acquire outcomes |
| `src/main/java/org/apache/guacamole/cluster/SeatKey.java` | One limit-bearing index: key, limit, failure outcome |
| `src/main/java/org/apache/guacamole/cluster/SeatRequest.java` | Tunnel UUID plus its ordered `SeatKey` list |
| `src/main/java/org/apache/guacamole/cluster/TunnelRegistration.java` | Immutable description of one live tunnel |
| `src/main/java/org/apache/guacamole/cluster/ClusterProperties.java` | `GuacamoleProperty` definitions for every `cluster-*` and `guacd-cluster-*` key |
| `src/main/java/org/apache/guacamole/cluster/redis/RedisClusterStore.java` | Lettuce implementation of `ClusterStore` |
| `src/main/java/org/apache/guacamole/cluster/redis/LuaScript.java` | Script text loading, SHA caching, `NOSCRIPT` recovery |
| `src/main/resources/org/apache/guacamole/cluster/redis/acquire-seats.lua` | The atomic seat script |
| `src/main/java/org/apache/guacamole/cluster/ClusterHeartbeat.java` | Scheduled refresh of this replica's registrations |
| `src/main/java/org/apache/guacamole/cluster/guacd/GuacdEndpoint.java` | Host, port, encryption method; Redis-safe string form |
| `src/main/java/org/apache/guacamole/cluster/guacd/GuacdPool.java` | Candidate resolution from a static list or headless-Service DNS |
| `src/main/java/org/apache/guacamole/cluster/guacd/GuacdSelector.java` | Join routing, pinned override, least-loaded selection, circuit breaker |
| `src/test/java/org/apache/guacamole/cluster/redis/RedisTestSupport.java` | Shared Testcontainers Redis fixture |

**Modified**

| File | Change |
|---|---|
| `pom.xml` | Add `guacamole-cluster` module; add Lettuce and Testcontainers versions and `dependencyManagement` entries |
| `extensions/guacamole-auth-jdbc/modules/guacamole-auth-jdbc-base/pom.xml` | Add `guacamole-cluster` dependency |
| `.../jdbc/JDBCAuthenticationProviderModule.java` | Bind `ClusterStore`, `GuacdPool`, `GuacdSelector` |
| `.../jdbc/tunnel/AbstractGuacamoleTunnelService.java:552-555` | Replace fixed proxy config with `GuacdSelector`; register the tunnel; release on cleanup |

**Deployment**

| File | Responsibility |
|---|---|
| `docs/superpowers/deploy/guacd-headless-service.yaml` | Headless Service and Deployment with a TCP readiness probe |
| `docs/superpowers/deploy/guacamole-deployment.yaml` | Two web application replicas plus JVM DNS TTL setting |
| `docs/superpowers/deploy/ingress-sticky.yaml` | Session affinity on the auth token |
| `docs/superpowers/deploy/README.md` | How to run the two-replica stack |

---

## Task 1: Module skeleton, build wiring, and Testcontainers harness

**Files:**
- Create: `guacamole-cluster/pom.xml`
- Create: `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/redis/RedisTestSupport.java`
- Create: `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/redis/RedisHarnessTest.java`
- Modify: `pom.xml` (add module, versions, dependencyManagement)

**Interfaces:**
- Consumes: nothing.
- Produces: Maven module `org.apache.guacamole:guacamole-cluster:${revision}`. Test fixture `RedisTestSupport.redisUri()` returning a `String` such as `redis://localhost:32771`.

- [ ] **Step 1: Add dependency versions to the parent pom**

In `pom.xml`, inside the existing `<properties>` block (alongside `<junit.version>` at line 53), add:

```xml
        <lettuce.version>6.3.2.RELEASE</lettuce.version>
        <testcontainers.version>1.19.8</testcontainers.version>
```

In the parent `<dependencyManagement><dependencies>` block, add:

```xml
            <dependency>
                <groupId>io.lettuce</groupId>
                <artifactId>lettuce-core</artifactId>
                <version>${lettuce.version}</version>
            </dependency>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers</artifactId>
                <version>${testcontainers.version}</version>
                <scope>test</scope>
            </dependency>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>junit-jupiter</artifactId>
                <version>${testcontainers.version}</version>
                <scope>test</scope>
            </dependency>
```

- [ ] **Step 2: Register the module**

In `pom.xml`, immediately after the `<module>guacamole-ext</module>` entry (line 91), add:

```xml
        <!-- Cluster coordination (Redis-backed HA support) -->
        <module>guacamole-cluster</module>
```

- [ ] **Step 3: Create the module pom**

Create `guacamole-cluster/pom.xml`. Copy the ASF XML license header verbatim from `guacamole-ext/pom.xml:2-19` as the first thing after the XML declaration, then:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                        http://maven.apache.org/maven-v4_0_0.xsd">

    <modelVersion>4.0.0</modelVersion>
    <groupId>org.apache.guacamole</groupId>
    <artifactId>guacamole-cluster</artifactId>
    <packaging>jar</packaging>
    <name>guacamole-cluster</name>
    <url>http://guacamole.apache.org/</url>

    <parent>
        <groupId>org.apache.guacamole</groupId>
        <artifactId>guacamole-client</artifactId>
        <version>${revision}</version>
    </parent>

    <description>
        Redis-backed cluster coordination for running multiple Guacamole web
        application replicas against a pool of guacd instances.
    </description>

    <dependencies>

        <dependency>
            <groupId>org.apache.guacamole</groupId>
            <artifactId>guacamole-ext</artifactId>
            <version>${revision}</version>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>io.lettuce</groupId>
            <artifactId>lettuce-core</artifactId>
        </dependency>

        <dependency>
            <groupId>com.google.inject</groupId>
            <artifactId>guice</artifactId>
            <version>${guice.version}</version>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <scope>provided</scope>
        </dependency>

        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers</artifactId>
        </dependency>

        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
        </dependency>

    </dependencies>

</project>
```

`slf4j-api` is already managed by the parent (`pom.xml:459-461`, version `${slf4j.version}` =
`2.0.18`), so no version is declared here. The same applies to the JUnit and Testcontainers
dependencies above.

- [ ] **Step 4: Create the shared Redis test fixture**

Create `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/redis/RedisTestSupport.java` (ASF Java header first):

```java
package org.apache.guacamole.cluster.redis;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Redis container for cluster store tests. A single container is
 * started for the whole JVM and reused, since every test isolates itself by
 * flushing the database rather than by starting a new server.
 */
public final class RedisTestSupport {

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
                    .withExposedPorts(6379);

    static {
        REDIS.start();
    }

    private RedisTestSupport() {}

    public static String redisUri() {
        return "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379);
    }

}
```

- [ ] **Step 5: Write the failing harness test**

Create `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/redis/RedisHarnessTest.java`:

```java
package org.apache.guacamole.cluster.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RedisHarnessTest {

    @Test
    public void redisContainerRespondsToPing() {
        RedisClient client = RedisClient.create(RedisTestSupport.redisUri());
        StatefulRedisConnection<String, String> connection = client.connect();
        try {
            assertEquals("PONG", connection.sync().ping());
        }
        finally {
            connection.close();
            client.shutdown();
        }
    }

}
```

- [ ] **Step 6: Run the test to verify the harness works**

```bash
cd /Users/ziv.lifshits/Workspace/honeybadger/guacamole-client
mvn -q -pl guacamole-cluster -am test -Dtest=RedisHarnessTest
```

Expected: PASS. If Docker is not running the test errors with a Testcontainers connection failure — start Docker and re-run. If the build fails with a RAT license error, the ASF header is missing from a new file.

- [ ] **Step 7: Commit**

```bash
git add pom.xml guacamole-cluster/
git commit -m "build: add guacamole-cluster module with Testcontainers Redis harness"
```

---

## Task 2: Redis key construction

**Files:**
- Create: `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/ClusterKeys.java`
- Test: `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/ClusterKeysTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `ClusterKeys.connectionIndex(String)`, `.groupIndex(String)`, `.userIndex(String)`, `.guacdIndex(String endpointKey)`, `.ALL_INDEX`, `.userConnectionSeat(String, String)`, `.userGroupSeat(String, String)`, `.tunnel(String)`, `.route(String)` — all returning `String`.

Why this is its own task: usernames and identifiers are user-controlled and are concatenated into keys. Without escaping, a username containing the separator can be crafted to collide with a different user's key. That is a security-relevant bug, and it is much easier to get right once, in isolation, with its own test.

- [ ] **Step 1: Write the failing test**

Create `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/ClusterKeysTest.java`:

```java
package org.apache.guacamole.cluster;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ClusterKeysTest {

    @Test
    public void buildsExpectedKeyShapes() {
        assertEquals("guac:idx:conn:7", ClusterKeys.connectionIndex("7"));
        assertEquals("guac:idx:group:3", ClusterKeys.groupIndex("3"));
        assertEquals("guac:idx:user:alice", ClusterKeys.userIndex("alice"));
        assertEquals("guac:idx:all", ClusterKeys.ALL_INDEX);
        assertEquals("guac:seat:user:alice:7", ClusterKeys.userConnectionSeat("alice", "7"));
        assertEquals("guac:seat:user:alice:g:3", ClusterKeys.userGroupSeat("alice", "3"));
        assertEquals("guac:tunnel:abc", ClusterKeys.tunnel("abc"));
        assertEquals("guac:route:$xyz", ClusterKeys.route("$xyz"));
    }

    @Test
    public void escapesSeparatorsSoSegmentsCannotCollide() {

        // Username "alice:7" with connection "9" must not produce the same key
        // as username "alice" with connection "7:9"
        assertNotEquals(
                ClusterKeys.userConnectionSeat("alice:7", "9"),
                ClusterKeys.userConnectionSeat("alice", "7:9"));

    }

    @Test
    public void escapesPercentSoEscapingIsUnambiguous() {

        // "a%3Ab" must not collide with the escaped form of "a:b"
        assertNotEquals(
                ClusterKeys.userIndex("a%3Ab"),
                ClusterKeys.userIndex("a:b"));

    }

}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
mvn -q -pl guacamole-cluster test -Dtest=ClusterKeysTest
```

Expected: FAIL — compilation error, `ClusterKeys` does not exist.

- [ ] **Step 3: Implement `ClusterKeys`**

Create `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/ClusterKeys.java` (ASF header first):

```java
package org.apache.guacamole.cluster;

/**
 * Construction of every Redis key used by the cluster store. Key strings are
 * never built anywhere else.
 *
 * Identifier segments may contain arbitrary user-supplied text, including the
 * ":" separator. Segments are therefore escaped so that two different pairs of
 * segments can never produce the same key.
 */
public final class ClusterKeys {

    /**
     * Sorted set containing every active tunnel in the cluster.
     */
    public static final String ALL_INDEX = "guac:idx:all";

    private ClusterKeys() {}

    /**
     * Escapes a single key segment. "%" is escaped first so that the escaping
     * is reversible and unambiguous, then ":" is replaced.
     *
     * @param segment
     *     The raw, possibly user-supplied segment.
     *
     * @return
     *     The segment, safe for use between ":" separators.
     */
    private static String escape(String segment) {
        if (segment == null)
            return "";
        return segment.replace("%", "%25").replace(":", "%3A");
    }

    public static String connectionIndex(String connectionIdentifier) {
        return "guac:idx:conn:" + escape(connectionIdentifier);
    }

    public static String groupIndex(String groupIdentifier) {
        return "guac:idx:group:" + escape(groupIdentifier);
    }

    public static String userIndex(String username) {
        return "guac:idx:user:" + escape(username);
    }

    public static String guacdIndex(String endpointKey) {
        return "guac:idx:guacd:" + escape(endpointKey);
    }

    public static String userConnectionSeat(String username, String connectionIdentifier) {
        return "guac:seat:user:" + escape(username) + ":" + escape(connectionIdentifier);
    }

    public static String userGroupSeat(String username, String groupIdentifier) {
        return "guac:seat:user:" + escape(username) + ":g:" + escape(groupIdentifier);
    }

    public static String tunnel(String tunnelUuid) {
        return "guac:tunnel:" + escape(tunnelUuid);
    }

    public static String route(String guacdConnectionId) {
        return "guac:route:" + escape(guacdConnectionId);
    }

}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
mvn -q -pl guacamole-cluster test -Dtest=ClusterKeysTest
```

Expected: PASS, 3 tests.

- [ ] **Step 5: Commit**

```bash
git add guacamole-cluster/src/main/java/org/apache/guacamole/cluster/ClusterKeys.java \
        guacamole-cluster/src/test/java/org/apache/guacamole/cluster/ClusterKeysTest.java
git commit -m "feat(cluster): add Redis key construction with segment escaping"
```

---

## Task 3: Value objects and the `ClusterStore` interface

**Files:**
- Create: `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/SeatResult.java`
- Create: `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/SeatKey.java`
- Create: `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/SeatRequest.java`
- Create: `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/guacd/GuacdEndpoint.java`
- Create: `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/TunnelRegistration.java`
- Create: `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/ClusterStore.java`
- Test: `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/guacd/GuacdEndpointTest.java`

**Interfaces:**
- Consumes: `ClusterKeys` (Task 2).
- Produces:
  - `enum SeatResult { SUCCESS, CONNECTION_LIMIT, USER_CONNECTION_LIMIT, GROUP_LIMIT, USER_GROUP_LIMIT }`
  - `SeatKey(String redisKey, int limit, SeatResult failureResult)` with `getRedisKey()`, `getLimit()`, `getFailureResult()`
  - `SeatRequest(String tunnelUuid, List<SeatKey> keys)` with `getTunnelUuid()`, `getKeys()`
  - `GuacdEndpoint(String hostname, int port, EncryptionMethod method)` with `getHostname()`, `getPort()`, `getEncryptionMethod()`, `toProxyConfiguration()`, `toKey()`, static `from(GuacamoleProxyConfiguration)`, static `fromKey(String)`
  - `TunnelRegistration` with getters listed in Step 5
  - `interface ClusterStore` with the methods listed in Step 6, including `String getNodeId()`

- [ ] **Step 1: Write the failing test for `GuacdEndpoint`**

`GuacdEndpoint` gets its own test because its string form is persisted in Redis as a route value, and IPv6 addresses contain colons — a naive `host:port` encoding silently corrupts every IPv6 endpoint.

Create `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/guacd/GuacdEndpointTest.java`:

```java
package org.apache.guacamole.cluster.guacd;

import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GuacdEndpointTest {

    @Test
    public void roundTripsThroughKeyForm() {
        GuacdEndpoint endpoint = new GuacdEndpoint("guacd-0", 4822, EncryptionMethod.NONE);
        assertEquals(endpoint, GuacdEndpoint.fromKey(endpoint.toKey()));
    }

    @Test
    public void roundTripsIpv6Addresses() {
        GuacdEndpoint endpoint = new GuacdEndpoint("fd00::1", 4822, EncryptionMethod.SSL);
        assertEquals(endpoint, GuacdEndpoint.fromKey(endpoint.toKey()));
    }

    @Test
    public void convertsToProxyConfiguration() {
        GuacdEndpoint endpoint = new GuacdEndpoint("guacd-1", 4823, EncryptionMethod.SSL);
        assertEquals("guacd-1", endpoint.toProxyConfiguration().getHostname());
        assertEquals(4823, endpoint.toProxyConfiguration().getPort());
        assertEquals(EncryptionMethod.SSL,
                endpoint.toProxyConfiguration().getEncryptionMethod());
    }

}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
mvn -q -pl guacamole-cluster test -Dtest=GuacdEndpointTest
```

Expected: FAIL — compilation error, `GuacdEndpoint` does not exist.

- [ ] **Step 3: Implement `GuacdEndpoint`**

Create `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/guacd/GuacdEndpoint.java` (ASF header first):

```java
package org.apache.guacamole.cluster.guacd;

import java.util.Objects;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;

/**
 * A single guacd instance: hostname, port, and required encryption method.
 *
 * The string form produced by toKey() is stored in Redis as the value of a
 * route entry and as part of a load index key. "|" is used as the separator
 * because IPv6 literal addresses contain ":".
 */
public class GuacdEndpoint {

    private static final String SEPARATOR = "|";

    private final String hostname;
    private final int port;
    private final EncryptionMethod encryptionMethod;

    public GuacdEndpoint(String hostname, int port, EncryptionMethod encryptionMethod) {
        this.hostname = hostname;
        this.port = port;
        this.encryptionMethod = encryptionMethod;
    }

    public static GuacdEndpoint from(GuacamoleProxyConfiguration config) {
        return new GuacdEndpoint(config.getHostname(), config.getPort(),
                config.getEncryptionMethod());
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public EncryptionMethod getEncryptionMethod() {
        return encryptionMethod;
    }

    public GuacamoleProxyConfiguration toProxyConfiguration() {
        return new GuacamoleProxyConfiguration(hostname, port, encryptionMethod);
    }

    /**
     * Returns the stable string form of this endpoint, suitable for storage in
     * Redis.
     *
     * @return
     *     This endpoint as "hostname|port|encryptionMethod".
     */
    public String toKey() {
        return hostname + SEPARATOR + port + SEPARATOR + encryptionMethod.name();
    }

    /**
     * Parses the string form produced by toKey().
     *
     * @param key
     *     The string form of an endpoint.
     *
     * @return
     *     The endpoint described by the given string.
     *
     * @throws IllegalArgumentException
     *     If the given string is not a valid endpoint key.
     */
    public static GuacdEndpoint fromKey(String key) {

        int portStart = key.indexOf(SEPARATOR);
        int methodStart = key.indexOf(SEPARATOR, portStart + 1);

        if (portStart < 0 || methodStart < 0)
            throw new IllegalArgumentException("Malformed guacd endpoint key: " + key);

        return new GuacdEndpoint(
                key.substring(0, portStart),
                Integer.parseInt(key.substring(portStart + 1, methodStart)),
                EncryptionMethod.valueOf(key.substring(methodStart + 1)));

    }

    @Override
    public boolean equals(Object other) {

        if (this == other)
            return true;

        if (!(other instanceof GuacdEndpoint))
            return false;

        GuacdEndpoint endpoint = (GuacdEndpoint) other;
        return port == endpoint.port
                && Objects.equals(hostname, endpoint.hostname)
                && encryptionMethod == endpoint.encryptionMethod;

    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, port, encryptionMethod);
    }

    @Override
    public String toString() {
        return toKey();
    }

}
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
mvn -q -pl guacamole-cluster test -Dtest=GuacdEndpointTest
```

Expected: PASS, 3 tests.

- [ ] **Step 5: Implement the remaining value objects**

Create `SeatResult.java`:

```java
package org.apache.guacamole.cluster;

/**
 * The outcome of an attempt to acquire cluster-wide seats for a tunnel.
 */
public enum SeatResult {

    /** All limits were satisfied and the seats are now held. */
    SUCCESS,

    /** The connection is at its overall concurrent-use limit. */
    CONNECTION_LIMIT,

    /** This user is at their per-connection concurrent-use limit. */
    USER_CONNECTION_LIMIT,

    /** The balancing group is at its overall concurrent-use limit. */
    GROUP_LIMIT,

    /** This user is at their per-group concurrent-use limit. */
    USER_GROUP_LIMIT

}
```

Create `SeatKey.java`:

```java
package org.apache.guacamole.cluster;

/**
 * One limit-bearing sorted set: the Redis key, the maximum number of members
 * permitted, and the result to report if that maximum is reached.
 */
public class SeatKey {

    private final String redisKey;
    private final int limit;
    private final SeatResult failureResult;

    /**
     * @param redisKey
     *     The Redis sorted set backing this limit.
     *
     * @param limit
     *     The maximum number of concurrent members permitted. Zero or negative
     *     means unlimited.
     *
     * @param failureResult
     *     The result reported when this limit is what blocked acquisition.
     */
    public SeatKey(String redisKey, int limit, SeatResult failureResult) {
        this.redisKey = redisKey;
        this.limit = limit;
        this.failureResult = failureResult;
    }

    public String getRedisKey() {
        return redisKey;
    }

    public int getLimit() {
        return limit;
    }

    public SeatResult getFailureResult() {
        return failureResult;
    }

}
```

Create `SeatRequest.java`:

```java
package org.apache.guacamole.cluster;

import java.util.Collections;
import java.util.List;

/**
 * A request to hold cluster-wide seats for one tunnel across an ordered set of
 * limit-bearing indexes. The order of the keys is significant: the seat script
 * reports failure by position.
 */
public class SeatRequest {

    private final String tunnelUuid;
    private final List<SeatKey> keys;

    public SeatRequest(String tunnelUuid, List<SeatKey> keys) {
        this.tunnelUuid = tunnelUuid;
        this.keys = Collections.unmodifiableList(keys);
    }

    public String getTunnelUuid() {
        return tunnelUuid;
    }

    public List<SeatKey> getKeys() {
        return keys;
    }

}
```

Create `TunnelRegistration.java`:

```java
package org.apache.guacamole.cluster;

import org.apache.guacamole.cluster.guacd.GuacdEndpoint;

/**
 * Immutable description of one live tunnel, as published to the cluster.
 */
public class TunnelRegistration {

    private final String tunnelUuid;
    private final String nodeId;
    private final String guacdConnectionId;
    private final GuacdEndpoint endpoint;
    private final String connectionIdentifier;
    private final String groupIdentifier;
    private final String sharingProfileIdentifier;
    private final String username;
    private final String remoteHost;
    private final long startTime;

    public TunnelRegistration(String tunnelUuid, String nodeId,
            String guacdConnectionId, GuacdEndpoint endpoint,
            String connectionIdentifier, String groupIdentifier,
            String sharingProfileIdentifier, String username,
            String remoteHost, long startTime) {
        this.tunnelUuid = tunnelUuid;
        this.nodeId = nodeId;
        this.guacdConnectionId = guacdConnectionId;
        this.endpoint = endpoint;
        this.connectionIdentifier = connectionIdentifier;
        this.groupIdentifier = groupIdentifier;
        this.sharingProfileIdentifier = sharingProfileIdentifier;
        this.username = username;
        this.remoteHost = remoteHost;
        this.startTime = startTime;
    }

    public String getTunnelUuid() { return tunnelUuid; }
    public String getNodeId() { return nodeId; }
    public String getGuacdConnectionId() { return guacdConnectionId; }
    public GuacdEndpoint getEndpoint() { return endpoint; }
    public String getConnectionIdentifier() { return connectionIdentifier; }
    public String getGroupIdentifier() { return groupIdentifier; }
    public String getSharingProfileIdentifier() { return sharingProfileIdentifier; }
    public String getUsername() { return username; }
    public String getRemoteHost() { return remoteHost; }
    public long getStartTime() { return startTime; }

}
```

- [ ] **Step 6: Define the `ClusterStore` interface**

Create `ClusterStore.java`:

```java
package org.apache.guacamole.cluster;

import java.util.Collection;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.cluster.guacd.GuacdEndpoint;

/**
 * Cluster-wide state shared by every Guacamole web application replica.
 *
 * Implementations must tolerate the backing store being unreachable. Methods
 * that cannot safely proceed without it throw GuacamoleException; methods whose
 * callers can degrade to replica-local behavior return a neutral value and
 * report unavailability through isAvailable().
 */
public interface ClusterStore {

    /**
     * Atomically prunes stale members, checks every limit, and takes seats.
     *
     * @param request
     *     The seats to acquire.
     *
     * @return
     *     SUCCESS if every limit was satisfied, otherwise the failure result of
     *     the first key whose limit was reached.
     *
     * @throws GuacamoleException
     *     If the store is unreachable.
     */
    SeatResult acquireSeats(SeatRequest request) throws GuacamoleException;

    /**
     * Releases seats previously acquired by acquireSeats(). Safe to call more
     * than once for the same tunnel.
     *
     * @param request
     *     The seats to release. Only the tunnel UUID and the key list are used.
     *
     * @throws GuacamoleException
     *     If the store is unreachable.
     */
    void releaseSeats(SeatRequest request) throws GuacamoleException;

    /**
     * Publishes a live tunnel to the cluster, making it visible to every
     * replica and routable by its guacd connection ID.
     *
     * @param registration
     *     The tunnel to publish.
     *
     * @throws GuacamoleException
     *     If the store is unreachable.
     */
    void registerTunnel(TunnelRegistration registration) throws GuacamoleException;

    /**
     * Removes a tunnel and its route from the cluster. Safe to call for a
     * tunnel that is already absent.
     *
     * @param registration
     *     The tunnel to remove.
     *
     * @throws GuacamoleException
     *     If the store is unreachable.
     */
    void unregisterTunnel(TunnelRegistration registration) throws GuacamoleException;

    /**
     * Refreshes the heartbeat score of every given tunnel, preventing them from
     * ageing out of the cluster indexes.
     *
     * @param registrations
     *     Every tunnel currently owned by this replica.
     *
     * @throws GuacamoleException
     *     If the store is unreachable.
     */
    void heartbeat(Collection<TunnelRegistration> registrations) throws GuacamoleException;

    /**
     * Returns the guacd instance hosting the given guacd connection ID.
     *
     * @param guacdConnectionId
     *     The connection ID issued by guacd in its "ready" instruction.
     *
     * @return
     *     The guacd instance hosting that connection, or null if no live route
     *     exists.
     *
     * @throws GuacamoleException
     *     If the store is unreachable.
     */
    GuacdEndpoint lookupRoute(String guacdConnectionId) throws GuacamoleException;

    /**
     * Returns the number of live tunnels currently assigned to the given guacd
     * instance across the whole cluster.
     *
     * @param endpoint
     *     The guacd instance to count.
     *
     * @return
     *     The number of live tunnels, or 0 if the store is unreachable.
     */
    long countTunnels(GuacdEndpoint endpoint);

    /**
     * @return
     *     The identity of this replica, as recorded on every tunnel this
     *     replica owns. Used to decide whether a tunnel can be closed locally
     *     or must be killed through another replica.
     */
    String getNodeId();

    /**
     * @return
     *     true if the backing store was reachable as of the most recent
     *     operation, false if callers should degrade to replica-local behavior.
     */
    boolean isAvailable();

    /**
     * Releases all resources held by this store.
     */
    void shutdown();

}
```

- [ ] **Step 7: Verify the module compiles**

```bash
mvn -q -pl guacamole-cluster test
```

Expected: PASS — `ClusterKeysTest`, `GuacdEndpointTest`, and `RedisHarnessTest` all green.

- [ ] **Step 8: Commit**

```bash
git add guacamole-cluster/src
git commit -m "feat(cluster): add ClusterStore interface and value objects"
```

---

## Task 4: The atomic seat script

**Files:**
- Create: `guacamole-cluster/src/main/resources/org/apache/guacamole/cluster/redis/acquire-seats.lua`
- Create: `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/redis/LuaScript.java`
- Create: `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/redis/RedisClusterStore.java` (seat methods only)
- Test: `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/redis/SeatAcquisitionTest.java`

**Interfaces:**
- Consumes: `ClusterKeys`, `SeatRequest`, `SeatKey`, `SeatResult`, `ClusterStore`, `RedisTestSupport`.
- Produces: `new RedisClusterStore(String redisUri, long staleWindowMs, String nodeId)`; implementations of `acquireSeats` and `releaseSeats`. `LuaScript.load(String resourcePath)` returning a `LuaScript` with `eval(RedisCommands<String,String>, String[] keys, String[] args)` returning `long`.

This is the highest-risk unit in the whole design: the only place that hands out capacity. Its tests come first and are the most detailed in the plan.

- [ ] **Step 1: Write the failing tests**

Create `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/redis/SeatAcquisitionTest.java`:

```java
package org.apache.guacamole.cluster.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.guacamole.cluster.ClusterKeys;
import org.apache.guacamole.cluster.SeatKey;
import org.apache.guacamole.cluster.SeatRequest;
import org.apache.guacamole.cluster.SeatResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SeatAcquisitionTest {

    private static final long STALE_WINDOW_MS = 30000L;

    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;
    private RedisClusterStore store;

    @BeforeEach
    public void setUp() {
        client = RedisClient.create(RedisTestSupport.redisUri());
        connection = client.connect();
        connection.sync().flushall();
        store = new RedisClusterStore(RedisTestSupport.redisUri(), STALE_WINDOW_MS, "test-node");
    }

    @AfterEach
    public void tearDown() {
        store.shutdown();
        connection.close();
        client.shutdown();
    }

    /**
     * Builds a request for a single connection limited to the given number of
     * concurrent uses, with no per-user or group limits.
     */
    private SeatRequest connectionRequest(String uuid, String connectionId, int limit) {
        return new SeatRequest(uuid, Arrays.asList(
                new SeatKey(ClusterKeys.connectionIndex(connectionId), limit,
                        SeatResult.CONNECTION_LIMIT)));
    }

    @Test
    public void grantsUpToTheLimitThenRefuses() throws Exception {

        assertEquals(SeatResult.SUCCESS, store.acquireSeats(connectionRequest("t1", "c", 2)));
        assertEquals(SeatResult.SUCCESS, store.acquireSeats(connectionRequest("t2", "c", 2)));
        assertEquals(SeatResult.CONNECTION_LIMIT,
                store.acquireSeats(connectionRequest("t3", "c", 2)));

    }

    @Test
    public void treatsZeroLimitAsUnlimited() throws Exception {

        for (int i = 0; i < 50; i++)
            assertEquals(SeatResult.SUCCESS,
                    store.acquireSeats(connectionRequest("t" + i, "c", 0)));

    }

    @Test
    public void reportsWhichLimitFailed() throws Exception {

        // Connection allows 5, but this user allows only 1
        List<SeatKey> keys = Arrays.asList(
                new SeatKey(ClusterKeys.connectionIndex("c"), 5, SeatResult.CONNECTION_LIMIT),
                new SeatKey(ClusterKeys.userConnectionSeat("alice", "c"), 1,
                        SeatResult.USER_CONNECTION_LIMIT));

        assertEquals(SeatResult.SUCCESS, store.acquireSeats(new SeatRequest("t1", keys)));
        assertEquals(SeatResult.USER_CONNECTION_LIMIT,
                store.acquireSeats(new SeatRequest("t2", keys)));

    }

    @Test
    public void isIdempotentForTheSameTunnel() throws Exception {

        // A retry after a failover must not count the member it already added
        SeatRequest request = connectionRequest("t1", "c", 1);
        assertEquals(SeatResult.SUCCESS, store.acquireSeats(request));
        assertEquals(SeatResult.SUCCESS, store.acquireSeats(request));

        // ...and the seat must still be held exactly once
        assertEquals(1L, connection.sync().zcard(ClusterKeys.connectionIndex("c")));

    }

    @Test
    public void ignoresStaleMembers() throws Exception {

        String key = ClusterKeys.connectionIndex("c");

        // Simulate a tunnel from a dead replica, last seen well outside the window
        long ancient = System.currentTimeMillis() - (STALE_WINDOW_MS * 10);
        connection.sync().zadd(key, (double) ancient, "dead-tunnel");

        // The stale member must neither block nor survive
        assertEquals(SeatResult.SUCCESS, store.acquireSeats(connectionRequest("t1", "c", 1)));
        assertEquals(1L, connection.sync().zcard(key));

    }

    @Test
    public void releaseFreesTheSeat() throws Exception {

        assertEquals(SeatResult.SUCCESS, store.acquireSeats(connectionRequest("t1", "c", 1)));
        store.releaseSeats(connectionRequest("t1", "c", 1));
        assertEquals(SeatResult.SUCCESS, store.acquireSeats(connectionRequest("t2", "c", 1)));

    }

    @Test
    public void releaseIsSafeToRepeat() throws Exception {

        store.releaseSeats(connectionRequest("never-acquired", "c", 1));
        store.releaseSeats(connectionRequest("never-acquired", "c", 1));
        assertEquals(0L, connection.sync().zcard(ClusterKeys.connectionIndex("c")));

    }

    @Test
    public void neverExceedsTheLimitUnderConcurrency() throws Exception {

        final int limit = 5;
        final int threads = 200;

        ExecutorService executor = Executors.newFixedThreadPool(32);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);
        final AtomicInteger granted = new AtomicInteger();
        final List<Throwable> errors = new ArrayList<Throwable>();

        for (int i = 0; i < threads; i++) {
            final String uuid = UUID.randomUUID().toString();
            executor.submit(new Runnable() {

                @Override
                public void run() {
                    try {
                        start.await();
                        if (store.acquireSeats(connectionRequest(uuid, "c", limit))
                                == SeatResult.SUCCESS)
                            granted.incrementAndGet();
                    }
                    catch (Throwable e) {
                        synchronized (errors) {
                            errors.add(e);
                        }
                    }
                    finally {
                        done.countDown();
                    }
                }

            });
        }

        start.countDown();
        assertTrue(done.await(60, TimeUnit.SECONDS), "acquisition threads did not finish");
        executor.shutdownNow();

        synchronized (errors) {
            assertTrue(errors.isEmpty(), "errors during acquisition: " + errors);
        }

        assertEquals(limit, granted.get());
        assertEquals((long) limit, connection.sync().zcard(ClusterKeys.connectionIndex("c")));

    }

}
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
mvn -q -pl guacamole-cluster test -Dtest=SeatAcquisitionTest
```

Expected: FAIL — compilation error, `RedisClusterStore` does not exist.

- [ ] **Step 3: Write the Lua script**

Create `guacamole-cluster/src/main/resources/org/apache/guacamole/cluster/redis/acquire-seats.lua`. Lua comments use `--`, so the ASF header goes in as `--` comment lines:

```lua
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.

-- Atomically prunes stale members, verifies every limit, and takes seats.
--
-- KEYS    one sorted set per limit-bearing index, in caller-defined order
-- ARGV[1] tunnel UUID
-- ARGV[2] stale window, in milliseconds
-- ARGV[i+2] limit for KEYS[i]; zero or negative means unlimited
--
-- Returns 0 on success, or the 1-based index of the key whose limit was
-- reached. Time is taken from the Redis server so that replica clock drift
-- cannot affect staleness.

local uuid  = ARGV[1]
local stale = tonumber(ARGV[2])

local time = redis.call('TIME')
local now = (tonumber(time[1]) * 1000) + math.floor(tonumber(time[2]) / 1000)
local cutoff = now - stale

for i = 1, #KEYS do
    redis.call('ZREMRANGEBYSCORE', KEYS[i], '-inf', cutoff)
end

for i = 1, #KEYS do
    local limit = tonumber(ARGV[i + 2])
    if limit > 0 and redis.call('ZSCORE', KEYS[i], uuid) == false then
        if redis.call('ZCARD', KEYS[i]) >= limit then
            return i
        end
    end
end

for i = 1, #KEYS do
    redis.call('ZADD', KEYS[i], now, uuid)
end

return 0
```

- [ ] **Step 4: Implement `LuaScript`**

Create `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/redis/LuaScript.java` (ASF header first):

```java
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
```

- [ ] **Step 5: Implement the seat portion of `RedisClusterStore`**

Create `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/redis/RedisClusterStore.java` (ASF header first). The remaining `ClusterStore` methods are filled in by Task 5; for now they throw `UnsupportedOperationException` so the class compiles and the seat tests can run:

```java
package org.apache.guacamole.cluster.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.util.Collection;
import java.util.List;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.cluster.ClusterStore;
import org.apache.guacamole.cluster.SeatKey;
import org.apache.guacamole.cluster.SeatRequest;
import org.apache.guacamole.cluster.SeatResult;
import org.apache.guacamole.cluster.TunnelRegistration;
import org.apache.guacamole.cluster.guacd.GuacdEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis-backed implementation of ClusterStore, using Lettuce.
 */
public class RedisClusterStore implements ClusterStore {

    private static final Logger logger = LoggerFactory.getLogger(RedisClusterStore.class);

    private static final String ACQUIRE_SEATS_SCRIPT =
            "/org/apache/guacamole/cluster/redis/acquire-seats.lua";

    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    private final LuaScript acquireSeats = LuaScript.load(ACQUIRE_SEATS_SCRIPT);
    private final long staleWindowMs;
    private final String nodeId;

    private volatile boolean available = true;

    /**
     * @param redisUri
     *     Lettuce URI of the Redis server or Sentinel set.
     *
     * @param staleWindowMs
     *     Milliseconds after which an unrefreshed member is considered dead.
     *
     * @param nodeId
     *     Identity of this replica.
     */
    public RedisClusterStore(String redisUri, long staleWindowMs, String nodeId) {
        this.client = RedisClient.create(redisUri);
        this.connection = client.connect();
        this.staleWindowMs = staleWindowMs;
        this.nodeId = nodeId;
    }

    private RedisCommands<String, String> commands() {
        return connection.sync();
    }

    @Override
    public SeatResult acquireSeats(SeatRequest request) throws GuacamoleException {

        List<SeatKey> seatKeys = request.getKeys();

        String[] keys = new String[seatKeys.size()];
        String[] args = new String[seatKeys.size() + 2];

        args[0] = request.getTunnelUuid();
        args[1] = Long.toString(staleWindowMs);

        for (int i = 0; i < seatKeys.size(); i++) {
            keys[i] = seatKeys.get(i).getRedisKey();
            args[i + 2] = Integer.toString(seatKeys.get(i).getLimit());
        }

        try {

            long result = acquireSeats.eval(commands(), keys, args);
            available = true;

            if (result == 0)
                return SeatResult.SUCCESS;

            return seatKeys.get((int) result - 1).getFailureResult();

        }
        catch (RedisException e) {
            available = false;
            throw new GuacamoleServerException("Unable to acquire cluster seats.", e);
        }

    }

    @Override
    public void releaseSeats(SeatRequest request) throws GuacamoleException {

        try {

            RedisCommands<String, String> commands = commands();
            for (SeatKey key : request.getKeys())
                commands.zrem(key.getRedisKey(), request.getTunnelUuid());

            available = true;

        }
        catch (RedisException e) {
            available = false;
            throw new GuacamoleServerException("Unable to release cluster seats.", e);
        }

    }

    @Override
    public void registerTunnel(TunnelRegistration registration) throws GuacamoleException {
        throw new UnsupportedOperationException("Implemented in Task 5.");
    }

    @Override
    public void unregisterTunnel(TunnelRegistration registration) throws GuacamoleException {
        throw new UnsupportedOperationException("Implemented in Task 5.");
    }

    @Override
    public void heartbeat(Collection<TunnelRegistration> registrations) throws GuacamoleException {
        throw new UnsupportedOperationException("Implemented in Task 5.");
    }

    @Override
    public GuacdEndpoint lookupRoute(String guacdConnectionId) throws GuacamoleException {
        throw new UnsupportedOperationException("Implemented in Task 5.");
    }

    @Override
    public long countTunnels(GuacdEndpoint endpoint) {
        throw new UnsupportedOperationException("Implemented in Task 5.");
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void shutdown() {
        connection.close();
        client.shutdown();
    }

}
```

- [ ] **Step 6: Run the tests to verify they pass**

```bash
mvn -q -pl guacamole-cluster test -Dtest=SeatAcquisitionTest
```

Expected: PASS, 8 tests. The concurrency test takes several seconds.

If `neverExceedsTheLimitUnderConcurrency` reports more than 5 grants, the script is not atomic — check that the limit check and the `ZADD` loop are in the same script and that `evalsha` is being used rather than separate commands.

- [ ] **Step 7: Commit**

```bash
git add guacamole-cluster/src
git commit -m "feat(cluster): add atomic Redis seat acquisition script"
```

---

## Task 5: Tunnel registration, routing, and load counting

**Files:**
- Modify: `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/redis/RedisClusterStore.java`
- Test: `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/redis/TunnelRegistrationTest.java`

**Interfaces:**
- Consumes: everything from Tasks 2-4.
- Produces: working `registerTunnel`, `unregisterTunnel`, `heartbeat`, `lookupRoute`, `countTunnels` on `RedisClusterStore`.

- [ ] **Step 1: Write the failing tests**

Create `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/redis/TunnelRegistrationTest.java`:

```java
package org.apache.guacamole.cluster.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import java.util.Arrays;
import java.util.Collections;
import org.apache.guacamole.cluster.ClusterKeys;
import org.apache.guacamole.cluster.TunnelRegistration;
import org.apache.guacamole.cluster.guacd.GuacdEndpoint;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TunnelRegistrationTest {

    private static final long STALE_WINDOW_MS = 30000L;

    private static final GuacdEndpoint GUACD_A =
            new GuacdEndpoint("guacd-a", 4822, EncryptionMethod.NONE);

    private static final GuacdEndpoint GUACD_B =
            new GuacdEndpoint("guacd-b", 4822, EncryptionMethod.NONE);

    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;
    private RedisClusterStore store;

    @BeforeEach
    public void setUp() {
        client = RedisClient.create(RedisTestSupport.redisUri());
        connection = client.connect();
        connection.sync().flushall();
        store = new RedisClusterStore(RedisTestSupport.redisUri(), STALE_WINDOW_MS, "node-1");
    }

    @AfterEach
    public void tearDown() {
        store.shutdown();
        connection.close();
        client.shutdown();
    }

    private TunnelRegistration registration(String uuid, GuacdEndpoint endpoint,
            String guacdConnectionId) {
        return new TunnelRegistration(uuid, "node-1", guacdConnectionId, endpoint,
                "conn-1", "group-1", null, "alice", "10.0.0.5",
                System.currentTimeMillis());
    }

    @Test
    public void registeredTunnelIsRoutableByGuacdConnectionId() throws Exception {

        store.registerTunnel(registration("t1", GUACD_A, "$abc"));
        assertEquals(GUACD_A, store.lookupRoute("$abc"));

    }

    @Test
    public void unknownRouteReturnsNull() throws Exception {
        assertNull(store.lookupRoute("$nonexistent"));
    }

    @Test
    public void registeredTunnelAppearsInEveryIndex() throws Exception {

        store.registerTunnel(registration("t1", GUACD_A, "$abc"));

        assertEquals(1L, connection.sync().zcard(ClusterKeys.ALL_INDEX));
        assertEquals(1L, connection.sync().zcard(ClusterKeys.userIndex("alice")));
        assertEquals(1L, connection.sync().zcard(ClusterKeys.guacdIndex(GUACD_A.toKey())));

    }

    @Test
    public void storesTheFullTunnelRecord() throws Exception {

        store.registerTunnel(registration("t1", GUACD_A, "$abc"));

        assertEquals("node-1", connection.sync().hget(ClusterKeys.tunnel("t1"), "nodeId"));
        assertEquals("conn-1",
                connection.sync().hget(ClusterKeys.tunnel("t1"), "connIdentifier"));
        assertEquals(GUACD_A.toKey(),
                connection.sync().hget(ClusterKeys.tunnel("t1"), "guacdEndpoint"));

    }

    @Test
    public void countsLiveTunnelsPerGuacdInstance() throws Exception {

        store.registerTunnel(registration("t1", GUACD_A, "$a1"));
        store.registerTunnel(registration("t2", GUACD_A, "$a2"));
        store.registerTunnel(registration("t3", GUACD_B, "$b1"));

        assertEquals(2L, store.countTunnels(GUACD_A));
        assertEquals(1L, store.countTunnels(GUACD_B));

    }

    @Test
    public void ignoresStaleTunnelsWhenCounting() throws Exception {

        String key = ClusterKeys.guacdIndex(GUACD_A.toKey());
        long ancient = System.currentTimeMillis() - (STALE_WINDOW_MS * 10);
        connection.sync().zadd(key, (double) ancient, "dead-tunnel");

        store.registerTunnel(registration("t1", GUACD_A, "$a1"));

        assertEquals(1L, store.countTunnels(GUACD_A));

    }

    @Test
    public void unregisterRemovesTunnelAndRoute() throws Exception {

        TunnelRegistration record = registration("t1", GUACD_A, "$abc");
        store.registerTunnel(record);
        store.unregisterTunnel(record);

        assertNull(store.lookupRoute("$abc"));
        assertEquals(0L, connection.sync().zcard(ClusterKeys.ALL_INDEX));
        assertEquals(0L, store.countTunnels(GUACD_A));

    }

    @Test
    public void unregisterIsSafeToRepeat() throws Exception {

        TunnelRegistration record = registration("t1", GUACD_A, "$abc");
        store.registerTunnel(record);
        store.unregisterTunnel(record);
        store.unregisterTunnel(record);

        assertEquals(0L, connection.sync().zcard(ClusterKeys.ALL_INDEX));

    }

    @Test
    public void heartbeatRefreshesScoresAndExpiry() throws Exception {

        TunnelRegistration record = registration("t1", GUACD_A, "$abc");
        store.registerTunnel(record);

        // Age the entry to just inside the stale window, then heartbeat
        String key = ClusterKeys.guacdIndex(GUACD_A.toKey());
        long aged = System.currentTimeMillis() - (STALE_WINDOW_MS - 1000L);
        connection.sync().zadd(key, (double) aged, "t1");

        store.heartbeat(Collections.singletonList(record));

        Double refreshed = connection.sync().zscore(key, "t1");
        assertEquals(true, refreshed.longValue() > aged,
                "heartbeat must move the score forward");

        // The route key must not be allowed to expire while the tunnel lives
        assertEquals(true, connection.sync().ttl(ClusterKeys.route("$abc")) > 0);

    }

    @Test
    public void heartbeatOfAnEmptyCollectionIsHarmless() throws Exception {
        store.heartbeat(Collections.<TunnelRegistration>emptyList());
        assertEquals(0L, connection.sync().zcard(ClusterKeys.ALL_INDEX));
    }

}
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
mvn -q -pl guacamole-cluster test -Dtest=TunnelRegistrationTest
```

Expected: FAIL — `UnsupportedOperationException: Implemented in Task 5.`

- [ ] **Step 3: Implement the registration methods**

In `RedisClusterStore.java`, add these imports:

```java
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
```

Add a helper that lists the index keys a registration belongs to, and a server-time helper:

```java
    /**
     * Returns every sorted set the given tunnel is indexed in. The group index
     * is included only when the tunnel was established through a balancing
     * group.
     *
     * @param registration
     *     The tunnel to index.
     *
     * @return
     *     Every index key for the given tunnel.
     */
    private List<String> indexKeys(TunnelRegistration registration) {

        List<String> keys = new ArrayList<String>(5);
        keys.add(ClusterKeys.ALL_INDEX);
        keys.add(ClusterKeys.userIndex(registration.getUsername()));
        keys.add(ClusterKeys.guacdIndex(registration.getEndpoint().toKey()));
        keys.add(ClusterKeys.connectionIndex(registration.getConnectionIdentifier()));

        if (registration.getGroupIdentifier() != null)
            keys.add(ClusterKeys.groupIndex(registration.getGroupIdentifier()));

        return keys;

    }

    /**
     * @return
     *     The current time according to the Redis server, in milliseconds.
     */
    private long serverTimeMillis(RedisCommands<String, String> commands) {
        List<String> time = commands.time();
        return (Long.parseLong(time.get(0)) * 1000L)
                + (Long.parseLong(time.get(1)) / 1000L);
    }
```

Add `import java.util.ArrayList;` alongside the others. Then replace the four stubbed methods:

```java
    @Override
    public void registerTunnel(TunnelRegistration registration) throws GuacamoleException {

        try {

            RedisCommands<String, String> commands = commands();
            long now = serverTimeMillis(commands);

            Map<String, String> record = new HashMap<String, String>();
            record.put("nodeId", registration.getNodeId());
            record.put("guacdConnectionId", registration.getGuacdConnectionId());
            record.put("guacdEndpoint", registration.getEndpoint().toKey());
            record.put("connIdentifier", registration.getConnectionIdentifier());
            record.put("username", registration.getUsername());
            record.put("startTime", Long.toString(registration.getStartTime()));

            if (registration.getGroupIdentifier() != null)
                record.put("groupIdentifier", registration.getGroupIdentifier());

            if (registration.getSharingProfileIdentifier() != null)
                record.put("sharingProfileId", registration.getSharingProfileIdentifier());

            if (registration.getRemoteHost() != null)
                record.put("remoteHost", registration.getRemoteHost());

            String tunnelKey = ClusterKeys.tunnel(registration.getTunnelUuid());
            commands.hset(tunnelKey, record);
            commands.pexpire(tunnelKey, staleWindowMs);

            for (String key : indexKeys(registration))
                commands.zadd(key, (double) now, registration.getTunnelUuid());

            if (registration.getGuacdConnectionId() != null) {
                String routeKey = ClusterKeys.route(registration.getGuacdConnectionId());
                commands.psetex(routeKey, staleWindowMs, registration.getEndpoint().toKey());
            }

            available = true;

        }
        catch (RedisException e) {
            available = false;
            throw new GuacamoleServerException("Unable to register tunnel with cluster.", e);
        }

    }

    @Override
    public void unregisterTunnel(TunnelRegistration registration) throws GuacamoleException {

        try {

            RedisCommands<String, String> commands = commands();

            for (String key : indexKeys(registration))
                commands.zrem(key, registration.getTunnelUuid());

            commands.del(ClusterKeys.tunnel(registration.getTunnelUuid()));

            if (registration.getGuacdConnectionId() != null)
                commands.del(ClusterKeys.route(registration.getGuacdConnectionId()));

            available = true;

        }
        catch (RedisException e) {
            available = false;
            throw new GuacamoleServerException("Unable to unregister tunnel from cluster.", e);
        }

    }

    @Override
    public void heartbeat(Collection<TunnelRegistration> registrations) throws GuacamoleException {

        if (registrations.isEmpty())
            return;

        try {

            RedisCommands<String, String> commands = commands();
            long now = serverTimeMillis(commands);

            for (TunnelRegistration registration : registrations) {

                for (String key : indexKeys(registration))
                    commands.zadd(key, (double) now, registration.getTunnelUuid());

                commands.pexpire(ClusterKeys.tunnel(registration.getTunnelUuid()), staleWindowMs);

                if (registration.getGuacdConnectionId() != null)
                    commands.pexpire(ClusterKeys.route(registration.getGuacdConnectionId()),
                            staleWindowMs);

            }

            available = true;

        }
        catch (RedisException e) {
            available = false;
            throw new GuacamoleServerException("Unable to refresh cluster heartbeat.", e);
        }

    }

    @Override
    public GuacdEndpoint lookupRoute(String guacdConnectionId) throws GuacamoleException {

        try {

            String value = commands().get(ClusterKeys.route(guacdConnectionId));
            available = true;

            if (value == null)
                return null;

            return GuacdEndpoint.fromKey(value);

        }
        catch (RedisException e) {
            available = false;
            throw new GuacamoleServerException("Unable to look up guacd route.", e);
        }

    }

    @Override
    public long countTunnels(GuacdEndpoint endpoint) {

        try {

            RedisCommands<String, String> commands = commands();
            long cutoff = serverTimeMillis(commands) - staleWindowMs;

            Long count = commands.zcount(ClusterKeys.guacdIndex(endpoint.toKey()),
                    io.lettuce.core.Range.from(
                            io.lettuce.core.Range.Boundary.excluding((double) cutoff),
                            io.lettuce.core.Range.Boundary.unbounded()));

            available = true;
            return count == null ? 0L : count;

        }
        catch (RedisException e) {
            available = false;
            logger.warn("Unable to count tunnels for guacd \"{}\". Treating as unloaded.",
                    endpoint, e);
            return 0L;
        }

    }
```

Note `countTunnels` returns 0 rather than throwing: the selector must keep working when Redis is down, degrading to effectively random selection.

- [ ] **Step 4: Run the tests to verify they pass**

```bash
mvn -q -pl guacamole-cluster test -Dtest=TunnelRegistrationTest
```

Expected: PASS, 10 tests.

- [ ] **Step 5: Run the whole module suite**

```bash
mvn -q -pl guacamole-cluster test
```

Expected: PASS — all tests from Tasks 1-5.

- [ ] **Step 6: Commit**

```bash
git add guacamole-cluster/src
git commit -m "feat(cluster): add tunnel registration, guacd routing, and load counting"
```

---

## Task 6: Heartbeat scheduler and the no-op store

**Files:**
- Create: `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/ClusterHeartbeat.java`
- Create: `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/NoOpClusterStore.java`
- Test: `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/ClusterHeartbeatTest.java`
- Test: `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/NoOpClusterStoreTest.java`

**Interfaces:**
- Consumes: `ClusterStore`, `TunnelRegistration`.
- Produces:
  - `ClusterHeartbeat(ClusterStore store, long intervalMs)` with `add(TunnelRegistration)`, `remove(String tunnelUuid)`, `start()`, `shutdown()`, and `getRegistrations()` returning `Collection<TunnelRegistration>`
  - `NoOpClusterStore` implementing `ClusterStore`

- [ ] **Step 1: Write the failing tests**

Create `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/ClusterHeartbeatTest.java`:

```java
package org.apache.guacamole.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.cluster.guacd.GuacdEndpoint;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClusterHeartbeatTest {

    private static final GuacdEndpoint ENDPOINT =
            new GuacdEndpoint("guacd-a", 4822, EncryptionMethod.NONE);

    /**
     * Cluster store which records every heartbeat it receives and counts down a
     * latch, so tests can wait for real scheduler activity rather than sleeping.
     */
    private static class RecordingStore extends NoOpClusterStore {

        private final List<Collection<TunnelRegistration>> beats =
                new ArrayList<Collection<TunnelRegistration>>();

        private final CountDownLatch latch;

        RecordingStore(int expectedBeats) {
            this.latch = new CountDownLatch(expectedBeats);
        }

        @Override
        public void heartbeat(Collection<TunnelRegistration> registrations) {
            synchronized (beats) {
                beats.add(new ArrayList<TunnelRegistration>(registrations));
            }
            latch.countDown();
        }

        boolean await() throws InterruptedException {
            return latch.await(10, TimeUnit.SECONDS);
        }

        Collection<TunnelRegistration> lastBeat() {
            synchronized (beats) {
                return beats.get(beats.size() - 1);
            }
        }

    }

    private TunnelRegistration registration(String uuid) {
        return new TunnelRegistration(uuid, "node-1", "$" + uuid, ENDPOINT,
                "conn-1", null, null, "alice", null, System.currentTimeMillis());
    }

    @Test
    public void heartbeatsRegisteredTunnels() throws Exception {

        RecordingStore store = new RecordingStore(2);
        ClusterHeartbeat heartbeat = new ClusterHeartbeat(store, 50L);

        heartbeat.add(registration("t1"));
        heartbeat.start();

        try {
            assertTrue(store.await(), "heartbeat did not fire");
            assertEquals(1, store.lastBeat().size());
        }
        finally {
            heartbeat.shutdown();
        }

    }

    @Test
    public void removedTunnelsAreNoLongerHeartbeated() throws Exception {

        RecordingStore store = new RecordingStore(2);
        ClusterHeartbeat heartbeat = new ClusterHeartbeat(store, 50L);

        heartbeat.add(registration("t1"));
        heartbeat.remove("t1");
        heartbeat.start();

        try {
            assertTrue(store.await(), "heartbeat did not fire");
            assertEquals(0, store.lastBeat().size());
        }
        finally {
            heartbeat.shutdown();
        }

    }

    @Test
    public void storeFailureDoesNotStopTheScheduler() throws Exception {

        final CountDownLatch attempts = new CountDownLatch(3);

        ClusterStore failing = new NoOpClusterStore() {

            @Override
            public void heartbeat(Collection<TunnelRegistration> registrations)
                    throws GuacamoleException {
                attempts.countDown();
                throw new GuacamoleException("Redis is down.");
            }

        };

        ClusterHeartbeat heartbeat = new ClusterHeartbeat(failing, 50L);
        heartbeat.add(registration("t1"));
        heartbeat.start();

        try {
            assertTrue(attempts.await(10, TimeUnit.SECONDS),
                    "scheduler stopped after a store failure");
        }
        finally {
            heartbeat.shutdown();
        }

    }

}
```

Create `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/NoOpClusterStoreTest.java`:

```java
package org.apache.guacamole.cluster;

import java.util.Arrays;
import java.util.Collections;
import org.apache.guacamole.cluster.guacd.GuacdEndpoint;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

public class NoOpClusterStoreTest {

    private static final GuacdEndpoint ENDPOINT =
            new GuacdEndpoint("guacd-a", 4822, EncryptionMethod.NONE);

    @Test
    public void alwaysGrantsSeats() throws Exception {

        NoOpClusterStore store = new NoOpClusterStore();

        SeatRequest request = new SeatRequest("t1", Arrays.asList(
                new SeatKey(ClusterKeys.connectionIndex("c"), 1,
                        SeatResult.CONNECTION_LIMIT)));

        assertEquals(SeatResult.SUCCESS, store.acquireSeats(request));
        assertEquals(SeatResult.SUCCESS, store.acquireSeats(request));

    }

    @Test
    public void reportsUnavailableSoCallersDegradeLocally() {
        assertFalse(new NoOpClusterStore().isAvailable());
    }

    @Test
    public void hasNoRoutesAndNoLoad() throws Exception {
        NoOpClusterStore store = new NoOpClusterStore();
        assertNull(store.lookupRoute("$abc"));
        assertEquals(0L, store.countTunnels(ENDPOINT));
    }

}
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
mvn -q -pl guacamole-cluster test -Dtest='ClusterHeartbeatTest,NoOpClusterStoreTest'
```

Expected: FAIL — compilation error, neither class exists.

- [ ] **Step 3: Implement `NoOpClusterStore`**

Create `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/NoOpClusterStore.java` (ASF header first):

```java
package org.apache.guacamole.cluster;

import java.util.Collection;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.cluster.guacd.GuacdEndpoint;

/**
 * Cluster store used when clustering is disabled, and as the base for test
 * doubles. Every operation succeeds and does nothing, and isAvailable() reports
 * false so that callers fall back to replica-local behavior.
 */
public class NoOpClusterStore implements ClusterStore {

    @Override
    public SeatResult acquireSeats(SeatRequest request) throws GuacamoleException {
        return SeatResult.SUCCESS;
    }

    @Override
    public void releaseSeats(SeatRequest request) throws GuacamoleException {
        // Nothing is tracked, so nothing is released
    }

    @Override
    public void registerTunnel(TunnelRegistration registration) throws GuacamoleException {
        // Nothing is published when clustering is disabled
    }

    @Override
    public void unregisterTunnel(TunnelRegistration registration) throws GuacamoleException {
        // Nothing is published when clustering is disabled
    }

    @Override
    public void heartbeat(Collection<TunnelRegistration> registrations) throws GuacamoleException {
        // Nothing is published when clustering is disabled
    }

    @Override
    public GuacdEndpoint lookupRoute(String guacdConnectionId) throws GuacamoleException {
        return null;
    }

    @Override
    public long countTunnels(GuacdEndpoint endpoint) {
        return 0L;
    }

    @Override
    public String getNodeId() {
        return "local";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void shutdown() {
        // No resources are held
    }

}
```

- [ ] **Step 4: Implement `ClusterHeartbeat`**

Create `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/ClusterHeartbeat.java` (ASF header first):

```java
package org.apache.guacamole.cluster;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Periodically refreshes the cluster-wide heartbeat score of every tunnel owned
 * by this replica. A replica that stops heartbeating has its tunnels aged out of
 * every cluster index automatically, which is what frees seats and clears the
 * admin view after a crash.
 */
public class ClusterHeartbeat {

    private static final Logger logger = LoggerFactory.getLogger(ClusterHeartbeat.class);

    private final ClusterStore store;
    private final long intervalMs;

    private final Map<String, TunnelRegistration> registrations =
            new ConcurrentHashMap<String, TunnelRegistration>();

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable, "guacamole-cluster-heartbeat");
                    thread.setDaemon(true);
                    return thread;
                }

            });

    /**
     * @param store
     *     The cluster store to refresh against.
     *
     * @param intervalMs
     *     Milliseconds between heartbeats. Must be well below the stale window.
     */
    public ClusterHeartbeat(ClusterStore store, long intervalMs) {
        this.store = store;
        this.intervalMs = intervalMs;
    }

    public void add(TunnelRegistration registration) {
        registrations.put(registration.getTunnelUuid(), registration);
    }

    public void remove(String tunnelUuid) {
        registrations.remove(tunnelUuid);
    }

    public Collection<TunnelRegistration> getRegistrations() {
        return registrations.values();
    }

    /**
     * Begins heartbeating. A failure to reach the store is logged and the
     * schedule continues; throwing out of the scheduled task would silently
     * cancel all future heartbeats.
     */
    public void start() {
        executor.scheduleWithFixedDelay(new Runnable() {

            @Override
            public void run() {
                try {
                    store.heartbeat(registrations.values());
                }
                catch (Throwable e) {
                    logger.warn("Cluster heartbeat failed. Tunnels owned by this node "
                            + "may age out of the cluster if this persists.", e);
                }
            }

        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        executor.shutdownNow();
    }

}
```

- [ ] **Step 5: Run the tests to verify they pass**

```bash
mvn -q -pl guacamole-cluster test -Dtest='ClusterHeartbeatTest,NoOpClusterStoreTest'
```

Expected: PASS, 6 tests.

- [ ] **Step 6: Commit**

```bash
git add guacamole-cluster/src
git commit -m "feat(cluster): add heartbeat scheduler and no-op cluster store"
```

---

## Task 7: guacd pool and selector

**Files:**
- Create: `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/guacd/GuacdPool.java`
- Create: `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/guacd/GuacdSelector.java`
- Test: `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/guacd/GuacdPoolTest.java`
- Test: `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/guacd/GuacdSelectorTest.java`

**Interfaces:**
- Consumes: `ClusterStore`, `GuacdEndpoint`.
- Produces:
  - `GuacdPool` — abstract `getCandidates()` returning `List<GuacdEndpoint>`; concrete `StaticGuacdPool(List<GuacdEndpoint>)` and `DnsGuacdPool(String hostname, int port, EncryptionMethod method, long cacheMs)`
  - `GuacdSelector(ClusterStore store, GuacdPool pool, long circuitBreakMs)` with `selectForJoin(String guacdConnectionId)`, `selectForNew()`, and `markFailed(GuacdEndpoint)`

- [ ] **Step 1: Write the failing pool test**

Create `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/guacd/GuacdPoolTest.java`:

```java
package org.apache.guacamole.cluster.guacd;

import java.util.List;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GuacdPoolTest {

    @Test
    public void staticPoolParsesAConfiguredHostList() throws Exception {

        GuacdPool pool = GuacdPool.fromHostList("guacd-a:4822, guacd-b:4823",
                4822, EncryptionMethod.NONE);

        List<GuacdEndpoint> candidates = pool.getCandidates();

        assertEquals(2, candidates.size());
        assertEquals(new GuacdEndpoint("guacd-a", 4822, EncryptionMethod.NONE),
                candidates.get(0));
        assertEquals(new GuacdEndpoint("guacd-b", 4823, EncryptionMethod.NONE),
                candidates.get(1));

    }

    @Test
    public void staticPoolAppliesTheDefaultPortWhenOmitted() throws Exception {

        GuacdPool pool = GuacdPool.fromHostList("guacd-a", 4822, EncryptionMethod.NONE);

        assertEquals(new GuacdEndpoint("guacd-a", 4822, EncryptionMethod.NONE),
                pool.getCandidates().get(0));

    }

    @Test
    public void dnsPoolResolvesLocalhostToAtLeastOneEndpoint() throws Exception {

        GuacdPool pool = GuacdPool.fromDns("localhost", 4822, EncryptionMethod.NONE, 1000L);

        List<GuacdEndpoint> candidates = pool.getCandidates();

        assertTrue(!candidates.isEmpty(), "expected at least one resolved address");
        assertEquals(4822, candidates.get(0).getPort());

    }

}
```

- [ ] **Step 2: Run it to verify it fails**

```bash
mvn -q -pl guacamole-cluster test -Dtest=GuacdPoolTest
```

Expected: FAIL — compilation error, `GuacdPool` does not exist.

- [ ] **Step 3: Implement `GuacdPool`**

Create `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/guacd/GuacdPool.java` (ASF header first):

```java
package org.apache.guacamole.cluster.guacd;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;

/**
 * The set of guacd instances this replica may connect to.
 *
 * Membership is owned by the platform, not by Guacamole: a DNS-backed pool
 * resolves a Kubernetes headless Service, which contains only ready pods.
 *
 * Note on DNS caching: the JVM caches successful lookups for 30 seconds by
 * default. The cache duration here therefore sets a floor, not a ceiling. In
 * Kubernetes, set -Dnetworkaddress.cache.ttl=5 (or the equivalent entry in
 * java.security) so that pod churn is observed promptly.
 */
public abstract class GuacdPool {

    /**
     * @return
     *     Every guacd instance currently believed to be available.
     *
     * @throws GuacamoleException
     *     If the set of candidates cannot be determined.
     */
    public abstract List<GuacdEndpoint> getCandidates() throws GuacamoleException;

    /**
     * Creates a pool from a comma-separated list of "host" or "host:port"
     * entries.
     *
     * @param hostList
     *     The configured list.
     *
     * @param defaultPort
     *     Port to use for entries which omit one.
     *
     * @param encryptionMethod
     *     Encryption method required by every instance in the pool.
     *
     * @return
     *     A pool over the configured hosts.
     */
    public static GuacdPool fromHostList(String hostList, final int defaultPort,
            final EncryptionMethod encryptionMethod) {

        final List<GuacdEndpoint> endpoints = new ArrayList<GuacdEndpoint>();

        for (String entry : hostList.split(",")) {

            String trimmed = entry.trim();
            if (trimmed.isEmpty())
                continue;

            int separator = trimmed.lastIndexOf(':');

            // Treat a trailing ":digits" as a port; anything else (including a
            // bare IPv6 literal) is a hostname
            if (separator > 0 && trimmed.substring(separator + 1).matches("\\d+"))
                endpoints.add(new GuacdEndpoint(
                        trimmed.substring(0, separator),
                        Integer.parseInt(trimmed.substring(separator + 1)),
                        encryptionMethod));
            else
                endpoints.add(new GuacdEndpoint(trimmed, defaultPort, encryptionMethod));

        }

        return new GuacdPool() {

            @Override
            public List<GuacdEndpoint> getCandidates() {
                return Collections.unmodifiableList(endpoints);
            }

        };

    }

    /**
     * Creates a pool which resolves every address of a hostname, intended for
     * use against a Kubernetes headless Service.
     *
     * @param hostname
     *     The hostname to resolve.
     *
     * @param port
     *     Port on which every resolved instance listens.
     *
     * @param encryptionMethod
     *     Encryption method required by every instance in the pool.
     *
     * @param cacheMs
     *     Milliseconds to reuse a resolution before resolving again.
     *
     * @return
     *     A pool over every address of the given hostname.
     */
    public static GuacdPool fromDns(final String hostname, final int port,
            final EncryptionMethod encryptionMethod, final long cacheMs) {

        return new GuacdPool() {

            private volatile List<GuacdEndpoint> cached = Collections.emptyList();
            private volatile long resolvedAt = 0L;

            @Override
            public synchronized List<GuacdEndpoint> getCandidates()
                    throws GuacamoleException {

                long now = System.currentTimeMillis();
                if (!cached.isEmpty() && now - resolvedAt < cacheMs)
                    return cached;

                try {

                    InetAddress[] addresses = InetAddress.getAllByName(hostname);
                    List<GuacdEndpoint> endpoints =
                            new ArrayList<GuacdEndpoint>(addresses.length);

                    for (InetAddress address : addresses)
                        endpoints.add(new GuacdEndpoint(address.getHostAddress(), port,
                                encryptionMethod));

                    cached = Collections.unmodifiableList(endpoints);
                    resolvedAt = now;
                    return cached;

                }
                catch (UnknownHostException e) {

                    // Prefer a stale answer to no answer: a transient DNS
                    // failure must not take down connection establishment
                    if (!cached.isEmpty())
                        return cached;

                    throw new GuacamoleServerException(
                            "Unable to resolve guacd hostname \"" + hostname + "\".", e);

                }

            }

        };

    }

}
```

- [ ] **Step 4: Run the pool test to verify it passes**

```bash
mvn -q -pl guacamole-cluster test -Dtest=GuacdPoolTest
```

Expected: PASS, 3 tests.

- [ ] **Step 5: Write the failing selector test**

Create `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/guacd/GuacdSelectorTest.java`:

```java
package org.apache.guacamole.cluster.guacd;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.cluster.NoOpClusterStore;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GuacdSelectorTest {

    private static final GuacdEndpoint A =
            new GuacdEndpoint("guacd-a", 4822, EncryptionMethod.NONE);

    private static final GuacdEndpoint B =
            new GuacdEndpoint("guacd-b", 4822, EncryptionMethod.NONE);

    private static final GuacdEndpoint C =
            new GuacdEndpoint("guacd-c", 4822, EncryptionMethod.NONE);

    /**
     * Cluster store with fixed per-endpoint load and a fixed route table.
     */
    private static class FakeStore extends NoOpClusterStore {

        private final Map<String, Long> load = new HashMap<String, Long>();
        private final Map<String, GuacdEndpoint> routes = new HashMap<String, GuacdEndpoint>();

        void setLoad(GuacdEndpoint endpoint, long count) {
            load.put(endpoint.toKey(), count);
        }

        void setRoute(String connectionId, GuacdEndpoint endpoint) {
            routes.put(connectionId, endpoint);
        }

        @Override
        public long countTunnels(GuacdEndpoint endpoint) {
            Long count = load.get(endpoint.toKey());
            return count == null ? 0L : count;
        }

        @Override
        public GuacdEndpoint lookupRoute(String guacdConnectionId) {
            return routes.get(guacdConnectionId);
        }

    }

    private GuacdPool poolOf(final List<GuacdEndpoint> endpoints) {
        return new GuacdPool() {

            @Override
            public List<GuacdEndpoint> getCandidates() {
                return endpoints;
            }

        };
    }

    @Test
    public void selectsTheLeastLoadedEndpoint() throws Exception {

        FakeStore store = new FakeStore();
        store.setLoad(A, 10L);
        store.setLoad(B, 2L);
        store.setLoad(C, 7L);

        GuacdSelector selector = new GuacdSelector(store,
                poolOf(Arrays.asList(A, B, C)), 15000L);

        assertEquals(B, selector.selectForNew());

    }

    @Test
    public void routesAJoinToTheOwningEndpoint() throws Exception {

        FakeStore store = new FakeStore();
        store.setLoad(A, 100L);
        store.setRoute("$abc", A);

        GuacdSelector selector = new GuacdSelector(store,
                poolOf(Arrays.asList(A, B)), 15000L);

        // Load is irrelevant: the join must go where the connection lives
        assertEquals(A, selector.selectForJoin("$abc"));

    }

    @Test
    public void failsClosedWhenAJoinTargetIsGone() {

        FakeStore store = new FakeStore();

        GuacdSelector selector = new GuacdSelector(store,
                poolOf(Arrays.asList(A, B)), 15000L);

        assertThrows(GuacamoleException.class, new org.junit.jupiter.api.function.Executable() {

            @Override
            public void execute() throws Throwable {
                selector.selectForJoin("$vanished");
            }

        });

    }

    @Test
    public void skipsCircuitBrokenEndpoints() throws Exception {

        FakeStore store = new FakeStore();
        store.setLoad(A, 0L);
        store.setLoad(B, 5L);

        GuacdSelector selector = new GuacdSelector(store,
                poolOf(Arrays.asList(A, B)), 15000L);

        // A is least loaded, but has just failed
        selector.markFailed(A);

        assertEquals(B, selector.selectForNew());

    }

    @Test
    public void recoversAfterTheCircuitBreakExpires() throws Exception {

        FakeStore store = new FakeStore();
        store.setLoad(A, 0L);
        store.setLoad(B, 5L);

        GuacdSelector selector = new GuacdSelector(store,
                poolOf(Arrays.asList(A, B)), 50L);

        selector.markFailed(A);
        Thread.sleep(120L);

        assertEquals(A, selector.selectForNew());

    }

    @Test
    public void usesAllEndpointsWhenEveryOneIsCircuitBroken() throws Exception {

        FakeStore store = new FakeStore();

        GuacdSelector selector = new GuacdSelector(store,
                poolOf(Arrays.asList(A, B)), 15000L);

        selector.markFailed(A);
        selector.markFailed(B);

        // Refusing to connect at all would be worse than trying a suspect host
        GuacdEndpoint selected = selector.selectForNew();
        assertTrue(A.equals(selected) || B.equals(selected));

    }

    @Test
    public void failsWhenThePoolIsEmpty() {

        GuacdSelector selector = new GuacdSelector(new FakeStore(),
                poolOf(java.util.Collections.<GuacdEndpoint>emptyList()), 15000L);

        assertThrows(GuacamoleException.class, new org.junit.jupiter.api.function.Executable() {

            @Override
            public void execute() throws Throwable {
                selector.selectForNew();
            }

        });

    }

}
```

- [ ] **Step 6: Run it to verify it fails**

```bash
mvn -q -pl guacamole-cluster test -Dtest=GuacdSelectorTest
```

Expected: FAIL — compilation error, `GuacdSelector` does not exist.

- [ ] **Step 7: Implement `GuacdSelector`**

Create `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/guacd/GuacdSelector.java` (ASF header first):

```java
package org.apache.guacamole.cluster.guacd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleResourceNotFoundException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.cluster.ClusterStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chooses which guacd instance a connection should be established against.
 */
public class GuacdSelector {

    private static final Logger logger = LoggerFactory.getLogger(GuacdSelector.class);

    private final ClusterStore store;
    private final GuacdPool pool;
    private final long circuitBreakMs;
    private final Random random = new Random();

    /**
     * Endpoint key to the time at which its circuit break expires.
     */
    private final Map<String, Long> brokenUntil = new ConcurrentHashMap<String, Long>();

    public GuacdSelector(ClusterStore store, GuacdPool pool, long circuitBreakMs) {
        this.store = store;
        this.pool = pool;
        this.circuitBreakMs = circuitBreakMs;
    }

    /**
     * Records that connecting to the given endpoint failed, excluding it from
     * selection until its circuit break expires.
     *
     * @param endpoint
     *     The endpoint which could not be reached.
     */
    public void markFailed(GuacdEndpoint endpoint) {
        brokenUntil.put(endpoint.toKey(), System.currentTimeMillis() + circuitBreakMs);
        logger.info("guacd \"{}\" failed and will be skipped for {} ms.",
                endpoint, circuitBreakMs);
    }

    private boolean isBroken(GuacdEndpoint endpoint) {
        Long until = brokenUntil.get(endpoint.toKey());
        return until != null && System.currentTimeMillis() < until;
    }

    /**
     * Returns the guacd instance already hosting the given connection.
     *
     * @param guacdConnectionId
     *     The connection ID issued by guacd.
     *
     * @return
     *     The guacd instance hosting that connection.
     *
     * @throws GuacamoleException
     *     If no live route exists, or the route cannot be read. This fails
     *     closed deliberately: connecting elsewhere would silently open a NEW
     *     session rather than joining the intended one.
     */
    public GuacdEndpoint selectForJoin(String guacdConnectionId) throws GuacamoleException {

        GuacdEndpoint endpoint = store.lookupRoute(guacdConnectionId);
        if (endpoint == null)
            throw new GuacamoleResourceNotFoundException(
                    "The connection being joined is no longer available.");

        return endpoint;

    }

    /**
     * Returns the least-loaded available guacd instance.
     *
     * @return
     *     The guacd instance which should host a new connection.
     *
     * @throws GuacamoleException
     *     If the pool contains no instances at all.
     */
    public GuacdEndpoint selectForNew() throws GuacamoleException {

        List<GuacdEndpoint> candidates = pool.getCandidates();
        if (candidates.isEmpty())
            throw new GuacamoleServerException("No guacd instances are available.");

        List<GuacdEndpoint> usable = new ArrayList<GuacdEndpoint>(candidates.size());
        for (GuacdEndpoint candidate : candidates) {
            if (!isBroken(candidate))
                usable.add(candidate);
        }

        // Trying a suspect instance beats refusing every connection
        if (usable.isEmpty()) {
            logger.warn("Every guacd instance is currently circuit-broken. "
                    + "Selecting from the full pool regardless.");
            usable = new ArrayList<GuacdEndpoint>(candidates);
        }

        // Shuffle first so that equally loaded instances are chosen evenly
        // rather than always favouring the first in DNS order
        Collections.shuffle(usable, random);

        GuacdEndpoint best = null;
        long bestLoad = Long.MAX_VALUE;

        for (GuacdEndpoint candidate : usable) {
            long load = store.countTunnels(candidate);
            if (load < bestLoad) {
                bestLoad = load;
                best = candidate;
            }
        }

        return best;

    }

}
```

- [ ] **Step 8: Run the selector test to verify it passes**

```bash
mvn -q -pl guacamole-cluster test -Dtest=GuacdSelectorTest
```

Expected: PASS, 7 tests.

- [ ] **Step 9: Run the full module suite**

```bash
mvn -q -pl guacamole-cluster test
```

Expected: PASS — everything from Tasks 1-7.

- [ ] **Step 10: Commit**

```bash
git add guacamole-cluster/src
git commit -m "feat(cluster): add guacd pool resolution and least-loaded selector"
```

---

## Task 8: Configuration properties and Guice wiring

**Files:**
- Create: `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/ClusterProperties.java`
- Create: `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/ClusterModule.java`
- Test: `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/ClusterModuleTest.java`
- Modify: `extensions/guacamole-auth-jdbc/modules/guacamole-auth-jdbc-base/pom.xml`
- Modify: `extensions/guacamole-auth-jdbc/modules/guacamole-auth-jdbc-base/src/main/java/org/apache/guacamole/auth/jdbc/JDBCAuthenticationProviderModule.java`

**Interfaces:**
- Consumes: `ClusterStore`, `NoOpClusterStore`, `RedisClusterStore`, `ClusterHeartbeat`, `GuacdPool`, `GuacdSelector`.
- Produces: `ClusterModule(Environment environment)` — a Guice `AbstractModule` binding `ClusterStore`, `ClusterHeartbeat`, `GuacdPool`, and `GuacdSelector` as singletons. Also `ClusterModule.isEnabled(Environment)` returning `boolean`.

- [ ] **Step 1: Write the failing test**

Create `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/ClusterModuleTest.java`:

```java
package org.apache.guacamole.cluster;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.properties.GuacamoleProperty;
import org.apache.guacamole.protocol.GuacamoleProxyConfiguration;
import org.apache.guacamole.protocols.ProtocolInfo;
import org.apache.guacamole.cluster.guacd.GuacdSelector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClusterModuleTest {

    /**
     * Minimal Environment backed by an in-memory property map. Only the methods
     * the cluster module actually calls are implemented; everything else is
     * unreachable from this code path.
     */
    private static class MapEnvironment implements Environment {

        private final Map<String, String> properties;

        MapEnvironment(Map<String, String> properties) {
            this.properties = properties;
        }

        @Override
        public File getGuacamoleHome() {
            return new File(System.getProperty("java.io.tmpdir"));
        }

        @Override
        public Map<String, ProtocolInfo> getProtocols() {
            return new HashMap<String, ProtocolInfo>();
        }

        @Override
        public ProtocolInfo getProtocol(String name) {
            return null;
        }

        @Override
        public GuacamoleProxyConfiguration getDefaultGuacamoleProxyConfiguration() {
            return new GuacamoleProxyConfiguration("localhost", 4822,
                    EncryptionMethod.NONE);
        }

        @Override
        public <Type> Type getProperty(GuacamoleProperty<Type> property)
                throws GuacamoleException {
            return property.parseValue(properties.get(property.getName()));
        }

        @Override
        public <Type> Type getProperty(GuacamoleProperty<Type> property, Type defaultValue)
                throws GuacamoleException {
            Type value = getProperty(property);
            return value == null ? defaultValue : value;
        }

        @Override
        public <Type> Type getRequiredProperty(GuacamoleProperty<Type> property)
                throws GuacamoleException {
            Type value = getProperty(property);
            if (value == null)
                throw new GuacamoleException("Missing required property: "
                        + property.getName());
            return value;
        }

    }

    @Test
    public void clusteringIsDisabledByDefault() throws Exception {
        Environment environment = new MapEnvironment(new HashMap<String, String>());
        assertFalse(ClusterModule.isEnabled(environment));
    }

    @Test
    public void bindsTheNoOpStoreWhenDisabled() throws Exception {

        Environment environment = new MapEnvironment(new HashMap<String, String>());
        Injector injector = Guice.createInjector(new ClusterModule(environment));

        assertTrue(injector.getInstance(ClusterStore.class) instanceof NoOpClusterStore);
        assertNotNull(injector.getInstance(GuacdSelector.class));

    }

    @Test
    public void enablesClusteringWhenConfigured() throws Exception {

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("cluster-enabled", "true");
        properties.put("cluster-redis-uri", "redis://localhost:6379");
        properties.put("guacd-cluster-hosts", "guacd-a:4822,guacd-b:4822");

        assertTrue(ClusterModule.isEnabled(new MapEnvironment(properties)));

    }

}
```

`Environment` declares exactly seven abstract methods — `getGuacamoleHome()`, `getProtocols()`,
`getProtocol(String)`, both `getProperty(...)` overloads, `getRequiredProperty(...)`, and
`getDefaultGuacamoleProxyConfiguration()`. Everything else on the interface is a `default`
method. All seven are implemented above.

Confirm the import package for `GuacamoleProxyConfiguration` and `ProtocolInfo` before
compiling — `Environment.java` imports both, so copy the import lines from
`guacamole-ext/src/main/java/org/apache/guacamole/environment/Environment.java` verbatim if the
ones above do not resolve.

- [ ] **Step 2: Run it to verify it fails**

```bash
mvn -q -pl guacamole-cluster test -Dtest=ClusterModuleTest
```

Expected: FAIL — compilation error, `ClusterProperties` and `ClusterModule` do not exist.

- [ ] **Step 3: Implement `ClusterProperties`**

Create `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/ClusterProperties.java` (ASF header first). This follows the anonymous-subclass pattern used at `BanningAuthenticationListener.java:54`:

```java
package org.apache.guacamole.cluster;

import org.apache.guacamole.properties.BooleanGuacamoleProperty;
import org.apache.guacamole.properties.IntegerGuacamoleProperty;
import org.apache.guacamole.properties.LongGuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;

/**
 * Every guacamole.properties key read by the cluster module.
 */
public class ClusterProperties {

    private ClusterProperties() {}

    /**
     * Whether cluster coordination is active. When false, every code path
     * behaves exactly as unmodified upstream Guacamole.
     */
    public static final BooleanGuacamoleProperty CLUSTER_ENABLED =
            new BooleanGuacamoleProperty() {

        @Override
        public String getName() {
            return "cluster-enabled";
        }

    };

    /**
     * Lettuce URI of the Redis server, for example "redis://host:6379" or
     * "rediss://host:6379" for TLS.
     */
    public static final StringGuacamoleProperty CLUSTER_REDIS_URI =
            new StringGuacamoleProperty() {

        @Override
        public String getName() {
            return "cluster-redis-uri";
        }

    };

    /**
     * Identity of this replica. Defaults to the HOSTNAME environment variable
     * plus a random suffix.
     */
    public static final StringGuacamoleProperty CLUSTER_NODE_ID =
            new StringGuacamoleProperty() {

        @Override
        public String getName() {
            return "cluster-node-id";
        }

    };

    /**
     * Milliseconds between heartbeats.
     */
    public static final LongGuacamoleProperty CLUSTER_HEARTBEAT_INTERVAL =
            new LongGuacamoleProperty() {

        @Override
        public String getName() {
            return "cluster-heartbeat-interval";
        }

    };

    /**
     * Milliseconds after which an unrefreshed cluster entry is considered dead.
     */
    public static final LongGuacamoleProperty CLUSTER_STALE_WINDOW =
            new LongGuacamoleProperty() {

        @Override
        public String getName() {
            return "cluster-stale-window";
        }

    };

    /**
     * Comma-separated list of "host" or "host:port" guacd instances.
     */
    public static final StringGuacamoleProperty GUACD_CLUSTER_HOSTS =
            new StringGuacamoleProperty() {

        @Override
        public String getName() {
            return "guacd-cluster-hosts";
        }

    };

    /**
     * Hostname resolving to every available guacd instance, such as a
     * Kubernetes headless Service.
     */
    public static final StringGuacamoleProperty GUACD_CLUSTER_DNS =
            new StringGuacamoleProperty() {

        @Override
        public String getName() {
            return "guacd-cluster-dns";
        }

    };

    /**
     * Port on which pooled guacd instances listen.
     */
    public static final IntegerGuacamoleProperty GUACD_CLUSTER_PORT =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() {
            return "guacd-cluster-port";
        }

    };

    /**
     * Milliseconds a guacd instance is skipped after a failed connection.
     */
    public static final LongGuacamoleProperty GUACD_CIRCUIT_BREAK_DURATION =
            new LongGuacamoleProperty() {

        @Override
        public String getName() {
            return "guacd-circuit-break-duration";
        }

    };

}
```

`BooleanGuacamoleProperty`, `IntegerGuacamoleProperty`, `LongGuacamoleProperty`, and
`StringGuacamoleProperty` all exist in `guacamole-ext/src/main/java/org/apache/guacamole/properties/`.

**Redis authentication and TLS** are carried in the URI rather than in separate properties:
`rediss://:password@host:6379` enables TLS and supplies the password, and
`redis-sentinel://:password@host:26379?sentinelMasterId=mymaster` addresses a Sentinel set.
Lettuce parses both. Dedicated `cluster-redis-password` and `cluster-redis-sentinel-master`
properties are deferred to P5 hardening.

- [ ] **Step 4: Implement `ClusterModule`**

Create `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/ClusterModule.java` (ASF header first):

```java
package org.apache.guacamole.cluster;

import com.google.inject.AbstractModule;
import java.util.UUID;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.cluster.guacd.GuacdPool;
import org.apache.guacamole.cluster.guacd.GuacdSelector;
import org.apache.guacamole.cluster.redis.RedisClusterStore;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration;
import org.apache.guacamole.net.auth.GuacamoleProxyConfiguration.EncryptionMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guice bindings for cluster coordination. When clustering is disabled, a
 * no-op store and a single-endpoint pool are bound, so every consumer can
 * depend on these types unconditionally.
 */
public class ClusterModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(ClusterModule.class);

    private static final long DEFAULT_HEARTBEAT_INTERVAL = 10000L;
    private static final long DEFAULT_STALE_WINDOW = 30000L;
    private static final long DEFAULT_CIRCUIT_BREAK = 15000L;
    private static final int DEFAULT_GUACD_PORT = 4822;
    private static final long DNS_CACHE_MS = 5000L;

    private final Environment environment;

    public ClusterModule(Environment environment) {
        this.environment = environment;
    }

    /**
     * @param environment
     *     The Guacamole server environment.
     *
     * @return
     *     true if cluster coordination is enabled.
     *
     * @throws GuacamoleException
     *     If the property cannot be read.
     */
    public static boolean isEnabled(Environment environment) throws GuacamoleException {
        return environment.getProperty(ClusterProperties.CLUSTER_ENABLED, false);
    }

    private String resolveNodeId() throws GuacamoleException {

        String configured = environment.getProperty(ClusterProperties.CLUSTER_NODE_ID);
        if (configured != null)
            return configured;

        String hostname = System.getenv("HOSTNAME");
        if (hostname == null)
            hostname = "guacamole";

        return hostname + "-" + UUID.randomUUID().toString().substring(0, 8);

    }

    private GuacdPool buildPool() throws GuacamoleException {

        int port = environment.getProperty(ClusterProperties.GUACD_CLUSTER_PORT,
                DEFAULT_GUACD_PORT);

        EncryptionMethod encryptionMethod =
                environment.getDefaultGuacamoleProxyConfiguration().getEncryptionMethod();

        String dns = environment.getProperty(ClusterProperties.GUACD_CLUSTER_DNS);
        if (dns != null)
            return GuacdPool.fromDns(dns, port, encryptionMethod, DNS_CACHE_MS);

        String hosts = environment.getProperty(ClusterProperties.GUACD_CLUSTER_HOSTS);
        if (hosts != null)
            return GuacdPool.fromHostList(hosts, port, encryptionMethod);

        // Fall back to the single configured guacd, which makes an unconfigured
        // pool behave exactly like unmodified Guacamole
        GuacamoleProxyConfiguration fallback =
                environment.getDefaultGuacamoleProxyConfiguration();

        return GuacdPool.fromHostList(
                fallback.getHostname() + ":" + fallback.getPort(),
                fallback.getPort(), fallback.getEncryptionMethod());

    }

    @Override
    protected void configure() {

        try {

            GuacdPool pool = buildPool();
            long circuitBreak = environment.getProperty(
                    ClusterProperties.GUACD_CIRCUIT_BREAK_DURATION, DEFAULT_CIRCUIT_BREAK);

            ClusterStore store;
            ClusterHeartbeat heartbeat;

            if (isEnabled(environment)) {

                String uri = environment.getRequiredProperty(
                        ClusterProperties.CLUSTER_REDIS_URI);

                long staleWindow = environment.getProperty(
                        ClusterProperties.CLUSTER_STALE_WINDOW, DEFAULT_STALE_WINDOW);

                long interval = environment.getProperty(
                        ClusterProperties.CLUSTER_HEARTBEAT_INTERVAL,
                        DEFAULT_HEARTBEAT_INTERVAL);

                store = new RedisClusterStore(uri, staleWindow, resolveNodeId());
                heartbeat = new ClusterHeartbeat(store, interval);
                heartbeat.start();

                logger.info("Cluster coordination is ENABLED against \"{}\".", uri);

            }
            else {
                store = new NoOpClusterStore();
                heartbeat = new ClusterHeartbeat(store, DEFAULT_HEARTBEAT_INTERVAL);
                logger.debug("Cluster coordination is disabled.");
            }

            bind(ClusterStore.class).toInstance(store);
            bind(ClusterHeartbeat.class).toInstance(heartbeat);
            bind(GuacdPool.class).toInstance(pool);
            bind(GuacdSelector.class)
                    .toInstance(new GuacdSelector(store, pool, circuitBreak));

        }
        catch (GuacamoleException e) {
            addError(new GuacamoleServerException(
                    "Unable to configure cluster coordination.", e));
        }

    }

}
```

- [ ] **Step 5: Run the test to verify it passes**

```bash
mvn -q -pl guacamole-cluster test -Dtest=ClusterModuleTest
```

Expected: PASS, 3 tests.

- [ ] **Step 6: Add the dependency to the JDBC extension**

In `extensions/guacamole-auth-jdbc/modules/guacamole-auth-jdbc-base/pom.xml`, inside `<dependencies>`, immediately after the existing `guacamole-ext` dependency (around line 51):

```xml
        <!-- Cluster coordination -->
        <dependency>
            <groupId>org.apache.guacamole</groupId>
            <artifactId>guacamole-cluster</artifactId>
            <version>${revision}</version>
            <scope>compile</scope>
        </dependency>
```

`guacamole-cluster` declares `guacamole-ext` as `provided`, so this does not duplicate it on the classpath.

- [ ] **Step 7: Install the cluster module into the JDBC Guice injector**

In `JDBCAuthenticationProviderModule.java`, add the import:

```java
import org.apache.guacamole.cluster.ClusterModule;
```

Then, inside the existing `configure()` method, immediately before the line binding `SharedConnectionMap` (line 197), add:

```java
        // Cluster coordination (no-op unless cluster-enabled is true)
        install(new ClusterModule(environment));
```

The field is `private final JDBCEnvironment environment;`
(`JDBCAuthenticationProviderModule.java:107`), assigned in the constructor at `:118` and already
used inside `configure()` at `:131` and `:162`, so it is in scope. `JDBCEnvironment` implements
`Environment`, which is what `ClusterModule` takes.

- [ ] **Step 8: Verify the JDBC extension still builds**

```bash
mvn -q -pl extensions/guacamole-auth-jdbc/modules/guacamole-auth-jdbc-base -am install -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 9: Commit**

```bash
git add guacamole-cluster/src \
        extensions/guacamole-auth-jdbc/modules/guacamole-auth-jdbc-base/pom.xml \
        extensions/guacamole-auth-jdbc/modules/guacamole-auth-jdbc-base/src/main/java/org/apache/guacamole/auth/jdbc/JDBCAuthenticationProviderModule.java
git commit -m "feat(cluster): add cluster configuration properties and Guice wiring"
```

---

## Task 9: Wire guacd selection and tunnel registration into the tunnel service

**Files:**
- Modify: `extensions/guacamole-auth-jdbc/modules/guacamole-auth-jdbc-base/src/main/java/org/apache/guacamole/auth/jdbc/tunnel/AbstractGuacamoleTunnelService.java`

**Interfaces:**
- Consumes: `GuacdSelector`, `ClusterStore`, `ClusterHeartbeat`, `TunnelRegistration`, `GuacdEndpoint`.
- Produces: no new public API. `AbstractGuacamoleTunnelService` now selects a guacd instance per connection and publishes each tunnel to the cluster.

This is the P1 payload. Everything before it was foundation with no behavior change.

- [ ] **Step 1: Add the injected dependencies**

In `AbstractGuacamoleTunnelService.java`, add the imports:

```java
import org.apache.guacamole.cluster.ClusterHeartbeat;
import org.apache.guacamole.cluster.ClusterStore;
import org.apache.guacamole.cluster.TunnelRegistration;
import org.apache.guacamole.cluster.guacd.GuacdEndpoint;
import org.apache.guacamole.cluster.guacd.GuacdSelector;
```

Immediately after the existing `@Inject private JDBCEnvironment environment;` field (line 176-177), add:

```java
    /**
     * Selects which guacd instance each connection should be established
     * against.
     */
    @Inject
    private GuacdSelector guacdSelector;

    /**
     * Cluster-wide state shared with every other Guacamole replica.
     */
    @Inject
    private ClusterStore clusterStore;

    /**
     * Heartbeat keeping this replica's tunnels alive in the cluster indexes.
     */
    @Inject
    private ClusterHeartbeat clusterHeartbeat;
```

- [ ] **Step 2: Add the endpoint selection helper**

Add this private method to `AbstractGuacamoleTunnelService`, directly above `getUnconfiguredGuacamoleSocket` (line 334):

```java
    /**
     * Determines which guacd instance the given active connection should be
     * established against.
     *
     * Selection is ordered deliberately:
     *
     *   1. If an existing guacd connection is being joined, the join MUST go to
     *      the guacd instance already hosting it. If that instance is no longer
     *      known, this fails rather than silently opening a new session.
     *   2. If the connection pins a specific guacd in the database, that pin is
     *      honored, preserving upstream behavior for deliberately pinned
     *      connections.
     *   3. Otherwise the least-loaded instance in the pool is chosen.
     *
     * @param activeConnection
     *     The connection record being established.
     *
     * @param connection
     *     The connection being established.
     *
     * @return
     *     The guacd instance to connect to.
     *
     * @throws GuacamoleException
     *     If a join target has vanished, or no guacd instance is available.
     */
    private GuacdEndpoint selectGuacdEndpoint(ActiveConnectionRecord activeConnection,
            ModeledConnection connection) throws GuacamoleException {

        // Joining an existing guacd connection: route to its owner
        String joinId = activeConnection.getConnectionID();
        if (joinId != null)
            return guacdSelector.selectForJoin(joinId);

        // Explicitly pinned to a specific guacd in the database
        if (connection.getModel().getProxyHostname() != null)
            return GuacdEndpoint.from(connection.getGuacamoleProxyConfiguration());

        // Otherwise balance across the pool
        return guacdSelector.selectForNew();

    }
```

- [ ] **Step 3: Replace the fixed proxy configuration at the socket call site**

At `AbstractGuacamoleTunnelService.java:552-555`, replace:

```java
            // Obtain socket which will automatically run the cleanup task
            ConfiguredGuacamoleSocket socket = new ConfiguredGuacamoleSocket(
                getUnconfiguredGuacamoleSocket(connection.getGuacamoleProxyConfiguration(),
                        cleanupTask), config, info);
```

with:

```java
            // Select the guacd instance which should host this connection
            GuacdEndpoint endpoint = selectGuacdEndpoint(activeConnection, connection);

            // Obtain socket which will automatically run the cleanup task
            ConfiguredGuacamoleSocket socket;
            try {
                socket = new ConfiguredGuacamoleSocket(
                    getUnconfiguredGuacamoleSocket(endpoint.toProxyConfiguration(),
                            cleanupTask), config, info);
            }

            // Record the failure so a dying guacd is not selected again
            // immediately, then let the existing handling take over
            catch (GuacamoleException e) {
                guacdSelector.markFailed(endpoint);
                throw e;
            }

            // Publish this tunnel to the cluster, making it visible to other
            // replicas and routable by its guacd connection ID
            TunnelRegistration registration = new TunnelRegistration(
                    activeConnection.getUUID().toString(),
                    clusterStore.getNodeId(),
                    socket.getConnectionID(),
                    endpoint,
                    connection.getIdentifier(),
                    activeConnection.hasBalancingGroup()
                            ? activeConnection.getBalancingGroup().getIdentifier() : null,
                    activeConnection.getSharingProfile() != null
                            ? activeConnection.getSharingProfile().getIdentifier() : null,
                    activeConnection.getUser().getIdentifier(),
                    activeConnection.getUser().getRemoteHost(),
                    activeConnection.getStartDate().getTime());

            clusterStore.registerTunnel(registration);
            clusterHeartbeat.add(registration);

            // Retain the registration so cleanup removes exactly what was added
            activeConnection.setClusterRegistration(registration);
```

Every accessor used above is verified to exist: `getUUID()` and `getStartDate()` on
`ActiveConnectionRecord`, `hasBalancingGroup()` / `getBalancingGroup()` (used by the existing
cleanup task at `AbstractGuacamoleTunnelService.java:430-431`), `getSharingProfile()`
(`ActiveConnectionRecord.java:318`), and `getUser().getRemoteHost()` on
`RemoteAuthenticatedUser` (used at `ActiveConnectionRecord.java:136`). `getNodeId()` is on
`ClusterStore` as defined in Task 3.

- [ ] **Step 3b: Retain the registration on the connection record**

Add to `ActiveConnectionRecord.java`, alongside the existing fields:

```java
    /**
     * The cluster registration published for this connection, or null if this
     * connection was never published (clustering disabled, or the connection
     * failed before a socket was established).
     */
    private TunnelRegistration clusterRegistration;

    public TunnelRegistration getClusterRegistration() {
        return clusterRegistration;
    }

    public void setClusterRegistration(TunnelRegistration clusterRegistration) {
        this.clusterRegistration = clusterRegistration;
    }
```

Add the import `org.apache.guacamole.cluster.TunnelRegistration` to that file.

Storing the registration rather than rebuilding it at cleanup time matters: the endpoint
actually connected to may differ from `connection.getGuacamoleProxyConfiguration()` whenever
the pool selected a host, so a rebuilt registration would remove the tunnel from the wrong
`guac:idx:guacd` index and leave a stale member behind until it aged out.

- [ ] **Step 4: Release cluster state during cleanup**

In `ConnectionCleanupTask.run()` (`AbstractGuacamoleTunnelService.java:374-431`), immediately after the existing lines that remove the record from `activeConnections` and `activeConnectionGroups` (lines 423-424), add:

```java
                // Remove this tunnel from the cluster as well
                clusterHeartbeat.remove(activeConnection.getUUID().toString());

                TunnelRegistration registration = activeConnection.getClusterRegistration();
                if (registration != null) {
                    try {
                        clusterStore.unregisterTunnel(registration);
                    }
                    catch (GuacamoleException e) {
                        // Not fatal: the entry ages out of every cluster index
                        // within the stale window even if this call fails
                        logger.warn("Unable to unregister tunnel from cluster. It will "
                                + "expire on its own.", e);
                    }
                }
```

`getClusterRegistration()` is null when the connection never reached the point of publishing —
clustering disabled, or a failure before the socket was established — and the block is skipped.

- [ ] **Step 5: Verify the module compiles**

```bash
mvn -q -pl extensions/guacamole-auth-jdbc/modules/guacamole-auth-jdbc-base -am install -DskipTests
```

Expected: BUILD SUCCESS. Fix any accessor-name mismatches surfaced here against the real class definitions.

- [ ] **Step 6: Verify the disabled path is unchanged**

```bash
mvn -q -pl guacamole-cluster test
```

Expected: PASS. `NoOpClusterStoreTest` proves the disabled path grants every seat, has no routes, and reports zero load, and `ClusterModuleTest` proves `NoOpClusterStore` is what gets bound when `cluster-enabled` is absent — together, that the default configuration behaves as upstream.

- [ ] **Step 7: Commit**

```bash
git add extensions/guacamole-auth-jdbc guacamole-cluster
git commit -m "feat(cluster): route connections to pooled guacd instances

Selection order: an existing guacd connection is joined on the instance
already hosting it; a connection pinned in the database keeps its pin;
everything else goes to the least-loaded instance in the pool.

Each established tunnel is published to the cluster so that other replicas
can route joins to it."
```

---

## Task 10: Two-replica deployment and end-to-end verification

**Files:**
- Create: `docs/superpowers/deploy/guacd-headless-service.yaml`
- Create: `docs/superpowers/deploy/guacamole-deployment.yaml`
- Create: `docs/superpowers/deploy/ingress-sticky.yaml`
- Create: `docs/superpowers/deploy/README.md`

**Interfaces:**
- Consumes: the built `guacamole-cluster` module and patched JDBC extension.
- Produces: a runnable two-replica configuration.

- [ ] **Step 1: Create the guacd headless Service and Deployment**

Create `docs/superpowers/deploy/guacd-headless-service.yaml` (ASF header as YAML `#` comments at the top):

```yaml
---
# Headless Service: DNS returns one A record per READY guacd pod. This is the
# entire guacd membership and liveness mechanism -- Kubernetes removes pods that
# fail the readiness probe, so Guacamole never sees them.
apiVersion: v1
kind: Service
metadata:
  name: guacd
spec:
  clusterIP: None
  selector:
    app: guacd
  ports:
    - name: guacd
      port: 4822
      targetPort: 4822
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: guacd
spec:
  replicas: 2
  selector:
    matchLabels:
      app: guacd
  template:
    metadata:
      labels:
        app: guacd
    spec:
      containers:
        - name: guacd
          image: guacamole/guacd:1.6.1
          ports:
            - containerPort: 4822
          readinessProbe:
            tcpSocket:
              port: 4822
            initialDelaySeconds: 2
            periodSeconds: 5
            failureThreshold: 2
          livenessProbe:
            tcpSocket:
              port: 4822
            initialDelaySeconds: 10
            periodSeconds: 10
```

- [ ] **Step 2: Create the web application Deployment**

Create `docs/superpowers/deploy/guacamole-deployment.yaml`:

```yaml
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: guacamole-cluster-config
data:
  # Cluster coordination
  cluster-enabled: "true"
  cluster-redis-uri: "redis://redis:6379"
  cluster-heartbeat-interval: "10000"
  cluster-stale-window: "30000"

  # guacd pool: resolved from the headless Service, so membership follows
  # Kubernetes readiness
  guacd-cluster-dns: "guacd"
  guacd-cluster-port: "4822"
  guacd-circuit-break-duration: "15000"
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: guacamole
spec:
  replicas: 2
  selector:
    matchLabels:
      app: guacamole
  template:
    metadata:
      labels:
        app: guacamole
    spec:
      containers:
        - name: guacamole
          image: guacamole/guacamole:1.6.1
          ports:
            - containerPort: 8080
          env:
            # The JVM caches successful DNS lookups for 30s by default, which
            # would delay observing guacd pod churn well past the pool's own
            # 5s cache. This lowers the JVM-level cache to match.
            - name: JAVA_OPTS
              value: "-Dnetworkaddress.cache.ttl=5"
            - name: HOSTNAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
          readinessProbe:
            httpGet:
              path: /guacamole/
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 10
```

- [ ] **Step 3: Create the sticky ingress**

Create `docs/superpowers/deploy/ingress-sticky.yaml`:

```yaml
---
# A GuacamoleTunnel wraps a live socket and cannot move between replicas, so
# tunnel requests must return to the replica that opened the tunnel. Cookie
# affinity provides this.
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: guacamole
  annotations:
    nginx.ingress.kubernetes.io/affinity: "cookie"
    nginx.ingress.kubernetes.io/session-cookie-name: "GUAC_ROUTE"
    nginx.ingress.kubernetes.io/session-cookie-max-age: "86400"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "3600"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "3600"
spec:
  rules:
    - http:
        paths:
          - path: /guacamole
            pathType: Prefix
            backend:
              service:
                name: guacamole
                port:
                  number: 8080
```

- [ ] **Step 4: Write the deployment README**

Create `docs/superpowers/deploy/README.md` documenting: the required Redis 7 instance, the apply order (Redis, guacd, ConfigMap, Guacamole, Ingress), how to confirm the pool resolved (`kubectl exec` into a Guacamole pod and `getent hosts guacd`), and the manual verification steps in Step 5 below.

- [ ] **Step 5: Manually verify the two behaviors P1 delivers**

Deploy the stack, then confirm:

**Multi-guacd load spreading.** Open four connections. Confirm they are distributed across both guacd pods:

```bash
kubectl logs deploy/guacd --all-containers --prefix | grep "Creating new client"
```

Expected: connections appear in both pods' logs, not all in one.

**Cross-replica session join.** Open a connection, note which Guacamole pod served it, then force the next request to the other pod (delete the `GUAC_ROUTE` cookie, or `kubectl port-forward` directly to the other pod) and join the same connection. Confirm it attaches to the same session rather than starting a new one:

```bash
kubectl exec deploy/redis -- redis-cli --scan --pattern 'guac:route:*'
kubectl exec deploy/redis -- redis-cli get 'guac:route:$<connection id>'
```

Expected: the route key exists and names the guacd pod the join actually reached.

**Fail-closed on a vanished join target.** Delete the route key while a connection is open, then attempt a join:

```bash
kubectl exec deploy/redis -- redis-cli del 'guac:route:$<connection id>'
```

Expected: the join is refused with "The connection being joined is no longer available." — **not** a new desktop session.

- [ ] **Step 6: Commit**

```bash
git add docs/superpowers/deploy/
git commit -m "docs: add two-replica Kubernetes deployment for cluster P1"
```

---

## What P1 does NOT yet do

Stated so that nobody mistakes a later phase's gap for a bug in this one:

- **Concurrency limits are still per-replica.** The seat script is built and tested but not wired into `RestrictedGuacamoleTunnelService`. That is P2.
- **The admin active-connection view is still replica-local**, and a kill only works on the owning replica. That is P3.
- **Share keys still do not cross replicas.** `HashSharedConnectionMap` remains bound. That is P3.
- **Auth tokens are still replica-local**, so a replica death forces re-login. That is P4.
- **Brute-force ban counts are still per-replica.** That is P4.

Sticky sessions are what make P1 correct in the meantime: every user stays on one replica, so the replica-local state above is consistent for that user.
