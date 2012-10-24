package org.grails.plugin.easygrid.datasource

import groovy.util.logging.Log4j
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.grails.plugin.easygrid.EasygridContextHolder
import grails.gorm.DetachedCriteria
import org.springframework.dao.DataIntegrityViolationException
import org.grails.plugin.easygrid.Column
import org.grails.datastore.mapping.query.api.Criteria

/**
 * Datasource implementation for a GORM Domain class
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Log4j
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
            gridConfig.columns = []
            //dynamically generate the columns

            //wtf?? - without .name - doesn't work for reloading domain classes
            GrailsDomainClass domainClass = grailsApplication.domainClasses.find {it.clazz.name == gridConfig.domainClass.name}

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
            def idProperty = domainClass.properties.find {it.name == 'id'}
            if (idProperty) {
                def idCol = new Column(property: 'id', type: 'id', label: "${domainClass.propertyName}.id.label")
                if (easygridService.implService?.respondsTo('dynamicProperties')) {
                    easygridService.implService.dynamicProperties(idProperty, idCol)
                }
                gridConfig.columns.add(idCol)
            }

            domainClass.properties.findAll {!(it.name in ['id', 'version'])}.sort {a, b -> a.name <=> b.name}
                    .each { GrailsDomainClassProperty prop ->
                if(prop.isAssociation()){
                    return
                }
                def column = new Column()
                column.property = prop.name
                column.label = "${domainClass.propertyName}.${prop.name}.label"
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
            createCriteria(filters).list(max: maxRows, offset: rowOffset, sort: sort, order: order)
        }

    }

    /**
     * returns the total no of rows
     * @param filters - it will be a criteria
     * @return
     */
    def countRows(filters = null) {
        createCriteria(filters).count()
    }

    def Criteria createCriteria(filters) {
        def initial = new DetachedCriteria(gridConfig.domainClass)
        initial = gridConfig.initialCriteria ? initial.and(gridConfig.initialCriteria) : initial
        filters.inject(initial) {criteria, searchCriteria ->
            criteria.and(searchCriteria.curry(params))
        }
    }

    // inlineEdit implementations  - only works if domainClass is defined

    /**
     * default method called on updating a grid element
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
     * default method  called on saving a new grid element
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
     * default method  called on deleting a grid element
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
