# Health

Health lets a client or supervisor confirm the service is up before using it.

## Sub-features

1. `health-ok` returns `200` with body `ok`.

## How to get to it (user POV)

1. `GET /health`.

## Driving it with verify.sh

Preconditions: baseline.

1. **Probe.** Run `verify.sh req health-ok /health`. Status `200`, body `ok`.

## Gotchas

1. `/health` does not touch the vault. A healthy service can still serve the wrong vault. Use `doctor`, which also checks the vault name.
