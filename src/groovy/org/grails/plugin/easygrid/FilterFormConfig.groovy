package org.grails.plugin.easygrid

import groovy.transform.AutoClone
import org.grails.plugin.easygrid.ast.DynamicConfig

/**
 * configurations for the dynamic filter form
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@DynamicConfig
@AutoClone
class FilterFormConfig {

    ListMapWrapper<FilterFieldConfig> fields = new ListMapWrapper<FilterFieldConfig>('name')
    Class filterFormService
    String filterFormRenderer
}
