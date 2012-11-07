package org.grails.plugin.easygrid

import groovy.util.logging.Log4j
import grails.converters.JSON

/**
 * Autocomplete service
 *
 * manages the interaction between the autocomplete javascript control & the configuration
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Log4j
@Mixin(EasygridContextHolder)
class AutocompleteService {

    def easygridService

    def supportsAutocomplete(Grid grid) {
        return grid.autocomplete
    }

    def response(Grid grid) {
        assert grid.autocomplete.textBoxSearchClosure
        easygridService.guard(grid) {

            //store the grid to threadLocal
            setLocalGridConfig(grid)

//            [rowOffset: params.iDisplayStart as int, maxRows: maxRows, sort: sort, order: order]
            easygridService.dataSourceService.list([rowOffset: 0, maxRows: 10], [grid.autocomplete.textBoxSearchClosure]).collect {
                [
                        value: GridUtils.getNestedPropertyValue(gridConfig.autocomplete.codeProp, it),
                        label: GridUtils.getNestedPropertyValue(gridConfig.autocomplete.labelProp, it),
                        id: GridUtils.getNestedPropertyValue(gridConfig.autocomplete.idProp, it)
                ]
            } as JSON
        }
    }

    def label(Grid grid) {
        assert grid.autocomplete.labelProp

        easygridService.guard(grid) {

            //store the grid to threadLocal
            setLocalGridConfig(grid)

            easygridService.dataSourceService.getById(params.id).collect {
                [
                        label: GridUtils.getNestedPropertyValue(gridConfig.autocomplete.labelProp, it),
                ]
            } as JSON
        }
    }

}

