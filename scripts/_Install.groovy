// add the configurations to the Config file
def configFile = new File(basedir, 'grails-app/conf/Config.groovy')
if (configFile.exists() && configFile.text.indexOf("easygrid") == -1) {
    configFile.withWriterAppend {
        it.writeLine '\n// Added by Easygrid:'
        it.writeLine '''
easygrid {

    //default values added to each defined grid
    defaults {
        defaultMaxRows = 20
        //called before inline editing : transforms the parameters into the actual object to be stored
        beforeSave = {params -> params}
        gridImpl = 'jqgrid'
        exportService = org.grails.plugin.easygrid.EasyGridExportService

        //jqgrid default properties
        jqgrid {
            width = '100%'
            height = 250
        }
    }

    // each grid has a "type" - which must be one of the datasources
    dataSourceImplementations {
        domain {
            // mandatory attribute: domainClass or initialCriteria
            dataSourceService = org.grails.plugin.easygrid.datasource.GormDatasourceService
        }

        list {
            //mandatory attributes: context, attributeName
            dataSourceService = org.grails.plugin.easygrid.datasource.ListDatasourceService
        }


        custom {
            // mandatory attributes: 'dataProvider', 'dataCount'
            dataSourceService = org.grails.plugin.easygrid.datasource.CustomDatasourceService
        }
    }

    // will be specified using the 'gridImpl' property
    gridImplementations {
        //grails classic implementation
        classic {
            gridRenderer = '/templates/classicGridRenderer'
            gridImplService = org.grails.plugin.easygrid.grids.ClassicGridService
            inlineEdit = false
            formats = [
                    (Date.class): {it.format("dd/MM/yyyy")},
                    (Boolean.class): { it ? "Yes" : "No" }
            ]
        }

        jqgrid {
            gridRenderer = '/templates/jqGridRenderer'
            gridImplService = org.grails.plugin.easygrid.grids.JqueryGridService
            inlineEdit = true
            editRenderer = '/templates/jqGridEditResponse'
            formats = [
                    (Date.class): {it.format("dd/MM/yyyy")},
                    (Boolean.class): { it ? "Yes" : "No" }
            ]
        }

        datatable {
            gridImplService = org.grails.plugin.easygrid.grids.DatatableGridService
            gridRenderer = '/templates/datatableGridRenderer'
            inlineEdit = false
            formats = [
                    (Date.class): {it.format("dd/MM/yyyy")},
                    (Boolean.class): { it ? "Yes" : "No" }
            ]
        }

        visualization {
            gridImplService = org.grails.plugin.easygrid.grids.VisualizationGridService
            gridRenderer = '/templates/visualizationGridRenderer'
            inlineEdit = false
            formats = [
                    (Date.class): {def cal = com.ibm.icu.util.Calendar.getInstance(); cal.setTime(it); cal.setTimeZone(TimeZone.getTimeZone("GMT")); cal}, //wtf?
            ]
        }
    }

    columns {

        //default values for the columns
        defaults {
            jqgrid {
                editable = true
            }
            export {
                width = 40
            }
            classic {
            }
        }

        // predefined column types
        types {
            id {
                property = 'id'

                jqgrid {
                    width = 40
                    fixed = true
                    search = false
                    editable = false
//                formatter = 'editFormatter'
                }
            }
        }
    }

    // here we define different formatters
    formats {
        stdDateFormatter = {
            it.format("dd/MM/yyyy")
        }
        visualizationDateFormatter = {
            def cal = com.ibm.icu.util.Calendar.getInstance(); cal.setTime(it); cal.setTimeZone(TimeZone.getTimeZone("GMT")); cal
        }
        stdBoolFormatter = {
            it ? "Yes" : "No"
        }
    }

}


'''
    }
}

// copy the templates
copyTemplates([ 'jqGridRenderer', 'jqGridEditResponse','classicGridRenderer', 'datatableGridRenderer', 'visualizationGridRenderer', ])


def copyTemplates(templates) {
    try {
        templates.each {
            ant.copy file: new File(easygridPluginDir, "grails-app/views/templates/_${it}.gsp"), todir: new File(basedir, 'grails-app/views/templates'), overwrite: false
        }
    } catch (Exception e) {
        e.printStackTrace()
    }
}