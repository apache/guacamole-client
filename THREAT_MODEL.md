<!--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
-->

# Apache Guacamole — Threat Model

## §1 Header

- **Project:** Apache Guacamole (the clientless remote-desktop gateway family)
- **Repositories modeled (scope locked by the PMC):**
  `apache/guacamole-client`, `apache/guacamole-server`, `apache/guacamole-website`.
- **Version/commit binding:** Written against the `1.6.x` line / `main` of both
  code repositories as of the date below. This is an *umbrella* model covering all
  three repositories; a report against release *N* is triaged against the model as
  it stood at *N*, not at HEAD.
- **Date:** 2026-07-04
- **Author:** ASF Security team, drafted via the threat-model-producer (Scovetta)
  rubric at the Guacamole PMC's request (path 3 — the Security team drafts, the
  PMC reviews and ratifies).
- **Status:** **v0 DRAFT — for Guacamole PMC review.** Nothing here is ratified;
  every *(inferred)* claim is a proposal for the PMC to confirm, correct, or
  strike (see §14). No maintainer answers have been folded in yet.
- **Reporting cross-reference:** Findings that violate a property we claim in §8
  should be reported privately per our security policy
  (<https://guacamole.apache.org/security/>, `security@guacamole.apache.org`, or
  the ASF Security Team) *(documented)*. Findings that fall under §3 (out of
  scope) or §9 (properties we do not provide) will be closed citing this
  document.
- **Provenance legend** — every non-trivial claim carries exactly one tag:
  - *(documented)* — stated in our own docs (the security page, the manual, the
    configuration reference). Cited inline.
  - *(maintainer)* — stated by a Guacamole maintainer in response to this
    process. **None yet — this is v0.**
  - *(inferred)* — reasoned from architecture, code structure, or domain
    knowledge; not yet confirmed. Every such tag has a matching open question in
    §14.
- **Draft confidence:** ~30 documented citations / 0 maintainer / 18 distinct
  *(inferred)* claims (I1–I18, each with a §14 question). The bulk of the model's
  *reasoning* is still hypothesis pending PMC review — expected for a v0.

