package org.codehaus.groovy.grails.plugins.easygrid

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.grails.plugin.easygrid.Grid

/**
 * Taglib
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class EasygridTagLib {

    static namespace = "grid"
    def easygridService
    def grailsApplication

    /**
     * Include the code for the grid
     * -- id = the id of the grid
     */
    def grid = { attrs, body ->
        def gridConfig = getGridConfig(attrs)
        def model = easygridService.htmlGridDefinition(gridConfig)
        if (model) {
            out << render(template: gridConfig.gridRenderer, model: model)
        }
    }

    def exportButton = { attrs, body ->
        def gridConfig = getGridConfig(attrs)
        out << export.formats(action: "${gridConfig.id}Export", formats: ['excel'])
    }

    private Grid getGridConfig(attrs) {
        def instance = attrs.controllerInstance ?: grailsApplication.getArtefactByLogicalPropertyName(ControllerArtefactHandler.TYPE, attrs.controller ?: controllerName).newInstance()
        assert instance
        instance.gridsConfig."${attrs.id}"
    }


}
