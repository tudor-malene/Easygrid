package org.grails.plugin.easygrid

import static org.junit.Assert.*
import grails.test.mixin.Mock
import grails.test.mixin.TestFor

import org.codehaus.groovy.control.ConfigurationException
import org.junit.Before
import groovy.time.TimeCategory
import org.grails.plugin.easygrid.builder.EasygridBuilder

/**
 * tests for the central service
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Mock(TestDomain)
@TestFor(TestDomainController)
class EasygridServiceTests extends AbstractServiceTest {

    def domainGridConfig
    def err1GridConfig
    def err2GridConfig

    def simpleGridConfig

    @Before
    void setUp() {
        super.setup()

        domainGridConfig = generateConfigForGrid {
            id 'testDomainGrid'
            dataSourceType 'domain'
            domainClass TestDomain
        }

        err1GridConfig = generateConfigForGrid {
            id 'err1GridConfig'
            dataSourceType 'domain'
        }

        err2GridConfig = generateConfigForGrid {
            id 'err2GridConfig'
            dataSourceType 'nonExistent'
        }
    }

    /// start builder tests-----------------

    void testCloning() {

        def gridConfigs = new EasygridBuilder(grailsApplication).evaluate {
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

        assertEquals 1, gridConfigs.size()

        def gridCfg1 = gridConfigs.authorGrid
        def gridCfg2 = gridConfigs.authorGrid.deepClone()
        assertNotSame gridCfg1, gridCfg2


        assertNotSame gridCfg1.columns, gridCfg2.columns
        assertNotSame gridCfg1.dynamicProperties, gridCfg2.dynamicProperties

        gridCfg1.xx = 0
        assertNull gridCfg2.xx

        assertNotSame gridCfg1.columns[0], gridCfg2.columns[0]

        gridCfg1.columns[0].xx = 0
        assertNull gridCfg2.columns[0].xx


    }

    void testNewColumn() {

//        defaultValues.columns.buildStyle = 'columnNameAsHeader'
        def gridConfigs = new EasygridBuilder(grailsApplication).evaluate {
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


        assertEquals 1, gridConfigs.size()

        GridConfig gridCfg = gridConfigs.authorGrid
        gridCfg.id = 'authorGrid'

        easygridService.addDefaultValues(gridCfg, defaultValues)
        assertNotNull gridCfg.columns[1]
        assertNotNull gridCfg.columns['name']
        assertEquals gridCfg.columns['name'], gridCfg.columns[1]
        assertNull gridCfg.columns.age.property
        assertEquals 'birthDate', gridCfg.columns.birthDate.property
        assertEquals 'testLabel', gridCfg.columns.name.label

    }



    void testSimpleGrid() {
        simpleGridConfig = generateConfigForGrid {
            id 'simpleGrid'
            dataSourceType 'domain'
            domainClass TestDomain
            columns {
                testStringProperty
                testIntProperty
            }
        }
        easygridService.addDefaultValues(simpleGridConfig, defaultValues)
        assertEquals 'testDomain.testStringProperty.label', simpleGridConfig.columns[0].label
        assertEquals 'testStringProperty', simpleGridConfig.columns[0].property

        assertEquals 'testDomain.testIntProperty.label', simpleGridConfig.columns[1].label
        assertEquals 'testIntProperty', simpleGridConfig.columns[1].property

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
        assertEquals 'testDomainPrefix.testStringProperty.label', simpleGridConfig.columns[0].label
        assertEquals 'testDomainPrefix.testIntProperty.label', simpleGridConfig.columns[1].label
    }

    /**
     * test the builder
     * the map representing the config is generated from the closure
     * for the custom type grid
     */
    void testCustomBuilder() {

        easygridService.addDefaultValues(customGridConfig, defaultValues)
//        assertEquals 8, customGridConfig.size()         --todo
        assertEquals 'authorGrid', customGridConfig.id
        assertEquals 'custom', customGridConfig.dataSourceType
//        assertEquals 5, customGridConfig.columns.size() --todo

//        assertEquals 2, customGridConfig.columns[0].size() // column id [label, type] -- todo

//        assertEquals 4, customGridConfig.columns[1].size()
        assertEquals 'name', customGridConfig.columns[1].property
        assertEquals 1, customGridConfig.columns[1].jqgrid.size()
        assertEquals true, customGridConfig.columns[1].jqgrid.editable
        assertEquals 1, customGridConfig.columns[1].export.size()
        assertEquals 'author.name.label', customGridConfig.columns[1].label

//        assertEquals 2, customGridConfig.jqgrid.size()--todo
        assertEquals 150, customGridConfig.jqgrid.height
    }

    void testGridCheck() {
        shouldFail(ConfigurationException) {
            easygridService.addDefaultValues(err1GridConfig, defaultValues)
        }
//        assertEquals 1, easygridService.verifyGridConstraints(err1GridConfig).size()

        shouldFail(ConfigurationException) {
            easygridService.addDefaultValues(err1GridConfig, defaultValues)
        }
        //        assertEquals 1, easygridService.verifyGridConstraints(err2GridConfig).size()
    }

    /**
     * test the builder
     * the map representing the config is generated from the closure
     * for the domain type grid
     */
    void testDomainBuilder() {

//        assertEquals 3, domainGridConfig.size()--todo
        assertEquals 'testDomainGrid', domainGridConfig.id
        assertEquals 'domain', domainGridConfig.dataSourceType
        assertEquals TestDomain, domainGridConfig.domainClass
        assertEquals 0, domainGridConfig.columns.size()
    }

    /**
     * test the builder
     * the map representing the config is generated from the closure
     * for the list type grid
     */
    void testListBuilder() {

        assertEquals 'listProviderGrid', listGridConfig.id
        assertEquals 'list', listGridConfig.dataSourceType
        assertEquals 3, listGridConfig.columns.size()
    }

    /// end builder tests-----------------

    /// start default values tests-----------------

    /**
     * test that default values are injected correctly into the configuration
     */
    void testCustomBuilderAndDefaultValues() {
        easygridService.addDefaultValues(customGridConfig, defaultValues)

//        assertEquals 19, customGridConfig.size()      --todo
        assertEquals 'authorGrid', customGridConfig.id
        assertEquals 'custom', customGridConfig.dataSourceType
        assertEquals 5, customGridConfig.columns.size()

        //verify type
        assertEquals 'id', customGridConfig.columns[0].property
        assertEquals 40, customGridConfig.columns[0].jqgrid.width

        assertEquals 'name', customGridConfig.columns[1].property
        assertEquals 'name', customGridConfig.columns[1].name
        assertEquals true, customGridConfig.columns[1].jqgrid.editable
        assertEquals true, customGridConfig.columns[1].jqgrid.editable

        assertEquals 'age', customGridConfig.columns[3].name
        assertEquals 'author.age.label', customGridConfig.columns[3].label
        assertNull customGridConfig.columns[3].property
    }

    void testDomainBuilderDynamicGeneration() {

        easygridService.addDefaultValues(domainGridConfig, defaultValues)

        assertNotNull domainGridConfig.columns
        assertEquals 3, domainGridConfig.columns.size()

        assertEquals 'id', domainGridConfig.columns[0].property
        assertEquals 40, domainGridConfig.columns[0].jqgrid.width

        assertEquals 'testIntProperty', domainGridConfig.columns[1].property
        assertEquals 'testDomain.testIntProperty.label', domainGridConfig.columns[1].label
        assertEquals 'testIntProperty', domainGridConfig.columns[1].name

        assertEquals 'testStringProperty', domainGridConfig.columns[2].property
        assertEquals 'testStringProperty', domainGridConfig.columns[2].name
    }

    /// end default values tests-----------------

    /// start valueOfColumn tests-----------------

    void testValueOfFieldForProperties() {

//        EasygridContextHolder.setLocalGridConfig(customGridConfig)
        easygridService.addDefaultValues(customGridConfig, defaultValues)
        customGridConfig.formats = [(Calendar): { it.format("dd/MM/yyyy") }]

        assertEquals 'Fyodor Dostoyevsky', easygridService.valueOfColumn(customGridConfig.columns[1], [id: 1, name: 'Fyodor Dostoyevsky', nation: 'russian', age: (Calendar.getInstance().get(Calendar.YEAR) - 1821), birthDate: new GregorianCalendar(1821, 10, 11)], -1)

        //test Format
        assertEquals 'stdDateFormatter', customGridConfig.columns[4].formatName
        assert customGridConfig.columns[4].formatter
        assertEquals '11/11/1821', easygridService.valueOfColumn(customGridConfig.columns[4], [id: 1, name: 'Fyodor Dostoyevsky', nation: 'russian', age: (Calendar.getInstance().get(Calendar.YEAR) - 1821), birthDate: new GregorianCalendar(1821, 10, 11)], -1)

        //test valueOf on domain type
        easygridService.addDefaultValues(domainGridConfig, defaultValues)
        assertEquals 10, easygridService.valueOfColumn(domainGridConfig.columns[1], new TestDomain(testStringProperty: "aa", testIntProperty: 10), -1)
    }

    void testValueOfFieldForClosures() {
        assertEquals Calendar.getInstance().get(Calendar.YEAR) - 1821, easygridService.valueOfColumn(customGridConfig.columns[3], [id: 1, name: 'Fyodor Dostoyevsky', nation: 'russian', birthDate: new GregorianCalendar(1821, 10, 11)], -1)
    }

    /// end valueOfColumn tests-----------------

    void testSecurity() {

        easygridService.addDefaultValues(customGridConfig, defaultValues)
        customGridConfig.roles = 'admin1'

        def model = easygridService.htmlGridDefinition(customGridConfig)
        assertNull model

        def gridElements = easygridService.gridData(customGridConfig)
        assertNull gridElements

        TestDomain.list(sort: 'aa')
    }

    void testRestoreParamsScenario() {
/*
        populateTestDomain()
        def controller = new TestDomainController()
        easygridService.initGrids(controller)
        def gridConfig = controller.gridsConfig.test1

        println gridConfig

//        1) intru pe o pagina ( salvez params)
        params.param1 =1
        easygridService.gridData(gridConfig)

//        2) cautare
        params.param1 =1
        params.param2 =2
        easygridService.gridData(gridConfig)
*/

//        3) schimb pagina ( ? se pastreaza cautarea )
// da

//        4) intru pe un update

//        5) ma intorc la grid ( vreau parametrii de la 3 )

//        6) fac un export ( vreau parametrii de la 5)
    }
}
