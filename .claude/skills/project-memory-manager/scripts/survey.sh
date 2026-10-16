#!/usr/bin/env bash
# Prints facts about the repository that docs/current-status.md and docs/ai/ describe,
# so the documents can be compared with the code. Read-only: changes nothing.
set -u
ROOT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
APP="$ROOT/onboarding-progress-system"
SRC="$APP/src/main/java/com/example/onboarding"
TEST="$APP/src/test/java/com/example/onboarding"
cd "$ROOT" || exit 1

section() { printf '\n## %s\n' "$1"; }

section "Git"
echo "branch: $(git branch --show-current)"
git log --oneline -5
echo "uncommitted files: $(git status --short | wc -l)"

section "Merged PRs (last 10 merge commits)"
git log --merges --format='%s' -10 | sed -n 's/^Merge pull request \(#[0-9]*\).*/\1/p' | tr '\n' ' '; echo

section "Flyway migrations"
ls "$APP/src/main/resources/db/migration"

section "Java classes per package"
for d in "$SRC"/*/; do
  printf '%-12s %s\n' "$(basename "$d")" "$(ls "$d" | sed 's/\.java$//' | tr '\n' ' ')"
done

section "Endpoints (@GetMapping/@PostMapping/...)"
# Class-level @RequestMapping is a prefix for the methods below it (TaskController: /api/tasks)
for f in "$SRC"/controller/*.java; do
  echo "$(basename "$f" .java): $(grep -oE '@(Get|Post|Put|Delete|Request)Mapping(\("[^"]*"\))?' "$f" | sed 's/@\(.*\)Mapping/\1/; s/("\(.*\)")/ \1/' | tr '\n' ',' | sed 's/,$//; s/,/, /g')"
done

section "Templates"
(cd "$APP/src/main/resources/templates" && find . -name '*.html' | sed 's|^\./||' | sort | tr '\n' ' '); echo

section "Tests"
tests=$(grep -rhE '^\s*@(Test|ParameterizedTest)\b' "$TEST" | wc -l)
classes=$(grep -rlE '@(Test|ParameterizedTest)\b' "$TEST" | wc -l)
echo "test methods: $tests (in $classes classes; parameterized tests count once here)"

section "Numbers written in the docs (compare with the facts above)"
grep -noE '(自動テスト|テスト)[^|]{0,6}[0-9]+ 件' docs/current-status.md docs/design.md 2>/dev/null

section "Doc headers (edition / last update)"
grep -nE '^- (版|最終更新)|^最終更新|第[0-9]+版' docs/current-status.md docs/ai/backlog.md docs/requirements.md docs/design.md docs/design-diagrams.md 2>/dev/null | head -10

section "Technical debt signals"
echo "- Controllers that use a Repository directly:"
grep -l 'import com.example.onboarding.repository\.' "$SRC"/controller/*.java 2>/dev/null | xargs -r -n1 basename | sed 's/^/    /'
echo "- LocalDate.now()/Instant.now() without a Clock (outside entity/):"
grep -rnE '(LocalDate|LocalDateTime|Instant)\.now\(\)' "$SRC" | grep -v '/entity/' | sed "s|$SRC/|    |"
echo "- TODO / FIXME:"
grep -rnE 'TODO|FIXME' "$SRC" "$APP/src/main/resources" | sed "s|$APP/|    |"
echo "- Largest classes (lines):"
wc -l "$SRC"/*/*.java | sort -rn | sed -n '2,6p' | sed "s|$SRC/|    |"
echo "- th:utext uses (should only be @links.linkify):"
grep -rn 'th:utext' "$APP/src/main/resources/templates" | grep -v 'links.linkify' | sed "s|$APP/|    |"
