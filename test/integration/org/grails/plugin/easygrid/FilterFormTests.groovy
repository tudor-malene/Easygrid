package org.grails.plugin.easygrid

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.junit.Before

import static org.junit.Assert.*

/**
 * tests the filter form feature
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Mock(TestDomain)
@TestFor(TestDomainController)
class FilterFormTests extends AbstractServiceTest {

    GridConfig filterFormGridConfig

    @Before
    void setup() {
        super.setup()

        filterFormGridConfig = generateConfigForGrid {
            id 'filterFormGridConfig'
            dataSourceType 'gorm'
            domainClass TestDomain
            gridImpl 'jqgrid'
            filterForm{ // un fel de columns
                'filterForm.testIntProperty'{
                    type 'number'
                    label 'testDomain.testIntProperty.label'
                    filterClosure { Filter filter ->
                        eq('testIntProperty', filter.paramValue as int)
                    }
                }
                'filterForm.testStringProperty'{
                    type 'text'
                    label 'testDomain.testStringProperty.label'
                    filterClosure { Filter filter ->
                        ilike('testStringProperty', "%${filter.paramValue}%")
                    }
                }
            }
        }
    }


    void testFilterFormInit() {

        assertEquals 2, filterFormGridConfig.filterForm.size()
        assertEquals 'filterForm.testStringProperty', filterFormGridConfig.filterForm['filterForm.testStringProperty'].name

    }

    void testFormFilter(){
        easygridService.addDefaultValues(filterFormGridConfig, defaultValues)
        populateTestDomain()
        params['filterForm.testStringProperty']='1'
        def data = easygridService.gridData(filterFormGridConfig)
        println "data = $data"
    }

}