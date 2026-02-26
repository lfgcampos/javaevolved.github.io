# java.evolved

**Java has evolved. Your code can too.**

A collection of side-by-side code comparisons showing old Java patterns next to their clean, modern replacements — from Java 8 all the way to Java 25.

🔗 **[javaevolved.github.io](https://javaevolved.github.io)**

[![GitHub Pages](https://img.shields.io/badge/GitHub%20Pages-live-brightgreen)](https://javaevolved.github.io)
[![Snippets](https://img.shields.io/badge/snippets-107-blue)](#categories)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Contributions welcome](https://img.shields.io/badge/contributions-welcome-orange)](#contributing)

> **Note:** Update the snippet count badge above when adding new patterns.

---

## What is this?

Every snippet shows two panels:

- **✕ Old** — the traditional way (Java 7/8 era)
- **✓ Modern** — the clean, idiomatic replacement (Java 9–25)

Each comparison includes an explanation of *why* the modern approach is better, which JDK version introduced it, and links to related patterns.

## Categories

| Category | Examples |
|---|---|
| **Language** | Records, sealed classes, pattern matching, switch expressions, var, unnamed variables |
| **Collections** | Immutable factories, sequenced collections, unmodifiable collectors |
| **Strings** | Text blocks, `isBlank()`, `strip()`, `repeat()`, `formatted()`, `indent()` |
| **Streams** | `toList()`, `mapMulti()`, `takeWhile()`/`dropWhile()`, gatherers |
| **Concurrency** | Virtual threads, structured concurrency, scoped values, `ExecutorService` as `AutoCloseable` |
| **I/O** | `Files.readString()`, `writeString()`, `Path.of()`, `transferTo()`, HTTP Client |
| **Errors** | `requireNonNullElse()`, record-based errors, deserialization filters |
| **Date/Time** | `java.time` basics, `Duration`/`Period`, `DateTimeFormatter`, instant precision |
| **Security** | TLS defaults, `SecureRandom`, PEM encoding, key derivation functions |
| **Tooling** | JShell, single-file execution, JFR profiling, compact source files, AOT |
| **Enterprise** | EJB → CDI, JDBC → JPA/Jakarta Data, JNDI → injection, MDB → reactive messaging, REST |

## Architecture

This site uses a **JSON-first** build pipeline:

- **Source of truth**: Individual `content/category/slug.json` files (107 across 11 category folders)
- **Templates**: `templates/` — shared HTML templates with `{{placeholder}}` tokens
- **Generator**: `html-generators/generate.jar` — pre-built fat JAR that produces all HTML detail pages and `data/snippets.json`
- **Deploy**: GitHub Actions runs the generator and deploys to GitHub Pages

Generated files (`site/category/*.html` and `site/data/snippets.json`) are in `.gitignore` — never edit them directly.

## Build & run locally

### Prerequisites

- **Java 25+** (e.g. [Temurin](https://adoptium.net/))

### Generate and serve

```bash
# Generate all HTML pages and data/snippets.json into site/
jbang html-generators/generate.java

# Serve locally
jwebserver -b 0.0.0.0 -d site -p 8090
# Open http://localhost:8090
```

The fat JAR is a self-contained ~2.2 MB file with all dependencies bundled. No JBang installation needed.

For development on the generator itself, you can use JBang or Python — see [html-generators/README.md](html-generators/README.md) for details.

## Contributing

Contributions are welcome! Content is managed as YAML files — never edit generated HTML.

### Adding a new pattern

1. Fork the repo
2. Create a new YAML file in the appropriate `content/<category>/` folder (e.g. `content/language/my-feature.yaml`)
3. Copy [`content/template.json`](content/template.json) as a starting point for all required fields (see the [snippet schema](.github/copilot-instructions.md) for details)
4. Update the `prev`/`next` fields in adjacent pattern files to maintain navigation
5. Run `jbang html-generators/generate.java` to verify your changes build correctly
6. Open a pull request

Please ensure JDK version labels only reference the version where a feature became **final** (non-preview).

### Translating the site

The site supports multiple languages. See [`specs/i18n/i18n-spec.md`](specs/i18n/i18n-spec.md) for the full specification.

**Adding a new locale:**

1. Add the locale to `html-generators/locales.properties` (e.g. `ja=日本語`)
2. Create `translations/strings/<locale>.yaml` with all UI strings translated (copy `translations/strings/en.yaml` as a starting point)
3. Create content translation files under `translations/content/<locale>/<category>/<slug>.yaml`
4. Run `jbang html-generators/generate.java` and verify the build succeeds
5. Open a pull request

**Translating content files:**

Translation files contain **only** translatable fields — the generator merges them onto the English base at build time. This prevents translated files from diverging structurally from the English source of truth.

A translation file should contain exactly these fields:

```yaml
title: "Inferencia de tipos con var"
oldApproach: "Tipos explícitos"
modernApproach: "Palabra clave var"
summary: "Usa var para inferencia de tipos..."
explanation: "Desde Java 10, el compilador infiere..."
whyModernWins:
  - icon: "⚡"
    title: "Menos código repetitivo"
    desc: "No es necesario repetir tipos genéricos..."
  - icon: "👁"
    title: "Mejor legibilidad"
    desc: "..."
  - icon: "🔒"
    title: "Igualmente seguro"
    desc: "..."
support:
  description: "Ampliamente disponible desde JDK 10 (marzo 2018)"
```

Do **not** include `id`, `slug`, `category`, `difficulty`, `jdkVersion`, `oldCode`, `modernCode`, `prev`, `next`, `related`, or `docs` — these are always taken from the English source.

**Important:** If your text contains colons (`:`), ensure the value is properly quoted in YAML to avoid parse errors. Always validate with `jbang html-generators/generate.java` before submitting.

## Tech stack

- Plain HTML, CSS, and JavaScript — no frontend frameworks
- [JBang](https://jbang.dev) + [Jackson](https://github.com/FasterXML/jackson) for build-time generation
- Hosted on GitHub Pages via GitHub Actions

## Author

**Bruno Borges**

- GitHub: [@brunoborges](https://github.com/brunoborges)
- X/Twitter: [@brunoborges](https://x.com/brunoborges)
- LinkedIn: [brunocborges](https://www.linkedin.com/in/brunocborges)

## License

This project is licensed under the [MIT License](LICENSE).
