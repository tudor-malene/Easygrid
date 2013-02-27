package org.grails.plugin.easygrid

/**
 * defines a filter form field
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class FilterableConfig {
    String name   // the name of the filter field
    Closure filterClosure

}
