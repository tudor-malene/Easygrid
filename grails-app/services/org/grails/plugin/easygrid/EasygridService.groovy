package org.grails.plugin.easygrid

import groovy.util.logging.Slf4j
import org.codehaus.groovy.control.ConfigurationException
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException
import org.grails.plugin.easygrid.builder.EasygridBuilder

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * main service class
 * - initializes & validates the grids ( calles te builder & adds default values & types )
 * - dispatches calls to the proper datasource & grid implementation
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
@Mixin(EasygridContextHolder)
class EasygridService {

    static transactional = false

    def grailsApplication
    def gridsDelegate

    static gridsCacheClosure

    private final ReadWriteLock initLock = new ReentrantReadWriteLock();
    private final Lock rLock = initLock.readLock();
    private final Lock wLock = initLock.writeLock();

    /**
     * constructs the configuration from the builder
     * @param controller - the annotated class which
     * @return the initialized grids structure for the controller
     */
    def Map<String, GridConfig> initGrids(controller) {

        if (reloadGrids()) {
            log.debug('clear grid cache')
            //clear or initialize the cache
            memoizeGrids(controller)
        } else {
            log.debug('use grid  cache')
            // do this to avoid NPE  in the rare case when - after a reload - a thread has not acquired the writeLock yet but is in the other if branch
            while (!gridsCacheClosure) {
                sleep(2)
            }
            rLock.lock()
            try {
                gridsCacheClosure(controller)
            } finally {
                rLock.unlock()
            }
        }
    }

    /**
     * write protected method that initializes the grids for a controller
     * run only on the first usage  ( in dev mode - run on first usage after a reload)
     * @param controller
     * @return the initialized grids structure
     */
    def Map<String, GridConfig> memoizeGrids(controller) {
        wLock.lock()
        try {
            gridsCacheClosure = initGridsClosure.memoize()
            gridsCacheClosure(controller)
        } finally {
            wLock.unlock()
        }
    }

