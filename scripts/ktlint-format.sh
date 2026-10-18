#!/bin/sh
# 1 つの .kt / .kts を ktlint で整形する。VS Code の保存時（.vscode/settings.json）と
# Claude Code の編集後の hook（.claude/settings.json）から呼ぶ。
# 引数がなければ、hook が標準入力に渡す JSON から編集したファイルを読む。
file=${1:-$(jq -r '.tool_input.file_path // empty')}
case "$file" in
*.kt | *.kts) ;;
*) exit 0 ;;
esac
cd "$(dirname "$0")/.." || exit 0
# 自動で直せない違反（長すぎる行など）が残ったら、hook では exit 2 でエージェントに見せる
./gradlew -q ktlintFormat -PktlintFiles="$file" >&2 || exit 2
