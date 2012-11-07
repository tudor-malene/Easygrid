package org.grails.plugin.easygrid

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import jxl.Workbook
import jxl.Sheet
import jxl.Cell
import org.junit.Before
import static org.junit.Assert.*

/**
 * test the export feature
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Mock(TestDomain)
@TestFor(TestDomainController)
class ExportServiceTests extends AbstractServiceTest {
    //injected
    def easyGridExportService

    @Before
    void setUp() {
        super.setup()
    }

    /**
     * test the export feature
     */
    void testExport() {
        def controller = new TestDomainController()
        def gridsConfig = easygridService.initGrids(controller)
        easygridService.addDefaultValues(gridsConfig.testGrid, defaultValues)

        populateTestDomain(100)

        easygridService.export(gridsConfig.testGrid)

        Workbook wb = Workbook.getWorkbook(new ByteArrayInputStream(response.contentAsByteArray))
        assertEquals 1, wb.getNumberOfSheets()
        Sheet sheet = wb.getSheet(0)
        assertNotNull sheet

        //todo - verify the other custom settings

        //todo - add  label
        Cell[] header = sheet.getRow(0)
        assertEquals 'testDomain.id.label', header[0].contents
        assertEquals 'testDomain.testIntProperty.label', header[1].contents
        assertEquals 'testDomain.testStringProperty.label', header[2].contents

        Cell[] firstRow = sheet.getRow(1)
        assertEquals '1', firstRow[0].contents
        assertEquals '1', firstRow[1].contents
        assertEquals '1', firstRow[2].contents

        Cell[] lastRow = sheet.getRow(100)
        assertEquals '100', lastRow[0].contents
        assertEquals '100', lastRow[1].contents
        assertEquals '100', lastRow[2].contents


        assertEquals 101, sheet.getRows()
    }

}
