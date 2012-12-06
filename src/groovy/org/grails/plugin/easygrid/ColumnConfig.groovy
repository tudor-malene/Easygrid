package org.grails.plugin.easygrid

import groovy.transform.AutoClone

/**
 * defines a grid column
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@AutoClone
class ColumnConfig {

    // the name of the column
    String name

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
    private Map dynamicProperties = [:]


    def propertyMissing(String name, value) { dynamicProperties[name] = value }
    def propertyMissing(String name) { dynamicProperties[name] }
    def deepClone() {
        def clone = this.clone()
        clone.dynamicProperties = this.dynamicProperties.collectEntries {key, value ->
            [(key): (value instanceof Cloneable) ? value.clone() : value]
        }
        clone
    }
}