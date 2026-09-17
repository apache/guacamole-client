# Two-Replica Cluster Deployment (P1)

Runs two Guacamole web application replicas against a pool of two `guacd`
instances, coordinated through Redis.

What P1 delivers, and therefore what is worth verifying here:

1. **Multi-guacd load spreading** — new connections go to the least-loaded
   `guacd` in the pool.
2. **Join routing** — when a join happens, it reaches the `guacd` already
   hosting the session. Note the limit measured below: in P1 a join can only be
   *initiated* on the replica that owns the session.
3. **Fail-closed routing** — if the route is gone, the join is refused rather
   than silently opening a new desktop.

## Build the image first — this is not optional

**The upstream `guacamole/guacamole` image contains none of this code.** The
cluster module and the patched JDBC extension only exist in an image built from
this repository. Deploying the stock image produces a stack that starts
normally, spreads nothing, and verifies nothing.

```bash
cd <repository root>
docker build -t guacamole-cluster:1.6.1 --build-arg MAVEN_ARGUMENTS=-DskipTests .
```

`--build-arg MAVEN_ARGUMENTS=-DskipTests` is required. The Dockerfile defaults
to `-DskipTests=false`, and `guacamole-cluster`'s tests are Testcontainers tests
that need a Docker daemon — which the build container does not have. Run those
tests on the host instead:

```bash
mvn -pl guacamole-cluster test
```

Two notes on the build:

- `mvn package` runs `generate-license-files` (bound to `generate-resources`)
  over the `guacamole` webapp module. On a host with a stale
  `guacamole/src/main/frontend/node_modules` this fails with `Missing license
  information in LICENSE for <package>`. A clean container build installs the
  frontend fresh and does not hit it. If it does, add
  `-DdownloadMissingLicenses` to `MAVEN_ARGUMENTS`.
- No separate layering step is needed. The extension is packaged as a
  jar-of-jars and already bundles `guacamole-cluster`, `lettuce-core`,
  `reactor-core` and the netty modules. Confirm with:

  ```bash
  unzip -l extensions/guacamole-auth-jdbc/modules/guacamole-auth-jdbc-postgresql/target/*.jar \
      | grep -E 'cluster|lettuce'
  ```

If your cluster pulls from a registry rather than a local daemon, tag and push,
then update `image:` in `guacamole-deployment.yaml` to match.

## Apply order

Redis and `guacd` must be ready before the webapp starts — the webapp opens its
Redis connection while the Guice injector is built, and resolves the `guacd`
headless Service on the first connection.

```bash
kubectl apply -f redis.yaml
kubectl apply -f guacd-headless-service.yaml
kubectl rollout status deploy/redis deploy/guacd

kubectl apply -f guacamole-deployment.yaml   # ConfigMap + Deployment + Service
kubectl rollout status deploy/guacamole

kubectl apply -f ingress-sticky.yaml

# Autoscaling. Requires metrics-server and the resource requests already
# declared in the deployment manifests above.
kubectl apply -f hpa.yaml
```

A database is still required — this stack carries only the cluster pieces. Add
your existing PostgreSQL/MySQL Service and the matching `POSTGRESQL_*` /
`MYSQL_*` environment variables to the Guacamole Deployment. Without them the
JDBC extension does not load, and neither does the cluster module that rides
inside it.

## Confirm the pool resolved

```bash
kubectl exec deploy/guacamole -- getent hosts guacd
```

Expected: **one line per ready `guacd` pod**. A single line means the Service is
not headless, or only one pod is ready. An empty result means the webapp will
fail every connection with "No guacd instances are available."

Confirm clustering actually engaged:

```bash
kubectl logs deploy/guacamole | grep 'Cluster coordination'
```

Expected: `Cluster coordination is ENABLED against "redis://redis:6379".`
If it says nothing, `CLUSTER_ENABLED` did not reach the webapp — check
`envFrom` resolved the ConfigMap.

## Manual verification

These three checks are the point of the deployment. They are manual because
each needs a real RDP/VNC session against a real `guacd`.

### 1. Multi-guacd load spreading

Open four connections, then:

```bash
kubectl logs deploy/guacd --all-containers --prefix | grep "Creating new client"
```

**Expected:** connections appear in *both* pods' logs. All four in one pod means
selection is not consulting cluster load — check that `countTunnels` is reaching
Redis, and that `guacd-cluster-dns` is set rather than falling back to the single
configured `guacd`.

### 2. Join routing, and the P1 limit on it

**Measured on a real two-replica deployment (devqa, 2026-09-16): a join cannot
be initiated from a replica that does not own the session.** With a session open
and provably live on replica A — registered in Redis under A's `nodeId` — the
two replicas disagree:

