package org.grails.plugin.easygrid

import groovy.transform.AutoClone

/**
 * Defines the grid
 *
 * todo - annotation with ast transformation to add dynamicProperties & deepClone
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@AutoClone
class GridConfig {

    String id

    // the columns
//    List<ColumnConfig> columns = []
    ColumnsConfig columns = new ColumnsConfig()

//    Map autocomplete = [:]
    AutocompleteConfig autocomplete = new AutocompleteConfig()

    // the datasource
    String dataSourceType
    Class dataSourceService  // the datasource

    // the implementation
    String gridImpl
    Class gridImplService
    String gridRenderer

    // export
    boolean export
    String export_title
    Class exportService

    //security
    def roles
    Closure securityProvider

    // inline edit
    boolean inlineEdit
    String editRenderer
    Closure beforeSave

    // formatters for each value ( depend on the class )
    Map<Class, Closure> formats


    private Map dynamicProperties = [:]

    //setter
    def propertyMissing(String name, value) {
        dynamicProperties[name] = value
    }

    //getter
    def propertyMissing(String name) {
        dynamicProperties[name]
    }

    def deepClone() {
        def clone = this.clone()

        //clone the collections
        ['dynamicProperties', 'formats'].each {prop ->
            if (this[prop]) {
                clone[prop] = this[prop].collectEntries {key, value ->
                    [(key): (value instanceof Cloneable) ? value.clone() : value]
                }
            }
        }

        //deep clone the columns container
        clone.columns = this.columns.deepClone()
        clone.autocomplete = this.autocomplete.deepClone()
        clone
    }

}
