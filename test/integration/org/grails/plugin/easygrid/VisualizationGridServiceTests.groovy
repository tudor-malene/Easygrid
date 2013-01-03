package org.grails.plugin.easygrid

import static org.junit.Assert.*
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import groovy.time.TimeCategory

import org.junit.Before

import com.google.visualization.datasource.datatable.value.DateTimeValue
import com.google.visualization.datasource.datatable.value.ValueType
import com.ibm.icu.util.GregorianCalendar
import com.ibm.icu.util.TimeZone

/**
 * google visualization impl tests
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Mock(TestDomain)
@TestFor(TestDomainController)
class VisualizationGridServiceTests extends AbstractServiceTest {

    def domainGridConfig
    def visualizationGridService
    def customVisGridConfig

    @Before
    void setUp() {
        super.setup()
/*
        defaultValues.formats = [
//            (Date): {it.format("dd/MM/yyyy")},
                (Date): {def cal = com.ibm.icu.util.Calendar.getInstance(); cal.setTime(it); cal.setTimeZone(TimeZone.getTimeZone("GMT")); cal}, //wtf?
//            (Boolean): { it ? "Yes" : "No" }
        ]
*/

        //initialize the list grid
        domainGridConfig = generateConfigForGrid {
            id 'testDomainGrid'
            dataSourceType 'domain'
            gridImpl 'visualization'
            domainClass TestDomain
        }

        //initialize the custom grid
        customVisGridConfig = generateConfigForGrid {
            id 'authorGrid'
            labelPrefix 'author'
            dataSourceType 'custom'
            gridImpl 'visualization'
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
            dataProvider { filters, listParams ->
                def values = [
                        [name: 'Fyodor Dostoyevsky', nation: 'russian', birthDate: new GregorianCalendar(1821, 10, 11).time],
                        [name: 'Ion Creanga', nation: 'romanian', birthDate: new GregorianCalendar(1837, 2, 3).time],
                ]
                if (listParams.sort != 'age') {
                    values.sort { o1, o2 -> def x = o1[listParams.sort] <=> o2[listParams.sort]; (listParams.order == 'asc') ? x : -x }
                } else {
                    values.sort { o1, o2 -> def x = o1.birthDate <=> o2.birthDate; (listParams.order == 'asc') ? x : -x }
                }
            }
            dataCount { filterClosure ->
                2
            }
            columns {
                name {
                    visualization {
                        valueType ValueType.TEXT
                    }
                }
                nation {
                    visualization {
                        valueType ValueType.TEXT
                    }
                }
                age {
                    value { row ->
                        use(TimeCategory) {
                            new Date().year - row.birthDate.year
                        }
                    }
                    visualization {
                        valueType ValueType.NUMBER
                    }
                }
                birthDate {
                    visualization {
                        valueType ValueType.DATE
                    }
                }
            }
        }
    }

    void testSetup() {

        def N = 100
        populateTestDomain(N)

        params.tq = 'order by `testIntProperty` desc limit 50 offset 30'
        params.tqx = ''

        //hack
        request.setParameter('tq', params.tq)
        request.setParameter('tqx', params.tqx)

        //add default & types
        easygridService.addDefaultValues(domainGridConfig, defaultValues)

        def errors = easygridService.verifyGridConstraints(domainGridConfig)
        assertEquals 0, errors.size()

        visualizationGridService.filters()
        visualizationGridService.listParams()

        //todo
//        visualizationGridService.transform(domainGridConfig,[:],[:])
        def data = easygridService.gridData(domainGridConfig)
//        println data

//        def gridDef = visualizationGridService.htmlGridDefinition(domainGridConfig)
//        assertEquals 1, gridDef.size()
//        assertEquals N, gridDef.rows.size()
//        assertEquals domainGridConfig, gridDef.gridConfig
    }

    void testValueTypes() {

        def cal = (com.ibm.icu.util.Calendar.getInstance())
        cal.setTimeZone(TimeZone.getTimeZone("GMT"))
        println new DateTimeValue(cal)
    }

    void testCustomVisConfig() {

        easygridService.addDefaultValues(customVisGridConfig, defaultValues)

        params.tq = 'order by `age` desc limit 10 offset 0'
        params.tqx = ''
        //hack
        request.setParameter('tq', params.tq)
        request.setParameter('tqx', params.tqx)

        def result = easygridService.gridData(customVisGridConfig)
        println result
    }
}