```
podA  /api/session/data/postgresql/activeConnections  ->  {"6caf3bd5-...": {...}}
podB  /api/session/data/postgresql/activeConnections  ->  {}
```

That is not a bug in the routing. It is the replica-local active-connection
directory listed under *What P1 does NOT do* below, which lands in P3. The
routing half is built and works; the discovery half is not here yet, so replica
B has no identifier to join with.

What you can verify today, on the owning replica:

```bash
# Open a session, then join it. The connect response carries the tunnel session
# token in the Guacamole-Tunnel-Token header, which read/write requests need.
curl -s -D headers.txt -X POST "$BASE/tunnel?connect" \
    --data-urlencode "token=$AUTH" --data-urlencode "GUAC_DATA_SOURCE=postgresql" \
    --data-urlencode "GUAC_ID=<connection id>" --data-urlencode "GUAC_TYPE=c" \
    --data-urlencode "GUAC_WIDTH=1024" --data-urlencode "GUAC_HEIGHT=768" \
    --data-urlencode "GUAC_DPI=96" --data-urlencode "GUAC_TIMEZONE=UTC" \
    --data-urlencode "GUAC_AUDIO=audio/L16" --data-urlencode "GUAC_IMAGE=image/png"
```

Then join the resulting active connection with `GUAC_TYPE=a`. A successful join
returns a new tunnel UUID, and the route it used is visible as:

```bash
kubectl exec deploy/redis -- redis-cli --scan --pattern 'guac:route:*'
kubectl exec deploy/redis -- redis-cli get 'guac:route:$<guacd connection id>'
```

The value is the endpoint key (`host|port|encryptionMethod`) of the `guacd`
hosting that session.

**End-to-end cross-replica join therefore requires P3**, which makes the active
connection directory (and share keys) cluster-wide. P1 supplies the route table
it will use.

### 3. Fail-closed on a vanished join target

With a connection open, delete its route and then attempt a join:

```bash
kubectl exec deploy/redis -- redis-cli del 'guac:route:$<connection id>'
```

**Expected:** the join is refused with *"The connection being joined is no longer
available."* A **new** desktop session here is a serious failure, not a cosmetic
one — it means a user asked to join a colleague's session and silently got a
fresh login instead.

### 4. Scaling guacd up and down

**Measured on devqa, 2026-09-16.** Membership is DNS, so nothing needs telling.

Scale up, then confirm the new pods actually receive traffic — not merely that
DNS resolves:

```bash
kubectl scale deploy/guacd --replicas=4
kubectl exec deploy/guacamole -- getent hosts guacd     # one line per ready pod
```

Opening 12 connections across a freshly-scaled 4-pod pool distributed them
**3 / 3 / 3 / 3**, both new pods included. Note `getent` reports the *OS*
resolver, not the JVM's view — the pool caches for 5s on top of the JVM's own
DNS TTL, so distribution is the real check and DNS is only a precondition.

Scaling down to 1 shrank the pool and sent **6 of 6** subsequent connections to
the survivor.

**Scaling guacd down disconnects users.** Every session on a removed pod dies;
guacd session state is never replicated. That is a design decision of this work,
which is why the HPA in `hpa.yaml` scales down far more slowly than it scales up.

### 5. Losing a replica, and the stale window

This is the crash-recovery mechanism, and the reason cluster entries carry a
heartbeat score at all.

```bash
# Open a session on a specific replica, then kill that replica outright.
# --grace-period=0 --force means the cleanup path never runs.
kubectl delete pod <replica> --grace-period=0 --force
watch "kubectl exec deploy/redis -- redis-cli exists 'guac:tunnel:<uuid>'"
```

Measured with `cluster-stale-window` at its 30000ms default: the route key
disappeared at ~27s and the tunnel record at ~36s, with **no cleanup code
running anywhere**. The heartbeat that had been refreshing their TTLs died with
the replica, so they simply expired.

**One thing does not get cleaned up, and it is worth knowing.** The tunnel's
membership in the `guac:idx:*` sorted sets survives. Verified against a dead
tunnel 84s after the replica was killed:

```
zcard  guac:idx:guacd:<endpoint>              -> 1     (member still present)
zcount guac:idx:guacd:<endpoint> (cutoff +inf -> 0     (not counted as live)
```

Correctness is unaffected: `countTunnels` filters by score, so a dead member is
never counted and never influences selection. But nothing physically removes it.
The only pruning is the `ZREMRANGEBYSCORE` inside the seat script. Gracefully
closed tunnels are removed properly by `unregisterTunnel`, so this grows with
crashes, not with traffic.

