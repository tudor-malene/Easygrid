package org.grails.plugin.easygrid

import grails.gorm.DetachedCriteria
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.grails.plugin.easygrid.datasource.GormDatasourceService
import spock.lang.Specification
import static org.grails.plugin.easygrid.TestUtils.*

import static org.junit.Assert.assertEquals

/**
 * test the Gorm impl
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@TestFor(GormDatasourceService)
@Mock([TestDomain, PetTest, OwnerTest])
class GormDatasourceServiceSpec extends Specification {

    GridConfig domainGridConfig
    GridConfig criteriaGridConfig

    static int N = 100

    def setup() {
        domainGridConfig = TestUtils.generateConfigForGrid(grailsApplication, service) {
            'testDomainGrid' {
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
        }.testDomainGrid

        criteriaGridConfig = TestUtils.generateConfigForGrid(grailsApplication, service) {
            'testDomainGrid' {
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
        }.testDomainGrid
    }


    def "testCriteriaDataSource"() {
        given:
        populateTestDomain(N)
        def (params, request, response, session) = TestUtils.mockEasyGridContextHolder()

        expect:
        20 == service.countRows(criteriaGridConfig)

        when:
        def domainRows = service.list(criteriaGridConfig)

        then:
        20 == domainRows.size()

        //test max rows & page
//        max: maxRows, offset: rowOffset, sort: sort, order: order)
        when:
        domainRows = service.list(criteriaGridConfig, [maxRows: 10, rowOffset: 10])
        then:
        10 == domainRows.size()
        31 == domainRows[0].testIntProperty

        //test criteria search
        when:
        domainRows = service.list(criteriaGridConfig, [maxRows: 10, rowOffset: 0],
                [new Filter({ between("testIntProperty", 31, 80) }, '1')]
        )
        then:
        10 == domainRows.size()
        31 == domainRows[0].testIntProperty
    }

    def "testDomainDataSource"() {
        given:
        populateTestDomain(N)
        def (params, request, response, session) = TestUtils.mockEasyGridContextHolder()

        expect:
        N == service.countRows(domainGridConfig)

        when:
        def domainRows = service.list(domainGridConfig)
        then:
        N == domainRows.size()

        //test max rows & page
//        max: maxRows, offset: rowOffset, sort: sort, order: order)
        when:
        domainRows = service.list(domainGridConfig, [maxRows: 10, rowOffset: 10])
        then:
        10 == domainRows.size()
        11 == domainRows[0].testIntProperty

        //test criteria search
        when:
        domainRows = service.list(domainGridConfig, [maxRows: 10, rowOffset: 10], [new Filter({ filter -> between("testIntProperty", 31, 80) })])
        then:
        10 == domainRows.size()
        41 == domainRows[0].testIntProperty
    }

    def "testGormSearch"() {
        given:
        populateTestDomain(N)
        def (params, request, response, session) = TestUtils.mockEasyGridContextHolder()

        when:
        params.testStringProperty = 1

        //todo - conversion, move this to  config
//        domainGridConfig.columns.testStringProperty.filterClosure = { Filter filter -> ilike(filter.column.name, "%${filter.paramValue}%") }
//        domainGridConfig.columns.testIntProperty.filterClosure = { Filter filter -> eq(filter.column.name, filter.paramValue as int) }

        then:
        20 == service.countRows(domainGridConfig, [new Filter(domainGridConfig.columns.testStringProperty)])


        [14, 15, 16, 17, 18] == service.list(domainGridConfig,
                [maxRows: 5, rowOffset: 5, sort: 'testIntProperty'],
                [new Filter(domainGridConfig.columns.testStringProperty)]
        ).collect { it.testIntProperty }


        when:
        params.testIntProperty = 100
        then:

        [100] == service.list(domainGridConfig,
                [maxRows: 5, sort: 'testIntProperty'],
                [new Filter(domainGridConfig.columns.testStringProperty), new Filter(domainGridConfig.columns.testIntProperty),]
        ).collect { it.testIntProperty }
    }

    def "test Gorm operations"() {
        given:
        populateTestDomain(N)
        def (params, request, response, session) = TestUtils.mockEasyGridContextHolder()

        when: "delete"
        params.id = TestDomain.findByTestIntProperty(1).id
        service.delRow(domainGridConfig)
        then:
        N - 1 == service.countRows(domainGridConfig)

        when: "before update"
        def before = TestDomain.findByTestIntProperty(2)
        then:
        2 == before.testIntProperty
        '2' == before.testStringProperty

        when: "update"
        params.id = TestDomain.findByTestIntProperty(2).id
        params.testIntProperty = -2
        params.testStringProperty = 'two'
        service.updateRow(domainGridConfig)
        def instance = TestDomain.get(params.id)

        then:
        -2 == instance.testIntProperty
        'two' == instance.testStringProperty

        expect:
        N - 1 == service.countRows(domainGridConfig)

        when: "add"
        params.testIntProperty = 101
        params.testStringProperty = '101'
        def errors = service.saveRow(domainGridConfig)
        instance = TestDomain.findByTestIntProperty(101)

        then:
        N == service.countRows(domainGridConfig)
        '101' == instance.testStringProperty
    }



    def "test Complex Query"() {
        given:
        populatePets()
        def (params, request, response, session) = TestUtils.mockEasyGridContextHolder()

        def petsGridConfig = TestUtils.generateConfigForGrid(grailsApplication) {
            'petsGridConfig' {
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
        }.petsGridConfig


        expect:
        'owner.name' == petsGridConfig.columns['owner.name'].property
        'owner.city' == petsGridConfig.columns['owner.city'].property

        when:
        def data = service.list(petsGridConfig)
        then:
        5 == data.size()
        'Bonkers' == data[0].name

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
