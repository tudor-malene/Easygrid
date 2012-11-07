package org.codehaus.groovy.grails.plugins.easygrid

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.grails.plugin.easygrid.Grid
import org.grails.plugin.easygrid.GridUtils

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


    def autocomplete = { attrs, body ->
        def gridConfig = getGridConfig(attrs)
        attrs.disabled = attrs.disabled ? "disabled='disabled'" : ''
        //todo -
        out << render(plugin: 'easygrid', template: "/templates/autocompleteRenderer", model: [attrs: attrs, gridConfig: gridConfig])
    }


    private Grid getGridConfig(attrs) {
        def instance = attrs.controllerInstance ?: grailsApplication.getArtefactByLogicalPropertyName(ControllerArtefactHandler.TYPE, attrs.controller ?: controllerName).newInstance()
        assert instance
        instance.gridsConfig."${attrs.id}"
    }

    /**
     * iterates the columns of a grid - depending on the context
     */
    def eachColumn = { attrs, body ->
        Grid gridConfig = attrs.gridConfig

//        gridConfig.columns.findAll {col -> (params.selectionComp) ? col.showInSelection : true}.eachWithIndex { col, idx ->
        GridUtils.eachColumn(gridConfig){col, idx ->
            out << body(col: col, idx: idx, last: (idx == gridConfig.columns.size() - 1))
        }

    }


}
