package org.grails.plugin.easygrid

import groovy.util.logging.Log4j

import org.codehaus.groovy.control.ConfigurationException

/**
 * main service class
 * - initializes & validates the grids ( calles te builder & adds default values & types )
 * - dispatches calls to the proper datasource & grid implementation
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */

@Log4j
@Mixin(EasygridContextHolder)
class EasygridService {

    static transactional = false

    def grailsApplication
    def gridsDelegate

    static def gridsCacheClosure

    /**
     * constructs the configuration from the builder
     * @param controller - the annotated class which
     * @return
     */
    def initGrids(controller) {
        if (reloadGrids()) {
            log.debug('clear cache')
            gridsCacheClosure = initGridsClosure.memoize()
            gridsCacheClosure(controller)
        } else {
            log.debug('use cache')
            gridsCacheClosure(controller)
        }
    }

    /**
     * closure that initializes the grids defined in a controller
     */
    def initGridsClosure = { controller ->

        log.debug("run init grids for ${controller}")

        //call the builder & add the default settings from the config
        generateConfigForGrids(controller.grids).each {gridName, gridConfig ->

            gridConfig.id = gridName

            //add default & types
            addDefaultValues(gridConfig, grailsApplication?.config?.easygrid)
        }
    }

    /**
     * check the validity of the configuration
     * @param gridConfig
     * @return
     */
    def verifyGridConstraints(gridConfig) {
        def errors = []

        if (!gridConfig.dataSourceType) {
            errors.add("Grid data source type is mandatory")
            return errors
        }

        dataSourceService.verifyGridConstraints(gridConfig)
    }

    /**
     * adds the default values specified in the config file - to the grid configuration
     * @param gridConfig
     * @param defaultValues
     * @return
     */
    def addDefaultValues(gridConfig, Map defaultValues) {

        log.trace "start: $gridConfig"
        setLocalGridConfig(gridConfig)

        assert gridConfig.id
        if (gridConfig?.export_title == null) {
            gridConfig.export_title = gridConfig.id
        }

        log.trace "before defaults: $gridConfig"

        //add the default values for the mandatory properties ( impl, type )
        GridUtils.copyProperties defaultValues.defaults, gridConfig, 1

        log.trace "before impls: $gridConfig"

        assert gridConfig.gridImpl
        assert gridConfig.dataSourceType

        if (!gridConfig[gridConfig.gridImpl]) {
            gridConfig[gridConfig.gridImpl] = [:]
        }
        //add the default values for the impl
        GridUtils.copyProperties defaultValues.defaults[gridConfig.gridImpl], gridConfig[gridConfig.gridImpl]

        // handle the implementations...
        GridUtils.copyProperties defaultValues.gridImplementations[gridConfig.gridImpl], gridConfig
        GridUtils.copyProperties defaultValues.dataSourceImplementations[gridConfig.dataSourceType], gridConfig

        assert gridConfig.gridImplService
        assert gridConfig.gridRenderer
        assert gridConfig.dataSourceService

        log.trace "after impls: $gridConfig"

        //validate the grid
        def errors = verifyGridConstraints(gridConfig)
        if (errors) {
            throw new ConfigurationException("Grid ${gridConfig.id} not defined correctly: $errors")
        }

        if (dataSourceService?.respondsTo('generateDynamicColumns')) {
            dataSourceService.generateDynamicColumns()
        }

        //add the predefined types  to the columns
        gridConfig.columns.each {Column column ->
            if (!column[gridConfig.gridImpl]) {
                column[gridConfig.gridImpl] = [:]
            }
            if (!column.export) {
                column.export = [:]
            }

            // copy the properties from the predefined type
            if (column.type) {
                def type = defaultValues?.columns?.types?."$column.type"

                if (!type) {
                    throw new RuntimeException("type ${column.type} not defined for grid ${gridConfig.id}")
                }

                GridUtils.copyProperties type, column, 1
                GridUtils.copyProperties type[gridConfig.gridImpl], column[gridConfig.gridImpl]
                GridUtils.copyProperties type.export, column.export
                column.type = null
            }

            // copy the properties for the grid implementation & for the export
            GridUtils.copyProperties defaultValues?.columns?.defaults[gridConfig.gridImpl], column[gridConfig.gridImpl]
            GridUtils.copyProperties defaultValues?.columns?.defaults?.export, column.export

            // set the format closure
            if (column.formatName) {
                column.formatter = defaultValues?.formats[column.formatName]
                assert column.formatter
            }

        }

        //calls the "addDefaultValues" method of the service class for the specific implementation of the grid

        if (implService?.respondsTo('addDefaultValues')) {
            implService.addDefaultValues(defaultValues)
        }

    }

