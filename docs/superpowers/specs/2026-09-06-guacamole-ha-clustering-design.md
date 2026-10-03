# Guacamole HA Clustering — Design

**Date:** 2026-09-06
**Status:** Approved design, pending implementation plan
**Repos:** `guacamole-client` (all changes), `guacamole-server` (unchanged)

---

## 1. Problem

Apache Guacamole cannot run as a horizontally scaled cluster. Every piece of state
that governs connection admission, routing, and visibility is held in the memory of
a single JVM (for the web application) or a single process (for `guacd`). Running N
web application replicas therefore produces N independent, mutually blind Guacamole
installations that happen to share a database.

### 1.1 Verified in-memory state — `guacamole-client`

| State | Location | Consequence with N replicas |
|---|---|---|
| Active tunnels by UUID | `AbstractGuacamoleTunnelService.java:182` (`activeTunnels`) | A tunnel is usable only on the replica that opened it |
| Active connections per connection | `AbstractGuacamoleTunnelService.java:188` (`ActiveConnectionMultimap`) | Admin "active connections" view shows one replica's tunnels |
| Active connections per group | `AbstractGuacamoleTunnelService.java:193` | Balancing group occupancy is per-replica |
| Concurrency seats | `RestrictedGuacamoleTunnelService.java:63,73` (`ConcurrentHashMultiset<Seat>`) | `max-connections` and `max-connections-per-user` are enforced per replica, so the cluster admits up to N times the configured limit |
| Balancing child selection | `AbstractGuacamoleTunnelService.java:625` (`getBalancedConnections`) | Each replica balances using only its own counts |
| Share keys | `HashSharedConnectionMap.java:29` | A share key issued by one replica cannot be redeemed on another |
| Session tunnels | `GuacamoleSession.java:62` | Tunnel requests must return to the owning replica |
| Auth tokens | `HashTokenSessionMap.java:29` | A token issued by one replica is unknown to the others |

### 1.2 Verified in-memory state — `guacamole-server`

| State | Location | Consequence with M instances |
|---|---|---|
| `connection_id` to process map | `src/guacd/proc-map.c:120,152,183` | Joining or sharing connection `$id` succeeds only on the `guacd` instance hosting it |
| `guacd` selection | `ModeledConnection.java:454-466`, falling back to `LocalEnvironment.java:428` | The web application connects to a single statically configured hostname; no discovery, health checking, or failover |

### 1.3 Scope decisions

Confirmed with the project owner during design:

1. **Failure semantics:** reconnect is acceptable. A dying `guacd` or web application
   replica may drop live remote-desktop sessions. Session state is never replicated.
2. **Cluster-correct requirements:** concurrency limits, `guacd` pooling and health,
   session sharing and join, and the cluster-wide admin active-connection view and
   kill must all be correct across the cluster.
3. **Fork posture:** hard fork. Both repositories may be modified freely; upstream
   merge cost is not a constraint.
4. **Platform:** Kubernetes. Redis is new infrastructure introduced by this design.
5. **Scale target:** 500 to 5,000 concurrent sessions.
6. **Auth token store:** in scope. Both `guacamole-auth-jdbc` and SSO providers are in use.

---

## 2. Architecture

### 2.1 Topology

```
                    ┌──────────────────────────────┐
                    │  Ingress (nginx / traefik)   │
                    │  sticky by GUAC_AUTH token   │
                    └──────────────┬───────────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │                    │                    │
        ┌─────▼─────┐        ┌─────▼─────┐        ┌─────▼─────┐
        │ webapp-0  │        │ webapp-1  │        │ webapp-2  │   Deployment
        └──┬─────┬──┘        └──┬─────┬──┘        └──┬─────┬──┘   N replicas
           │     │              │     │              │     │
           │     └──────────────┼─────┴──────────────┼─────┘
           │                    │                    │
           │            ┌───────▼────────┐           │  seats, tunnel index,
           │            │  Redis (HA)    │◄──────────┘  conn→guacd routing,
           │            │  Sentinel x3   │              share keys, tokens,
           │            └────────────────┘              kill / logout pub-sub
           │
           │  direct TCP :4822, endpoint chosen per connection
           ▼
    ┌──────────────────────────────────────────────┐
    │  guacd headless Service (clusterIP: None)    │  membership + liveness
    │   guacd-0   guacd-1   guacd-2   ...          │  via TCP readiness probe
    │   UNMODIFIED upstream C                      │  M replicas
    └──────────────────────────────────────────────┘
```

