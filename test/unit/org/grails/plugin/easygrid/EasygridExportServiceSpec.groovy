package org.grails.plugin.easygrid

import de.andreasschmitt.export.ExportService
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

import static TestUtils.generateConfigForGrid
import static TestUtils.populateTestDomain

/**
 * test the export feature
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@TestFor(EasygridExportService)
@Mock(TestDomain)
class EasygridExportServiceSpec extends Specification {

    GridConfig testGrid
    ExportService exportService

    EasygridDispatchService easygridDispatchService

    def setup() {
        testGrid = generateConfigForGrid(grailsApplication) {
            testGrid {
                dataSourceType 'domain'
                domainClass TestDomain
                gridRenderer '/templates/testGridRenderer'
                export {
                    'column.width' 100
                }
                columns {
                    id {
                        type id
                    }
                    testStringProperty {
                        property 'testStringProperty'
                    }
                    testIntProperty {
                        property 'testIntProperty'
                    }
                }
            }
        }.testGrid

        exportService = Mock(ExportService)
        service.exportService = exportService
    }

    def "excel Export"() {

        given:
        def (params, request, response, session) = TestUtils.mockEasyGridContextHolder()
        populateTestDomain(100)

        when:
        params.format = 'excel'
        service.export(testGrid, TestDomain.list())

        then:
        1 * exportService.export(
                'excel', //type
                _, //
                { it.size() == 100 }, //objects
                ['id', 'testStringProperty', 'testIntProperty'], //fields
                ['id': 'testDomain.id.label', 'testStringProperty': 'testDomain.testStringProperty.label', 'testIntProperty': 'testDomain.testIntProperty.label'], //labels
                [:], //formatters
                { it['column.widths'] } //parameters
        )

/*
        Workbook wb = Workbook.getWorkbook(new ByteArrayInputStream(response.contentAsByteArray))

        expect:
        1 == wb.getNumberOfSheets()

        when:
        Sheet sheet = wb.getSheet(0)

        then:
        sheet != null

        //todo - verify the other custom settings

        //todo - add  label
        when:
        Cell[] header = sheet.getRow(0)

        then:
        'testDomain.id.label' == header[0].contents
        'testDomain.testIntProperty.label' == header[1].contents
        'testDomain.testStringProperty.label' == header[2].contents


        when:
        Cell[] firstRow = sheet.getRow(1)

        then:
        '1' == firstRow[1].contents
        '1' == firstRow[2].contents


        when:
        Cell[] lastRow = sheet.getRow(100)

        then:
        '100' == lastRow[1].contents
        '100' == lastRow[2].contents

        101 == sheet.getRows()
*/
    }


    def "testCsvExport"() {

        given:
        populateTestDomain(2)

        def (params, request, response, session) = TestUtils.mockEasyGridContextHolder()

        when: "export to csv"
        params.format = 'csv'
        service.export(testGrid,TestDomain.list())

        then:
        1 * exportService.export(
                'csv', //type
                _, //
                { it.size() == 2 }, //objects
                ['id', 'testStringProperty', 'testIntProperty'], //fields
                ['id': 'testDomain.id.label', 'testStringProperty': 'testDomain.testStringProperty.label', 'testIntProperty': 'testDomain.testIntProperty.label'], //labels
                [:], //formatters
                _ //parameters
        )


/*
        def responseLines = ((String) response.contentAsString).readLines()

        then: "the exported csv has 3 lines ( 1 header + 2 rows)"
        responseLines.size() == 3
        and: " the first line is the header"
        responseLines[0] == '"testDomain.id.label","testDomain.testIntProperty.label","testDomain.testStringProperty.label"'
        and: " the first row has a int value of 1"
        responseLines[1].split(',')[1] == '"1"'
*/
    }
}