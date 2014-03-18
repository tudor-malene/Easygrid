package org.grails.plugin.easygrid.datasource

import groovy.util.logging.Slf4j
import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.grails.plugin.easygrid.Filter
import org.grails.plugin.easygrid.FilterOperatorsEnum
import org.grails.plugin.easygrid.Filters
import org.springframework.web.context.request.RequestContextHolder

import static org.grails.plugin.easygrid.EasygridContextHolder.*
import static org.grails.plugin.easygrid.FilterOperatorsEnum.*
import static org.grails.plugin.easygrid.FiltersEnum.and
import static org.grails.plugin.easygrid.FiltersEnum.or

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
    def list(gridConfig, Map listParams, Filters filters = null) {

        def tempList = filteredList(gridConfig, filters)
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

    private filteredList(gridConfig, Filters filters) {
        def list = getList(gridConfig)
        filters ? list.findAll(createFiltersClosure(filters)) : list
    }

    /**
     * traverses the filters structure and creates a criteria closure that will be applied to the Detached Criteria
     * @param filters
     * @return
     */
    private Closure createFiltersClosure(Filters filters) {
        if (filters) {
            filters.postorder(
                    { Filters node, List siblings ->
                        return { row ->
                            boolean result = node.type == and  // true - for 'and' , and false for 'or'
                            for (Closure criteria : siblings) {
                                result = DefaultGroovyMethods."${node.type}"(result, criteria(row))
                                //optimization - if result=0 for 'and' or 1 for 'or' then stop and return
                                if (result == (node.type == or)) {
                                    break
                                }
                            }
                            result
                        }
                    },
                    { Filter filter ->
                        getCriteria(filter)
                    }
            )
        }
    }


    def getCriteria(Filter filter) {
        filter.searchFilter ?: createFilterClosure(filter.operator, filter.filterable.filterProperty, filter.value)
    }

    //thanks to doig ken
    private Closure createFilterClosure(FilterOperatorsEnum operator, String property, Object value) {
        switch (operator) {
            case EQ: return { row -> row[property] == value }
            case NE: return { row -> row[property] != value }
            case LT: return { row -> row[property] < value }
            case LE: return { row -> row[property] <= value }
            case GT: return { row -> row[property] > value }
            case GE: return { row -> row[property] >= value }
            case BW: return { row -> row[property].startsWith(value) }
            case BN: return { row -> !(row[property].startsWith(value)) }
            case IN: return { row -> row[property] in value }
            case NI: return { row -> !(row[property] in value) }
            case EW: return { row -> row[property].endsWith(value) }
            case EN: return { row -> !(row[property].endsWith(value)) }
            case CN: return { row -> row[property].contains(value) }
            case NC: return { row -> !(row[property].contains(value)) }
            default: log.warn("Operation not supported [${op}]")
        }
    }

    /**
     * returns the total no of rows
     * @param gridConfig
     * @param filters - when type==domain - it will be a criteria
     * @return
     */
    def countRows(gridConfig, filters = null) {
        filteredList(gridConfig, filters)?.size()
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
