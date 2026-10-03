# Guacamole HA Clustering — P2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make concurrency limits cluster-wide, so `max-connections` and `max-connections-per-user` are enforced across every replica instead of separately on each one.

**Architecture:** `RestrictedGuacamoleTunnelService` currently enforces limits against in-memory multisets that only the local replica sees. P2 routes each acquire through the Lua seat script built and tested in P0, which decides atomically in Redis, and keeps the existing in-memory counters as the documented fallback for when Redis is unreachable. Two structural problems must be fixed first: there is no identifier available at acquire time to key a seat by, and P1 already writes into the very sorted sets the seat script guards.

**Tech Stack:** Java 8, Maven, Guice 5.1.0, Lettuce 6.3.2.RELEASE, Redis 7.x, JUnit 5.14.4, Testcontainers 1.21.3, Docker.

**Spec:** `docs/superpowers/specs/2026-09-06-guacamole-ha-clustering-design.md` (§3.3 seat script, §4.1 primary connect, §6.1 degradation)

**Prior phase:** `docs/superpowers/plans/2026-09-06-guacamole-ha-p0-p1.md`

## Global Constraints

- **Java 8 source and target.** `pom.xml:259-260`. No `var`, no records, no `List.of`. Use `Arrays.asList`, `new HashMap<>()`, explicit types.
- **Apache RAT is active.** Every new file needs the ASF licence header, and every new module an empty `.ratignore`. `.md` files are excluded.
- **`cluster-enabled` defaults to `false`.** With clustering off, every path in this plan must behave exactly as unmodified upstream. This is asserted by a dedicated test in Task 7.
- **Never build a Redis key inline.** All keys come from `ClusterKeys`.
- **Redis unavailable at a seat call site degrades to the in-memory counters** and logs at `ERROR` (spec §6.1). It never fails the connection closed — that would turn a Redis outage into a total outage.
- **Do not change `guacamole-server`.**
- **Existing test suite must stay green:** `mvn -pl guacamole-cluster test` (44 tests as of P1).

---

## Two problems P1 left behind

Both are load-bearing for this phase, and neither is obvious from reading the spec alone. They were found by deploying P1 and measuring it.

### Problem 1 — nothing identifies a seat at acquire time

The seat script is keyed by a member id, used both for idempotency (`ZSCORE key uuid`) and for release (`ZREM key uuid`). But `acquire()` runs *before* the `ActiveConnectionRecord` exists:

```java
// AbstractGuacamoleTunnelService.java:827
acquire(user, Collections.singletonList(connection), true);
ActiveConnectionRecord connectionRecord = new ActiveConnectionRecord(connectionMap, user, connection);
```

and the record's UUID is derived from the *database* record id (`ModeledActivityRecord.java:115-121`), which does not exist until connection history is inserted — later still. **At acquire time there is no identifier of any kind.**

P2 therefore mints a **cluster seat token**: a random UUID created at the top of the connect flow, passed into `acquire()`, and carried on the `ActiveConnectionRecord` so `release()` can remove exactly what was added.

### Problem 2 — P1 already writes into the limit-bearing sets

`RedisClusterStore.indexKeys()` adds every registered tunnel to `ClusterKeys.connectionIndex(...)` and `ClusterKeys.groupIndex(...)` — **the same sorted sets the seat script counts**. Today that is harmless, because nothing reads those sets as limits. The moment P2 turns them into limits, every connection occupies *two* members of its own connection index: one written by the seat script, one by `registerTunnel`. A `max-connections=2` connection would refuse the second user.

The fix is to make both writers use the same member id — the seat token from Problem 1. Task 2 does this **before** any limit goes live, so the double-count never reaches a running system.

---

## File Structure

**New**

| File | Responsibility |
|---|---|
| `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/SeatRequestBuilder.java` | Turns a connection or group plus its limits into an ordered `SeatRequest`. Pure, no Redis, independently testable |
| `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/SeatRequestBuilderTest.java` | Tests for the above |
| `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/redis/SeatPruningTest.java` | Proves the seat script removes the tombstones P1 leaks |

**Modified**

| File | Change |
|---|---|
| `.../jdbc/tunnel/ActiveConnectionRecord.java` | Carry the cluster seat token |
| `.../jdbc/tunnel/AbstractGuacamoleTunnelService.java` | Mint the seat token; pass it to `acquire()`/`release()`; register tunnels under it |
| `.../jdbc/tunnel/RestrictedGuacamoleTunnelService.java` | Enforce limits through `ClusterStore`, falling back to the in-memory counters |
| `guacamole-cluster/.../redis/RedisClusterStore.java` | Nothing functional — verified only |

---

## Task 1: Carry a cluster seat token through the connect flow

Pure plumbing. No limit behaviour changes, so this task is safe to merge on its own and makes the next two reviewable.

