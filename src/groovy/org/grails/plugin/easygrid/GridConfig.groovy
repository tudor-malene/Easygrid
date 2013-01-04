package org.grails.plugin.easygrid

import groovy.transform.AutoClone

/**
 * Defines the configuration for a grid
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@AutoClone
class GridConfig {

    String id

    // the columns
//    ColumnsConfig columns = new ColumnsConfig()
    ListMapWrapper<ColumnConfig> columns = new ListMapWrapper<ColumnConfig>('name')

    // datasource settings
    String dataSourceType
    Class dataSourceService  // the datasource implementation

    // UI implementation settings
    String gridImpl
    Class gridImplService    // the UI service implementation
    String gridRenderer      // the UI template renderer

    // export settings
    boolean export          // allow exporting
    String export_title     // the title of the exported file
    Class exportService     // the implementation of the export service

    //security settings
    def roles                   // list of roles, or map of tipe [oper:Role] in case you need to fine grain
    Closure securityProvider    // closure that enforces access control on the grid methods

    // inline edit  settings
    boolean inlineEdit      // allow inline editing
    String editRenderer     // todo?
    Closure beforeSave      // closure used to transform the incoming parameters in an object ready to be persisted

    // formatters - map of type of data & the format closure
    Map<Class, Closure> formats

    // autocomplete settings
    AutocompleteConfig autocomplete = new AutocompleteConfig()

    // other properties which can be used in custom implementations
    private Map dynamicProperties = [:]


    /*************************************************************/
    // todo - add a AST transformation to add dynamicProperties & deepClone

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
