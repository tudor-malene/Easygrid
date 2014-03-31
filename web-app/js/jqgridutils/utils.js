var easygrid = {

    /**
     * function to be called when on selection of a row in a grid
     * @param childGridName - the child grid to be reloaded
     * @param childParamName - the child param name
     */
    onSelectGridRowReloadGrid: function (childGridName, childParamName) {
        return function (id) {
            var grid = jQuery("#" + childGridName);
            var params = {'_search': true};
            params[childParamName] = id;
            grid.jqGrid('setGridParam', {postData: params});
            grid.trigger('reloadGrid');
            var onselrow = grid.jqGrid('getGridParam', 'onSelectRow');
            if (onselrow) {
                onselrow.call(grid, -1);
            }
        }
    },

    filterForm: function (gridName, form) {
        var ser = jQuery(form).serialize();
        console.log(ser);
        var grid = jQuery("#" + gridName);
        grid.jqGrid('setGridParam', {postData: ser});
        grid.trigger('reloadGrid');
        return false;
    },

    subGridRowExpanded: function (baseUrl) {
        return function (subgrid_id, row_id) {
            var url = baseUrl + "?id=" + row_id + "&gridId=row" + row_id;
            console.log(url);
            $.ajax({
                url: url,
                dataType: "html",
                success: function (data) {
                    jQuery("#" + subgrid_id).html(data);
                }
            });
        }
    },

    onSelectRowInlineEdit: function (gridName) {
        return function (id) {
            jQuery("#" + gridName).jqGrid('editRow', id //, true
                , {
                    keys: true,
                    errorfunc: function (rowid, xhr) {
                        try {
                            jQuery.jgrid.info_dialog(jQuery.jgrid.errors.errcap, '<div class="ui-state-error">' + xhr.responseText + '</div>', jQuery.jgrid.edit.bClose, {buttonalign: 'right'});
                        } catch (e) {
                            alert(xhr.responseText);
                        }
                    }
                }
            );
        }
    },

    loadError: function (xhr, status, err) {
        try {
            jQuery.jgrid.info_dialog(jQuery.jgrid.errors.errcap, '<div class="ui-state-error">' + xhr.responseText + '</div>', jQuery.jgrid.edit.bClose, {buttonalign: 'right'});
        } catch (e) {
            alert(xhr.responseText);
        }
    }

}