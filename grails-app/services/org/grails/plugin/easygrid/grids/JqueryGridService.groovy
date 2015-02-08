package org.grails.plugin.easygrid.grids

import grails.converters.JSON
import groovy.util.logging.Slf4j
import org.grails.plugin.easygrid.ColumnConfig
import org.grails.plugin.easygrid.GridConfig
import org.grails.plugin.easygrid.GridUtils
import org.grails.plugin.easygrid.InlineResponse
import org.grails.plugin.easygrid.JsUtils
import org.springframework.http.HttpStatus

import static org.grails.plugin.easygrid.EasygridContextHolder.getParams
import static org.grails.plugin.easygrid.EasygridContextHolder.messageLabel
import static org.grails.plugin.easygrid.EasygridContextHolder.errorLabel

@Slf4j
class JqueryGridService {

    static transactional = false

    def grailsApplication
    def easygridDispatchService

    def jqGridMultiSearchService
    def filterService


    def addDefaultValues(GridConfig gridConfig, defaultValues) {
        if (gridConfig.enableFilter) {
            gridConfig.columns.each { ColumnConfig columnConfig ->
                if (columnConfig.enableFilter && columnConfig?.filterType) {
                    List operators = grailsApplication.config.easygrid.columns.defaults.filterOperators[columnConfig.filterType]
                    columnConfig.jqgrid.searchoptions << [sopt: operators.collect {
                        JsUtils.jqgridFilterOperatorConverter(it)
                    }]
                }

                //if no property defined
                if (columnConfig.jqgrid.editable == null) {
                    columnConfig.jqgrid.editable = (columnConfig.property != null)
                }
            }
        }

        //disable sorting in the UI if the column is not sortable
        gridConfig.columns.each { ColumnConfig columnConfig ->
            columnConfig.jqgrid.sortable = columnConfig.sortable
        }
    }

    def filters(GridConfig gridConfig) {
        if (params._search == 'true') {        // text field not boolean
/*
            searchParams.each { param ->
                def column = gridConfig.columns[param]
                if(column){
                    filters << filterService.createFilterFromColumn(gridConfig, column)
                }
            }
*/
            if (params.filters) {
                //  Translate jqgrid search rules into a Filters structure for EasyGrid
                jqGridMultiSearchService.multiSearchToCriteriaClosure(gridConfig, params.filters)
            }
        }
    }

    def listParams(gridConfig) {
        def currentPage = params.page ? (params.page as int) : 1
        def maxRows = params.rows ? (params.rows as int) : grailsApplication.config?.easygrid?.defaults?.defaultMaxRows
        def offset = (currentPage - 1) * maxRows

        def result = [rowOffset: offset, maxRows: maxRows]

        //check for multisort
        if (gridConfig.jqgrid?.multiSort) {
//In case when the data is obtained from the server the sidx parameter contain the order clause. It is a comma separated string in format field1 asc, field2 desc …, fieldN. Note that the last field does not not have asc or desc. It should be obtained from sord parameter
//        When the option is true the behavior is a s follow
            if (params.sidx) {
                result.multiSort = params.sidx.split(',')?.collect { String token ->
                    String[] tokens = token.trim().split(' ')
                    if (tokens.size() == 2) {
                        return [sort: tokens[0], order: tokens[1]]
                    } else {
                        return [sort: tokens[0], order: params.sord]
                    }
                }
            }

        } else {
            def sort = (params.sidx) ? params.sidx : null
            def order = sort ? params.sord : null
            result += [sort: sort, order: order]
        }

        result


    }

    def transform(gridConfig, rows, nrRecords, listParams) {
        // transform the list of elements to a jqGrid format
        def results = rows.collect { element ->
            def cell = []
            def id
            GridUtils.eachColumn(gridConfig) { column, row ->
                def val = GridUtils.valueOfColumn(gridConfig, column, element, row + 1)
                cell << val
                if (column.name == gridConfig.idColName) {
                    id = val
                }
            }

            [id: id, cell: cell]
        }

        [rows: results, page: 1 + (listParams.rowOffset / listParams.maxRows as int), records: nrRecords, total: Math.ceil(nrRecords / listParams.maxRows) as int] as JSON
    }

    def transformInlineResponse(GridConfig gridConfig, InlineResponse response) {
        def httpResponse = [status: HttpStatus.OK]
//        response.message - a global error message
//        response.instance - the object instance  ( the instance.errors object will be used to render the errors)
//        response.errors - send the errors directly

        def errorStruct = [:]
        if (response.message) {
            errorStruct.message = messageLabel(response.message)
        }
        def errors = response.errors ?: response.instance?.errors
        if (errors && errors.hasErrors()) {
            errorStruct.fields = [:]
            errors.fieldErrors.each { err ->
                //determine the column based on the field
                ColumnConfig col = gridConfig.columns.find { it.property == err.field }
                errorStruct.fields[col.name] = errorLabel(err)
            }
        }

        if (errorStruct) {
            httpResponse.status = HttpStatus.BAD_REQUEST
            httpResponse.text = errorStruct as JSON
        } else {
            def success = [:]
            //todo - send the id
            if (response.instance?.version != null) {
                //send the version
                success.version = response.instance?.version
            }
            if (response.instance?."${gridConfig.idColName}" != null) {
                success.id = response.instance[gridConfig.idColName]
            }
            httpResponse.text = success as JSON
        }

        httpResponse
    }

}