### 2.2 Division of responsibility

**Kubernetes owns membership.** A headless Service over the `guacd` Deployment plus a
TCP readiness probe on port 4822 removes dead `guacd` pods from the EndpointSlice
automatically. No registry, heartbeat protocol, or sidecar is required, and
`guacamole-server` needs no changes.

**Redis owns state.** Seats, the active-tunnel index, `connectionID` to `guacd`
endpoint routing, share keys, auth-token identity, and the kill and logout channels.

**The ingress owns affinity.** A `GuacamoleTunnel` wraps a live socket and cannot
migrate between replicas. Tunnel requests for a given user must therefore return to
the replica that opened the tunnel. Sticky sessions keyed on the auth token provide
this.

### 2.3 Why `guacd` needs no changes

The web application already learns the `guacd`-issued connection identifier: `guacd`
returns it in the `ready` instruction, which `ConfiguredGuacamoleSocket` captures at
`ConfiguredGuacamoleSocket.java:309-316` and exposes at `:338`. It is carried on the
active connection record at `ActiveConnectionRecord.java:271,418`, and a join is
performed by passing that identifier back to `guacd` in the configuration
(`ConfiguredGuacamoleSocket.java:215`).

Cluster-wide join therefore reduces to a `connectionID → guacd endpoint` lookup on
the Java side. The per-process map in `src/guacd/proc-map.c` does not need to become
shared, because requests are routed to the `guacd` instance that already owns the
connection.

### 2.4 Code layout

One new Maven module:

| Module | Contents |
|---|---|
| `guacamole-cluster` (new) | `ClusterStore` interface, `RedisClusterStore` (Lettuce), `NodeIdentity`, `GuacdPool`, `GuacdSelector`, `ClusterHeartbeat`, `RehydratableAuthenticationProvider` SPI |

Patch sites, all verified to exist:

| Concern | File:line | Change |
|---|---|---|
| Seats | `RestrictedGuacamoleTunnelService.java:63,73` | `ConcurrentHashMultiset<Seat>` replaced by `ClusterStore`; retained as the degraded-mode fallback |
| Tunnel index | `AbstractGuacamoleTunnelService.java:182,188,193` | Mirrored into `ClusterStore`; local maps retained as the replica's own routing table |
| `guacd` selection | `AbstractGuacamoleTunnelService.java:553` | `connection.getGuacamoleProxyConfiguration()` replaced by `GuacdSelector.select(...)` with join-routing override |
| Cleanup | `AbstractGuacamoleTunnelService.java:374-431` (`ConnectionCleanupTask`) | Also releases cluster state |
| Listing and kill | `ActiveConnectionService.java:88,114-127,140` | Cluster-wide read; remote kill via pub-sub |
| Share keys | `SharedConnectionMap.java:28` (interface), bound at `JDBCAuthenticationProviderModule.java:197` | New Redis-backed implementation |
| Share definition | `SharedConnectionDefinition.java:48,99,113` | Remote variant not requiring a live `ActiveConnectionRecord` |
| Token store | `TokenSessionMap.java:28`, `AuthenticationService.java:264,314,404` | Redis-backed identity store plus rehydration |

### 2.5 Non-goals

- `guacd` session state is never replicated. A `guacd` pod death drops its sessions.
- Tunnels never migrate between web application replicas.
- Authorization is never cached in Redis; it is always re-derived from the database.

### 2.6 Brute-force ban tracking

`guacamole-auth-ban` tracks authentication failures in a per-JVM Caffeine cache
(`InMemoryAuthenticationFailureTracker.java:47`). With N replicas behind a load
balancer, an attacker receives up to N times the configured `maxAttempts` before
being banned, and a ban applied on one replica is not observed by the others — a
security weakening that scales linearly with replica count.

This is **in scope, delivered in P4** alongside the token store, since both concern
cluster-wide authentication state. Design in §5.7.

---

## 3. Data model

### 3.1 Core mechanism: heartbeat-scored sorted sets

Every cluster index is a Redis sorted set whose **member is a tunnel UUID** and whose
**score is the last heartbeat timestamp**.

- Live count: `ZCOUNT key (now-30000 +inf`
- Cleanup: `ZREMRANGEBYSCORE key -inf (now-30000)`