**Files:**
- Modify: `extensions/guacamole-auth-jdbc/modules/guacamole-auth-jdbc-base/src/main/java/org/apache/guacamole/auth/jdbc/tunnel/ActiveConnectionRecord.java`
- Modify: `extensions/guacamole-auth-jdbc/modules/guacamole-auth-jdbc-base/src/main/java/org/apache/guacamole/auth/jdbc/tunnel/AbstractGuacamoleTunnelService.java`
- Modify: `extensions/guacamole-auth-jdbc/modules/guacamole-auth-jdbc-base/src/main/java/org/apache/guacamole/auth/jdbc/tunnel/RestrictedGuacamoleTunnelService.java`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `ActiveConnectionRecord.getClusterSeatToken()` / `setClusterSeatToken(String)`
  - `acquire(RemoteAuthenticatedUser, List<ModeledConnection>, boolean, String seatToken)` — abstract, replaces the 3-arg form
  - `release(RemoteAuthenticatedUser, ModeledConnection, String seatToken)`
  - `acquire(RemoteAuthenticatedUser, ModeledConnectionGroup, String seatToken)`
  - `release(RemoteAuthenticatedUser, ModeledConnectionGroup, String seatToken)`

- [ ] **Step 1: Add the token to `ActiveConnectionRecord`**

Alongside the `clusterRegistration` field added in P1:

```java
    /**
     * Identifies this connection's cluster seats. Minted before the seats are
     * acquired, which is necessarily before this record's own UUID exists --
     * that is derived from the database record ID and is null until connection
     * history is written.
     */
    private String clusterSeatToken;

    public String getClusterSeatToken() {
        return clusterSeatToken;
    }

    public void setClusterSeatToken(String clusterSeatToken) {
        this.clusterSeatToken = clusterSeatToken;
    }
```

**Scope note, verified before writing this plan:** `RestrictedGuacamoleTunnelService`
is the *only* subclass of `AbstractGuacamoleTunnelService` in the repository, and the
only implementor of the protected acquire/release contract. Widening these signatures
therefore cannot break an extension elsewhere.

- [ ] **Step 2: Widen the abstract signatures**

In `AbstractGuacamoleTunnelService.java`, change the four abstract declarations at lines 240, 255, 272 and 286 to take a trailing `String seatToken`, and add this to each Javadoc:

```java
     * @param seatToken
     *     Opaque identifier for the cluster seats held by this connection.
     *     Must be the same value on the matching acquire and release.
```

- [ ] **Step 3: Mint the token at both call sites**

At `AbstractGuacamoleTunnelService.java:827` (single connection):

```java
        // Minted here because no identifier for this connection exists yet --
        // the record's UUID comes from the database record ID, written later.
        String seatToken = UUID.randomUUID().toString();

        // Acquire access to single connection, ignoring the failover-only flag
        acquire(user, Collections.singletonList(connection), true, seatToken);

        // Connect only if the connection was successfully acquired
        ActiveConnectionRecord connectionRecord = new ActiveConnectionRecord(connectionMap, user, connection);
        connectionRecord.setClusterSeatToken(seatToken);
        return assignGuacamoleTunnel(connectionRecord, info, tokens, false);
```

In the balancing-group path at `:858-870`:

```java
            String seatToken = UUID.randomUUID().toString();

            // Acquire group
            acquire(user, connectionGroup, seatToken);

            ModeledConnection connection;
            try {
                connection = acquire(user, connections, upstreamHasFailed, seatToken);
            }

            // Ensure connection group is always released if child acquire fails
            catch (GuacamoleException e) {
                release(user, connectionGroup, seatToken);
                throw e;
            }

            try {

                // Connect to acquired child
                ActiveConnectionRecord connectionRecord = new ActiveConnectionRecord(connectionMap, user, connectionGroup, connection);
                connectionRecord.setClusterSeatToken(seatToken);
```

`java.util.UUID` is already imported in this file.

- [ ] **Step 4: Update the cleanup task's release calls**

At `AbstractGuacamoleTunnelService.java:508` and `:514`, pass the record's token:

```java
                release(user, connection, activeConnection.getClusterSeatToken());
```
```java
            release(user, activeConnection.getBalancingGroup(), activeConnection.getClusterSeatToken());
```

- [ ] **Step 5: Thread the parameter through `RestrictedGuacamoleTunnelService`**

Add `String seatToken` to all four overrides. **Do not use it yet** — the bodies are unchanged in this task. This keeps the diff reviewable and the behaviour identical.

- [ ] **Step 6: Verify the extension still builds**

```bash
mvn -q -pl extensions/guacamole-auth-jdbc/modules/guacamole-auth-jdbc-base -am install -DskipTests
```

Expected: BUILD SUCCESS. A compile error here means a call site was missed — search for `acquire(user,` and `release(user,`.

- [ ] **Step 7: Commit**

