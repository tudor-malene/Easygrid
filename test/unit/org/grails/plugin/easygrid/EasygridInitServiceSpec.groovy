package org.grails.plugin.easygrid

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import groovy.time.TimeCategory
import spock.lang.Ignore
import spock.lang.Specification


/**
 * tests the building of grids
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@TestFor(EasygridInitService)
@Mock(TestDomain)
class EasygridInitServiceSpec extends Specification {


    def "test initialization"() {

        given: "initialize a grid config"
        def gridConfigs = TestUtils.generateConfigForGrid(grailsApplication) {
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
                        label 'testLabel'
                        filterClosure {
                            ilike('name', "%${it}%")
                        }
                        jqgrid {
                            editable true
                        }
                        export {
                            width 100
                        }
                    }
                    nation {
                        filterClosure {
                            ilike('nation', "%${it}%")
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
                        filterClosure {
                            eq('age', it as int)
                        }
                        jqgrid {
                            width 110
                        }
                    }
                    birthDate {
                        formatName 'stdDateFormatter'
                        filterClosure {
                            eq('birthDate', it)
                        }
                        jqgrid {
                            width 110
                        }
                    }
                }
            }
        }
        GridConfig customGridConfig = gridConfigs.authorGrid


        expect: "test that the grid was initialized properly"
        1 == gridConfigs.size()
        5 == gridConfigs.authorGrid.columns.size()

        'authorGrid' == customGridConfig.id
        customGridConfig.columns[1]
        customGridConfig.columns['name']
        customGridConfig.columns['name'] == customGridConfig.columns[1]
        customGridConfig.columns.age.property == null
        'birthDate' == customGridConfig.columns.birthDate.property
        'testLabel' == customGridConfig.columns.name.label
        'custom' == customGridConfig.dataSourceType
        'name' == customGridConfig.columns[1].property
        1 == customGridConfig.columns[1].jqgrid.size()
        true == customGridConfig.columns[1].jqgrid.editable
        1 == customGridConfig.columns[1].export.size()
        'testLabel' == customGridConfig.columns[1].label
        150 == customGridConfig.jqgrid.height



        when: "clone"
        GridConfig gridCfg1 = gridConfigs.authorGrid
        GridConfig gridCfg2 = gridCfg1.deepClone()
        then:
        gridCfg1 != gridCfg2


        when: "set some property on the original"
        gridCfg1.jqgrid.height = 200

        then: "test that the cloned version does not 'see' it"
        150 == gridCfg2.jqgrid.height
        gridCfg1.columns.birthDate.jqgrid.width == gridCfg2.columns.birthDate.jqgrid.width
        gridCfg1.columns.birthDate.name == gridCfg2.columns.birthDate.name
        gridCfg1.columns != gridCfg2.columns
        gridCfg1.columns.birthDate != gridCfg2.columns.birthDate
        !(gridCfg1.columns.birthDate.jqgrid.is(gridCfg2.columns.birthDate.jqgrid))
        !(gridCfg1.dynamicProperties.is(gridCfg2.dynamicProperties))


        when:
        gridCfg1.xx = 0

        then:
        gridCfg2.xx == null
        gridCfg1.columns[0] != gridCfg2.columns[0]



        when:
        gridCfg1.columns[0].xx = 0

        then:
        gridCfg2.columns[0].xx == null


    }


    def "test failed initialization"() {
/*
        when:
        service.initializeFromClosure {
            err1GridConfig {
                dataSourceType 'gorm'
            }
        }

        then: "should fail at the validation stage"
        thrown(ConfigurationException)
*/


        when: "grid without a valid datasource type"
        TestUtils.generateConfigForGrid(grailsApplication) {
            err2GridConfig {
                dataSourceType 'nonExistent'
            }
        }

        then:
        thrown(AssertionError)

    }

    //move to easygrid service
    @Ignore
    def "test ValueOfFieldForProperties"() {

        when:
        def customGridConfig = gridConfigs.authorGrid
        customGridConfig.formats = [(Calendar): { it.format("MM/dd/yyyy") }]
        def row = [id: 1, name: 'Fyodor Dostoyevsky', nation: 'russian', age: (Calendar.getInstance().get(Calendar.YEAR) - 1821), birthDate: new GregorianCalendar(1821, 10, 11)]

        then:
        'Fyodor Dostoyevsky' == service.valueOfColumn(customGridConfig, customGridConfig.columns.name, row, -1)

        //test Format
        'stdDateFormatter' == customGridConfig.columns.birthDate.formatName
        customGridConfig.columns.birthDate.formatter != null
        '11/11/1821' == service.valueOfColumn(customGridConfig, customGridConfig.columns.birthDate, row, -1)

        //test valueOf on domain type
//        service.addDefaultValues(domainGridConfig, defaultValues)
//        assertEquals 10, service.valueOfColumn(domainGridConfig.columns[1], new TestDomain(testStringProperty: "aa", testIntProperty: 10), -1)
    }

    //move to easygrid service
    @Ignore
    def "test ValueOfFieldForClosures"() {
        expect:
        Calendar.getInstance().get(Calendar.YEAR) - 1821 == service.valueOfColumn(gridConfigs.authorGrid, gridConfigs.authorGrid.columns.age, [id: 1, name: 'Fyodor Dostoyevsky', nation: 'russian', birthDate: new GregorianCalendar(1821, 10, 11)], -1)
    }

    /**
     * test the builder
     * the map representing the config is generated from the closure
     * for the domain type grid
     */
    def "testDomainBuilder"() {

        given:
        def domainGridConfig = TestUtils.generateConfigForGrid(grailsApplication) {
            testDomainGrid {
                dataSourceType 'gorm'
                domainClass TestDomain
            }
        }.testDomainGrid

        expect:
        'testDomainGrid' == domainGridConfig.id
        'gorm' == domainGridConfig.dataSourceType
        TestDomain == domainGridConfig.domainClass
        0 == domainGridConfig.columns.size()
    }


    def "Controller grid initialized properly"() {

        given: "mock the dispatch service"
        service.easygridDispatchService = [invokeMethod: { String name, Object args -> }] as GroovyInterceptable

        when:
        def grids = service.initControllerGrids(new TestDomainController(), TestDomainController)

        then:
        4 == grids.size()

    }

    def "External grid initialized properly"() {

        given: "mock the dispatch service"
        service.easygridDispatchService = [invokeMethod: { String name, Object args -> }] as GroovyInterceptable

        when:
        def grids = service.initControllerGrids(new TestController(), TestController)

        then:
        1 == grids.size()
        'testDomainGrid' == grids.testDomainGrid.id

    }

    def "Closure grids initialized properly"() {

        given: "mock the dispatch service"
        service.easygridDispatchService = [invokeMethod: { String name, Object args -> }] as GroovyInterceptable

        when:
        def grids = service.initControllerGrids(new Test1Controller(), Test1Controller)

        then:
        1 == grids.size()
        'test1Domain' == grids.test1Domain.id

    }

}

@Easygrid(externalGrids = ExternalGrids)
class TestController {
}

class ExternalGrids {
    static grids = {
        testDomainGrid {
            dataSourceType 'gorm'
            domainClass TestDomain
        }
    }
}

@Easygrid
class Test1Controller {

    def test1DomainGrid = {
        dataSourceType 'gorm'
        domainClass TestDomain
    }
}
