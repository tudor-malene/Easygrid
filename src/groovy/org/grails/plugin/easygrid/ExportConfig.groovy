package org.grails.plugin.easygrid

import groovy.transform.AutoClone

/**
 * configurations for the export section of the grid
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@AutoClone
class ExportConfig {

    boolean export          // allow exporting
    String export_title     // the title of the exported file
    Class exportService     // the implementation of the export service


    /***********************************************************/
    //dynamic
    private Map dynamicProperties = [:]

    def toMap(){
        this.properties.findAll {k,v -> k != 'dynamicProperties'} + dynamicProperties
    }

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
