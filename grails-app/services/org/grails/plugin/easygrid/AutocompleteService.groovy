package org.grails.plugin.easygrid

import grails.converters.JSON
import groovy.util.logging.Log4j

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
        assert grid.autocomplete.textBoxFilterClosure
        easygridService.guard(grid) {

            //store the grid to threadLocal
            setLocalGridConfig(grid)

//            [rowOffset: params.iDisplayStart as int, maxRows: maxRows, sort: sort, order: order]
            easygridService.dataSourceService.list([rowOffset: 0, maxRows: 10], [grid.autocomplete.textBoxFilterClosure, grid.autocomplete.constraintsFilterClosure ]).collect {
                [
                        label: getLabel(it),
                        id: GridUtils.getNestedPropertyValue(gridConfig.autocomplete.idProp, it)
                ]
            } as JSON
        }
    }

    def label(Grid grid) {
        assert grid.autocomplete.labelProp ||grid.autocomplete.labelValue

        easygridService.guard(grid) {

            //store the grid to threadLocal
            setLocalGridConfig(grid)

            easygridService.dataSourceService.getById(params.id).collect {
                [label: getLabel(it)]
            } as JSON
        }
    }

    def getLabel(element){
        if(gridConfig.autocomplete.labelProp){
            GridUtils.getNestedPropertyValue(gridConfig.autocomplete.labelProp, element)
        }else{
            assert gridConfig.autocomplete.labelValue
            switch (gridConfig.autocomplete.labelValue?.parameterTypes?.size()) {
                case 1:
                    return gridConfig.autocomplete.labelValue.call(element)
                case 2:
                    return gridConfig.autocomplete.labelValue.call(element, params)
            }
        }
    }
}
