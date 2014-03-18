package org.grails.plugin.easygrid

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.grails.plugin.easygrid.grids.JqGridMultiSearchService
import org.grails.plugin.easygrid.grids.JqueryGridService
import spock.lang.Specification

/**
 * jqgrid impl tests
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@TestFor(JqueryGridService)
@Mock(TestDomain)
class JqgridSpec extends Specification {

    def setup() {
        service.filterService = new FilterService()
    }

    def "Edit row"() {
        when:
        def domainGridConfig = TestUtils.generateConfigForGrid(grailsApplication, {
            testDomainGrid {
                dataSourceType 'gorm'
                domainClass TestDomain
                updateRowClosure {
                    'default.not.found.message'
                }
            }
        }).testDomainGrid
        def (params, request, response, session) = TestUtils.mockEasyGridContextHolder()
        params.oper = 'edit'

        then:
        service.inlineEdit(domainGridConfig) == 'default.not.found.message'
    }

    /**
     * test the call to gridData as it would be called from jqgrid
     * with various values for the page no, no or rows, search col,
     */
    def "various jqgrid operations"() {
        given:
        service.jqGridMultiSearchService = new JqGridMultiSearchService()
        service.jqGridMultiSearchService.filterService = new FilterService()

        def listGridConfig = TestUtils.generateConfigForGrid(grailsApplication) {
            listProviderGrid {
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
                        enableFilter true
                        filterFieldType 'text'
                        jqgrid {
                        }
                    }
                    col3 {
                        value {
                            val -> val.col1 ^ 2
                        }
                        filterClosure { val, element ->
                        }
                    }
                }
            }
        }.listProviderGrid
        def (params, request, response, session) = TestUtils.mockEasyGridContextHolder()


        when:
        params.page = 2
        params.rows = 10

        def listParams = service.listParams(listGridConfig)

        then:
        listParams.rowOffset == 10
        listParams.maxRows == 10
        listParams.sort == null
        listParams.order == null


        when:
        params.clear()
        params._search = 'true'
        params.filters = '''
{   "groupOp":"AND",
     "rules":    [{"field":"col1","op":"eq","data":"xxx"},{"field":"col2","op":"cn","data":"xxx"},{"field":"col3","op":"eq","data":"xxx"}],
     "groups":   [{   "groupOp":"AND",
     "rules":    [{"field":"col1","op":"ne","data":"xx"}],
     "groups":   [{   "groupOp":"OR",
     "rules":    [{"field":"col2","op":"bw","data":"xx"}],
     "groups":   []}]}]}'''
        def filters = service.filters(listGridConfig)

        then:
        filters.filters.size() == 4

        when:
        params.clear()
        def gridElements = service.transform(listGridConfig,
                (1..200).collect { [col1: it, col2: "$it"] },
                200,
                [rowOffset: 40, maxRows: 20, sort: 'col1', order: 'asc'])

        then:
        200 == gridElements.target.records
        3 == gridElements.target.page
//        20 == gridElements.target.total
//        10 == gridElements.target.rows.size()
//        11 == gridElements.target.rows[0].cell[0]

/*
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
        def gridsConfig = easygridService.initControllerGrids(controller)
        // test default page nr
        gridElements = easygridService.gridData(gridsConfig.testGrid)

        then:
        N == gridElements.target.records
        1 == gridElements.target.page
        N / params.rows == gridElements.target.total
        params.rows == gridElements.target.rows.size()
*/

/*
        when: "test global filter closure"
        addControllerMethods()
        controller.testGlobalFilterGridRows()

        then:
        '{"rows":[{"id":3,"cell":[3,3,"3"]}],"page":1,"records":1,"total":1}' == response.content.toString()
*/

/*
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
*/
    }

    def "test multisort"() {
        when:
        def domainGridConfig = TestUtils.generateConfigForGrid(grailsApplication, {
            testDomainGrid {
                dataSourceType 'gorm'
                domainClass TestDomain
                jqgrid {
                    multiSort true
                }
            }
        }).testDomainGrid

        def (params, request, response, session) = TestUtils.mockEasyGridContextHolder()
        params.sidx = 'testStringProperty asc, testIntProperty'
        params.sord = 'desc'

        then:
        [[sort: 'testStringProperty', order: 'asc'], [sort: 'testIntProperty', order: 'desc']] == service.listParams(domainGridConfig).multiSort

    }


}
