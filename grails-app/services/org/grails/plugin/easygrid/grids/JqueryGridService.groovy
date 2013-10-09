package org.grails.plugin.easygrid.grids

import grails.converters.JSON
import groovy.util.logging.Slf4j
import org.grails.plugin.easygrid.Filter
import org.grails.plugin.easygrid.GridConfig
import org.grails.plugin.easygrid.GridUtils
import org.springframework.validation.Errors
import org.springframework.validation.ObjectError

import static org.grails.plugin.easygrid.EasygridContextHolder.getParams

@Slf4j
class JqueryGridService {

    static transactional = false

    def grailsApplication
    def easygridDispatchService


    def filters(gridConfig) {

        if (params._search) {
            // determine if there is a search
            def searchParams = params.keySet().intersect(gridConfig.columns.collect { it.name })

            // determine the search closure from the config
//            searchParam ? (gridConfig.columns.find {it.jqgrid.name == searchParam}?.jqgrid?.search) : null
            searchParams.inject([]) { list, param ->
//                def closure = gridConfig.columns.find { col -> col.name == param }?.filterClosure
//                def column = gridConfig.columns.find { col -> col.name == param }
                def column = gridConfig.columns[param]
                column?.filterClosure ? (list + new Filter(column)) : list
            }

            //todo - implement dynamic search: searchOper
        }
    }


    def listParams(gridConfig) {
        def currentPage = params.page ? (params.page as int) : 1
        def maxRows = params.rows ? (params.rows as int) : grailsApplication.config?.easygrid?.defaults?.defaultMaxRows
        def offset = (currentPage - 1) * maxRows

        def sort = (params.sidx) ? params.sidx : null
        def order = sort ? params.sord : null

        [rowOffset: offset, maxRows: maxRows, sort: sort, order: order]
    }

    def transform(gridConfig, rows, nrRecords, listParams) {
        // transform the list of elements to a jqGrid format
        def results = rows.collect { element ->
            def cell = []
/*
            gridConfig.columns.eachWithIndex {  column, row ->
                cell.add GridUtils.valueOfColumn(column, element, row + 1)
            }
*/
//            gridConfig.columns.findAll {col -> (params.selectionComp) ? col.showInSelection : true}.eachWithIndex { column, row ->
            GridUtils.eachColumn(gridConfig) { column, row ->
                cell.add GridUtils.valueOfColumn(gridConfig, column, element, row + 1)
            }

            [id: element.id, cell: cell]
        }

        [rows: results, page: 1 + (listParams.rowOffset / listParams.maxRows as int), records: nrRecords, total: Math.ceil(nrRecords / listParams.maxRows) as int] as JSON
    }

    ///////////////////////////// INLINE EDIT /////////////////

    /**
     * dispatches the Edit operation
     * @param gridConfig
     */
    def inlineEdit(GridConfig gridConfig) {

        //the closure that will handle the operation
        def result

        switch (params.oper) {
            case 'add':
//                result = gridConfig.saveRowClosure ? gridConfig.saveRowClosure() : gridConfig.callGridPropertyMethod('dataSourceService', 'saveRow')
                result = gridConfig.saveRowClosure ? gridConfig.saveRowClosure() : easygridDispatchService.callDSSaveRow(gridConfig)
                break
            case 'edit':
//                result = gridConfig.updateRowClosure ? gridConfig.updateRowClosure() : gridConfig.callGridPropertyMethod('dataSourceService', 'updateRow')
                result = gridConfig.updateRowClosure ? gridConfig.updateRowClosure() : easygridDispatchService.callDSUpdateRow(gridConfig)
                break
            case 'del':
//                result = gridConfig.delRowClosure ? gridConfig.delRowClosure() : gridConfig.callGridPropertyMethod('dataSourceService', 'delRow')
                result = gridConfig.delRowClosure ? gridConfig.delRowClosure() : easygridDispatchService.callDSDelRow(gridConfig)
                break
            default:
                throw new RuntimeException("unknown oper: ${params.oper}")
        }

        // the closure returns null if success and an error message or an instance of errors in case of failure
        //should return an instance of Errors
//        def result = oper.call()
//        def result = easygridService.guard(gridConfig, params.oper, oper)
//        def result = oper()

        if (result != null) {
            if (Errors.isAssignableFrom(result.getClass())) {
                Errors errors = result
                if (errors.hasErrors()) {
                    def err = []
                    errors.allErrors.each { ObjectError objectError ->
                        err << objectError.code
                    }
                    return err
                }
            } else {
                return [result]
            }
        }
    }
}
