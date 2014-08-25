package org.grails.plugin.easygrid

/**
 * defines a filter form field
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class FilterableConfig extends AbstractDynamicConfig{
    String name   // the name of the filter field

    Boolean enableFilter   // flag that specifies if filtering should be enabled on this column

    //// used for converting the param
    Class filterDataType

    //    can be 'numeric','text','date'
    // used for determining different properties - like the filter operators
    String filterType

    // for converting filter values
    Closure filterConverter

    //if the filterClosure is not defined then easygrid will generate a filter based on the operator & the filterProperty
    Closure filterClosure

    String filterProperty
    FilterOperatorsEnum defaultFilterOperator
}