Each replica re-adds its own live tunnels every 10 seconds in a single pipeline. A
replica that dies stops refreshing, and its entries age out of every index
simultaneously. There is no reaper process, no lease-renewal daemon, and no
possibility of divergence between a TTL key and an index entry.

Staleness window: 30 s. Heartbeat interval: 10 s. Both configurable.

Cost at the 5,000-session target: each tunnel is refreshed into roughly 7 keys (five
indexes, its per-user seat key, and the tunnel hash and route key expiries) once per
10 s. With three replicas that is approximately **850 operations per second per
replica** and approximately **3,500 per second in aggregate** at Redis, pipelined
into a small number of round trips. Redis sustains well over an order of magnitude
more than this.

### 3.2 Keyspace

```
guac:tunnel:{uuid}              HASH  nodeId, guacdConnectionId, guacdEndpoint,
                                      connIdentifier, groupIdentifier,
                                      sharingProfileId, username, remoteHost,
                                      startTime
                                      EXPIRE 30s, refreshed by heartbeat

guac:idx:conn:{connId}          ZSET  uuid -> ts   active per connection
guac:idx:group:{groupId}        ZSET  uuid -> ts   active per balancing group
guac:idx:user:{username}        ZSET  uuid -> ts   active per user
guac:idx:guacd:{endpoint}       ZSET  uuid -> ts   guacd load
guac:idx:all                    ZSET  uuid -> ts   cluster-wide admin list

guac:seat:user:{user}:{connId}  ZSET  uuid -> ts   per-user-per-connection limit
guac:seat:user:{user}:g:{gid}   ZSET  uuid -> ts   per-user-per-group limit

guac:route:{guacdConnectionId}  STR   guacdEndpoint          EXPIRE 30s
guac:share:{shareKey}           HASH  guacdConnectionId, guacdEndpoint,
                                      connIdentifier, sharingProfileId
guac:authfail:{address}         STR   failure count, EXPIRE = ban duration

guac:token:{sha256(token)}      HASH  username, authProviderIdentifier,
                                      remoteAddress, remoteHostname,
                                      authTime, lastAccess
                                      EXPIRE = api-session-timeout

guac:killset                    ZSET  uuid -> ts   durable fallback for kills
guac:kill                       PUBSUB  message = tunnel UUID
guac:logout                     PUBSUB  message = sha256(token)
guac:share:revoke               PUBSUB  message = shareKey
```

**Deliberate collapse.** `guac:idx:conn:{connId}` *is* the seat counter for
`max-connections`, and `guac:idx:group:{groupId}` *is* the counter for the group
limit. One structure serves both. It is therefore structurally impossible for the
admin's active-connection view to disagree with what the limiter counts. Upstream
keeps these in two separate maps (`AbstractGuacamoleTunnelService.java:188` versus
`RestrictedGuacamoleTunnelService.java:63`), which in a cluster would drift.

### 3.3 Seat acquisition — atomic Lua script

```
KEYS: connIdx, userConnIdx, groupIdx, userGroupIdx
ARGV: uuid, staleWindowMs, maxConn, maxConnPerUser, maxGroup, maxGroupPerUser

now = redis.call('TIME')                       -- server time, never client time
cutoff = now - staleWindowMs

for each supplied key:
    ZREMRANGEBYSCORE key -inf cutoff           -- self-prune

for each supplied key with a limit > 0:
    if ZSCORE key uuid == nil                  -- idempotency: already mine?
       and ZCARD key >= limit:
        return <code identifying which limit failed>

for each supplied key:
    ZADD key now uuid

return 0
```

Properties:

- **Atomic.** One round trip; no check-then-act race between replicas.
- **Self-pruning.** Stale entries from dead replicas are removed on every acquire.
- **Idempotent per member.** Lettuce retries on Sentinel failover, so the script can
  run twice for the same UUID. The `ZSCORE` guard prevents a retry from counting the
  member it just inserted and self-blocking a `max-connections=1` connection.
- **Server-sourced time.** Scores come from `redis.call('TIME')`. Client clocks are
  never used; see §6.2.

The return code preserves the distinction the caller depends on at
`RestrictedGuacamoleTunnelService.java:214,244,291`: "this connection is busy"
(raise `GuacamoleResourceConflictException`, and for a balancing group try the next
child) versus "this user is at their limit" (do not try other children).

Release is the mirror: `ZREM` from each index, `DEL` of the tunnel hash and route key.

---

## 4. Data flows

