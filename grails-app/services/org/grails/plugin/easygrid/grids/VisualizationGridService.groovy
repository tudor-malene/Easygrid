package org.grails.plugin.easygrid.grids

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.grails.plugin.easygrid.ColumnConfig
import org.grails.plugin.easygrid.EasygridContextHolder

import com.google.visualization.datasource.DataSourceHelper
import com.google.visualization.datasource.DataSourceRequest
import com.google.visualization.datasource.datatable.ColumnDescription
import com.google.visualization.datasource.datatable.DataTable
import com.google.visualization.datasource.datatable.TableCell
import com.google.visualization.datasource.datatable.TableRow
import com.google.visualization.datasource.datatable.value.ValueType
import com.google.visualization.datasource.query.Query
import com.google.visualization.datasource.query.SortOrder

/**
 * service class that implements the google visualization grid
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Mixin(EasygridContextHolder)
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
    def dynamicProperties(GrailsDomainClassProperty prop, column) {
        column.visualization ?: (column.visualization = [:])
        column.visualization.valueType = getValueType(prop.type)
    }

    def addDefaultValues(Map defaultValues) {
        gridConfig.columns.each {ColumnConfig column ->
            if (column?.visualization?.name == null) {
                column.visualization ?: (column.visualization = [:])
                assert column.property
                column.visualization.name = column.property
            }
            if (column?.visualization?.valueType == null) {
                //fallback to text if
                column.visualization.valueType = ValueType.TEXT
            }
        }
    }

    def filters() {
        if (params._filter) {
            params.findAll {k, v -> v}.collect {k, v -> k}.intersect(gridConfig.columns.collect {it.visualization.name }).inject([]) {list, param ->
                def closure = gridConfig.columns.find {col -> col.visualization.name == param}?.filterClosure
                closure ? (list + closure) : list
            }
        }
    }

    def listParams() {

        def result = [:]
        Query query = new DataSourceRequest(request).query
        assert query
        //only works for 1 column
        query.sort?.sortColumns?.each {sortColumn ->
            result.sort = sortColumn.column.id
            result.order = (sortColumn.order == SortOrder.DESCENDING) ? 'desc' : 'asc'
        }

        result.maxRows = query.rowLimit
        result.rowOffset = query.rowOffset

        result
    }

    def transform(rows, nrRecords, listParams) {
        DataTable dataTable = createDataTable(rows)
        DataSourceHelper.generateResponse(dataTable, new DataSourceRequest(request))
    }

    def createDataTable(rows) {
        DataTable dataTable = new DataTable()

        //add table properties
        gridConfig.visualization.each {k, v ->
            dataTable.setCustomProperty(k, v.toString())
        }

        //add columns
        dataTable.addColumns gridConfig.columns.collect {column ->
            def cd = new ColumnDescription(column.visualization.name, column.visualization.valueType, message(column.label))
            // only className and style
            column.visualization.findAll {(it.key in ['className', 'style'])}.each {k, v ->
                cd.setCustomProperty(k, v)
            }
            cd
        }

        //add rows
        rows.each { element ->
            TableRow row = new TableRow()

            gridConfig.columns.eachWithIndex { ColumnConfig col, idx ->
                def val = easygridService.valueOfColumn(col, element, idx + 1)
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
