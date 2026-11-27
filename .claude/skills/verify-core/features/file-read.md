# File read

The files endpoint returns one vault file's raw bytes with a content hash, and refuses paths outside the vault's allowed set.

## Sub-features

1. `file-bytes` returns the exact file bytes.
2. `file-etag` sets `ETag` to the quoted SHA-256 of the bytes.
3. `file-type` sets `Content-Type` from the file format.
4. `file-missing` returns `404` for a missing file, a directory or an unknown format.
5. `file-policy` returns `400` for parent segments, excluded folders and symlinks that escape the vault.

## How to get to it (user POV)

1. `GET /v1/files/<vault-relative path>`.

## Driving it with verify.sh

Preconditions: baseline.

1. **Markdown.** Run `verify.sh req file-md /v1/files/A/Same.md`. Status `200`, body `a`, `ETag: "ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb"`, `Content-Type: text/markdown`.
2. **Image.** Run `verify.sh req file-png /v1/files/Projects/image.png`. Status `200`, body `png`, `Content-Type: image/png`.
3. **Base.** Run `verify.sh req file-base /v1/files/Notes.base`. Status `200`, `Content-Type: application/yaml`.
4. **Missing.** `verify.sh req file-missing /v1/files/Nope.md` gives `404` and `{"status":404,"message":"File not found: Nope.md"}`. `/v1/files/A` and `/v1/files/notes.txt` also give `404`.
5. **Excluded.** `verify.sh req file-excluded /v1/files/.obsidian/app.json` gives `400`, message `Path is excluded: .obsidian/app.json`.
6. **Parent segment.** `verify.sh req file-parent /v1/files/../etc/hosts` gives `400`, message `Parent segments are not allowed: ../etc/hosts`. `/v1/files/%2E%2E/etc/hosts` gives the same.
7. **Symlink escape.** `verify.sh req file-symlink /v1/files/Escape.md` gives `400`, message `Path escapes the vault: Escape.md`. The body does not contain `outside the vault`.

## Gotchas

1. Plain `curl` collapses `..` before sending. `verify.sh req` passes `--path-as-is`. Do not test the path policy with a client that normalizes URLs.
2. Policy failures are `400` and missing files are `404`. A `404` on a traversal attempt means the request never reached the policy.
