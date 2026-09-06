#!/usr/bin/env bash
# Offline, dependency-free build/test gate. Requires JDK 21 or newer.
set -euo pipefail
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
for tool in javac java jar jdeps; do
  command -v "$tool" >/dev/null || { printf 'Missing JDK tool: %s\n' "$tool" >&2; exit 2; }
done
mkdir -p target
build_dir="$(mktemp -d target/check.XXXXXXXX)"
trap 'rm -rf -- "$build_dir"' EXIT
mkdir -p "$build_dir/classes" "$build_dir/test-classes"
find src/main/java -name '*.java' -print | LC_ALL=C sort > "$build_dir/main-sources.txt"
find src/test/java -name '*.java' -print | LC_ALL=C sort > "$build_dir/test-sources.txt"
javac --release 21 -encoding UTF-8 -Xlint:all -Werror -d "$build_dir/classes" @"$build_dir/main-sources.txt"
javac --release 21 -encoding UTF-8 -Xlint:all -Werror -cp "$build_dir/classes" -d "$build_dir/test-classes" @"$build_dir/test-sources.txt"
java -ea -cp "$build_dir/classes:$build_dir/test-classes" io.github.gustavo2358.air.validation.ContractSuite
jar --create --file "$build_dir/air-java.jar" -C "$build_dir/classes" .
deps="$(jdeps --multi-release 21 --print-module-deps "$build_dir/air-java.jar")"
[[ "$deps" == 'java.base' ]] || { printf 'Unexpected domain modules: %s\n' "$deps" >&2; exit 1; }
cp -- "$build_dir/air-java.jar" target/air-java-0.1.0-SNAPSHOT.jar
printf '%s\n' 'PASS: architecture bytecode dependency check (java.base only)' 'PASS: JAR generated in target/air-java-0.1.0-SNAPSHOT.jar'
