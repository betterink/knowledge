# Manifest

The manifest endpoint returns the vault index built once at startup: every allowed folder, and every allowed file's content version. A client diffs it against its local copy to decide which files to fetch.

## Sub-features

1. `manifest-files` maps each allowed vault-relative file path to `{hash, size, modified}`, ordered by path. `hash` is the SHA-256 of the file bytes.
2. `manifest-folders` lists every allowed folder path, sorted, including empty folders. The vault root is not listed.
3. `manifest-exclusions` omits excluded folders (`.git`, `.obsidian`, `.claude`, `.trash`), unknown formats and symlinks, the same set the workspace tree omits.
4. `manifest-tree` agrees with `/v1/workspace`: every file node's `version` equals its manifest entry.
5. `manifest-snapshot` serves the startup index. Files added after launch are absent until a restart.

## How to get to it (user POV)

1. `GET /v1/manifest`.

## Driving it with verify.sh

Preconditions: baseline.

1. **Fetch.** Run `verify.sh req manifest /v1/manifest`. Status `200`, `Content-Type: application/json`.
2. **Files.** `jq -r '.files | keys_unsorted | join(",")' $VERIFY_RUN/evidence/manifest.body` prints exactly `A/Other.md,A/Same.md,B/Same.md,Home.md,Notes.base,Projects/Diagram.excalidraw.md,Projects/Projects.md,Projects/image.png`. No `.obsidian`, `notes.txt` or `Escape.md`.
3. **Folders.** `jq -r '.folders | join(",")' $VERIFY_RUN/evidence/manifest.body` prints exactly `A,B,Empty,Projects`.
4. **Hash.** `jq -r '.files["A/Same.md"].hash' $VERIFY_RUN/evidence/manifest.body` equals `shasum -a 256 "$VERIFY_RUN/instance/$(cat "$VERIFY_RUN/vault")/A/Same.md" | cut -d' ' -f1`. Both print `ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb`, and `size` is `1`.
5. **Tree agreement.** Run `verify.sh req workspace /v1/workspace`, then `jq -n --slurpfile m $VERIFY_RUN/evidence/manifest.body --slurpfile w $VERIFY_RUN/evidence/workspace.body '[$w[0] | .. | objects | select(.kind|IN("page","diagram","base","attachment")) | {(.path): .version}] | add == $m[0].files'`. Prints `true`.
6. **Snapshot.** Run this step last, since it changes the vault. `printf x > "$VERIFY_RUN/instance/$(cat "$VERIFY_RUN/vault")/Late.md"`, then `verify.sh req manifest-snapshot /v1/manifest` and `verify.sh req file-late /v1/files/Late.md`. The manifest has no `Late.md` key, while the file request returns `200` with body `x`.

## Gotchas

1. Use `keys_unsorted` to read the server's order. `keys` re-sorts in jq and hides an ordering bug.
2. `modified` is the file mtime in epoch milliseconds and changes with every launch's seed. Assert `hash` and `size`.
3. The index is built once at startup, so restart (`stop`, then `launch`) to see disk changes until the watcher lands. `/v1/files` reads from disk on every request.
4. The server logs `Indexed vault: <files> files, <folders> folders in <ms> ms` once at startup. `stop` copies it to `evidence/server.log`.
