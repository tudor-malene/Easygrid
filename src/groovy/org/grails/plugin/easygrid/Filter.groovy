package org.grails.plugin.easygrid

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap

/**
 * represents a search filter
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class Filter {

    // the column on which the filter was applied
    ColumnConfig column

    // the name of the request parameter
    String paramName

    // the search filter
    Closure searchFilter

    // the actual value of the user input
    String paramValue

    // the parameter map
    GrailsParameterMap params

//    def convertedValue - todo : convert type


    public Filter(ColumnConfig columnConfig) {
        init()
        this.column = columnConfig
        this.searchFilter = columnConfig.filterClosure
        this.paramName = columnConfig.name
        this.paramValue = this.params[this.paramName]
    }

    public Filter(Closure searchFilter, paramValue) {
        init()
        this.searchFilter = searchFilter
        this.paramValue = paramValue
    }

    public Filter(Closure searchFilter) {
        init()
        this.searchFilter = searchFilter
    }


    private init(){
        this.params = EasygridContextHolder.params
    }

}
