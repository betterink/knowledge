---
name: verify-core
description: Launch and drive the rm-knowledge core Ktor HTTP service (core/) against a disposable seeded Obsidian vault, and capture HTTP evidence. Use to prove behavior of /health, /v1/workspace (tree, titles, folder notes, links), /v1/manifest (startup index, file hashes) or /v1/files (bytes, ETag, path policy) after a change in core/.
---

# Verify core

`core/` is a Ktor service that reads an Obsidian vault from `VAULT_ROOT` and serves it over HTTP. Its only surface is HTTP, so it is driven with `curl` through `verify.sh`. The tablet app (`client/tablet/`) needs the physical device and is not covered here.

All commands run from the repo root. `verify.sh` is `.claude/skills/verify-core/verify.sh`.

## Launch

```sh
export VERIFY_RUN="$(.claude/skills/verify-core/verify.sh launch)"
```

1. Runs `./gradlew installDist` in `core/`, so the current working tree is what runs.
2. Creates `$TMPDIR/verify-core/<run-id>/` with `instance/` (vault, server log) and `evidence/`.
3. Seeds `instance/vault-<run-id>/` with the fixture described in [features/README.md](features/README.md).
4. Starts the server on a free port with `PORT` and `VAULT_ROOT` set, and stores `pid`, `port` and `vault` in the run dir.
5. Ready means `/health` returns `ok`. It prints the run dir only when ready, or exits non-zero with the server log.

Each launch gets its own port and vault, so parallel runs are safe. Never drive port 8080 or a server this run did not start.

## Doctor

```sh
.claude/skills/verify-core/verify.sh doctor
```

Read-only. Passes only if the stored pid is alive, it owns the stored port, `/health` is `ok`, and `/v1/workspace` reports `name` equal to this run's vault directory name. Run it first, and again whenever a response looks wrong.

## Drive

```sh
.claude/skills/verify-core/verify.sh req <label> <path>
```

Sends `GET http://127.0.0.1:<port><path>` with `curl --path-as-is` and prints `<status> <body-file>`. Routes are in `core/src/main/kotlin/ink/knowledge/Application.kt` and `controller/workspace/WorkspaceController.kt`:

1. `GET /health` returns `ok`.
2. `GET /v1/workspace` returns the vault tree as JSON. Nodes carry a `kind` discriminator: `folder`, `page`, `diagram`, `base`, `attachment`.
3. `GET /v1/manifest` returns the startup vault index as JSON: `folders` (sorted paths) and `files` (path to `{hash, size, modified}`).
4. `GET /v1/files/{path...}` returns raw bytes with `ETag: "<sha256>"` and a `Content-Type` from `FileFormat`.

Assert with `jq` on the saved body, for example:

```sh
jq -r '.. | objects | select(.kind=="page" and .path=="Home.md") | .title' "$VERIFY_RUN/evidence/workspace.body"
```

Per-feature recipes are in [features/README.md](features/README.md).

## Evidence

For each `req`, `$VERIFY_RUN/evidence/` gets `<label>.request`, `<label>.headers`, `<label>.body` and `<label>.status`. `stop` also copies `server.log` there.

1. Drive the real HTTP route. Never call services directly or use `testApplication` as proof.
2. Assert status, headers and body together. A 200 alone proves nothing.
3. For a change to file reading, compare the body and ETag against the seeded file (`shasum -a 256`).
4. The service is read-only today. If a write route is added, also check the vault file on disk after the call.

## Cleanup

```sh
.claude/skills/verify-core/verify.sh stop
```

Kills only the stored pid, copies the server log into `evidence/`, and deletes `instance/` (vault and pid). `evidence/` stays at `$VERIFY_RUN/evidence`. Run `stop` after every attempt, including failed ones.
