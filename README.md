# knowledge

Obsidian-backed knowledge base for the reMarkable Paper Pro.

| Directory | Contents |
| --- | --- |
| `client/tablet/app/` | Qt Quick tablet application |
| `client/tablet/toolkit/` | Ink, pen navigation and display control, copied from rm-toolkit (`rm::toolkit`) |
| `client/tablet/deploy/` | SDK container, SDK candidate and on-device launcher |
| `core/` | Kotlin + Ktor backend (Gradle) |
| `tools/` | Host-side build, test and deploy scripts |

## Requirements

1. macOS with Homebrew `qt` and `cmake`.
2. Docker Desktop (tablet build).
3. JDK 21 (backend).
4. SSH alias `rm` for the tablet.

## Tablet

```sh
python3 tools/tablet.py test                  # Mac build and native tests
python3 tools/tablet.py preview               # Mac preview window
python3 tools/tablet.py build                 # SDK container build -> dist/knowledge.tar.gz
python3 tools/tablet.py deploy                # install to /home/root/knowledge
python3 tools/tablet.py run --pen-input evdevtablet
python3 tools/tablet.py stop
```

`run` stops Xochitl for a 10-minute session and restores it on exit. See `client/tablet/toolkit/DISPLAY.md` for display-bridge limitations.

## Backend

```sh
cd core
./gradlew build   # compile, test, ktlint
./gradlew run     # http://localhost:8080/health
./gradlew ktlintFormat
```

## Self-hosting

The backend runs in Docker Compose and serves a vault folder that the host already syncs. The vault is mounted read-only.

1. `cp .env.example .env`
2. Set `VAULT_PATH` in `.env` to the vault folder. If the vault is not world-readable, also set `CONTAINER_USER` to its owner's `UID:GID`.
3. `docker compose up -d --build`
4. `curl localhost:8080/health` prints `ok` (use your `PORT` if changed).
5. For tablet access, set `BIND_ADDRESS=0.0.0.0` (or a Tailscale IP) and run `docker compose up -d`. There is no auth yet, so anyone who can reach the port can read the vault.

The index is built at startup, so run `docker compose restart backend` to pick up vault changes until the watcher lands.
