var easygrid = {

    filterForm: function (gridName, form) {
        var dt = $('#' + gridName + '_datatable').dataTable();
        $.data(dt[0], 'filterParams', jQuery(form).serializeArray());
        dt._fnAjaxUpdate(dt.fnSettings());
        return false;
    },

    initComplete: function (gridName, fixedColumns, noFixedColumns) {
        return function () {
            //hack - removes the filter div
            $('#' + gridName + '_datatable_filter').remove();
            var oSettings = $('#' + gridName + '_datatable').dataTable().fnSettings();
            for (var i = 0; i < oSettings.aoPreSearchCols.length; i++) {
                if (oSettings.aoPreSearchCols[i].sSearch.length > 0) {
                    console.log(oSettings.aoPreSearchCols[i].sSearch);
                    $("tfoot input")[i].value = oSettings.aoPreSearchCols[i].sSearch;
                    $("tfoot input")[i].className = "";
                }
            }
            if (fixedColumns) {
                new FixedColumns(oTable, {
                    "iLeftColumns": noFixedColumns
                });
            }
        }
    },
    serverParams: function (gridName) {
        return function (aoData) {
            var dt = $('#' + gridName + '_datatable').dataTable();
            var params = $.data(dt[0], 'filterParams');
            if(params){
                jQuery.each(params, function () {
                    aoData.push(this);
                });
            }
        }
    }
}