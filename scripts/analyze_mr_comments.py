#!/usr/bin/env python3
"""
Analyze MR comments from fetch_mr_comments.py output to generate a code review rule set.

Usage:
    python analyze_mr_comments.py --input output/mygroup.myrepo.json
    python analyze_mr_comments.py --input output/mygroup.myrepo.json --output rules/myrepo_rules.md

What it does:
    - Clusters comments by keyword/phrase similarity
    - Scores each cluster by frequency and resolution rate
    - Emits a ranked Markdown rule set with supporting evidence
"""

import argparse
import json
import os
import re
import sys
from collections import Counter, defaultdict


# ---------------------------------------------------------------------------
# Text normalisation
# ---------------------------------------------------------------------------

STOPWORDS = {
    "a", "an", "the", "and", "or", "but", "in", "on", "at", "to", "for",
    "of", "with", "this", "that", "is", "it", "be", "are", "was", "were",
    "have", "has", "had", "do", "does", "did", "will", "would", "could",
    "should", "can", "may", "might", "not", "no", "so", "if", "as", "by",
    "we", "you", "i", "they", "he", "she", "here", "there", "what", "when",
    "where", "which", "who", "how", "also", "just", "then", "than", "from",
    "into", "via", "per", "etc", "e.g", "i.e",
}

# Phrases that frequently appear in review comments — used to anchor clusters
ANCHOR_PATTERNS = [
    # Naming / style
    (r"\b(nit|nitpick)\b", "nit / style"),
    (r"\b(naming|rename|variable name|method name|class name)\b", "naming"),
    (r"\b(camel.?case|snake.?case|pascal.?case)\b", "naming convention"),
    # Error handling
    (r"\b(error handling|exception|catch|throws?|try.?catch)\b", "error handling"),
    (r"\b(null.?check|null pointer|npe|none.?check)\b", "null / none handling"),
    # Testing
    (r"\b(test|spec|unit test|integration test|coverage)\b", "testing"),
    (r"\b(mock|stub|fake|spy)\b", "test doubles"),
    # Security
    (r"\b(sql injection|xss|csrf|injection|sanitize|escape)\b", "security"),
    (r"\b(auth|authoriz|authenticat|permission|role|access control)\b", "auth / access"),
    (r"\b(secret|credential|password|token|api.?key)\b", "credential handling"),
    # API / interface design
    (r"\b(breaking.?change|backwards?.?compat|deprecat)\b", "backwards compatibility"),
    (r"\b(rest|endpoint|route|status.?code|http)\b", "API design"),
    (r"\b(serializ|deserializ|marshal|unmarshal|json|xml)\b", "serialisation"),
    # Performance
    (r"\b(performance|slow|n\+1|query|index|cache|batch)\b", "performance"),
    (r"\b(memory.?leak|gc|garbage.?collect)\b", "memory management"),
    # Code quality
    (r"\b(duplicate|duplication|dry|copy.?paste|repeated)\b", "duplication"),
    (r"\b(complex|simplif|refactor|clean.?up|readab)\b", "complexity / readability"),
    (r"\b(dead.?code|unused|remove|delete)\b", "dead code"),
    (r"\b(comment|documentation|doc.?string|javadoc|docblock)\b", "documentation"),
    (r"\b(magic.?number|magic.?string|constant|enum)\b", "magic values"),
    # Concurrency
    (r"\b(thread.?safe|race.?condition|concurren|synchronized|lock|mutex)\b", "concurrency"),
    (r"\b(async|await|promise|future|coroutine)\b", "async / concurrency"),
    # Dependency / architecture
    (r"\b(dependency|inject|coupling|abstraction|interface|contract)\b", "dependency / coupling"),
    (r"\b(layer|architecture|separation.?of.?concern|solid)\b", "architecture"),
    # Open banking specific
    (r"\b(psd2|open.?banking|consent|tpp|aspsp|pisp|aisp)\b", "open banking / PSD2"),
    (r"\b(payment|transaction|account|balance|mandate)\b", "payments domain"),
    (r"\b(idempotent|idempotency)\b", "idempotency"),
    (r"\b(retry|back.?off|circuit.?breaker)\b", "resilience"),
    (r"\b(audit|audit.?trail|log|logging|monitor)\b", "observability / audit"),
    (r"\b(encrypt|tls|mtls|certificate|cipher)\b", "encryption / TLS"),
]


