package org.codehaus.groovy.grails.plugins.easygrid

import groovy.util.logging.Slf4j
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.codehaus.groovy.grails.plugins.web.taglib.FormTagLib
import org.grails.plugin.easygrid.GridConfig
import org.grails.plugin.easygrid.GridUtils

/**
 * Taglib
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
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

        def gridConfig = easygridService.overwriteGridProperties(easygridService.getGridConfig(attrs.controller ?: controllerName, attrs.name), attrs)
        def model = easygridService.htmlGridDefinition(gridConfig)

        if (model) {
            model.attrs = attrs
            out << render(template: gridConfig.gridRenderer, model: model)
        }
    }

    /**
     * renders export buttons for the grid
     *
     * @attr name REQUIRED - the name of the grid
     * @attr id - the javascript id of the grid - if different from the name
     * @attr controller - the controller where the grid is defined ( by default the current controller)
     */
    def exportButton = { attrs, body ->
        if (attrs.id == null) {
            attrs.id = attrs.name
        }

        //ignore the attributes of the export tag
        def gridConfig = easygridService.overwriteGridProperties(easygridService.getGridConfig(attrs.controller ?: controllerName, attrs.name), attrs, ['formats', 'params'])
        attrs.action = "${gridConfig.id}Export"
        out << export.formats(attrs)
    }

    /**
     * renders the filter form
     *
     * @attr name REQUIRED - the name of the grid
     * @attr id - the javascript id of the grid - if different from the name
     * @attr controller - the controller where the grid is defined ( by default the current controller)
     */
    def filterForm = { attrs, body ->
        if (attrs.id == null) {
            attrs.id = attrs.name
        }

        def gridConfig = easygridService.getGridConfig(attrs.controller ?: controllerName, attrs.name)
        def model = easygridService.htmlGridDefinition(gridConfig)
        if (model) {
            model.attrs = attrs
//            out << render(template: gridConfig.gridRenderer, model: model)
            out << render(template: '/templates/easygrid/filterFormRenderer', model: model)
        }
    }

    /**
     * todo - nested ca sa fie coerent si sa poti sa ai si griduri fara coloane
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
//        def gridConfig = getGridConfig([name: attrs.gridName, controller: attrs.controller])
        attrs.disabled = attrs.disabled ? true : false
        attrs.id = attrs.id ?: attrs.name

        //todo -mutate in config
        attrs.title = attrs.title ?: 'default.selectionComponent.title'
        attrs.width = attrs.width ?: 940
        attrs.height = attrs.height ?: 400
        attrs.showAutocompleteBox = (attrs.showAutocompleteBox != null) ? attrs.showAutocompleteBox : true
        attrs.disabled = (attrs.disabled != null) ? attrs.disabled : false

        attrs.showSeparateLabel = (attrs.showSeparateLabel != null) ? attrs.showSeparateLabel : false
        attrs.autocompleteSize = attrs.autocompleteSize ?: (attrs.showSeparateLabel ? 30 : 2)


        def template = grailsApplication.config.easygrid.defaults.autocomplete.template
        out << render(plugin: 'easygrid', template: template, model: [attrs: attrs])
    }



    /**
     * iterates the columns of a grid - depending on the context
     *
     * @attr gridConfig REQUIRED - the grid
     */
    def eachColumn = { attrs, body ->
        GridConfig gridConfig = attrs.gridConfig

//        gridConfig.columns.findAll {col -> (params.selectionComp) ? col.showInSelection : true}.eachWithIndex { col, idx ->
        GridUtils.eachColumn(gridConfig) { col, idx ->
            out << body(col: col, idx: idx, last: (idx == gridConfig.columns.size() - 1))
        }
    }
}