    /**
     * returns the model for  the html/javascript template code that will render the grid
     * called from the taglib
     * by default passes the gridConfig
     * @param gridConfig
     * @return - the map that will be passed to the renderer
     */
    def htmlGridDefinition(gridConfig) {

        guard(gridConfig) {
            //call the   htmlGridDefinition from the implementation
            setLocalGridConfig(gridConfig)
            if (implService?.respondsTo('htmlGridDefinition')) {
                implService.htmlGridDefinition(gridConfig)
            } else {
                //return a map with the gridConfig
                [gridConfig: gridConfig]
            }
        }
    }

    /**
     * returns the list of elements formatted for each implementation
     * @param gridConfig
     * @return
     */
    def gridData(gridConfig) {
        guard(gridConfig) {

            //store the grid to threadLocal
            setLocalGridConfig(gridConfig)

            //save or restore the search params
            GridUtils.restoreSearchParams()

            if (implService?.respondsTo('gridData')) {
                implService.gridData()

            } else {

                //check that the service implements the necessary methods
                assert implService.respondsTo('filters')
                assert implService.respondsTo('listParams')
                assert implService.respondsTo('transform')

                //returns a list of search Closures
                def filters = implService.filters()
                def listParams = implService.listParams()

                def rows = dataSourceService.list(listParams, filters)

                def nrRecords = dataSourceService.countRows(filters)

                implService.transform(rows, nrRecords, listParams)
            }
        }
    }

    /**
     * return the value for a column from a row
     * the main link between the datasource & the grid Implementation
     * @param column - the column from the config
     * @param element - the data
     * @param row - the index - used for numberings
     * @return
     */
    def valueOfColumn(Column column, element, idx) {

        def method = column.property ? this.&valueOfPropertyColumn : this.&valueOfClosureColumn

        method(column, element, idx)
    }


    def valueOfPropertyColumn(Column column, element, idx) {
        assert column.property
        def val = GridUtils.getNestedPropertyValue(column.property, element)

        if (val == null) {
            return null
        }

        //apply the format
        if (column.formatter) {
            return column.formatter(val)
        }

        // apply the default value formats
        def formatClosure = gridConfig.formats.find {clazz, closure -> clazz.isAssignableFrom(val.class)}?.value
        formatClosure ? formatClosure.call(val) : val
    }

    /**
     * returns the value from the "value" closure
     * @param column
     * @param element
     * @param idx
     * @return
     */
    def valueOfClosureColumn(Column column, element, idx) {
        assert column.value
        Closure closure = column.value
        switch (closure?.parameterTypes?.size()) {
            case null:
                return ''
            case 1:
                return closure.call(element)
            case 2:
                return closure.call(element, params)
            case 3:
                return closure.call(element, params, idx + 1)
        }
    }


    def supportsInlineEdit(gridConfig) {
        grailsApplication.config?.easygrid?.gridImplementations[gridConfig.gridImpl].inlineEdit
    }

    def inlineEdit(gridConfig) {
        setLocalGridConfig(gridConfig)

        if (implService?.respondsTo('inlineEdit')) {
            implService.inlineEdit()
        } else {
            throw new UnsupportedOperationException("Inline edit not implemented for ${gridConfig.gridImpl}");
        }

    }

    def export(gridConfig) {
        guard(gridConfig) {
            setLocalGridConfig(gridConfig)
            exportService.exportXls()
        }
    }

/****    utility methods    ******/

    /**
     * generates a config from a grids closure
     * @param gridsConfigClosure
     * @return
     */
    def generateConfigForGrids(Closure gridsConfigClosure) {
        def gridsConfig = [:]
        gridsDelegate.grids = gridsConfig
        gridsConfigClosure.delegate = gridsDelegate
        gridsConfigClosure.resolveStrategy = Closure.DELEGATE_FIRST
        gridsConfigClosure()
        gridsConfig
    }

    /**
     * returns the impl service
     * @param gridConfig
     * @return
     */
    def getImplService() {
        grailsApplication.mainContext.getBean(gridConfig.gridImplService)
    }

    /**
     * returns the impl service
     * @param gridConfig
     * @return
     */
    def getDataSourceService() {
        grailsApplication.mainContext.getBean(gridConfig.dataSourceService)
    }

    /**
     * returns the export service
     * @param gridConfig
     * @return
     */
    def getExportService() {
        grailsApplication.mainContext.getBean(gridConfig.exportService)
    }

    /**
     * calls the action closure only if the secureProvider passes
     * @param gridConfig
     * @param action
     * @return
     */
    def guard(Grid gridConfig, def oper = 'list', Closure action) {

        assert gridConfig

        def display = true
        //check if there is a securityProvider defined
        if (gridConfig.securityProvider) {
            display = gridConfig.securityProvider(gridConfig, oper)
        }

        if (display) {
            action()
        }

    }

}
