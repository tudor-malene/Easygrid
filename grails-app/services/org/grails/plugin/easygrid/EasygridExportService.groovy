package org.grails.plugin.easygrid

import groovy.util.logging.Slf4j
import org.springframework.web.servlet.support.RequestContextUtils

/**
 * Standard export service
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
@Mixin(EasygridContextHolder)
class EasygridExportService {

    static transactional = false

    def easygridService
    def grailsApplication

    def exportService

    def addDefaultValues(Map defaultValues) {
        if (gridConfig.export.export_title == null) {
            gridConfig.export.export_title = gridConfig.id
        }
    }

    def export() {
        //export parameters
        def extension = params.extension
        def format = params.format

        if (format && format != "html") {

            def contentTypes = grailsApplication.config.grails.mime.types[format]
            assert contentTypes : "No content type declared for format: ${format}"
            response.contentType = String.isAssignableFrom(contentTypes.getClass()) ? contentTypes : contentTypes[0]
            response.setHeader("Content-disposition", "attachment; filename=${gridConfig.export.export_title}.${extension}")

            // restore the previous search
            GridUtils.markRestorePreviousSearch()
            GridUtils.restoreSearchParams()

            //apply the previous filters , retrieve the raw data & transform the data to an export friendly format
            def filters = easygridService.implService.filters()
            def data = easygridService.dataSourceService.list([:], filters)
            def exportData = new ArrayList(data.size())
            data.each { element ->
                def resultRow = [:]
                GridUtils.eachColumn(gridConfig, true) { column, row ->
                    resultRow[column.name] = easygridService.valueOfColumn(column, element, row + 1)
                }
                exportData.add resultRow
            }

            // compose other parameters needed by the export parameter
            def fields = []
            def labels = [:]
            GridUtils.eachColumn(gridConfig, true) { ColumnConfig column ->
                fields << column.name
                labels[column.name] = grailsApplication.mainContext.getMessage(column.label, new Object[0], column.label, RequestContextUtils.getLocale(request))
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
}
