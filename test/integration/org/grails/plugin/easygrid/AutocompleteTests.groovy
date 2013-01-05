package org.grails.plugin.easygrid

import grails.converters.JSON
import groovy.json.JsonSlurper

import static org.junit.Assert.*
import grails.test.mixin.Mock
import grails.test.mixin.TestFor

import org.junit.Before

/**
 * tests the autocomplete feature
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Mock(TestDomain)
@TestFor(TestDomainController)
class AutocompleteTests extends AbstractServiceTest {

    def autocompleteService

    def autocompleteGridConfig
    def autocomplete1GridConfig


    @Before
    void setup() {
        super.setup()

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

        populateTestDomain(100)
    }

    void testAutocompleteInit() {

        assertEquals 'testStringProperty', autocompleteGridConfig.autocomplete.labelProp
        assertNotNull autocompleteGridConfig.autocomplete.textBoxFilterClosure
        easygridService.addDefaultValues(autocompleteGridConfig, defaultValues)
        assertEquals 'id', autocompleteGridConfig.autocomplete.idProp

    }

    void testDefaultValus() {

        easygridService.addDefaultValues(autocomplete1GridConfig, defaultValues)

        assertEquals 'id', autocomplete1GridConfig.autocomplete.idProp
        assertEquals 'testStringProperty', autocomplete1GridConfig.autocomplete.labelProp
        assertNotNull autocomplete1GridConfig.autocomplete.textBoxFilterClosure
    }


    void testBasicScenario() {

        easygridService.addDefaultValues(autocompleteGridConfig, defaultValues)

        params.term = '1'
        JSON result = autocompleteService.searchedElementsJSON(autocompleteGridConfig)
        assertEquals 10, result.target.size()

        params.term = '100'
        result = autocompleteService.searchedElementsJSON(autocompleteGridConfig)
        assertEquals 1, result.target.size()
        assertEquals 100, result.target[0].id


        params.id = '10'
        result = autocompleteService.label(autocompleteGridConfig)
        assertEquals 1, result.target.size()
        assertEquals '10', result.target[0].label
    }

}
