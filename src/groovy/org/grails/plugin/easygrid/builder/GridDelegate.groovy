package org.grails.plugin.easygrid.builder

import org.grails.plugin.easygrid.GridUtils

/**
 * builder for the Grid
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class GridDelegate {
    //injected
    def grailsApplication
    def columnsDelegate

    def gridConfig


    void columns(Closure columnsClosure) {
        gridConfig.columns = []
        columnsDelegate.columns = gridConfig.columns
        columnsDelegate.gridConfig = gridConfig
        columnsClosure.delegate = columnsDelegate
        columnsClosure.resolveStrategy = Closure.DELEGATE_FIRST
        columnsClosure()
    }


    def methodMissing(String name, values) {
        if (name in GridUtils.findImplementations(grailsApplication?.config?.easygrid)) {
            gridConfig[name] = [:]
            def closure = values[0]
            closure.delegate = new GridImplDelegate(gridConfig[name])
            closure.resolveStrategy = Closure.DELEGATE_FIRST
            closure()
        } else {
            gridConfig[name] = values[0]
        }
    }
}
