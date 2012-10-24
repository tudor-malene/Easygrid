package org.grails.plugin.easygrid

import groovy.util.logging.Log4j
import de.andreasschmitt.export.builder.ExcelBuilder

/**
 * Standard export service
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Log4j
@Mixin(EasygridContextHolder)
class EasyGridExportService {

    static transactional = false
    def easygridService
    def grailsApplication

    def exportXls() {

        response.contentType = grailsApplication.config?.grails?.mime?.types?.xls
//        response.setHeader("Content-disposition", "attachment; filename=export_${message(code: gridConfig.exportTitle)}.xls")
//            response.setHeader("Content-disposition", "attachment; filename=${message(code: gridConfig.export_title)}.xls")
        //todo
        response.setHeader("Content-disposition", "attachment; filename=${gridConfig.export_title}.xls")

        setLocalGridConfig(gridConfig)

        GridUtils.markRestorePreviousSearch()
        GridUtils.restoreSearchParams()

        //returns a list of search Closures
        def filters = easygridService.implService.filters()

        def data = easygridService.dataSourceService.list([:], filters)

        def startAt = 0

        //call a collect closure to add more fields
        def builder = new ExcelBuilder()
        def visibleColumns = gridConfig.columns.findAll{!it.export.hidden}

        builder {
            workbook(outputStream: response.outputStream) {
                def widths = []
                visibleColumns.eachWithIndex {  column, index ->
                    widths[index] = column.export.width
                    assert widths[index] // the width setting is mandatory
                }

                sheet(name: message(gridConfig.export_title) ?: "Export", widths: widths) {
                    //Default format
                    format(name: "header") {
                        font(name: "arial", bold: true)
                    }

                    //Create header
                    visibleColumns.eachWithIndex { Column column, index ->
                        cell(row: startAt, column: index, value: grailsApplication.mainContext.getMessage(column.label, new Object[0], column.label, Locale.getDefault()), format: "header")
                    }

                    //Rows
                    data.eachWithIndex { element, row ->
                        visibleColumns.eachWithIndex { column, idx ->
                            cell(row: startAt + row + 1, column: idx, value: easygridService.valueOfColumn(column, element, row))
                        }
                    }
                }
            }
        }
        builder.write()
    }

}
