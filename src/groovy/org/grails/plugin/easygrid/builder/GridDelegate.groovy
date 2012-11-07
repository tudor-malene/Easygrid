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
    def autocompleteDelegate

    def gridConfig

    /**
     * the columns section
     * @param columnsClosure
     */
    void columns(Closure columnsClosure) {
        gridConfig.columns = []
        columnsDelegate.columns = gridConfig.columns
        columnsDelegate.gridConfig = gridConfig
        columnsClosure.delegate = columnsDelegate
        columnsClosure.resolveStrategy = Closure.DELEGATE_FIRST
        columnsClosure()
    }

    /**
     * the autocomplete section
     * @param autocompleteClosure
     */
    void autocomplete(Closure autocompleteClosure) {
        gridConfig.autocomplete = [:]
        autocompleteDelegate.autocomplete = gridConfig.autocomplete
        autocompleteDelegate.gridConfig = gridConfig
        autocompleteClosure.delegate = autocompleteDelegate
        autocompleteClosure.resolveStrategy = Closure.DELEGATE_FIRST
        autocompleteClosure()
    }


    /**
     * the properties of the grid
     * @param name
     * @param values
     * @return
     */
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
