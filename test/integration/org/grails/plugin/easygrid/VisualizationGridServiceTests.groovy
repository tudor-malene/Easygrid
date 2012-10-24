package org.grails.plugin.easygrid

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import static org.junit.Assert.*
import org.junit.Before
import com.google.visualization.datasource.datatable.value.ValueType
import groovy.time.TimeCategory
import com.ibm.icu.util.GregorianCalendar
import com.google.visualization.datasource.datatable.value.DateTimeValue
import com.ibm.icu.util.TimeZone

/**
 * User: Tudor
 * Date: 27.09.2012
 * Time: 18:50
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
//            (Date.class): {it.format("dd/MM/yyyy")},
                (Date.class): {def cal = com.ibm.icu.util.Calendar.getInstance(); cal.setTime(it); cal.setTimeZone(TimeZone.getTimeZone("GMT")); cal}, //wtf?
//            (Boolean.class): { it ? "Yes" : "No" }
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
            dataProvider {filters,listParams ->
                def values = [
                        [name: 'Fyodor Dostoyevsky', nation: 'russian', birthDate: new GregorianCalendar(1821, 10, 11).time],
                        [name: 'Ion Creanga', nation: 'romanian', birthDate: new GregorianCalendar(1837, 2, 3).time],
                ]
                if (listParams.sort != 'age') {
                    values.sort {o1, o2 -> def x = o1[listParams.sort] <=> o2[listParams.sort]; (listParams.order == 'asc') ? x : -x}
                } else {
                    values.sort {o1, o2 -> def x = o1.birthDate <=> o2.birthDate; (listParams.order == 'asc') ? x : -x}
                }
            }
            dataCount {searchClosure->
                2
            }
            columns {
                'author.name.label' {
                    property 'name'
                    visualization {
                        valueType ValueType.TEXT
                    }
                }
                'author.nation.label' {
                    property 'nation'
                    visualization {
                        valueType ValueType.TEXT
                    }
                }
                'author.age.label' {
                    value { row ->
                        use(TimeCategory) {
                            new Date().year - row.birthDate.year
                        }
                    }
                    visualization {
                        name 'age'
                        valueType ValueType.NUMBER
                    }
                }
                'author.birthDate.label' {
                    property 'birthDate'
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
