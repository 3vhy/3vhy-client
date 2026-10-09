"""Turn Gradle/javac errors in a build log into GitHub annotations.

GitHub shows at most 10 annotations of each kind per step, so the first
10 errors become ::error, the next 10 ::warning and the next 10 ::notice.
Usage: gradle-annotate.py build.log [offset]
"""
import re
import sys

log_path = sys.argv[1]
offset = int(sys.argv[2]) if len(sys.argv) > 2 else 0
lines = open(log_path, encoding="utf-8", errors="replace").read().splitlines()

errors = []
pattern = re.compile(r"^(?:e: )?(?P<file>[^\s:][^:]*\.java):(?P<line>\d+): error: (?P<msg>.*)$")
for i, line in enumerate(lines):
    m = pattern.match(line.strip())
    if m:
        context = " | ".join(l.strip() for l in lines[i + 1 : i + 3] if l.strip())
        f = m.group("file")
        short = f.split("/apps/mod/")[-1]
        errors.append(f"{short}:{m.group('line')}: {m.group('msg')} || {context}")

# Gradle's own failure summary (dependency or plugin problems)
for i, line in enumerate(lines):
    if line.strip().startswith("* What went wrong:"):
        block = []
        for l in lines[i + 1 : i + 14]:
            if l.strip().startswith("* Try:"):
                break
            if l.strip():
                block.append(l.strip())
        errors.append("GRADLE: " + " / ".join(block))

seen = set()
unique = [e for e in errors if not (e in seen or seen.add(e))]
print(f"{len(unique)} problems found")
kinds = ["error", "warning", "notice"]
for n, msg in enumerate(unique[offset : offset + 30]):
    kind = kinds[n // 10]
    msg = msg.replace("%", "%25").replace("\r", "").replace("\n", "%0A")
    print(f"::{kind} title=#{offset + n + 1}::{msg[:900]}")
