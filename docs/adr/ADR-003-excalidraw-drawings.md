# ADR-003: Excalidraw for non-markdown drawings

Status: Accepted

## Context

Diagrams, drawings and markings captured on the tablet cannot always be represented or converted into markdown.

## Decision

Store such content as Excalidraw diagrams (`.excalidraw.md`) via the Obsidian Excalidraw plugin, handled by a diagram module. Unconverted strokes fall back to Excalidraw freedraw elements.

## Consequences

1. The plugin version is pinned; the `.excalidraw.md` format is undocumented and is guarded by golden files.
2. In-drawing merge and editing existing drawings on the tablet are out of scope for v1.
