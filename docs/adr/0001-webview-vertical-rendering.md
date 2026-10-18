# 縦書き本文は WebView + CSS 段組みで描画する

Android/Compose には縦書きテキスト描画がなく、ルビ・傍点・禁則・縦中横を Canvas で自作すると数か月規模になる。そこで青空文庫の XHTML を WebView に読み込み、`writing-mode: vertical-rl` で縦書きにし、CSS 段組み（`column-width` = 画面幅、`overflow: hidden`）で1画面ずつに分割、`scrollLeft` を動かしてページをめくる。元アプリ（青空読書）も同じ方式であることを APK の静的解析で確認しており、読み心地を寄せる目的にも合う。

## Consequences

- 画面サイズや読書位置のやり取りは `@JavascriptInterface` 経由になり、本文の UI は Compose ではなく HTML/CSS/JS 側に持つ。
- 元アプリのコードは流用しない（ソースを公開するため）。方式のみ参考にする。
