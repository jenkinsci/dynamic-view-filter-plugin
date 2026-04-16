(function() {
    'use strict';

    function initFilterBar() {
        const bar = document.getElementById('dynamic-view-filter-bar');
        if (!bar) return;

        const position = bar.getAttribute('data-position') || 'top';
        const content = document.getElementById('dvf-content');
        const toggle = document.getElementById('dvf-toggle');
        const form = document.getElementById('dvf-form');

        // Place bar inside .dashboard, between tab bar and table
        const tabBar = document.getElementById('projectstatus-tabBar');
        const dashboard = tabBar ? tabBar.parentNode : null;

        // Fallback: when no jobs match, Jenkins omits the dashboard/projectstatus-tabBar.
        // Look for the .tabBarFrame (classic layout) or .jenkins-inline-page (new dashboard)
        // so the filter bar still appears.
        if (!dashboard) {
            const tabFrame = document.querySelector('.tabBarFrame');
            // New dashboard layout: find the content area inside .jenkins-inline-page
            const inlinePage = document.querySelector('.jenkins-inline-page');
            let contentArea = null;
            if (inlinePage) {
                for (const child of inlinePage.children) {
                    if (!child.classList.contains('jenkins-inline-page__side-panel')) {
                        contentArea = child;
                        break;
                    }
                }
            }
            const anchor = tabFrame || contentArea;
            if (!anchor) return;

            if (position === 'sidebar' && contentArea) {
                // New dashboard sidebar: wrap content + bar
                const wrapper = document.createElement('div');
                wrapper.classList.add('dvf-sidebar-wrapper');
                contentArea.parentNode.replaceChild(wrapper, contentArea);
                wrapper.appendChild(contentArea);
                wrapper.appendChild(bar);
                bar.classList.remove('jenkins-hidden');
            } else if (position === 'sidebar' && tabFrame) {
                // Classic layout sidebar fallback
                const emptyContent = tabFrame.nextElementSibling;
                const wrapper = document.createElement('div');
                wrapper.classList.add('dvf-sidebar-wrapper');
                const parent = tabFrame.parentNode;
                parent.insertBefore(wrapper, emptyContent);
                while (wrapper.nextSibling) {
                    wrapper.appendChild(wrapper.nextSibling);
                }
                wrapper.appendChild(bar);
                bar.classList.remove('jenkins-hidden');
            } else {
                if (tabFrame) {
                    tabFrame.parentNode.insertBefore(bar, tabFrame.nextSibling);
                } else if (contentArea) {
                    contentArea.insertBefore(bar, contentArea.firstChild);
                }
                bar.classList.remove('jenkins-hidden');
                bar.classList.add('dvf-filter-bar--sticky');
            }
        } else {
            const desktopTable = dashboard.querySelector('.jenkins-mobile-hide');

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
        const storageKey = 'dvf-collapsed-' + position;
        if (localStorage.getItem(storageKey) === 'true' && content && toggle) {
            collapseContent(content, position, bar);
        }

        // Toggle collapse/expand
        if (toggle) {
            toggle.addEventListener('click', function() {
                const isCollapsed = bar.classList.contains('dvf-filter-bar--collapsed');
                if (isCollapsed) {
                    expandContent(content, position, bar);
                    localStorage.setItem(storageKey, 'false');
                } else {
                    collapseContent(content, position, bar);
                    localStorage.setItem(storageKey, 'true');
                }
            });
        }

        // Auto-navigate dropdowns on change (avoids crumb in URL)
        if (form) {
            const selects = form.querySelectorAll('select');
            selects.forEach(function(sel) {
                sel.addEventListener('change', function() {
                    const location = new URL(window.location);
                    selects.forEach(function(s) {
                        location.searchParams.set(s.name, s.value);
                    });
                    window.location.href = location.toString();
                });
            });
        }

        // Clear All button
        const clearBtn = document.getElementById('dvf-clear-all');
        if (clearBtn) {
            clearBtn.addEventListener('click', function() {
                clearBtn.classList.add('dvf-spinning');
                setTimeout(function() { window.location.href = '.'; }, 500);
            });
        }
    }

    function initSidebar(bar, desktopTable, dashboard, tabBar) {
        // Create a flex wrapper for table + sidebar, insert after tab bar
        const wrapper = document.createElement('div');
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
