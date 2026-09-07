"""Inspect freshly compiled production bytecode using the JDK, not source grep."""
from __future__ import annotations

import re
import struct
import tempfile
from pathlib import Path

from common import require, run

MODEL = "io.github.gustavo2358.air.model."
VALIDATION = "io.github.gustavo2358.air.validation."
# Ordinary values/collections/math and compiler-generated record/lambda support.
# java.io/java.nio/java.net/JSON/frontend/frameworks are outside this boundary.
JDK_PACKAGES = {"java.lang", "java.lang.invoke", "java.lang.runtime", "java.math",
                "java.util", "java.util.function", "java.util.stream"}
FORBIDDEN_CLASSES = {"java.lang.Process", "java.lang.ProcessBuilder", "java.lang.Runtime",
                     "java.lang.System", "java.util.ServiceLoader"}


def inspect_dependencies(output):
    edges = []
    for line in output.splitlines():
        match = re.fullmatch(r"\s+(\S+)\s+->\s+(\S+)\s+(.+?)\s*", line)
        if match:
            edges.append(match.groups())
    require(edges, "No class dependencies found in jdeps output")
    for source, target, location in edges:
        require(source.startswith((MODEL, VALIDATION)), f"Unexpected production class: {source}")
        require(location != "not found", f"Unresolved dependency: {source} -> {target}")
        if target.startswith((MODEL, VALIDATION)):
            require(not (source.startswith(MODEL) and target.startswith(VALIDATION)),
                    f"model -> validation forbidden: {source} -> {target}")
        else:
            package = target.rpartition(".")[0]
            require(location == "java.base" and package in JDK_PACKAGES
                    and target not in FORBIDDEN_CLASSES,
                    f"Forbidden dependency: {source} -> {target} ({location})")
    return len(edges)


def inspect_classes(classes):
    files = sorted(classes.rglob("*.class"))
    require(files, "No production classfiles")
    for file in files:
        header = file.read_bytes()[:8]
        require(len(header) == 8, f"Truncated classfile: {file.name}")
        magic, minor, major = struct.unpack(">IHH", header)
        require((magic, minor, major) == (0xCAFEBABE, 0, 65),
                f"Expected Java 21 without preview: {file.name}")
        name = file.relative_to(classes).as_posix()[:-6].replace("/", ".")
        require(name.startswith((MODEL, VALIDATION)), f"Unexpected production class: {name}")
    return len(files)


def check(root):
    sources = sorted((root / "src/main/java").rglob("*.java"))
    require(sources, "No production sources")
    with tempfile.TemporaryDirectory(prefix="air-harness-architecture-") as temp:
        classes = Path(temp) / "classes"
        classes.mkdir()
        run(["javac", "--release", "21", "-encoding", "UTF-8", "-Xlint:all", "-Werror",
             "-d", str(classes), *map(str, sources)], root)
        count = inspect_classes(classes)
        output = run(["jdeps", "--multi-release", "21", "-verbose:class", "-filter:none",
                      str(classes)], root)
        edges = inspect_dependencies(output)
    return f"{count} Java 21 classfiles; {edges} dependencies; model/validation boundary preserved"
