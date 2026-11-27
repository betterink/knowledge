---
name: verify-vault-path-policy
description: Bounded exhaustive check that core's FileService never serves a file outside the vault, under an excluded folder, or in an unknown format, for every request path up to 4 segments over an adversarial alphabet. Use after any change to FileService path handling, VaultProperties.excluded, FileFormat, or the /v1/files route.
---

# Verify vault path policy

## Property

1. **Containment.** If `FileService.read(path)` returns bytes, the file it read is a regular file inside the vault, not under an excluded folder (`.git`, `.obsidian`, `.claude`, `.trash`), and in a known `FileFormat`.
2. **No 500s.** Every path either returns bytes or throws `NotFoundException` (404) or `IllegalArgumentException` (400).

Harm if broken: `GET /v1/files/...` leaks host files, or `.git`/`.claude` contents, to any client.

## Tool and scope

There is no formal verifier in this project's JVM toolchain, and adding one would be a new dependency. The check is a bounded exhaustive test that runs the real `FileService` against a real filesystem, using only `kotlin.test`.

1. Inputs: every `/`-joined sequence of 1 to 4 tokens from `"" . .. A Same.md .claude .CLAUDE CLAUDE.md Escape.md LinkDir secret.md Hidden Up vault`. That is 41,370 paths.
2. Vault: `VaultFactory.create()` plus `.claude/CLAUDE.md`, `Escape.md` (a file symlink to `../outside/secret.md`), `LinkDir` (a directory symlink to `../outside`), `Hidden` (a symlink to `.claude`) and `Up` (a symlink to the vault's parent).
3. Oracle: the allowed set is every non-symlink regular file under the vault with no excluded path segment and a known format. A served path passes if `Files.isSameFile` matches one of them, so case-insensitive filesystem aliases such as `.CLAUDE` are caught.

Not covered:

1. Paths longer than 4 segments, or tokens outside the alphabet (unicode, `\`, NUL).
2. Ktor URL decoding before `FileService`. The `verify-core` skill covers `%2E%2E` over HTTP.
3. Races where the vault changes between the policy check and the read (TOCTOU).
4. `version()` and `index()`. `version()` shares `existingFile` with `read()`.

## Coverage record

| Check | Code |
| --- | --- |
| Both properties | `core/src/main/kotlin/ink/knowledge/service/vault/FileService.kt`: `read`, `existingFile`, `resolve`, `isAllowed` |
| Format filter | `core/src/main/kotlin/ink/knowledge/enums/vault/FileFormat.kt`: `of` |
| Excluded set | `core/src/main/resources/application.conf`: `vault.excluded` |
| The check | `core/src/test/kotlin/ink/knowledge/service/vault/FileServicePathPolicyTest.kt` |

Last confirmed in sync: commit `1ca7a0b` on 2026-11-26. Move this forward only after rerunning the check and confirming that the model still matches the code at the new commit.

## Does a change touch this?

```sh
git diff 1ca7a0b --stat -- core/src/main/kotlin/ink/knowledge/service/vault/FileService.kt core/src/main/kotlin/ink/knowledge/enums/vault/FileFormat.kt core/src/main/resources/application.conf core/src/main/kotlin/ink/knowledge/controller/workspace/WorkspaceController.kt
```

Any output means rerun the check.

## Run

```sh
cd core && ./gradlew test --tests '*FileServicePathPolicyTest'
```

The run takes about 1 second. A pass prints nothing with `-q`. On failure, the message lists up to 20 lines of the form `<request path> -> <real file served> (<bytes>)`. An uncaught exception other than 404/400 fails property 2 and names the path in the stack trace.

## Reading a failure

1. **Real bug:** a listed path serves a file outside the allowed set. Report it as "requesting `<path>` returns `<real file>`". Do not edit the alphabet, the oracle or the vault to make it pass.
2. **Oracle error:** the served file is allowed, but the oracle missed it. For example, comparing an unnormalized path makes `A/Same.md/.` fail with `Not a directory`. Fix the oracle only if `FileService` normalizes the path the same way.
3. **Intended policy change** (for example, a new excluded folder or a new format): update `application.conf` or `FileFormat`, add a token or a fixture that exercises it, rerun, and update the coverage record.

## Proof it can fail

Each mutation was run against the real code on 2026-11-26 and then reverted.

1. Removing `require(real.startsWith(realRoot))` fails with `Escape.md -> .../outside/secret.md` and `LinkDir/secret.md -> .../outside/secret.md`.
2. Removing `require(realRoot.relativize(real).none { ... })` fails with `Hidden/CLAUDE.md -> .../vault/.claude/CLAUDE.md` and `.CLAUDE/CLAUDE.md -> .../vault/.claude/CLAUDE.md`. The second one shows that the case-insensitive alias of an excluded folder is blocked only by the real-path check.

Repeat one mutation after any large edit to `existingFile` to confirm the check still bites.
