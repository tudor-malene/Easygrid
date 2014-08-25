package org.grails.plugin.easygrid

import groovy.transform.Canonical

/**
 * configurations for the export section of the grid
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Canonical
class ExportConfig extends AbstractDynamicConfig{

    Boolean export          // allow exporting
    String export_title     // the title of the exported file
    Class exportService     // the implementation of the export service

    Integer maxRows             // the maximum number of rows to be exported

    ExportConfig() {
    }
}
