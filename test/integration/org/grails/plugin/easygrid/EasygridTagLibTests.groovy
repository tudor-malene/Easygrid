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
 * tests the taglib
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
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
