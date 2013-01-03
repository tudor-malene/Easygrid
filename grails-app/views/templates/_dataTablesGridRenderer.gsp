<script type="text/javascript">jQuery(function () {

    %{--todo - move some hardcodings to the config --}%
    var oTable = $('#${attrs.id}_datatable').dataTable({
        bFilter:true,
        "bStateSave":false,
        'sPaginationType':'full_numbers',
        "fnInitComplete":function () {
            //hack - removes the filter div
            $('#${attrs.id}_datatable_filter').remove();
            var oSettings = $('#${attrs.id}_datatable').dataTable().fnSettings();
            for (var i = 0; i < oSettings.aoPreSearchCols.length; i++) {
                if (oSettings.aoPreSearchCols[i].sSearch.length > 0) {
                    console.log(oSettings.aoPreSearchCols[i].sSearch);
                    $("tfoot input")[i].value = oSettings.aoPreSearchCols[i].sSearch;
                    $("tfoot input")[i].className = "";
                }
            }
        },
        "bSort":true,
        "bProcessing":true,
        "bServerSide":true,
        "sAjaxSource":"${g.createLink(action: "${gridConfig.id}Rows")}",
        "aoColumns":[
            <g:each in="${gridConfig.columns}" var="col" status="idx">
            { "sName":"${col.name}", "bSortable":true }  <g:if test="${idx < gridConfig.columns.size() - 1}">,
            </g:if>
            </g:each>
        ]

    });

    /* Add the events etc before DataTables hides a column */
    $("tfoot input").keyup(function () {
        /* Filter on the column (the index) of this element */
        oTable.fnFilter(this.value, oTable.oApi._fnVisibleToColumnIndex(oTable.fnSettings(), $("tfoot input").index(this)));
    });

    /*
     * Support functions to provide a little bit of 'user friendlyness' to the textboxes
     */
    $("tfoot input").each(function (i) {
        this.initVal = this.value;
    });

    $("tfoot input").focus(function () {
        if (this.className == "search_init") {
            this.className = "";
            this.value = "";
        }
    });

    $("tfoot input").blur(function (i) {
        if (this.value == "") {
            this.className = "search_init";
            this.value = this.initVal;
        }
    });

});
</script>


<table id="${attrs.id}_datatable" cellpadding="0" cellspacing="0" border="0"
       class="display">%{--width="${gridConfig.datatable.width}">--}%
    <thead>
    <tr>
        <g:each in="${gridConfig.columns}" var="col">
            <th %{--width="${col.datatable.width}"--}%>${message(code: col.label, default: col.label)}</th>
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
            <td>%{--width="${col.datatable.width}">--}%
                <g:if test="${col.enableFilter}">
                    <input type="text" name="search_${col.name}" class="search_init" size="10"/>
                </g:if>
                <g:else>
                    &nbsp;
                </g:else>
            </td>
        </g:each>
    </tr>
    </tfoot>
</table>

