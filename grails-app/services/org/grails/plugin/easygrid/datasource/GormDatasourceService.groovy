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
import org.springframework.dao.DataIntegrityViolationException

/**
 * Datasource implementation for a GORM Domain class
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
@Mixin(EasygridContextHolder)
class GormDatasourceService {

    def grailsApplication
    def easygridService

    /**
     * if no columns specified in the builder - then generate the columns from the properties of the domain class
     * - useful during prototyping
     * @param gridConfig
     */
    def generateDynamicColumns() {

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
                if (easygridService.implService?.respondsTo('dynamicProperties')) {
                    easygridService.implService.dynamicProperties(idProperty, idCol)
                }
                gridConfig.columns.add(idCol)
            }

            domainClass.properties.findAll { !(it.name in ['id', 'version']) }.sort { a, b -> a.name <=> b.name }.each { GrailsDomainClassProperty prop ->
                if (prop.isAssociation()) {
                    return
                }
                def column = new ColumnConfig(property: prop.name, name: prop.name)
                //todo -add other info from the property
                if (easygridService.implService?.respondsTo('dynamicProperties')) {
                    easygridService.implService.dynamicProperties(prop, column)
                }
                gridConfig.columns.add column
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
    def list(Map listParams = [:], filters = null) {

        listParams.with {
//            createCriteria(filters).list(max: maxRows, offset: rowOffset, sort: sort, order: order)
            createWhereQuery(filters).list(max: maxRows, offset: rowOffset, sort: sort, order: order)
        }
    }

    /**
     * returns an element by id
     * @param id
     */
    def getById(id) {
        if (id != null) {
            createWhereQuery([new Filter({ filter -> eq('id', id as long) })]).find()
        }
    }

    /**
     * returns the total no of rows
     * @param filters - it will be a criteria
     * @return
     */
    def countRows(filters = null) {
//        createCriteria(filters).count()
        createWhereQuery(filters).count()
    }

    /**
     * combines all the filters into a gorm where query
     * @param filters - map of filter closures
     * @return
     */
    DetachedCriteria createWhereQuery(filters) {

        def initial = new DetachedCriteria(gridConfig.domainClass)
        initial = gridConfig.initialCriteria ? initial.build(gridConfig.initialCriteria) : initial.build {}
        filters.inject(initial) { DetachedCriteria criteria, Filter filter ->
            if (getCriteria(filter) instanceof Closure) {
                criteria.and(getCriteria(filter))
            } else {
                getCriteria(filter).criteria.criteria.each { Query.Criterion criterion -> criteria.add(criterion) }
                criteria
            }
        }
    }

    /**
     * combines all the filters into a gorm criteria
     * @param filters - list of closures
     * @return
     */
    @Deprecated
    Criteria createCriteria(filters) {
        def initial = new DetachedCriteria(gridConfig.domainClass)
        initial = gridConfig.initialCriteria ? initial.and(gridConfig.initialCriteria) : initial
        filters.inject(initial) { criteria, searchCriteria ->
            criteria.and(searchCriteria.curry(params))
        }
    }

    def getCriteria(Filter filter) {
        assert filter.searchFilter instanceof Closure
        assert filter.searchFilter.parameterTypes.size() == 1

        filter.searchFilter.curry(filter)


//        if (curriedClosure.parameterTypes.size() == 1 && curriedClosure.parameterTypes[0] == Filter) {
//            if (curriedClosure.parameterTypes[0] == Filter) {
//            return curriedClosure.curry(filter)
//        }
/*
        if (curriedClosure.parameterTypes.size() in [0, 1]) {
            //todo - sa fac si conversia ( de ce pasez paramValue - daca e deja in filtru ?? )
            curriedClosure = curriedClosure.curry(filter.paramValue)
        } else {
            curriedClosure = curriedClosure.curry(filter.paramValue, EasygridContextHolder.params)
        }
*/

//        return curriedClosure
    }

    // inlineEdit implementations  - only works if domainClass is defined

    /**
     * default closure called on updating a grid element
     * @param gridConfig
     * @param params
     * @param session
     */
    def updateRow = {

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

        if (!instance.save(flush: true)) {
            return instance.errors
        }
    }

    /**
     * default closure  called on saving a new grid element
     * @param gridConfig
     * @param params
     * @param session
     */
    def saveRow = {
        def instance = gridConfig.domainClass.newInstance()
        instance.properties = gridConfig.beforeSave(params)
        if (!instance.save(flush: true)) {
            return instance.errors
        }
    }

    /**
     * default closure called on deleting a grid element
     * @param gridConfig
     * @param params
     * @param session
     */
    def delRow = {
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
