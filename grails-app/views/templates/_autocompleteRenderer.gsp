<g:hiddenField id="${attrs.id}" name="${attrs.name}" value="${attrs.value}"/>

<script type="text/javascript">
    $(function() {
        // initialize with two customized options
        $( "#${attrs.id}" ).selectionComp({
            urlAjaxAutocomp: "${createLink(controller: attrs.controller, action: "${attrs.gridName}AutocompleteResult")}",
            urlAjaxSelLabel: "${createLink(controller: attrs.controller, action: "${attrs.gridName}SelectionLabel")}",
            urlAjaxGrid:    "${createLink(controller: attrs.controller, action: "${attrs.gridName}Html")}",
            gridName: "${attrs.gridName}" ,
            showAutocompleteBox: ${attrs.showAutocompleteBox},
            staticConstraints: {
                <g:each in="${attrs.staticConstraints}" >
                    "${it.key}": "${it.value}",
                </g:each>
            },
            dynamicConstraints: {
                <g:each in="${attrs.dynamicConstraints}">
                    "${it.key}": "${it.value}",
                </g:each>
            } ,
            change: function(){
                ${attrs.onChange}
            },
            title: '${message(code: attrs.title)}' ,
            width: '${attrs.width}',
            height: '${attrs.height}'
        });

        if($('#${attrs.id}').val()){
            $( "#${attrs.id}" ).selectionComp('setLabel', $('#${attrs.id}').val());
        }
    });
</script>
