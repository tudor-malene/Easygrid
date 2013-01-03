package org.grails.plugin.easygrid

/**
 * represents a search filter
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class Filter {

    boolean autocompleteConstraint = false
    boolean autocompleteTerm = false

    String paramName
    def searchFilter

    ColumnConfig column
    String paramValue
    Object convertedValue

    /**
     * fast initializer used for testing
     * @param column
     */
    static Filter initFromColumn(ColumnConfig column) {
        Filter filter = new Filter()
        filter.column = column
        filter.searchFilter = column.filterClosure
        filter.paramName = column.name
        filter.paramValue = EasygridContextHolder.params[this.paramName]
        filter
    }

    /**
     *
     */
    def convertValue() {

        //todo
        column.filterFieldType

        //todo
        convertedValue = paramValue
    }

}
