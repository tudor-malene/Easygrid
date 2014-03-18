package org.grails.plugin.easygrid

/**
 * defines a filter form field
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class FilterableConfig {
    String name   // the name of the filter field

    //// used for converting the param
    Class dataType

    //if the filterClosure is not defined then easygrid will generate a filter based on the operator & the filterProperty
    Closure filterClosure

    //todo - implement
    String filterProperty
    FilterOperatorsEnum defaultFilterOperator
}