def normalise(text):
    """Lowercase, strip punctuation, remove stopwords."""
    text = text.lower()
    text = re.sub(r"[^a-z0-9\s]", " ", text)
    tokens = [t for t in text.split() if t not in STOPWORDS and len(t) > 2]
    return tokens


def classify_comment(body):
    """Return all matching anchor categories for a comment body."""
    body_lower = body.lower()
    matches = []
    for pattern, label in ANCHOR_PATTERNS:
        if re.search(pattern, body_lower, re.IGNORECASE):
            matches.append(label)
    return matches if matches else ["uncategorised"]


# ---------------------------------------------------------------------------
# Cluster building
# ---------------------------------------------------------------------------

def build_clusters(comments):
    """
    Group comments by anchor category. Returns:
        { category: [comment, ...] }
    """
    clusters = defaultdict(list)
    for c in comments:
        categories = classify_comment(c["body"])
        for cat in categories:
            clusters[cat].append(c)
    return clusters


# ---------------------------------------------------------------------------
# Rule synthesis
# ---------------------------------------------------------------------------

def top_phrases(comments, n=5):
    """Extract the most common 2-3 word phrases across a set of comments."""
    bigrams = Counter()
    trigrams = Counter()
    for c in comments:
        tokens = normalise(c["body"])
        for i in range(len(tokens) - 1):
            bigrams[f"{tokens[i]} {tokens[i+1]}"] += 1
        for i in range(len(tokens) - 2):
            trigrams[f"{tokens[i]} {tokens[i+1]} {tokens[i+2]}"] += 1

    combined = Counter()
    combined.update(bigrams)
    combined.update(trigrams)
    return [phrase for phrase, _ in combined.most_common(n)]


def resolution_rate(comments):
    """Fraction of resolvable comments that were resolved."""
    resolvable = [c for c in comments if c.get("resolvable")]
    if not resolvable:
        return None
    resolved = sum(1 for c in resolvable if c.get("resolved"))
    return resolved / len(resolvable)


def sample_quotes(comments, n=3):
    """Return up to n short representative comment bodies."""
    # Prefer resolved inline comments as they were actionable
    inline_resolved = [c for c in comments if c.get("is_inline") and c.get("resolved")]
    pool = inline_resolved or comments
    # Sort by body length — shorter quotes are more quotable
    pool = sorted(pool, key=lambda c: len(c["body"]))
    seen = set()
    quotes = []
    for c in pool:
        body = c["body"].strip()
        # Deduplicate near-identical bodies
        key = body[:60]
        if key not in seen and len(body) > 10:
            seen.add(key)
            quotes.append(body[:200] + ("..." if len(body) > 200 else ""))
        if len(quotes) >= n:
            break
    return quotes


def synthesise_rules(clusters, min_comments=3):
    """
    For each cluster with enough evidence, synthesise a rule.
    Returns a list of rule dicts sorted by frequency descending.
    """
    rules = []
    for category, comments in clusters.items():
        if len(comments) < min_comments:
            continue

        rate = resolution_rate(comments)
        inline_count = sum(1 for c in comments if c.get("is_inline"))
        affected_files = Counter(
            c["file_path"] for c in comments if c.get("file_path")
        ).most_common(3)

        rule = {
            "category": category,
            "frequency": len(comments),
            "resolution_rate": rate,
            "inline_ratio": inline_count / len(comments) if comments else 0,
            "top_phrases": top_phrases(comments),
            "affected_files": [f for f, _ in affected_files],
            "sample_quotes": sample_quotes(comments),
        }
        rules.append(rule)

    rules.sort(key=lambda r: r["frequency"], reverse=True)
    return rules


# ---------------------------------------------------------------------------
# Markdown output
# ---------------------------------------------------------------------------

