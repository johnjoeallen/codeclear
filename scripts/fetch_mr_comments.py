#!/usr/bin/env python3
"""
Fetch all MR comments from a GitLab repository.

Usage:
    python fetch_mr_comments.py --url https://gitlab.example.com --token <token> --project <namespace/repo>

Output:
    JSON file containing all MR comments, written to ./output/mr_comments_<project>.json
"""

import argparse
import json
import os
import sys
import time
from urllib.request import urlopen, Request
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, quote


def make_request(url, token, retries=3):
    """Make a GET request to the GitLab API with retry logic."""
    headers = {"PRIVATE-TOKEN": token, "Content-Type": "application/json"}
    req = Request(url, headers=headers)

    for attempt in range(1, retries + 1):
        try:
            with urlopen(req, timeout=30) as response:
                body = response.read().decode("utf-8")
                next_page = response.headers.get("X-Next-Page")
                total_pages = response.headers.get("X-Total-Pages")
                return json.loads(body), next_page, total_pages
        except HTTPError as e:
            if e.code == 429:
                wait = 2 ** attempt
                print(f"  Rate limited. Waiting {wait}s before retry {attempt}/{retries}...")
                time.sleep(wait)
            elif e.code == 404:
                raise ValueError(f"Not found: {url}. Check project path and token permissions.") from e
            elif e.code == 401:
                raise ValueError("Unauthorized. Check your token has 'read_api' scope.") from e
            else:
                raise
        except URLError as e:
            if attempt == retries:
                raise
            print(f"  Network error: {e}. Retrying {attempt}/{retries}...")
            time.sleep(2)

    raise RuntimeError(f"Failed after {retries} retries: {url}")


def paginate(base_url, token):
    """Yield all items across all pages for a GitLab API endpoint."""
    page = 1
    while True:
        sep = "&" if "?" in base_url else "?"
        url = f"{base_url}{sep}per_page=100&page={page}"
        items, next_page, total_pages = make_request(url, token)

        if not items:
            break

        yield from items

        if not next_page:
            break
        page = int(next_page)


def fetch_mrs(gitlab_url, token, project_id, state="merged"):
    """Fetch all MRs for a project. State: opened, merged, closed, or all."""
    encoded_id = quote(project_id, safe="")
    url = f"{gitlab_url}/api/v4/projects/{encoded_id}/merge_requests?state={state}"
    print(f"Fetching MRs (state={state})...")
    mrs = list(paginate(url, token))
    print(f"  Found {len(mrs)} MRs")
    return mrs


def fetch_notes_for_mr(gitlab_url, token, project_id, mr_iid):
    """Fetch all notes (comments) for a single MR."""
    encoded_id = quote(project_id, safe="")
    url = f"{gitlab_url}/api/v4/projects/{encoded_id}/merge_requests/{mr_iid}/notes"
    return list(paginate(url, token))


def fetch_discussions_for_mr(gitlab_url, token, project_id, mr_iid):
    """Fetch all discussion threads (inline + general) for a single MR."""
    encoded_id = quote(project_id, safe="")
    url = f"{gitlab_url}/api/v4/projects/{encoded_id}/merge_requests/{mr_iid}/discussions"
    return list(paginate(url, token))


def extract_comment_data(mr, discussions):
    """Extract structured comment data from a MR's discussions."""
    comments = []
    for discussion in discussions:
        for note in discussion.get("notes", []):
            # Skip system-generated notes (e.g. "added 2 commits")
            if note.get("system", False):
                continue

            comment = {
                "mr_iid": mr["iid"],
                "mr_title": mr["title"],
                "mr_author": mr.get("author", {}).get("username"),
                "mr_created_at": mr.get("created_at"),
                "mr_merged_at": mr.get("merged_at"),
                "note_id": note["id"],
                "discussion_id": discussion["id"],
                "author": note.get("author", {}).get("username"),
                "created_at": note.get("created_at"),
                "updated_at": note.get("updated_at"),
                "body": note.get("body", ""),
                "resolvable": note.get("resolvable", False),
                "resolved": note.get("resolved", False),
                "is_inline": note.get("position") is not None,
                "file_path": None,
                "line_range": None,
            }

            # Inline diff comments include position metadata
            position = note.get("position")
            if position:
                comment["file_path"] = position.get("new_path") or position.get("old_path")
                new_line = position.get("new_line")
                old_line = position.get("old_line")
                comment["line_range"] = {"new_line": new_line, "old_line": old_line}

            comments.append(comment)

    return comments


def main():
    parser = argparse.ArgumentParser(description="Fetch MR comments from a GitLab project")
    parser.add_argument("--url", required=True, help="GitLab base URL, e.g. https://gitlab.example.com")
    parser.add_argument("--token", required=True, help="GitLab personal access token (read_api scope)")
    parser.add_argument("--project", required=True, help="Project path, e.g. mygroup/myrepo")
    parser.add_argument(
        "--state",
        default="merged",
        choices=["opened", "merged", "closed", "all"],
        help="MR state to fetch (default: merged)",
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=None,
        help="Limit number of MRs to process (useful for testing)",
    )
    parser.add_argument(
        "--output",
        default=None,
        help="Output file path (default: ./output/mr_comments_<project-slug>.json)",
    )
    args = parser.parse_args()

    gitlab_url = args.url.rstrip("/")

    # Determine output path: mygroup/myrepo -> mygroup.myrepo.json
    project_slug = args.project.replace("/", ".")
    output_path = args.output or os.path.join("output", f"{project_slug}.json")
    os.makedirs(os.path.dirname(output_path) or ".", exist_ok=True)

    # Fetch MRs
    mrs = fetch_mrs(gitlab_url, args.token, args.project, state=args.state)
    if args.limit:
        mrs = mrs[: args.limit]
        print(f"Processing first {args.limit} MRs (--limit applied)")

    # Fetch comments for each MR
    all_comments = []
    for i, mr in enumerate(mrs, 1):
        print(f"[{i}/{len(mrs)}] MR !{mr['iid']}: {mr['title'][:60]}")
        try:
            discussions = fetch_discussions_for_mr(gitlab_url, args.token, args.project, mr["iid"])
            comments = extract_comment_data(mr, discussions)
            all_comments.extend(comments)
            print(f"  -> {len(comments)} human comments")
        except Exception as e:
            print(f"  WARNING: Failed to fetch discussions for MR !{mr['iid']}: {e}", file=sys.stderr)

        # Polite rate limiting: 10 requests/sec is well within GitLab's limits
        time.sleep(0.1)

    # Write output
    result = {
        "project": args.project,
        "gitlab_url": gitlab_url,
        "mr_state_filter": args.state,
        "total_mrs_processed": len(mrs),
        "total_comments": len(all_comments),
        "comments": all_comments,
    }

    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(result, f, indent=2, ensure_ascii=False)

    print(f"\nDone. {len(all_comments)} comments from {len(mrs)} MRs written to: {output_path}")


if __name__ == "__main__":
    main()
