#!/usr/bin/env python3
"""
dataset/preprocess.py
=====================
Preprocessing pipeline for the AOL Search Query Log.

Input  : dataset/raw/user-ct-test-collection-02.txt
         Tab-separated (TSV). Columns: AnonID, Query, QueryTime, ItemRank, ClickURL
         3,614,507 rows (including header). Do NOT use a CSV parser on this file.

Output : dataset/processed/queries.csv
         Comma-separated. Columns: query,count,lastSeen
         Top 150,000 queries by count (cap applied AFTER full aggregation across
         all 3.6M rows — counts are accurate, we just limit the output size).
         Still well above the assignment's 100,000-query minimum.
         Using the full 1.24M unique set would bloat the Trie to ~1.5 GB and make
         demo restarts slow; 150K captures all practically-relevant queries.

Pipeline (DESIGN.md §9.1):
  1. Read TSV, split on \\t
  2. Drop header row
  3. Drop rows with NULL / empty Query
  4. Trim whitespace
  5. Lowercase
  6. Collapse duplicate internal spaces (e.g. "iphone  15" → "iphone 15")
  7. Group by normalised query:
       - count   = frequency across all rows
       - lastSeen = latest QueryTime seen for that query
  8. Sort by count DESC
  9. Take top 150,000 (cap applied post-aggregation; input is fully read)
  10. Write queries.csv

Usage:
    python3 dataset/preprocess.py            # run from repo root
    python3 preprocess.py                    # run from dataset/ directory
"""

import csv
import os
import re
import sys
from collections import defaultdict
from datetime import datetime

# ── Path resolution ──────────────────────────────────────────────────────────
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT  = os.path.dirname(SCRIPT_DIR)

RAW_PATH       = os.path.join(REPO_ROOT, "dataset", "raw",       "user-ct-test-collection-02.txt")
PROCESSED_PATH = os.path.join(REPO_ROOT, "dataset", "processed", "queries.csv")

# Fall back if run from repo root directly
if not os.path.exists(RAW_PATH):
    RAW_PATH = os.path.join(os.getcwd(), "dataset", "raw", "user-ct-test-collection-02.txt")

# ── Constants ────────────────────────────────────────────────────────────────
INTERNAL_SPACE_RE   = re.compile(r' {2,}')     # two or more spaces → one space
ALPHANUM_RE        = re.compile(r'[a-z0-9]')  # at least one letter or digit required
LOG_INTERVAL        = 500_000                  # print progress every N rows
TOP_N               = 150_000                  # cap output to top N queries by count
                                                # (applied post-aggregation so counts
                                                # remain accurate across the full input)


def normalise(query: str) -> str:
    """Apply the normalisation pipeline to a single query string.
    Returns empty string if the result contains no alphanumeric characters
    (e.g. bare punctuation like '-' from null filler rows in the raw log).
    """
    q = query.strip()
    q = q.lower()
    q = INTERNAL_SPACE_RE.sub(' ', q)
    # Discard queries that are purely punctuation / symbols with no real content
    if not ALPHANUM_RE.search(q):
        return ''
    return q


def parse_time(ts: str) -> str:
    """
    Parse a QueryTime string and return an ISO date string (YYYY-MM-DD).
    AOL format example: "2006-03-01 16:01:20"
    Returns empty string on parse failure (treated as unknown).
    """
    if not ts:
        return ''
    try:
        dt = datetime.strptime(ts.strip(), '%Y-%m-%d %H:%M:%S')
        return dt.strftime('%Y-%m-%d')
    except ValueError:
        return ''


def main():
    if not os.path.exists(RAW_PATH):
        print(f"ERROR: Raw dataset not found at:\n  {RAW_PATH}", file=sys.stderr)
        print("Make sure user-ct-test-collection-02.txt is in dataset/raw/", file=sys.stderr)
        sys.exit(1)

    os.makedirs(os.path.dirname(PROCESSED_PATH), exist_ok=True)

    print(f"Reading: {RAW_PATH}")
    counts:    dict[str, int] = defaultdict(int)
    last_seen: dict[str, str] = {}

    raw_rows   = 0
    kept_rows  = 0
    skipped    = 0

    with open(RAW_PATH, encoding='utf-8', errors='replace') as f:
        # The file is TSV — do NOT use csv.reader with default comma delimiter.
        header = f.readline()  # consume and discard the header row

        for line in f:
            raw_rows += 1
            if raw_rows % LOG_INTERVAL == 0:
                print(f"  ... processed {raw_rows:,} rows, kept {kept_rows:,} unique so far")

            parts = line.rstrip('\n').split('\t')
            if len(parts) < 2:
                skipped += 1
                continue

            # Columns: AnonID(0), Query(1), QueryTime(2), ItemRank(3), ClickURL(4)
            raw_query = parts[1]
            query_time = parts[2] if len(parts) > 2 else ''

            if not raw_query or not raw_query.strip():
                skipped += 1
                continue

            q = normalise(raw_query)
            if not q:
                skipped += 1
                continue

            counts[q] += 1
            kept_rows += 1

            # Track latest QueryTime per query
            date_str = parse_time(query_time)
            if date_str:
                if q not in last_seen or date_str > last_seen[q]:
                    last_seen[q] = date_str

    print(f"\nRaw rows read    : {raw_rows:,}")
    print(f"Rows kept        : {kept_rows:,}")
    print(f"Rows skipped     : {skipped:,}")
    print(f"Unique queries   : {len(counts):,}")

    # Sort by count descending, then cap to TOP_N.
    # Full aggregation is always done first so counts are accurate.
    sorted_queries = sorted(counts.items(), key=lambda kv: kv[1], reverse=True)
    if len(sorted_queries) > TOP_N:
        print(f"Capping to top {TOP_N:,} queries (from {len(sorted_queries):,} unique)")
        sorted_queries = sorted_queries[:TOP_N]

    print(f"\nWriting: {PROCESSED_PATH}")
    # Use newline='\n' (LF only) so Spring's CSV reader gets clean lines on all platforms
    with open(PROCESSED_PATH, 'w', newline='\n', encoding='utf-8') as f:
        writer = csv.writer(f, lineterminator='\n')
        writer.writerow(['query', 'count', 'lastSeen'])
        for query, count in sorted_queries:
            writer.writerow([query, count, last_seen.get(query, '')])

    print(f"Done. Wrote {len(sorted_queries):,} rows to queries.csv")


if __name__ == '__main__':
    main()
