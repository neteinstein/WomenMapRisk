#!/usr/bin/env python3
"""Writes a Markdown coverage table from Kover's aggregated XML report (build/reports/kover/report.xml)."""
import sys
import xml.etree.ElementTree as ET

path = sys.argv[1] if len(sys.argv) > 1 else "build/reports/kover/report.xml"
root = ET.parse(path).getroot()
rows = []
for counter in root.findall("counter"):
    missed, covered = int(counter.get("missed")), int(counter.get("covered"))
    total = missed + covered
    rows.append((counter.get("type"), covered, total, 100.0 * covered / total if total else 0.0))
print("### Code coverage (Kover, shared modules)\n")
print("| Metric | Covered | Total | % |\n|---|---:|---:|---:|")
for kind, covered, total, pct in rows:
    print(f"| {kind.title()} | {covered} | {total} | {pct:.1f}% |")
