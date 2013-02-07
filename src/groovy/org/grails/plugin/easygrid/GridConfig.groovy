package org.grails.plugin.easygrid

import groovy.transform.AutoClone
import org.grails.plugin.easygrid.ast.DynamicConfig

/**
 * Defines the configuration for a grid
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@DynamicConfig
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
    ExportConfig export = new ExportConfig()

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

    // global filter
    Closure globalFilterClosure
}
