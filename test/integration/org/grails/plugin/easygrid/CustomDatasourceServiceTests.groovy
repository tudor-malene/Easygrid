package org.grails.plugin.easygrid

import static org.junit.Assert.*
import grails.test.mixin.TestFor

import org.junit.Before

/**
 * test for the custom datasource
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@TestFor(TestDomainController)
class CustomDatasourceServiceTests extends AbstractServiceTest {

    def customDatasourceService

    @Before
    void setUp() {
        super.setup()
    }

    void testCustomDataSource() {

        easygridService.addDefaultValues(customGridConfig, defaultValues)
        EasygridContextHolder.setLocalGridConfig(customGridConfig)
//        def rows = customDatasourceService.list(params)
//        assertEquals 1, rows.size()
    }
}
