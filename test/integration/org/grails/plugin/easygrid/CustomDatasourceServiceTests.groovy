package org.grails.plugin.easygrid

import static org.junit.Assert.*
import org.junit.Before
import grails.test.mixin.TestFor

/**
 * User: Tudor
 * Date: 05.10.2012
 * Time: 15:45
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
