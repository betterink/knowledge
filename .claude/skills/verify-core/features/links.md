# Link resolution

Pages and diagrams expose their Obsidian wikilinks and embeds with resolved vault targets, so a client can follow links without parsing Markdown.

## Sub-features

1. `link-basic` resolves `[[Name]]` to a vault path and sets `kind` to `LINK`.
2. `link-embed` sets `kind` to `EMBED` for `![[...]]`.
3. `link-alias` splits `[[target|alias]]`, including table-escaped `[[target\|alias]]`.
4. `link-subpath` splits `#heading`. `[[#Local]]` targets its own page.
5. `link-missing` keeps unresolvable links with `target: null`.
6. `link-fence` ignores links inside fenced code.
7. `link-ambiguous` prefers an exact path, then the source's folder, then the shortest path.

## How to get to it (user POV)

1. `GET /v1/workspace`, then the `links` array of a `page` or `diagram` node.

## Driving it with verify.sh

Preconditions: baseline.

1. **Fetch.** Run `verify.sh req links /v1/workspace`. Status `200`.
2. **Home links.** `jq -c '.. | objects | select(.kind=="page" and .path=="Home.md") | [.links[] | {raw,target,subpath}]' $VERIFY_RUN/evidence/links.body` prints `[{"raw":"Notes.base#Table","target":"Notes.base","subpath":"Table"},{"raw":"Missing","target":null,"subpath":null},{"raw":"#Local","target":"Home.md","subpath":"Local"}]`.
3. **Projects links.** For `Projects/Projects.md`, `[.links[] | {target,kind,alias}]` prints `Home.md LINK Start`, `Projects/Diagram.excalidraw.md EMBED null`, `Projects/image.png EMBED 300`, `Home.md LINK Escaped`, in that order. No link has raw `Ignored`.
4. **Ambiguous name.** For `A/Other.md`, the one link `Same` has target `A/Same.md`, not `B/Same.md`.
5. **Diagram.** For `Projects/Diagram.excalidraw.md`, the one link `Home` has target `Home.md`.

## Gotchas

1. `start` and `end` are character offsets into the file, end-exclusive. Check them against the seeded text if a parser change touches them.
2. `raw` keeps the escape backslash (`Home\\|Escaped` in JSON). `alias` does not.
3. Embed size hints such as `|300` come back as `alias`.
