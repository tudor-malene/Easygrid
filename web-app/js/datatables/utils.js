var easygrid = {

    filterForm: function (gridName, form) {
        var dt = jQuery('#' + gridName + '_datatable').dataTable();
        jQuery.data(dt[0], 'filterParams', jQuery(form).serializeArray());
        dt._fnAjaxUpdate(dt.fnSettings());
        return false;
    },

    initComplete: function (gridName, fixedColumns, noFixedColumns, hideSearch) {
        return function () {
            if(hideSearch){
                //hack - removes the filter div
                jQuery('#' + gridName + '_datatable_filter').remove();
                var oSettings = jQuery('#' + gridName + '_datatable').dataTable().fnSettings();
                for (var i = 0; i < oSettings.aoPreSearchCols.length; i++) {
                    if (oSettings.aoPreSearchCols[i].sSearch.length > 0) {
                        console.log(oSettings.aoPreSearchCols[i].sSearch);
                        jQuery("tfoot input")[i].value = oSettings.aoPreSearchCols[i].sSearch;
                        jQuery("tfoot input")[i].className = "";
                    }
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
            var dt = jQuery('#' + gridName + '_datatable').dataTable();
            var params = jQuery.data(dt[0], 'filterParams');
            if(params){
                jQuery.each(params, function () {
                    aoData.push(this);
                });
            }
        }
    }
}