$.extend($.jgrid.inlineEdit, { restoreAfterError: false });

var easygrid = {

    /**
     * function to be called when on selection of a row in a grid
     * todo -should be renamed to: onSelectRowReloadChildGrid
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
            $.ajax({
                url: baseUrl,
                data: {
                    id: row_id,
                    gridId: subgrid_id + "_row" + row_id
                },
                dataType: "html",
                success: function (data) {
                    jQuery("#" + subgrid_id).html(data);
                }
            });
        }
    },

    onSelectRowInlineEdit: function (gridName) {
        return function (id) {
            var myGrid = jQuery("#" + gridName);
            myGrid.jqGrid('editRow', id //, true
                , {
                    keys: true,
                    aftersavefunc: easygrid.afterSave(gridName),
                    errorfunc: easygrid.onError(gridName)
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
    },

    afterSave: function (gridName) {
        var myGrid = jQuery("#" + gridName);
        return function (id, xhr) {
            var response = jQuery.parseJSON(xhr.responseText);
            if (response.hasOwnProperty('version')) {
                var rowData = myGrid.jqGrid('getRowData', id);
                rowData.version = response.version;
                myGrid.jqGrid('setRowData', id, rowData);
            }
            if (response.hasOwnProperty('id')) {
                var element = $('#' + id)
                element.attr('id', response.id)
            }
            return true;
        }
    },

    onError: function (gridName) {
        var myGrid = jQuery("#" + gridName);
        return function (rowid, xhr) {
            var response = jQuery.parseJSON(xhr.responseText);
            if (response.message != undefined) {
                jQuery.jgrid.info_dialog(jQuery.jgrid.errors.errcap, '<div class="ui-state-error">' + response.message + '</div>', jQuery.jgrid.edit.bClose, {buttonalign: 'right'});
            }
            var row = $(myGrid.jqGrid('getRowData', rowid));
            var columns = myGrid.jqGrid('getGridParam', 'colModel');
            for (var i = 0; i < columns.length; i++) {
                var col = columns[i];
                var element = $('#' + rowid + '_' + col.name);
                element.removeClass("ui-state-error");
            }

            for (var key in response.fields) {
                var element = $('#' + rowid + '_' + key);
                element.parent().attr("title", response.fields[key]);
                element.addClass("ui-state-error");
            }
        }
    },
    /*
     onEdit: function (gridName) {
     },
     onSuccess: function (gridName) {
     },
     */

    afterSubmit: function (gridName) {
        var myGrid = jQuery("#" + gridName);
        return function (xhr, postdata) {
            var id = null;
            var response = jQuery.parseJSON(xhr.responseText);
            if (response.hasOwnProperty('id')) {
                id = response.id;
            }

            return [true, '', id ];
        }
    },
    errorTextFormat: function (gridName) {
        var myGrid = jQuery("#" + gridName);
        return function (xhr) {
            var response = jQuery.parseJSON(xhr.responseText);
            console.log(response);
            var columns = myGrid.jqGrid('getGridParam', 'colModel');
            for (var i = 0; i < columns.length; i++) {
                var col = columns[i];
                var element = $('#' + col.name);
                element.removeClass("ui-state-error");
            }
            for (var key in response.fields) {
                var element = $('#' + key);
                element.parent().attr("title", response.fields[key]);
                element.addClass("ui-state-error");
            }
            if (response.message == undefined) {
                return 'Invalid values';
            }
            return response.message;
        }
    }

};