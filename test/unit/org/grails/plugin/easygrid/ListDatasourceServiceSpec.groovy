package org.grails.plugin.easygrid

import grails.test.mixin.TestFor
import grails.validation.Validateable
import org.grails.plugin.easygrid.datasource.ListDatasourceService
import spock.lang.Specification
import spock.lang.Unroll

import static org.grails.plugin.easygrid.FilterOperatorsEnum.*
import static org.grails.plugin.easygrid.FiltersEnum.or
import static org.grails.plugin.easygrid.TestUtils.generateConfigForGrid
import static org.grails.plugin.easygrid.TestUtils.mockEasyGridContextHolder

/**
 * Created by Tudor on 19.12.2013.
 */
@TestFor(ListDatasourceService)
class ListDatasourceServiceSpec extends Specification {

    def filterService
    def peopleGridConfig

    def setup() {
        filterService = new FilterService()
        peopleGridConfig = generateConfigForGrid(grailsApplication, service) {
            'peopleGridConfig' {
                dataSourceType 'list'
                attributeName 'people'
                columns {
                    id
                    name {
                        filterDataType String
                    }
                    age {
                        filterDataType Integer
                    }
                }
            }
        }.peopleGridConfig

    }

    @Unroll
    def "test result size"(list, size) {
        given:
        mockEasyGridContextHolder()[3].people = list

        when:
        def result = service.list(peopleGridConfig, [maxRows: 10, rowOffset: 0, sort: 'age', order: 'desc'])

        then:
        result.size() == size

        where:
        list                                              | size
        [[name: 'john', age: 5], [name: 'mary', age: 10]] | 2
        [[name: 'john', age: 5]]                          | 1
    }

    def "test order"() {
        given:
        mockEasyGridContextHolder()[3].people = [[name: 'john', age: 5], [name: 'mary', age: 10]]

        when:
        def result = service.list(peopleGridConfig, [maxRows: 10, rowOffset: 0, sort: 'age', order: 'desc'])

        then:
        result == [[name: 'mary', age: 10], [name: 'john', age: 5]]

        when:
        result = service.list(peopleGridConfig, [maxRows: 10, rowOffset: 0, sort: 'name', order: 'asc'])

        then:
        result == [[name: 'john', age: 5], [name: 'mary', age: 10]]

    }

    def "test pagination"() {
        given:
        mockEasyGridContextHolder()[3].people = (1..100).collect { [name: "$it", age: it] }

        when:
        def result = service.list(peopleGridConfig, [maxRows: 20, rowOffset: 30, sort: 'age', order: 'desc'])

        then:
        result.size == 20
        result[0].age == 70
    }

    def "test filtering"() {
        given:
        mockEasyGridContextHolder()[3].people = (1..100).collect { [name: "$it", age: it] }

        when:
        def result = service.list(peopleGridConfig, [maxRows: 20, rowOffset: 0, sort: 'age', order: 'asc'],
                new Filters(filters: [
                        filterService.createGlobalFilter({ params, row ->
                            row.age >= 50
                        }),
                        filterService.createGlobalFilter({ params, row ->
                            row.age < 60
                        }),
                ]))

        then:
        result.size == 10
        result[0].age == 50
        result[9].age == 59

        when:
        def count = service.countRows(peopleGridConfig,
                new Filters(filters: [
                        filterService.createGlobalFilter({ params, row ->
                            row.age >= 50
                        }),
                        filterService.createGlobalFilter({ params, row ->
                            row.age < 60
                        }),
                ]))

        then:
        count == 10

    }

    def "test order by multiple fields"() {
        given:
        mockEasyGridContextHolder()[3].people = (1..10).collect { [name: "John", age: it] }

        when:
        def result = service.list(peopleGridConfig, [multiSort: [[sort: 'name', order: 'asc'], [sort: 'age', order: 'desc']]])

        then:
        10 == result.size()
        10 == result[0].age
    }


    def "test sort"() {
        given:
        mockEasyGridContextHolder()[3].people = (1..100).collect { [name: "John", age: it] }

        when:
        def result = service.list(peopleGridConfig, [maxRows: 10, rowOffset: 10, sort: 'age', order: 'desc'])

        then:
        10 == result.size()
        (90..81) == result.collect { it.age }

        when:
        result = service.list(peopleGridConfig, [maxRows: 10, rowOffset: 10, sort: 'age', order: 'asc'])

        then:
        10 == result.size()
        (11..20) == result.collect { it.age }
    }


