# java.evolved

**Java has evolved. Your code can too.**

A collection of 86 side-by-side code comparisons showing old Java patterns next to their clean, modern replacements — from Java 8 all the way to Java 25.

🔗 **[javaevolved.github.io](https://javaevolved.github.io)**

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

## Tech stack

Plain HTML, CSS, and JavaScript — no frameworks, no build step. Hosted on GitHub Pages.

## Run locally

```bash
cd modern-java
python3 -m http.server 8090
# Open http://localhost:8090
```

## Modernize with GitHub Copilot

GitHub Copilot can help you migrate legacy Java codebases automatically:

- [App Modernization](https://github.com/solutions/use-case/app-modernization)
- [Modernize Java Applications with Copilot](https://docs.github.com/en/enterprise-cloud@latest/copilot/tutorials/modernize-java-applications)

## Contributing

Contributions are welcome! If you'd like to add a new snippet, fix an inaccuracy, or improve the site:

1. Fork the repo
2. Add or edit snippets in `data/snippets.json`
3. Create the corresponding `.html` article page
4. Update `index.html` with the new card
5. Open a pull request

Please ensure JDK version labels only reference the version where a feature became **final** (non-preview).

## Author

**Bruno Borges**

- GitHub: [@brunoborges](https://github.com/brunoborges)
- X/Twitter: [@brunoborges](https://x.com/brunoborges)
- LinkedIn: [brunocborges](https://www.linkedin.com/in/brunocborges)

## License

This project is licensed under the [MIT License](LICENSE).
