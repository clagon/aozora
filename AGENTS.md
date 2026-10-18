# AGENTS.md

青空文庫リーダー「青空猫」（Android, Kotlin + Jetpack Compose）。仕様は [docs/spec.md](docs/spec.md)、用語は [CONTEXT.md](CONTEXT.md) を参照。

## コードの整形

Kotlin（`.kt` / `.kts`）は ktlint で整形する。規則は [.editorconfig](.editorconfig)（IntelliJ の書き方、1 行 120 文字まで）。

- `.kt` / `.kts` を編集したら、そのファイルに `scripts/ktlint-format.sh <ファイル>` をかける。全体なら `./gradlew ktlintFormat`。
  - Claude Code は `.claude/settings.json` の hook で編集のたびに自動でかかる。
  - VS Code は保存時に同じスクリプトが走る（`.vscode/settings.json`。拡張 Run on Save が要る）。
- 長すぎる行など、自動で直せない違反は手で直す。表のように 1 行にまとめておきたいところだけ `@Suppress("ktlint:standard:max-line-length")` で外す。
- CI の `./gradlew ktlintCheck` が通らない PR は出さない。
