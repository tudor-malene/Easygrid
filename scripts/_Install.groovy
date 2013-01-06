// add the configurations to the Config file
File configFile = new File(basedir, 'grails-app/conf/Config.groovy')
//todo - de verificat
if (configFile.exists() && configFile.text.matches("easygrid[\\s*]{") == -1) {
    configFile.withWriterAppend {
        it.writeLine '\n// Added by Easygrid:'
        it.writeLine '''

easygrid {

    //default values added to each defined grid
    defaults {
        defaultMaxRows = 10
        labelFormat = '${labelPrefix}.${column.name}.label'
        //called before inline editing : transforms the parameters into the actual object to be stored
        beforeSave = {params -> params}
        gridImpl = 'jqgrid'
        exportService = org.grails.plugin.easygrid.EasygridExportService

        //jqgrid default properties
        jqgrid {
            width = '"100%"'
            height = 250
        }

        // spring security implementation
        securityProvider = { grid, oper ->
            if (!grid.roles) {
                return true
            }
            def grantedRoles
            if (Map.isAssignableFrom(grid.roles.getClass())) {
                grantedRoles = grid.roles.findAll {op, role -> oper == op}.collect {op, role -> role}
            } else if (List.isAssignableFrom(grid.roles.getClass())) {
                grantedRoles = grid.roles
            } else {
                grantedRoles = [grid.roles]
            }
            SpringSecurityUtils.ifAllGranted(grantedRoles.inject('') {roles, role -> "${roles},${role}"})
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
            gridRenderer = '/templates/easygrid/classicGridRenderer'
            gridImplService = org.grails.plugin.easygrid.grids.ClassicGridService
            inlineEdit = false
            formats = [
                    (Date): {it.format("dd/MM/yyyy")},
                    (Boolean): { it ? "Yes" : "No" }
            ]
        }

        jqgrid {
            gridRenderer = '/templates/easygrid/jqGridRenderer'
            gridImplService = org.grails.plugin.easygrid.grids.JqueryGridService
            inlineEdit = true
            editRenderer = '/templates/easygrid/jqGridEditResponse'
            formats = [
                    (Date): {it.format("dd/MM/yyyy")},
                    (Calendar): {Calendar cal ->cal.format("dd/MM/yyyy")},
                    (Boolean): { it ? "Yes" : "No" }
            ]
        }

        dataTables {
            gridImplService = org.grails.plugin.easygrid.grids.DataTablesGridService
            gridRenderer = '/templates/easygrid/dataTablesGridRenderer'
            inlineEdit = false
            formats = [
                    (Date): {it.format("dd/MM/yyyy")},
                    (Boolean): { it ? "Yes" : "No" }
            ]
        }

        visualization {
            gridImplService = org.grails.plugin.easygrid.grids.VisualizationGridService
            gridRenderer = '/templates/easygrid/visualizationGridRenderer'
            inlineEdit = false
            formats = [
                    (Date): {def cal = com.ibm.icu.util.Calendar.getInstance(); cal.setTime(it); cal.setTimeZone(com.ibm.icu.util.TimeZone.getTimeZone("GMT")); cal}, //wtf?
            ]
        }

    }

    columns {

        //default values for the columns
        defaults {
            jqgrid {
                editable = true
            }
            classic {
                sortable = true
            }
            visualization {
                search = false
                searchType = 'text'
                valueType = com.google.visualization.datasource.datatable.value.ValueType.TEXT
            }
            dataTables {
                width = "'100%'"
            }
            export {
                width = 25
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
                visualization {
                    valueType = com.google.visualization.datasource.datatable.value.ValueType.NUMBER
                }
                export {
                    width = 10
                }

            }

            actions {
                value = {''}
                jqgrid {
                    formatter = '"actions"'
                    editable = false
                    sortable = false
                    resizable = false
                    fixed = true
                    width = 60
                    search = false
                    formatoptions = '{"keys":true}'
                }
                export {
                    hidden = true
                }
            }

            version {
                property = 'version'
                jqgrid {
                    hidden = true
                }
                export {
                    hidden = true
                }
                visualization {
                    valueType = com.google.visualization.datasource.datatable.value.ValueType.NUMBER
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
            def cal = com.ibm.icu.util.Calendar.getInstance(); cal.setTime(it); cal.setTimeZone(java.util.TimeZone.getTimeZone("GMT")); cal
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
copyTemplates([ 'jqGridRenderer', 'jqGridEditResponse','classicGridRenderer', 'dataTablesGridRenderer', 'visualizationGridRenderer', ])


def copyTemplates(templates) {
    try {
        File dest = new File(basedir, 'grails-app/views/templates/easygrid')
        if (!dest.exists()){
            dest.mkdir()
        }
        templates.each {
            ant.copy file: new File(easygridPluginDir, "grails-app/views/templates/_${it}.gsp"), todir: dest, overwrite: false
        }
    } catch (Exception e) {
        e.printStackTrace()
    }
}