#!/usr/bin/env bash
# Mechanical checks for the architecture review. Read-only.
# Every hit is a *candidate*: read the code before calling it a finding.
set -u
ROOT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
APP="$ROOT/onboarding-progress-system"
SRC="$APP/src/main/java/com/example/onboarding"
MIG="$APP/src/main/resources/db/migration"
BASE="${1:-origin/master}"   # branch to compare migrations with
cd "$ROOT" || exit 1

check() { printf '\n## %s\n' "$1"; }
rel() { sed "s|$SRC/||; s|$APP/||"; }

check "L1 Controller -> Repository (recommended: Controller -> Service -> Repository)"
for f in $(grep -l 'import com.example.onboarding.repository\.' "$SRC"/controller/*.java); do
  n=$(grep -cE '[a-z]+Repository\.[a-zA-Z]+\(' "$f")
  echo "$(basename "$f"): $n repository calls"
  grep -nE '[a-z]+Repository\.[a-zA-Z]+\(' "$f" | sed 's/^/    /'
done

check "L2 Repository used outside service/ (security/, web/ are allowed for lookups; still list them)"
grep -lE 'import com.example.onboarding.repository\.' "$SRC"/{security,web,config}/*.java 2>/dev/null | rel

check "L3 @Transactional on controllers (should be on services)"
grep -n '@Transactional' "$SRC"/controller/*.java | rel

check "L4 Entities bound directly from requests (@RequestBody / @ModelAttribute with an entity type)"
ENTITIES=$(ls "$SRC"/entity/*.java | xargs -n1 basename | sed 's/\.java$//' | paste -sd'|')
grep -nE "@(RequestBody|ModelAttribute)[^,)]*\b($ENTITIES)\b" "$SRC"/controller/*.java | rel

check "L5 Field injection in main code (use constructor injection)"
grep -rn '@Autowired' "$SRC" | rel

check "L6 now() without the Clock bean (allowed only in entity @PrePersist/@PreUpdate)"
grep -rnE '(LocalDate|LocalDateTime|Instant|ZonedDateTime)\.now\(\)' "$SRC" | grep -v '/entity/' | rel

check "E1 Enum mapping (must be EnumType.STRING)"
grep -rn 'EnumType.ORDINAL' "$SRC" | rel
ENUMS=$(grep -l '^public enum' "$SRC"/entity/*.java | xargs -n1 basename | sed 's/\.java$//' | paste -sd'|')
for f in "$SRC"/entity/*.java; do
  # an enum field needs @Enumerated(EnumType.STRING) within the 3 lines above it
  awk -v enums="^($ENUMS)$" -v file="$(basename "$f")" '
    { line[NR] = $0 }
    $1 == "private" && $2 ~ enums {
      ok = 0; for (i = NR - 3; i < NR; i++) if (line[i] ~ /EnumType\.STRING/) ok = 1
      if (!ok) print file ":" NR ": enum field without @Enumerated(EnumType.STRING)"
    }' "$f"
done

check "E2 Eager fetching (prefer LAZY + @EntityGraph on the query)"
grep -rn 'FetchType.EAGER' "$SRC" | rel
grep -rnE '@(ManyToOne|OneToOne)\b' "$SRC"/entity/*.java | grep -v 'LAZY' | rel

check "S1 Service classes without @Transactional"
for f in "$SRC"/service/*Service.java; do grep -q '@Transactional' "$f" || basename "$f"; done

check "F1 Flyway: duplicated versions"
ls "$MIG" | sed -n 's/^V\([0-9]*\)__.*/\1/p' | sort | uniq -d

check "F2 Flyway: already-merged migrations changed on this branch (never edit them; add a new V*.sql)"
if git rev-parse -q --verify "$BASE" >/dev/null; then
  git diff --name-status "$BASE"...HEAD -- "$MIG" | grep -v '^A' || echo "(none)"
  echo "new on this branch:"; git diff --name-status "$BASE"...HEAD -- "$MIG" | grep '^A' || echo "(none)"
else
  echo "($BASE not found: run git fetch origin master)"
fi

check "F3 Flyway: next version number"
last=$(ls "$MIG" | sed -n 's/^V\([0-9]*\)__.*/\1/p' | sort -n | tail -1)
echo "latest: V$last -> next: V$((last + 1))__<what_it_does>.sql"

check "T1 th:utext other than @links.linkify (XSS risk)"
grep -rn 'th:utext' "$APP/src/main/resources/templates" | grep -v 'links.linkify' | rel

check "Size (lines) - classes over 250 lines are split candidates"
wc -l "$SRC"/*/*.java | sort -rn | awk 'NR>1 && $1>250' | rel
