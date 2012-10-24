package org.grails.plugin.easygrid.builder

/**
 * utility class that transforms a simple closure into a map
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class GridImplDelegate {
    private Map properties

    GridImplDelegate(Map properties) {
        this.properties = properties
    }

    def methodMissing(String columnProperty, columnValue) {
        properties[columnProperty]=columnValue[0]
    }

}
