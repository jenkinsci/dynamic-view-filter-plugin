Behaviour.specify('#dynamic-view-filter-bar', 'DropdownFilterView-main', 0, function(bar) {
    // Reposition filter bar after the tab bar
    var tabBar = document.querySelector('.tabBar') || document.querySelector('.jenkins-tab-bar');
    if (tabBar && tabBar.parentNode) {
        tabBar.parentNode.insertBefore(bar, tabBar.nextSibling);
    }
    bar.style.display = '';

    var content = document.getElementById('dvf-content');
    var toggle = document.getElementById('dvf-toggle');

    // Restore collapse state from localStorage
    if (localStorage.getItem('dvf-collapsed') === 'true' && content && toggle) {
        content.style.display = 'none';
        var chevDown = toggle.querySelector('.dvf-chevron-down');
        var chevRight = toggle.querySelector('.dvf-chevron-right');
        if (chevDown) chevDown.style.display = 'none';
        if (chevRight) chevRight.style.display = '';
    }

    // Toggle collapse/expand
    if (toggle) {
        toggle.addEventListener('click', function() {
            var chevDown = this.querySelector('.dvf-chevron-down');
            var chevRight = this.querySelector('.dvf-chevron-right');
            if (content.style.display === 'none') {
                content.style.display = '';
                if (chevDown) chevDown.style.display = '';
                if (chevRight) chevRight.style.display = 'none';
                localStorage.setItem('dvf-collapsed', 'false');
            } else {
                content.style.display = 'none';
                if (chevDown) chevDown.style.display = 'none';
                if (chevRight) chevRight.style.display = '';
                localStorage.setItem('dvf-collapsed', 'true');
            }
        });
    }

    // Auto-submit dropdowns on change
    var form = document.getElementById('dvf-form');
    if (form) {
        form.querySelectorAll('select').forEach(function(sel) {
            sel.addEventListener('change', function() {
                form.submit();
            });
        });
    }

    // Clear All button
    var clearBtn = document.getElementById('dvf-clear-all');
    if (clearBtn && form) {
        clearBtn.addEventListener('click', function() {
            var selects = form.querySelectorAll('select');
            for (var i = 0; i < selects.length; i++) {
                selects[i].selectedIndex = 0;
            }
            form.submit();
        });
    }
});
