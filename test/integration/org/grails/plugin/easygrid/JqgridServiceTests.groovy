package org.grails.plugin.easygrid

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.junit.Before
import static org.junit.Assert.*

/**
 * User: Tudor
 * Date: 27.09.2012
 * Time: 18:49
 */
@Mock(TestDomain)
@TestFor(TestDomainController)
class JqgridServiceTests extends AbstractServiceTest {

    def jqueryGridService
    def domainGridConfig
    def testListGridSize = 200


    @Before
    void setUp() {
        super.setup()

        //initialize the list grid
        domainGridConfig = generateConfigForGrid {
            id 'testDomainGrid'
            dataSourceType 'domain'
            domainClass TestDomain
        }
    }


    void testJQGridConfig() {
        def controller = new TestDomainController()

        def gridsConfig = easygridService.initGrids(controller)
        EasygridContextHolder.setLocalGridConfig(gridsConfig.testGrid)
//        assertEquals 16, gridsConfig.testGrid.size()--todo
        assertEquals false, gridsConfig.testGrid.columns[0].jqgrid.editable

//        assertEquals "'testDomain.id.label','testDomain.testIntProperty.label','testDomain.testStringProperty.label'", colNames
//        assertEquals "{name:'id', width:40,fixed:true,editable:false,},{name:'testIntProperty', editable:true,},{name:'testStringProperty', editable:true,}", colModel
//        assertEquals "width:300,height:150,", jqgridProperties

    }

    void testEditRow() {

        params.oper = 'edit'
        easygridService.addDefaultValues(domainGridConfig, defaultValues)

        assertEquals 'default.not.found.message', jqueryGridService.inlineEdit()[0]
    }

    /**
     * test the call to gridData as it would be called from jqgrid
     * with various values for the page no, no or rows, search col,
     */
    void testListGrid() {
        easygridService.addDefaultValues(listGridConfig, defaultValues)

        //no search
        params.page = 2
        params.rows = 10
        def gridElements = easygridService.gridData(listGridConfig)

        assertEquals testListGridSize, gridElements.target.records
        assertEquals 2, gridElements.target.page
        assert (testListGridSize / 10) == gridElements.target.total
        assertEquals 10, gridElements.target.rows.size()
        assertEquals 11, gridElements.target.rows[0].cell[0]

        //with search
        params.page = 2
        params.rows = 10
        params.min = 100
        params.col1 = 'col1'
        params._search = 'true'
        gridElements = easygridService.gridData(listGridConfig)
        assertEquals 100, gridElements.target.records
        assertEquals 2, gridElements.target.page
        assert 10 == gridElements.target.total
        assertEquals 10, gridElements.target.rows.size()
        assertEquals 111, gridElements.target.rows[0].cell[0]

        //with 2 searches
        params.page = 1
        params.rows = 5
        params.min = 100
        params.col1 = 'col1'
        params.col2 = '10'
        params._search = 'true'
        gridElements = easygridService.gridData(listGridConfig)
        assertEquals 10, gridElements.target.records
        assertEquals 1, gridElements.target.page
//        assert 5 == gridElements.target.total
//        assertEquals 5, gridElements.target.rows.size()
//        assertEquals 111, gridElements.target.rows[0].cell[0]



        params.clear()
        easygridService.addDefaultValues(customGridConfig, defaultValues)
        gridElements = easygridService.gridData(customGridConfig)
        assertEquals 1, gridElements.target.records
        assertEquals 1, gridElements.target.page
        assertEquals 1, gridElements.target.total
        assertEquals 1, gridElements.target.rows.size()

        def N = 100
        populateTestDomain(N)
        def gridsConfig = easygridService.initGrids(new TestDomainController())
//        easygridService.generateDynamicColumns(gridsConfig.testGrid)

        // test default page nr
        gridElements = easygridService.gridData(gridsConfig.testGrid)
        assertEquals N, gridElements.target.records
        assertEquals 1, gridElements.target.page
        assertEquals 5, gridElements.target.total
        assertEquals 20, gridElements.target.rows.size()

        //test a different page
        params.page = 2
        params.rows = 10
        gridElements = easygridService.gridData(gridsConfig.testGrid)
        assertEquals N, gridElements.target.records
        assertEquals 2, gridElements.target.page
        assertEquals 10, gridElements.target.total
        assertEquals 10, gridElements.target.rows.size()
        assertEquals 11, gridElements.target.rows[0].cell[0]
        assertEquals 11, gridElements.target.rows[0].id


    }


    void testHtmlGridDefinition() {
        easygridService.addDefaultValues(customGridConfig, defaultValues)
        def model = easygridService.htmlGridDefinition(customGridConfig);
        assertNotNull model

        assertNotNull model
        assertEquals customGridConfig, model.gridConfig
    }


}
