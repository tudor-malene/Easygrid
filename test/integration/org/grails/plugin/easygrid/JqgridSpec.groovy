package org.grails.plugin.easygrid

import grails.converters.JSON
import spock.lang.Shared

/**
 * jqgrid impl tests
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class JqgridSpec extends AbstractBaseTest {

    static transactional = true

    @Shared def domainGridConfig
    @Shared def listGridConfig

    def jqueryGridService

    def initGrids() {
        //initialize the list grid
        domainGridConfig = generateConfigForGrid {
            id 'testDomainGrid'
            dataSourceType 'domain'
            domainClass TestDomain
        }

        //initialize the list grid
        listGridConfig = generateConfigForGrid {
            id 'listProviderGrid'
            labelPrefix 'list'
            dataSourceType 'list'
            context 'session'
            attributeName 'listData'
            columns {
                col1 {
                    filterClosure { Filter filter, element ->
                        element.col1 > filter.params.min
                    }
                    jqgrid {
                        editable true
                    }
                }
                col2 {
//                    filterClosure {val, params, element ->
//                        element.col2.contains(params.col2)
//                    }
                    enableFilter true
                    filterFieldType 'text'
                    jqgrid {
                    }
                }
                col3 {
                    value { it.col1 * it.col1 }
                    filterClosure { val, element ->
                    }
                }
            }
        }
    }



    def "Controller grid initialized properly"() {
        when:
        def controller = new TestDomainController()
        def gridsConfig = easygridService.initGrids(controller)
        EasygridContextHolder.setLocalGridConfig(gridsConfig.testGrid)

        then:
        gridsConfig.testGrid.columns[0].jqgrid.editable == false
    }


    def "Edit row"() {
        when:
        params.oper = 'edit'
        easygridService.addDefaultValues(domainGridConfig, defaultValues)

        then:
        jqueryGridService.inlineEdit()[0] == 'default.not.found.message'
    }

    /**
     * test the call to gridData as it would be called from jqgrid
     * with various values for the page no, no or rows, search col,
     */
    def "various jqgrid operations"() {
        given:
        easygridService.addDefaultValues(listGridConfig, defaultValues)
        def N = 100
        populateTestDomain(N)


        when:
        params.page = 2
        params.rows = 10
        def gridElements = easygridService.gridData(listGridConfig)

        then:
        200 == gridElements.target.records
        2 == gridElements.target.page
        20 == gridElements.target.total
        10 == gridElements.target.rows.size()
        11 == gridElements.target.rows[0].cell[0]


        when: "search after the first col"
        params.page = 2
        params.rows = 10
        params.min = 100
        params.col1 = 'col1'
        params._search = 'true'
        gridElements = easygridService.gridData(listGridConfig)

        then:
        100 == gridElements.target.records
        2 == gridElements.target.page
        10 == gridElements.target.total
        10 == gridElements.target.rows.size()
        111 == gridElements.target.rows[0].cell[0]


        when: "with 2 searches"
        params.page = 1
        params.rows = 5
        params.min = 100
        params.col1 = 'col1'
        params.col2 = '10'
        params._search = 'true'
        gridElements = easygridService.gridData(listGridConfig)

        then:
        10 == gridElements.target.records
        1 == gridElements.target.page

        when:
        def gridsConfig = easygridService.initGrids(new TestDomainController())
        // test default page nr
        gridElements = easygridService.gridData(gridsConfig.testGrid)

        then:
        N == gridElements.target.records
        1 == gridElements.target.page
        N/params.rows == gridElements.target.total
        params.rows == gridElements.target.rows.size()


        when: "test global filter closure"
        def controller = new TestDomainController()
        controller.testGlobalFilterGridRows()

        then:
        '{"rows":[{"id":3,"cell":[3,3,"3"]}],"page":1,"records":1,"total":1}' == response.content.toString()

        when: "test a different page"
        params.page = 2
        params.rows = 10
        gridElements = easygridService.gridData(gridsConfig.testGrid)

        then:
        N == gridElements.target.records
        2 == gridElements.target.page
        10 == gridElements.target.total
        10 == gridElements.target.rows.size()
        11 == gridElements.target.rows[0].cell[1]
        null != gridElements.target.rows[0].id
    }

}