```bash
git add extensions/guacamole-auth-jdbc
git commit -m "refactor(cluster): carry a seat token through the connect flow"
```

---

## Task 2: Register tunnels under the seat token

Fixes Problem 2. Must land **before** any limit is enforced, or the first cluster-limited connection double-counts itself.

**Files:**
- Modify: `extensions/guacamole-auth-jdbc/modules/guacamole-auth-jdbc-base/src/main/java/org/apache/guacamole/auth/jdbc/tunnel/AbstractGuacamoleTunnelService.java`
- Test: `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/redis/TunnelRegistrationTest.java`

**Interfaces:**
- Consumes: `ActiveConnectionRecord.getClusterSeatToken()` (Task 1).
- Produces: no new API. `TunnelRegistration.getTunnelUuid()` now carries the seat token rather than the database-derived UUID.

- [ ] **Step 1: Write the failing test**

Add to `TunnelRegistrationTest.java`. This asserts the property that makes limits correct — one member per connection, not two:

```java
    @Test
    public void registrationAndSeatShareOneMemberPerConnection() throws Exception {

        // A seat taken for a tunnel, then that same tunnel registered, must
        // occupy exactly one member of the connection index -- not two. If
        // these ever diverge, every connection counts double against its own
        // max-connections limit.
        String token = "seat-token-1";

        store.acquireSeats(new SeatRequest(token, Arrays.asList(
                new SeatKey(ClusterKeys.connectionIndex("conn-1"), 5,
                        SeatResult.CONNECTION_LIMIT))));

        store.registerTunnel(new TunnelRegistration(token, "node-1", "$abc",
                GUACD_A, "conn-1", null, null, "alice", "10.0.0.5",
                System.currentTimeMillis()));

        assertEquals(1L, connection.sync().zcard(ClusterKeys.connectionIndex("conn-1")));

    }
```

Add the imports `org.apache.guacamole.cluster.SeatKey`, `SeatRequest`, `SeatResult` and `java.util.Arrays` if not already present.

- [ ] **Step 2: Run it and watch it pass for the wrong reason**

```bash
mvn -q -pl guacamole-cluster test -Dtest=TunnelRegistrationTest
```

Expected: **PASS**, because this test exercises `ClusterStore` directly and already passes the same id to both calls. It is a regression guard for the contract, not a reproduction of the bug — the bug lives in the *caller*. Note this in the commit; a reader who assumes it reproduces the defect will be misled.

- [ ] **Step 3: Make the caller use the seat token**

In `AbstractGuacamoleTunnelService.java`, in the registration block added by P1, replace the first constructor argument:

```java
            TunnelRegistration registration = new TunnelRegistration(
                    activeConnection.getClusterSeatToken(),
                    clusterStore.getNodeId(),
```

was `activeConnection.getUUID().toString()`.

- [ ] **Step 4: Update the cleanup path**

In `ConnectionCleanupTask.run()`, the heartbeat removal must use the same token:

```java
                clusterHeartbeat.remove(activeConnection.getClusterSeatToken());
```

was `activeConnection.getUUID().toString()`.

- [ ] **Step 5: Verify**

```bash
mvn -q -pl guacamole-cluster test
mvn -q -pl extensions/guacamole-auth-jdbc/modules/guacamole-auth-jdbc-base -am install -DskipTests
```

Expected: 45 tests pass; extension builds.

- [ ] **Step 6: Commit**

```bash
git add extensions/guacamole-auth-jdbc guacamole-cluster
git commit -m "fix(cluster): register tunnels under the seat token

P1 registered each tunnel under a UUID derived from the database record
ID, while seats are keyed by the token minted at acquire time. Both write
into guac:idx:conn:{id}, so a connection would occupy two members of the
sorted set that P2 turns into its own max-connections limit."
```

---

## Task 3: Build seat requests from a connection's limits

A pure translation step, isolated so the limit-ordering logic can be tested without Redis, a database, or Guice.

**Files:**
- Create: `guacamole-cluster/src/main/java/org/apache/guacamole/cluster/SeatRequestBuilder.java`
- Test: `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/SeatRequestBuilderTest.java`

**Interfaces:**
- Consumes: `ClusterKeys`, `SeatKey`, `SeatRequest`, `SeatResult`.
- Produces:
  - `SeatRequestBuilder.forConnection(String seatToken, String username, String connectionIdentifier, int maxConnections, int maxConnectionsPerUser)` returning `SeatRequest`
  - `SeatRequestBuilder.forGroup(String seatToken, String username, String groupIdentifier, int maxConnections, int maxConnectionsPerUser)` returning `SeatRequest`

- [ ] **Step 1: Write the failing test**

