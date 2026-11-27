# Spike: Qt Network and SQLite on the Paper Pro (ENG-49)

Goal: confirm the ferrari SDK sysroot and 3.29 device support HTTPS and local
SQLite storage (FR-1.3, FR-1.5), then define tablet modules for both.

Target architecture: the tablet application calls a separately hosted Kotlin
backend over HTTPS. All local changes are written to SQLite first and synced
when the backend is reachable.

## Result

Both requirements are met on device firmware 3.29.0.153 (verified 2026-11-25).

1. HTTPS: Qt Network 6.10.3 with the OpenSSL TLS backend, linked against the
   device's system Qt. `GET https://example.com/` and `https://www.google.com/`
   returned 200.
2. SQLite: the SDK's `libsqlite3.a` (3.45.3) is linked statically. Writes to
   `/home/root/network-storage-probe/probe.db` (WAL mode) succeeded and persisted
   across runs.
3. Qt Sql is not used. `libQt6Sql` ships, but neither the SDK nor the device
   includes the `QSQLITE` driver plugin (`plugins/sqldrivers` is absent).

Probe source: removed after `rm::network` and `rm::storage` replaced it; see
commit d1723ef.

## Evidence

| Item | SDK 3.28.0.172 sysroot | Device 3.29.0.153 |
| --- | --- | --- |
| Qt version | 6.10.3 (`QT_VERSION_STR`) | 6.10.3 (`libQt6Core.so.6.10.3`) |
| `libQt6Network` | Present + CMake config | Present |
| TLS plugin `tls/libqopensslbackend.so` | Present | Present (also `libqcertonlybackend.so`) |
| `QRestAccessManager` header | Present | n/a (in `libQt6Network` 6.7+) |
| `networkinformation/libqnetworkmanager.so` | CMake config present | Present |
| `libssl.so.3` / `libcrypto.so.3` | Present | Present |
| CA bundle | n/a | `/etc/ssl/certs/ca-certificates.crt`, 243 entries |
| `libQt6Sql` | Present + CMake config | Present |
| `QSQLITE` driver plugin | Absent | Absent |
| SQLite | `sqlite3.h`, `libsqlite3.a`/`.so`, 3.45.3 | `libsqlite3.so.0`, 3.51.3 |
| libcurl | `libcurl.a`/`.so` 4.8.0, headers | `libcurl.so.4.8.0` |
| Root filesystem | n/a | `/` ext4 read-only, 91% used (42.5M free) |
| Data partition | n/a | `/home` encrypted ext4, read-write, 45.7G free |

Probe runtime dependencies (`tools/common.py elf_dependencies`):
`libQt6Network.so.6, libQt6Core.so.6, libstdc++.so.6, libm.so.6, libgcc_s.so.1, libc.so.6`.

Probe output on device:

```
qt 6.10.3 tls=1 backend=openssl
sqlite 3.45.3 rows=1
https https://example.com/ status=200 error=Unknown error
```

## Decisions

1. HTTP: `QNetworkAccessManager` from the system Qt. No libcurl: it would
   duplicate the event loop integration Qt already provides.
2. Storage: SQLite C API, statically linked from the SDK. This removes any
   runtime dependency on the device's SQLite version, which already differs
   from the SDK (3.45.3 vs 3.51.3).
3. Database location: under `/home/root`. The root filesystem is read-only and
   nearly full.

## Not yet verified

1. Certificate rejection: every `badssl.com` endpoint, including its valid
   control page, failed at handshake from the device, so the result does not
   show whether untrusted certificates are rejected. Retest against the Kotlin
   backend with a self-signed certificate.
2. Offline behaviour: run the probe in airplane mode; expect exit code 2 and a
   successful SQLite write.
3. `/home` persistence across a firmware update (expected, as it is a separate
   partition).

## Proposed tablet modules

Separate CMake targets under `tablet/`, independent of `rm::toolkit`.

1. `rm::network` (`tablet/network/`, Qt6::Network)
   1. `HttpClient`: base URL, auth header, JSON request/response, transfer timeout.
   2. Retry: exponential backoff with jitter on network errors, 408, 429, 5xx;
      honour `Retry-After` (FR-1.5).
   3. TLS trust: system CA bundle by default; optional pinned CA or certificate
      for a self-hosted backend.
   4. `Connectivity`: `QNetworkInformation` reachability signal (FR-1.4).
2. `rm::store` (`tablet/store/`, static SQLite)
   1. `Database`: open/close, WAL, transactions, versioned migrations.
   2. `Preferences`: key/value table.
   3. `Outbox`: durable change queue (enqueue, next batch, ack, attempt count,
      last error). Every local change is enqueued in the same transaction as
      the data write.
3. `rm::sync` (optional, depends on both): drains the outbox through
   `HttpClient` when `Connectivity` reports online; applies retry policy.
4. Application schema (vault copy, base versions for merging) lives in
   `tablet/app/`.

## Open questions

1. Backend TLS certificate: public CA, or self-signed / private CA on the LAN?
2. Authentication: bearer token, API key, or mutual TLS?
3. Separate `rm::sync` target, or does `tablet/app/` drive the outbox directly?
4. Conflict policy when the server copy changed while offline: server
   resolves, or tablet merges using base versions (FR-1.3)?
5. Will the Kotlin SDK on the tablet call these modules (C ABI), or only C++?
