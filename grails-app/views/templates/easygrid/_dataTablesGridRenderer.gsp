<%@page defaultCodec="none" %>
<script type="text/javascript">

    var ${attrs.id}FilterParams ;

<g:if test="${gridConfig.filterForm}">
    // implementation to work with the dynamic search form
    function filterForm${attrs.id}(form){
        ${attrs.id}FilterParams  = jQuery(form).serializeArray();
        var dt = $('#${attrs.id}_datatable').dataTable();
        dt._fnAjaxUpdate(dt.fnSettings());
        return false;
    }
    </g:if>

jQuery(function () {

    var oTable = $('#${attrs.id}_datatable').dataTable({

        <g:each in="${gridConfig.dataTables}" var="property">
        "${property.key}":${property.value},
        </g:each>

        bFilter: true,
        "bStateSave": false,
        'sPaginationType': 'full_numbers',
        "bSort": true,
        "bProcessing": true,
        "bServerSide": true,

        "sAjaxSource": '${g.createLink(controller: attrs.controller, action: "${gridConfig.id}Rows", params: params)}',

        "fnInitComplete": function () {
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
            <g:if test="${gridConfig.fixedColumns == 'true'}">
            new FixedColumns(oTable, {
                "iLeftColumns": ${gridConfig.noFixedColumns}
//                "iLeftWidth": 350
            });
            </g:if>

        },
        "fnRowCallback": function (nRow, aData, iDisplayIndex, iDisplayIndexFull) {
        },
        "fnServerParams": function ( aoData ) {
            if(${attrs.id}FilterParams){
                jQuery.each(${attrs.id}FilterParams, function() {
                    aoData.push(this);
                });
            }
        },

        "aoColumns": [
            <g:each in="${gridConfig.columns}" var="col" status="idx">
            {   "sName": "${col.name}",
                "bSearchable": ${col.enableFilter},
                "bSortable": ${col.sortable},
                <g:each in="${col.dataTables}" var="property">
                "${property.key}":${property.value},
                </g:each>
                "bVisible": true
                %{--"sWidth": "${col.dataTables.sWidth}",--}%
                %{--"sClass": "${col.dataTables.sClass}"--}%
            }  <g:if test="${idx < gridConfig.columns.size() - 1}">,</g:if>
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
            <th>${g.message(code: col.label, default: col.label)}</th>
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
            <td>
                <g:if test="${(gridConfig.fixedColumns != 'true') &&gridConfig.enableFilter && col.enableFilter}">
                %{--todo - add variables--}%
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