### 4.1 Primary connect

1. `acquire()` runs the Lua seat script against `guac:idx:conn:{id}`,
   `guac:seat:user:{u}:{id}`, and the group keys when connecting through a balancing
   group. Returns 0, or a code identifying the limit that failed.
2. `connectionRecordMapper.insert(...)` writes connection history to PostgreSQL
   (unchanged, `AbstractGuacamoleTunnelService.java:474-520`).
3. `GuacdSelector` chooses an endpoint (§4.4).
4. `ConfiguredGuacamoleSocket` connects; `guacd` returns `ready` carrying the
   connection identifier.
5. Write `guac:tunnel:{uuid}`, `SET guac:route:{guacdConnectionId} {endpoint} EX 30`,
   and `ZADD` into `guac:idx:guacd:{ep}`, `guac:idx:user`, `guac:idx:all`.
6. Populate the local `activeTunnels` map as the replica's own routing table.
7. The heartbeat refreshes all of the above every 10 s.

The seat is taken at step 1, before `guacd` has issued a connection identifier at
step 4. If the connect fails in between, the existing `ConnectionCleanupTask`
(`AbstractGuacamoleTunnelService.java:374-431`) releases the cluster state; and
independently, the sorted-set entry ages out within the staleness window. Both
guarantees are retained deliberately, because this is the path that hands out
capacity.

### 4.2 Join an existing connection

Selection step 1 (§4.4) performs `GET guac:route:{guacdConnectionId}` and connects to
that endpoint regardless of which replica is serving the request. This is what makes
session join work cluster-wide without modifying `guacd`.

### 4.3 Share key redemption across replicas

`SharedConnectionMap` is already an interface (`SharedConnectionMap.java:28`, bound at
`JDBCAuthenticationProviderModule.java:197`), so the map is a clean substitution. The
obstacle is one layer down: `SharedConnectionDefinition.java:48` holds a live
`ActiveConnectionRecord`, and `AbstractGuacamoleTunnelService.java:808` builds a new
record from `definition.getActiveConnection()`.

The Redis implementation stores `{guacdConnectionId, guacdEndpoint, connIdentifier,
sharingProfileId}`. A **remote variant of `SharedConnectionDefinition`** supplies
those four fields without a live record, and the join path is refactored to build its
configuration from them. This is the largest single diff in the design.

Revocation (`SharedConnectionDefinition.java:163`, `invalidate()`) publishes to
`guac:share:revoke` so every replica drops the key.

### 4.4 `guacd` endpoint selection

Replaces the unconditional `connection.getGuacamoleProxyConfiguration()` at
`AbstractGuacamoleTunnelService.java:553`. Evaluated in order:

1. **Joining an existing connection.** `GET guac:route:{guacdConnectionId}`. Connect
   to that endpoint. If the key is absent, **fail closed** with "connection no longer
   available". Never fall back to a new session: doing so would place the joining
   user in a different desktop session from the one they were authorized to observe.
2. **Connection has an explicit `proxy_hostname` in the database.** Honor it
   unchanged (`ModeledConnection.java:454-466`). Backwards compatible for
   deliberately pinned connections.
3. **Otherwise.** `GuacdPool` resolves candidates; `GuacdSelector` picks the endpoint
   with the lowest `ZCOUNT guac:idx:guacd:{ep}`, with a random tie-break. On connect
   failure, the endpoint is circuit-broken for approximately 15 s and the next
   candidate is tried.

`GuacdPool` resolves from either a static list (`guacd-cluster-hosts`) or a headless
Service name (`guacd-cluster-dns`) via `InetAddress.getAllByName()`, re-resolved on a
few-second cache. Kubernetes returns only ready pod IPs for a headless Service, which
is the liveness signal.

### 4.5 Cluster-wide admin listing and kill

**Listing.** `ActiveConnectionService.java:88,140` merges local `ActiveConnectionRecord`
instances with remote entries read from `guac:idx:all` and their `guac:tunnel:{uuid}`
hashes. `TrackedActiveConnection` exposes plain setters for every field the UI needs
(`TrackedActiveConnection.java:156,178,190,228,238,248,258`), so remote entries populate
directly with `tunnel = null`.

