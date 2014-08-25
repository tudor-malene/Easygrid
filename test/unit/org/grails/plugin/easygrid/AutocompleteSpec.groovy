package org.grails.plugin.easygrid

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

import static TestUtils.generateConfigForGrid
import static TestUtils.populateTestDomain

/**
 * tests the autocomplete feature
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@TestFor(AutocompleteService)
@Mock(TestDomain)
class AutocompleteSpec extends Specification {

    def setup() {
        populateTestDomain(100)
        service.filterService = new FilterService()
    }


    def "test autocomplete"() {
        given:
        GridConfig autocompleteGridConfig = generateConfigForGrid(grailsApplication) {
            autocompleteGridConfig {
                dataSourceType 'gorm'
                domainClass TestDomain
                gridImpl 'jqgrid'
                autocomplete {
                    labelProp 'testStringProperty'
                    textBoxFilterClosure { Filter filter ->
                        ilike('testStringProperty', "%${filter.paramValue}%")
                    }
                }
            }
        }.autocompleteGridConfig

        def (params, request, response, session) = TestUtils.mockEasyGridContextHolder()
        service.easygridDispatchService = new Expando()

        expect:
        'testStringProperty' == autocompleteGridConfig.autocomplete.labelProp
        null != autocompleteGridConfig.autocomplete.textBoxFilterClosure
        'id' == autocompleteGridConfig.autocomplete.idProp

        when:
        params.id = TestDomain.findByTestIntProperty(10).id
        service.easygridDispatchService.callDSGetById = { grid, id -> TestDomain.get(id) }
        def label = service.label(autocompleteGridConfig)

        then:
        1 == label.target.size()
        '10' == label.target[0].label

        when:
        params.term = term
        service.easygridDispatchService.callDSList = { grid, listParams, filters ->
            TestDomain.where {
                testStringProperty ==~ "%${term}%"
                order('id')
            }.list(max: 10)
        }
        def result = service.searchedElementsJSON(autocompleteGridConfig)

        then:
        size == result.target.size()
        firstLabel == result.target[0].label

        where:
        term  | size | firstLabel
        '1'   | 10   | '1'
        '100' | 1    | '100'


    }

    def "testDefaultValues"() {
        when:
        def autocomplete1GridConfig = generateConfigForGrid(grailsApplication) {
            autocomplete1GridConfig {
                dataSourceType 'gorm'
                domainClass TestDomain
                autocomplete {
                    labelProp 'testStringProperty'
                    textBoxFilterClosure { Filter filter ->
                        ilike('testStringProperty', "%${filter.paramValue}%")
                    }
                }
            }
        }.autocomplete1GridConfig


        then:
        'id' == autocomplete1GridConfig.autocomplete.idProp
        'testStringProperty' == autocomplete1GridConfig.autocomplete.labelProp
        autocomplete1GridConfig.autocomplete.textBoxFilterClosure != null
    }

}
