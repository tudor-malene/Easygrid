package org.grails.plugin.easygrid.builder

import org.grails.plugin.easygrid.Grid

/**
 * builder for the "grids" field
 * each closure represents a grid
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class GridsDelegate {

    //injected
    def gridDelegate

    // map that will hold all the grids to be exposed from this controller
    def grids


    def methodMissing(String name, gridClosure) {
//        grids[name] = [:]
        grids[name] = new Grid()
        gridDelegate.gridConfig = grids[name]
        gridClosure[0].delegate = gridDelegate
        gridClosure[0].resolveStrategy = Closure.DELEGATE_FIRST
        gridClosure[0]()
    }
}
