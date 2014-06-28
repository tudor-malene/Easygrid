package org.grails.plugin.easygrid.datasource

import groovy.util.logging.Slf4j
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.grails.plugin.easygrid.*
import org.hibernate.Criteria
import org.hibernate.criterion.Distinct
import org.hibernate.criterion.ProjectionList
import org.hibernate.criterion.PropertyProjection
import org.springframework.dao.DataIntegrityViolationException

import org.springframework.transaction.annotation.Transactional

import static org.codehaus.groovy.grails.commons.GrailsClassUtils.getStaticPropertyValue
import static org.grails.plugin.easygrid.EasygridContextHolder.getParams
import static org.grails.plugin.easygrid.FilterUtils.getOperatorMapKey
import static org.grails.plugin.easygrid.GormUtils.createFilterClosure
import static org.grails.plugin.easygrid.GridUtils.*

/**
 * Datasource implementation for a GORM Domain class
 * Works only with Hibernate Datasource
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
@Transactional
class GormDatasourceService {

    def grailsApplication
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
            if (!column.filterProperty) {
                column.filterProperty = column.property
            }
        }

        (gridConfig.columns.elementList + gridConfig.filterForm?.fields?.elementList).findAll {
            it?.enableFilter
        }.each { FilterableConfig filterable ->
            def property = filterable.filterProperty
            if (property) {
                if (!filterable.filterDataType) {
                    GrailsDomainClassProperty columnProperty = getDomainProperty(grailsApplication, gridConfig.domainClass, property)
                    Class columnPropertyType = columnProperty?.type
                    filterable.filterDataType = columnPropertyType
                    if (!columnPropertyType) {
                        log.warn("Property '${property}' for grid: ${gridConfig.id} does not exist in domain class ${gridConfig.domainClass}")
                    }
                }
                if (!filterable.filterType) {
                    filterable.filterType = getOperatorMapKey(filterable.filterDataType)
                }

//                if (columnProperty.association) {
//                    def assoc = columnProperty.referencedDomainClass
//                    println assoc
//                }
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

//        Collections.sort(properties, new DomainClassPropertyComparator(domainClass))
        //implement the comparator locally because it moved
        properties.sort { o1, o2 ->
            if (o1.equals(domainClass.getIdentifier())) {
                return -1;
            }
            if (o2.equals(domainClass.getIdentifier())) {
                return 1;
            }

            GrailsDomainClassProperty prop1 = (GrailsDomainClassProperty) o1;
            GrailsDomainClassProperty prop2 = (GrailsDomainClassProperty) o2;

            ConstrainedProperty cp1 = (ConstrainedProperty) domainClass.constrainedProperties.get(prop1.getName());
            ConstrainedProperty cp2 = (ConstrainedProperty) domainClass.constrainedProperties.get(prop2.getName());

            if (cp1 == null & cp2 == null) {
                return prop1.getName().compareTo(prop2.getName());
            }

            if (cp1 == null) {
                return 1;
            }

            if (cp2 == null) {
                return -1;
            }

            if (cp1.getOrder() > cp2.getOrder()) {
                return 1;
            }

            if (cp1.getOrder() < cp2.getOrder()) {
                return -1;
            }

            return 0;
        }
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

//        def result = addOrderBy(gridConfig, createWhereQuery(gridConfig, filters), orderBy).list(max: listParams.maxRows, offset: listParams.rowOffset)
        def criteria = createWhereQuery(gridConfig, filters, orderBy, false)
        if (listParams.maxRows) {
            criteria.maxResults = listParams.maxRows
        }
        if (listParams.rowOffset) {
            criteria.firstResult = listParams.rowOffset
        }
        def result = criteria.list()

        if (gridConfig.transformData) {
            result.collect(gridConfig.transformData)
        } else {
            result
        }
    }

    /**
     * returns an element by id
     * @param id
     */
    def getById(GridConfig gridConfig, id) {
        String idProp = gridConfig.autocomplete.idProp
        if (id != null) {
//            createWhereQuery(gridConfig, filterService.createGlobalFilters { eq(idProp, id) }).find()
            gridConfig.domainClass."findBy${idProp.capitalize()}"(id)
        }
    }

    /**
     * returns the total no of rows
     * @param filters -
     * @return
     */
    def countRows(gridConfig, filters = null) {
        Criteria c = createWhereQuery(gridConfig, filters, null, true)
        c.uniqueResult()
    }

    /**
     * combines all the filters into a gorm where query
     * @param filters - map of filter closures
     * @return
     */
    def createWhereQuery(GridConfig gridConfig, Filters filters, List orderBy = [], boolean countRows = false) {
//        DetachedCriteria baseCriteria = new DetachedCriteria(gridConfig.domainClass)
        def baseCriteria = gridConfig.domainClass.createCriteria()
        baseCriteria.buildCriteria {
            if (gridConfig.initialCriteria) {
//            baseCriteria = baseCriteria.build(gridConfig.initialCriteria)
                def initialCriteria = gridConfig.initialCriteria.clone()
                initialCriteria.resolveStrategy = Closure.DELEGATE_FIRST
                initialCriteria.delegate = delegate
                initialCriteria()
            }

            Closure filterCriteria = createFiltersClosure(filters)
            if (filterCriteria) {
                filterCriteria.resolveStrategy = Closure.DELEGATE_FIRST
                filterCriteria.delegate = delegate
                filterCriteria()
            }
            if (countRows) {
                ProjectionList projectionList = delegate.criteria.projection
                projections {
                    Distinct distinct = projectionList?.elements?.find { it instanceof Distinct }
                    if (gridConfig.countDistinct || distinct) {
                        def propertyName = gridConfig.countDistinct
                        if (propertyName) {
                            def c = { countDistinct(lastProperty(propertyName)) }
                            if (propertyName.indexOf('.') > -1) {
                                c = buildClosure(propertyName.split('\\.')[0..-2], c)
                            }
                            c.delegate = delegate
                            c()
                        } else {
                            //try to determine the count distinct property from the criteria
                            def proj
                            if (distinct.hasProperty('wrappedProjection')) {
                                //hibernate 4
                                proj = distinct.wrappedProjection
                            } else {
                                //hibernate 3
                                proj = distinct.@projection
                            }

                            if (proj && proj instanceof PropertyProjection) {
                                propertyName = proj.propertyName
                            }
                            if (propertyName) {
                                countDistinct(propertyName)
                            } else {
                                log.warn("Row count for grid: ${gridConfig.id} might not be correct")
                                //fallback to rowCount
                                rowCount()
                            }
                        }

                    } else {
                        rowCount()
                    }
                }
            } else {
                orderBy.each {
                    def sortCol = gridConfig.columns[it.sort]
                    assert sortCol
                    def sort = valueOfSortColumn(gridConfig, sortCol)
                    if (sort instanceof Closure) {
                        //execute the closure
                        sort.delegate = delegate
                        sort.call(it.order)
                    } else {
                        def c = getOrderClosure(sort, it.order)
                        c.delegate = delegate
                        c()
                    }
                }
            }
        }
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
                        if (siblings.findAll { it }) {
                            return {
                                "${node.type}" {
                                    def del = delegate
                                    siblings.findAll { it }.each { Closure criteria ->
                                        criteria.delegate = del
                                        criteria.resolveStrategy = Closure.DELEGATE_FIRST
                                        criteria()
                                    }
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
        assert filter
        if (filter.searchFilter) {
            return filter.searchFilter
        }

        //create a dynamic closure
        def prop = filter.filterable?.filterProperty
        def c = createFilterClosure(filter.operator, lastProperty(prop), filter.value)
        if (prop && prop.indexOf('.') > -1) {
            return buildClosure(prop.split('\\.')[0..-2], c)
        } else {
            return c
        }
    }

    def getOrderClosure(sort, orderDir) {
        def c = { order(lastProperty(sort), orderDir) }
        if (sort.indexOf('.') > -1) {
            return buildClosure(sort.split('\\.')[0..-2], c)
        } else {
            return c
        }
    }

    def addOrderBy(GridConfig gridConfig, criteria, List orderBy) {
        orderBy.each {
            def sortCol = gridConfig.columns[it.sort]
            assert sortCol
            def sort = valueOfSortColumn(gridConfig, sortCol)
            if (sort instanceof Closure) {
                //execute the closure
                sort.delegate = criteria
                sort.call(it.order)
            } else {
                criteria.order(sort, it.order)
            }
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
    def updateRow(gridConfig, InlineResponse response) {

        def instance = gridConfig.domainClass.get(params.id)
        if (!instance) {
            response.message = 'default.not.found.message'
            return
        }

        if (params.version) {
            def version = params.version.toLong()
            if (instance.version > version) {
                response.message = 'default.optimistic.locking.failure'
                return
            }
        }

        //default returns params
        instance.properties = gridConfig.beforeSave params
        log.debug "instance = $instance"

        instance.save()
        response.instance = instance
    }

    /**
     * on saving a new grid element
     * @param gridConfig
     * return - should return null or an empty string on succes, or a short error message
     */
    @Transactional
    def saveRow(gridConfig, InlineResponse response) {
        def instance = gridConfig.domainClass.newInstance()
        instance.properties = gridConfig.beforeSave(params)
        instance.save()
        response.instance = instance
    }

    /**
     * called on deleting a grid element
     * @param gridConfig
     * return - should return null or an empty string on succes, or a short error message
     */
    @Transactional
    def delRow(gridConfig, InlineResponse response) {
        def instance = gridConfig.domainClass.get(params.id)

        if (!instance) {
            response.message = 'default.not.found.message'
            return
        }

        try {
            instance.delete(flush: true)
        }
        catch (DataIntegrityViolationException e) {
            response.message = 'default.not.deleted.message'
        }
    }
}
