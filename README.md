# rm-knowledge

Obsidian-backed knowledge base for the reMarkable Paper Pro.

| Directory | Contents |
| --- | --- |
| `client/tablet/app/` | Qt Quick tablet application |
| `client/tablet/toolkit/` | Reusable tablet modules; `rm::toolkit` links all of them |
| `client/tablet/toolkit/interaction/` | Ink, pen navigation, display control and QML components (`rm::interaction`) |
| `client/tablet/toolkit/network/` | HTTP(S) client over Qt Network (`rm::network`) |
| `client/tablet/toolkit/storage/` | SQLite database with versioned migrations, statically linked (`rm::storage`) |
| `client/tablet/toolkit/examples/interaction-benchmark/` | Scroll, drag and pen test app for comparing display modes |
| `client/tablet/api/` | C++ API models generated from core's serializers (`rm::api`). Regenerate with `cd core && ./gradlew generateClientModels` |
| `client/tablet/deploy/` | SDK container, SDK candidate and on-device launcher |
| `core/` | Kotlin + Ktor backend (Gradle) |
| `docs/adr/` | Architecture decision records |
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
python3 tools/tablet.py build                 # SDK container build -> dist/<app>.tar.gz
python3 tools/tablet.py deploy                # install to /home/root/rm-knowledge
python3 tools/tablet.py run --pen-input evdevtablet --server https://host:8080
python3 tools/tablet.py stop
```

`preview`, `deploy`, `run`, `stop` and `logs` take `--app interaction-benchmark` to target the benchmark instead of `rm-knowledge`.

`run` stops Xochitl for a 10-minute session and restores it on exit. See `client/tablet/toolkit/interaction/DISPLAY.md` for display-bridge limitations.

The app reads the backend URL from `RM_KNOWLEDGE_SERVER` and stores its database at `/home/root/.local/share/rm-knowledge/rm-knowledge.db` on the tablet.

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