**Kill.** `ActiveConnectionService.java:114-127` reads `guac:tunnel:{uuid}.nodeId`. If
it is the local replica, the tunnel is closed as today. Otherwise the UUID is
published to `guac:kill` and written to `guac:killset`; each replica subscribes, and
only the owner finds the UUID in its local `activeTunnels`. The caller waits, bounded
at approximately 2 s, for the sorted-set entry to disappear before returning, so the
API does not report success for a kill that did not land. `deleteObject` already
null-checks the tunnel, so remote records require no special casing there.

### 4.6 Failure flows

| Event | Behavior |
|---|---|
| Web application replica dies | Sockets die with the JVM; `guacd` tears down its clients (`src/guacd/proc.c:106`). Cluster entries stop being heartbeated and age out of every index within the staleness window: seats free, admin view clears, route keys expire. Users reconnect through the ingress to a surviving replica, remaining logged in for rehydratable providers (§5). |
| `guacd` pod dies | Tunnels close; `ConnectionCleanupTask` performs the normal release. Kubernetes removes the pod from the EndpointSlice and `GuacdPool` stops offering it. Joins targeting it fail closed. |
| Balancing group | `getBalancedConnections` (`AbstractGuacamoleTunnelService.java:625`) is unchanged; occupancy now comes from the shared Lua script, so every replica sees identical counts. `connection_weight` handling is untouched. |

**Known limitation.** If a replica is hard-killed, `guacd` may hold its side of the
connection open until its own timeout expires. During that window `guac:idx:guacd`
undercounts real `guacd` load and selection may briefly over-assign work to a
recovering instance. This self-corrects. Eliminating it would require `guacd` to
report its own load, which would mean modifying the C repository.

---

## 5. Auth token store and session recovery

### 5.1 Constraints

`GuacamoleSession` holds a live `AuthenticatedUser` and `List<DecoratedUserContext>`
(`GuacamoleSession.java:52,58`). These are provider-specific objects wrapping
database access and provider state. They are not serializable and must not be
serialized. The store therefore holds only enough to **rebuild** a session.

Two designs were considered and are rejected:

- **Storing credentials in Redis to replay `authenticateUser(credentials)`.** This
  places a user's password in Redis for the life of their session. Rejected.
- **Serializing the `UserContext` or permission set.** This caches authorization, so
  revoking a permission or disabling an account would not take effect until token
  expiry. Rejected: authorization must be re-derived, never replayed from cache.

### 5.2 What is stored

Identity only: username, authenticating provider identifier, remote address and
hostname, authentication time, last access. No secrets and no permissions.

The key is the **SHA-256 of the token, not the token**. A Guacamole auth token is a
bearer credential. Storing it in plaintext means a Redis dump, a backup, or an
operator running `KEYS guac:token:*` yields a set of live, immediately usable
sessions. Hashing makes the store useless for impersonation while preserving the
exact-match lookup that is the only required operation.

Redis must run with `requirepass` or ACL authentication **and** TLS, with the
Guacamole ACL user scoped to the `guac:` key prefix. This becomes a hard requirement
once the token store is deployed.

### 5.3 Rehydration flow

A request arrives at a replica holding no local session for the token:

1. Local `HashTokenSessionMap` miss, then `HGETALL guac:token:{sha256}`.
2. Redis miss results in `401`, as today.
3. On a hit, the stored `authProviderIdentifier` is checked against the
   **rehydratable-provider allowlist**. A provider not on the list results in `401`
   and the user authenticates again.
4. On an allowed provider, the `AuthenticatedUser` is reconstructed from the identity
   fields and the existing `getUserContexts()` path runs
   (`AuthenticationService.java:264,314`), calling
   `authProvider.getUserContext(authenticatedUser)`, which **re-reads permissions
   from the database**. An account disabled or de-permissioned since login is caught
   here.
5. The `GuacamoleSession` is placed in the local map and the request is served.

No `AuthenticationSuccessEvent` is fired on rehydration. That event drives login
accounting and `guacamole-auth-ban`, and a replica restart must not present as a
burst of logins. Rehydration emits its own event type or none.

### 5.4 Default-deny provider allowlist

```java
public interface RehydratableAuthenticationProvider {
    AuthenticatedUser rehydrate(String username, Credentials skeleton)
        throws GuacamoleException;
}
```

Only providers implementing this interface are rehydratable.
`guacamole-auth-jdbc` implements it. **All other providers are denied by default**,
and the default is load-bearing:

- `guacamole-auth-totp` records that a session has passed its second factor in
  memory. Rebuilding such a session from a username alone is a **2FA bypass**: the
  new replica would issue an authenticated session without ever seeing a TOTP code.
