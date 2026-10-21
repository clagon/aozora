# 青空猫

[青空文庫](https://www.aozora.gr.jp/)の作品を縦書きで読む Android アプリ。配信が止まった「青空読書」の代わりに、自分で使うために作っている。

## できること

- 作家名・作品名（読みでも）で検索し、分野・作家・五十音・児童書からたどる
- 新着作品と公式のアクセスランキング（2009〜2022 年、年別・月別）を見る
- 縦書き・ページめくりで読む。文字サイズ・余白・行間・背景・夜間を変えられ、読んだ位置は自動で残る
- 本棚・読書履歴・お気に入り作家と、ホームの「続きから読む」
- 直近 7 日の読書時間

一度開いた作品は端末に保存するので、次からはオフラインでも読める。

## インストール

[Releases](https://github.com/clagon/aozora/releases/latest) から `aozora-neko-v*.apk` を落として入れる。Android 12 以上。

## ビルド

JDK 21 と Android SDK を用意して、

```sh
./gradlew assembleDebug
```

Kotlin は ktlint で整形する（`./gradlew ktlintFormat`）。仕様は [docs/spec.md](docs/spec.md)、用語は [CONTEXT.md](CONTEXT.md)、作業の決まりは [AGENTS.md](AGENTS.md) にある。

## データ

作品一覧・本文・アクセスランキングは青空文庫のものを使う。本文と作品一覧はアプリに入れず、使うときに青空文庫から取ってくる。アプリに入れているのは、ランキングの作品 ID の並びだけ。

## ライセンス

コードは [MIT License](LICENSE)。青空文庫の作品やデータの扱いは、[青空文庫収録ファイルの取り扱い規準](https://www.aozora.gr.jp/guide/kijyunn.html)に従う。
