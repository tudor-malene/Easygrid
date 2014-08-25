package org.grails.plugin.easygrid

import groovy.transform.Canonical

/**
 * configurations for the selection - autocomplete widget
 *
 * this widget is meant to replace select Boxes
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Canonical
class AutocompleteConfig extends AbstractDynamicConfig{

    String idProp     // the name of the property of the id of the selected element (optionKey - in the replaced select tag)

    String labelProp   // the equivalent of "optionValue"
    Closure labelValue  // in case the label is more complex

    Closure constraintsFilterClosure  // filter closure called each time values are displayed ( in the select pop-up or autocomplete widgete ). Will receive the constraints from the page
    Closure textBoxFilterClosure   // the filter closure called each time the user types in the autocomplete widget. Will receive the user input in: params.term

    Integer maxRows  //maximum rows to be shown in the jquery autocomplete widget

    Boolean showSeparateLabel // flag that indicates if the selection widget should be rendered with a seperate label

    Class autocompleteService // the service

    AutocompleteConfig() {
    }
}
