package org.grails.plugin.easygrid.grids

import grails.converters.JSON
import groovy.util.logging.Log4j

import org.springframework.validation.Errors
import org.springframework.validation.ObjectError

import org.grails.plugin.easygrid.Column
import org.grails.plugin.easygrid.EasygridContextHolder

@Log4j
@Mixin(EasygridContextHolder)
class JqueryGridService {

    static transactional = false

    def easygridService
    def grailsApplication

    /**
     * @param defaultValues
     * @return
     */
    def addDefaultValues(Map defaultValues) {

        if (!gridConfig.jqgrid) {
            gridConfig.jqgrid = [:]
        }

        // set the default max rows
        if (gridConfig.defaultMaxRows) {
            gridConfig.jqgrid.rowNum = gridConfig.defaultMaxRows
        }


        gridConfig.columns.each {Column column ->
            if (column?.jqgrid?.name == null) {
                column.jqgrid ?: (column.jqgrid = [:])
                assert column.property
                column.jqgrid.name = column.property
            }
        }
    }


    def filters() {

        if (params._search) {
            // determine if there is a search
            def searchParam = params.keySet().intersect(gridConfig.columns.collect {it.jqgrid.name })

            // determine the search closure from the config
//            searchParam ? (gridConfig.columns.find {it.jqgrid.name == searchParam}?.jqgrid?.search) : null
            searchParam.inject([]) {list, param ->
                def closure = gridConfig.columns.find {col -> col.jqgrid.name == param}?.jqgrid?.searchClosure
                closure ? (list + closure) : list
            }

            //todo - implement dynamic search: searchOper
        }
    }


    def listParams() {
        def currentPage = params.page ? (params.page as int) : 1
        def maxRows = params.rows ? (params.rows as int) : grailsApplication.config?.easygrid?.defaults?.defaultMaxRows
        def offset = (currentPage - 1) * maxRows

        def sort = (params.sidx) ? params.sidx : null
        def order = sort ? params.sord : null

        [rowOffset: offset, maxRows: maxRows, sort: sort, order: order]
    }

    def transform(rows, nrRecords, listParams) {
        // transform the list of elements to a jqGrid format
        def results = rows.collect { element ->
            def cell = []
            gridConfig.columns.eachWithIndex {  column, row ->
                cell.add easygridService.valueOfColumn(column, element, row + 1)
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
    def inlineEdit() {

        //the closure that will handle the operation
        def oper

//        assert gridConfig.type == 'domain'

        switch (params.oper) {
            case 'add':
                oper = gridConfig.save ?: easygridService.dataSourceService.saveRow
                break;
            case 'edit':
                oper = gridConfig.update ?: easygridService.dataSourceService.updateRow
                break;
            case 'del':
                oper = gridConfig.del ?: easygridService.dataSourceService.delRow
                break;
            default:
                throw new RuntimeException("unknown oper: ${params.oper}")
        }

        // the closure returns null if success and an error message or an instance of errors in case of failure
        //should return an instance of Errors
//        def result = oper.call()
        def result = easygridService.guard(gridConfig, params.oper, oper)

        if (result != null) {
            if (Errors.isAssignableFrom(result.class)) {
                Errors errors = result
                if (errors.hasErrors()) {
                    def err = []
                    errors.allErrors.each {ObjectError objectError ->
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
