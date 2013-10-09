package org.grails.plugin.easygrid

import static org.grails.plugin.easygrid.EasygridContextHolder.*

class FilterFormService {
    static transactional = false

    def filters(GridConfig gridConfig) {
        params.keySet().intersect(gridConfig.filterForm.fields.collect { it.name }).inject([]) { filters, param ->
            def filterForm = gridConfig.filterForm.fields[param]
            if (filterForm?.filterClosure) {
                filters << new Filter(filterForm)
            }
        }
    }
}


//  ) renderer