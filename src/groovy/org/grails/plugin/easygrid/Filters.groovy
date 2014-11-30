package org.grails.plugin.easygrid

import static org.grails.plugin.easygrid.FiltersEnum.and

/**
 * the filters structure
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class Filters {

    FiltersEnum type = and
    // can be a collection of Filter and Filters
    List filters = []

    /**
     * goes through the structure in post order and applies the transformation closures
     * @param transformNode
     * @param transformLeaf
     * @return
     */
    def postorder(Closure transformNode, Closure transformLeaf) {
        def siblings = []
        filters.findAll { it instanceof Filters }.each { Filters filters ->
            siblings << filters.postorder(transformNode, transformLeaf)
        }
        filters.findAll { !(it instanceof Filters) }.each { def filter ->
            siblings << transformLeaf(filter)
        }
        transformNode(this, siblings)
    }

    def leftShift(filter) {
        if (filter) {
            if (filter instanceof Collection) {
                filters.addAll(filter.findAll{it})
            } else {
                filters << filter
            }
        }
    }
}


enum FiltersEnum {
    and, or
}