package org.grails.plugin.easygrid.builder

import org.grails.plugin.easygrid.GridUtils

/**
 * builder for the column
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class ColumnDelegate {

    //injected
    def grailsApplication

    def column

    //the value closure
    def value(implClosure) {
        column.value = implClosure
    }

    //the export builder
    def export(implClosure) {
        column.export = [:]
        buildImpl(column.export, implClosure)
    }


    def methodMissing(String columnProperty, columnValue) {
        if (columnProperty in GridUtils.findImplementations(grailsApplication?.config?.easygrid)) {
            column[columnProperty] = [:]
            buildImpl(column[columnProperty], columnValue[0])
        } else {
            column[columnProperty] = columnValue[0]
        }
    }

    /**
     * utility method -
     * @param impl
     * @param implClosure
     */
    private void buildImpl(impl, implClosure) {
        implClosure.delegate = new GridImplDelegate(impl)
        implClosure.resolveStrategy = Closure.DELEGATE_FIRST
        implClosure()
    }
}