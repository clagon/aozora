// 縦書きの本文を 1 画面ずつめくる。ページの番号はアプリ側（ReaderScreen）が持ち、ここは数えて動かすだけ。
(function () {
    // 本文の字下げは横書き向けに margin-left で書かれているので、縦書きの上の余白に移す
    function verticalIndents() {
        document.querySelectorAll('[style*="margin-left"]').forEach(function (el) {
            el.style.marginTop = el.style.marginLeft;
            el.style.marginLeft = '';
        });
    }

    // 段は画面の高さごとに並ぶので、文書の高さを画面の高さで割ったものがページ数
    function layout() {
        if (window.innerHeight === 0) return;
        var root = document.documentElement.style;
        root.setProperty('--page-width', window.innerWidth + 'px');
        root.setProperty('--page-height', window.innerHeight + 'px');
        var pages = Math.max(1, Math.round(document.documentElement.scrollHeight / window.innerHeight));
        Android.onLayout(pages);
    }

    window.reader = {
        // page は 0 から数える
        go: function (page) {
            window.scrollTo(0, page * window.innerHeight);
        },
    };

    document.addEventListener('DOMContentLoaded', verticalIndents);
    window.addEventListener('load', layout);
    window.addEventListener('resize', layout);
})();
