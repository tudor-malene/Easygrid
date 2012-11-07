package org.grails.plugin.easygrid

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.junit.Before
import static org.junit.Assert.*

/**
 * test for the classic grid impl
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Mock(TestDomain)
@TestFor(TestDomainController)
class ClassicGridServiceTests extends  AbstractServiceTest{

    def classicDomainGridConfig

    @Before
    void setUp(){
        super.setup()

        classicDomainGridConfig = generateConfigForGrid {
            id 'classicTestDomainGrid'
            dataSourceType 'domain'
            domainClass TestDomain
            gridImpl 'classic'
//            service ClassicGridService
//            renderer '/templates/classicGridRenderer'
        }

    }

    void testClassicGrid() {
        populateTestDomain(100)

        easygridService.addDefaultValues(classicDomainGridConfig, defaultValues)

        assertNotNull easygridService.htmlGridDefinition(classicDomainGridConfig)
    }

}
