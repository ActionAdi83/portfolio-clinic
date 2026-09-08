# Demo dermatology services

One folder per service; `about.txt` holds the title (first line) and the
description (the rest). A `# durationMinutes: N` comment line documents the
suggested duration used by the seeder — Mongo shell ignores lines starting
with `#` when the script reads `about.txt`, since only the first line and the
non-comment body are used.

No real photos are sourced for this first iteration — `imageUrl` is left empty
so the frontend falls back to its placeholder icon. Point it at a real image
URL later by editing the service in the admin panel, or by adding an
`imageUrl` line to `about.txt` and extending the seeder to read it.

## Importing

From the repo root, with the local stack's Mongo container running and a
`.env` file present (see `.env.example`):

    scripts/add-populated-services.sh

Idempotent: rerunning it replaces each service (matched by title) rather than
duplicating it, so it is safe to run again after editing an `about.txt` file.
It never touches appointments or the weekly schedule.
