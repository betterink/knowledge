# rm-knowledge

Obsidian-backed knowledge base for the reMarkable Paper Pro.

| Directory | Contents |
| --- | --- |
| `tablet/app/` | Qt Quick tablet application |
| `tablet/toolkit/` | Ink, pen navigation and display control, copied from rm-toolkit (`rm::toolkit`) |
| `tablet/deploy/` | SDK container, SDK candidate and on-device launcher |
| `backend/` | Kotlin + Ktor backend (Gradle) |
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
python3 tools/tablet.py build                 # SDK container build -> dist/rm-knowledge.tar.gz
python3 tools/tablet.py deploy                # install to /home/root/rm-knowledge
python3 tools/tablet.py run --pen-input evdevtablet
python3 tools/tablet.py stop
```

`run` stops Xochitl for a 10-minute session and restores it on exit. See `tablet/toolkit/DISPLAY.md` for display-bridge limitations.

## Backend

```sh
cd backend
./gradlew build   # compile, test, ktlint
./gradlew run     # http://localhost:8080/health
./gradlew ktlintFormat
```
