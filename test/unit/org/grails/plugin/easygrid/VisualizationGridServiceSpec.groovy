package org.grails.plugin.easygrid

import com.google.visualization.datasource.datatable.value.ValueType
import com.ibm.icu.util.GregorianCalendar
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import groovy.json.JsonSlurper
import groovy.time.TimeCategory
import org.grails.plugin.easygrid.grids.VisualizationGridService
import spock.lang.Ignore
import spock.lang.Specification

import static org.grails.plugin.easygrid.TestUtils.populateTestDomain

/**
 * google visualization impl tests
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@TestFor(VisualizationGridService)
@Mock(TestDomain)
class VisualizationGridServiceSpec extends Specification {

    def setup() {
        service.filterService = new FilterService()
        service.filterService.grailsApplication = grailsApplication
    }

    def "testGlobalFilter"() {

        given:
        def N = 100
        populateTestDomain(N)

        //initialize the list grid
        GridConfig domainGridConfig = TestUtils.generateConfigForGrid(grailsApplication) {
            'testDomainGrid' {
                dataSourceType 'gorm'
                gridImpl 'visualization'
                domainClass TestDomain
                globalFilterClosure { params ->
                    if (params.min && params.max) {
                        between("testIntProperty", params.min as int, params.max as int)
                    }
                }
                columns {
                    id {
                        type 'id'
                        valueType Long
                    }
                    testStringProperty {
                        valueType String
                        filterDataType String
                        filterClosure { filter ->
                            ilike('testStringProperty', "%${filter.paramValue}%")
                        }
                    }
                    testIntProperty {
                        valueType Integer
                        filterDataType Integer
                        filterClosure { filter ->
                            eq('testIntProperty', filter.paramValue as int)
                        }
                    }
                }
            }
        }.testDomainGrid
        def (params, request, response, session) = TestUtils.mockEasyGridContextHolder()

        when:
        domainGridConfig.columns.each { ColumnConfig col ->
            service.dynamicProperties(domainGridConfig, col)
        }

        then:
        domainGridConfig.columns['id'].visualization.valueType == ValueType.NUMBER
        domainGridConfig.columns['testStringProperty'].visualization.valueType == ValueType.TEXT
        domainGridConfig.columns['testIntProperty'].visualization.valueType == ValueType.NUMBER


        when:
        params._filter = 'true'
        params.testStringProperty = '3'
        def filters = service.filters(domainGridConfig).filters

        then:
        1 == filters.size()
        'testStringProperty' == filters[0].filterable.name
        '3' == filters[0].paramValue

        when:
        params.tq = 'order by `testIntProperty` desc limit 50 offset 0'
        params.tqx = ''
        //hack
        request.setParameter('tq', params.tq)
        request.setParameter('tqx', params.tqx)
        def listParams = service.listParams(domainGridConfig)

        then:
        [sort: 'testIntProperty', order: 'desc', maxRows: 50, rowOffset: 0] == listParams

        when:
        params.min = 1
        params.max = 3
        def rows = TestDomain.list()

//        def data = easygridService.gridData(domainGridConfig)
        def data = service.transform(domainGridConfig, rows, 100, listParams)
        def resp = new JsonSlurper().parseText data['google.visualization.Query.setResponse('.length()..-3]

        then:
        100 == resp.table.rows.size()
    }


    def "testCustomVisConfig"() {
        given:
        def customVisGridConfig = TestUtils.generateConfigForGrid(grailsApplication) {
            'authorGrid' {
                labelPrefix 'author'
                dataSourceType 'custom'
                gridImpl 'visualization'
                roles 'admin'
                securityProvider { grid, oper ->
                    if (grid.roles) {
                        if (grid.roles == 'admin') {
                            return true
                        }
                        return false
                    }
                    return true
                }
                dataProvider { filters, listParams ->
                    def values = [
                            [name: 'Fyodor Dostoyevsky', nation: 'russian', birthDate: new GregorianCalendar(1821, 10, 11).time],
                            [name: 'Ion Creanga', nation: 'romanian', birthDate: new GregorianCalendar(1837, 2, 3).time],
                    ]
                    if (listParams.sort != 'age') {
                        values.sort { o1, o2 ->
                            def x = o1[listParams.sort] <=> o2[listParams.sort]; (listParams.order == 'asc') ? x : -x
                        }
                    } else {
                        values.sort { o1, o2 ->
                            def x = o1.birthDate <=> o2.birthDate; (listParams.order == 'asc') ? x : -x
                        }
                    }
                }
                dataCount { filterClosure ->
                    2
                }
                columns {
                    name {
                        visualization {
                            valueType ValueType.TEXT
                        }
                    }
                    nation {
                        visualization {
                            valueType ValueType.TEXT
                        }
                    }
                    age {
                        value { row ->
                            use(TimeCategory) {
                                new Date().year - row.birthDate.year
                            }
                        }
                        visualization {
                            valueType ValueType.NUMBER
                        }
                    }
                    birthDate {
                        visualization {
                            valueType ValueType.DATE
                        }
                    }
                }
            }
        }.authorGrid

        def (params, request, response, session) = TestUtils.mockEasyGridContextHolder()

        when:
        params.tq = 'order by `age` desc limit 10 offset 0'
        params.tqx = ''
        //hack
        request.setParameter('tq', params.tq)
        request.setParameter('tqx', params.tqx)

        def result = service.listParams(customVisGridConfig)

        then:
        [sort: 'age', order: 'desc', maxRows: 10, rowOffset: 0] == result
    }
}
