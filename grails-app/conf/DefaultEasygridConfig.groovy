import com.google.visualization.datasource.datatable.value.ValueType
import org.codehaus.groovy.grails.plugins.web.taglib.FormatTagLib
import org.grails.plugin.easygrid.AutocompleteService
import org.grails.plugin.easygrid.EasygridExportService
import org.grails.plugin.easygrid.FilterFormService
import org.grails.plugin.easygrid.GridUtils
import org.grails.plugin.easygrid.datasource.CustomDatasourceService
import org.grails.plugin.easygrid.datasource.GormDatasourceService
import org.grails.plugin.easygrid.datasource.ListDatasourceService
import org.grails.plugin.easygrid.grids.ClassicGridService
import org.grails.plugin.easygrid.grids.DataTablesGridService
import org.grails.plugin.easygrid.grids.JqueryGridService
import org.grails.plugin.easygrid.grids.VisualizationGridService

import java.awt.*

import static org.grails.plugin.easygrid.FilterOperatorsEnum.*

easygrid {

    //default values added to each defined grid  ( if they are not already set )
    defaults {

        defaultMaxRows = 20 // the max no of rows displayed in the grid

        //used for automatically generating label messages from the column name
        //this will be transformed into a SimpleTemplateEngine instance ( '#' will be replaced with '$') and the binding variables will be: labelPrefix , column, gridConfig
        labelFormat = '#{labelPrefix}.#{column.name}.label'

        //called before inline editing : transforms the parameters into the actual object to be stored
        beforeSave = { params ->
            params.remove('oper')
            params
        }

        gridImpl = 'jqgrid' // the default grid implementation

        //used by jqgrid
        enableFilter = true
        addNavGrid = true

        //the id column
        idColName = 'id'
        idColType = Long

        //default export settings for various formats
        export {
            exportService = EasygridExportService
            maxRows = 10000 // maximum rows to be exported - to avoid out of memory exceptions

            //this section provides default values for the different export formats
            // for more options check out the export plugin

            // csv settings
            csv {
                separator = ','
                quoteCharacter = '"'
            }
            csv['header.enabled'] = true

            // excel settings
            excel['header.enabled'] = true
            //property that aggregates the widths defined per column
            excel['column.widths'] = { gridConfig ->
                def widths = []
                GridUtils.eachColumn(gridConfig, true) { column ->
                    widths.add(column?.export?.width ?: 0.2)
                }
                widths
            }

            // pdf settings
            pdf['header.enabled'] = true
            pdf['column.widths'] = { gridConfig ->
                def widths = []
                GridUtils.eachColumn(gridConfig, true) { column ->
                    widths.add(column?.export?.width ?: 0.2)
                }
                widths
            }
            pdf['border.color'] = Color.BLACK
            pdf['pdf.orientation'] = 'landscape'

            // rtf settings
            rtf['header.enabled'] = true
            rtf {
            }

            // ods settings
            ods {
            }

            // xml settings
            xml['xml.root'] = { gridConfig ->
                //defaults to the export title
                gridConfig.export.export_title
            }
            xml {
            }
        }

        // jqgrid default properties
        // check the jqgrid documentation
        jqgrid {
            datatype = 'json'
            viewrecords = true
            width = "100%"
            height = 240
            // number of rows to display by default
            rowNum = 10
            rowList = [10, 20, 50]

            multiSort = true

            navGrid {
                generalOpts {
                    add = false
                    view = true
                    edit = false
                    del = false
                    search = true
                    refresh = true
                }
                editOpts {

                }
                addOpts {
                    afterSubmit = "g:easygrid.afterSubmit"
                    errorTextFormat = "g:easygrid.errorTextFormat"
                    reloadAfterSubmit = false
                    closeAfterAdd = true
                }
                delOpts {

                }
                searchOpts {
                    multipleSearch = true
                    multipleGroup = true
                    showQuery = true
                    caption = 'Multi-clause Searching'
                    closeAfterSearch = true
//                    groupOps = '[ { op: "AND", text: "and" }, { op: "OR", text: "or" } ]'
                    sopt = ['eq', 'ne', 'lt', 'le', 'gt', 'ge', 'bw', 'bn', 'ew', 'en', 'cn', 'nc', 'nu', 'nn']
                }
                viewOpts {
                    closeOnEscape = true
                }
            }

            filterToolbar {
                stringResult = true
                searchOperators = true
            }

        }

        dataTables {
            bFilter = true
            bStateSave = false
            sPaginationType = 'full_numbers'
            bSort = true
            bProcessing = true
            bServerSide = true
        }

        visualization {
            page = "enable"
            allowHtml = true
            alternatingRowStyle = true
//            showRowNumber = false
            pageSize = 10
        }

        //default autocomplete settings
        autocomplete {
            idProp = 'id'
            // the name of the property of the id of the selected element (optionKey - in the replaced select tag)
            maxRows = 10 // the max no of elements to be displayed by the jquery autocomplete box
            template = '/templates/easygrid/autocompleteRenderer' //the default autocomplete renderer
            autocompleteService = AutocompleteService
        }

        filterType {
            'text' {
                defaultOperator = CN
            }
            'boolean' {
                defaultOperator = EQ
            }
            'numeric' {
                defaultOperator = EQ
            }
            'date' {
                defaultOperator = EQ
            }
            'enum' {
                defaultOperator = EQ
            }
            'currency' {
                defaultOperator = EQ
            }
        }
    }

    // each grid has a "type" - which must be one of the datasources defined here
    dataSourceImplementations {
        // mandatory attribute: domainClass or initialCriteria

        gorm.dataSourceService = GormDatasourceService

        //mandatory attributes: context, attributeName
        list.dataSourceService = ListDatasourceService

        // mandatory attributes: 'dataProvider', 'dataCount'
        custom.dataSourceService = CustomDatasourceService
    }

    // these are the actual UI grid implementations
    // will be specified in the grid config using the 'gridImpl' property
    gridImplementations {

        //grails classic implementation - for demo purposes
        classic {
            gridRenderer = '/templates/easygrid/classicGridRenderer'
            gridImplService = ClassicGridService
            inlineEdit = false
            formats = [
                    (Date)   : { new FormatTagLib().formatDate(date: it) },
                    (Boolean): { new FormatTagLib().formatBoolean(boolean: it) }
            ]
        }

        //  jqgrid implementation
        jqgrid {
            gridRenderer = '/templates/easygrid/jqGridRenderer'          //  a gsp template that will be rendered
            gridImplService = JqueryGridService  // the service class for this implementation
            inlineEdit = true    // specifies that this implementation allows inline Editing

            // there are 3 options to format the data
            // using the value closure in the column
            // using the named formatters ( defined below )
            // using the default type formats ( defined here ) - where you specify the type of data & the format closure
            formats = [
                    (Date)    : { new FormatTagLib().formatDate(date: it) },
                    (Calendar): { new FormatTagLib().formatDate(date: it) },
                    (Boolean) : { new FormatTagLib().formatBoolean(boolean: it) }
            ]
        }

        //  jquery datatables implementation
        dataTables {
            gridImplService = DataTablesGridService
            gridRenderer = '/templates/easygrid/dataTablesGridRenderer'
            inlineEdit = false
            formats = [
                    (Date)   : { new FormatTagLib().formatDate(date: it) },
                    (Boolean): { new FormatTagLib().formatBoolean(boolean: it) }
            ]
        }

        // google visualization implementation
        visualization {
            gridImplService = VisualizationGridService
            gridRenderer = '/templates/easygrid/visualizationGridRenderer'
            inlineEdit = false
            formats = [
                    (Date): {
                        def cal = com.ibm.icu.util.Calendar.getInstance(); cal.setTime(it);
                        cal.setTimeZone(com.ibm.icu.util.TimeZone.getTimeZone("GMT")); cal
                    }, //wtf?
            ]
        }

    }

    // section to define per column configurations
    columns {

        //default values for the columns
        defaults {
            enableFilter = true
            showInSelection = true
            sortable = true
            render = true

            jqgrid {
                searchoptions {
                    clearSearch = false
                }
            }

            classic {
                sortable = true
            }
            visualization {
                search = false
                searchType = 'text'
                valueType = ValueType.TEXT
            }
            dataTables {
                sWidth = '100%'
                sClass = ""
                bVisible = true
            }
            export {
                width = 25
            }
            //the operators supported for different types of columns
            filterOperators.text = [CN, NC, EQ, NE, BW, EW, NU, NN]
            filterOperators.boolean = [EQ, NE, NU, NN]
            filterOperators.numeric = [EQ, NE, LT, LE, GT, GE, NU, NN]
            filterOperators.date = [EQ, NE, LT, LE, GT, GE, NU, NN]
            filterOperators.enum = [EQ, NE, NU, NN]
            filterOperators.currency = [EQ, NE, NU, NN]
        }

        // predefined column types  (set of configurations)
        // used to avoid code duplication
        types {
            id {
                property = 'id'
                enableFilter = false

                jqgrid {
                    width = 40
                    fixed = true
                    search = false
                    editable = false
//                formatter = 'editFormatter'
                }
                visualization {
                    valueType = ValueType.NUMBER
                }
                export {
                    width = 10
                }

            }

            actions {
                value = { '' }
                enableFilter = false
                sortable = false
                jqgrid {
                    formatter = 'actions'
                    editable = false
                    sortable = false
                    resizable = false
                    fixed = true
                    width = 60
                    search = false
                    formatoptions {
                        keys = true
//                        onEdit = 'g:easygrid.onEdit'
//                        onSuccess = 'g:easygrid.onSuccess'
                        afterSave = 'g:easygrid.afterSave'
                        onError = 'g:easygrid.onError'
                    }
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
                    valueType = ValueType.NUMBER
                }
            }
        }
    }

    //section to define the filter form configurations
    filterForm {
        defaults {
            filterFormService = FilterFormService
            filterFormTemplate = '/templates/filterFormRenderer'
        }
    }

    // here we define different formatters
    // these are closures  which are called before the data is displayed to format the cell data
    // these are specified in the column section using : formatName
    formats {
        stdDateFormatter = {
            new FormatTagLib().formatDate(date: it)
        }
        visualizationDateFormatter = {
            def cal = com.ibm.icu.util.Calendar.getInstance(); cal.setTime(it);
            cal.setTimeZone(TimeZone.getTimeZone("GMT") as com.ibm.icu.util.TimeZone); cal
        }
        stdBoolFormatter = {
            new FormatTagLib().formatBoolean(boolean: it)
        }
    }
}
