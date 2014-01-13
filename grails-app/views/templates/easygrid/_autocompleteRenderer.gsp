<%@page defaultCodec="none" %>
<g:hiddenField id="${attrs.id}" name="${attrs.name}" value="${attrs.value}"/>

<script type="text/javascript">
    $(function() {
        $( "#${attrs.id}" ).selectionComp({
            urlAjaxAutocomp: "${createLink(controller: attrs.controller, action: "${attrs.gridName}AutocompleteResult")}",
            urlAjaxSelLabel: "${createLink(controller: attrs.controller, action: "${attrs.gridName}SelectionLabel")}",
            urlAjaxGrid:    "${createLink(controller: attrs.controller, action: "${attrs.gridName}Html")}",
            gridName: "${attrs.gridName}" ,
            selButton: "<a href='#'>Sel    <a>",
            labelElement: "<div style='display:inline;'/>",
            showSeparateLabel: ${attrs.showSeparateLabel},
            showAutocompleteBox: ${attrs.showAutocompleteBox},
            autocompleteSize: ${attrs.autocompleteSize},
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
            title: '${g.message(code: attrs.title)}' ,
            width: '${attrs.width}',
            height: '${attrs.height}'
        });
        $( "#${attrs.id}" ).selectionComp("addElements");
        // on editing - set the label value
        if($('#${attrs.id}').val()){
            $( "#${attrs.id}" ).selectionComp('setLabel', $('#${attrs.id}').val());
        }
    });
</script>
