package org.codehaus.groovy.grails.plugins.easygrid

import groovy.util.logging.Slf4j
import org.grails.plugin.easygrid.ColumnConfig
import org.grails.plugin.easygrid.GridConfig
import org.grails.plugin.easygrid.GridUtils

import java.util.concurrent.atomic.AtomicInteger

import static org.grails.plugin.easygrid.GridUtils.setNestedPropertyValue

/**
 * Taglib
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
class EasygridTagLib {

    static namespace = "grid"

    def easygridService
    def easygridInitService
    def grailsApplication

    //used for generating unique grid names
    static AtomicInteger gridCounter = new AtomicInteger(0)

    /**
     * Include the code for the grid
     *
     * @attr name - the name of the grid
     * @attr domain - the domain class to scaffold the grid
     * @attr id - the javascript id of the component ( by default the grid name)
     * @attr controller - the controller where the grid is defined ( by default the current controller)
     * @attr masterGrid -(only for subgrids) the id of the master grid
     * @attr childParamName -(only for subgrids) the parameter name which can be used in the globalFilterClosure to retrieve the id of the selected master row
     */
    def grid = { attrs, body ->

        def gridConfig
        if (!attrs.name) {
            //will create an adhoc grid -this is only for quick prototyping
            if (!attrs.domainClass) throwTagError("Tag [grid] must refer a grid defined in a controller or a must specify a domain class to scaffold a grid")

            def controller = grailsApplication.controllerClasses.find {
                it.logicalPropertyName == retrieveController(attrs)
            }
            if (!controller) {
                throwTagError("No controller: ${attrs.controller} found")
            }

            def gridName = "${actionName}${gridCounter.incrementAndGet()}"

            //create a new gorm grid config from a domain
            gridConfig = easygridInitService.initializeGrid(controller, gridName, attrs.domainClass)
            log.debug("Grid ${gridName} created for controller ${controller} and domain ${attrs.domainClass}")

        } else {
            //use a grid defined in a controller
            if (attrs.id == null) {
                attrs.id = attrs.name
            }

            def originalGridConfig = easygridService.getGridConfig(retrieveController(attrs), attrs.name)
            if (!originalGridConfig) {
                throwTagError("Could not find grid definition for controller: ${retrieveController(attrs)} and grid name: ${attrs.name}")
            }
            gridConfig = easygridService.overwriteGridProperties(originalGridConfig, attrs)
        }

        pageScope.setVariable(CURRENT_GRID, gridConfig)
        body()
        pageScope.setVariable(CURRENT_GRID, null)
        def model = easygridService.htmlGridDefinition(gridConfig)

        if (model) {
            model.attrs = attrs
            out << render(template: gridConfig.gridRenderer, model: model)
        }

    }

    private static final String CURRENT_GRID = 'currentGrid'

    /**
     * overwrite view properties
     *
     * @attr col - the name of the column - if not specified , grid properties will be overwritten
     */
    def set = { attrs, body ->
        GridConfig grid = pageScope.getVariable(CURRENT_GRID)
        if (!grid) {
            throwTagError("'grid:set' tag must be nested inside a 'grid:grid' tag")
        }
        ColumnConfig column
        if (attrs.col) {
            def col = attrs.remove('col')
            column = grid.columns[col]
            if (!column) {
                throwTagError("${col} is not a valid column")
            }

            def label = attrs.remove('label')
            if (label) {
                column.label = label
            }
/*
            def order = attrs.remove('order')
            if (order) {
                grid.columns.move(col, order as int)
            }
*/
        }
        def gridImpl = grid.gridImpl
        attrs.each { k, v ->
            setNestedPropertyValue(k, column ? column[gridImpl] : grid[gridImpl], v)
        }

    }

    /**
     * can be used for quick prototyping
     * the body will be evaluated and the result copied in the grid property section
     * (attention - property conflicts may happen - better use set)
     */
    def p = { attrs, body ->
        GridConfig grid = pageScope.getVariable(CURRENT_GRID)
        if (!grid) {
            throwTagError("'grid:c' tag must be nested inside a 'grid:grid' tag")
        }
        String val = body()
        val.split(',').each { String tkn ->
            if (tkn.trim()) {
                def key = tkn.split(':')[0]
                grid.jqgrid.remove(key.trim())
            }
        }
        grid.otherProperties = val
    }

    /**
     * can be used for quick prototyping
     * the body will be evaluated and the result copied in the grid property section
     * (attention - property conflicts may happen - better use set)
     *
     * @attr name - the name of the column
     */
    def c = { attrs, body ->
        def colName = attrs.name
        GridConfig grid = pageScope.getVariable(CURRENT_GRID)
        if (!grid) {
            throwTagError("'grid:c' tag must be nested inside a 'grid:grid' tag")
        }
        ColumnConfig currentColumn = grid.columns[colName]
        String val = body()
        val.split(',').each { String tkn ->
            if (tkn.trim()) {
                def key = tkn.split(':')[0]
                currentColumn.jqgrid.remove(key.trim())
            }
        }
        currentColumn.otherProperties = val
    }

    /**
     * renders export buttons for the grid
     *
     * @attr name REQUIRED - the name of the grid
     * @attr id - the javascript id of the grid - if different from the name
     * @attr controller - the controller where the grid is defined ( by default the current controller)
     */
    def exportButton = { attrs, body ->
        if (!attrs.name) throwTagError("Tag [exportButton] is missing required attribute [name]")

        if (attrs.id == null) {
            attrs.id = attrs.name
        }

        //ignore the attributes of the export tag
        def gridConfig = easygridService.overwriteGridProperties(easygridService.getGridConfig(retrieveController(attrs), attrs.name), attrs, ['formats', 'params'])
        attrs.action = "${gridConfig.id}Export"
        attrs.params = GridUtils.externalParams(gridConfig)

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
     * form tag that will submit the content to a grid and trigger a reload
     * to be used for custom filters
     */
    def gridForm = { attrs, body ->
        if (attrs.id == null) {
            attrs.id = attrs.name
        }
        out << "<form name='${attrs.id}' onsubmit=\""
        if (attrs.function) {
            out << "${attrs.function};"
        }
        out << "return easygrid.filterForm('${attrs.name}_table',this)"
        out << "\">"
        out << body()
        out << "</form> "
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

    /**
     *  can be used in javascript to retrieve the column idx using the column name:
     *  for ex: var age = rowObject[${grid.columnIndex(column:'age')}]
     */
    def columnIndex = { attrs, body ->
        if (!attrs.name) throwTagError("Tag [columnIndex] is missing required attribute [name]")

        //ignore the attributes of the export tag
        GridConfig gridConfig = easygridService.getGridConfig(retrieveController(attrs), attrs.name)

        int colNo = -1
        for (int i = 0; i < gridConfig.columns.size(); i++) {
            ColumnConfig cc = gridConfig.columns[i]
            if (cc.name == attrs.column) {
                colNo = i
                break;
            }
        }
        out << colNo
    }


    private retrieveController(attrs) {
        attrs.controller ?: controllerName
    }
}
