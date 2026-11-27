# core verification map

This directory is the maintained source for verifying the user-facing behavior of the core HTTP service. Read this index before driving the service, then use the matching feature file as the recipe.

## Baseline preconditions

1. `export VERIFY_RUN="$(.claude/skills/verify-core/verify.sh launch)"` succeeded.
2. `.claude/skills/verify-core/verify.sh doctor` prints `ok`.
3. The vault is the seeded fixture:

| Path | Content / purpose |
| --- | --- |
| `Home.md` | Frontmatter with a fake `# not a title`, H1 `Home Page`, links `[[Notes.base#Table]] [[Missing]] [[#Local]]` |
| `Projects/Projects.md` | Folder note. `[[Home\|Start]]`, embeds `![[Diagram.excalidraw]] ![[image.png\|300]]`, `[[Ignored]]` inside a fence, table-escaped `[[Home\\|Escaped]]` |
| `Projects/Diagram.excalidraw.md` | Diagram linking `[[Home]]` |
| `Projects/image.png` | Attachment, body `png` |
| `Notes.base` | Base, body `views: []` |
| `A/Same.md`, `B/Same.md` | Same name in two folders, bodies `a` and `b` |
| `A/Other.md` | `[[Same]]`, must resolve to its own folder |
| `.obsidian/app.json` | Excluded folder |
| `notes.txt` | Unknown format |
| `Empty/` | Empty folder |
| `Escape.md` | Symlink to a file outside the vault |

## Driving conventions

1. Run every request through `verify.sh req <label> <path>`. Use the feature ID as the label.
2. Assert with `jq` on `$VERIFY_RUN/evidence/<label>.body`, and with `grep -i` on `<label>.headers`.
3. Treat commands as literal.

## Proof and skip reporting

1. Proof is the `.request`, `.status`, `.headers` and `.body` files for each request, plus the assertion command and its output.
2. Record the feature ID with every artifact.
3. Report an unreachable path with the command and the unmet precondition. Do not report it as verified through another path.

## Feature entry contract

Each feature file starts with an H1 title and one paragraph describing the behavior. It then uses exactly four H2 sections in this order: `Sub-features`, `How to get to it (user POV)`, `Driving it with verify.sh`, `Gotchas`.

## Features

1. [Health](./health.md) covers the liveness probe.
2. [Workspace tree](./workspace-tree.md) covers node kinds, exclusions, titles, folder notes and ordering.
3. [Link resolution](./links.md) covers wikilinks, embeds, aliases, subpaths, fences and ambiguity.
4. [File read](./file-read.md) covers bytes, ETag, content types and the path policy.
5. [Manifest](./manifest.md) covers the startup index: file versions, folders, exclusions, tree agreement and the snapshot.
