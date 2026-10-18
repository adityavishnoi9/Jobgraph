# JobGraph Gmail learning guide

Start with [JobGraph-Gmail-Guide.pdf](JobGraph-Gmail-Guide.pdf). It is a 30-page
explanation of the Gmail implementation reviewed on 3 October 2026, with flow
diagrams, important source excerpts, security explanations, and debugging notes.

The [HTML edition](JobGraph-Gmail-Guide.html) contains the same material and can
be opened in a browser. No real API keys, client secrets, passwords, Google
tokens, or mailbox contents are included. Example addresses are fictional.

## Rebuilding

From the repository root:

```sh
python3 docs/gmail-guide/build_guide.py
```

This uses Python's standard library to generate the HTML and
`source-manifest.json`. It reads the named application source files, not `.env`.
The manifest records the SHA-256 hashes of the files used for code excerpts.

Open the HTML in Chrome and print to PDF with A4 paper, no margins, 100% scale,
background graphics enabled, and browser headers/footers disabled. The HTML
already includes its own page numbers and source references. The expected
output is 30 pages.

The explanations and line selections describe a specific source snapshot.
After application changes, review them before regenerating. Updating excerpts
alone does not automatically update the surrounding explanation.

## Document verification

The published PDF was checked for 30 pages, selectable text, page-content and
diagram bounds, source-excerpt consistency, and credential-pattern absence.
All pages were rendered for layout review. This documentation task did not
perform a new live Google consent test or rerun the backend application tests.
