package org.grails.plugin.easygrid

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.junit.Before

import static org.junit.Assert.*

/**
 * tests the autocomplete feature
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Mock(TestDomain)
@TestFor(TestDomainController)
class AutocompleteTests extends AbstractServiceTest {

    def autocompleteGridConfig

    @Before
    void setup() {
        super.setup()

        autocompleteGridConfig = generateConfigForGrid {
            id 'autocompleteGridConfig'
            dataSourceType 'domain'
            domainClass TestDomain
            gridImpl 'jqgrid'
            autocomplete {
                idProp 'id'                // evident e idul - in loc de selectbox - ar trebui sa fie default
//                codeProp 'testStringProperty'                // valoarea care sa se afiseze - codul
                labelProp 'testStringProperty'                // daca vrei sa afisezi o descriere
                textBoxSearchClosure { params ->

                }
            }
        }

    }

    void testAutocompleteInit() {

        assertEquals 'id', autocompleteGridConfig.autocomplete.idProp
//        assertEquals 'testStringProperty', autocompleteGridConfig.autocomplete.codeProp
        assertEquals 'testStringProperty', autocompleteGridConfig.autocomplete.labelProp
        assertNotNull autocompleteGridConfig.autocomplete.textBoxSearchClosure

    }

}
