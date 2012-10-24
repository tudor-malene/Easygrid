package org.grails.plugin.easygrid

/**
 * this Class is the basic structure of each Grid
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class Grid {

    String id

    // the columns
    List<Column> columns  =[]

    // the datasource
    String dataSourceType
    Class dataSourceService  // the datasource

    // the implementation
    String gridImpl
    Class gridImplService
    String gridRenderer

    // export
    boolean export
    String export_title
    Class exportService

    //security
    def roles
    Closure securityProvider


    // inline edit
    boolean inlineEdit
    String editRenderer
    Closure beforeSave

    // formatters for each value ( depend on the class )
    Map<Class,Closure> formats


    //dynamic
    Map properties = [:]
    def propertyMissing(String name, value) { properties[name] = value }
    def propertyMissing(String name) { properties[name] }

}
