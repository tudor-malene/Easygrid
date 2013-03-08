package org.grails.plugin.easygrid

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.junit.Before
import spock.lang.Shared
import spock.lang.Stepwise

import static org.junit.Assert.*

/**
 * tests the filter form feature
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class FilterFormSpec extends AbstractBaseTest {

    static transactional = true
    @Shared GridConfig filterFormGridConfig

    def initGrids() {
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


    def "testFilterFormInit"() {

        expect:
        2 == filterFormGridConfig.filterForm.size()
        'filterForm.testStringProperty' == filterFormGridConfig.filterForm['filterForm.testStringProperty'].name

    }

    def "testFormFilter"(){
        given:
        easygridService.addDefaultValues(filterFormGridConfig, defaultValues)
        populateTestDomain()
        params['filterForm.testStringProperty']='1'
        when:
        def data = easygridService.gridData(filterFormGridConfig)
        then:
        println "data = $data"
    }

}