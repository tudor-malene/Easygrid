package org.grails.plugin.easygrid.datasource

import groovy.util.logging.Slf4j
import org.grails.plugin.easygrid.EasygridContextHolder
import org.grails.plugin.easygrid.Filter
import org.springframework.web.context.request.RequestContextHolder
import static org.grails.plugin.easygrid.EasygridContextHolder.*

/**
 * Datasource implementation
 * the rows are stored in a context ( by default 'session')
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
class ListDatasourceService {

    def verifyGridConstraints(gridConfig) {
        def errors = []

        if (!gridConfig.columns) {
            errors.add("if the type of the grid is not 'domain' then you must define the columns")
        }

        if (!gridConfig.attributeName) {
            errors.add("if the type of the grid is 'list' then you must define a custom 'attributeName' attribute, that will return the list from the specified context")
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

        Collection tempList = filters.inject(getList(gridConfig)) { list, Filter filter ->
            list.findAll getCriteria(filter)
        }

        if (tempList) {
            def offset = listParams.rowOffset ?: 0
            def maxRows = listParams.maxRows ?: tempList.size()
            def end = (offset + maxRows > tempList.size()) ? tempList.size() - 1 : offset + maxRows - 1
            if (end >= offset) {
                tempList = tempList[offset..end]
                def orderBy = []
                if (listParams.multiSort) {
                    orderBy = listParams.multiSort
                } else {
                    if (listParams.sort) {
                        def entry = [:]
                        entry.sort = listParams.sort
                        entry.order = listParams.order ?: 'asc'
                        orderBy << entry
                    }
                }

                return orderBy.inject(tempList) { acc, val ->
                    acc.sort { a, b ->
                        def comp = a[val.sort] <=> b[val.sort]
                        (val.order == 'asc') ? comp : -comp
                    }
                }

            }
        }
        []
    }

    def getCriteria(Filter filter) {
/*
        Closure curriedClosure = filter.searchFilter
        curriedClosure = curriedClosure.curry(filter)
//        if (curriedClosure.parameterTypes.size() > 1) {
//            curriedClosure = curriedClosure.curry(params)
//        }
        curriedClosure
*/
        filter.searchFilter.curry(filter)
    }

    /**
     * returns the total no of rows
     * @param gridConfig
     * @param filters - when type==domain - it will be a criteria
     * @return
     */
    def countRows(gridConfig, filters = null) {
        filters.inject(getList(gridConfig)) { list, Filter filter ->
            list.findAll getCriteria(filter)
        }.size()

    }

    // inlineEdit implementations

    /**
     * default method called on updating a grid element
     */
    def updateRow(gridConfig) {

        def instance = getList(gridConfig)[params.id as int]
        if (!instance) {
            return 'default.not.found.message'
        }

/*
        if (params.version) {
            def version = params.version.toLong()
            if (instance.version > version) {
                return 'default.optimistic.locking.failure'
            }
        }
*/

        //default returns params
        gridConfig.beforeSave(params).each { k, v ->
            instance[k] = v
        }
/*

        if (!instance.save(flush: true)) {
            return instance.errors
        }
*/
    }

    /**
     * default method  called on saving a new grid element
     */
    def saveRow(gridConfig) {
        getList(gridConfig).add gridConfig.beforeSave params
    }

    /**
     * default method  called on deleting a grid element
     */
    def delRow(gridConfig) {
        def instance = getList(gridConfig)[params.id as int]

        if (!instance) {
//            Errors errors = new
            return 'default.not.found.message'
        }
        getList(gridConfig).remove(params.id as int)
    }


    def getList(gridConfig) {
        def ctx
        switch (gridConfig.context) {
            case null:
            case 'session':
                ctx = session
                break
            case 'applicationContext':
                ctx = RequestContextHolder.currentRequestAttributes().applicationContext
                break
            case 'request':
                ctx = request
                break
            case 'flash':
                ctx = flashScope
                break

        }

        ctx[gridConfig.attributeName]
    }
}
