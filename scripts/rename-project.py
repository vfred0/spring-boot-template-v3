#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
import shutil
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

OLD_PACKAGE = "com.template"
OLD_NAME = "spring-boot-template"
_PACKAGE_RE = re.compile(r"^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$")
_NAME_RE = re.compile(r"^[a-z0-9]([a-z0-9-]*[a-z0-9])?$")
_SKIP_DIRS = {".git", "build", ".gradle", ".idea", "logs", "__pycache__"}


@dataclass(frozen=True)
class RenameConfig:
    new_package: str
    new_name: str
    dry_run: bool
    assume_yes: bool


def _build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        description="Rebrand this Spring Boot template to a new project identity.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=(
            "EXAMPLE:\n"
            "  Linux/Mac:  python3 scripts/rename-project.py"
            " --package com.acme.billing --name billing-service\n"
            "  Windows:    python  scripts\\rename-project.py"
            " --package com.acme.billing --name billing-service"
        ),
    )
    p.add_argument("--package", required=True, metavar="PKG",
                   help=f"New Java base package, replaces {OLD_PACKAGE} (e.g. com.acme.billing)."
                        f" Also becomes the Gradle group.")
    p.add_argument("--name", required=True, metavar="NAME",
                   help=f"New artifact/app name, replaces {OLD_NAME} (e.g. billing-service)."
                        f" Lower-case kebab-case.")
    p.add_argument("--dry-run", action="store_true",
                   help="Show what would change. Writes nothing.")
    p.add_argument("--yes", action="store_true",
                   help="Skip the confirmation prompt.")
    return p


def parse_args() -> RenameConfig:
    args = _build_parser().parse_args()
    return RenameConfig(
        new_package=args.package,
        new_name=args.name,
        dry_run=args.dry_run,
        assume_yes=args.yes,
    )


def validate(config: RenameConfig) -> None:
    if not _PACKAGE_RE.match(config.new_package):
        sys.exit(f"error: invalid package '{config.new_package}' (expected lower-case dotted, e.g. com.acme.billing)")
    if not _NAME_RE.match(config.new_name):
        sys.exit(f"error: invalid name '{config.new_name}' (expected lower-case kebab-case, e.g. billing-service)")
    if config.new_package == OLD_PACKAGE:
        sys.exit("error: --package equals the current package; nothing to do")


def _run_git(args: list[str], cwd: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(["git", *args], capture_output=True, text=True, cwd=cwd)


def find_repo_root() -> Path:
    result = _run_git(["rev-parse", "--show-toplevel"], Path.cwd())
    return Path(result.stdout.strip()) if result.returncode == 0 else Path.cwd()


def is_git_repo(root: Path) -> bool:
    return _run_git(["rev-parse", "--is-inside-work-tree"], root).returncode == 0


def ensure_template_state(root: Path) -> None:
    if not (root / "settings.gradle.kts").exists():
        sys.exit("error: run from the repository root (settings.gradle.kts not found)")
    old_pkg_path = Path(*OLD_PACKAGE.split("."))
    if not (root / "src" / "main" / "java" / old_pkg_path).exists():
        sys.exit(f"error: {OLD_PACKAGE.replace('.', '/')} not found — already renamed?")


def warn_if_dirty(root: Path) -> None:
    if not is_git_repo(root):
        return
    if _run_git(["status", "--porcelain"], root).stdout.strip():
        print(
            "warning: uncommitted changes — commit or branch first to keep the rename reversible.",
            file=sys.stderr,
        )


def _git_files(root: Path) -> list[Path]:
    result = subprocess.run(["git", "ls-files", "-z"], capture_output=True, cwd=root)
    paths = [root / p for p in result.stdout.decode("utf-8").split("\x00") if p]
    env = root / ".env"
    if env.exists() and env not in paths:
        paths.append(env)
    return paths


def _walk_files(root: Path) -> list[Path]:
    return [
        p for p in root.rglob("*")
        if p.is_file() and not _SKIP_DIRS.intersection(p.parts)
    ]


def collect_files(root: Path) -> list[Path]:
    return _git_files(root) if is_git_repo(root) else _walk_files(root)


def _replace_in_file(path: Path, config: RenameConfig, self_path: Path) -> bool:
    if path == self_path:
        return False
    try:
        raw = path.read_bytes()
        text = raw.decode("utf-8")
    except (UnicodeDecodeError, PermissionError, OSError):
        return False
    if OLD_PACKAGE not in text and OLD_NAME not in text:
        return False
    updated = text.replace(OLD_PACKAGE, config.new_package).replace(OLD_NAME, config.new_name)
    if updated == text:
        return False
    if not config.dry_run:
        path.write_bytes(updated.encode("utf-8"))
    return True


def replace_in_files(files: list[Path], config: RenameConfig, self_path: Path) -> int:
    changed = 0
    for path in files:
        if _replace_in_file(path, config, self_path):
            if config.dry_run:
                print(f"  edit  {path}")
            changed += 1
    print(f"files touched: {changed}")
    return changed


def _remove_empty_dirs(path: Path) -> None:
    for dirpath in sorted(path.rglob("*"), reverse=True):
        if dirpath.is_dir():
            try:
                dirpath.rmdir()
            except OSError:
                pass


def _move_package_dir(src_dir: Path, dest_dir: Path, root: Path) -> None:
    dest_dir.parent.mkdir(parents=True, exist_ok=True)
    if is_git_repo(root):
        subprocess.run(["git", "mv", str(src_dir), str(dest_dir)], check=True, cwd=root)
    else:
        shutil.move(str(src_dir), str(dest_dir))
    _remove_empty_dirs(src_dir.parent)


def move_packages(root: Path, config: RenameConfig) -> None:
    new_pkg_path = Path(*config.new_package.split("."))
    old_pkg_path = Path(*OLD_PACKAGE.split("."))
    for subtree in ("src/main/java", "src/test/java"):
        src_dir = root / subtree / old_pkg_path
        if not src_dir.exists():
            continue
        dest_dir = root / subtree / new_pkg_path
        if config.dry_run:
            print(f"  move  {src_dir} -> {dest_dir}")
        else:
            _move_package_dir(src_dir, dest_dir, root)


def confirm(config: RenameConfig) -> None:
    if config.assume_yes or config.dry_run:
        return
    answer = input(
        f"Rename {OLD_PACKAGE} -> {config.new_package}"
        f" and {OLD_NAME} -> {config.new_name}? [y/N] "
    )
    if answer.strip().lower() not in ("y", "yes"):
        sys.exit("aborted")


def _print_header(config: RenameConfig) -> None:
    print(f"package: {OLD_PACKAGE} -> {config.new_package}")
    print(f"name:    {OLD_NAME} -> {config.new_name}")
    if config.dry_run:
        print("(dry-run — no files will be modified)")


def _print_completion(dry_run: bool) -> None:
    if dry_run:
        print("dry-run complete.")
        return
    print("done. Review with 'git diff', run './gradlew build', then remove this one-shot script:")
    print("  Linux/Mac:  rm scripts/rename-project.py")
    print("  Windows:    del scripts\\rename-project.py")


def main() -> None:
    config = parse_args()
    validate(config)
    root = find_repo_root()
    ensure_template_state(root)
    warn_if_dirty(root)
    _print_header(config)
    confirm(config)
    self_path = root / "scripts" / "rename-project.py"
    files = collect_files(root)
    replace_in_files(files, config, self_path)
    move_packages(root, config)
    _print_completion(config.dry_run)


if __name__ == "__main__":
    main()
