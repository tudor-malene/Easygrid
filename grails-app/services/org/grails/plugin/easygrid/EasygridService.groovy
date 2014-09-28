package org.grails.plugin.easygrid

import groovy.util.logging.Slf4j
import org.springframework.web.context.request.RequestContextHolder

import static org.grails.plugin.easygrid.EasygridContextHolder.getParams
import static org.grails.plugin.easygrid.EasygridContextHolder.storeParams
import static org.grails.plugin.easygrid.GridUtils.cloneGrid
import static org.grails.plugin.easygrid.GridUtils.retreiveLastSearch

/**
 * main service class
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
class EasygridService {

    public static final String GRIDS_REPOSITORY = 'GRIDS_REPOSITORY'
    static transactional = false

    def grailsApplication
    def easygridDispatchService
    def filterService

    /**
     * returns the model for  the html/javascript template code that will render the grid
     * called from the taglib - or from the controller - ( in case of dynamic loading)
     * by default returns the gridConfig
     * @param gridConfig
     * @return - the map that will be passed to the renderer
     */
    def htmlGridDefinition(GridConfig gridConfig) {

        //call the   htmlGridDefinition from the implementation
//            def result = gridConfig.callGridPropertyMethod('gridImplService', 'htmlGridDefinition')
        def result = easygridDispatchService.callGridImplHtmlGridDefinition(gridConfig)
        if (!result) {
            //todo - refactor this
            //disable inline editing in selection Mode
            if (params.selectionComp) {
                gridConfig.inlineEdit = false
            }

            //return a map with the gridConfig
            return [gridConfig: gridConfig]

        }
        result
    }

    /**
     * returns the list of elements formatted for each implementation
     * @param gridConfig
     * @return
     */
    def gridData(GridConfig gridConfig) {

        //save the search params
        GridUtils.storeLastSearch(gridConfig)

        //returns a map of search [colName: Closure]
        def listParams = easygridDispatchService.callGridImplListParams(gridConfig)

        def filters = filters(gridConfig)
        def rows = easygridDispatchService.callDSList(gridConfig, listParams, filters)
        def nrRecords = easygridDispatchService.callDSCountRows(gridConfig, filters)

        easygridDispatchService.callGridImplTransform(gridConfig, rows, nrRecords, listParams)
    }

    /**
     * @param gridConfig
     * @return list of filters after applying all conditions
     */
    Filters filters(GridConfig gridConfig) {
        def filters = new Filters()

        // apply the global filter
        if (gridConfig.globalFilterClosure) {
            filters << filterService.createGlobalFilter(gridConfig.globalFilterClosure)
        }

        //apply the filters input in the actual grid
        filters << easygridDispatchService.callGridImplFilters(gridConfig)

        // apply the selection component constraint filter ( if it's the case )
        if (gridConfig.autocomplete) {
            filters << easygridDispatchService.callACFilters(gridConfig)
        }

        //add the search form filters
        if (gridConfig.filterForm) {
            filters << easygridDispatchService.callFFFilters(gridConfig)
        }

        filters
    }

    def export(GridConfig gridConfig) {
        log.debug("export ${gridConfig}")

        def extension = params.extension
        def format = params.format

        if (format == null) {
            // hack - incompatibility between the export plugin and grails >=2.3.5
            //this should fix it temporarily
            format = RequestContextHolder.currentRequestAttributes().originalParams.format
        }

        // restore the previous search params
        storeParams(retreiveLastSearch(gridConfig))

        //apply the previous filters, fetch all the data & call the export method
        def listParams = easygridDispatchService.callGridImplListParams(gridConfig)
        assert gridConfig.export.maxRows: "You must define maxRows"
        listParams.maxRows = gridConfig.export.maxRows
        listParams.rowOffset = 0
        easygridDispatchService.callExportExport(gridConfig, easygridDispatchService.callDSList(gridConfig, listParams, filters(gridConfig)), format, extension)
    }

    ///////////////////////////// INLINE EDIT /////////////////

    /**
     * dispatches the Edit operation
     * @param gridConfig
     */
    def inlineEdit(GridConfig gridConfig) {

        def oper = params.oper

        def response = new InlineResponse()

        switch (oper) {
            case 'add':
                gridConfig.saveRowClosure ? gridConfig.saveRowClosure(gridConfig, response) : easygridDispatchService.callDSSaveRow(gridConfig, response)
                break
            case 'edit':
                gridConfig.updateRowClosure ? gridConfig.updateRowClosure(gridConfig, response) : easygridDispatchService.callDSUpdateRow(gridConfig, response)
                break
            case 'del':
                gridConfig.delRowClosure ? gridConfig.delRowClosure(gridConfig, response) : easygridDispatchService.callDSDelRow(gridConfig, response)
                break
            default:
                throw new RuntimeException("unknown oper: ${oper}")
        }
        response
    }

    /**
     * returns the grid from the specified controller  ( by default the current )
     * @param attrs
     * @return
     */
    GridConfig getGridConfig(controller, gridName) {
        gridRepository[controller][gridName]
    }

    def setGridConfig(controller, gridName, GridConfig gridConfig) {
        synchronized (gridRepository) {
            if (gridRepository[controller] == null) {
                gridRepository[controller] = [:]
            }
            gridRepository[controller][gridName] = gridConfig
        }
        gridConfig
    }

    def getGridRepository() {
        grailsApplication.mainContext.servletContext.getAttribute(GRIDS_REPOSITORY)
    }

    def setGridRepository(grids) {
        grailsApplication.mainContext.servletContext.setAttribute(GRIDS_REPOSITORY
                , Collections.synchronizedMap(grids))
    }


    GridConfig overwriteGridProperties(GridConfig gridConfig, attrs, ignoreProps = []) {

        def gridClone = cloneGrid gridConfig
        //overwrite grid properties
        attrs.findAll { !(it.key in (['name', 'id', 'controller'] + ignoreProps)) }.each { property, value ->
            try {
                GridUtils.setNestedPropertyValue(property, gridClone, value)
            } catch (any) {
                log.error("Could not set property '${property}' on grid '${gridConfig.id}'. Ignoring...", any)
            }
        }
        gridClone
    }

/****    utility methods    ******/

    /**
     * calls the action closure only if the secureProvider passes
     * @param gridConfig
     * @param action
     * @return
     */
    def guard(GridConfig gridConfig, Closure action) {

        assert gridConfig

        def display = true

        //todo - make implementation dependent
        def oper = params.oper ?: 'list'

        //check if there is a securityProvider defined
        if (gridConfig.securityProvider) {
            display = gridConfig.securityProvider(gridConfig, oper)
        }

        if (display) {
            action()
        }
    }

}