    def "test complex filter"() {
        given:
        mockEasyGridContextHolder()[3].people = (1..100).collect { [name: "John ${it}", age: it] }

        when:
        def filters = new Filters(filters: [
                filterService.createFilterFromColumn(peopleGridConfig, peopleGridConfig.columns.name, EW, '1'),
                filterService.createFilterFromColumn(peopleGridConfig, peopleGridConfig.columns.name, NN, null),
                new Filters(type: or, filters: [
                        filterService.createFilterFromColumn(peopleGridConfig, peopleGridConfig.columns.age, GT, '90'),
                        filterService.createFilterFromColumn(peopleGridConfig, peopleGridConfig.columns.age, LT, '10'),
                ]
                )
        ]
        )

        and:
        def result = service.list(peopleGridConfig, [:], filters)

        then:
        2 == result.size()
        1 == result[0].age
        91 == result[1].age

    }

    def "test sort closure"() {
        given:
        mockEasyGridContextHolder()[3].people = (1..100).collect { [name: "John ${it}", age: it] }

        and:
        def grid = generateConfigForGrid(grailsApplication, service) {
            'peopleGridConfig' {
                dataSourceType 'list'
                attributeName 'people'
                columns {
                    id
                    name {
                        filterDataType String
                    }
                    age {
                        filterDataType Integer
                        sortClosure { order, val1, val2 ->
                            def comp = val1.age <=> val2.age
                            (order == 'asc') ? comp : -comp
                        }
                    }
                }
            }
        }.peopleGridConfig

        when:
        def result = service.list(grid, [sort: 'age', order: 'desc'])

        then:
        100 == result[0].age
        1 == result[99].age

    }

    def "test inline edit"() {
        given:
        def (params, request, response, session) = mockEasyGridContextHolder()
        session.people = (1..100).collect { [id: it, name: "John ${it}", age: it] as Person }

        and:
        def grid = generateConfigForGrid(grailsApplication, service) {
            'peopleGridConfig' {
                dataSourceType 'list'
                attributeName 'people'
                listClass Person
                columns {
                    id
                    name
                    age
                }
            }
        }.peopleGridConfig


        when: "update an existing row with valid values"
        params.name = "Paul"
        params.id = '1'
        params.oper = 'edit'
        def resp = new InlineResponse()
        service.updateRow(grid, resp)

        then:
        !resp.message
        !resp.errors
        'Paul' == service.findById(grid, params.id).name


        when: "update an existing row with invalid values"
        params.name = ""
        params.id = '1'
        params.oper = 'edit'
        resp = new InlineResponse()
        service.updateRow(grid, resp)

        then:
        !resp.message
        resp.instance.errors.hasFieldErrors('name')

        when: "update an inexisting row"
        params.name = ""
        params.id = '103'
        params.oper = 'edit'
        resp = new InlineResponse()
        service.updateRow(grid, resp)

        then:
        resp.message.contains('not')


        when: "add a new row"
        params.name = "x"
        params.age = "30"
        params.id = '101'
        params.oper = 'add'
        resp = new InlineResponse()
        service.saveRow(grid, resp)

        then:
        !resp.message
        'x' == service.findById(grid, params.id).name


        when: "add a new row with invalid values"
        params.name = "y"
        params.age = ""
        params.id = '102'
        params.oper = 'add'
        resp = new InlineResponse()
        service.saveRow(grid, resp)

        then:
        resp.instance.errors.hasFieldErrors('age')
        !service.findById(grid, params.id)


        when: "delete an existing row"
        params.oper = 'del'
        params.id = '101'
        resp = new InlineResponse()
        service.delRow(grid, resp)

        then:
        !resp.message
        !service.findById(grid, params.id)


        when: "delete a non existing row"
        params.oper = 'del'
        params.id = '101'
        resp = new InlineResponse()
        service.delRow(grid, resp)

        then:
        resp.message.contains('not')

    }

}

@Validateable
class Person {
    long id
    String name
    int age

    static constraints = {
        name nullable: false, blank: false, unique: true
    }
}
