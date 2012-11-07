%{--//todo - de pus numele calumea --}%
<g:hiddenField id="${gridConfig.id}" name="${gridConfig.id}.id" value="${attrs.idValue}"/>

<script type="text/javascript">
    $(function() {
        // initialize with two customized options
        $( "#${gridConfig.id}" ).selectionComp({
            url_ajax_autocomp: "${createLink(controller: attrs.controller, action: "${gridConfig.id}AutocompleteResult")}",
            url_ajax_selLabel: "${createLink(controller: attrs.controller, action: "${gridConfig.id}SelectionLabel")}",
            url_ajax_tabel:    "${createLink(controller: attrs.controller, action: "${gridConfig.id}Html", params: [selectionComp:true])}",
            gridName: "${gridConfig.id}"    ,
            showAutocompleteBox: true
        });
    });
</script>