package org.grails.plugin.easygrid

import static org.junit.Assert.*
import grails.test.mixin.Mock
import grails.test.mixin.TestFor

import org.junit.Before

/**
 * test the Gorm impl
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Mock(TestDomain)
@TestFor(TestDomainController)
class GormDatasourceServiceTests extends AbstractServiceTest {

    def domainGridConfig
    def criteriaGridConfig

    def gormDatasourceService
    def N = 100

    @Before
    void setUp() {
        super.setup()

        domainGridConfig = generateConfigForGrid {
            id 'testDomainGrid'
            labelPrefix ''
            dataSourceType 'domain'
            domainClass TestDomain
            columns {
                id {
                    type 'id'
                }
                testStringProperty {
                    filterClosure { params ->
                        ilike('testStringProperty', "%${params.testStringProperty}%")
                    }
                    jqgrid {
                    }
                }
                testIntProperty {
                    filterClosure {  params ->
                        eq('testIntProperty', params.'testIntProperty' as int)
                    }
                    jqgrid {
                    }
                }
            }
        }

        criteriaGridConfig = generateConfigForGrid {
            id 'testDomainGrid'
            labelPrefix ''
            dataSourceType 'domain'
            domainClass TestDomain
            initialCriteria {
                between("testIntProperty", 21, 40)
            }
            columns {
                id {
                    type 'id'
                }
                testStringProperty {
                    filterClosure { params ->
                        ilike('testStringProperty', "%${params.testStringProperty}%")
                    }
                }
                testIntProperty {
                    filterClosure {  params ->
                        eq('testIntProperty', params.'testIntProperty' as int)
                    }
                }
            }
        }

        populateTestDomain(N)
    }

    void testCriteriaDataSource() {
        easygridService.addDefaultValues(criteriaGridConfig, defaultValues)

        assertEquals 20, gormDatasourceService.countRows()
        def domainRows = gormDatasourceService.list()
        assertEquals 20, domainRows.size()

        //test max rows & page
//        max: maxRows, offset: rowOffset, sort: sort, order: order)
        domainRows = gormDatasourceService.list([maxRows: 10, rowOffset: 10])
        assertEquals 10, domainRows.size()
        assertEquals 31, domainRows[0].testIntProperty

        //test criteria search
        domainRows = gormDatasourceService.list([maxRows: 10, rowOffset: 0], [{params -> between("testIntProperty", 31, 80)}])
        assertEquals 10, domainRows.size()
        assertEquals 31, domainRows[0].testIntProperty
    }

    void testDomainDataSource() {
        easygridService.addDefaultValues(domainGridConfig, defaultValues)

        assertEquals N, gormDatasourceService.countRows()
        def domainRows = gormDatasourceService.list()
        assertEquals N, domainRows.size()

        //test max rows & page
//        max: maxRows, offset: rowOffset, sort: sort, order: order)
        domainRows = gormDatasourceService.list([maxRows: 10, rowOffset: 10])
        assertEquals 10, domainRows.size()
        assertEquals 11, domainRows[0].testIntProperty

        //test criteria search
        domainRows = gormDatasourceService.list([maxRows: 10, rowOffset: 10], [{params -> between("testIntProperty", 31, 80)}])
        assertEquals 10, domainRows.size()
        assertEquals 41, domainRows[0].testIntProperty
    }

    void testGormSearch() {
        easygridService.addDefaultValues(domainGridConfig, defaultValues)

        params.testStringProperty = 1

        assertEquals 20, gormDatasourceService.countRows(domainGridConfig.columns[1].filterClosure )

        assertArrayEquals(
                [14, 15, 16, 17, 18].toArray(),
                gormDatasourceService.list([maxRows: 5, rowOffset: 5, sort: 'testIntProperty'], [domainGridConfig.columns[1].filterClosure]).collect {it.testIntProperty}.toArray()
        )

        params.testIntProperty = 100
        assertArrayEquals(
                [100].toArray(),
                gormDatasourceService.list([maxRows: 5,  sort: 'testIntProperty'], [domainGridConfig.columns[1].filterClosure, domainGridConfig.columns[2].filterClosure,]).collect {it.testIntProperty}.toArray())
    }

    void testGormDelete() {
        easygridService.addDefaultValues(domainGridConfig, defaultValues)

        //delete
        params.id = 1
        gormDatasourceService.delRow()
        assertEquals N - 1, gormDatasourceService.countRows()
    }

    void testGormUpdate() {
        easygridService.addDefaultValues(domainGridConfig, defaultValues)

        //update
        def before = TestDomain.get(2)
        assertEquals(2, before.testIntProperty)
        assertEquals '2', before.testStringProperty

        params.id = 2
        params.testIntProperty = -2
        params.testStringProperty = 'two'
        gormDatasourceService.updateRow()

        def instance = TestDomain.get(2)
        assertEquals(-2, instance.testIntProperty)
        assertEquals 'two', instance.testStringProperty
    }

    //add
    void testGormAdd() {
        easygridService.addDefaultValues(domainGridConfig, defaultValues)

        assertEquals N, gormDatasourceService.countRows()

        params.testIntProperty = 101
        params.testStringProperty = '101'
        gormDatasourceService.saveRow()

        assertEquals N + 1, gormDatasourceService.countRows()
        def instance = TestDomain.get(101)
        assertEquals(101, instance.testIntProperty)
    }
}
