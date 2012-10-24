package org.grails.plugin.easygrid

/**
 * represents a grid column
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class Column {

    String label
    def type

    // the value
    def property
    Closure value

    String formatName // one of the predefined formatters
    Closure formatter  //

    //dynamic
    Map properties = [:]
    def propertyMissing(String name, value) { properties[name] = value }
    def propertyMissing(String name) { properties[name] }

}
