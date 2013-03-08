package org.grails.plugin.easygrid

import jxl.Cell
import jxl.Sheet
import jxl.Workbook

/**
 * test the export feature
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class ExportServiceSpec extends AbstractBaseTest {

    static transactional = true

    /**
     * test the export feature
     */
    def "testXlsExport"() {

        given:
        def controller = new TestDomainController()
        def gridsConfig = easygridService.initGrids(controller)
        easygridService.addDefaultValues(gridsConfig.testGrid, defaultValues)
        populateTestDomain(100)
        params.format = 'excel'
        easygridService.export(gridsConfig.testGrid)
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
    }


    def "testCsvExport"() {

        given:
        params.format = 'csv'
        def controller = new TestDomainController()
        def gridsConfig = easygridService.initGrids(controller)
        easygridService.addDefaultValues(gridsConfig.testGrid, defaultValues)

        populateTestDomain(2)

        when: "export to csv"
        easygridService.export(gridsConfig.testGrid)
        def responseLines = ((String)response.contentAsString).readLines()

        then: "the exported csv has 3 lines ( 1 header + 2 rows)"
        responseLines.size() == 3
        and: " the first line is the header"
        responseLines[0]=='"testDomain.id.label","testDomain.testIntProperty.label","testDomain.testStringProperty.label"'
        and: " the first row has a int value of 1"
        responseLines[1].split(',')[1] == '"1"'
    }
}