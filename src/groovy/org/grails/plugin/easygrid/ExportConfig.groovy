package org.grails.plugin.easygrid

import groovy.transform.AutoClone
import org.grails.plugin.easygrid.ast.DynamicConfig

/**
 * configurations for the export section of the grid
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@DynamicConfig
@AutoClone
class ExportConfig {

    Boolean export          // allow exporting
    String export_title     // the title of the exported file
    Class exportService     // the implementation of the export service

    Integer maxRows             // the maximum number of rows to be exported
}