```java
package org.apache.guacamole.cluster;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SeatRequestBuilderTest {

    @Test
    public void ordersPerUserLimitBeforeOverallLimit() {

        // Order is the contract: the script reports failure by position, and
        // the caller must be able to tell "this user is at their limit" from
        // "this connection is busy". Upstream throws different exceptions for
        // the two, and only the latter tries the next child of a balancing
        // group.
        SeatRequest request = SeatRequestBuilder.forConnection(
                "t1", "alice", "conn-1", 5, 1);

        List<SeatKey> keys = request.getKeys();
        assertEquals(2, keys.size());
        assertEquals(ClusterKeys.userConnectionSeat("alice", "conn-1"), keys.get(0).getRedisKey());
        assertEquals(SeatResult.USER_CONNECTION_LIMIT, keys.get(0).getFailureResult());
        assertEquals(ClusterKeys.connectionIndex("conn-1"), keys.get(1).getRedisKey());
        assertEquals(SeatResult.CONNECTION_LIMIT, keys.get(1).getFailureResult());

    }

    @Test
    public void carriesTheSeatToken() {
        assertEquals("t1", SeatRequestBuilder.forConnection("t1", "alice", "c", 0, 0).getTunnelUuid());
    }

    @Test
    public void keepsUnlimitedKeysSoTheyAreStillTracked() {

        // A zero limit means unlimited, not "skip". The member must still be
        // added, because guac:idx:conn:{id} is also the cluster's live-session
        // view and the admin listing reads it.
        SeatRequest request = SeatRequestBuilder.forConnection("t1", "alice", "conn-1", 0, 0);
        assertEquals(2, request.getKeys().size());
        assertEquals(0, request.getKeys().get(0).getLimit());

    }

    @Test
    public void buildsGroupKeys() {

        SeatRequest request = SeatRequestBuilder.forGroup("t1", "alice", "grp-1", 10, 2);

        assertEquals(ClusterKeys.userGroupSeat("alice", "grp-1"),
                request.getKeys().get(0).getRedisKey());
        assertEquals(SeatResult.USER_GROUP_LIMIT, request.getKeys().get(0).getFailureResult());
        assertEquals(ClusterKeys.groupIndex("grp-1"),
                request.getKeys().get(1).getRedisKey());
        assertEquals(SeatResult.GROUP_LIMIT, request.getKeys().get(1).getFailureResult());

    }

}
```

- [ ] **Step 2: Run it to verify it fails**

```bash
mvn -q -pl guacamole-cluster test -Dtest=SeatRequestBuilderTest
```

Expected: FAIL — compilation error, `SeatRequestBuilder` does not exist.

- [ ] **Step 3: Implement it** (ASF header first)

```java
package org.apache.guacamole.cluster;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates a connection's or group's configured limits into the ordered list
 * of seats that must be held for it.
 *
 * The order of the keys is the contract with the seat script, which reports
 * failure by position: the per-user limit is checked before the overall limit,
 * so that a caller can distinguish "this user is already using this connection"
 * from "this connection is busy". Only the latter should cause a balancing
 * group to try its next child.
 */
public final class SeatRequestBuilder {

    private SeatRequestBuilder() {}

    /**
     * @param seatToken
     *     Identifier for the seats, minted at acquire time.
     *
     * @param username
     *     The user connecting.
     *
     * @param connectionIdentifier
     *     The connection being acquired.
     *
     * @param maxConnections
     *     Overall concurrent-use limit; zero or negative means unlimited.
     *
     * @param maxConnectionsPerUser
     *     Per-user concurrent-use limit; zero or negative means unlimited.
     *
     * @return
     *     The seats which must be held for this connection.
     */
    public static SeatRequest forConnection(String seatToken, String username,
            String connectionIdentifier, int maxConnections,
            int maxConnectionsPerUser) {

        List<SeatKey> keys = new ArrayList<SeatKey>(2);

        keys.add(new SeatKey(
                ClusterKeys.userConnectionSeat(username, connectionIdentifier),
                maxConnectionsPerUser, SeatResult.USER_CONNECTION_LIMIT));

        keys.add(new SeatKey(
                ClusterKeys.connectionIndex(connectionIdentifier),
                maxConnections, SeatResult.CONNECTION_LIMIT));

        return new SeatRequest(seatToken, keys);

    }

    /**
     * @param seatToken
     *     Identifier for the seats, minted at acquire time.
     *
     * @param username
     *     The user connecting.
     *
     * @param groupIdentifier
     *     The balancing group being acquired.
     *
     * @param maxConnections
     *     Overall concurrent-use limit; zero or negative means unlimited.
     *
     * @param maxConnectionsPerUser
     *     Per-user concurrent-use limit; zero or negative means unlimited.
     *
     * @return
     *     The seats which must be held for this group.
     */
    public static SeatRequest forGroup(String seatToken, String username,
            String groupIdentifier, int maxConnections,
            int maxConnectionsPerUser) {

        List<SeatKey> keys = new ArrayList<SeatKey>(2);

        keys.add(new SeatKey(
                ClusterKeys.userGroupSeat(username, groupIdentifier),
                maxConnectionsPerUser, SeatResult.USER_GROUP_LIMIT));

        keys.add(new SeatKey(
                ClusterKeys.groupIndex(groupIdentifier),
                maxConnections, SeatResult.GROUP_LIMIT));

        return new SeatRequest(seatToken, keys);

    }

}
```

