// 縦書きの本文を 1 画面ずつめくる。ページの番号はアプリ側（ReaderScreen）が持ち、ここは数えて動かすだけ。
(function () {
    // 本文の字下げは横書き向けに margin-left で書かれているので、縦書きの上の余白に移す
    function verticalIndents() {
        document.querySelectorAll('[style*="margin-left"]').forEach(function (el) {
            el.style.marginTop = el.style.marginLeft;
            el.style.marginLeft = '';
        });
    }

    // 見出し（大・中・小 = h3・h4・h5）と、それが載っているページ。目次と前後の章への移動に使う。
    // 1 行に 1 つ「階層<TAB>ページ<TAB>見出し」の形で返す（Heading.kt の parseHeadings が読む）
    function headings() {
        var levels = { H3: 0, H4: 1, H5: 2 };
        var found = document.querySelectorAll('.main_text h3, .main_text h4, .main_text h5');
        return Array.prototype.map.call(found, function (el) {
            var page = Math.floor((el.getBoundingClientRect().top + window.scrollY) / window.innerHeight);
            return levels[el.tagName] + '\t' + page + '\t' + el.textContent.replace(/\s+/g, ' ').trim();
        }).join('\n');
    }

    // 段は画面の高さごとに並ぶので、文書の高さを画面の高さで割ったものがページ数
    function layout() {
        if (window.innerHeight === 0) return;
        var root = document.documentElement.style;
        root.setProperty('--page-width', window.innerWidth + 'px');
        root.setProperty('--page-height', window.innerHeight + 'px');
        document.documentElement.classList.add('paged');
        var pages = Math.max(1, Math.round(document.documentElement.scrollHeight / window.innerHeight));
        Android.onLayout(pages, headings());
    }

    window.reader = {
        // page は 0 から数える
        go: function (page) {
            window.scrollTo(0, page * window.innerHeight);
        },
        // 文字パネルの設定（CSS の変数）を入れ替え、ページを数え直す
        style: function (variables) {
            document.getElementById('reader-vars').textContent = variables;
            layout();
        },
    };

    // 本文が読めた時点で組み、画像（外字・挿絵）が揃って大きさが変わったらもう一度組む。
    // 外字の多い作品は画像が揃うまで時間がかかるので、load だけを待つと長いあいだ組まれないままになる
    document.addEventListener('DOMContentLoaded', function () {
        verticalIndents();
        layout();
    });
    window.addEventListener('load', layout);
    window.addEventListener('resize', layout);
})();
