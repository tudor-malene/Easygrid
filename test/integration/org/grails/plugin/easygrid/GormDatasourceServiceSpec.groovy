package org.grails.plugin.easygrid

import grails.gorm.DetachedCriteria
import spock.lang.Shared

import static org.junit.Assert.*

/**
 * test the Gorm impl
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class GormDatasourceServiceSpec extends AbstractBaseTest {

    static transactional = true

    @Shared GridConfig domainGridConfig
    @Shared GridConfig criteriaGridConfig

    def gormDatasourceService

    static int N = 100

    def initGrids() {
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
                    enableFilter true
//                    filterFieldType 'text'
                    jqgrid {
                    }
                }
                testIntProperty {
                    enableFilter true
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
                    filterClosure { filter ->
                        ilike('testStringProperty', "%${filter.paramValue}%")
                    }
                }
                testIntProperty {
                    filterClosure { filter ->
                        eq('testIntProperty', filter.paramValue as int)
                    }
                }
            }
        }
    }


    def "testCriteriaDataSource"() {
        given:
        populateTestDomain(N)
        easygridService.addDefaultValues(criteriaGridConfig, defaultValues)

        expect:
        20 == gormDatasourceService.countRows()

        when:
        def domainRows = gormDatasourceService.list()

        then:
        20 == domainRows.size()

        //test max rows & page
//        max: maxRows, offset: rowOffset, sort: sort, order: order)
        when:
        domainRows = gormDatasourceService.list([maxRows: 10, rowOffset: 10])
        then:
        10 == domainRows.size()
        31 == domainRows[0].testIntProperty

        //test criteria search
        when:
        domainRows = gormDatasourceService.list([maxRows: 10, rowOffset: 0],
                [new Filter({ params -> between("testIntProperty", 31, 80) }, '1')]
        )
        then:
        10 == domainRows.size()
        31 == domainRows[0].testIntProperty
    }

    def "testDomainDataSource"() {
        given:
        populateTestDomain(N)
        easygridService.addDefaultValues(domainGridConfig, defaultValues)

        expect:
        N == gormDatasourceService.countRows()

        when:
        def domainRows = gormDatasourceService.list()
        then:
        N == domainRows.size()

        //test max rows & page
//        max: maxRows, offset: rowOffset, sort: sort, order: order)
        when:
        domainRows = gormDatasourceService.list([maxRows: 10, rowOffset: 10])
        then:
        10 == domainRows.size()
        11 == domainRows[0].testIntProperty

        //test criteria search
        when:
        domainRows = gormDatasourceService.list([maxRows: 10, rowOffset: 10], [new Filter({ filter -> between("testIntProperty", 31, 80) })])
        then:
        10 == domainRows.size()
        41 == domainRows[0].testIntProperty
    }

    def "testGormSearch"() {
        given:
        populateTestDomain(N)
        easygridService.addDefaultValues(domainGridConfig, defaultValues)

        when:
        params.testStringProperty = 1

        //todo - conversion, move this to  config
//        domainGridConfig.columns.testStringProperty.filterClosure = { Filter filter -> ilike(filter.column.name, "%${filter.paramValue}%") }
//        domainGridConfig.columns.testIntProperty.filterClosure = { Filter filter -> eq(filter.column.name, filter.paramValue as int) }

        then:
        20 == gormDatasourceService.countRows([new Filter(domainGridConfig.columns.testStringProperty)])


        [14, 15, 16, 17, 18] == gormDatasourceService.list(
                [maxRows: 5, rowOffset: 5, sort: 'testIntProperty'],
                [new Filter(domainGridConfig.columns.testStringProperty)]
        ).collect { it.testIntProperty }


        when:
        params.testIntProperty = 100
        then:

        [100]== gormDatasourceService.list(
                [maxRows: 5, sort: 'testIntProperty'],
                [new Filter(domainGridConfig.columns.testStringProperty), new Filter(domainGridConfig.columns.testIntProperty),]
        ).collect { it.testIntProperty }
    }

    def "test Gorm operations"() {
        given:
        populateTestDomain(N)
        easygridService.addDefaultValues(domainGridConfig, defaultValues)

        when: "delete"
        params.id = TestDomain.findByTestIntProperty(1).id
        gormDatasourceService.delRow()
        then:
        N - 1 == gormDatasourceService.countRows()

        when:"before update"
        def before = TestDomain.findByTestIntProperty(2)
        then:
        2 == before.testIntProperty
        '2' == before.testStringProperty

        when:"update"
        params.id = TestDomain.findByTestIntProperty(2).id
        params.testIntProperty = -2
        params.testStringProperty = 'two'
        gormDatasourceService.updateRow()
        def instance = TestDomain.get(params.id)

        then:
        -2 == instance.testIntProperty
        'two'== instance.testStringProperty

        expect:
        N-1 == gormDatasourceService.countRows()

        when: "add"
        params.testIntProperty = 101
        params.testStringProperty = '101'
        def errors = gormDatasourceService.saveRow()
        instance = TestDomain.findByTestIntProperty(101)

        then:
        N == gormDatasourceService.countRows()
        '101' == instance.testStringProperty
    }



    def "test Complex Query"() {
        given:
        populatePets()
        def petsGridConfig = generateConfigForGrid {
            id 'petsGridConfig'
            dataSourceType 'gorm'
            domainClass PetTest
//            initialCriteria{
//                createAlias "owner", "o"
//            }
            columns {
                id {
                    type 'id'
                }
                name {
                    enableFilter true
//                    filterFieldType 'text'
                    jqgrid {
                    }
                }
                'owner.name' {
//                    name 'o.name'
//                    property 'owner.name'
                    enableFilter true
                    jqgrid {
                    }
                }
                'owner.city' {
                    enableFilter true
                    jqgrid {
                    }
                }
            }
        }

        easygridService.addDefaultValues(petsGridConfig, defaultValues)

        expect:
        'owner.name' == petsGridConfig.columns['owner.name'].property
        'owner.city' == petsGridConfig.columns['owner.city'].property

        when:
        def data = easygridService.gridData(petsGridConfig)
        then:
        5 == data.target.rows.size()
        'Bonkers' == data.target.rows[0].cell[1]
        'John' == data.target.rows[0].cell[2]

//        params.ownerName='John'
//        params._search='true'
//        data = easygridService.gridData(petsGridConfig)
//        println data

        // sort by owner name
//        when:
//        params.sidx = 'owner.name'
//        data = easygridService.gridData(petsGridConfig)
//        then:
//        5 == data.target.rows.size()
//        'severin' == data.target.rows[0].cell[1]
    }

    //utility
    private void populatePets() {
        def john = new OwnerTest(name: 'John', city: 'NY').save()
        def mary = new OwnerTest(name: 'Mary', city: 'NJ').save()
        def joe = new OwnerTest(name: 'Joe', city: 'LA').save()

        def bonkers = new PetTest(name: 'Bonkers', owner: john).save()
        def tommy = new PetTest(name: 'tommy', owner: john).save()
        def pandora = new PetTest(name: 'pandora', owner: mary).save()
        def wanikiy = new PetTest(name: 'wanikiy', owner: mary).save()
        def severin = new PetTest(name: 'severin', owner: joe).save()

        john.addToPets(bonkers).addToPets(tommy).save()
        mary.addToPets(pandora).addToPets(wanikiy).save()
        joe.addToPets(severin).save()

        assertEquals 3, OwnerTest.count()
        assertEquals 2, OwnerTest.findByName('John').pets.size()
    }

    void testStuff() {
        populatePets()
        DetachedCriteria d = OwnerTest.where {}
        println d.list()
        Closure f = { eq('name', 'John') }
        d = d.where f
        println d.list()

//        OwnerTest.nameQuery.

    }

}
