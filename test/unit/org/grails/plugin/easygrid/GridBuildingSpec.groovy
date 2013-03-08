package org.grails.plugin.easygrid

import groovy.text.SimpleTemplateEngine
import groovy.time.TimeCategory
import org.codehaus.groovy.control.ConfigurationException
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.grails.plugin.easygrid.builder.EasygridBuilder
import org.grails.plugin.easygrid.datasource.CustomDatasourceService
import org.grails.plugin.easygrid.datasource.GormDatasourceService
import org.grails.plugin.easygrid.grids.JqueryGridService
import spock.lang.Specification

import static org.junit.Assert.assertEquals

/**
 * tests the building of grids
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class GridBuildingSpec extends Specification {

    def gridConfigs
    def mockGrailsApplication
    def easygridService
    def defaultValues

    def setupSpec(){
        GridUtils.addMixins()
    }

    def setup() {
        mockGrailsApplication = Mock(GrailsApplication)
        mockGrailsApplication.config >>
                [
                        easygrid: [
                                defaults: [gridImpl: 'jqgrid', labelFormatTemplate: new SimpleTemplateEngine().createTemplate('')],
                                gridImplementations: [jqgrid: [gridImplService: JqueryGridService, gridRenderer: '/templates/test']],
                                dataSourceImplementations: [
                                        custom: [dataSourceService: CustomDatasourceService],
                                        gorm: [dataSourceService: GormDatasourceService],
                                ],
                                columns: [
                                        types: [id: [:]],
                                        defaults: [:],
                                ],
                                formats: [
                                        stdDateFormatter: { it.format('MM/dd/yyyy') }
                                ]
                        ]
                ]

        easygridService = Spy(EasygridService)
        easygridService.dataSourceService >> Spy(CustomDatasourceService)
        easygridService.implService >> Spy(JqueryGridService)
        easygridService.exportService >> Spy(EasygridExportService)
        easygridService.grailsApplication >> mockGrailsApplication
        defaultValues = mockGrailsApplication.config.easygrid

        gridConfigs = new EasygridBuilder(mockGrailsApplication).evaluate {
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
    }

    def "test Cloning"() {

        given:
        GridConfig gridCfg1 = gridConfigs.authorGrid
        GridConfig gridCfg2 = gridCfg1.deepClone()


        expect:
        1 == gridConfigs.size()
        gridCfg1 != gridCfg2


        when:
        gridCfg1.jqgrid.height = 200

        then:
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

    def "test Grid initialization"() {

        when:
        GridConfig gridCfg = gridConfigs.authorGrid
        gridCfg.id = 'authorGrid'
        easygridService.addDefaultValues(gridCfg, defaultValues)

        then:
        gridCfg.columns[1]
        gridCfg.columns['name']
        gridCfg.columns['name'] == gridCfg.columns[1]
        gridCfg.columns.age.property == null
        'birthDate' == gridCfg.columns.birthDate.property
        'testLabel' == gridCfg.columns.name.label
        //todo - add more
    }

    def "test failed initialization"() {
        when:
        def err1GridConfig = new EasygridBuilder(mockGrailsApplication).evaluateGrid {
            id 'err1GridConfig'
            dataSourceType 'gorm'
        }
        easygridService.addDefaultValues(err1GridConfig, defaultValues)

        then: "should fail at the validation stage"
        thrown(ConfigurationException)


        when: "grid without a valid datasource type"
        def err2GridConfig = new EasygridBuilder(mockGrailsApplication).evaluateGrid {
            id 'err2GridConfig'
            dataSourceType 'nonExistent'
        }
        easygridService.addDefaultValues(err2GridConfig, defaultValues)

        then:
        thrown(AssertionError)

    }


    def "test Custom Builder"() {

        when:
        def customGridConfig = gridConfigs.authorGrid
        customGridConfig.id = 'authorGrid'
        easygridService.addDefaultValues(customGridConfig, defaultValues)

        then:
        'authorGrid' == customGridConfig.id
        'custom' == customGridConfig.dataSourceType
        'name' == customGridConfig.columns[1].property
        1 == customGridConfig.columns[1].jqgrid.size()
        true == customGridConfig.columns[1].jqgrid.editable
        1 == customGridConfig.columns[1].export.size()
        'testLabel' == customGridConfig.columns[1].label
        150 == customGridConfig.jqgrid.height
    }


    def "test ValueOfFieldForProperties"() {

        when:
        def customGridConfig = gridConfigs.authorGrid
        customGridConfig.id = 'authorGrid'
        easygridService.addDefaultValues(customGridConfig, defaultValues)
        customGridConfig.formats = ["java.util.Calendar": { it.format("MM/dd/yyyy") }]
        def row = [id: 1, name: 'Fyodor Dostoyevsky', nation: 'russian', age: (Calendar.getInstance().get(Calendar.YEAR) - 1821), birthDate: new GregorianCalendar(1821, 10, 11)]

        then:
        'Fyodor Dostoyevsky' == easygridService.valueOfColumn(customGridConfig.columns.name, row, -1)

        //test Format
        'stdDateFormatter' == customGridConfig.columns.birthDate.formatName
        customGridConfig.columns.birthDate.formatter != null
        '11/11/1821' == easygridService.valueOfColumn(customGridConfig.columns.birthDate, row, -1)

        //test valueOf on domain type
//        easygridService.addDefaultValues(domainGridConfig, defaultValues)
//        assertEquals 10, easygridService.valueOfColumn(domainGridConfig.columns[1], new TestDomain(testStringProperty: "aa", testIntProperty: 10), -1)
    }

    def "test ValueOfFieldForClosures"() {
        expect:
        Calendar.getInstance().get(Calendar.YEAR) - 1821 == easygridService.valueOfColumn(gridConfigs.authorGrid.columns.age, [id: 1, name: 'Fyodor Dostoyevsky', nation: 'russian', birthDate: new GregorianCalendar(1821, 10, 11)], -1)
    }

    /**
     * test the builder
     * the map representing the config is generated from the closure
     * for the domain type grid
     */
    def "testDomainBuilder"() {

        given:
        def domainGridConfig = new EasygridBuilder(mockGrailsApplication).evaluateGrid {
            id 'testDomainGrid'
            dataSourceType 'domain'
            domainClass TestDomain
        }

        expect:
        'testDomainGrid' == domainGridConfig.id
        'domain' == domainGridConfig.dataSourceType
        TestDomain == domainGridConfig.domainClass
        0 == domainGridConfig.columns.size()
    }


}