- OIDC and SAML providers under `guacamole-auth-sso` carry assertion state on their
  `AuthenticatedUser`. Reconstructing it from a username fabricates an authentication
  the identity provider never issued.
- `guacamole-vault` may hold provider-issued secrets scoped to the session.

Silently rehydrating any of these converts "the user logs in again after a replica
restart" into "an attacker holding a stale token receives a fresh session".

The check is per-provider and per-session, keyed on the token's own
`authProviderIdentifier`. In this deployment, which runs both `guacamole-auth-jdbc`
and SSO, JDBC-authenticated sessions rehydrate while SSO sessions fall through to the
identity provider redirect. Both behaviors coexist in one cluster.

### 5.5 Cluster-wide logout and expiry

Logout performs `DEL guac:token:{sha256}` **and** publishes to `guac:logout`. Every
replica drops any local `GuacamoleSession` it rehydrated for that token. Without the
publish, logging out on one replica would leave a fully usable session in another
replica's local map until timeout, which is a worse security property than the
current single-node behavior.

Cluster-wide idle expiry follows from the same structure: the Redis `EXPIRE` mirrors
`api-session-timeout` and is refreshed on access.

### 5.6 Scope of the benefit

Tunnels still do not migrate. The token store means a replica death costs a user
their sessions, not their login: they land on a healthy replica already
authenticated and reconnect.

### 5.7 Cluster-wide brute-force ban tracking

`AuthenticationFailureTracker` (`AuthenticationFailureTracker.java:29`) is a
three-method interface — `notifyAuthenticationRequestReceived`,
`notifyAuthenticationFailed`, `notifyAuthenticationSuccess` — and
`BanningAuthenticationListener.java:155` is the single construction site of the
in-memory implementation. A `RedisAuthenticationFailureTracker` is therefore a
substitution at one line, selected when `cluster-enabled` is true.

Redis model, one key per client address:

- **Failure** — a Lua script performs `INCR guac:authfail:{address}` followed by
  `EXPIRE key banDuration`, refreshing the window on each failure. This matches the
  in-memory semantics, where `AuthenticationFailureStatus.notifyFailed()` updates
  `lastFailure` and `isValid()` measures from it.
- **Request received** — read the counter; if it is at or above `maxAttempts`, throw
  exactly as the in-memory tracker does, so the listener contract is unchanged.
- **Success** — `DEL guac:authfail:{address}`.

The increment and threshold check share one script so that concurrent attempts
against different replicas cannot both observe a count below the limit.

**One semantic change, stated deliberately.** The in-memory tracker bounds memory
with Caffeine's `maximumSize(maxAddresses)`
(`InMemoryAuthenticationFailureTracker.java:84-85`). Redis has no equivalent bound,
so the `maxAddresses` property no longer caps anything: memory instead scales with
the number of *distinct* addresses failing authentication within one ban window.
Each key is a small counter with a TTL, so even a million distinct addresses is on
the order of tens of megabytes and drains automatically. It is nonetheless an
attacker-influenced allocation.

Do **not** mitigate this by setting an LRU `maxmemory-policy` on this Redis instance:
eviction would silently discard tunnel index and seat state, converting a nuisance
into a correctness failure. The correct outer defense is connection rate limiting at
the ingress, which should be in place regardless. The behavior change is called out
in the P4 release notes so that operators do not assume `maxAddresses` still bounds
anything.

---

## 6. Error handling and degradation

### 6.1 Redis unavailable — per-call-site behavior

A single global fail-open or fail-closed policy is wrong here, because the call sites
have opposite risk profiles.

| Path | Behavior when Redis is unreachable | Rationale |
|---|---|---|
| Seats and limits | Degrade to the retained in-memory per-replica counters | Limits are then enforced per replica, which is exactly current upstream behavior, rather than not at all. Log at `ERROR`; raise a health gauge |
| Join and share routing | **Fail closed** — reject the join | A join without a route lookup would connect to an arbitrary `guacd` and open a *new* session rather than the one the user was authorized to observe |
| Token rehydration | **Fail closed** — `401`, re-login | Degrades to current single-node behavior |
| Brute-force ban tracking | Degrade to the in-memory per-replica tracker | Failing closed would refuse **all** logins during a Redis outage — a self-inflicted denial of service. Per-replica counting is the current upstream behavior and the correct fallback |
| `guacd` load and admin listing | Degrade to replica-local view | Current upstream behavior |

