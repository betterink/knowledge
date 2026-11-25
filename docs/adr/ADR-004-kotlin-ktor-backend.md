# ADR-004: Kotlin (JVM) with Ktor for the backend

Status: Accepted

## Context

`obsidian-headless` runs as its own Node service and models are called over HTTP, so the backend language is independent of both.

## Decision

Write the backend in Kotlin (JVM, JDK 21) using Ktor, built with Gradle.

1. Model interaction uses the official Anthropic Java SDK.
2. Markdown edits are applied as text splices at commonmark-java source spans; the AST is not re-serialised.
3. Three-way merges use JGit `MergeAlgorithm`.
4. Excalidraw JSON uses kotlinx.serialization.

## Alternatives

TypeScript was considered (single runtime with `obsidian-headless`, stronger markdown tooling) but not chosen.

## Consequences

1. Kotlin aligns with the planned rm-toolkit Kotlin SDK; shared models are possible via Kotlin Multiplatform.
2. Custom model training (Module 5) remains in Python as offline tooling.
