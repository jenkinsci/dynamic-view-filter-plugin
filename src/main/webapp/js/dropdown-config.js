(function() {
    'use strict';

    function toggleDropdownFields(sel) {
        var container = sel.closest('.repeated-chunk');
        if (!container) {
            container = sel.parentNode;
            while (container && container !== document.body) {
                if (container.querySelector('.dd-job-regex-field') &&
                    container.querySelector('.dd-build-param-field')) {
                    break;
                }
                container = container.parentNode;
            }
        }
        if (!container) return;
        var val = sel.value;
        var regexFields = container.querySelectorAll('.dd-job-regex-field');
        var paramFields = container.querySelectorAll('.dd-build-param-field');
        for (var i = 0; i < regexFields.length; i++) {
            regexFields[i].style.display = (val === 'jobNameRegex' || val === '') ? '' : 'none';
        }
        for (var i = 0; i < paramFields.length; i++) {
            paramFields[i].style.display = val === 'buildParameter' ? '' : 'none';
        }
    }

    // Event delegation: handles both existing and dynamically added selects
    document.addEventListener('change', function(e) {
        if (e.target && e.target.classList.contains('dropdown-source-type')) {
            toggleDropdownFields(e.target);
        }
    });

    // Initialize existing selects on page load
    function initAll() {
        document.querySelectorAll('.dropdown-source-type').forEach(function(sel) {
            toggleDropdownFields(sel);
        });
    }

    // Run init when DOM is ready and also observe for new repeatable chunks
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAll);
    } else {
        initAll();
    }

    // Observe for dynamically added repeatable chunks
    var observer = new MutationObserver(function(mutations) {
        for (var i = 0; i < mutations.length; i++) {
            var nodes = mutations[i].addedNodes;
            for (var j = 0; j < nodes.length; j++) {
                if (nodes[j].nodeType === 1 && nodes[j].querySelectorAll) {
                    var selects = nodes[j].querySelectorAll('.dropdown-source-type');
                    for (var k = 0; k < selects.length; k++) {
                        toggleDropdownFields(selects[k]);
                    }
                }
            }
        }
    });
    observer.observe(document.body || document.documentElement, { childList: true, subtree: true });
})();
