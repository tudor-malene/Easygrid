package org.grails.plugin.easygrid

import grails.gorm.DetachedCriteria
import grails.test.spock.IntegrationSpec
import org.grails.plugin.easygrid.datasource.GormDatasourceService
import spock.lang.Unroll

import static org.grails.plugin.easygrid.FilterOperatorsEnum.*

/**
 * test the Gorm impl
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class GormDatasourceServiceSpec extends IntegrationSpec {

    def filterService
    def grailsApplication
    GormDatasourceService service
    GormDatasourceService gormDatasourceService

    GridConfig domainGridConfig
    GridConfig criteriaGridConfig

    static int N = 100

    def setup() {

        service = gormDatasourceService

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
//                dataSourceType 'gorm'
                domainClass TestDomain
                transformData { data ->
//                    data.unique { a, b -> a <=> b }
                    data
                }
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
//        def (params, request, response, session) = mockEasyGridContextHolder()
        def c = new TestDomainController()

        when: "delete"
        c.params.id = TestDomain.findByTestIntProperty(1).id
        def resp = new InlineResponse()
        service.delRow(domainGridConfig, resp)
        then:
        N - 1 == service.countRows(domainGridConfig)

        when: "before update"
        def before = TestDomain.findByTestIntProperty(2)
        then:
        2 == before.testIntProperty
        '2' == before.testStringProperty

        when: "update"
        c.params.id = TestDomain.findByTestIntProperty(2).id
        c.params.testIntProperty = -2
        c.params.testStringProperty = 'two'
        resp = new InlineResponse()
        service.updateRow(domainGridConfig, resp)
        def instance = TestDomain.get(c.params.id)

        then:
        !resp.message
        !resp.instance.errors.hasErrors()
        -2 == instance.testIntProperty
        'two' == instance.testStringProperty

        expect:
        N - 1 == service.countRows(domainGridConfig)

        when: "add"
        c.params.testIntProperty = 101
        c.params.testStringProperty = '101'
        resp = new InlineResponse()
        service.saveRow(domainGridConfig, resp)
        instance = TestDomain.findByTestIntProperty(101)

        then:
        N == service.countRows(domainGridConfig)
        '101' == instance.testStringProperty
    }


    def "test update operation with Integer id"() {
        given:
        def c = new TestDomainController()
        populatePets()
        and:
        def petsConfig = generateConfigForGrid(grailsApplication, service) {
            'petsGridConfig' {
                dataSourceType 'gorm'
                domainClass PetTest
            }
        }.petsGridConfig


        when: "update"
        c.params.id = "${PetTest.findByName('Bonkers').id}"
        c.params.name = "Bonkers the Great"
        def resp = new InlineResponse()
        service.updateRow(petsConfig, resp)
        def instance = PetTest.get(c.params.id)

        then:
        "Bonkers the Great" == instance.name

    }


    def "test Complex Query"() {
        given:
        populatePets()

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
                }, null, true)

        then:
        count == query.uniqueResult()

        where:
        paramVal  | count
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
        paramVal  | name
        'NJ'      | 'Mary'
        'Bonkers' | 'John'
    }


    def "test a complex query"() {
        given:
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
        data = service.list(petsGridConfig, [:],
                new Filters(filters: [filterService.createFilterFromColumn(petsGridConfig, petsGridConfig.columns['owner.name'], BW, 'John')]))
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

    def "test projections"() {
        given:
        populatePets()

        and:
        def petsGridConfig = generateConfigForGrid(grailsApplication, service) {
            'petsGridConfig' {
                domainClass PetTest
                initialCriteria {
                    projections {
                        property('id')
                        property('name')
                        owner {
                            property('name')
                            property('city')
                        }
                    }
                }
                transformData { row ->
                    def result = [:]
                    result.id = row[0]
                    result.name = row[1]
                    result['owner.name'] = row[2]
                    result['owner.city'] = row[3]
                    result
                }
                columns {
                    id
                    name
                    'owner.name' {}
                    'owner.city' {
                    }
                }
            }
        }.petsGridConfig


        and:
        Filters filters = new Filters(filters: [
                filterService.createFilterFromColumn(petsGridConfig, petsGridConfig.columns.name, CN, 'o'),
                new Filters(type: FiltersEnum.or, filters: [
                        filterService.createFilterFromColumn(petsGridConfig, petsGridConfig.columns['owner.city'], EQ, 'NJ'),
                        filterService.createFilterFromColumn(petsGridConfig, petsGridConfig.columns['owner.name'], BW, 'J'),
                ])
        ])

        when:
        def pets = service.list(petsGridConfig, [:], filters)

        then:
        3 == pets.size()
        ['Bonkers', 'pandora', 'tommy',] == pets.collect { it.name }.sort()

        when:
        def cnt = service.countRows(petsGridConfig)

        then:
        5 == cnt

        when:
        cnt = service.countRows(petsGridConfig, filters)

        then:
        3 == cnt

    }


    def "test count distinct"() {
        given:
        populatePets()

        and:
        def petsGridConfig = generateConfigForGrid(grailsApplication, service) {
            'petsGridConfig' {
                domainClass PetTest
                initialCriteria {
                    projections {
                        owner {
                            distinct('name')
                            property('city')
                        }
                    }
                }
                transformData { row ->
                    def result = [:]
                    result['owner.name'] = row[0]
                    result['owner.city'] = row[1]
                    result
                }
                columns {
                    'owner.name' {}
                    'owner.city' {
                    }
                }
            }
        }.petsGridConfig


        when:
        def pets = service.list(petsGridConfig, [:])

        then:
        3 == pets.size()

        when:
        def cnt = service.countRows(petsGridConfig)

        then:
        3 == cnt

        when: "set the countDistinct property"
        petsGridConfig.countDistinct = 'owner.name'
        and:
        cnt = service.countRows(petsGridConfig)


        then:
        3 == cnt

    }
/*

    def "test named queries"() {
        given:
        populatePets()

        when:
        def ownersGridConfig = generateConfigForGrid(grailsApplication, service) {
            'ownersGridConfig' {
                query OwnerTest.namedJohn
                columns {
                    id
                    name
                    address
                }

            }
        }.ownersGridConfig

        then:
        1==1

    }
*/

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

    }

    def easygridInitService
    /**
     * generates a config from a grid closure
     * @param gridConfigClosure
     * @return
     */
    GridConfig generateConfigForGrid(grailsApplication, dataSourceService = null, Closure gridConfigClosure) {
        easygridInitService.initializeFromClosure gridConfigClosure
    }


    static populateTestDomain(N = 100) {
        (1..N).each {
            new TestDomain(id: it, testStringProperty: "$it", testIntProperty: it).save(failOnError: true)
        }
        assert N == TestDomain.count()
    }
}
