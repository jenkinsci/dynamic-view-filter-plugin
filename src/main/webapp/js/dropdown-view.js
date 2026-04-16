(function() {
    'use strict';

    function initFilterBar() {
        var bar = document.getElementById('dynamic-view-filter-bar');
        if (!bar) return;

        var position = bar.getAttribute('data-position') || 'top';
        var content = document.getElementById('dvf-content');
        var toggle = document.getElementById('dvf-toggle');
        var form = document.getElementById('dvf-form');

        // Place bar inside .dashboard, between tab bar and table
        var tabBar = document.getElementById('projectstatus-tabBar');
        var dashboard = tabBar ? tabBar.parentNode : null;

        // Fallback: when no jobs match, Jenkins omits the dashboard/projectstatus-tabBar.
        // Look for the .tabBarFrame instead so the filter bar still appears.
        if (!dashboard) {
            var tabFrame = document.querySelector('.tabBarFrame');
            if (!tabFrame) return;
            var parent = tabFrame.parentNode;

            if (position === 'sidebar') {
                // Wrap remaining content + bar in a sidebar layout
                var emptyContent = tabFrame.nextElementSibling;
                var wrapper = document.createElement('div');
                wrapper.classList.add('dvf-sidebar-wrapper');
                parent.insertBefore(wrapper, emptyContent);
                if (emptyContent) wrapper.appendChild(emptyContent);
                wrapper.appendChild(bar);
                bar.classList.remove('jenkins-hidden');
            } else {
                parent.insertBefore(bar, tabFrame.nextSibling);
                bar.classList.remove('jenkins-hidden');
                bar.classList.add('dvf-filter-bar--sticky');
            }
        } else {
            var desktopTable = dashboard.querySelector('.jenkins-mobile-hide');

            if (position === 'sidebar' && desktopTable) {
                initSidebar(bar, desktopTable, dashboard, tabBar);
            } else {
                // Top mode: insert bar between tab bar and table, sticky on scroll
                dashboard.insertBefore(bar, tabBar.nextSibling);
                bar.classList.remove('jenkins-hidden');
                bar.classList.add('dvf-filter-bar--sticky');
            }
        }

        // Restore collapse state from localStorage
        var storageKey = 'dvf-collapsed-' + position;
        if (localStorage.getItem(storageKey) === 'true' && content && toggle) {
            collapseContent(content, position, bar);
        }

        // Toggle collapse/expand
        if (toggle) {
            toggle.addEventListener('click', function() {
                var isCollapsed = bar.classList.contains('dvf-filter-bar--collapsed');
                if (isCollapsed) {
                    expandContent(content, position, bar);
                    localStorage.setItem(storageKey, 'false');
                } else {
                    collapseContent(content, position, bar);
                    localStorage.setItem(storageKey, 'true');
                }
            });
        }

        // Auto-submit dropdowns on change
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
    }

    function initSidebar(bar, desktopTable, dashboard, tabBar) {
        // Create a flex wrapper for table + sidebar, insert after tab bar
        var wrapper = document.createElement('div');
        wrapper.classList.add('dvf-sidebar-wrapper');

        // Insert wrapper after tab bar
        dashboard.insertBefore(wrapper, tabBar.nextSibling);

        // Move desktop table and bar into wrapper
        wrapper.appendChild(desktopTable);
        wrapper.appendChild(bar);

        // Show the bar
        bar.classList.remove('jenkins-hidden');
    }

    function collapseContent(content, position, bar) {
        bar.classList.add('dvf-filter-bar--collapsed');
        if (position === 'sidebar') {
            content.classList.add('jenkins-hidden');
            bar.classList.add('dvf-sidebar--collapsed');
        }
    }

    function expandContent(content, position, bar) {
        bar.classList.remove('dvf-filter-bar--collapsed');
        if (position === 'sidebar') {
            content.classList.remove('jenkins-hidden');
            bar.classList.remove('dvf-sidebar--collapsed');
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initFilterBar);
    } else {
        initFilterBar();
    }
})();
