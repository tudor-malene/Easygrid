package org.codehaus.groovy.grails.plugins.easygrid

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.grails.plugin.easygrid.GridConfig
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
     *
     * @attr name REQUIRED - the name of the grid
     * @attr id - the javascript id of the component ( by default the grid name)
     * @attr controller - the controller where the grid is defined ( by default the current controller)
     */
    def grid = { attrs, body ->
        if (attrs.id == null) {
            attrs.id = attrs.name
        }

        def gridConfig = getGridConfig(attrs)
        def model = easygridService.htmlGridDefinition(gridConfig)
        if (model) {
            model.attrs = attrs
            out << render(template: gridConfig.gridRenderer, model: model)
        }
    }

    /**
     * a simple excel export button for the grid
     *
     * @attr name REQUIRED - the name of the grid
     * @attr id - the javascript id of the grid - if different from the name
     * @attr controller - the controller where the grid is defined ( by default the current controller)
     */
    def exportButton = { attrs, body ->
        if (attrs.id == null) {
            attrs.id = attrs.name
        }

        def gridConfig = getGridConfig(attrs)
        out << export.formats(action: "${gridConfig.id}Export", formats: ['excel'])
    }

    /**
     * Generates a selection widget -
     * which is a replacement for drop-down boxes, when the data to select from is larger
     * It features a input ( decorated with jquery-ui autocomplete_)
     * and a button which opens a dialog with a full grid ( with filtering) where you can select your desired value
     *
     * @attr name REQUIRED- the form property
     * @attr value - the value of the form property ( the id )
     * @attr id -  the id of the hidden field( on which the jquery widget is attached)
     * @attr gridName REQUIRED-  the name of the grid to be used
     * @attr controller REQUIRED- the controller
     * @attr title - the title of the popup
     * @attr width  - width of the popup
     * @attr height  - height of the popup
     * @attr showAutocompleteBox  - displays the fast input
     * @attr onChange - onChange event for the widget
     * @attr staticConstraints - a key-value pair of constraints that will be sent to the server
     * @attr dynamicConstraints - a key-value pair of dynamic constraints that will be sent to the server ( the value will be a jquery selector - on which - at runtime the .val() method is called -and that is sent to the server.)
     * @attr disabled - disables the component
     */
    def selection = { attrs, body ->
        attrs.disabled = attrs.disabled ? true : false
        attrs.id = attrs.id ?: attrs.name
        attrs.title = attrs.title ?: 'default.selectionComponent.title'
        attrs.width = attrs.width ?: 940
        attrs.height = attrs.height ?: 400
        attrs.showAutocompleteBox = (attrs.showAutocompleteBox != null) ? attrs.showAutocompleteBox : true
        attrs.disabled = (attrs.disabled != null) ? attrs.disabled : false

        out << render(plugin: 'easygrid', template: "/templates/autocompleteRenderer", model: [attrs: attrs])
    }

    /**
     * iterates the columns of a grid - depending on the context
     *
     * @attr gridConfig REQUIRED - the grid
     */
    def eachColumn = { attrs, body ->
        GridConfig gridConfig = attrs.gridConfig

//        gridConfig.columns.findAll {col -> (params.selectionComp) ? col.showInSelection : true}.eachWithIndex { col, idx ->
        GridUtils.eachColumn(gridConfig) {col, idx ->
            out << body(col: col, idx: idx, last: (idx == gridConfig.columns.size() - 1))
        }
    }

    /**
     * returns the grid from the specified controller  ( by default the current )
     * @param attrs
     * @return
     */
    private GridConfig getGridConfig(attrs) {
        def instance = attrs.controllerInstance ?: grailsApplication.getArtefactByLogicalPropertyName(ControllerArtefactHandler.TYPE, attrs.controller ?: controllerName).newInstance()
        assert instance
        def gridConfig = instance.gridsConfig."${attrs.name}".deepClone()

        //overwrite grid properties
        attrs.findAll {!(it.key in ['name', 'id',]) }.each {k, v ->
            GridUtils.setNestedPropertyValue(k, gridConfig, v)
        }
        gridConfig
    }
}