- [ ] **Step 4: Run it to verify it passes**

```bash
mvn -q -pl guacamole-cluster test -Dtest=SeatRequestBuilderTest
```

Expected: PASS, 4 tests.

- [ ] **Step 5: Commit**

```bash
git add guacamole-cluster/src
git commit -m "feat(cluster): add seat request construction from configured limits"
```

---

## Task 4: Enforce connection limits through the cluster

**Files:**
- Modify: `extensions/guacamole-auth-jdbc/modules/guacamole-auth-jdbc-base/src/main/java/org/apache/guacamole/auth/jdbc/tunnel/RestrictedGuacamoleTunnelService.java`

**Interfaces:**
- Consumes: `SeatRequestBuilder` (Task 3), `ClusterStore`, the seat token (Task 1).
- Produces: no new API. `acquire(user, connections, includeFailoverOnly, seatToken)` now consults Redis when clustering is available.

- [ ] **Step 1: Inject the cluster store**

Add the imports and field:

```java
import org.apache.guacamole.cluster.ClusterStore;
import org.apache.guacamole.cluster.SeatRequest;
import org.apache.guacamole.cluster.SeatRequestBuilder;
import org.apache.guacamole.cluster.SeatResult;
```

```java
    /**
     * Cluster-wide seat accounting. Bound to NoOpClusterStore when clustering
     * is disabled, in which case every acquire falls through to the in-memory
     * counters below.
     */
    @Inject
    private ClusterStore clusterStore;
```

- [ ] **Step 2: Add the cluster acquire helper**

Add this private method. It returns `null` when the cluster could not decide, so the caller falls back:

```java
    /**
     * Attempts to take cluster-wide seats for one connection.
     *
     * @param seatToken
     *     Identifier for the seats.
     *
     * @param username
     *     The user connecting.
     *
     * @param connection
     *     The connection being acquired.
     *
     * @return
     *     The outcome, or null if the cluster store is unavailable and the
     *     caller must fall back to replica-local accounting.
     */
    private SeatResult tryClusterAcquire(String seatToken, String username,
            ModeledConnection connection) throws GuacamoleException {

        if (!clusterStore.isAvailable())
            return null;

        SeatRequest request = SeatRequestBuilder.forConnection(seatToken, username,
                connection.getIdentifier(), connection.getMaxConnections(),
                connection.getMaxConnectionsPerUser());

        try {
            return clusterStore.acquireSeats(request);
        }

        // A Redis outage must not become a connection outage. Limits degrade to
        // per-replica, which is exactly upstream behavior (spec 6.1).
        catch (GuacamoleException e) {
            logger.error("Cluster seat acquisition failed for connection \"{}\". "
                    + "Concurrency limits are now enforced per replica only.",
                    connection.getIdentifier(), e);
            return null;
        }

    }
```

- [ ] **Step 3: Use it inside the connection loop**

Replace the body of the `for (ModeledConnection connection : sortedConnections)` loop's acquire attempt (currently the nested `tryAdd` calls) with:

```java
            // Cluster-wide decision first; null means Redis could not answer
            SeatResult result = tryClusterAcquire(seatToken, username, connection);

            if (result == SeatResult.SUCCESS)
                return connection;

            if (result == SeatResult.CONNECTION_LIMIT) {
                // Busy, but not because of this user -- try the next child
                userSpecificFailure = false;
                continue;
            }

            if (result == SeatResult.USER_CONNECTION_LIMIT)
                continue;

            // result == null: Redis unavailable, fall back to replica-local
            Seat seat = new Seat(username, connection.getIdentifier());
            if (tryAdd(activeSeats, seat, connection.getMaxConnectionsPerUser())) {

                if (tryAdd(activeConnections, connection.getIdentifier(),
                        connection.getMaxConnections()))
                    return connection;

                activeSeats.remove(seat);
                userSpecificFailure = false;

            }
```

The two exception types thrown after the loop are unchanged. `GuacamoleClientTooManyException` still means every candidate failed on a per-user limit.

- [ ] **Step 4: Release the cluster seats**

Replace `release(RemoteAuthenticatedUser, ModeledConnection, String)`:

