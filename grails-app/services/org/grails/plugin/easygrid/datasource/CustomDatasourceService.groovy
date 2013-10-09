package org.grails.plugin.easygrid.datasource

import groovy.util.logging.Slf4j
import org.grails.plugin.easygrid.EasygridContextHolder
import static org.grails.plugin.easygrid.EasygridContextHolder.*
/**
 * datasource service for a customizable grid
 * dispatches the call to dataProvider and dataCount Closures defined in the grid
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
class CustomDatasourceService {

    def verifyGridConstraints(gridConfig) {
        def errors = []

        if (!gridConfig.columns) {
            errors.add("if the type of the grid is not 'domain' then you must define the columns")
        }

        if (!gridConfig.dataProvider) {
            errors.add("if the type of the grid is 'custom' then you must define a custom 'dataProvider' closure")
        }

        if (!gridConfig.dataCount) {
            errors.add("if the type of the grid is 'custom' then you must define a custom 'dataCount' closure")
        }

        errors
    }

    /**
     * returns the list of rows
     * by default will return all elements
     * @param listParams - ( like  rowOffset maxRows sort order
     * @param filters - the search filters
     * @return
     */
    def list(gridConfig, Map listParams, filters = null) {

        def closure = gridConfig.dataProvider
        switch (closure.parameterTypes?.size()) {
            case 2:
                return closure.call(filters, listParams)
            case 3:
                return closure.call(gridConfig, filters, listParams)
            default:
                throw new IllegalArgumentException("illegal number of arguments for the dataProvider closure: ${closure.parameterTypes?.size()}  ")
        }
    }

    /**
     * returns the total no of rows
     * @param gridConfig
     * @param filters - when type==domain - it will be a criteria
     * @return
     */
    def countRows(gridConfig, filters = null) {

        //call some custom closure wich returns the rows
//        return gridConfig.dataCount.call(session, gridConfig, filters, params)

        def closure = gridConfig.dataCount
        switch (closure.parameterTypes?.size()) {
            case 1:
                return closure.call(filters)
            case 2:
                return closure.call(gridConfig, filters)
            default:
                throw new IllegalArgumentException("illegal number of arguments for the dataCount closure: ${closure.parameterTypes?.size()}  ")
        }
    }
}
