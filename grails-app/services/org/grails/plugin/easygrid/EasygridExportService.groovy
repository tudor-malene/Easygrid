package org.grails.plugin.easygrid

import groovy.util.logging.Slf4j
import org.springframework.web.servlet.support.RequestContextUtils
import static org.grails.plugin.easygrid.EasygridContextHolder.*

/**
 * Standard export service
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
class EasygridExportService {

    static transactional = false

    def grailsApplication

    def exportService

    def addDefaultValues(gridConfig, Map defaultValues) {
        if (gridConfig.export.export_title == null) {
            gridConfig.export.export_title = gridConfig.id
        }
    }

    def export(GridConfig gridConfig, data, format, extension) {
        log.debug("export ${data.size()}")

        if (format && format != "html") {

            def contentTypes = grailsApplication.config.grails.mime.types[format]
            assert contentTypes: "No content type declared for format: ${format}"
            response.contentType = String.isAssignableFrom(contentTypes.getClass()) ? contentTypes : contentTypes[0]
            response.setHeader("Content-disposition", "attachment; filename=${gridConfig.export.export_title}.${extension}")

            // apply an additional filter on the data which is available in the grid
            if (gridConfig.export.exportFilter) {
                data = data.findAll exportFilter
            }

            //transform the raw data to the actual values to be exported
            def exportData = new ArrayList(data.size())

            data.each { element ->
                def resultRow = [:]
                GridUtils.eachColumn(gridConfig, true) { column, row ->
                    resultRow[getColName(column)] = GridUtils.valueOfExportColumn(gridConfig, column, element, row + 1)
                }
                exportData.add resultRow
            }

            // compose other parameters needed by the export parameter
            def fields = []
            def labels = [:]
            GridUtils.eachColumn(gridConfig, true) { ColumnConfig column ->
                def colName=getColName(column)
                fields << colName
                labels[colName] = grailsApplication.mainContext.getMessage(column.label, new Object[0], column.label, RequestContextUtils.getLocale(request))
            }
            log.debug("export fields: $fields")
            log.debug("export labels: $labels")

            //aggregate export properties defined in columns like 'column.widths'
            Map parameters = gridConfig.export[format].collectEntries { k, v ->
                if (v instanceof Closure) {
                    [(k): v.call(gridConfig)]
                } else {
                    [(k): v]
                }
            }
            log.debug("export parameters: $parameters")

            // invoke the export plugin
            exportService.export(format, response.outputStream, exportData, fields, labels, [:], parameters)
        }
    }

    /**
     * the export plugin ignores columns with '.'
     */
    static def getColName(column){
        String name = column.name
        name.replaceAll('\\.','_')
    }

}
