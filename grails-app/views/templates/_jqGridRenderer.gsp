<jq:jquery>
    jQuery("#${gridConfig.id}_table").jqGrid(
            {
        datatype: 'json',
        url: '${g.createLink(action: "${gridConfig.id}Rows", params: params)}',
    <g:each in="${gridConfig.jqgrid}" var="property">
        "${property.key}":${property.value},
    </g:each>
    <g:if test="${gridConfig.inlineEdit}">
        editurl: '${g.createLink(action: "${gridConfig.id}InlineEdit")}',
        cellurl: '${g.createLink(action: "${gridConfig.id}InlineEdit")}',
    </g:if>
    colNames: [
    <grid:eachColumn gridConfig="${gridConfig}" >
        '${message(code: col.label, default: col.label)}'<g:if test="${!last}">,</g:if>
    </grid:eachColumn>
    ],
   colModel: [
    <grid:eachColumn gridConfig="${gridConfig}" >
        {name:'${col.name}',
        <g:each in="${col.jqgrid}" >
            "${it.key}":${it.value},
        </g:each>
        },
    </grid:eachColumn>
    ],
   viewrecords: true,
   pager: '#${gridConfig.id}Pager',
    <g:if test="${gridConfig.inlineEdit}">
        onSelectRow: function(id){

            jQuery("#${gridConfig.id}_table").jqGrid('editRow', id //, true
                , {
                    keys:true,
                    aftersavefunc:function (rowid, response) {
                    //todo
                    }
                }
            );
        }
    </g:if>
    });

    jQuery('#${gridConfig.id}_table').navGrid('#${gridConfig.id}Pager',
        {
            add: false,
            edit:false,
            del: false,
            search: false,
            refresh: true
        });


    jQuery('#${gridConfig.id}_table').filterToolbar(
        {
            autosearch: true,
            searchOnEnter: true
        });

</jq:jquery>

<table id="${gridConfig.id}_table"></table>
<div id="${gridConfig.id}Pager"></div>

