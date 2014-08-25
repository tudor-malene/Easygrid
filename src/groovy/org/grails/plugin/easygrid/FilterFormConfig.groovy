package org.grails.plugin.easygrid

import groovy.transform.Canonical

/**
 * configurations for the dynamic filter form
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Canonical
class FilterFormConfig extends AbstractDynamicConfig{

    ListMapWrapper<FilterFieldConfig> fields = new ListMapWrapper<FilterFieldConfig>('name')
    Class filterFormService
    String filterFormRenderer

    FilterFormConfig() {
    }
}
