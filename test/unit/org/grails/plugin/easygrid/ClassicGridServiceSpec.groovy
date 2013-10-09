package org.grails.plugin.easygrid

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.grails.plugin.easygrid.grids.ClassicGridService
import spock.lang.Ignore
import spock.lang.Specification
import static org.grails.plugin.easygrid.TestUtils.*

/**
 * test for the classic grid impl
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@TestFor(ClassicGridService)
@Mock([TestDomain])
class ClassicGridServiceSpec extends Specification {

    @Ignore
    def "testClassicGrid"() {

        given:
        def classicDomainGridConfig = TestUtils.generateConfigForGrid(grailsApplication) {
            'classicTestDomainGrid' {
                dataSourceType 'domain'
                domainClass TestDomain
                gridImpl 'classic'
                columns {
                    id {
                        type 'id'
                    }
                    testStringProperty {
                        enableFilter true
//                    filterFieldType 'text'
                        jqgrid {
                        }
                    }
                    testIntProperty {
                        enableFilter true
                        jqgrid {
                        }
                    }

                }
            }
        }.classicTestDomainGrid

        populateTestDomain(100)

//        when:
        //todo

//        then:
//        todo
    }
}
