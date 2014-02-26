package org.grails.plugin.easygrid.datasource

import grails.gorm.DetachedCriteria
import groovy.util.logging.Slf4j
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.scaffolding.DomainClassPropertyComparator
import org.grails.datastore.mapping.query.Query
import org.grails.plugin.easygrid.ColumnConfig
import org.grails.plugin.easygrid.Filter
import org.grails.plugin.easygrid.GridConfig
import org.grails.plugin.easygrid.GridUtils
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional

import static org.codehaus.groovy.grails.commons.GrailsClassUtils.getStaticPropertyValue
import static org.grails.plugin.easygrid.EasygridContextHolder.getParams

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

    /**
     * if no columns specified in the gridConfig - then generate the columns from the properties of the domain class
     * - useful during prototyping
     * @param gridConfig
     */
    def generateDynamicColumns(gridConfig) {

        // only generate if there are no columns defined
        if (!gridConfig.columns && gridConfig.domainClass) {
            GrailsDomainClass domainClass = GridUtils.resolveDomainClass grailsApplication, gridConfig.domainClass

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
            if (column.enableFilter && column.filterClosure == null) {

                if (column.filterFieldType == null) {
                    if (gridConfig.domainClass) {
//                        assert column.property: "you must specify a filterFieldType for ${column.name}"
                        if (column.property) {
                            Class columnPropertyType = GridUtils.getPropertyType(grailsApplication, gridConfig.domainClass, column.property)
                            if (!columnPropertyType) {
                                log.warn("Property '${column.property}' for grid: ${gridConfig.id} does not exist in domain class ${gridConfig.domainClass}")
                            }
                            switch (columnPropertyType) {
                                case String:
                                    column.filterFieldType = 'text'
                                    break
                                case int:
                                case Integer:
                                case BigDecimal:
                                    column.filterFieldType = 'number'
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

                if (column.filterFieldType) {
                    def filterClosure = defaultValues?.dataSourceImplementations?."${gridConfig.dataSourceType}"?.filters?."${column.filterFieldType}"
                    assert filterClosure: "no default filterClosure defined for '${column.filterFieldType}'"
                    column.filterClosure = filterClosure
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
            createWhereQuery(gridConfig, [new Filter({ filter -> eq(idProp, id) })]).find()
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
    DetachedCriteria createWhereQuery(gridConfig, filters) {
        def initial = new DetachedCriteria(gridConfig.domainClass)
        DetachedCriteria result = filters.inject(gridConfig.initialCriteria ? initial.build(gridConfig.initialCriteria) : initial) { DetachedCriteria criteria, Filter filter ->
            def filterCriteria = getCriteria(filter);
            if (filterCriteria instanceof Closure) {
                criteria.and(filterCriteria)
            } else {
                //todo                   filterCriteria
                filterCriteria.criteria.criteria.each { Query.Criterion criterion -> criteria.add(criterion) }
                criteria
            }
        }


        // add the filterpane stuff -if supported
/*
        if (filterPaneService) {
            filterPaneService.addFiltersToCriteria(result, params, gridConfig.domainClass)
        }
*/
        result
    }


    def getCriteria(Filter filter) {
        assert filter.searchFilter instanceof Closure
        assert filter.searchFilter.parameterTypes.size() == 1

        //if a global filter , then pass the params
        filter.searchFilter.curry(filter.global ? params : filter)
    }

    DetachedCriteria addOrderBy(DetachedCriteria criteria, List orderBy) {
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