```java
    @Override
    protected void release(RemoteAuthenticatedUser user,
            ModeledConnection connection, String seatToken) {

        // Release cluster seats first; the local counters are the fallback and
        // are always safe to decrement
        try {
            if (clusterStore.isAvailable())
                clusterStore.releaseSeats(SeatRequestBuilder.forConnection(
                        seatToken, user.getIdentifier(), connection.getIdentifier(),
                        connection.getMaxConnections(),
                        connection.getMaxConnectionsPerUser()));
        }

        // Not fatal: the seat ages out of every index within the stale window
        catch (GuacamoleException e) {
            logger.warn("Unable to release cluster seats for connection \"{}\". "
                    + "They will expire on their own.", connection.getIdentifier(), e);
        }

        activeSeats.remove(new Seat(user.getIdentifier(), connection.getIdentifier()));
        activeConnections.remove(connection.getIdentifier());
        totalActiveConnections.decrementAndGet();

    }
```

`getMaxConnections()` and `getMaxConnectionsPerUser()` on `ModeledConnection` declare `throws GuacamoleException`; the surrounding `try` already covers them.

- [ ] **Step 5: Verify the extension builds**

```bash
mvn -q -pl extensions/guacamole-auth-jdbc/modules/guacamole-auth-jdbc-base -am install -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add extensions/guacamole-auth-jdbc
git commit -m "feat(cluster): enforce connection concurrency limits cluster-wide"
```

---

## Task 5: Enforce balancing-group limits through the cluster

**Files:**
- Modify: `extensions/guacamole-auth-jdbc/modules/guacamole-auth-jdbc-base/src/main/java/org/apache/guacamole/auth/jdbc/tunnel/RestrictedGuacamoleTunnelService.java`

**Interfaces:**
- Consumes: `SeatRequestBuilder.forGroup` (Task 3).
- Produces: no new API.

- [ ] **Step 1: Replace the group acquire body**

```java
    @Override
    protected void acquire(RemoteAuthenticatedUser user,
            ModeledConnectionGroup connectionGroup, String seatToken)
            throws GuacamoleException {

        String username = user.getIdentifier();

        SeatResult result = null;
        if (clusterStore.isAvailable()) {
            try {
                result = clusterStore.acquireSeats(SeatRequestBuilder.forGroup(
                        seatToken, username, connectionGroup.getIdentifier(),
                        connectionGroup.getMaxConnections(),
                        connectionGroup.getMaxConnectionsPerUser()));
            }
            catch (GuacamoleException e) {
                logger.error("Cluster seat acquisition failed for group \"{}\". "
                        + "Group limits are now enforced per replica only.",
                        connectionGroup.getIdentifier(), e);
            }
        }

        if (result == SeatResult.SUCCESS)
            return;

        if (result == SeatResult.GROUP_LIMIT)
            throw new GuacamoleResourceConflictException("Cannot connect. This connection group is in use.");

        if (result == SeatResult.USER_GROUP_LIMIT)
            throw new GuacamoleClientTooManyException("Cannot connect. Connection group already in use by this user.");

        // Redis unavailable -- replica-local accounting, as upstream
        Seat seat = new Seat(username, connectionGroup.getIdentifier());
        if (tryAdd(activeGroupSeats, seat, connectionGroup.getMaxConnectionsPerUser())) {

            if (tryAdd(activeGroups, connectionGroup.getIdentifier(),
                    connectionGroup.getMaxConnections()))
                return;

            activeGroupSeats.remove(seat);
            throw new GuacamoleResourceConflictException("Cannot connect. This connection group is in use.");

        }

        throw new GuacamoleClientTooManyException("Cannot connect. Connection group already in use by this user.");

    }
```

- [ ] **Step 2: Replace the group release body**

```java
    @Override
    protected void release(RemoteAuthenticatedUser user,
            ModeledConnectionGroup connectionGroup, String seatToken) {

        try {
            if (clusterStore.isAvailable())
                clusterStore.releaseSeats(SeatRequestBuilder.forGroup(
                        seatToken, user.getIdentifier(),
                        connectionGroup.getIdentifier(),
                        connectionGroup.getMaxConnections(),
                        connectionGroup.getMaxConnectionsPerUser()));
        }
        catch (GuacamoleException e) {
            logger.warn("Unable to release cluster seats for group \"{}\". "
                    + "They will expire on their own.",
                    connectionGroup.getIdentifier(), e);
        }

        activeGroupSeats.remove(new Seat(user.getIdentifier(), connectionGroup.getIdentifier()));
        activeGroups.remove(connectionGroup.getIdentifier());

    }
```

- [ ] **Step 3: Verify the extension builds**

```bash
mvn -q -pl extensions/guacamole-auth-jdbc/modules/guacamole-auth-jdbc-base -am install -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add extensions/guacamole-auth-jdbc
git commit -m "feat(cluster): enforce balancing group limits cluster-wide"
```

---

## Task 6: Prove the seat script prunes the tombstones P1 leaks

