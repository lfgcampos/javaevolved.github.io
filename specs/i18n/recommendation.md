# i18n Architecture Recommendation

## TL;DR — **Plan B is recommended** for an AI-driven translation workflow.

---

## Why Plan B wins when translations are AI-generated

When a new slug is added and AI generates the translations automatically, the
pipeline becomes:

```
New English slug  →  AI prompt  →  Translated JSON file  →  Commit to repo
```

### Plan A (overlay) is awkward for AI

- AI must know *which fields to include* in the overlay and which to omit
  (`oldCode`, `modernCode`, `docs`, `related`, `prev`, `next` must all be
  absent).
- If the AI accidentally includes any of those fields the build silently uses
  the AI value instead of the canonical English one — hard to detect.
- Prompt engineering must explicitly say "only output these seven fields":
  `title`, `summary`, `explanation`, `oldApproach`, `modernApproach`,
  `whyModernWins`, `support.description`.
- The overlay schema is a non-standard concept; every new contributor (human
  or AI) needs to learn it.

### Plan B (full replacement) is ideal for AI

- AI receives the full English JSON and outputs a complete translated JSON.
  No special field-filtering rules.
- `oldCode` and `modernCode` are simply copied verbatim from the English file
  at build time, regardless of what the AI wrote — the generator always
  overwrites them. Zero prompt-engineering required to handle this case.
- The output is **self-contained and trivially validatable**: run the same JSON
  schema checks as for English files.
- The fallback mechanism is explicit: if the AI-generated file does not yet
  exist, the generator falls back to English and can display an "untranslated"
  banner — a clear signal rather than a silent gap.
- `translations/strings/{locale}.json` (UI strings) is a simple key-value
  file; AI can translate it in one shot with minimal instructions.

---

## Recommended AI automation workflow

1. **Trigger**: GitHub Actions detects a new `content/<cat>/<slug>.json` commit
   (or a workflow dispatch).
2. **AI step**: For each supported locale, call the translation AI with a
   structured prompt:
   ```
   Translate the following Java pattern JSON from English to {locale}.
   - Keep slug, id, category, difficulty, jdkVersion, oldLabel, modernLabel,
     oldCode, modernCode, docs, related, prev, next, support.state unchanged.
   - Translate: title, summary, explanation, oldApproach, modernApproach,
     whyModernWins[*].title, whyModernWins[*].desc, support.description.
   - Return valid JSON only.
   ```
3. **Validate**: Run the same JSON schema validation used for English files.
4. **Write**: Commit the translated file to
   `translations/content/{locale}/<cat>/<slug>.json`.
5. **Build**: The generator picks it up on the next deployment and removes the
   "untranslated" banner automatically.

> **Note**: Even though Plan B asks translators to provide `oldCode` and
> `modernCode`, the build tooling always overwrites those fields with the
> English values. So AI can safely copy them verbatim — no risk of translated
> or hallucinated code leaking into the site.

---

## Addressing Plan B's main risk

Plan B's biggest concern is keeping translated content files in sync when the
English original changes (e.g. a corrected explanation). The AI workflow
mitigates this:

- When a `content/<cat>/<slug>.json` file is **modified**, the same automation
  can regenerate the translated file, or flag the diff for human review.
- A simple CI check can compare the `jdkVersion`, `id`, and `slug` in the
  English file with the translated file to catch stale translations.

---

## Summary

| Criterion | Plan A | Plan B |
|---|---|---|
| AI prompt complexity | Higher (must specify excluded fields) | Lower (translate whole file; build ignores code fields) |
| Risk of AI error slipping through | Higher (wrong field inclusion is silent) | Lower (code fields are always overwritten) |
| Validation of AI output | Schema + overlay semantics | Schema only |
| Fallback visibility | Silent (English shown without notice) | Explicit banner |
| Human translator experience | Must understand overlay contract | Self-contained file |
| **Overall for AI workflow** | ⚠️ Workable but fragile | ✅ Recommended |
