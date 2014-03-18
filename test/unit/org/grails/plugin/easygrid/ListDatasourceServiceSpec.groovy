package org.grails.plugin.easygrid

import grails.test.mixin.TestFor
import org.grails.plugin.easygrid.datasource.ListDatasourceService
import spock.lang.Specification
import spock.lang.Unroll

import static org.grails.plugin.easygrid.FilterOperatorsEnum.EW
import static org.grails.plugin.easygrid.FilterOperatorsEnum.GT
import static org.grails.plugin.easygrid.FilterOperatorsEnum.LT
import static org.grails.plugin.easygrid.FiltersEnum.or
import static org.grails.plugin.easygrid.TestUtils.generateConfigForGrid
import static org.grails.plugin.easygrid.TestUtils.mockEasyGridContextHolder

/**
 * Created by Tudor on 19.12.2013.
 */
@TestFor(ListDatasourceService)
class ListDatasourceServiceSpec extends Specification {

    def filterService

    def setup() {
        filterService = new FilterService()
    }

    @Unroll
    def "test result size"(list, size) {
        given:
        mockEasyGridContextHolder()[3].people = list

        when:
        def result = service.list([attributeName: 'people'], [maxRows: 10, rowOffset: 0, sort: 'age', order: 'desc'])

        then:
        result.size() == size

        where:
        list | size
        [[name: 'john', age: 5], [name: 'mary', age: 10]] | 2
        [[name: 'john', age: 5]]                          | 1
    }

    def "test order"() {
        given:
        mockEasyGridContextHolder()[3].people = [[name: 'john', age: 5], [name: 'mary', age: 10]]

        when:
        def result = service.list([attributeName: 'people'], [maxRows: 10, rowOffset: 0, sort: 'age', order: 'desc'])

        then:
        result == [[name: 'mary', age: 10], [name: 'john', age: 5]]

        when:
        result = service.list([attributeName: 'people'], [maxRows: 10, rowOffset: 0, sort: 'name', order: 'asc'])

        then:
        result == [[name: 'john', age: 5], [name: 'mary', age: 10]]

    }

    def "test pagination"() {
        given:
        mockEasyGridContextHolder()[3].people = (1..100).collect { [name: "$it", age: it] }

        when:
        def result = service.list([attributeName: 'people'], [maxRows: 20, rowOffset: 30, sort: 'age', order: 'desc'])

        then:
        result.size == 20
        result[0].age == 50
    }

    def "test filtering"() {
        given:
        mockEasyGridContextHolder()[3].people = (1..100).collect { [name: "$it", age: it] }

        when:
        def result = service.list([attributeName: 'people'], [maxRows: 20, rowOffset: 0, sort: 'age', order: 'asc'],
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
        def count = service.countRows([attributeName: 'people'],
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
        def result = service.list([attributeName: 'people'], [multiSort: [[sort: 'name', order: 'asc'], [sort: 'age', order: 'desc']]])

        then:
        10 == result.size()
        10 == result[0].age
    }

    def "test complex filter"() {
        given:
        mockEasyGridContextHolder()[3].people = (1..100).collect { [name: "John ${it}", age: it] }

        and:
        def peopleGridConfig = generateConfigForGrid(grailsApplication, service) {
            'peopleGridConfig' {
                dataSourceType 'list'
                attributeName 'people'
                columns {
                    id
                    name {
                        dataType String
                    }
                    age {
                        dataType Integer
                    }
                }
            }
        }.peopleGridConfig


        when:
        def filters = new Filters(filters: [
                filterService.createFilterFromColumn(peopleGridConfig, peopleGridConfig.columns.name, EW, '1'),
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


}
