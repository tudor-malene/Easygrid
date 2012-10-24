def appCtx = org.codehaus.groovy.grails.commons.ApplicationHolder.application.mainContext
def plugin = appCtx.pluginManager.getGrailsPlugin('easygrid')

def jqgridVer = plugin.instance.JQGRID_VERSION
def datatablesVer = plugin.instance.DATATABLES_VERSION

modules = {
    'easygrid-jqgrid-theme' {
        resource id: 'theme',
                url: [plugin: 'easygrid', dir: "jquery.jqGrid-${jqgridVer}/css",
                        file: 'ui.jqgrid.css'],
                attrs: [media: 'screen, projection']
    }

    'easygrid-jqgrid' {
        dependsOn 'jquery-ui', 'easygrid-jqgrid-theme'

        resource id: 'js-jqgrid', url: [plugin: 'easygrid', dir: "jquery.jqGrid-${jqgridVer}/js", file: "jquery.jqGrid.min.js"],
                nominify: true, disposition: 'head'
        //todo multilanguage
        resource id: 'js-locale-jqgrid', url: [plugin: 'easygrid', dir: "jquery.jqGrid-${jqgridVer}/js/i18n", file: "grid.locale-en.js"],
                nominify: true, disposition: 'head'
    }

    'easygrid-datatables' {
        dependsOn 'jquery-ui'

        resource id: 'js-datatable', url: [plugin: 'easygrid', dir: "DataTables-${datatablesVer}/media/js", file: "jquery.dataTables.min.js"],
                nominify: true, disposition: 'head'
    }


    'easygrid-jqgrid-dev' {
        dependsOn 'jquery-ui', 'easygrid-jqgrid-theme'

        //JQGRID
        resource id: 'js-jqgrid', url: [plugin: 'easygrid', dir: "jquery.jqGrid-${jqgridVer}/js", file: "jquery.jqGrid.src.js"],
                nominify: true, disposition: 'head'
        //todo multilanguage
        resource id: 'js-locale-jqgrid', url: [plugin: 'easygrid', dir: "jquery.jqGrid-${jqgridVer}/js/i18n", file: "grid.locale-en.js"],
                nominify: true, disposition: 'head'
    }

    'easygrid-datatables-dev' {
        dependsOn 'jquery-ui'

        resource id: 'js-datatable', url: [plugin: 'easygrid', dir: "DataTables-${datatablesVer}/media/js", file: "jquery.dataTables.js"],
                nominify: true, disposition: 'head'
    }

    'easygrid-visualization-dev' {
//        dependsOn 'jquery-ui', 'easygrid-theme'

        resource id: 'js-visualization-api', url: [plugin: 'easygrid', dir: "visualization", file: "jsapi.js"],
                nominify: true, disposition: 'head'
        resource id: 'js-visualization-util', url: [plugin: 'easygrid', dir: "visualization", file: "visualizationUtils.js"],
                nominify: true, disposition: 'head'
    }


}