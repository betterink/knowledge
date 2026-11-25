# ADR-002: Obsidian Sync with obsidian-headless until self-hosting

Status: Accepted

## Context

The backend needs a live file copy of the vault (FR-0.1). A self-hosted sync stack is not yet available.

## Decision

Use Obsidian Sync. The backend reads and writes the vault through `obsidian-headless` (official, open beta, Node.js 22+) running as its own Docker Compose service on a shared volume.

## Consequences

1. Requires an Obsidian Sync subscription.
2. `obsidian-headless` is beta; a storage adapter interface isolates it so Self-hosted LiveSync can replace it without tablet changes.
3. Sync credentials live only on the backend.
