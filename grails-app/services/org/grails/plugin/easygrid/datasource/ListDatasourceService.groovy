package org.grails.plugin.easygrid.datasource

import groovy.util.logging.Slf4j
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.web.binding.DataBindingUtils
import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.grails.plugin.easygrid.*
import org.springframework.beans.BeanUtils
import org.springframework.web.context.request.RequestContextHolder

import static org.grails.plugin.easygrid.EasygridContextHolder.*
import static org.grails.plugin.easygrid.FilterOperatorsEnum.*
import static org.grails.plugin.easygrid.FilterUtils.getOperatorMapKey
import static org.grails.plugin.easygrid.FiltersEnum.and
import static org.grails.plugin.easygrid.FiltersEnum.or
import static org.grails.plugin.easygrid.GridUtils.getDomainProperty
import static org.grails.plugin.easygrid.GridUtils.getNestedPropertyValue
import static org.grails.plugin.easygrid.GridUtils.valueOfSortColumn
import static org.grails.plugin.easygrid.Filter.v

/**
 * Datasource implementation
 * the rows are stored as a list in a context ( by default 'session')
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

    def addDefaultValues(GridConfig gridConfig, Map defaultValues) {

        gridConfig.columns.each { ColumnConfig column ->
            if (!column.filterProperty) {
                column.filterProperty = column.property
            }
        }

        (gridConfig.columns.elementList + gridConfig.filterForm?.fields?.elementList).findAll {
            it?.enableFilter
        }.each { FilterableConfig filterable ->
            def property = filterable.filterProperty
            if (property && gridConfig.listClass) {
                if (!filterable.filterDataType) {
                    Class columnPropertyType = BeanUtils.findPropertyType(property, gridConfig.listClass)
                    filterable.filterDataType = columnPropertyType
                    if (!columnPropertyType) {
                        log.warn("Property '${property}' for grid: ${gridConfig.id} does not exist in domain class ${gridConfig.domainClass}")
                    }
                }
                if (!filterable.filterType) {
                    filterable.filterType = getOperatorMapKey(filterable.filterDataType)
                }

            } else {

                //by default - if no other config -
                if (!filterable.filterDataType) {
                    filterable.filterDataType = String
                }
                if (!filterable.filterType) {
                    filterable.filterType = getOperatorMapKey(filterable.filterDataType)
                }
            }
        }
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

                orderBy.each { val ->
                    def sortCol = gridConfig.columns[val.sort]
                    assert sortCol
                    def sort = valueOfSortColumn(gridConfig, sortCol)

                    if (sort instanceof Closure) {
                        //execute the closure
                        tempList = tempList.sort(sort.curry(val.order))
                    } else {
                        tempList = tempList.sort { a, b ->
                            def comp = a[sort] <=> b[sort]
                            (val.order == 'asc') ? comp : -comp
                        }
                    }
                }
                return tempList[offset..end]
            }
        }

        //return an empty array
        []
    }

    private filteredList(gridConfig, Filters filters) {
        def list = getList(gridConfig)
        filters ? list.findAll(createFiltersClosure(filters)) : list
    }

    /**
     * traverses the filters structure and creates a criteria closure that will be applied to the
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
                                if(criteria) {
                                    result = DefaultGroovyMethods."${node.type}"(result, criteria(row))
                                    //optimization - if result=0 for 'and' or 1 for 'or' then stop and return
                                    if (result == (node.type == or)) {
                                        break
                                    }
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
            case EQ: return v(value){ row -> row[property] == value }
            case NE: return v(value){ row -> row[property] != value }
            case LT: return v(value){ row -> row[property] < value }
            case LE: return v(value){ row -> row[property] <= value }
            case GT: return v(value){ row -> row[property] > value }
            case GE: return v(value){ row -> row[property] >= value }
            case BW: return v(value){ row -> row[property].startsWith(value) }
            case BN: return v(value){ row -> !(row[property].startsWith(value)) }
            case IN: return v(value){ row -> row[property] in value }
            case NI: return v(value){ row -> !(row[property] in value) }
            case EW: return v(value){ row -> row[property].endsWith(value) }
            case EN: return v(value){ row -> !(row[property].endsWith(value)) }
            case CN: return v(value){ row -> row[property].contains(value) }
            case NC: return v(value){ row -> !(row[property].contains(value)) }
            case NU: return { row -> row[property] == null }
            case NN: return { row -> row[property] != null }
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
    def updateRow(gridConfig, InlineResponse response) {

        def instance = findById(gridConfig, params.id)
//        def clone = instance.properties

        if (!instance) {
            response.message = 'default.not.found.message'
            return
        }

        def parameters = gridConfig.beforeSave(params)
        DataBindingUtils.bindObjectToInstance(instance, parameters)

        //for validateable classes
        if (instance.respondsTo('validate')) {
            instance.validate()
        }
//        if (instance.hasProperty('errors') && instance.errors.hasErrors()) {
//            //restore
//            DataBindingUtils.bindObjectToInstance(instance, clone)
//        }

        response.instance = instance
    }

    /**
     * default method  called on saving a new grid element
     */
    def saveRow(gridConfig, InlineResponse response) {
        def instance
        def parameters = gridConfig.beforeSave(params)
        def list = getList(gridConfig)

        if (gridConfig.listClass) {
            instance = gridConfig.listClass.newInstance()
            DataBindingUtils.bindObjectToInstance(instance, parameters)
            //for validateable classes
            if (instance.respondsTo('validate')) {
                instance.validate()
            }
            response.instance = instance
            if (!instance.hasProperty('errors') || !instance.errors.hasErrors()) {
                //todo
                list.add(instance)
            }
        } else {
            list.add(parameters)
        }
    }

    /**
     * default method  called on deleting a grid element
     */
    def delRow(gridConfig, InlineResponse response) {
        def instance = findById(gridConfig, params.id)

        if (!instance) {
            response.message = 'default.not.found.message'
            return
        }

        getList(gridConfig).remove(instance)
    }

    def findById(GridConfig gridConfig, String idString) {
        if (gridConfig.findById) {
            return gridConfig.findById(params.id)
        }

        def list = getList(gridConfig)
        def id = idString.asType(gridConfig.idColType)

        list.find { it[gridConfig.idColName] == id }
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
        getNestedPropertyValue(gridConfig.attributeName, ctx)
    }

}
