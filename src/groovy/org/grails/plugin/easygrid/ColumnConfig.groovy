package org.grails.plugin.easygrid

import groovy.transform.AutoClone
import org.grails.plugin.easygrid.ast.DynamicConfig

/**
 * defines a grid column
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@DynamicConfig
@AutoClone
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


    // filtering of data settings
    Boolean enableFilter   // flag that specifies if filtering should be enabled on this column
//    String filterFieldType  // one of the predefined filters defined for each datasource
//    Closure filterClosure    // a closure called when filtering on a column from the UI ( either specified directly or through the filterFieldType

    Boolean sortable // flag to enable sortable

    Boolean showInSelection // if selection is enabled for the grid - this flag decides if this column will be shown in the dialog
}