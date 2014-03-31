package org.grails.plugin.easygrid

import grails.converters.JSON
import groovy.util.logging.Slf4j
import groovy.util.logging.Slf4j
import static org.grails.plugin.easygrid.EasygridContextHolder.*

/**
 * Autocomplete service
 *
 * manages the interaction between the autocomplete javascript control & the configuration
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
class AutocompleteService {

    def easygridDispatchService
    def filterService

    def filters(gridConfig) {
        // add the selection component constraint filter closure
        if (params.selectionComp && gridConfig.autocomplete.constraintsFilterClosure) {
            //add a new criteria
//            [new Filter(gridConfig.autocomplete.constraintsFilterClosure)]
            filterService.createGlobalFilters gridConfig.autocomplete.constraintsFilterClosure
        }
    }

    /**
     * applies the autocomplete search term and the constraints to the data set
     * & returns a list of elements in a friendly JSON format
     * @param grid
     * @return
     */
    def searchedElementsJSON(GridConfig gridConfig) {
        assert gridConfig.autocomplete.textBoxFilterClosure

        // compose an array with the input term filter and an eventual contraint
//        def filters = [new Filter(gridConfig.autocomplete.textBoxFilterClosure, params.term)]
        def filters = filterService.createGlobalFilters(gridConfig.autocomplete.textBoxFilterClosure)

        if (gridConfig.autocomplete.constraintsFilterClosure != null) {
//            filters << new Filter(gridConfig.autocomplete.constraintsFilterClosure)
            filters << filterService.createGlobalFilters(gridConfig.autocomplete.constraintsFilterClosure)
        }

        //retreive the rows & transform the values to the JSON format
        //todo - what about order?
//        gridConfig.callGridPropertyMethod(
//                'dataSourceService', 'list', [rowOffset: 0, maxRows: gridConfig.autocomplete.maxRows], filters
//        )
        easygridDispatchService.callDSList(gridConfig, [rowOffset: 0, maxRows: gridConfig.autocomplete.maxRows], filters)
                .collect {
            [
                    id   : GridUtils.getNestedPropertyValue(gridConfig.autocomplete.idProp, it),
                    label: getLabel(gridConfig, it),
            ]
        } as JSON
    }

    /**
     * for a given id , returns the label of that element
     * @param grid
     * @return
     */
    def label(GridConfig gridConfig) {
        assert gridConfig.autocomplete.labelProp || gridConfig.autocomplete.labelValue
        //retreive the selected element and provide the label in a convenient format
//        gridConfig.callGridPropertyMethod('dataSourceService', 'getById', params.id).
        easygridDispatchService.callDSGetById(gridConfig, params.id).
                collect {
                    [label: getLabel(gridConfig, it)]
                } as JSON
    }

    /**
     * utility method to generate a label from the configuration
     * fetches the label property from the config and evaluates it against the element
     * @param element - the selected element
     * @return
     */
    private getLabel(gridConfig, element) {
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
