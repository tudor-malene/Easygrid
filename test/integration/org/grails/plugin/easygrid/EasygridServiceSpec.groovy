package org.grails.plugin.easygrid

import groovy.time.TimeCategory
import spock.lang.Shared


/**
 * tests for the central service
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class EasygridServiceSpec extends AbstractBaseTest {

    static transactional = true

    @Shared def domainGridConfig
    @Shared def customGridConfig


    def initGrids() {
        domainGridConfig = generateConfigForGrid {
            id 'testDomainGrid'
            dataSourceType 'domain'
            domainClass TestDomain
        }

        //initialize the custom grid
        customGridConfig = generateConfigForGrid {
            id 'authorGrid'
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

    }


    def "test label generation"() {
        when: "use the default label prefix"
        def simpleGridConfig = generateConfigForGrid {
            id 'simpleGrid'
            dataSourceType 'domain'
            domainClass TestDomain
            columns {
                testStringProperty
                testIntProperty
            }
        }
        easygridService.addDefaultValues(simpleGridConfig, defaultValues)

        then:
        'testDomain.testStringProperty.label' == simpleGridConfig.columns[0].label
        'testStringProperty' == simpleGridConfig.columns[0].property

        and:
        'testDomain.testIntProperty.label' == simpleGridConfig.columns[1].label
        'testIntProperty' == simpleGridConfig.columns[1].property


        when: "use a custom label prefix"
        simpleGridConfig = generateConfigForGrid {
            id 'simpleGrid'
            dataSourceType 'domain'
            domainClass TestDomain
            labelPrefix 'testDomainPrefix'
            columns {
                testStringProperty
                testIntProperty
            }
        }
        easygridService.addDefaultValues(simpleGridConfig, defaultValues)

        then:
        'testDomainPrefix.testStringProperty.label' == simpleGridConfig.columns[0].label
        'testDomainPrefix.testIntProperty.label' == simpleGridConfig.columns[1].label
    }

    /**
     * test the builder
     * the map representing the config is generated from the closure
     * for the list type grid
     */
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
        when:
        easygridService.addDefaultValues(customGridConfig, defaultValues)

        then:
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

    def "testDomainBuilderDynamicGeneration"() {

        when:
        easygridService.addDefaultValues(domainGridConfig, defaultValues)

        then:
        domainGridConfig.columns != null
        3 == domainGridConfig.columns.size()

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
        easygridService.addDefaultValues(customGridConfig, defaultValues)
        customGridConfig.roles = 'admin1'

        def model = easygridService.htmlGridDefinition(customGridConfig)

        then:
        model == null

        when:
        def gridElements = easygridService.gridData(customGridConfig)

        then:
        gridElements==null
    }

    def "test grid data"(){
        when:
        params.clear()
        customGridConfig.roles = 'admin'
        easygridService.addDefaultValues(customGridConfig, defaultValues)
        def gridElements = easygridService.gridData(customGridConfig)

        then:
        1 == gridElements.target.records
        1 == gridElements.target.page
        1 == gridElements.target.total
        1 == gridElements.target.rows.size()

    }


    def "Html Grid Definition"() {
        when:
        customGridConfig.roles = 'admin'
        easygridService.addDefaultValues(customGridConfig, defaultValues)
        def model = easygridService.htmlGridDefinition(customGridConfig)

        then:
        model !=null
        customGridConfig == model.gridConfig
    }


}
