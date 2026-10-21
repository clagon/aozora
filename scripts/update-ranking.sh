#!/bin/sh
# 青空文庫公式のアクセスランキング（XHTML 版）を取り直し、app/src/main/assets/ranking.tsv に書き出す。
# 1 行が 1 期間で、「期間（2022 は通年、2022-05 は月）<TAB>作品 ID を 1 位から , でつないだもの」。新しい期間から並ぶ。
# 公式の更新は 2022 年で止まっているので、ふだんは流さない。ページが消えている月（404）だけは飛ばす。
# 途中で取れなかったり形が違ったりしたら止め、今の ranking.tsv はそのまま残す。
set -eu
base=https://www.aozora.gr.jp/access_ranking
out=$(dirname "$0")/../app/src/main/assets/ranking.tsv
tmp=$(mktemp)
page=$(mktemp)
trap 'rm -f "$tmp" "$page"' EXIT

index=$(curl -fsS "$base/")
pages=$(echo "$index" | grep -o 'href="[0-9_]*_xhtml\.html"' | sed 's/href="//; s/"//')
[ -n "$pages" ] || { echo "no pages in $base/" >&2; exit 1; }
for name in $pages; do
    status=$(curl -sS -o "$page" -w '%{http_code}' "$base/$name")
    case $status in
        200) ;;
        404) echo "skip $name (404)" >&2; continue ;;
        *) echo "failed $name ($status)" >&2; exit 1 ;;
    esac
    ids=$(grep -o 'card[0-9]*\.html' "$page" | sed 's/card//; s/\.html//' | paste -sd, -)
    count=$(echo "$ids" | tr , '\n' | grep -c .)
    [ "$count" -eq 500 ] || { echo "$name has $count works, not 500" >&2; exit 1; }
    printf '%s\t%s\n' "$(echo "${name%_xhtml.html}" | tr _ -)" "$ids" >>"$tmp"
    sleep 0.3
done
mv "$tmp" "$out"
