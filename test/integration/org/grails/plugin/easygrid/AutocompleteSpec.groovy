package org.grails.plugin.easygrid

import grails.converters.JSON
import groovy.json.JsonSlurper
import spock.lang.Shared
import spock.lang.Stepwise

import static org.junit.Assert.*
import grails.test.mixin.Mock
import grails.test.mixin.TestFor

import org.junit.Before

/**
 * tests the autocomplete feature
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class AutocompleteSpec extends AbstractBaseTest {

    static transactional = true

    def autocompleteService

    @Shared def autocompleteGridConfig
    @Shared def autocomplete1GridConfig


    def initGrids() {
        autocompleteGridConfig = generateConfigForGrid {
            id 'autocompleteGridConfig'
            dataSourceType 'domain'
            domainClass TestDomain
            gridImpl 'jqgrid'
            autocomplete {
                labelProp 'testStringProperty'
                textBoxFilterClosure { Filter filter ->
                    ilike('testStringProperty', "%${filter.paramValue}%")
                }
            }
        }

        autocomplete1GridConfig = generateConfigForGrid {
            id 'autocomplete1GridConfig'
            dataSourceType 'gorm'
            domainClass TestDomain
            autocomplete {
                labelProp 'testStringProperty'
                textBoxFilterClosure { Filter filter ->
                    ilike('testStringProperty', "%${filter.paramValue}%")
                }
            }
        }

    }


    def "testAutocompleteInit"() {

        expect:
        'testStringProperty' == autocompleteGridConfig.autocomplete.labelProp
        autocompleteGridConfig.autocomplete.textBoxFilterClosure !=null

        when:
        easygridService.addDefaultValues(autocompleteGridConfig, defaultValues)

        then:
        'id' == autocompleteGridConfig.autocomplete.idProp

    }

    def "testDefaultValues"() {

        when:
        easygridService.addDefaultValues(autocomplete1GridConfig, defaultValues)

        then:
        'id' == autocomplete1GridConfig.autocomplete.idProp
        'testStringProperty' == autocomplete1GridConfig.autocomplete.labelProp
        autocomplete1GridConfig.autocomplete.textBoxFilterClosure !=null
    }


    def "testBasicScenario"() {

        given:
        populateTestDomain(100)
        easygridService.addDefaultValues(autocompleteGridConfig, defaultValues)

        when:
        params.term = '1'
        JSON result = autocompleteService.searchedElementsJSON(autocompleteGridConfig)

        then:
        10 == result.target.size()

        when:
        params.term = '100'
        result = autocompleteService.searchedElementsJSON(autocompleteGridConfig)

        then:
        1 == result.target.size()
        '100' == result.target[0].label


        when:
        params.id =  TestDomain.findByTestIntProperty(10).id
        result = autocompleteService.label(autocompleteGridConfig)

        then:
        1 == result.target.size()
        '10'== result.target[0].label
    }

}