**As of P2 the seat script runs on every acquire**, and its `ZREMRANGEBYSCORE`
removes these tombstones — see `SeatPruningTest`. A connection index that is
never acquired again still keeps its tombstones, which is harmless: nothing reads
it, and `countTunnels` filters by score regardless.

### 6. Autoscaling

`hpa.yaml` scales both tiers on CPU (the webapp also on memory), at the 70%/80%
targets this cluster already uses elsewhere. It requires `metrics-server` and the
resource **requests** set in the deployment manifests — an HPA divides by the
request, so without them it cannot compute utilization and silently never scales.

Up and down are deliberately asymmetric, because scaling down costs user
sessions: 60s stabilization up, versus 900s (guacd) and 600s (webapp) down, and
never more than one pod per 5 minutes. Observed holding a manual over-scale in
place rather than shedding pods immediately:

```
AbleToScale=True  ScaleDownStabilized: recent recommendations were higher than
                  current one, applying the highest recent recommendation
```

**CPU is a poor proxy for guacd load.** The cluster already knows the exact live
tunnel count per guacd instance — that is what `guac:idx:guacd:<endpoint>` is —
but an HPA cannot read it without a custom metrics adapter. Scaling on that
number directly (KEDA's Redis scaler, or a prometheus-adapter external metric)
would be strictly better, and belongs with the P5 hardening work rather than here.

### 7. Cluster-wide concurrency limits (P2)

**Measured on devqa, 2026-09-17**, two replicas, one connection with
`max-connections=1`.

**The limit holds across replicas.** This is the defect P2 exists to prevent, and
it cannot be reproduced in a single JVM:

```
replica A  POST /tunnel?connect  ->  200  (tunnel opened)
replica B  POST /tunnel?connect  ->  409  "Cannot connect. This connection is in use."
redis      zcard guac:idx:conn:<id>  ->  1
```

Closing the session on A frees the seat cluster-wide; B's retry then succeeds.

**A Redis outage degrades, it does not block.** With Redis scaled to zero, both
replicas grant the same connection — per-replica enforcement, which is exactly
upstream behaviour:

```
replica A -> 200 in 0.27s
replica B -> 200 in 0.16s
ERROR: Cluster seat acquisition failed for connection "<id>".
       Concurrency limits are now enforced per replica only.
```

**Redis returning needs no restart.** After scaling Redis back up and waiting out
the 10 s re-probe window, the cluster-wide limit is enforced again — A `200`,
B `409` — with the same pods still running.

#### Three defects this test found, all inherited from P1

Every one of them was invisible until the degraded path was exercised deliberately,
and all three made a Redis outage into a connection outage:

| Symptom | Cause | Fix |
|---|---|---|
| First connect after Redis died hung **over 180 s** | Lettuce buffers commands while disconnected and waits out a long timeout, so the first caller absorbs all of it | Reject commands while disconnected; 2 s command timeout |
| Connections then failed `500 Unable to register tunnel with cluster` | `registerTunnel` was called unguarded on the connect path | Publication is best-effort; the connection survives, only its cluster visibility is lost |
| A replica **started** during an outage came up dead — no authentication at all | `RedisClusterStore` connected in its constructor; Lettuce throws a `RuntimeException`, which `ClusterModule` does not catch, so Guice could not build the injector and the whole JDBC auth provider failed to load | Connect lazily on first use; re-probe every 10 s |

The last one is worth dwelling on: the symptom was
`Authentication attempt ignored because the relevant authentication provider could
not be loaded`, which names neither Redis nor clustering. It only appeared because
pods happened to restart while Redis was down.

## What P1 does NOT do

Stated so a later phase's gap is not mistaken for a bug in this one:

- **Concurrency limits are still per-replica.** The seat script is built and
  tested but not wired into `RestrictedGuacamoleTunnelService`. That is P2.
- **The admin active-connection view is still replica-local**, and a kill only
  works on the owning replica. That is P3. **This is also what stops a join from
  being initiated on a non-owning replica** — verified on a live two-replica
  deployment, see section 2 above.
- **Share keys still do not cross replicas** — `HashSharedConnectionMap` remains
  bound. That is P3.
- **Auth tokens are still replica-local**, so a replica death forces re-login.
  That is P4.
- **Brute-force ban counts are still per-replica.** That is P4.

Sticky sessions are what make P1 correct in the meantime: every user stays on
one replica, so the replica-local state above stays consistent for that user.
Removing the affinity annotations from `ingress-sticky.yaml` breaks P1.
