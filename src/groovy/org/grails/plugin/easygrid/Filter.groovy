package org.grails.plugin.easygrid

/**
 * represents a search filter
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class Filter {

    public static final Object FAILED_CONVERSION = new Object()

    // the column on which the filter was applied
    FilterableConfig filterable

    // the name of the request parameter
    String paramName

    FilterOperatorsEnum operator

    // the actual value of the user input
    String paramValue
    // converted to the actual filter type
    def value

    //only used for global
    // the search filter
    Closure searchFilter
    boolean global = false


    Filter() {
    }

    // the parameter map
    @Deprecated
    def params

    @Deprecated
    public Filter(FilterableConfig filterableConfig) {
        init()
        this.filterable = filterableConfig
        this.searchFilter = filterableConfig.filterClosure
        this.paramName = filterableConfig.name
        this.paramValue = this.params[this.paramName]
        this.value = GridUtils.convertValueUsingBinding(paramValue, filterableConfig.filterDataType)
    }

    @Deprecated
    public Filter(Closure searchFilter, String paramValue) {
        init()
        this.searchFilter = searchFilter
        this.paramValue = paramValue
    }

    @Deprecated
    public Filter(Closure searchFilter, boolean global = true) {
        init()
        this.searchFilter = searchFilter
        this.global = global
    }


    private init() {
        this.params = EasygridContextHolder.params
    }

    static Closure v(value, Closure c){
        (value == Filter.FAILED_CONVERSION)?null:c
    }


}

enum FilterOperatorsEnum {
    EQ('equal'),
    NE('not equal'),
    LT('less'),
    LE('less or equal'),
    GT('greater'),
    GE('greater or equal'),
    BW('begins with'),
    BN('does not begin with'),
    IN('is in'),
    NI('is not in'),
    EW('ends with'),
    EN('does not end with'),
    CN('contains'),
    NC('does not contain'),
    NU('is null'),
    NN('is not null')

    FilterOperatorsEnum(String name) {
        this.name = name
    }

    String name
}