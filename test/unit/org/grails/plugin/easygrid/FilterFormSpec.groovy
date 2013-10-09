package org.grails.plugin.easygrid

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Ignore
import spock.lang.Specification

/**
 * tests the filter form feature
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@TestFor(EasygridService)
@Mock(TestDomain)
class FilterFormSpec extends Specification {

    GridConfig filterFormGridConfig

    def setup() {
        filterFormGridConfig = TestUtils.generateConfigForGrid(grailsApplication) {
            'filterFormGridConfig' {
                dataSourceType 'gorm'
                domainClass TestDomain
                gridImpl 'jqgrid'
                filterForm {
                    filterFormService FilterFormService
                    fields {
                        'filterForm.testIntProperty' {
                            type 'number'
                            label 'testDomain.testIntProperty.label'
                            filterClosure { Filter filter ->
                                eq('testIntProperty', filter.paramValue as int)
                            }
                        }
                        'filterForm.testStringProperty' {
                            type 'text'
                            label 'testDomain.testStringProperty.label'
                            filterClosure { Filter filter ->
                                ilike('testStringProperty', "%${filter.paramValue}%")
                            }
                        }
                    }
                }
            }
        }.filterFormGridConfig
    }


    def "testFilterFormInit"() {

        expect:
        2 == filterFormGridConfig.filterForm.fields.size()
        'filterForm.testStringProperty' == filterFormGridConfig.filterForm.fields['filterForm.testStringProperty'].name

    }

    @Ignore
    def "testFormFilter"() {
        given:
        easygridService.addDefaultValues(filterFormGridConfig, defaultValues)
        populateTestDomain()
        params['filterForm.testStringProperty'] = '1'
        when:
        def data = easygridService.gridData(filterFormGridConfig)
        then:
        println "data = $data"
    }

}