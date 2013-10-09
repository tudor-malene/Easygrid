package org.grails.plugin.easygrid

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import groovy.time.TimeCategory
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification


/**
 * tests for the central service
 * todo - de luat la mana
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@TestFor(EasygridService)
@Mock(TestDomain)
class EasygridServiceSpec extends Specification {


    def domainGridConfig
    def customGridConfig


    def setup() {
        domainGridConfig = TestUtils.generateConfigForGrid(grailsApplication) {
            'testDomainGrid' {
                dataSourceType 'domain'
                domainClass TestDomain
            }
        }.testDomainGrid

        //initialize the custom grid
        customGridConfig = TestUtils.generateConfigForGrid(grailsApplication) {
            'authorGrid' {
                dataSourceType 'custom'
                labelPrefix 'author'
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
                dataProvider { gridConfig, filters, listParams ->
                    [
                            [id: 1, name: 'Fyodor Dostoyevsky', nation: 'russian', birthDate: new GregorianCalendar(1821, 10, 11)],
                    ]
                }
                dataCount { filters ->
                    1
                }
                jqgrid {
                    width 650
                    height 150
                }
                columns {
                    id {
                        type 'id'
                    }
                    name {
                        filterClosure { filter ->
                            ilike('name', "%${filter.paramValue}%")
                        }
                        jqgrid {
                            editable true
                        }
                        export {
                            width 100
                        }
                    }
                    nation {
                        filterClosure { filter ->
                            ilike('nation', "%${filter.paramValue}%")
                        }
                        jqgrid {
                        }
                    }
                    age {
                        value { row ->
                            use(TimeCategory) {
                                new Date().year - row.birthDate.time.year
                            }
                        }
                        filterClosure { filter ->
                            eq('age', filter.paramValue as int)
                        }
                        jqgrid {
                            width 110
                        }
                    }
                    birthDate {
                        formatName 'stdDateFormatter'
                        filterClosure { filter ->
                            eq('birthDate', filter.paramValue as Date)
                        }
                        jqgrid {
                            width 110
                        }
                    }
                }
            }
        }.authorGrid
    }


    def "test label generation"() {
        when: "use the default label prefix"
        def simpleGridConfig = TestUtils.generateConfigForGrid(grailsApplication) {
            'simpleGrid' {
                dataSourceType 'domain'
                domainClass TestDomain
                columns {
                    testStringProperty
                    testIntProperty
                }
            }
        }.simpleGrid
        def (params, request, response, session) = TestUtils.mockEasyGridContextHolder()


        then:
        'testDomain.testStringProperty.label' == simpleGridConfig.columns[0].label
        'testStringProperty' == simpleGridConfig.columns[0].property

        and:
        'testDomain.testIntProperty.label' == simpleGridConfig.columns[1].label
        'testIntProperty' == simpleGridConfig.columns[1].property


        when: "use a custom label prefix"
        simpleGridConfig = TestUtils.generateConfigForGrid(grailsApplication) {
            'simpleGrid' {
                dataSourceType 'domain'
                domainClass TestDomain
                labelPrefix 'testDomainPrefix'
                columns {
                    testStringProperty
                    testIntProperty
                }
            }
        }.simpleGrid

        then:
        'testDomainPrefix.testStringProperty.label' == simpleGridConfig.columns[0].label
        'testDomainPrefix.testIntProperty.label' == simpleGridConfig.columns[1].label
    }

    /**
     * test the builder
     * the map representing the config is generated from the closure
     * for the list type grid
     */
    @Ignore
    def "testListBuilder"() {
//    todo
//        expect:
//        'listProviderGrid' == listGridConfig.id
//        'list' == listGridConfig.dataSourceType
//        3 == listGridConfig.columns.size()
    }

    /// end builder tests-----------------

    /// start default values tests-----------------

    /**
     * test that default values are injected correctly into the configuration
     */
    def "testCustomBuilderAndDefaultValues"() {
        expect:
        'authorGrid' == customGridConfig.id
        'custom' == customGridConfig.dataSourceType
        5 == customGridConfig.columns.size()

        //verify type
        'id' == customGridConfig.columns[0].property
        40 == customGridConfig.columns[0].jqgrid.width

        'name' == customGridConfig.columns[1].property
        'name' == customGridConfig.columns[1].name
        true == customGridConfig.columns[1].jqgrid.editable
        true == customGridConfig.columns[1].jqgrid.editable

        'age' == customGridConfig.columns[3].name
        'author.age.label' == customGridConfig.columns[3].label
        null == customGridConfig.columns[3].property
    }

    @Ignore
    def "testDomainBuilderDynamicGeneration"() {

        expect:
        domainGridConfig.columns != null
//        3 == domainGridConfig.columns.size()

        'id' == domainGridConfig.columns[0].property
        40 == domainGridConfig.columns[0].jqgrid.width

        'testIntProperty' == domainGridConfig.columns[1].property
        'testDomain.testIntProperty.label' == domainGridConfig.columns[1].label
        'testIntProperty' == domainGridConfig.columns[1].name

        'testStringProperty' == domainGridConfig.columns[2].property
        'testStringProperty' == domainGridConfig.columns[2].name
    }

    /// end default values tests-----------------


    def "testSecurity"() {

        when:
        customGridConfig.roles = 'admin1'
        def (params, request, response, session) = TestUtils.mockEasyGridContextHolder()

        def model = service.htmlGridDefinition(customGridConfig)

        then:
        model == null

        when:
        def gridElements = service.gridData(customGridConfig)

        then:
        gridElements == null
    }

    @Ignore
    def "test grid data"() {
        when:
        customGridConfig.roles = 'admin'
        def (params, request, response, session) = TestUtils.mockEasyGridContextHolder()

        def gridElements = service.gridData(customGridConfig)

        then:
        1 == gridElements.target.records
        1 == gridElements.target.page
        1 == gridElements.target.total
        1 == gridElements.target.rows.size()

    }


    @Ignore
    def "Html Grid Definition"() {
        when:
        def (params, request, response, session) = TestUtils.mockEasyGridContextHolder()
        customGridConfig.roles = 'admin'
        def model = service.htmlGridDefinition(customGridConfig)

        then:
        model != null
        customGridConfig == model.gridConfig
    }


}
