package org.grails.plugin.easygrid.datasource

import grails.gorm.DetachedCriteria
import groovy.util.logging.Slf4j
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.scaffolding.DomainClassPropertyComparator
import org.grails.datastore.mapping.query.api.Criteria
import org.grails.plugin.easygrid.*
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional

import static org.codehaus.groovy.grails.commons.GrailsClassUtils.getStaticPropertyValue
import static org.grails.plugin.easygrid.EasygridContextHolder.getParams
import static org.grails.plugin.easygrid.FilterOperatorsEnum.*
import static org.grails.plugin.easygrid.GridUtils.*

/**
 * Datasource implementation for a GORM Domain class
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
@Transactional
class GormDatasourceService {

    def grailsApplication
//    def filterPaneService
    def filterService

    /**
     * if no columns specified in the gridConfig - then generate the columns from the properties of the domain class
     * - useful during prototyping
     * @param gridConfig
     */
    def generateDynamicColumns(gridConfig) {

        // only generate if there are no columns defined
        if (!gridConfig.columns && gridConfig.domainClass) {
            GrailsDomainClass domainClass = resolveDomainClass grailsApplication, gridConfig.domainClass

            def idProperty = domainClass.identifier
            if (idProperty) {
                def idCol = new ColumnConfig(property: idProperty.name, type: 'id', name: 'id')
                idCol.valueType = idProperty.type
                gridConfig.columns.add(idCol)
            }

            resolvePersistentProperties(domainClass).each { GrailsDomainClassProperty prop ->
                if (prop.isAssociation()) {
                    return
                }
                def column = new ColumnConfig(property: prop.name, name: prop.name,)
                column.valueType = prop.type
//                //todo -add other info from the property
                gridConfig.columns.add column
            }
        }
    }


    def addDefaultValues(GridConfig gridConfig, Map defaultValues) {

        gridConfig.columns.each { ColumnConfig column ->
            // add default filterClosure
            if (column.filterClosure == null) {

                if (column.filterFieldType == null) {
                    if (gridConfig.domainClass) {
//                        assert column.property: "you must specify a filterFieldType for ${column.name}"
                        if (column.property) {
                            Class columnPropertyType = getPropertyType(grailsApplication, gridConfig.domainClass, column.property)
                            column.dataType = columnPropertyType
                            if (!columnPropertyType) {
                                log.warn("Property '${column.property}' for grid: ${gridConfig.id} does not exist in domain class ${gridConfig.domainClass}")
                            }
                            switch (columnPropertyType) {
                                case String:
                                    column.filterFieldType = 'text'
                                    break
                                case int:
                                case Integer:
                                    column.filterFieldType = 'integerF'
                                    break
                                case long:
                                case Long:
                                    column.filterFieldType = 'longF'
                                    break
                                case double:
                                case Double:
                                    column.filterFieldType = 'doubleF'
                                    break
                                case float:
                                case Float:
                                    column.filterFieldType = 'floatF'
                                    break
                                case BigDecimal:
                                    column.filterFieldType = 'bigDecimalF'
                                    break
                                case Date:
                                    column.filterFieldType = 'date'
                                    break
                                default:
                                    break
                            }
                        }
                    }
                }

/*
                if (column.filterFieldType) {
                    def filterClosure = defaultValues?.dataSourceImplementations?."${gridConfig.dataSourceType}"?.filters?."${column.filterFieldType}"
                    assert filterClosure: "no default filterClosure defined for '${column.filterFieldType}'"
                    column.filterClosure = filterClosure
                }
*/
            }


        }
    }

    /**
     * "inspired" from Rob Fletcher's fields plugin
     * @param domainClass
     * @param ignoreList
     * @return
     */
    private List<GrailsDomainClassProperty> resolvePersistentProperties(GrailsDomainClass domainClass, ignoreList = []) {
        def properties = domainClass.persistentProperties as List

        def blacklist = ignoreList
        blacklist << 'dateCreated' << 'lastUpdated'
        def scaffoldProp = getStaticPropertyValue(domainClass.clazz, 'scaffold')
        if (scaffoldProp) {
            blacklist.addAll(scaffoldProp.exclude)
        }
        properties.removeAll { it.name in blacklist }
        properties.removeAll { !it.domainClass.constrainedProperties[it.name]?.display }
        properties.removeAll { it.derived }

        Collections.sort(properties, new DomainClassPropertyComparator(domainClass))
        properties
    }


    def verifyGridConstraints(gridConfig) {
        def errors = []
        if (!gridConfig.domainClass) {
            errors.add("if the type of the grid is 'gorm' then you must specify the domainClass")
        }
        errors
    }

    /**
     * returns the list of rows
     * by default will return all elements
     * @param listParams - ( like  rowOffset maxRows sort order)
     * @param filters - array of criterias
     * @return
     */
    def list(gridConfig, Map listParams = [:], filters = null) {
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

        addOrderBy(createWhereQuery(gridConfig, filters), orderBy).list(max: listParams.maxRows, offset: listParams.rowOffset)
    }

    /**
     * returns an element by id
     * @param id
     */
    def getById(GridConfig gridConfig, id) {
        String idProp = gridConfig.autocomplete.idProp
        if (id != null) {
//            createWhereQuery(gridConfig, [new Filter({ filter -> eq(idProp, id) })]).find()
            createWhereQuery(gridConfig, filterService.createGlobalFilters { eq(idProp, id) }).find()
        }
    }

    /**
     * returns the total no of rows
     * @param filters - it will be a criteria
     * @return
     */
    def countRows(gridConfig, filters = null) {
        createWhereQuery(gridConfig, filters).count()
    }

    /**
     * combines all the filters into a gorm where query
     * @param filters - map of filter closures
     * @return
     */
    Criteria createWhereQuery(GridConfig gridConfig, Filters filters) {
        DetachedCriteria baseCriteria = new DetachedCriteria(gridConfig.domainClass)
        if (gridConfig.initialCriteria) {
            baseCriteria = baseCriteria.build(gridConfig.initialCriteria)
        }

        Closure filterCriteria = createFiltersClosure(filters)
        if (filterCriteria) {
            filterCriteria.resolveStrategy = Closure.DELEGATE_FIRST
            filterCriteria.delegate = baseCriteria
            filterCriteria()
        }
        return baseCriteria

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
                        return {
                            "${node.type}" {
                                def del = delegate
                                siblings.each { Closure criteria ->
                                    criteria.delegate = del
                                    criteria.resolveStrategy = DELEGATE_FIRST
                                    criteria()
                                }
                            }
                        }
                    },
                    { Filter filter ->
                        getCriteria(filter)
                    }
            )
        }
    }

    /**
     * transforms a filter into a criteria closure
     * supports nested
     * @param filter
     * @return
     */
    def getCriteria(Filter filter) {
        if(filter.searchFilter){
            return filter.searchFilter
        }

        //create a dynamic closure
        def c = createFilterClosure(filter.operator, lastProperty(filter.filterable.name), filter.value)
        def prop = filter.filterable?.filterProperty
        if (prop && prop.indexOf('.') > -1) {
            return buildClosure(prop.split('\\.')[0..-2], c)
        } else {
            return c
        }
    }

    //thanks to doig ken
    private Closure createFilterClosure(FilterOperatorsEnum operator, String property, Object value) {
        switch (operator) {
            case EQ: return { eq(property, value) }
            case NE: return { ne(property, value) }
            case LT: return { lt(property, value) }
            case LE: return { le(property, value) }
            case GT: return { gt(property, value) }
            case GE: return { ge(property, value) }
            case BW: return { ilike(property, "${value}%") }
            case BN: return { not { ilike(property, "${value}%") } }
            case IN: return { 'in'(property, value) }
            case NI: return { not { 'in'(property, value) } }
            case EW: return { ilike(property, "%${value}") }
            case EN: return { not { ilike(property, "%${value}") } }
            case CN: return { ilike(property, "%${value}%") }
            case NC: return { not { ilike(property, "%${value}%") } }
            default: log.warn("Operation not supported [${op}]")
        }
    }


    Criteria addOrderBy(Criteria criteria, List orderBy) {
        orderBy.each {
            criteria.order(it.sort, it.order)
        }
        criteria
    }

    // inlineEdit implementations  - only works if domainClass is defined

    /**
     * method called on updating a grid element
     * @param gridConfig
     * return - should return null or an empty string on succes, or a short error message
     */
    @Transactional
    def updateRow(gridConfig) {

        def instance = gridConfig.domainClass.get(params.id)
        if (!instance) {
            return 'default.not.found.message'
        }

        if (params.version) {
            def version = params.version.toLong()
            if (instance.version > version) {
                return 'default.optimistic.locking.failure'
            }
        }

        //default returns params
        instance.properties = gridConfig.beforeSave params
        log.debug "instance = $instance"

        if (!instance.save()) {
            return instance.errors
        }
    }

    /**
     * on saving a new grid element
     * @param gridConfig
     * return - should return null or an empty string on succes, or a short error message
     */
    @Transactional
    def saveRow(gridConfig) {
        def instance = gridConfig.domainClass.newInstance()
        instance.properties = gridConfig.beforeSave(params)
        if (!instance.save(flush: true)) {
            return instance.errors
        }
    }

    /**
     * called on deleting a grid element
     * @param gridConfig
     * return - should return null or an empty string on succes, or a short error message
     */
    @Transactional
    def delRow(gridConfig) {
        def instance = gridConfig.domainClass.get(params.id)

        if (!instance) {
//            Errors errors = new
            return 'default.not.found.message'
        }

        try {
            instance.delete(flush: true)
        }
        catch (DataIntegrityViolationException e) {
            return 'default.not.deleted.message'
        }
    }
}