def priority_label(freq, total):
    ratio = freq / total if total else 0
    if ratio >= 0.15:
        return "HIGH"
    if ratio >= 0.06:
        return "MEDIUM"
    return "LOW"


def render_markdown(rules, meta):
    lines = []
    lines.append("# Code Review Rule Set")
    lines.append("")
    lines.append(f"**Project:** `{meta['project']}`  ")
    lines.append(f"**MRs analysed:** {meta['total_mrs_processed']}  ")
    lines.append(f"**Comments analysed:** {meta['total_comments']}  ")
    lines.append(f"**Rules generated:** {len(rules)}  ")
    lines.append("")
    lines.append("Rules are ranked by frequency across MR history. "
                 "High-priority rules appeared in ≥15% of all comments; "
                 "medium in 6–15%; low in <6%.")
    lines.append("")
    lines.append("---")
    lines.append("")

    total = meta["total_comments"]

    for i, rule in enumerate(rules, 1):
        priority = priority_label(rule["frequency"], total)
        rate_str = (
            f"{rule['resolution_rate']:.0%} resolution rate"
            if rule["resolution_rate"] is not None
            else "no resolution data"
        )
        inline_str = f"{rule['inline_ratio']:.0%} inline diff comments"

        lines.append(f"## Rule {i}: {rule['category'].title()}")
        lines.append("")
        lines.append(f"**Priority:** {priority}  ")
        lines.append(f"**Evidence:** {rule['frequency']} comments — {rate_str}, {inline_str}  ")

        if rule["top_phrases"]:
            phrases = ", ".join(f"`{p}`" for p in rule["top_phrases"])
            lines.append(f"**Common phrases:** {phrases}  ")

        if rule["affected_files"]:
            files = ", ".join(f"`{f}`" for f in rule["affected_files"])
            lines.append(f"**Hot files:** {files}  ")

        lines.append("")

        if rule["sample_quotes"]:
            lines.append("**Example review comments:**")
            lines.append("")
            for quote in rule["sample_quotes"]:
                # Indent multiline quotes
                indented = quote.replace("\n", "\n> ")
                lines.append(f"> {indented}")
                lines.append("")

        lines.append("**Suggested rule:**")
        lines.append("")
        lines.append(f"> _TODO: Refine this rule based on the {rule['category']} comments above._")
        lines.append("")
        lines.append("---")
        lines.append("")

    return "\n".join(lines)


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description="Analyse MR comments and generate a code review rule set")
    parser.add_argument("--input", required=True, help="Path to JSON file from fetch_mr_comments.py")
    parser.add_argument(
        "--output",
        default=None,
        help="Output Markdown file (default: same dir as input, with _rules.md suffix)",
    )
    parser.add_argument(
        "--min-comments",
        type=int,
        default=3,
        help="Minimum comments in a cluster to generate a rule (default: 3)",
    )
    args = parser.parse_args()

    if not os.path.exists(args.input):
        print(f"Error: input file not found: {args.input}", file=sys.stderr)
        sys.exit(1)

    with open(args.input, encoding="utf-8") as f:
        data = json.load(f)

    comments = data.get("comments", [])
    if not comments:
        print("No comments found in input file. Nothing to analyse.", file=sys.stderr)
        sys.exit(1)

    print(f"Loaded {len(comments)} comments from {data.get('total_mrs_processed', '?')} MRs")

    clusters = build_clusters(comments)
    print(f"Identified {len(clusters)} categories")

    rules = synthesise_rules(clusters, min_comments=args.min_comments)
    print(f"Generated {len(rules)} rules (min {args.min_comments} comments per rule)")

    # Determine output path: same stem as input but _rules.md
    if args.output:
        output_path = args.output
    else:
        base = os.path.splitext(args.input)[0]
        output_path = f"{base}_rules.md"

    os.makedirs(os.path.dirname(output_path) or ".", exist_ok=True)

    md = render_markdown(rules, data)
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(md)

    print(f"\nRule set written to: {output_path}")


if __name__ == "__main__":
    main()
