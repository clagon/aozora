#!/bin/sh
# 青空文庫公式のアクセスランキング（XHTML 版）を取り直し、app/src/main/assets/ranking.tsv に書き出す。
# 1 行が 1 期間で、「期間（2022 は通年、2022-05 は月）<TAB>作品 ID を 1 位から , でつないだもの」。新しい期間から並ぶ。
# 公式の更新は 2022 年で止まっているので、ふだんは流さない。ページが消えている月（404）は飛ばす。
set -eu
base=https://www.aozora.gr.jp/access_ranking
out=$(dirname "$0")/../app/src/main/assets/ranking.tsv
: >"$out"
for page in $(curl -fsS "$base/" | grep -o 'href="[0-9_]*_xhtml\.html"' | sed 's/href="//; s/"//'); do
    ids=$(curl -fsS "$base/$page" 2>/dev/null | grep -o 'card[0-9]*\.html' | sed 's/card//; s/\.html//' | paste -sd, -) || true
    [ -n "$ids" ] || { echo "skip $page" >&2; continue; }
    printf '%s\t%s\n' "$(echo "${page%_xhtml.html}" | tr _ -)" "$ids" >>"$out"
    sleep 0.3
done
