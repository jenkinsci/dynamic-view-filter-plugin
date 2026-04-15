Behaviour.specify('.dropdown-source-type', 'DropdownDefinition-config', 0, function(sel) {
    function toggleFields() {
        var chunk = sel.closest('.repeated-chunk') || sel.closest('.dd-job-regex-field').parentNode;
        if (!chunk) return;
        var regexFields = chunk.querySelectorAll('.dd-job-regex-field');
        var paramFields = chunk.querySelectorAll('.dd-build-param-field');
        var val = sel.value;
        for (var i = 0; i < regexFields.length; i++) {
            regexFields[i].style.display = val === 'jobNameRegex' ? '' : 'none';
        }
        for (var i = 0; i < paramFields.length; i++) {
            paramFields[i].style.display = val === 'buildParameter' ? '' : 'none';
        }
    }
    sel.addEventListener('change', toggleFields);
    toggleFields();
});
