# Plan A — Locale-Keyed Content Files

## Overview

Introduce locale-specific JSON content directories that mirror the existing
`content/` tree. The generator merges each locale's translations on top of the
English baseline at build time, then emits a fully localised copy of every
page under a locale-prefixed path (e.g. `site/pt-BR/language/slug.html`).

A language selector in the navigation bar lets users switch locales. The
English site continues to live at `/` (no prefix) so existing links and
search-engine rankings are preserved.

---

## Directory Layout

```
content/                        # English source of truth (unchanged)
  language/
    type-inference-with-var.json
  collections/
    ...

content/i18n/                   # Locale overlays
  pt-BR/
    language/
      type-inference-with-var.json   # Only the fields that differ
    ui.json                          # Navigation / section labels
  ja/
    language/
      type-inference-with-var.json
    ui.json
  ...

templates/                      # Templates unchanged
html-generators/
  categories.properties         # English display names
  categories.pt-BR.properties   # Localised category display names
  generate.java / generate.py   # Extended to accept a --locale flag

site/                           # Generated output
  index.html                    # English home (no prefix)
  language/
    type-inference-with-var.html
  data/
    snippets.json               # English search index
  pt-BR/                        # Locale subtree
    index.html
    language/
      type-inference-with-var.html
    data/
      snippets.json
  ja/
    ...
```

---

## JSON Overlay Schema

Each locale overlay file contains only the fields that change; everything else
is inherited from the English baseline file.

```json
// content/i18n/pt-BR/language/type-inference-with-var.json
{
  "title": "Inferência de tipo com var",
  "summary": "Use var para deixar o compilador inferir o tipo local.",
  "explanation": "...",
  "oldApproach": "Tipos explícitos",
  "modernApproach": "Palavra-chave var",
  "whyModernWins": [
    { "icon": "⚡", "title": "Menos ruído", "desc": "..." },
    { "icon": "👁",  "title": "Mais legível", "desc": "..." },
    { "icon": "🔒", "title": "Seguro", "desc": "..." }
  ]
}
```

Fields **not** present in the overlay (`oldCode`, `modernCode`, `jdkVersion`,
`docs`, `related`, `prev`, `next`, `support`) are always taken from the
English baseline — code snippets, links, and navigation stay language-neutral.

### `ui.json` — UI Strings

```json
// content/i18n/pt-BR/ui.json
{
  "nav": {
    "allPatterns": "← Todos os padrões",
    "toggleTheme": "Alternar tema",
    "viewOnGitHub": "Ver no GitHub"
  },
  "sections": {
    "codeComparison": "Comparação de código",
    "whyModernWins": "Por que a forma moderna ganha",
    "oldApproach": "Abordagem antiga",
    "modernApproach": "Abordagem moderna",
    "sinceJdk": "Desde o JDK",
    "difficulty": "Dificuldade",
    "jdkSupport": "Suporte ao JDK",
    "howItWorks": "Como funciona",
    "relatedDocs": "Documentação relacionada",
    "relatedPatterns": "Padrões relacionados"
  },
  "filters": {
    "show": "Mostrar:",
    "all": "Todos",
    "difficulty": {
      "beginner": "Iniciante",
      "intermediate": "Intermediário",
      "advanced": "Avançado"
    }
  },
  "search": {
    "placeholder": "Buscar padrões...",
    "noResults": "Nenhum resultado encontrado."
  }
}
```

---

## Generator Changes

1. **Accept `--locale` flag** (e.g. `generate.java --locale pt-BR`).  
   When omitted the generator builds only the English site (current
   behaviour).

2. **Merge algorithm**:
   ```
   merged = deepMerge(english_baseline, locale_overlay)
   ```
   Merge happens in memory; neither source file is modified.

3. **Output path**: all files for locale `<loc>` are written to
   `site/<loc>/…` with relative asset paths adjusted
   (`../../styles.css`, `../../app.js`, etc.).

4. **Category display names**: loaded from
   `html-generators/categories.<locale>.properties` when it exists,
   falling back to `categories.properties`.

5. **`data/snippets.json`**: a separate
   `site/<loc>/data/snippets.json` is generated from the merged content
   so the locale-specific search index is accurate.

6. **Build all locales at once**:
   ```bash
   jbang html-generators/generate.java          # English
   jbang html-generators/generate.java --locale pt-BR
   jbang html-generators/generate.java --locale ja
   ```
   Or, when a build-all mode is added:
   ```bash
   jbang html-generators/generate.java --all-locales
   ```

---

## Template Changes

Templates (`templates/slug-template.html`, `templates/index.html`) gain
additional `{{…}}` tokens for localised UI strings:

| Token | Source field |
|---|---|
| `{{labelCodeComparison}}` | `sections.codeComparison` |
| `{{labelWhyModernWins}}` | `sections.whyModernWins` |
| `{{labelHowItWorks}}` | `sections.howItWorks` |
| `{{labelRelatedDocs}}` | `sections.relatedDocs` |
| `{{labelRelatedPatterns}}` | `sections.relatedPatterns` |
| `{{navAllPatterns}}` | `nav.allPatterns` |
| `{{searchPlaceholder}}` | `search.placeholder` |

English values are hard-coded defaults (matching the current literal strings)
so the English build requires no `ui.json`.

---

## HTML `<head>` Changes

Each detail page gains `<link rel="alternate">` tags and the `<html lang>`
attribute is set to the locale:

```html
<html lang="pt-BR">
<head>
  ...
  <link rel="alternate" hreflang="en"    href="https://javaevolved.github.io/language/type-inference-with-var.html">
  <link rel="alternate" hreflang="pt-BR" href="https://javaevolved.github.io/pt-BR/language/type-inference-with-var.html">
  <link rel="alternate" hreflang="x-default" href="https://javaevolved.github.io/language/type-inference-with-var.html">
</head>
```

---

## Navigation — Language Selector

A locale pill is added to the nav bar (right-hand side, beside the theme
toggle). It is rendered as a `<select>` or a dropdown `<button>`:

```html
<select id="localePicker" aria-label="Select language">
  <option value="en"    selected>English</option>
  <option value="pt-BR">Português (Brasil)</option>
  <option value="ja">日本語</option>
</select>
```

JavaScript (`app.js`) listens for changes and navigates to the equivalent
page in the chosen locale by rewriting the path prefix.

---

## GitHub Actions Changes

The deploy workflow gains a step that loops over every locale directory in
`content/i18n/` and invokes the generator for each:

```yaml
- name: Generate locales
  run: |
    jbang html-generators/generate.java
    for locale in content/i18n/*/; do
      loc=$(basename "$locale")
      jbang html-generators/generate.java --locale "$loc"
    done
```

---

## Migration Path

| Phase | Work |
|---|---|
| 1 | Add `ui.json` English defaults; extract hard-coded label strings from templates into tokens |
| 2 | Extend generator to accept `--locale`, implement merge algorithm |
| 3 | Add language selector to nav; write `app.js` path-rewrite logic |
| 4 | Create first locale overlay (e.g. `pt-BR`) as a proof-of-concept |
| 5 | Update GitHub Actions; add `hreflang` and `<html lang>` to templates |

---

## Trade-offs

| Pros | Cons |
|---|---|
| Overlay-only translations mean small locale files | Translators must know the JSON schema |
| English site path unchanged — SEO not affected | Each new locale doubles generated-file count |
| Build-time generation — no runtime i18n library needed | Generator becomes more complex |
| Code snippets stay language-neutral automatically | Navigation (`prev`/`next`) still points to English slugs (cross-locale navigation not addressed) |
