package org.grails.plugin.easygrid

import groovy.transform.AutoClone
import org.grails.plugin.easygrid.ast.DynamicConfig

/**
 * represents an element that is filterable
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@DynamicConfig
@AutoClone
class FilterFieldConfig extends FilterableConfig{


    String label   // the label
    String type   // represents one of the predefined filter field types ( sets of configurations  )

}