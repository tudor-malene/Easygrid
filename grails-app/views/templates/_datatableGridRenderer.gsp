<jq:jquery>
    $('#${gridConfig.id}').dataTable( {
       "bProcessing": true,
       "bServerSide": true,
       "sAjaxSource": "${g.createLink(action: "${gridConfig.id}Rows")}"
   } );
</jq:jquery>

<table cellpadding="0" cellspacing="0" border="0" class="display" id="${gridConfig.id}" width="${gridConfig.datatable.width}">
    <thead>
    <tr>
        <g:each in="${gridConfig.columns}" var="col">
            <th width="${col.datatable.width}">${message(code: col.label, default: col.label)}</th>
        </g:each>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td colspan="${gridConfig.columns.size()}" class="dataTables_empty">Loading data from server</td>
    </tr>
    </tbody>
    <tfoot>
    <tr>
        <g:each in="${gridConfig.columns}" var="col">
            <th width="${col.datatable.width}">${message(code: col.label, default: col.label)}</th>
        </g:each>
    </tr>
    </tfoot>
</table>

