package org.grails.plugin.easygrid

import groovy.util.logging.Slf4j

import static org.grails.plugin.easygrid.GridUtils.convertValueUsingBinding

/**
 * responsible for the creation of filters
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
class FilterService {
    static transactional = false
    def grailsApplication

    /**
     *
     * @param gridConfig
     * @param filterableConfig
     * @return
     */
    Filter createFilterFromColumn(GridConfig gridConfig, FilterableConfig filterableConfig,
                                  def operator,  value) {

        assert filterableConfig.filterDataType

        //if a filterConverter is defined use it , otherwise use the standard binding
        def val = filterableConfig.filterConverter ? filterableConfig.filterConverter(value) : convertValueUsingBinding(value, filterableConfig.filterDataType)

        def f = new Filter()

        f.filterable = filterableConfig
        f.paramName = filterableConfig.name
        f.paramValue = value
        f.value = (val==null) ? Filter.FAILED_CONVERSION : val
        f.operator = (operator ?: filterableConfig.defaultFilterOperator) ?: grailsApplication.config.easygrid.defaults.filterType[filterableConfig.filterType]?.defaultOperator
        log.warn("operator is null for filter: ${filterableConfig.name}")

        if (filterableConfig.filterClosure) {
            f.searchFilter = filterableConfig.filterClosure.curry(f)
        }
        f
    }


    Filter createGlobalFilter(Closure c) {
        def f = new Filter()
        f.global = true
        f.searchFilter = c.curry(EasygridContextHolder.params)
        f
    }

    Filters createGlobalFilters(Closure c) {
        new Filters(filters: [createGlobalFilter(c)])
    }

}