P1 leaves a member in every `guac:idx:*` set for each tunnel lost to an ungraceful replica death. `countTunnels` filters them by score so they never affect selection, but nothing removes them — measured on a live cluster: `zcard` 1, score-filtered `zcount` 0, 84 seconds after the replica was killed. The seat script's `ZREMRANGEBYSCORE` is the only pruner in the design, and P2 is what finally runs it.

**Files:**
- Create: `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/redis/SeatPruningTest.java`

**Interfaces:**
- Consumes: `RedisClusterStore`, `SeatRequestBuilder`, `RedisTestSupport`.
- Produces: nothing.

- [ ] **Step 1: Write the test** (ASF header first)

```java
package org.apache.guacamole.cluster.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.apache.guacamole.cluster.ClusterKeys;
import org.apache.guacamole.cluster.SeatRequestBuilder;
import org.apache.guacamole.cluster.SeatResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SeatPruningTest {

    private static final long STALE_WINDOW_MS = 30000L;

    @BeforeAll
    public static void requireDocker() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker is not available; skipping Redis-backed tests.");
    }

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

    @Test
    public void acquiringASeatPrunesTombstonesLeftByDeadReplicas() throws Exception {

        String key = ClusterKeys.connectionIndex("conn-1");

        // Two tunnels whose replica died without ever releasing them. P1 has no
        // mechanism that removes these; only the seat script does.
        long ancient = System.currentTimeMillis() - (STALE_WINDOW_MS * 10);
        connection.sync().zadd(key, (double) ancient, "dead-tunnel-1");
        connection.sync().zadd(key, (double) ancient, "dead-tunnel-2");
        assertEquals(2L, connection.sync().zcard(key));

        assertEquals(SeatResult.SUCCESS, store.acquireSeats(
                SeatRequestBuilder.forConnection("live-1", "alice", "conn-1", 5, 0)));

        // Both tombstones physically gone, only the live seat remains
        assertEquals(1L, connection.sync().zcard(key));

    }

    @Test
    public void tombstonesDoNotConsumeCapacity() throws Exception {

        String key = ClusterKeys.connectionIndex("conn-1");

        // Three dead members against a limit of 2 -- if they counted, the next
        // acquire would be refused
        long ancient = System.currentTimeMillis() - (STALE_WINDOW_MS * 10);
        connection.sync().zadd(key, (double) ancient, "dead-1");
        connection.sync().zadd(key, (double) ancient, "dead-2");
        connection.sync().zadd(key, (double) ancient, "dead-3");

        assertEquals(SeatResult.SUCCESS, store.acquireSeats(
                SeatRequestBuilder.forConnection("live-1", "alice", "conn-1", 2, 0)));
        assertEquals(SeatResult.SUCCESS, store.acquireSeats(
                SeatRequestBuilder.forConnection("live-2", "bob", "conn-1", 2, 0)));

    }

}
```

- [ ] **Step 2: Run it**

```bash
mvn -q -pl guacamole-cluster test -Dtest=SeatPruningTest
```

Expected: PASS, 2 tests. These pass against the P0 script unchanged — the script always pruned; nothing had ever called it. If they fail, the `ZREMRANGEBYSCORE` loop in `acquire-seats.lua` has regressed.

- [ ] **Step 3: Update the deployment README**

In `docs/superpowers/deploy/README.md`, section 5 states that tombstones are never pruned because the seat script is not wired until P2. Replace that paragraph's final sentence with:

```markdown
As of P2 the seat script runs on every acquire, and its `ZREMRANGEBYSCORE`
removes these tombstones -- see `SeatPruningTest`. A connection index that is
never acquired again still keeps its tombstones, which is harmless: nothing
reads it, and `countTunnels` filters by score regardless.
```

Also remove the matching bullet from *What P1 does NOT do*.

- [ ] **Step 4: Commit**

```bash
git add guacamole-cluster/src docs/superpowers/deploy/README.md
git commit -m "test(cluster): prove seat acquisition prunes dead-replica tombstones"
```

---

## Task 7: Prove the disabled path is untouched, and Redis-down degrades

The two failure modes that matter most to anyone running this. Both must be provable without a cluster.

**Files:**
- Modify: `guacamole-cluster/src/test/java/org/apache/guacamole/cluster/NoOpClusterStoreTest.java`

**Interfaces:**
- Consumes: `NoOpClusterStore`, `SeatRequestBuilder`.
- Produces: nothing.

- [ ] **Step 1: Write the tests**

