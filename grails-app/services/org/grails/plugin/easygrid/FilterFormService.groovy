package org.grails.plugin.easygrid

import groovy.util.logging.Slf4j

import static org.grails.plugin.easygrid.EasygridContextHolder.*

/**
 *
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
class FilterFormService {
    static transactional = false
    def filterService

    def filters(GridConfig gridConfig) {
        params.keySet().intersect(gridConfig.filterForm.fields.collect { it.name }).inject([]) { filters, param ->
            def filterField = gridConfig.filterForm.fields[param]
//            if (filterForm?.filterClosure) {
//                filters << new Filter(filterForm)
//            }
            if(filterField){
                filters << filterService.createFilterFromColumn(gridConfig, filterField, null, params[param])
            }
        }
    }
}


//  ) renderer