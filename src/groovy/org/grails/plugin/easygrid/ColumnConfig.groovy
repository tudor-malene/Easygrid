package org.grails.plugin.easygrid

import groovy.transform.Canonical

/**
 * defines a grid column
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Canonical
class ColumnConfig extends FilterableConfig{

//    String name   // the name of the column - used for accessing the column in th grid or for other configurations ( like property or label)

    String label   // the label that represents the header of the column

    String type   // represents one of the predefined column types ( sets of configurations  )

    // the value - how to determine the value for each cell
    String property    // if the elements are maps or domain classes -this represents the property
    Closure value      // if the value is more complex ( like a sum , or etc ) then this closure can be used

    // formatting of the value - for the display
    String formatName   // when you want to use one of the predefined formatters
    Closure formatter   // a custom closure

    Boolean sortable // flag to enable sortable
    String sortProperty
    Closure sortClosure

    Boolean showInSelection // if selection is enabled for the grid - this flag decides if this column will be shown in the dialog

//    Map view //will contain the view properties

    ColumnConfig() {
    }
}