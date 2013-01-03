package org.grails.plugin.easygrid

import static org.junit.Assert.*
import groovy.time.TimeCategory
import org.grails.plugin.easygrid.builder.EasygridBuilder

/**
 * base class for integration tests
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
abstract class AbstractServiceTest {

    def easygridService
    def gridDelegate
    def grailsApplication

    def customGridConfig

    def listGridConfig

    //definitions
    def defaultValues

    void setup() {

        GridUtils.addMixins()

        assert grailsApplication?.domainClasses?.size() >= 1
//        easygridService.grailsApplication.config?.easygrid = defaultValues
        defaultValues = grailsApplication.config?.easygrid

//        defaultValues.columns.buildStyle = 'columnLabelAsHeader'
        //initialize the custom grid
        customGridConfig = generateConfigForGrid {
            id 'authorGrid'
            dataSourceType 'custom'
            labelPrefix 'author'
            roles 'admin'
            securityProvider { grid, oper ->
                if (grid.roles) {
                    if (grid.roles == 'admin') {
                        return true
                    }
                    return false
                }
                return true
            }
            dataProvider {gridConfig, filters, listParams ->
                [
                        [id: 1, name: 'Fyodor Dostoyevsky', nation: 'russian', birthDate: new GregorianCalendar(1821, 10, 11)],
                ]
            }
            dataCount {filters ->
                1
            }
            jqgrid {
                width 650
                height 150
            }
            columns {
                id {
                    type 'id'
                }
                name {
                    filterClosure {
                        ilike('name', "%${it}%")
                    }
                    jqgrid {
                        editable true
                    }
                    export {
                        width 100
                    }
                }
                nation {
                    filterClosure {
                        ilike('nation', "%${it}%")
                    }
                    jqgrid {
                    }
                }
                age {
                    value { row ->
                        use(TimeCategory) {
                            new Date().year - row.birthDate.time.year
                        }
                    }
                    filterClosure {
                        eq('age', it as int)
                    }
                    jqgrid {
                        width 110
                    }
                }
                birthDate {
                    formatName 'stdDateFormatter'
                    filterClosure {
                        eq('birthDate', it)
                    }
                    jqgrid {
                        width 110
                    }
                }
            }
        }

        EasygridContextHolder.session.setAttribute('listData', (1..200).collect {[col1: it, col2: "$it"]})

        //initialize the list grid
        listGridConfig = generateConfigForGrid {
            id 'listProviderGrid'
            labelPrefix 'list'
            dataSourceType 'list'
            context 'session'
            attributeName 'listData'
            columns {
                col1 {
                    filterClosure {Filter filter, params, element ->
                        element.col1 > params.min
                    }
                    jqgrid {
                        editable true
                    }
                }
                col2 {
//                    filterClosure {val, params, element ->
//                        element.col2.contains(params.col2)
//                    }
                    enableFilter true
                    filterFieldType 'text'
                    jqgrid {
                    }
                }
                col3 {
                    value {it.col1 * it.col1}
                    filterClosure { val, params, element ->
                    }
                }
            }
        }
    }

    /**
     * generates a config from a grid closure
     * @param gridConfigClosure
     * @return
     */
    def generateConfigForGrid(Closure gridConfigClosure) {
        new EasygridBuilder(grailsApplication).evaluateGrid gridConfigClosure
    }


    def populateTestDomain(N = 100) {
//        def N = 100
        (1..N).each {
            new TestDomain(testStringProperty: "$it", testIntProperty: it).save(true)
        }
        assertEquals N, TestDomain.count()
    }

}
