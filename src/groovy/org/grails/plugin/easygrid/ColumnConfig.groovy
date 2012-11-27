package org.grails.plugin.easygrid

/**
 * defines a grid column
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class ColumnConfig {

    String label
    def type

    // the value
    def property
    Closure value

    String formatName // one of the predefined formatters
    Closure formatter  //

    Closure filterClosure

    /**
     *  if selection is enabled for the grid - this flag decides if this column will be shown in the dialog
     */
    Boolean showInSelection

    //dynamic
    Map properties = [:]
    def propertyMissing(String name, value) { properties[name] = value }
    def propertyMissing(String name) { properties[name] }

}
