package org.grails.plugin.easygrid.builder

import org.grails.plugin.easygrid.Column
import groovy.text.SimpleTemplateEngine

/**
 * builder for the 'columns' section
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */

class ColumnsDelegate {

    //injected
    def columnDelegate
    def grailsApplication

    def columns
    def gridConfig

    def methodMissing(String name, columnPropertiesClosure) {
        def column = new Column(label: name)
        columns.add(column)
        columnDelegate.column = column
        columnPropertiesClosure[0].delegate = columnDelegate
        columnPropertiesClosure[0].resolveStrategy = Closure.DELEGATE_FIRST
        columnPropertiesClosure[0]()
    }

    /**
     * used for very simple declarations of columnns
     * @param name
     */
    def propertyMissing(String name) {
        assert gridConfig.labelPrefix || gridConfig.domainClass
        def prefix = gridConfig.labelPrefix ?: grails.util.GrailsNameUtils.getPropertyNameRepresentation(gridConfig.domainClass)
        assert prefix

        def label = grailsApplication?.config?.easygrid?.defaults?.labelFormat?.make(prefix: prefix, column: name)
        assert label

        def column = new Column(label: label, property: name)
        columns.add(column)
    }


}