Redis being down means the cluster continues serving connections with per-replica
limits, loses cross-replica joins and cluster-wide admin visibility, and forces
re-login on replica hop. It does not mean an outage.

### 6.2 Two details that silently break the design if missed

**Scores must come from Redis, not replica clocks.** Sorted-set scores are
timestamps and staleness is `now - 30s`. Scores written from replica clocks would,
under NTP drift exceeding the window, either evict live tunnels or leave dead ones
counted indefinitely — intermittently and per-replica. Every score is therefore taken
from `redis.call('TIME')` inside the Lua script, and from server-side time on the
heartbeat path.

**The seat script must be idempotent per member.** Covered in §3.3. Without the
`ZSCORE` guard, a Lettuce retry after a Sentinel failover self-blocks any connection
whose limit is 1.

### 6.3 Remaining failure modes

- **Sentinel failover split-brain.** A demoted master may accept writes briefly after
  promotion, permitting short-lived over-admission of seats. Bounded and
  self-correcting through sorted-set aging. Not addressed by consensus; recorded
  deliberately.
- **Pub-sub is fire-and-forget.** A replica briefly disconnected from Redis misses a
  `guac:kill`. Kills are therefore also written to `guac:killset`, which each replica
  checks on its 10 s heartbeat. Pub-sub provides latency; the sorted set provides
  correctness.
- **`guacd` endpoint fails at connect.** The next candidate is tried and the failing
  endpoint is circuit-broken for approximately 15 s, so a terminating pod is not
  repeatedly selected during readiness-probe lag.
- **Connect-path latency.** One in-cluster Redis round trip of roughly 1 ms against a
  connect that already costs a TCP handshake and protocol negotiation. The heartbeat
  runs on a dedicated scheduled executor and never touches tunnel I/O threads.

### 6.4 Observability

Required, because most new failure modes are invisible from the user interface.
Exposed for Prometheus scrape:

- Redis reachable; degraded-mode active
- Per-endpoint `guacd` load; `GuacdPool` size and circuit-breaker state
- Cluster seat counts against configured limits
- Rehydration attempts, successes, and denials by provider
- Kill delivery latency and pub-sub versus `killset` delivery counts
- Sorted-set member counts per index (leak detection)

---

## 7. Testing

`guacamole-auth-jdbc` currently has **no `src/test` directory**. The module taking the
heaviest patches has no existing coverage. JUnit 5 (`5.14.4`) is declared in the
parent `pom.xml:53`; Testcontainers is not present and must be added. Tests are
therefore written before the patches, not after.

### 7.1 `ClusterStore` contract tests

Testcontainers Redis. Keyspace shape, TTL refresh, index membership, route lifecycle.

### 7.2 Lua seat script

The highest-risk unit in the design; the only place that hands out capacity.

- Limits honored at the boundary (`n` succeed, `n+1` fails) for each of the four limit types
- Correct failure code returned, since the caller branches on it at
  `RestrictedGuacamoleTunnelService.java:214,244,291`
- Idempotency: running the script twice with the same UUID does not self-block
- Staleness: entries older than the window are pruned and do not count
- Concurrency property test: 200 threads racing on `max-connections=5`; the sorted set
  never exceeds 5 members at any observation

### 7.3 Two-replica integration rig

Testcontainers: Redis, PostgreSQL, two `guacd` containers, two web application
containers. Every cross-replica behavior in this design is invisible to a single-node
test, so this rig constitutes the acceptance criteria.

| # | Scenario | Assertion |
|---|---|---|
| 1 | `max-connections=1`, simultaneous connect via replica A and replica B | Exactly one succeeds |
| 2 | Connect on A, redeem share key on B | B connects to the same `guacd` endpoint and same `guacd` connection identifier |
| 3 | Kill from B a tunnel owned by A | Tunnel closes; the API returns only after it is gone |
| 4 | `SIGKILL` replica A holding 10 tunnels | Within the staleness window: seats freed, `guac:idx:all` clean, admin view accurate |
| 5 | Kill a `guacd` container | Its tunnels die; the next connect selects a different endpoint; joins to it fail closed |
| 6 | JDBC login on A, request on B | Rehydrates; user remains logged in |
| 6b | SSO login on A, request on B | `401`; no rehydration |
| 7 | Logout on A | Token immediately unusable on B |
| 8 | Redis stopped | Connects still succeed under per-replica limits; joins rejected; rehydration returns `401` |
| 9 | One replica's clock skewed by +5 minutes | No premature eviction, proving scores originate from `redis.call('TIME')` |
| 10 | `maxAttempts-1` failed logins on replica A, then one more on replica B | The attempt on B is refused; failures counted cluster-wide, not per replica |
| 11 | Address banned via replica A | Replica B refuses it immediately, with no local failures recorded |

