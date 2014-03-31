package org.grails.plugin.easygrid.grids

import com.google.visualization.datasource.DataSourceHelper
import com.google.visualization.datasource.DataSourceRequest
import com.google.visualization.datasource.datatable.ColumnDescription
import com.google.visualization.datasource.datatable.DataTable
import com.google.visualization.datasource.datatable.TableCell
import com.google.visualization.datasource.datatable.TableRow
import com.google.visualization.datasource.datatable.value.ValueType
import com.google.visualization.datasource.query.Query
import com.google.visualization.datasource.query.SortOrder
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.grails.plugin.easygrid.ColumnConfig
import org.grails.plugin.easygrid.EasygridContextHolder
import org.grails.plugin.easygrid.Filter
import org.grails.plugin.easygrid.Filters
import org.grails.plugin.easygrid.GridUtils
import static org.grails.plugin.easygrid.EasygridContextHolder.*

/**
 * service class that implements the google visualization grid
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class VisualizationGridService {

    static transactional = false

    def easygridService
    def grailsApplication

    /**
     * called during the dynamic generation phase  for each column
     * @param gridConfig
     * @param prop
     * @param column
     */
    def dynamicProperties(gridConfig, column) {
        column.visualization ?: (column.visualization = [:])
        column.visualization.valueType = getValueType(column.valueType)
    }

    def filterService

    def filters(gridConfig) {
        if (params._filter) {
            def filters = new Filters()
            params.findAll { k, v -> v }.collect { k, v -> k }.intersect(gridConfig.columns.collect {
                it.name
            }).each { param ->
                def column = gridConfig.columns[param]
//                column ? (list + new Filter(column)) : list
                if (column) {
                    filters << filterService.createFilterFromColumn(gridConfig, column, null, params[param])
                }
            }
            filters
        }
    }

    def listParams(gridConfig) {

        def result = [:]
        Query query = new DataSourceRequest(request).query
        assert query
        //only works for 1 column
        query.sort?.sortColumns?.each { sortColumn ->
            result.sort = sortColumn.column.id
            result.order = (sortColumn.order == SortOrder.DESCENDING) ? 'desc' : 'asc'
        }

        result.maxRows = (query.rowLimit <= 0) ? null : query.rowLimit
        result.rowOffset = query.rowOffset

        result
    }

    def transform(gridConfig, rows, nrRecords, listParams) {
        DataTable dataTable = createDataTable(gridConfig, rows)
        DataSourceHelper.generateResponse(dataTable, new DataSourceRequest(request))
    }

    def createDataTable(gridConfig, rows) {
        DataTable dataTable = new DataTable()

        //add table properties
        gridConfig.visualization.each { k, v ->
            dataTable.setCustomProperty(k, v.toString())
        }

        //add columns
        dataTable.addColumns gridConfig.columns.collect { column ->
            def cd = new ColumnDescription(column.name, column.visualization.valueType, messageLabel(column.label))
            // only className and style
            column.visualization.findAll { (it.key in ['className', 'style']) }.each { k, v ->
                cd.setCustomProperty(k, v)
            }
            cd
        }

        //add rows
        rows.each { element ->
            TableRow row = new TableRow()

            gridConfig.columns.eachWithIndex { ColumnConfig col, idx ->
                def val = GridUtils.valueOfColumn(gridConfig, col, element, idx + 1)
                //hack - createValue only takes strings
                val = GString.isAssignableFrom(val.getClass()) ? val.toString() : val
                TableCell cell = new TableCell(col.visualization.valueType.createValue(val))
                row.addCell(cell)
            }

            dataTable.addRow(row)
        }

        dataTable
    }

    private ValueType getValueType(Class type) {
        if (type == Boolean || type == boolean) {
            return ValueType.BOOLEAN
        }
        if (Number.isAssignableFrom(type) || (type.isPrimitive() && type != boolean)) {
            return ValueType.NUMBER
        }
        if (type == Date || type == java.sql.Date || type == java.sql.Time || type == Calendar) {
            return ValueType.DATE
        }
        return ValueType.TEXT
    }
}