**What Guacamole is.** Apache Guacamole is a *clientless, protocol-agnostic remote
desktop gateway* delivered as an HTML5 web application *(documented —
[introduction](https://guacamole.apache.org/doc/gug/introduction.html))*. A user
opens a browser, authenticates to the Guacamole web application, and is connected
to a remote machine over VNC, RDP, SSH, telnet, or Kubernetes — with no client
software installed beyond the browser. The system has two moving parts that must
be modeled separately because they live at very different trust levels and are
written in different languages: **guacd**, a C daemon (`apache/guacamole-server`)
that actually speaks the remote-desktop protocols and translates them to and from
the text-based *Guacamole protocol*; and the **web application**, a Java servlet
plus JavaScript/HTML5 front-end (`apache/guacamole-client`) that authenticates
users, brokers connections, and tunnels the Guacamole protocol between the browser
and guacd *(documented —
[architecture](https://guacamole.apache.org/doc/gug/guacamole-architecture.html))*.
The third repository, `apache/guacamole-website`, is the static project website and
carries essentially no runtime trust surface (see §2/§3).

---

## §2 Scope and intended use

**Primary intended use.** A centrally-administered gateway that lets authenticated
browser users reach remote desktops/shells they are authorized for, over a
firewall-friendly HTTPS front door, without installing native RDP/VNC/SSH clients
*(documented — introduction)*. Guacamole is a *deployed network service operated
by an administrator*, not an in-process library.

**Caller roles.** There is no single "caller"; the roles split as follows and each
gets separate treatment in §6/§7:

- **Browser user (client)** — authenticated to the web app, otherwise untrusted.
  Interacts only through the tunnel and the web UI *(inferred — I1)*.
- **Unauthenticated web attacker** — can reach the web app's HTTP surface but has
  no valid session *(inferred — I1)*.
- **Operator / administrator** — deploys guacd and the web app, writes
  `guacamole.properties`, configures extensions and connection definitions.
  Trusted for the instance *(documented — configuring-guacamole)*.
- **Remote desktop server (peer)** — the VNC/RDP/SSH/telnet/Kubernetes endpoint
  guacd connects out to. **Authenticated to guacd by address only, and adversarial
  in this model** — its protocol responses are parsed by C code (see §7)
  *(inferred — I2)*.
- **The web app, as seen by guacd** — guacd treats whatever connects to its socket
  and speaks the Guacamole protocol as authoritative (see §4, §9) *(inferred —
  I3)*.

**Component-family table.** The single most important orienting fact is the
**guacd (C) vs. web-app (Java) vs. website (static)** split:

| Family | Repo | Language | Representative entry point | Touches outside the process? | In model? |
| --- | --- | --- | --- | --- | --- |
| **guacd core + libguac** | guacamole-server | C | TCP `:4822`, Guacamole-protocol parser, plugin loader | Yes — outbound TCP to remote servers; loads `.so` plugins | **Yes** — primary memory-safety surface |
| **Protocol plugins** (VNC/RDP/SSH/telnet/Kubernetes) | guacamole-server | C | per-protocol client `.so`; parses remote-server wire data, images, audio, clipboard | Yes — network sockets to the remote peer | **Yes** — the untrusted-remote-input surface |
| **Web application / servlet** | guacamole-client | Java | HTTP endpoints, the tunnel servlet, REST API | Yes — HTTP in, TCP out to guacd | **Yes** |
| **Auth / extension framework** | guacamole-client | Java | LDAP, SAML, CAS, OpenID, DB auth (MySQL/Postgres/SQL Server/MariaDB), TOTP, Duo, RADIUS, header/JSON auth | Yes — LDAP/HTTP/DB to identity sources | **Yes** — authn/authz surface |
| **JavaScript / HTML5 front-end** | guacamole-client | JS/HTML | browser DOM, canvas renderer, tunnel client | Runs in the user's browser | **Yes** — client-side (XSS) surface |
| **Project website** | guacamole-website | static HTML/Jekyll | published pages | No runtime; build-time only | **Out of model for runtime findings** — see §3 |

Anything marked out of model here reappears in §3 with the reason.

---

## §3 Out of scope (explicit non-goals)

- **The project website (`apache/guacamole-website`) as a runtime target.** It is a
  static, published site with no server-side execution, no authentication surface,
  and no user data. It is *in scope only for discoverability* — i.e. it is where
  our published security policy and manual live, and a scan should be able to find
  those — but it has no runtime attack surface, so runtime memory-safety / injection
  / authz findings against it are `OUT-OF-MODEL: unsupported-component`
  *(inferred — I4)*.
- **The security of the remote desktops themselves.** Guacamole is a gateway; the
  hardening, patch level, and credentials of the VNC/RDP/SSH/telnet/Kubernetes
  targets are the operator's concern, not ours *(inferred — I5)*.
- **The operator / administrator as an adversary.** An operator who can edit
  `guacamole.properties`, define connections, or run code on the guacd or web-app
  hosts already controls the instance; we do not model them as an attacker
  *(inferred — I6)*.
- **guacd exposed directly to untrusted networks.** guacd is designed to sit behind
  the web app on a trusted network segment; a deployment that publishes `:4822` to
  the Internet is a misconfiguration, not a scenario we defend (see §9, §10)
  *(inferred — I3)*.
- **Third-party dependency internals.** libVNCserver, FreeRDP, libssh2, Cairo,
  etc., and the front-end's bundled JS libraries are modeled only at the boundary
  where guacd/the web app hands them data; CVEs internal to those projects are
  triaged upstream *(inferred — I7)*.
- **Test code, examples, and build tooling** across all three repos are separately
  authored and not covered by the core guarantees *(inferred — I8)*.

---

## §4 Trust boundaries and data flow

Guacamole is a service, so it has several distinct trust boundaries rather than one
API surface. The end-to-end path is:

```
browser  ──HTTPS/tunnel──▶  web app (Java)  ──TCP :4822──▶  guacd (C)  ──RDP/VNC/SSH──▶  remote server
   ▲                                                                                          │
   └───────────── Guacamole protocol (rendered images/audio/clipboard) ◀──────────────────────┘
```

Trust transitions, each of which a finding must cross to be in-model:

1. **Browser → web app (the HTTP boundary).** The primary authentication and
   session boundary. Everything arriving here is untrusted until a session is
   established and authorized *(inferred — I1)*.
2. **Web app → guacd (the socket boundary).** The web app connects to guacd on TCP
   4822 and sends connection instructions plus the Guacamole protocol stream. **By
   default this link is unauthenticated and unencrypted**; `guacd-ssl` and network
   placement are the only controls *(documented — configuring-guacamole)*. guacd
   trusts this peer (see §9).
3. **guacd → remote server (the outbound protocol boundary — CRITICAL).** guacd's C
   protocol plugins open a connection to the remote VNC/RDP/SSH/etc. endpoint. **The
   remote server's responses — framebuffer/image data, audio, clipboard, virtual
   channels, terminal/console escape sequences — flow back into C parsing code and
   then, as Guacamole-protocol instructions, all the way back to the browser.** This
   is the highest-value boundary in the whole system and the source of the
   project's memory-safety CVE history (see §7, §11a) *(documented — the security
   page lists VNC/RDP/SSH memory-corruption CVEs originating from remote-server or
   session data; inferred as to it being the load-bearing boundary — I2)*.
4. **guacd/web app → identity & storage backends.** LDAP/SAML/DB/RADIUS lookups
   during authentication, and connection-credential storage/retrieval *(documented
   — introduction lists the extensions; inferred as to trust treatment — I9)*.

**Reachability precondition per family.** A finding in a guacd protocol plugin is
in-model only if it is reachable from data the **remote server or the connected
session** controls (framebuffer, audio, clipboard, channel, console bytes). A
finding in libguac's Guacamole-protocol parser is in-model if reachable from the
bytes crossing boundary 2. A finding in the web app is in-model if reachable from
an **unauthenticated** request or from an **authenticated but unprivileged** user's
input. A finding reachable only from `guacamole.properties`, a connection
definition, or a plugin `.so` an operator installed is operator-controlled and
out of model (see §3) *(inferred — I10)*.

---

## §5 Assumptions about the environment

- **guacd host.** A POSIX/Linux host with a conformant C toolchain and allocator;
  guacd runs as a long-lived daemon, typically one process forking a child per
  connection *(inferred — I11)*.
- **Network placement.** guacd and the web app are co-located or on the same
  trusted network segment; guacd's `:4822` is **not** reachable by untrusted
  parties *(inferred — I3, see §10)*.
- **Web-app host.** A standard servlet container (e.g. Tomcat) fronted by a
  reverse proxy or the container itself terminating TLS *(inferred — I12)*.
- **TLS termination.** HTTPS for the browser boundary is provided by the operator's
  container/reverse proxy, not by Guacamole itself *(inferred — I12)*.
- **Concurrency.** guacd handles multiple simultaneous connections; the web app is
  multi-user and multi-session concurrently *(inferred — I11)*.
- **What we do *not* do to the host (negative inventory — high-priority to
  confirm).** We believe guacd does not read ambient environment variables as a
  trust input, does not phone home, and confines its outbound connections to the
  operator-defined targets; the web app does not execute operator-supplied code
  beyond the extensions an operator installs *(inferred — I13)*.

---

## §5a Build-time and configuration variants

The security envelope depends heavily on deployment configuration — "Guacamole" is
really a family of deployments. Load-bearing knobs:

| Knob | Default | Effect on the model | Maintainer stance (to confirm) |
| --- | --- | --- | --- |
| `guacd-ssl` | **off** — webapp↔guacd is plaintext *(documented — configuring-guacamole)* | Off ⇒ boundary 2 is unencrypted/unauthenticated; a network attacker between webapp and guacd can read/inject the Guacamole stream and connection credentials | I14 — is plaintext-on-trusted-network the supported posture (report against it = `OUT-OF-MODEL: non-default-build`), or should it be `VALID`? |
| `guacd-hostname` | `localhost` *(documented)* | Default localhost keeps guacd off the network; a non-local value widens boundary 2 | I14 |
| Which auth extension(s) are installed | none bundled by default; operator chooses DB/LDAP/SAML/etc. *(documented — introduction)* | Determines the entire authn/authz surface; a bare install has no persistent user store | I15 |
| MFA (TOTP/Duo) extension | not installed by default *(inferred — I15)* | Presence/absence changes the credential-theft threat | I15 |
| Cookie / transport hardening (secure flag, HTTPS) | historically operator-dependent (cf. CVE-2018-1340) *(documented — security page)* | Session-token confidentiality depends on TLS being terminated correctly | I12 |

---

## §6 Assumptions about inputs

We accept input at three principal surfaces: the browser-facing HTTP/tunnel
surface, the guacd socket, and — most importantly — the **inbound protocol stream
from the remote server** that guacd's C plugins parse.

**Per-parameter trust table.** First column is the endpoint / protocol message
rather than a function name, since these are services:

| Surface | Parameter / message | Attacker-controllable? | Caller/operator must enforce |
| --- | --- | --- | --- |
| Web app: login/REST endpoints | credentials, auth tokens, SAML assertions, headers | **yes** — unauthenticated web attacker | correct assertion/token validation (cf. CVE-2021-43999 SAML) *(documented — security page)* |
| Web app: tunnel servlet | tunnel/connection identifiers, session cookie | **yes** — authenticated user (and cross-user attempts) | tunnel-ID unpredictability & per-user scoping (cf. CVE-2021-41767) *(documented)* |
| Web app: file browser / SFTP names | filenames rendered in UI | **yes** — via remote session content | output encoding (cf. CVE-2016-1566 stored XSS) *(documented)* |
| Web app → guacd | connection instructions (protocol, hostname, port, credentials) | **no** — constructed by the trusted web app from operator-defined connections | `guacd-ssl` + network isolation of `:4822` *(inferred — I3)* |
| libguac | Guacamole-protocol element lengths/opcodes on the socket | at boundary 2, **yes** if the socket is exposed | length/element validation (cf. CVE-2023-30575, CVE-2012-4415) *(documented)* |
| guacd RDP plugin | virtual-channel PDUs, audio-input buffers, static channels | **yes** — malicious/compromised RDP server | bounds/UAF safety in C (cf. CVE-2020-9497/9498, CVE-2023-30576) *(documented)* |
| guacd VNC plugin | framebuffer/image geometry & pixel data | **yes** — malicious VNC server | integer-overflow safety on image buffers (cf. CVE-2023-43826) *(documented)* |
| guacd SSH/telnet plugin | terminal/console escape sequences | **yes** — remote shell / session content | console-code validation (cf. CVE-2024-35164) *(documented)* |
| guacd all plugins | clipboard, audio, printer/redirect data | **yes** — remote peer or session | safe parsing of variable-length remote data *(inferred — I2)* |

**Size/shape.** The Guacamole protocol is a streaming text protocol with
length-prefixed elements; remote-desktop streams are effectively unbounded and
real-time. We make no assumption that remote-server data is well-formed
*(inferred — I2)*.

---

## §7 Adversary model

We model four adversaries, in rough priority order:

1. **Malicious or compromised remote desktop server (peer).** The highest-value
   attacker for guacd. A user (or a MITM) points a Guacamole connection at a
   hostile RDP/VNC/SSH/telnet server, or an already-compromised legitimate server
   sends crafted framebuffer, audio, clipboard, virtual-channel, or console data.
   Goal: memory corruption / RCE inside guacd (the C process), or injection back
   into the browser via the Guacamole protocol. This adversary is *in scope* — the
   published CVE history is dominated by exactly these "malicious/compromised
   server → guacd memory corruption" cases *(documented — security page;
   inferred as the primary modeled adversary — I2)*.
2. **Unauthenticated web attacker.** Can reach the web app's HTTP surface with no
   valid session. Goal: authentication bypass, identity assumption, or reaching the
   tunnel without authorization *(inferred — I1)*.
3. **Authenticated but unprivileged browser user.** Holds a valid session. Goal:
   reach connections/data belonging to other users, escalate privilege, or read
   other sessions' identifiers or history (cf. CVE-2021-41767, CVE-2020-11997)
   *(documented — security page; inferred as modeled adversary — I1)*.
4. **Network attacker between browser↔web app or web app↔guacd.** Passive/active
   MITM. On the browser link this is defeated by operator TLS; on the guacd link it
   is defeated only by `guacd-ssl` + network isolation (see §5a) *(inferred —
   I12/I3)*.

**Explicitly *not* in scope:** the operator/administrator (§3, I6); an attacker
who already has code execution on the guacd or web-app host; and — as a *default*
posture — an attacker positioned on the trusted webapp↔guacd network segment when
the operator has followed the isolation guidance (§10). guacd does **not** treat
its connecting web app as an adversary (see §9) *(inferred — I3)*.

---

## §8 Security properties the project provides

Each property: statement + conditions, violation symptom, severity tier, provenance.

1. **Authentication of browser users before connection brokering.** Given a
   correctly-installed auth extension, the web app authenticates a user before
   issuing a tunnel to any connection. *Violation symptom:* unauthenticated access
   to a connection, or assuming another identity. *Severity:* **security-critical
   (CVE-class)** — cf. CVE-2021-43999 (SAML). *(documented — security page;
   inferred as a claimed property — I15)*
2. **Per-user authorization / connection isolation.** An authenticated user may
   reach only the connections and see only the session identifiers, history, and
   data they are authorized for; tunnel identifiers are not usable across users.
   *Violation symptom:* cross-user access to a live tunnel, connection history, or
   IP/log data. *Severity:* **security-critical (CVE-class)** — cf. CVE-2021-41767,
   CVE-2020-11997. *(documented — security page; inferred as claimed property —
   I1)*
3. **Memory safety of guacd on remote-server / session data (given the intended
   deployment).** guacd's C protocol plugins and libguac aim to parse untrusted
   framebuffer/audio/clipboard/virtual-channel/console/Guacamole-protocol data
   without out-of-bounds access, use-after-free, or integer-overflow-driven
   corruption. *Violation symptom:* crash, OOB read/write, UAF, or RCE in the guacd
   process driven by remote data. *Severity:* **security-critical (CVE-class)** —
   this is the project's dominant CVE category (CVE-2024-35164, CVE-2023-43826,
   CVE-2023-30576, CVE-2020-9497/9498, CVE-2012-4415). *(documented — security
   page; inferred as an intended, though C-caveated, guarantee — I2)*
4. **Correct Guacamole-protocol framing in libguac.** Protocol element lengths and
   opcodes are parsed and emitted consistently so that a peer cannot inject
   spurious instructions via length miscalculation. *Violation symptom:*
   instruction injection / desync. *Severity:* **security-critical** — cf.
   CVE-2023-30575. *(documented — security page; inferred as claimed — I2)*
5. **Output encoding of remote/session-derived strings in the web UI.** Filenames
   and other session-derived content are encoded so they cannot execute script in
   the browser. *Violation symptom:* stored/reflected XSS. *Severity:* **high** —
   cf. CVE-2016-1566. *(documented — security page; inferred as claimed — I16)*
6. **Session-token confidentiality controls.** Session cookies are issued with
   security-appropriate flags so tokens are not trivially interceptable when TLS is
   in use. *Violation symptom:* token interception / fixation. *Severity:* **high**
   — cf. CVE-2018-1340. *(documented — security page; inferred as claimed — I12)*

We make **no categorical resource-exhaustion (DoS) guarantee** at this layer:
whether a hostile remote server that maximizes CPU/bandwidth, or a client opening
many tunnels, constitutes a bug is unresolved and flagged in §9/§14 (I17)
*(inferred — I17)*.

---

## §9 Security properties the project does *not* provide

- **guacd is not an authentication or authorization boundary.** guacd trusts
  whatever connects to its socket and speaks the Guacamole protocol; it executes
  the connection instructions it is given (which protocol, which host, which
  credentials). Authorization is entirely the web app's job. A report that "guacd
  connected to an arbitrary host / used given credentials" is by design — the web
  app is responsible for only ever sending authorized instructions *(inferred —
  I3)*.
- **No confidentiality or authentication on the webapp↔guacd link by default.**
  Unless `guacd-ssl` is enabled and guacd is network-isolated, the Guacamole
  protocol stream — including connection credentials — crosses boundary 2 in
  cleartext to an unauthenticated peer *(documented — configuring-guacamole)*.
- **C memory-safety is best-effort, not a mathematical guarantee.** guacd and its
  plugins are C parsing untrusted, real-time, variable-length remote-protocol data.
  We treat remote-data-driven memory-corruption as security-critical and fix it as
  such (§8.3), but we do not claim the C surface is provably free of such bugs —
  this is precisely why the deployment guidance keeps guacd off untrusted networks
  *(inferred — I2)*.
- **We do not secure the remote desktops or their credentials.** Credential
  strength, target-server patch level, and target-server trustworthiness are the
  operator's responsibility (§3) *(inferred — I5)*.
- **No protection when auth is misconfigured or TLS is absent.** If no auth
  extension is installed, or the operator serves the web app over plain HTTP, the
  authentication and session-confidentiality properties (§8.1, §8.6) do not hold
  *(inferred — I12/I15)*.
- **No built-in defense of the browser session beyond documented mechanisms.** We
  do not claim general CSRF/clickjacking immunity beyond what the framework
  implements; operators layer their own controls where needed *(inferred — I16)*.

**False friends** (features easily mistaken for a stronger property):

- **The webapp↔guacd TCP connection is not an authenticated channel.** A reachable
  `:4822` is a full control channel over guacd, *not* merely "an internal port"
  *(inferred — I3)*.
- **`guacd-ssl` provides transport encryption, not peer authentication of the web
  app to guacd.** Do not read TLS on this link as "guacd now knows it is talking to
  the real web app" unless mutual authentication is configured *(inferred — I18)*.
- **A successful login is authentication, not authorization to a given target.**
  Reaching a connection still depends on the per-user authorization layer (§8.2)
  *(inferred — I1)*.

**Well-known attack classes we leave to the operator/caller:** MITM on the
webapp↔guacd link absent `guacd-ssl`; malicious-remote-server "reverse" attacks on
the RDP/VNC/SSH clients (the class behind CVE-2020-9497/9498); credential theft if
the gateway host itself is compromised; and DoS from hostile remote streams
(§8, unresolved I17).

---

## §10 Downstream (operator) responsibilities

For the assumptions in §5–§7 to hold, the *operator* must:

- **Keep guacd off untrusted networks.** Bind `:4822` to localhost or a trusted
  segment; never expose guacd directly to the Internet or to untrusted clients
  *(inferred — I3)*.
- **Enable `guacd-ssl`** (and, where supported, mutual authentication) whenever the
  webapp↔guacd link crosses any boundary that is not fully trusted *(documented —
  configuring-guacamole)*.
- **Terminate TLS for the browser** and ensure session cookies are served only over
  HTTPS *(inferred — I12; cf. CVE-2018-1340)*.
- **Install and correctly configure an authentication extension** (DB/LDAP/SAML/…)
  and, for sensitive deployments, an MFA extension (TOTP/Duo); never run a
  bare/no-auth install as production *(inferred — I15)*.
- **Only point connections at trusted remote servers, and trust their protocol data
  accordingly** — because that data reaches guacd's C parsers, connecting to a
  hostile server is connecting an attacker to guacd *(inferred — I2/I5)*.
- **Restrict and patch the gateway hosts**; treat `guacamole.properties`, connection
  definitions, and stored connection credentials as secrets *(inferred — I6)*.
- **Keep guacd and the web app patched together** — memory-safety fixes ship in
  guacd releases; run matching versions *(inferred — I11)*.

---

## §11 Known misuse patterns

- **Exposing guacd's `:4822` to untrusted networks.** Turns an internal proxy into
  a remotely-controllable, unauthenticated gateway; anyone who can reach the port
  can drive guacd. *Instead:* isolate the port and use `guacd-ssl` *(inferred —
  I3)*.
- **Serving the web app over plain HTTP.** Exposes session tokens and credentials
  in transit. *Instead:* terminate TLS and set secure cookie flags *(inferred —
  I12)*.
- **Running with no / weak authentication.** Treating a demo/no-auth install as
  production. *Instead:* install and harden an auth (and MFA) extension *(inferred
  — I15)*.
- **Connecting users to untrusted remote servers.** Because remote-server data is
  parsed by guacd's C code, this hands an attacker the memory-safety surface.
  *Instead:* restrict connection targets to servers the operator trusts *(inferred
  — I2)*.
- **Assuming guacd enforces access control.** Relying on guacd (rather than the web
  app) to decide who may reach what. *Instead:* enforce authorization in the web
  app; guacd is not a policy point *(inferred — I3)*.

---

## §11a Known non-findings (recurring false positives)

- **"guacd connects to an attacker-specified host / uses supplied credentials."**
  By design — guacd executes the connection instructions the trusted web app sends
  (§9). `BY-DESIGN` unless the instructions are reachable from an *unauthenticated
  or cross-user* web-app path (then it is a §8.1/§8.2 issue) *(inferred — I3)*.
- **"Cleartext webapp↔guacd traffic / credentials on `:4822`."** Expected when
  `guacd-ssl` is off and the port is on a trusted segment; `OUT-OF-MODEL:
  non-default-build` / operator-config, pending the I14 ruling *(documented —
  configuring-guacamole)*.
- **Findings in `apache/guacamole-website`.** Static site, no runtime surface;
  `OUT-OF-MODEL: unsupported-component` (§3) *(inferred — I4)*.
- **Dependency CVEs the project has already assessed as non-impacting** — e.g.
  CVE-2023-5129 (WebP decode, not encode), CVE-2021-44228 (Log4j; Guacamole uses
  Logback), and the routinely-reviewed AngularJS advisories. `KNOWN-NON-FINDING`
  *(documented — security page)*.
- **Reports in test/example/build code** across the repos — `OUT-OF-MODEL:
  unsupported-component` (§3) *(inferred — I8)*.
- **"guacd crashes / uses CPU on malformed data from a host the operator chose to
  trust but that is hostile."** In-model as a *memory-safety* issue (§8.3); but a
  pure *resource-exhaustion* report is unresolved pending I17 *(inferred — I17)*.

---

## §12 Conditions that would change this model

- guacd gaining its own authentication of the connecting web app by default (would
  rewrite §9 and the I14/I18 questions).
- A new protocol plugin, or guacd accepting a new remote data type (new untrusted-C
  input surface).
- A change to the `guacd-ssl` or `guacd-hostname` default (§5a).
- A new web-app auth extension or a change to the default auth posture.
- guacd being repositioned as a network-facing service rather than an internal
  proxy.
- **Evidence the model is incomplete:** any report that cannot be routed cleanly to
  a §13 disposition is a `MODEL-GAP` and triggers a revision here — not an ad-hoc
  call.

---

## §13 Triage dispositions

| Disposition | Meaning for Guacamole | Licensed by |
| --- | --- | --- |
| `VALID` | Violates a §8 property via an in-scope adversary/input — e.g. remote-server data corrupting guacd memory, an unauthenticated auth bypass, or cross-user tunnel access. | §8, §6, §7 |
| `VALID-HARDENING` | No §8 property broken, but the API/UX makes a §11 misuse easy enough to warrant hardening; fixed at maintainer discretion, usually no CVE. | §11 |
| `OUT-OF-MODEL: trusted-input` | Requires attacker control of a parameter marked trusted in §6 — e.g. the web-app→guacd connection instructions, `guacamole.properties`, or a connection definition. | §6 |
| `OUT-OF-MODEL: adversary-not-in-scope` | Requires an excluded capability — operator/host code execution, or a trusted-segment position when isolation guidance was followed. | §7 |
| `OUT-OF-MODEL: unsupported-component` | Lands in `guacamole-website`, or in test/example/build code. | §3 |
| `OUT-OF-MODEL: non-default-build` | Only manifests under a discouraged/non-default config (e.g. cleartext `:4822` exposed, no auth extension) — pending the I14/I15 rulings. | §5a |
| `BY-DESIGN: property-disclaimed` | Concerns a property in §9 — e.g. guacd trusting its web-app peer, or lack of default webapp↔guacd auth. | §9 |
| `KNOWN-NON-FINDING` | Matches a documented recurring false positive (dependency CVEs already assessed, website findings). | §11a |
| `MODEL-GAP` | Cannot be routed to any of the above — revise the model. | triggers §12 |

---

## §14 Open questions for the maintainers

Every *(inferred)* tag above routes to a question here. Each states a **proposed
answer** for the PMC to confirm, correct, or strike, and the section it lands in.
Grouped into waves.

### Wave 1 — scope, the guacd trust posture, and insecure defaults (highest leverage)

1. **(I3) guacd trusts its connecting web app and is not an auth boundary; it must
   be kept off untrusted networks.** Proposed: correct — guacd executes the
   Guacamole-protocol instructions it receives, authorization lives in the web app,
   and exposing `:4822` is a misconfiguration, not an in-model attack. *Lands in
   §4/§7/§9/§10/§11a.*
2. **(I2) The primary modeled guacd adversary is a malicious/compromised remote
   desktop server whose data reaches C parsers; remote-data-driven memory
   corruption is security-critical.** Proposed: correct — consistent with the CVE
   history. Confirm this is the intended framing and that clipboard/audio/printer
   channels are included. *Lands in §6/§7/§8.3/§9.*
3. **(I14) Is plaintext, unauthenticated webapp↔guacd (the `guacd-ssl`=off /
   `guacd-hostname`=localhost default) the *supported production posture on a
   trusted segment*, making a report against cleartext `:4822`
   `OUT-OF-MODEL: non-default-build`?** Or should such a report be `VALID`?
   Proposed: supported-on-trusted-segment; `guacd-ssl` required once the link is
   not fully trusted. *Lands in §5a/§9/§13.*
4. **(I4/I8) `guacamole-website` and all test/example/build code are out of model
   for runtime findings (in scope only for discoverability of the security policy
   and docs).** Proposed: correct. *Lands in §2/§3/§11a.*
5. **(I5/I6) The remote desktops, their credentials, and the operator/admin are out
   of the adversary model.** Proposed: correct. *Lands in §3/§7.*

### Wave 2 — web-app authn/authz surface

6. **(I1) The modeled web adversaries are the unauthenticated web attacker and the
   authenticated-but-unprivileged user; the claimed properties are user
   authentication (§8.1) and per-user connection/tunnel/history isolation (§8.2).**
   Proposed: correct — consistent with CVE-2021-43999/41767/2020-11997. *Lands in
   §6/§7/§8.*
7. **(I15) With no auth extension installed there is no production authentication;
   operators must install one (and MFA for sensitive deployments), and a no-auth
   install is `OUT-OF-MODEL: non-default-build`.** Proposed: correct. *Lands in
   §5a/§8.1/§9/§10.*
8. **(I16) Output encoding of session/remote-derived strings (§8.5) is a claimed
   property (CVE-2016-1566), but we make no blanket CSRF/clickjacking guarantee
   beyond framework mechanisms.** Proposed: correct. *Lands in §8/§9.*
9. **(I9) Identity/storage backends (LDAP/SAML/DB/RADIUS) are trusted infrastructure
   configured by the operator; compromise of those backends is out of model.**
   Proposed: correct. *Lands in §4/§6.*

### Wave 3 — environment, transport, and resource guarantees

10. **(I12) TLS termination and secure-cookie handling for the browser boundary are
    the operator's responsibility (cf. CVE-2018-1340); absent TLS, §8.6 does not
    hold.** Proposed: correct. *Lands in §5/§8.6/§10.*
11. **(I18) `guacd-ssl` provides transport encryption but not, by itself, mutual
    authentication of the web app to guacd (false friend).** Proposed: correct —
    confirm whether any mutual-auth mechanism exists on this link. *Lands in §9.*
12. **(I11/I13) guacd runs as a forking POSIX daemon, does not treat environment
    variables as a trust input, does not phone home, and confines outbound
    connections to operator-defined targets; guacd and the web app are expected to
    run matching patched versions.** Proposed: correct — this negative-side-effects
    inventory is the one most needing maintainer confirmation. *Lands in §5.*
13. **(I17) Resource guarantees: is there *any* categorical line on DoS from a
    hostile remote stream or a client opening many tunnels — e.g. "a crash/UAF is a
    bug, but sustained CPU/bandwidth from a connected server is not"?** Proposed: no
    categorical resource guarantee at this layer; memory-safety failures remain
    bugs. *Lands in §8/§9/§11a.*
14. **(I7/I10) Third-party protocol libraries (FreeRDP/libVNCserver/libssh2/Cairo,
    bundled JS) are modeled only at the boundary where we hand them data; internal
    CVEs are triaged upstream. Reachability preconditions per §4 are the triage
    test.** Proposed: correct. *Lands in §3/§4.*

### Wave 4 — meta

15. **Document ownership & coexistence.** There is currently **no `SECURITY.md` in
    either code repository** — the sole published security artifact is the website
    security page. Proposed: this threat model becomes the canonical model that the
    security page (and new `SECURITY.md` files in both repos) link to. Confirm the
    venue and whether the PMC wants the machine-readable §15 sidecar. *Meta — no
    body claim.*

---

## §15 Optional: machine-readable companion

Deferred for v0. Once the PMC ratifies §6/§8/§9/§11a/§13, we can emit a
`threat-model.yaml` sidecar encoding: entry points → per-parameter trust level;
component families (guacd / web app / extensions / website) → in/out of scope;
config knobs (`guacd-ssl`, `guacd-hostname`, auth-extension presence) →
security-relevant + default; claimed properties → severity + violation symptom;
disclaimed properties and false friends; known non-findings; and the §13
disposition labels — as a derived triage index for automated scanning. The prose
document remains canonical.
