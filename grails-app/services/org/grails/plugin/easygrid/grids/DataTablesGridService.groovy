package org.grails.plugin.easygrid.grids

import grails.converters.JSON
import org.grails.plugin.easygrid.ColumnConfig
import org.grails.plugin.easygrid.EasygridContextHolder
import org.grails.plugin.easygrid.Filter
import org.grails.plugin.easygrid.Filters
import org.grails.plugin.easygrid.GridUtils
import static org.grails.plugin.easygrid.EasygridContextHolder.*

/**
 * implementation for Datatable
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class DataTablesGridService {

    static transactional = false

    def easygridService
    def grailsApplication


    def addDefaultValues( gridConfig, defaultValues) {
        if (gridConfig.hideSearch == null) {
            //by default hide the search field
            gridConfig.hideSearch = true
        }
    }

/*
int	iDisplayStart	Display start point in the current data set.
int	iDisplayLength	Number of records that the table can display in the current draw. It is expected that the number of records returned will be equal to this number, unless the server has fewer records to return.
int	iColumns	Number of columns being displayed (useful for getting individual column search info)
string	sSearch	Global search field
bool	bRegex	True if the global filter should be treated as a regular expression for advanced filtering, false if not.
bool	bSearchable_(int)	Indicator for if a column is flagged as searchable or not on the client-side
string	sSearch_(int)	Individual column filter
bool	bRegex_(int)	True if the individual column filter should be treated as a regular expression for advanced filtering, false if not
bool	bSortable_(int)	Indicator for if a column is flagged as sortable or not on the client-side
int	iSortingCols	Number of columns to sort on
int	iSortCol_(int)	Column being sorted on (you will need to decode this number for your database)
string	sSortDir_(int)	Direction to be sorted - "desc" or "asc".
string	mDataProp_(int)	The value specified by mDataProp for each column. This can be useful for ensuring that the processing of data is independent from the order of the columns.
string	sEcho	Information for DataTables to use for rendering.
*/

    def filterService

    def filters(gridConfig) {
        def filters = []
        gridConfig.columns.findAll { it.enableFilter }.eachWithIndex { col, i ->
            if (params["bSearchable_$i"] && params["sSearch_$i"]) {
                def val = params["sSearch_$i"]
//                params["${col.name}"] = val
//                filterClosures.add new Filter(searchFilter: col?.filterClosure, paramName: "${col.name}", paramValue: val, column: col)
                filters << filterService.createFilterFromColumn(gridConfig, col, null, val)
            }
        }
        filters
    }


    def listParams(gridConfig) {
        def maxRows = params.iDisplayLength ? (params.iDisplayLength as int) : grailsApplication.config?.easygrid?.defaults?.defaultMaxRows
        assert maxRows
//        def currentPage = 1 + (params.iDisplayStart ? (params.iDisplayStart as int) : 0) / maxRows


        def orderMap = [:]
        if (params.iSortCol_0 != null) {

            (0..(params.iSortingCols as int) - 1).each {
                def sortCol = params["iSortCol_$it"] as int
//                println params["bSortable_${sortCol}"]
                if (params["bSortable_${sortCol}"]) {
                    orderMap[gridConfig.columns[sortCol]] = params["sSortDir_$it"] ?: 'asc'
                }
            }
        }

        //for now only single sorting
        def sort = null
        def order = null
        orderMap.find { 1 }.each {
            order = it.value
            sort = it.key.name
        }

        [rowOffset: params.iDisplayStart ? params.iDisplayStart as int : 0, maxRows: maxRows, sort: sort, order: order]
    }

/*
int	iTotalRecords	Total records, before filtering (i.e. the total number of records in the database)
int	iTotalDisplayRecords	Total records, after filtering (i.e. the total number of records after filtering has been applied - not just the number of records being returned in this result set)
string	sEcho	An unaltered copy of sEcho sent from the client side. This parameter will change with each draw (it is basically a draw count) - so it is important that this is implemented. Note that it strongly recommended for security reasons that you 'cast' this parameter to an integer in order to prevent Cross Site Scripting (XSS) attacks.
string	sColumns	Optional - this is a string of column names, comma separated (used in combination with sName) which will allow DataTables to reorder data on the client-side if required for display. Note that the number of column names returned must exactly match the number of columns in the table. For a more flexible JSON format, please consider using mDataProp.
array	aaData	The data in a 2D array. Note that you can change the name of this parameter with sAjaxDataProp.
*/

    def transform(gridConfig, rows, nrRecords, listParams) {
        [
                sEcho               : params.sEcho ?: '-1' as int,
                iTotalRecords       : nrRecords,
                iTotalDisplayRecords: nrRecords,
//                sColumns: [], - todo -

                aaData              : rows.collect { element ->
                    def cell = []
                    gridConfig.columns.eachWithIndex { col, row ->
                        cell.add GridUtils.valueOfColumn(gridConfig, col, element, row + 1)
                    }
                    cell
                }
        ] as JSON
    }
}
