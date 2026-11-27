# Workspace tree

The workspace endpoint returns the whole vault as a tree of folders and typed file nodes, so a client can render navigation without listing the disk itself.

## Sub-features

1. `tree-kinds` maps `.md` to `page`, `.excalidraw.md` to `diagram`, `.base` to `base`, images/pdf/video to `attachment`, directories to `folder`.
2. `tree-exclusions` omits excluded folders (`.git`, `.obsidian`, `.claude`, `.trash`), unknown formats and symlinks.
3. `tree-title` uses the first H1 outside frontmatter and fences, else the file name.
4. `tree-folder-note` sets `folderNote` when a folder holds a page with the folder's name.
5. `tree-order` lists subfolders first, then files, each sorted case-insensitively by name.

## How to get to it (user POV)

1. `GET /v1/workspace`.

## Driving it with verify.sh

Preconditions: baseline.

1. **Fetch.** Run `verify.sh req workspace /v1/workspace`. Status `200`. `jq -r .name` equals the contents of `$VERIFY_RUN/vault`.
2. **Kinds and exclusions.** Run `jq -r '[.. | objects | select(.kind|IN("page","folder","diagram","base","attachment")) | .path] | join(",")' $VERIFY_RUN/evidence/workspace.body`. Output is exactly `A,A/Other.md,A/Same.md,B,B/Same.md,Empty,Projects,Projects/Diagram.excalidraw.md,Projects/image.png,Projects/Projects.md,Home.md,Notes.base`. No `.obsidian`, `notes.txt` or `Escape.md`.
3. **Title.** `jq -r '.. | objects | select(.path=="Home.md" and .kind=="page") | .title'` prints `Home Page`, not `not a title`. `A/Same.md` has title `Same`.
4. **Folder note.** `jq -r '.. | objects | select(.kind=="folder" and .path=="Projects") | .folderNote.path'` prints `Projects/Projects.md`. The root folder's `folderNote` is `null`.
5. **Versions.** Every file node has `version.hash` equal to `shasum -a 256` of the seeded file.

## Gotchas

1. `jq '..'` also visits link objects, whose `kind` is `LINK` or `EMBED`. Filter on the node kinds listed above.
2. The tree is built once at startup, so restart (`stop`, then `launch`) to see disk changes until the watcher lands.
3. Symlinked files are skipped by the walk but still reachable through `/v1/files`, where they are rejected. See [file-read.md](./file-read.md).
