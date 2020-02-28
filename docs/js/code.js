hljs.initHighlightingOnLoad();
$ (function () {
    hljs.initLineNumbersOnLoad ({
        'oncreate': function () {
            var self = $(this), table = self.closest ('table'), selected = self.attr ('data-selected');
            table.find ('tr[data-selected]').removeAttr ('data-selected').removeClass ('hljs-ln-row-selected');
            if (!selected) {
                self.attr ('data-selected', true).addClass ('hljs-ln-row-selected');
            }
        }
    });
});