```java
    @Test
    public void disabledClusteringGrantsEverySeatRegardlessOfLimit() throws Exception {

        // With cluster-enabled=false the bound store is NoOpClusterStore. A
        // max-connections of 1 must not be enforced here at all -- enforcement
        // belongs to the in-memory counters, exactly as upstream.
        NoOpClusterStore store = new NoOpClusterStore();

        assertEquals(SeatResult.SUCCESS, store.acquireSeats(
                SeatRequestBuilder.forConnection("t1", "alice", "conn-1", 1, 1)));
        assertEquals(SeatResult.SUCCESS, store.acquireSeats(
                SeatRequestBuilder.forConnection("t2", "alice", "conn-1", 1, 1)));

    }

    @Test
    public void reportsUnavailableSoTheServiceFallsBackToLocalCounters() {

        // RestrictedGuacamoleTunnelService checks isAvailable() before every
        // acquire. False here is what routes it to the in-memory path.
        assertFalse(new NoOpClusterStore().isAvailable());

    }
```

Add the import `org.apache.guacamole.cluster.SeatRequestBuilder` if the test class is in a different package — it is not; both are in `org.apache.guacamole.cluster`.

- [ ] **Step 2: Run the whole module suite**

```bash
mvn -q -pl guacamole-cluster test
```

Expected: PASS. 45 from P1 plus 4 (Task 3) plus 2 (Task 6) plus the 1 new one here, with `reportsUnavailableSoTheServiceFallsBackToLocalCounters` replacing the P1 test of the same behaviour — reconcile the count against what the suite actually prints rather than trusting this arithmetic.

- [ ] **Step 3: Commit**

```bash
git add guacamole-cluster/src
git commit -m "test(cluster): assert disabled and degraded paths keep upstream behavior"
```

---

## Task 8: Verify cluster-wide limits on a real two-replica deployment

The unit tests prove the script and the translation. Only a deployment proves the wiring, because the defect this phase exists to prevent — two replicas each granting the same last seat — cannot occur in a single JVM.

**Files:** none. This task produces evidence and a README section.

**Interfaces:**
- Consumes: the deployment in `docs/superpowers/deploy/`.
- Produces: `docs/superpowers/deploy/README.md` section 7.

- [ ] **Step 1: Build and push the image**

```bash
docker build -t guacamole-cluster:1.6.1 --build-arg MAVEN_ARGUMENTS=-DskipTests .
```

Deploy per `docs/superpowers/deploy/README.md`. Two webapp replicas are required — the whole point is to drive them independently.

- [ ] **Step 2: Create a connection limited to one concurrent use**

In the UI or via the REST API, set `max-connections` to 1 on a test connection.

- [ ] **Step 3: Take the seat on replica A**

```bash
kubectl port-forward pod/<replica-a> 18081:8080
# authenticate, then open a tunnel against the limited connection and hold it
```

Confirm the seat exists:

```bash
kubectl exec deploy/redis -- redis-cli zcard 'guac:idx:conn:<connection id>'
```

Expected: `1`.

- [ ] **Step 4: Attempt the same connection on replica B**

```bash
kubectl port-forward pod/<replica-b> 18082:8080
# authenticate separately and attempt the same connection
```

**Expected: refused.** Before P2 this succeeded, because replica B's in-memory counter was empty — that is the defect this phase fixes. The webapp log should show the connection being rejected as in use, and `zcard` must still be `1`.

- [ ] **Step 5: Confirm release frees it cluster-wide**

Close the session on replica A, then retry on replica B.

Expected: succeeds, and `zcard` returns to `1` (B's seat, not A's).

- [ ] **Step 6: Confirm a Redis outage degrades rather than blocks**

```bash
kubectl scale deploy/redis --replicas=0
```

Expected: connections still succeed on both replicas, each enforcing the limit locally, with `Cluster seat acquisition failed` logged at ERROR. Restore with `--replicas=1`.

This is the behaviour spec §6.1 requires, and the one most likely to be got wrong by a future change: a Redis outage must never become a connection outage.

- [ ] **Step 7: Document the measurements**

Add section 7 to `docs/superpowers/deploy/README.md` recording what was observed, in the style of sections 4-6: the actual refusal, the actual `zcard` values, and the degraded-mode result. Record measured numbers, not expected ones.

- [ ] **Step 8: Commit**

```bash
git add docs/superpowers/deploy/README.md
git commit -m "docs: record cluster-wide limit verification on a live deployment"
```

---

## What P2 does NOT do

- **The admin active-connection view is still replica-local**, and a kill still only works on the owning replica. That is P3 — and it is also what stops a join being initiated from a non-owning replica, measured in `docs/superpowers/deploy/README.md` section 2.
- **Share keys still do not cross replicas.** `HashSharedConnectionMap` remains bound. P3.
- **Auth tokens are still replica-local.** P4.
- **Brute-force ban counts are still per-replica.** P4.
- **`getAbsoluteMaxConnections()` is still counted per replica.** It is a server-wide safety valve rather than a per-connection limit, and making it cluster-wide means giving `guac:idx:all` a limit — cheap, but it changes the meaning of an existing setting from "per replica" to "per cluster" and would silently tighten every existing deployment. Decide that deliberately in P5 rather than as a side effect here.
