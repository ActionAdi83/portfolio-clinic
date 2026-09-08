#!/usr/bin/env bash
#
# Adds every demo service found under a directory — one subfolder per service.
#
#   scripts/add-populated-services.sh scripts/populate
#
# Each subfolder is one service:
#
#   populate/dermatoscopie/about.txt   first line: title, rest: description.
#                                      A "# durationMinutes: N" comment line
#                                      sets the suggested duration; defaults
#                                      to 30 if absent.
#
# Idempotent: services are matched by title, so a rerun replaces each one
# (title, description, duration; active is always set true) rather than
# stacking duplicates. Safe to run against a database that already has real
# services in it, as long as no real service happens to share a seeded title.
#
# No image files are seeded — imageUrl is left empty and the frontend shows
# a placeholder. Point a service at a real image later from the admin panel.
#
# Runs on the machine where the mongo container runs. Reads Mongo credentials
# from .env in the repo root (never hardcodes the database name).
set -euo pipefail

ENV_FILE="${ENV_FILE:-.env}"
MONGO_CONTAINER="${MONGO_CONTAINER:-clinic-mongo}"

ROOT="${1:-scripts/populate}"
[ -d "$ROOT" ] || { echo "$ROOT is not a directory" >&2; exit 1; }

if [ -f "$ENV_FILE" ]; then
    MONGO_DB=$(sed -n 's/^MONGO_DB=//p' "$ENV_FILE" | head -1)
    MONGO_USER=$(sed -n 's/^MONGO_USER=//p' "$ENV_FILE" | head -1)
    MONGO_PASSWORD=$(sed -n 's/^MONGO_PASSWORD=//p' "$ENV_FILE" | head -1)
fi
MONGO_DB="${MONGO_DB:-clinic}"

if [ -n "${MONGO_USER:-}" ]; then
    AUTH="-u $MONGO_USER -p $MONGO_PASSWORD --authenticationDatabase admin"
else
    AUTH=""
fi

# Escapes a value for use inside a double-quoted JS string literal. Plain
# backslash-escaping rather than bash's ${VAR@Q}, which renders an apostrophe
# as 'it'\''s' — not valid JavaScript, and service descriptions are exactly
# the kind of text that contains one.
js() { printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'; }

ADDED=0
for dir in "$ROOT"/*/; do
    [ -d "$dir" ] || continue
    SLUG=$(basename "$dir")
    ABOUT="$dir/about.txt"
    if [ ! -f "$ABOUT" ]; then
        echo "-- $SLUG: no about.txt, skipped" >&2
        continue
    fi

    TITLE=$(head -1 "$ABOUT")
    # Body: every line after the first, minus comment lines and blanks, joined with spaces.
    DESCRIPTION=$(tail -n +2 "$ABOUT" | sed '/^#/d;/^$/d' | tr '\n' ' ' | sed 's/ *$//')
    DURATION=$(sed -n 's/^# *durationMinutes: *//p' "$ABOUT" | head -1)
    DURATION="${DURATION:-30}"

    docker exec -i "$MONGO_CONTAINER" mongosh $AUTH --quiet "$MONGO_DB" <<EOF
const title = "$(js "$TITLE")";
const description = "$(js "$DESCRIPTION")";
const durationMinutes = $DURATION;

db.services.deleteMany({ title: title });
db.services.insertOne({
    title: title,
    description: description,
    imageUrl: "",
    durationMinutes: durationMinutes,
    active: true,
    _class: "edu.clinic.entities.MedicalService"
});
print("-- " + title + ": " + durationMinutes + " min");
EOF

    ADDED=$((ADDED + 1))
done

echo ""
echo "$ADDED service(s) written."
