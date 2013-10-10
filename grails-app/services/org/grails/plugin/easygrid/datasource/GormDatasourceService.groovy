package org.grails.plugin.easygrid.datasource

import grails.gorm.DetachedCriteria
import groovy.util.logging.Slf4j
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.query.api.Criteria
import org.grails.plugin.easygrid.ColumnConfig
import org.grails.plugin.easygrid.EasygridContextHolder
import org.grails.plugin.easygrid.Filter
import org.grails.plugin.easygrid.GridConfig
import org.grails.plugin.easygrid.GridUtils
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional

import static org.grails.plugin.easygrid.EasygridContextHolder.*

/**
 * Datasource implementation for a GORM Domain class
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
@Transactional
class GormDatasourceService {

    def grailsApplication

    /**
     * if no columns specified in the builder - then generate the columns from the properties of the domain class
     * - useful during prototyping
     * @param gridConfig
     */
    def generateDynamicColumns(gridConfig) {

        // only generate if there are no columns defined
        if (!gridConfig.columns && gridConfig.domainClass) {
            assert gridConfig.domainClass != null
            //dynamically generate the columns

            //wtf?? - without .name - doesn't work for reloading domain classes
            GrailsDomainClass domainClass = grailsApplication.domainClasses.find { it.clazz.name == gridConfig.domainClass.name }

            assert domainClass != null

            /*           <%  excludedProps = Event.allEvents.toList() << 'id' << 'version'
                        allowedNames = domainClass.persistentProperties*.name << 'dateCreated' << 'lastUpdated'
                        props = domainClass.properties.findAll { allowedNames.contains(it.name) && !excludedProps.contains(it.name) && it.type != null && !Collection.isAssignableFrom(it.type) }
                        Collections.sort(props, comparator.constructors[0].newInstance([domainClass] as Object[]))
                        props.eachWithIndex { p, i ->
                            if (i < 6) {
                                if (p.isAssociation()) { %>
                                    <th><g:message code="${domainClass.propertyName}.${p.name}.label" default="${p.naturalName}" /></th>
                                <%      } else { %>
                                    <g:sortableColumn property="${p.name}" title="\${message(code: '${domainClass.propertyName}.${p.name}.label', default: '${p.naturalName}')}" />
                                    <%  }   }   } %>
                        </tr>
                            </thead>
                        <tbody>
                        <g:each in="\${${propertyName}List}" status="i" var="${propertyName}">
                        <tr class="\${(i % 2) == 0 ? 'even' : 'odd'}">
                        <%  props.eachWithIndex { p, i ->
                            if (i == 0) { %>
                                <td><g:link action="show" id="\${${propertyName}.id}">\${fieldValue(bean: ${propertyName}, field: "${p.name}")}</g:link></td>
                                <%      } else if (i < 6) {
                                if (p.type == Boolean || p.type == boolean) { %>
                                    <td><g:formatBoolean boolean="\${${propertyName}.${p.name}}" /></td>
                                <%          } else if (p.type == Date || p.type == java.sql.Date || p.type == java.sql.Time || p.type == Calendar) { %>
                                    <td><g:formatDate date="\${${propertyName}.${p.name}}" /></td>
                                <%          } else { %>
                                    <td>\${fieldValue(bean: ${propertyName}, field: "${p.name}")}</td>
                                    <%  }   }   } %>
                        </tr>
                            </g:each>
            */
//            todo - choose columns to exclude
            def idProperty = domainClass.properties.find { it.name == 'id' }
            if (idProperty) {
                def idCol = new ColumnConfig(property: 'id', type: 'id', name: 'id')
                idCol.valueType = idProperty.type
                gridConfig.columns.add(idCol)
            }

            domainClass.properties.findAll { !(it.name in ['id', 'version']) }.sort { a, b -> a.name <=> b.name }.each { GrailsDomainClassProperty prop ->
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


    def addDefaultValues(gridConfig, Map defaultValues) {

        gridConfig.columns.each { ColumnConfig column ->
            // add default filterClosure
            if (column.enableFilter && column.filterClosure == null) {

                if (column.filterFieldType == null) {
                    if (gridConfig.domainClass) {
//                        assert column.property: "you must specify a filterFieldType for ${column.name}"
                        if (column.property) {
                            Class columnPropertyType = GridUtils.getPropertyType(grailsApplication, gridConfig.domainClass, column.property)
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
        createWhereQuery(gridConfig, filters).list(max: listParams.maxRows, offset: listParams.rowOffset, sort: listParams.sort, order: listParams.order)
    }

    /**
     * returns an element by id
     * @param id
     */
    def getById(gridConfig, id) {
        //todo - idProp
        if (id != null) {
            createWhereQuery(gridConfig, [new Filter({ filter -> eq('id', id as long) })]).find()
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
        filters.inject(gridConfig.initialCriteria ? initial.build(gridConfig.initialCriteria) : initial) { DetachedCriteria criteria, Filter filter ->
            def filterCriteria = getCriteria(filter);
            if (filterCriteria instanceof Closure) {
                criteria.and(filterCriteria)
            } else {
                //todo                   filterCriteria
                filterCriteria.criteria.criteria.each { Query.Criterion criterion -> criteria.add(criterion) }
                criteria
            }
        }
    }

    def getCriteria(Filter filter) {
        assert filter.searchFilter instanceof Closure
        assert filter.searchFilter.parameterTypes.size() == 1

        //if a global filter , then pass the params
        filter.searchFilter.curry(filter.global ? params : filter)
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

        if (!instance.save(flush: true)) {
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
