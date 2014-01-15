package org.grails.plugin.easygrid

import groovy.transform.AutoClone
import groovy.transform.Canonical
import org.grails.plugin.easygrid.ast.DynamicConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Defines the configuration for a grid
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@DynamicConfig
@AutoClone
@Canonical
class GridConfig {

    String id

    // the columns
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
    Boolean inlineEdit      // allow inline editing
    Closure beforeSave      // closure used to transform the incoming parameters in an object ready to be persisted

    //closures called on inline editing - should return null on success or the errors
    Closure saveRowClosure      // closure used for saving a row
    Closure updateRowClosure
    Closure delRowClosure

    // formatters - map of type of data & the format closure
    Map<Class, Closure> formats

    // autocomplete settings
    AutocompleteConfig autocomplete

    // global filter
    Closure globalFilterClosure

    // the filter form
    FilterFormConfig filterForm


    // lifecycle closures- called during the initialization phase - will receive the gridConfig
    // useful for applying different rules

    // will be called before applying the default values - must be set in the builder
//    Closure beforeApplyingGridDefaults
    // useful for adding new columns, etc
    Closure beforeApplyingColumnRules
    Closure afterInitialization


    @Override
    public String toString() {
        return "GridConfig{${id}}"
    }
}
