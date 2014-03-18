package org.grails.plugin.easygrid

import grails.gorm.DetachedCriteria
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.grails.plugin.easygrid.datasource.GormDatasourceService
import spock.lang.Specification
import spock.lang.Unroll

import static org.grails.plugin.easygrid.FilterOperatorsEnum.*
import static org.grails.plugin.easygrid.TestUtils.*

/**
 * test the Gorm impl
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@TestFor(GormDatasourceService)
@Mock([TestDomain, PetTest, OwnerTest])
class GormDatasourceServiceSpec extends Specification {

    def filterService

    GridConfig domainGridConfig
    GridConfig criteriaGridConfig

    static int N = 100

    def setup() {

        filterService = new FilterService()
        service.filterService = filterService
        domainGridConfig = generateConfigForGrid(grailsApplication, service) {
            'testDomainGrid' {
                labelPrefix ''
                dataSourceType 'gorm'
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

        criteriaGridConfig = generateConfigForGrid(grailsApplication, service) {
            'testDomainGrid' {
                labelPrefix ''
                dataSourceType 'gorm'
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
        def (params, request, response, session) = mockEasyGridContextHolder()

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
                filterService.createGlobalFilters { between("testIntProperty", 31, 80) }
        )
        then:
        10 == domainRows.size()
        31 == domainRows[0].testIntProperty
    }

    def "testDomainDataSource"() {
        given:
        populateTestDomain(N)
        def (params, request, response, session) = mockEasyGridContextHolder()

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
        domainRows = service.list(domainGridConfig, [maxRows: 10, rowOffset: 10],
                filterService.createGlobalFilters {
                    between("testIntProperty", 31, 80)
                }
        )
        then:
        10 == domainRows.size()
        41 == domainRows[0].testIntProperty
    }

    def "testGormSearch"() {
        given:
        populateTestDomain(N)
        def (params, request, response, session) = mockEasyGridContextHolder()

        when:
        def filters = new Filters(filters: [filterService.createFilterFromColumn(domainGridConfig, domainGridConfig.columns.testStringProperty, BW, '%1%')])
        def cnt = service.countRows(domainGridConfig, filters)
        def lst = service.list(domainGridConfig, [maxRows: 5, rowOffset: 5, sort: 'testIntProperty'], filters)

        then:
        20 == cnt

        and:
        [14, 15, 16, 17, 18] == lst.collect { it.testIntProperty }


        when:
        def val = service.list(domainGridConfig,
                [maxRows: 5, sort: 'testIntProperty'],
                new Filters(filters:
                        [
                                filterService.createFilterFromColumn(domainGridConfig, domainGridConfig.columns.testStringProperty, CN, '1'),
                                filterService.createFilterFromColumn(domainGridConfig, domainGridConfig.columns.testIntProperty, EQ, '100'),
                        ])
        )

        then:
        [100] == val.collect { it.testIntProperty }
    }

    def "test Gorm operations"() {
        given:
        populateTestDomain(N)
        def (params, request, response, session) = mockEasyGridContextHolder()

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


    def "test update operation with Integer id"() {
        given:
        populatePets()
        def (params, request, response, session) = mockEasyGridContextHolder()
        def petsConfig = generateConfigForGrid(grailsApplication, service) {
            'petsGridConfig' {
                dataSourceType 'gorm'
                domainClass PetTest
            }
        }.petsGridConfig


        when: "update"
        params.id = "${PetTest.findByName('Bonkers').id}"
        params.name = "Bonkers the Great"
        service.updateRow(petsConfig)
        def instance = PetTest.get(params.id)

        then:
        "Bonkers the Great" == instance.name

    }


    def "test Complex Query"() {
        given:
        populatePets()
        def (params, request, response, session) = mockEasyGridContextHolder()

        def petsGridConfig = generateConfigForGrid(grailsApplication, service) {
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
                    someTransientProp {

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


    def "test add default values with for transient property"() {
        //todo
    }


    @Unroll
    def "test conditional clause"(paramVal, count) {
        given:
        populatePets()
        EasygridContextHolder.storeParams([:])

        when:

        def query = service.createWhereQuery([domainClass: PetTest] as GridConfig,
                filterService.createGlobalFilters {
                    if (paramVal.size() > 2) {
                        eq('name', paramVal)
                    } else {
                        owner {
                            eq('city', paramVal)
                        }
                    }
                })

        then:
        count == query.count()

        where:
        paramVal | count
        'NJ'      | 2
        'Bonkers' | 1
        'LA'      | 1
    }

    @Unroll
    def "test conditional clause for owners"(paramVal, name) {
        given:
        populatePets()
        EasygridContextHolder.storeParams([:])

        when:

        def query = service.createWhereQuery([domainClass: OwnerTest] as GridConfig,
                filterService.createGlobalFilters {
                    if (paramVal.size() == 2) {
                        eq('city', paramVal)
                    } else {
                        pets {
                            eq('name', paramVal)
                        }
                    }
                })
        def owners = query.list()

        then:
        owners[0].name == name

        where:
        paramVal | name
        'NJ'      | 'Mary'
        'Bonkers' | 'John'
    }


    def "test a complex query"() {
        given:
        def (params, request, response, session) = mockEasyGridContextHolder()
        populatePets()
        def petsGridConfig = generateConfigForGrid(grailsApplication, service) {
            'petsGridConfig' {
                dataSourceType 'gorm'
                domainClass PetTest
                initialCriteria {
                    join 'owner'
                }
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
                    someTransientProp {

                    }
                    'owner.name' {
//                    name 'o.name'
//                    property 'owner.name'
                        enableFilter true
                        filterClosure { Filter filter ->
                            owner {
                                eq('name', 'John')
                            }
                        }
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

        when:
        data = service.list(petsGridConfig, [:], new Filters(filters: [filterService.createFilterFromColumn(petsGridConfig, petsGridConfig.columns['owner.name'], BW, 'John')]))
        then:
        2 == data.size()
        'Bonkers' == data[0].name
        'tommy' == data[1].name

        when:
        int cnt = service.countRows(petsGridConfig, new Filters(filters: [filterService.createFilterFromColumn(petsGridConfig, petsGridConfig.columns['owner.name'], BW, 'John')]))
        then:
        2 == cnt

        when: "verify default composed filter closure "
        data = service.list(petsGridConfig, [:], new Filters(filters: [filterService.createFilterFromColumn(petsGridConfig, petsGridConfig.columns['owner.city'], EQ, 'NJ')]))
        then:
        2 == data.size()
        'pandora' == data[0].name
        'wanikiy' == data[1].name

    }

    def "test order by multiple fields"() {
        given:
        (1..10).each {
            new TestDomain(id: it, testStringProperty: "val", testIntProperty: it).save(failOnError: true)
        }

        def (params, request, response, session) = mockEasyGridContextHolder()

        expect:
        10 == service.countRows(domainGridConfig)

        when:
        def domainRows = service.list(domainGridConfig, [multiSort: [[sort: 'testStringProperty', order: 'asc'], [sort: 'testIntProperty', order: 'desc']]])
        then:
        10 == domainRows.size()
        10 == domainRows[0].testIntProperty
    }

    def "test create nested filter criteria"() {
        given:
        populatePets()

        when:
        def c = new DetachedCriteria(PetTest)
        c = c.build(GridUtils.buildClosure(['owner'], { ilike('name', "J%") }))

        then:
        3 == c.count()
    }

    def "test complex filter"() {
        given:
        def (params, request, response, session) = mockEasyGridContextHolder()
        populatePets()
        def petsGridConfig = generateConfigForGrid(grailsApplication, service) {
            'petsGridConfig' {
                dataSourceType 'gorm'
                domainClass PetTest
                columns {
                    id
                    name
                    'owner.name' {}
                    'owner.city' {}
                }
            }
        }.petsGridConfig


        when:
        Filters filters = new Filters(filters: [
                filterService.createFilterFromColumn(petsGridConfig, petsGridConfig.columns.name, CN, 'o'),
                new Filters(type: FiltersEnum.or, filters: [
                        filterService.createFilterFromColumn(petsGridConfig, petsGridConfig.columns['owner.city'], EQ, 'NJ'),
                        filterService.createFilterFromColumn(petsGridConfig, petsGridConfig.columns['owner.name'], BW, 'J'),
                ])
        ])

        and:
        def pets = service.list(petsGridConfig, [:], filters)

        then:
        3 == pets.size()
        ['Bonkers', 'pandora', 'tommy',] == pets.collect { it.name }.sort()

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


}
