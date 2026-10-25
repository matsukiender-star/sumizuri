#!/usr/bin/env bash
# Checks the files in the repo for three things: stray files that should not be committed, anything of the signing key, and Kotlin files longer than the hard limit.
set -u

MAX_LINES=400
failed=0

fail() {
    # GitHub shows an error line as a note on the file in the pull request.
    echo "::error file=$1::$2"
    failed=1
}

files=$(git ls-files)

# Stray files: build output, IDE files, test folders, and temporary tools that were only for checking something.
while IFS= read -r f; do
    [ -z "$f" ] && continue
    case "$f" in
        build/*|*/build/*) fail "$f" "Build output is committed. Remove it from git." ;;
        .idea/*|*.iml|.DS_Store|*/.DS_Store|local.properties|*.log|*.tmp|*~) fail "$f" "A local or temporary file is committed. Remove it from git." ;;
        */src/test/*|*/src/*Test/*) fail "$f" "The project has no tests. Remove the test folder." ;;
    esac
done <<< "$files"
while IFS= read -r f; do
    [ -z "$f" ] && continue
    fail "$f" "A temporary tool or block is committed, marked Temporary or TEMP. Remove it."
done < <(git ls-files '*.kt' '*.kts' | xargs -r grep -lE '^\s*// (Temporary:|TEMP )' 2>/dev/null)

# Signing: the key, its passwords and the file that holds them only ever live on the computer of the developer and in the GitHub secrets.
while IFS= read -r f; do
    [ -z "$f" ] && continue
    case "$f" in
        keystore.properties|*/keystore.properties|*.jks|*.keystore|*.p12|*.pfx) fail "$f" "A signing key or its settings are committed. Remove it from git and change the key passwords." ;;
    esac
done <<< "$files"
while IFS= read -r f; do
    [ -z "$f" ] && continue
    fail "$f" "A signing password is written in this file. Remove it and change the key passwords."
done < <(git ls-files '*.properties' | xargs -r grep -lE '^\s*(storePassword|keyPassword)\s*=\s*\S' 2>/dev/null)

# File length: a Kotlin file longer than this is split by what its parts do.
while IFS= read -r f; do
    # A file deleted but not yet committed is still listed, and has nothing to count.
    [ -f "$f" ] || continue
    lines=$(wc -l < "$f")
    if [ "$lines" -gt "$MAX_LINES" ]; then
        fail "$f" "This file has $lines lines, over the limit of $MAX_LINES. Split it by what its parts do."
    fi
done < <(git ls-files '*.kt' '*.kts')

if [ "$failed" -ne 0 ]; then
    echo "The repo checks found problems, see the errors above."
    exit 1
fi
echo "The repo checks passed."
