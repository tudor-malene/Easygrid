import org.grails.plugin.easygrid.Filter
import org.grails.plugin.easygrid.grids.DataTablesGridService

// configuration for plugin testing - will not be included in the plugin zip

log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}

    error 'org.codehaus.groovy.grails.web.servlet',  //  controllers
            'org.codehaus.groovy.grails.web.pages', //  GSP
            'org.codehaus.groovy.grails.web.sitemesh', //  layouts
            'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
            'org.codehaus.groovy.grails.web.mapping', // URL mapping
            'org.codehaus.groovy.grails.commons', // core / classloading
            'org.codehaus.groovy.grails.plugins', // plugins
            'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
            'org.springframework',
            'org.hibernate',
            'net.sf.ehcache.hibernate'

    warn 'org.mortbay.log'
}

// Added by Easygrid:

easygrid {

    //default values added to each defined grid
    defaults {
        defaultMaxRows = 10
        //used for automatically generating label messages from the column name
        //this will be transformed into a SimpleTemplateEngine instance ( '#' will be replaced with '$') and the binding variables will be: labelPrefix , column, gridConfig
        labelFormat = '#{labelPrefix}.#{column.name}.label'

        //called before inline editing : transforms the parameters into the actual object to be stored
        beforeSave = { params -> params }
        gridImpl = 'jqgrid'
        exportService = org.grails.plugin.easygrid.EasygridExportService

        //jqgrid default properties
        jqgrid {
            width = '"100%"'
            height = 250
            // number of rows to display by default
            rowNum = 20
        }

        // spring security implementation
        securityProvider = { grid, oper ->
            if (!grid.roles) {
                return true
            }
            def grantedRoles
            if (Map.isAssignableFrom(grid.roles.getClass())) {
                grantedRoles = grid.roles.findAll { op, role -> oper == op }.collect { op, role -> role }
            } else if (List.isAssignableFrom(grid.roles.getClass())) {
                grantedRoles = grid.roles
            } else {
                grantedRoles = [grid.roles]
            }
            SpringSecurityUtils.ifAllGranted(grantedRoles.inject('') { roles, role -> "${roles},${role}" })
        }


    }

    // each grid has a "type" - which must be one of the datasources
    dataSourceImplementations {
        //deprecated
        domain {
            // mandatory attribute: domainClass or initialCriteria
            dataSourceService = org.grails.plugin.easygrid.datasource.GormDatasourceService
            filters {
                //default search closures
                text = { Filter filter -> ilike(filter.column.name, "%${filter.paramValue}%") }
                number = { Filter filter -> eq(filter.column.name, filter.paramValue as int) }
                //todo
                date = { Filter filter -> eq(filter.column.name, filter.paramValue as Date) }

            }
        }

        // renamed for consistency - todo  -rename everywhere
        gorm {
            // mandatory attribute: domainClass or initialCriteria
            dataSourceService = org.grails.plugin.easygrid.datasource.GormDatasourceService
            filters {
                //default search closures
                text = { Filter filter -> ilike(filter.column.name, "%${filter.paramValue}%") }
                number = { Filter filter -> eq(filter.column.name, filter.paramValue as int) }
                //todo
                date = { Filter filter -> eq(filter.column.name, filter.paramValue as Date) }

            }
        }

        list {
            //mandatory attributes: context, attributeName
            dataSourceService = org.grails.plugin.easygrid.datasource.ListDatasourceService
            filters {
                //default search closures
                text = { Filter filter, row -> row[filter.column.name].contains filter.paramValue }
                number = { Filter filter, row -> row[filter.column.name] == filter.paramValue as int }
                //todo
                date = { Filter filter, row -> row[filter.column.name] == filter.paramValue as Date }

            }
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
                    (Date): { it.format("dd/MM/yyyy") },
                    (Boolean): { it ? "Yes" : "No" }
            ]
        }

        jqgrid {
            gridRenderer = '/templates/jqGridRenderer'
            gridImplService = org.grails.plugin.easygrid.grids.JqueryGridService
            inlineEdit = true
            editRenderer = '/templates/jqGridEditResponse'
            formats = [
                    (Date): { it.format("dd/MM/yyyy") },
                    (Calendar): { Calendar cal -> cal.format("dd/MM/yyyy") },
                    (Boolean): { it ? "Yes" : "No" }
            ]
        }

        dataTables {
            gridImplService = DataTablesGridService
            gridRenderer = '/templates/dataTablesGridRenderer'
            inlineEdit = false
            formats = [
                    (Date): { it.format("dd/MM/yyyy") },
                    (Boolean): { it ? "Yes" : "No" }
            ]
        }

        visualization {
            gridImplService = org.grails.plugin.easygrid.grids.VisualizationGridService
            gridRenderer = '/templates/visualizationGridRenderer'
            inlineEdit = false
            formats = [
                    (Date): { def cal = com.ibm.icu.util.Calendar.getInstance(); cal.setTime(it); cal.setTimeZone(com.ibm.icu.util.TimeZone.getTimeZone("GMT")); cal }, //wtf?
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
                value = { '' }
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

grails.views.default.codec = "none" // none, html, base64
grails.views.gsp.encoding = "UTF-8"
