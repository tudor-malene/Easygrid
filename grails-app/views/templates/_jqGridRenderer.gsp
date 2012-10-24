<jq:jquery>

    jQuery("#${gridConfig.id}").jqGrid(
            {
        datatype: 'json',
        url: '${g.createLink(action: "${gridConfig.id}Rows")}',
    <g:each in="${gridConfig.jqgrid}" var="property">
        "${property.key}":${property.value},
    </g:each>
    <g:if test="${gridConfig.inlineEdit}">
        editurl: '${g.createLink(action: "${gridConfig.id}InlineEdit")}',
        cellurl: '${g.createLink(action: "${gridConfig.id}InlineEdit")}',
    </g:if>
    colNames: [
    <g:each in="${gridConfig.columns}" var="col" status="idx">
        '${message(code: col.label, default: col.label)}'<g:if test="${idx < gridConfig.columns.size() - 1}">,</g:if>
    </g:each>
    ],
   colModel: [
    <g:each in="${gridConfig.columns}" var="column">
        {name:'${column.jqgrid.name}',
        <g:findAll in="${column.jqgrid}" expr="${!(it.key in ['name', 'searchClosure'])}">
            "${it.key}":${it.value},
        </g:findAll>
        },
    </g:each>
    ],
   viewrecords: true,
   pager: '#${gridConfig.id}Pager',
    <g:if test="${gridConfig.inlineEdit}">
        onSelectRow: function(id){
        jQuery("#${gridConfig.id}").jqGrid('editRow', id //, true
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

    jQuery('#${gridConfig.id}').navGrid('#${gridConfig.id}Pager',
        {
            add: false,
            edit:false,
            del: false,
            search: false,
            refresh: true
        });


    jQuery('#${gridConfig.id}').filterToolbar(
        {
            autosearch: true,
            searchOnEnter: true
        });

</jq:jquery>

<table id="${gridConfig.id}"></table>

<div id="${gridConfig.id}Pager"></div>

