package org.grails.plugin.easygrid

import spock.lang.Shared
import spock.lang.Stepwise

/**
 * test for the classic grid impl
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class ClassicGridServiceSpec extends AbstractBaseTest {

    static transactional = true

    @Shared
    def classicDomainGridConfig


    def initGrids() {
        classicDomainGridConfig = generateConfigForGrid {
            id 'classicTestDomainGrid'
            dataSourceType 'domain'
            domainClass TestDomain
            gridImpl 'classic'
        }
    }


    def "testClassicGrid"() {
        given:
        populateTestDomain(100)

        when:
        easygridService.addDefaultValues(classicDomainGridConfig, defaultValues)

        then:
        easygridService.htmlGridDefinition(classicDomainGridConfig) != null
    }
}