Scenarios 6b, 8, and 9 cover failure modes that would otherwise ship silently.

### 7.4 Soak and leak

5,000 concurrent sessions. Verify the heartbeat operation rate approximates the
predicted 850 per second per replica and 3,500 per second in aggregate, and that
after all disconnects **every sorted set returns to zero members**. Unbounded sorted-set growth is the most probable long-running
defect in this design.

### 7.5 Chaos

Rolling web application restart under load; Sentinel failover under load. Both must
cost sessions only — never permanently held seats, never a wedged limiter.

---

## 8. Phasing

| Phase | Delivers | Standalone value |
|---|---|---|
| **P0** | `guacamole-cluster` module, `ClusterStore` and `RedisClusterStore`, Lua script, `ClusterHeartbeat`, Testcontainers rig. Nothing wired in | Tested foundation; zero behavior change |
| **P1** | `GuacdPool`, `GuacdSelector`, route map, patch at `AbstractGuacamoleTunnelService.java:553`. Minimum viable deployment: two replicas, sticky ingress, `guacd` headless Service, single Redis | **Multi-`guacd` with working session join.** Largest single win; shippable alone |
| **P2** | Cluster seats, replacing `RestrictedGuacamoleTunnelService.java:63,73` | Concurrency limits correct across the cluster |
| **P3** | Cluster active-connection index and kill; remote `SharedConnectionDefinition` refactor | Cluster-wide admin visibility; cross-replica share keys |
| **P4** | Redis token store, rehydration SPI, cluster-wide logout, Redis-backed `AuthenticationFailureTracker` | Login survives replica death for JDBC sessions; brute-force bans counted and enforced cluster-wide |
| **P5** | Hardening: Redis Sentinel, Helm chart, Redis TLS and ACL, Prometheus metrics | Production-ready |

Sticky ingress belongs to P1 rather than P5 because nothing works across two replicas
without it; it is a prerequisite, not hardening.

P1 is a coherent stopping point for evaluation before committing to the remainder.

**Planning granularity.** This specification covers more work than one implementation
plan should carry. Each phase gets its own plan. The first plan covers **P0 and P1
together**, because P0 has no observable behavior on its own and P1 is the first
shippable outcome.

---

## 9. Configuration reference

New `guacamole.properties` keys, all read through the existing `Environment` abstraction:

| Key | Default | Meaning |
|---|---|---|
| `cluster-enabled` | `false` | Master switch; `false` preserves current single-node behavior exactly |
| `cluster-redis-uri` | — | Lettuce URI, `rediss://` when TLS is enabled |
| `cluster-redis-sentinel-master` | — | Sentinel master name, when using Sentinel |
| `cluster-redis-password` | — | Redis password or ACL credential |
| `cluster-node-id` | `$HOSTNAME` plus boot UUID | Replica identity |
| `cluster-heartbeat-interval` | `10000` | Milliseconds between heartbeats |
| `cluster-stale-window` | `30000` | Milliseconds before an unrefreshed entry is considered dead |
| `guacd-cluster-hosts` | — | Static `host:port` list, comma-separated |
| `guacd-cluster-dns` | — | Headless Service name, resolved to all ready pod IPs |
| `guacd-cluster-port` | `4822` | Port for DNS-resolved endpoints |
| `guacd-circuit-break-duration` | `15000` | Milliseconds an endpoint stays circuit-broken after a connect failure |
| `cluster-token-store-enabled` | `false` | Enables §5; requires Redis authentication and TLS |
| `cluster-rehydratable-providers` | `jdbc` | Allowlist of provider identifiers permitted to rehydrate |
| `cluster-ban-tracking-enabled` | `true` when `cluster-enabled` | Routes `guacamole-auth-ban` through Redis; `false` retains per-replica in-memory tracking |

`cluster-enabled=false` must leave every code path behaviorally identical to
upstream, so the fork remains deployable single-node without Redis.
