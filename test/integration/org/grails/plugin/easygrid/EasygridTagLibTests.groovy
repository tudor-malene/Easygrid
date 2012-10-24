package org.grails.plugin.easygrid

import grails.test.mixin.TestFor
import static org.junit.Assert.*
import org.codehaus.groovy.grails.plugins.easygrid.EasygridTagLib
import org.junit.Before
import org.grails.plugin.easygrid.grids.ClassicGridService
import org.grails.plugin.easygrid.grids.DatatableGridService
import org.grails.plugin.easygrid.grids.JqueryGridService
import org.grails.plugin.easygrid.grids.VisualizationGridService
import org.grails.plugin.easygrid.datasource.CustomDatasourceService
import org.grails.plugin.easygrid.datasource.GormDatasourceService
import org.grails.plugin.easygrid.datasource.ListDatasourceService

/**
 * User: Tudor
 * Date: 14.09.2012
 * Time: 14:02
 * See the API for {@link grails.test.mixin.web.GroovyPageUnitTestMixin} for usage instructions
 */
@TestFor(EasygridTagLib)
class EasygridTagLibTests {

    def easygridService

    @Before
    void setUp() {
        GridUtils.addMixins()
    }

    void testGridRender() {

        def controller = new TestDomainController()

        //simulate the ast transformation
        controller.metaClass.getGridsConfig = {easygridService.initGrids(controller)}
        tagLib.easygridService = easygridService

        def output = tagLib.grid(id: 'testGrid', controllerInstance: controller)
        assertTrue output.contains(controller.gridsConfig.testGrid.id)
    }
}
