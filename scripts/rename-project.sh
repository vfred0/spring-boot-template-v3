#!/usr/bin/env bash
set -euo pipefail

OLD_PACKAGE="com.template"
OLD_NAME="spring-boot-template"

NEW_PACKAGE=""
NEW_NAME=""
DRY_RUN="false"
ASSUME_YES="false"

usage() {
    cat <<'EOF'
Rebrand this template to a new project identity.

USAGE:
    scripts/rename-project.sh --package <java.package> --name <artifact-name> [--dry-run] [--yes]

OPTIONS:
    --package   New Java base package, replaces com.template (e.g. com.acme.billing).
                Also becomes the Gradle group.
    --name      New artifact/app/infra name, replaces spring-boot-template (e.g. billing-service).
                Lower-case kebab-case: drives rootProject.name, spring.application.name default,
                KEYCLOAK_REALM default, Docker service/db/network/volume names and the realm.
    --dry-run   Show what would change. Writes nothing.
    --yes       Skip the confirmation prompt.
    -h, --help  Show this help.

EXAMPLE:
    scripts/rename-project.sh --package com.acme.billing --name billing-service
EOF
}

die() {
    echo "error: $*" >&2
    exit 1
}

parse_args() {
    while [ "$#" -gt 0 ]; do
        case "$1" in
            --package) NEW_PACKAGE="${2:-}"; shift 2 ;;
            --name) NEW_NAME="${2:-}"; shift 2 ;;
            --dry-run) DRY_RUN="true"; shift ;;
            --yes) ASSUME_YES="true"; shift ;;
            -h|--help) usage; exit 0 ;;
            *) die "unknown argument: $1" ;;
        esac
    done
}

validate_inputs() {
    [ -n "$NEW_PACKAGE" ] || die "--package is required (see --help)"
    [ -n "$NEW_NAME" ] || die "--name is required (see --help)"
    echo "$NEW_PACKAGE" | grep -Eq '^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$' \
        || die "invalid package '$NEW_PACKAGE' (expected lower-case dotted, e.g. com.acme.billing)"
    echo "$NEW_NAME" | grep -Eq '^[a-z0-9]([a-z0-9-]*[a-z0-9])?$' \
        || die "invalid name '$NEW_NAME' (expected lower-case kebab-case, e.g. billing-service)"
    if [ "$NEW_PACKAGE" = "$OLD_PACKAGE" ]; then
        die "--package equals the current package; nothing to do"
    fi
}

resolve_repo_root() {
    git rev-parse --show-toplevel 2>/dev/null || pwd
}

ensure_template_state() {
    [ -f settings.gradle.kts ] || die "must run from the repository root (settings.gradle.kts not found)"
    [ -d "src/main/java/${OLD_PACKAGE//.//}" ] \
        || die "package dir src/main/java/${OLD_PACKAGE//.//} not found — already renamed?"
}

warn_if_dirty() {
    git rev-parse --is-inside-work-tree >/dev/null 2>&1 || return 0
    [ -n "$(git status --porcelain)" ] || return 0
    echo "warning: working tree has uncommitted changes — commit or branch first to keep the rename reversible." >&2
}

collect_files() {
    if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
        git ls-files -z
        { [ -f .env ] && printf '.env\0'; } || true
    else
        find . -type f \
            -not -path './.git/*' -not -path './build/*' -not -path './.gradle/*' \
            -not -path './.idea/*' -not -path './logs/*' -print0
    fi
}

replace_contents() {
    local self="$1" changed=0
    while IFS= read -r -d '' file; do
        [ "$file" = "$self" ] && continue
        grep -Ilq -e 'com\.template' -e 'spring-boot-template' "$file" 2>/dev/null || continue
        if [ "$DRY_RUN" = "true" ]; then
            local hits
            hits="$(grep -Eo 'com\.template|spring-boot-template' "$file" | wc -l | tr -d ' ')"
            echo "  edit  $file ($hits occurrence(s))"
        else
            NEW_PACKAGE="$NEW_PACKAGE" NEW_NAME="$NEW_NAME" perl -pi \
                -e 's/\Qcom.template\E\b/$ENV{NEW_PACKAGE}/g;' \
                -e 's/\Qspring-boot-template\E/$ENV{NEW_NAME}/g;' "$file"
        fi
        changed=$((changed + 1))
    done < <(collect_files)
    echo "files touched: $changed"
}

move_packages() {
    local new_path="${NEW_PACKAGE//.//}"
    local root src_dir dest_dir
    for root in src/main/java src/test/java; do
        src_dir="$root/${OLD_PACKAGE//.//}"
        [ -d "$src_dir" ] || continue
        dest_dir="$root/$new_path"
        if [ "$DRY_RUN" = "true" ]; then
            echo "  move  $src_dir -> $dest_dir"
            continue
        fi
        mkdir -p "$(dirname "$dest_dir")"
        if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
            git mv "$src_dir" "$dest_dir"
        else
            mv "$src_dir" "$dest_dir"
        fi
        find "$root" -type d -empty -delete
    done
}

confirm() {
    [ "$ASSUME_YES" = "true" ] && return 0
    [ "$DRY_RUN" = "true" ] && return 0
    printf 'Rename %s -> %s and %s -> %s ? [y/N] ' \
        "$OLD_PACKAGE" "$NEW_PACKAGE" "$OLD_NAME" "$NEW_NAME"
    read -r answer
    case "$answer" in y|Y|yes|YES) return 0 ;; *) die "aborted" ;; esac
}

main() {
    parse_args "$@"
    validate_inputs
    cd "$(resolve_repo_root)"
    ensure_template_state
    warn_if_dirty

    local self_rel="scripts/$(basename "$0")"
    echo "package: $OLD_PACKAGE -> $NEW_PACKAGE"
    echo "name:    $OLD_NAME -> $NEW_NAME"
    [ "$DRY_RUN" = "true" ] && echo "(dry-run — no files will be modified)"
    confirm

    replace_contents "$self_rel"
    move_packages

    if [ "$DRY_RUN" = "true" ]; then
        echo "dry-run complete."
    else
        echo "done. Review with 'git diff', run './gradlew build', then remove this one-shot script:"
        echo "  rm $self_rel"
    fi
}

main "$@"
