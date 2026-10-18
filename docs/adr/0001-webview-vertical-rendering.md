# 縦書き本文は WebView + CSS 段組みで描画する

Android/Compose には縦書きテキスト描画がなく、ルビ・傍点・禁則・縦中横を Canvas で自作すると数か月規模になる。そこで青空文庫の XHTML を WebView に読み込み、`writing-mode: vertical-rl` で縦書きにし、CSS 段組み（`overflow: hidden`）で1画面ずつに分割してページをめくる。元アプリ（青空読書）も同じ方式であることを APK の静的解析で確認しており、読み心地を寄せる目的にも合う。

## Consequences

- 画面サイズや読書位置のやり取りは `@JavascriptInterface` 経由になり、本文の UI は Compose ではなく HTML/CSS/JS 側に持つ。
- 元アプリのコードは流用しない（ソースを公開するため）。方式のみ参考にする。
- 縦書き（`vertical-rl`）の段組みでは段が上から下へ並ぶ。そこで段の高さ（`column-width`）を画面の高さにそろえ、縦にスクロールして 1 ページずつ見せる（`scrollLeft` ではなく `scrollTop`）。画面の大きさは CSS の `vh` ではなく JavaScript から px で渡す（WebView の大きさが決まる前は `vh` が 0 になる）。
- めくりの「スライド」は WebView の中では動かさず、めくる前の画面を Compose で写し取り、それを画面の外へ滑らせて見せる。
