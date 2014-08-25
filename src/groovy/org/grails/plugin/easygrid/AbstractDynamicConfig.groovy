package org.grails.plugin.easygrid

/**
 * enables a bean to store dynamic properties
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class AbstractDynamicConfig {

    private Map dynamicProperties = [:]
    def propertyMissing(String name, value) {
        dynamicProperties[name] = value
    }

    def propertyMissing(String name) {
        dynamicProperties[name]
    }

}
