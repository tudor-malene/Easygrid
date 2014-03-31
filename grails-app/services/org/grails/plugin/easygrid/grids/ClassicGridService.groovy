package org.grails.plugin.easygrid.grids

import groovy.util.logging.Slf4j
import org.grails.plugin.easygrid.EasygridContextHolder
import org.grails.plugin.easygrid.GridConfig
import org.grails.plugin.easygrid.GridUtils
import static org.grails.plugin.easygrid.EasygridContextHolder.*

/**
 * implements the classic grails grid
 * ( just for demo )
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
class ClassicGridService {

    static transactional = false

    def grailsApplication
    def easygridService

    def htmlGridDefinition(gridConfig) {
        [gridConfig: gridConfig, rows: easygridService.gridData(gridConfig)]
    }

    def filters(gridConfig) {
        null
        //todo
    }

    def listParams(GridConfig gridConfig) {
        gridConfig
        def maxRows = params.max ? (params.max as int) : grailsApplication.config?.easygrid?.defaults?.defaultMaxRows
//        def currentPage = 1 + (params.offset ? (params.offset as int) : 0) / maxRows
        def sort = params.sort
        def order = params.order
        [rowOffset: params.offset ? params.offset as int : 0, maxRows: maxRows, sort: sort, order: order]
    }

    def transform(gridConfig, rows, nrRecords, listParams) {
        def results = []
        rows.each { element ->
            def row = [:]
            gridConfig.columns.eachWithIndex { col, idx ->
                row[col] = GridUtils.valueOfColumn(gridConfig, col, element, idx + 1)
            }
            results << row
        }

        [rows: results, page: listParams.currentPage, records: nrRecords, total: Math.ceil(nrRecords / listParams.maxRows) as int]
    }

/*
    def gridData(gridConfig, params, session) {
        assert gridConfig != null

//        Map restoredParams = easygridService.restoreSearchParams(session, params, gridConfig.name)
        Map restoredParams = params

        // determine if there is a search   - toddo - not yet
//        def searchParam = restoredParams.keySet().intersect(gridConfig.columns.collect {it.jqgrid.name }).find {1}

        // determine the search closure from the config
//        Closure filterClosure = searchParam ? (gridConfig.columns.find {it.jqgrid.name == searchParam}?.jqgrid?.search) : null
        Closure filterClosure = null

        def maxRows = restoredParams.max ? (restoredParams.max as int) : grailsApplication.config?.easygrid?.defaults?.defaultMaxRows
        assert maxRows
        def currentPage = 1 + (restoredParams.offset ? (restoredParams.offset as int) : 0) / maxRows

        def sort = params.sort
        def order = params.order

        def nrRecords = easygridService.countRows(gridConfig, restoredParams, filterClosure)
        def rows = easygridService.list(gridConfig, restoredParams, currentPage, maxRows, filterClosure, sort, order)

        // transform the list of elements to a jqGrid format - NOT
        def results = []
        rows.each { element ->
            def row = [:]
            gridConfig.columns.eachWithIndex { Map col, idx ->
                row[col] = GridUtils.valueOfColumn(col, element, params, idx + 1)
            }
            results << row
        }

        [rows: results, page: currentPage, records: nrRecords, total: Math.ceil(nrRecords / maxRows) as int]
    }
*/
}
