package org.grails.plugin.easygrid

import grails.converters.JSON
import groovy.util.logging.Slf4j
import groovy.util.logging.Slf4j

/**
 * Autocomplete service
 *
 * manages the interaction between the autocomplete javascript control & the configuration
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
@Mixin(EasygridContextHolder)
class AutocompleteService {

    def easygridService

    /**
     * if the grid supports autocomplete
     * @param grid
     * @return
     */
    def supportsAutocomplete(GridConfig grid) {
        grid.autocomplete
    }

    /**
     * applies the autocomplete search term and the constraints to the data set
     * & returns a list of elements in a friendly JSON format
     * @param grid
     * @return
     */
    def searchedElementsJSON(GridConfig grid) {
        assert grid.autocomplete.textBoxFilterClosure

        easygridService.guard(grid) {

            //store the grid to threadLocal
            setLocalGridConfig(grid)

            def filters = [new Filter(grid.autocomplete.textBoxFilterClosure, params.term)]

            if (grid.autocomplete.constraintsFilterClosure != null) {
                filters << new Filter(grid.autocomplete.constraintsFilterClosure)
            }

            //todo - make maxrows configurable
            easygridService.dataSourceService.list(
                    [rowOffset: 0, maxRows: grid.autocomplete.maxRows], filters).collect {
                [
                        label: getLabel(it),
                        id: GridUtils.getNestedPropertyValue(gridConfig.autocomplete.idProp, it)
                ]
            } as JSON
        }
    }

    /**
     * for a given id , returns the label of that element
     * @param grid
     * @return
     */
    def label(GridConfig grid) {
        assert grid.autocomplete.labelProp || grid.autocomplete.labelValue

        easygridService.guard(grid) {

            //store the grid to threadLocal
            setLocalGridConfig(grid)

            easygridService.dataSourceService.getById(params.id).collect {
                [label: getLabel(it)]
            } as JSON
        }
    }

    /**
     * utility method to generate a label from the configuration
     * @param element
     * @return
     */
    def getLabel(element) {
        if (gridConfig.autocomplete.labelProp) {
            GridUtils.getNestedPropertyValue(gridConfig.autocomplete.labelProp, element)
        } else {
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