    /**
     * closure that initializes the grids defined in a controller
     */
    def initGridsClosure = { controller ->
        log.debug("run init grids for ${controller}")

        def gridsClosure = (controller.hasProperty('grids')) ? controller.grids : ((Class) controller.getAnnotation(Easygrid).externalGrids()).grids

        //call the builder & add the default settings from the config
        generateConfigForGrids(gridsClosure).each { gridName, gridConfig ->

            gridConfig.id = gridName

            //add default & types
            try {
                addDefaultValues(gridConfig, grailsApplication?.config?.easygrid)
            } catch (any) {
                log.error("Failed to initialize grid: ${gridName}, defined in controller ${controller}.", any)
                throw new GrailsConfigurationException("Failed to initialize grid: ${gridName}, defined in controller ${controller}.", any)
            }
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
    def addDefaultValues(GridConfig gridConfig, Map defaultValues) {

        log.trace "start: $gridConfig"
        setLocalGridConfig(gridConfig)

        assert gridConfig.id

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

        GridUtils.copyProperties defaultValues.defaults.autocomplete, gridConfig.autocomplete
        GridUtils.copyProperties defaultValues.defaults.export, gridConfig.export

        //add the predefined types  to the columns
        gridConfig.columns.each { ColumnConfig column ->
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
            GridUtils.copyProperties defaultValues?.columns?.defaults, column, 0
            GridUtils.copyProperties defaultValues?.columns?.defaults[gridConfig.gridImpl], column[gridConfig.gridImpl]
            GridUtils.copyProperties defaultValues?.columns?.defaults?.export, column.export

            // set the format closure
            if (column.formatName) {
                assert defaultValues?.formats[column.formatName]: "No ${column.formatName} formatter defined "
                column.formatter = defaultValues?.formats[column.formatName]
            }

            if (!column.property && !column.value) {
                column.property = column.name
            }

            if (!column.label) {
                def prefix = gridConfig.labelPrefix ?: grails.util.GrailsNameUtils.getPropertyNameRepresentation(gridConfig.domainClass)
                assert prefix
                column.label = grailsApplication.config.easygrid.defaults.labelFormatTemplate.make(labelPrefix: prefix, column: column, gridConfig: gridConfig)
            }

            // add default filterClosure
            if (column.enableFilter && column.filterClosure == null && column.filterFieldType ) {
                assert !column.property.contains('.') : "Currently default properties are supported only for simple properties. Please add the filter closure for ${column.name}"
                def filterClosure = defaultValues?.dataSourceImplementations?."${gridConfig.dataSourceType}"?.filters?."${column.filterFieldType}"
                assert filterClosure: "no default filterClosure defined for '${column.filterFieldType}'"
                column.filterClosure = filterClosure
            }
        }

        //calls the "addDefaultValues" method of the service class for the specific implementation of the grid

        if (implService?.respondsTo('addDefaultValues')) {
            implService.addDefaultValues(defaultValues)
        }

        if (dataSourceService?.respondsTo('addDefaultValues')) {
            dataSourceService.addDefaultValues(defaultValues)
        }

        if (exportService?.respondsTo('addDefaultValues')) {
            exportService.addDefaultValues(defaultValues)
        }

        gridConfig
    }

    /**
     * returns the model for  the html/javascript template code that will render the grid
     * called from the taglib - or from the controller - ( in case of dynamic loading)
     * by default returns the gridConfig
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
                //disable inline editing in selection Mode
                if (params.selectionComp) {
                    gridConfig.inlineEdit = false
                }

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
    def gridData(GridConfig gridConfig) {
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

                //returns a map of search [colName: Closure]
                def filters = implService.filters()
                def listParams = implService.listParams()

                //validation
/*
todo   validation
                def validationClosure = gridConfig.constraints
                if (validationClosure) {
                    def constrainedPropertyBuilder = new ConstrainedPropertyBuilder(cmdObject)
                    validationClosure.setDelegate(constrainedPropertyBuilder)
                    validationClosure()
                    def messageSource = grailsApplication.mainContext?.containsBean('messageSource') ? grailsApplication.mainContext.getBean('messageSource') : null
                    def localErrors = new ValidationErrors(cmdObject, gridConfig.id)

                    for (prop in constrainedPropertyBuilder.constrainedProperties.values()) {
                        prop.messageSource = messageSource
                        prop.validate(cmdObject, cmdObject.getProperty(prop.propertyName), localErrors)
                    }
                    if(localErrors.hasErrors()){
                        println localErrors.errorCount
                    }
                }
*/

                if (filters == null){
                    filters = []
                }

                if (params.selectionComp && gridConfig.autocomplete.constraintsFilterClosure) {
                    //add a new criteria
                    filters.add new Filter(gridConfig.autocomplete.constraintsFilterClosure)
                }

                if (gridConfig.globalFilterClosure){
                    filters.add new Filter(gridConfig.globalFilterClosure)
                }

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
    def valueOfColumn(ColumnConfig column, element, idx) {

        def method = column.property ? this.&valueOfPropertyColumn : this.&valueOfClosureColumn

        method(column, element, idx)
    }


    def valueOfPropertyColumn(ColumnConfig column, element, idx) {
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
        def formatClosure = gridConfig.formats.find { clazz, closure -> clazz.isAssignableFrom(val.getClass()) }?.value
        formatClosure ? formatClosure.call(val) : val
    }

    /**
     * returns the value from the "value" closure
     * @param column
     * @param element
     * @param idx
     * @return
     */
    def valueOfClosureColumn(ColumnConfig column, element, idx) {
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
            throw new UnsupportedOperationException("Inline edit not implemented for ${gridConfig.gridImpl}")
        }
    }

    def export(gridConfig) {
        guard(gridConfig) {
            setLocalGridConfig(gridConfig)
            exportService.export()
        }
    }

/****    utility methods    ******/

    /**
     * generates a config from a grids closure
     * @param gridsConfigClosure
     * @return
     */
    def generateConfigForGrids(Closure gridsConfigClosure) {
        new EasygridBuilder(grailsApplication).evaluate gridsConfigClosure
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
        grailsApplication.mainContext.getBean(gridConfig.export.exportService)
    }

    /**
     * calls the action closure only if the secureProvider passes
     * @param gridConfig
     * @param action
     * @return
     */
    def guard(GridConfig gridConfig, def oper = 'list', Closure action) {

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
