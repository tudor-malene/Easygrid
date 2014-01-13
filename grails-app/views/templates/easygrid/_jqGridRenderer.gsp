<%@page defaultCodec="none" %>
<g:if test="${gridConfig.filterForm}">

    <script type="text/javascript">
    // implementation to work with the dynamic search form
    function filterForm${attrs.id}(form) {
        var ser = jQuery(form).serialize();
        console.log(ser);
        var grid = jQuery("#${attrs.id}_table");
        grid.jqGrid('setGridParam', {postData: ser});
        grid.trigger('reloadGrid');
        return false;
    }
</script>
</g:if>

<jq:jquery>
    jQuery("#${attrs.id}_table").jqGrid(
            {
        datatype: 'json',
        url: '${g.createLink(controller: attrs.controller, action: "${gridConfig.id}Rows", params: params)}',
    <g:each in="${gridConfig.jqgrid}" var="property">
        "${property.key}":${property.value},
    </g:each>
    <g:if test="${gridConfig.childGrid}">
        "onSelectRow":function(id){onSelectGridRowReloadGrid('${gridConfig.childGrid}','${gridConfig.childParamName}',id);},
    </g:if>
    <g:if test="${gridConfig.inlineEdit}">
        editurl: '${g.createLink(controller: attrs.controller, action: "${gridConfig.id}InlineEdit")}',
        cellurl: '${g.createLink(controller: attrs.controller, action: "${gridConfig.id}InlineEdit")}',
    </g:if>
    colNames: [
    <grid:eachColumn gridConfig="${gridConfig}">
        '${g.message(code: col.label, default: col.label)}'<g:if test="${!last}">,</g:if>
    </grid:eachColumn>
    ],
   colModel: [
    <grid:eachColumn gridConfig="${gridConfig}">
        {name:'${col.name}',
        "search":${col.enableFilter},
        <g:each in="${col.jqgrid}">
            "${it.key}":${it.value},
        </g:each>
        },
    </grid:eachColumn>
    ],
   rowNum:${gridConfig.defaultMaxRows},
   viewrecords: true,
    "loadError": function (xhr, status, err) {
        try {
            jQuery.jgrid.info_dialog(jQuery.jgrid.errors.errcap, '<div class="ui-state-error">' + xhr.responseText + '</div>', jQuery.jgrid.edit.bClose, {buttonalign: 'right'});
        } catch (e) {
            alert(xhr.responseText);
        }
     },

   pager: '#${attrs.id}Pager',
    <g:if test="${gridConfig.inlineEdit}">
        onSelectRow: function(id){

            jQuery("#${attrs.id}_table").jqGrid('editRow', id //, true
                , {
                    keys:true,
                    errorfunc:function (rowid, xhr) {
                            try {
                                jQuery.jgrid.info_dialog(jQuery.jgrid.errors.errcap, '<div class="ui-state-error">' + xhr.responseText + '</div>', jQuery.jgrid.edit.bClose, {buttonalign: 'right'});
                            } catch (e) {
                                alert(xhr.responseText);
                            }
                    }
                }
            );
        }
    </g:if>
    });

    <g:if test="${gridConfig.addNavGrid}">
        jQuery('#${attrs.id}_table').jqGrid('navGrid','#${attrs.id}Pager',
        {
            add: false,
            edit:false,
            del: false,
            search: false,
            refresh: true
        })
        <g:if test="${gridConfig.addUrl}">
            .jqGrid('navButtonAdd','#${attrs.id}Pager',{caption:"", buttonicon:"ui-icon-plus", onClickButton:function(){
            document.location = '${gridConfig.addUrl}';
        }});
        </g:if>
        <g:if test="${gridConfig.addFunction}">
            .jqGrid('navButtonAdd','#${attrs.id}Pager',{caption:"", buttonicon:"ui-icon-plus", onClickButton:${gridConfig.addFunction}});
        </g:if>
    </g:if>

    <g:if test="${gridConfig.enableFilter}">
        jQuery('#${attrs.id}_table').jqGrid('filterToolbar',
        {
            autosearch: true,
            searchOnEnter: true
        });
    </g:if>


%{--test if the current grid has a master grid --}%
    <g:if test="${gridConfig.masterGrid}">
    %{--set the on select row of the master grid--}%
        jQuery('#${gridConfig.masterGrid}_table').setGridParam(
            {
                "onSelectRow" : function(rowid,status,e){
                        onSelectGridRowReloadGrid('${attrs.id}_table','${gridConfig.childParamName}',rowid);
                    }
            }
        );

    </g:if>

</jq:jquery>

<table id="${attrs.id}_table"></table>

<div id="${attrs.id}Pager"></div>


