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
