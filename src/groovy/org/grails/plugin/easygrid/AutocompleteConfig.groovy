package org.grails.plugin.easygrid

import groovy.transform.AutoClone

/**
 * defines a grid column
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@AutoClone
class AutocompleteConfig {

    String idProp
    String labelProp

    Closure textBoxFilterClosure
    Closure constraintsFilterClosure

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
