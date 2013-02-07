/**
 * function to be called when on selection of a row in a grid
 * @param childGridName - the child grid to be reloaded
 * @param childParamName - the child param name
 * @param rowId
 */
function onSelectGridRowReloadGrid(childGridName, childParamName, rowId) {
    var grid = jQuery("#" + childGridName );
    var params = {'_search': true} ;
    params[childParamName]= rowId;
    grid.jqGrid('setGridParam', {postData: params});
    grid.trigger('reloadGrid');
    grid.jqGrid('getGridParam','onSelectRow').call(grid,-1);
}

