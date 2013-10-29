package org.grails.plugin.easygrid

import groovy.text.SimpleTemplateEngine
import groovy.util.logging.Slf4j
import org.codehaus.groovy.control.ConfigurationException
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException
import org.grails.plugin.easygrid.builder.EasygridBuilder
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus

import java.lang.reflect.Field
import java.lang.reflect.Modifier

import static org.codehaus.groovy.grails.commons.GrailsClassUtils.getStaticFieldValue

/**
 * handles the initialization of the grid
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
class EasygridInitService {

    public static final String GRID_SUFFIX = 'Grid'
    public static final String GRIDS_FIELD = 'grids'

    static transactional = false

    def grailsApplication
    def easygridService
    def easygridDispatchService

    /**
     * method that will initialize all grids
     * @return
     */
    def initializeGrids() {
        log.debug('Start initializing Easygrid grids..')

        // will hold all the grids < ControllerName, Map<GridName, GridConfig>>
        def grids = [:]

        grailsApplication.controllerClasses?.findAll { it.clazz.isAnnotationPresent(Easygrid) }?.each { controller ->

            Map<String, GridConfig> gridsConfig = initControllerGrids(grailsApplication.mainContext.getBean(controller.clazz))
            grids[controller.logicalPropertyName] = gridsConfig
            log.debug "${gridsConfig.size()} grids found in ${controller}"

            def easyGridLogger = LoggerFactory.getLogger(controller.clazz)

            gridsConfig.each { String gridName, GridConfig gridConfig ->

                def closureMap = [
                        html: {
                            easyGridLogger.debug("entering ${gridName}Html")
                            def model = easygridService.htmlGridDefinition(gridConfig)
                            if (model) {
                                model.attrs = [id: gridConfig.id]
                                render(template: gridConfig.gridRenderer, model: model)
                            }
                        },
                        rows: {
                            easyGridLogger.debug("entering ${gridName}Rows")
                            render easygridService.gridData(gridConfig)
                        },
                        export: {
                            easyGridLogger.debug("entering ${gridName}Export")
                            easygridService.export(gridConfig)
                        },
                ]


                if (grailsApplication.config?.easygrid?.gridImplementations[gridConfig.gridImpl]?.inlineEdit) {
                    closureMap.inlineEdit = {
                        easyGridLogger.debug("entering ${gridName}InlineEdit")
                        def result = easygridDispatchService.callGridImplInlineEdit(gridConfig)
                        def response = [status: HttpStatus.OK]
                        if (result) {
                            response.status = HttpStatus.INTERNAL_SERVER_ERROR
                            response.text = result
                        }
                        render response
                    }
                }

                if (gridConfig.autocomplete) {
                    closureMap.autocompleteResult = {
                        easyGridLogger.debug("entering ${gridName}AutocompleteResult")
                        render easygridDispatchService.callACSearchedElementsJSON(gridConfig)

                    }
                    closureMap.selectionLabel = {
                        easyGridLogger.debug("entering ${gridName}SelectionLabel")
                        render easygridDispatchService.callACLabel(gridConfig)
                    }
                }

                registerClosures(controller,
                        // transform the name & guard all access
                        closureMap.collectEntries { String mehodName, Closure closure ->
                            ["${gridName}${mehodName.capitalize()}".toString(), {
                                closure.delegate = delegate
                                easygridService.guard(gridConfig, closure)
                            }]
                        }
                )
            }

            easygridService.setGridRepository(grids)
        }

        log.debug 'Finished initializing Easygrid '

    }

    def registerClosures(controller, closureMap) {
        //register the closures so they can be retreived by : metaProperty = controller.getMetaClass().getMetaProperty(actionName);
        // in MixedGrailsControllerHelper
        closureMap.each { action, closure ->
            controller.registerMapping action
            controller.clazz.metaClass."get${action.capitalize()}" << { ->
                Closure newClosure = closure.clone()
                newClosure.delegate = delegate
                newClosure
            }
        }
    }


    /**
     * constructs the configuration from the builder
     * @param controller - the controller bean
     * @return the initialized grids structure for the controller
     */
    def Map<String, GridConfig> initControllerGrids(controller) {

        log.debug("   Run init grids for ${controller.class}")
        def controllerGridsMap = [:]

        // add all the grids defined in the static grids section
        def staticGridsClosure = getStaticFieldValue(controller.class, GRIDS_FIELD)
        if (staticGridsClosure) {
            //set the owner of the closures to the controller - so that services injected in the controller, or params, session, etc, can be used
            staticGridsClosure = staticGridsClosure.dehydrate().rehydrate(null, controller, staticGridsClosure.thisObject)
            controllerGridsMap.putAll initializeFromClosure(staticGridsClosure)
        }

        // add all the grids defined in the external file
        def externalGridsClosure = getStaticFieldValue(controller.class.getAnnotation(Easygrid).externalGrids(), GRIDS_FIELD)
        if (externalGridsClosure) {
            controllerGridsMap.putAll initializeFromClosure(externalGridsClosure)
        }

        //add all the grids defined in closures ending with 'Grid'
        controller.class.declaredFields.findAll { Field field -> field.name.endsWith(GRID_SUFFIX) && !Modifier.isStatic(field.modifiers) && (controller[field.name] instanceof Closure) }.each { Field field ->
            def name = field.name[0..-GRID_SUFFIX.length() - 1]
            controllerGridsMap[name] = initializeFromClosureMethod(name, controller[field.name])
        }

        controllerGridsMap
    }

    //called when initialising a grid from a controller closure
    def initializeFromClosureMethod(name, closureMethod) {
        GridConfig gridConfig = new EasygridBuilder(grailsApplication).evaluateGrid closureMethod
        gridConfig.id = name
        try {
            addDefaultValues(gridConfig)
        } catch (any) {
            log.error("Failed to initialize grid: ${name}", any)
            throw new GrailsConfigurationException("Failed to initialize grid: ${name}.", any)
        }
    }


    def initializeFromClosure(gridsClosure) {
        generateConfigForGrids(gridsClosure).collectEntries { gridName, gridConfig ->

            gridConfig.id = gridName

            //add default & types
            try {
                [(gridName): addDefaultValues(gridConfig)]
            } catch (any) {
                log.error("Failed to initialize grid: ${gridName}", any)
                throw new GrailsConfigurationException("Failed to initialize grid: ${gridName}.", any)
            }
        }
    }

    /**
     * adds the default values specified in the config file - to the grid configuration
     * todo - refactor
     * @param gridConfig
     * @return a grid with the default values
     */
    def addDefaultValues(GridConfig grid) {
        log.debug "start adding default values: $grid"

        Map defaultValues = grailsApplication?.config?.easygrid
        GridConfig gridConfig = grid.deepClone()

        assert gridConfig.id

        log.trace "before defaults: $gridConfig"

        //add the default values for the mandatory properties ( impl, type )
        GridUtils.copyProperties defaultValues.defaults, gridConfig, 1

        //set the labelFormat
        gridConfig.labelFormatTemplate = new SimpleTemplateEngine().createTemplate(
                gridConfig.labelFormat.toString().replace('#', '$')
        )


        log.trace "before impls: $gridConfig"

        assert gridConfig.gridImpl
        assert gridConfig.dataSourceType

        if (!gridConfig[gridConfig.gridImpl]) {
            gridConfig[gridConfig.gridImpl] = [:]
        }
        //add the default values for the impl
        GridUtils.copyProperties defaultValues.defaults[gridConfig.gridImpl], gridConfig[gridConfig.gridImpl]

        // handle the implementations...
        GridUtils.copyProperties defaultValues.gridImplementations[gridConfig.gridImpl], gridConfig
        GridUtils.copyProperties defaultValues.dataSourceImplementations[gridConfig.dataSourceType], gridConfig

        assert gridConfig.gridImplService
        assert gridConfig.gridRenderer
        assert gridConfig.dataSourceService

        log.trace "after impls: $gridConfig"

        //validate the grid
        def errors = verifyGridConstraints(gridConfig)
        if (errors) {
            throw new ConfigurationException("Grid ${gridConfig.id} not defined correctly: $errors")
        }

        //try to generate dynamic columns in case they are not defined
        if (!gridConfig.columns) {
            //add the columns from the datasource
            easygridDispatchService.callDSGenerateDynamicColumns(gridConfig)

            //add specifiec view properties for each column
            gridConfig.columns.each { col ->
                easygridDispatchService.callGridImplDynamicProperties(gridConfig, col)
            }
        }

//        if (gridConfig.export) {
        //todo - configure if export should be enabled by default?
        GridUtils.copyProperties defaultValues.defaults.export, gridConfig.export
//        }
        if (gridConfig.filterForm) {
            GridUtils.copyProperties defaultValues.filterForm.defaults, gridConfig.filterForm
        }
        if (gridConfig.autocomplete) {
            GridUtils.copyProperties defaultValues.defaults.autocomplete, gridConfig.autocomplete
        }

        //add the predefined types  to the columns
        gridConfig.columns.each { ColumnConfig column ->
            if (!column[gridConfig.gridImpl]) {
                column[gridConfig.gridImpl] = [:]
            }
            if (!column.export) {
                column.export = [:]
            }

            // copy the properties from the predefined type
            if (column.type) {
                def type = defaultValues?.columns?.types?."$column.type"

                if (type == null) {
                    throw new RuntimeException("type ${column.type} not defined for grid ${gridConfig.id}")
                }

                GridUtils.copyProperties type, column, 1
                GridUtils.copyProperties type[gridConfig.gridImpl], column[gridConfig.gridImpl]
                GridUtils.copyProperties type.export, column.export
                column.type = null
            }

            // copy the properties for the grid implementation & for the export
            GridUtils.copyProperties defaultValues?.columns?.defaults, column, 0
            GridUtils.copyProperties defaultValues?.columns?.defaults[gridConfig.gridImpl], column[gridConfig.gridImpl]
            GridUtils.copyProperties defaultValues?.columns?.defaults?.export, column.export

            // set the format closure
            if (column.formatName) {
                assert defaultValues?.formats[column.formatName]: "No ${column.formatName} formatter defined "
                column.formatter = defaultValues?.formats[column.formatName]
            }

            if (!column.property && !column.value) {
                column.property = column.name
            }

            if (column.label == null) {
                def prefix = gridConfig.labelPrefix
                if (gridConfig.domainClass) {
                    prefix = prefix ?: grails.util.GrailsNameUtils.getPropertyNameRepresentation(gridConfig.domainClass)
                    assert prefix
                }
                if (prefix) {
                    column.label = gridConfig.labelFormatTemplate.make(labelPrefix: prefix, column: column, gridConfig: gridConfig)
                } else {
                    column.label = ''
                }
            }

            // add default filterClosure
            if (column.enableFilter && column.filterClosure == null && column.filterFieldType) {
                assert !column.property.contains('.'): "Currently default properties are supported only for simple properties. Please add the filter closure for ${column.name}"
                def filterClosure = defaultValues?.dataSourceImplementations?."${gridConfig.dataSourceType}"?.filters?."${column.filterFieldType}"
                assert filterClosure: "no default filterClosure defined for '${column.filterFieldType}'"
                column.filterClosure = filterClosure
            }
        }

        if (gridConfig.filterForm) {
            gridConfig.filterForm.fields.each { FilterFieldConfig filterFieldConfig ->
                //todo - types
                GridUtils.copyProperties defaultValues?.filterForm?.defaults, filterFieldConfig, 0
            }
        }

        //calls the "addDefaultValues" method of the service class for the specific implementation of the grid
        easygridDispatchService.callGridImplAddDefaultValues(gridConfig, defaultValues)
        easygridDispatchService.callDSAddDefaultValues(gridConfig, defaultValues)
        if (gridConfig.export) {
            easygridDispatchService.callExportAddDefaultValues(gridConfig, defaultValues)
        }
//        easygridDispatchService.callFFAddDefaultValues(gridConfig, defaultValues)

        gridConfig
    }

    /**
     * check the validity of the configuration
     * @param gridConfig
     * @return
     */
    def verifyGridConstraints(gridConfig) {
        def errors = []

        if (!gridConfig.dataSourceType) {
            errors.add("Grid data source type is mandatory")
            return errors
        }
//        gridConfig.callGridPropertyMethod 'dataSourceService', 'verifyGridConstraints'
        easygridDispatchService.callDSVerifyGridConstraints(gridConfig)
    }

    /**
     * generates a config from a grids closure
     * @param gridsConfigClosure
     * @return
     */
    def generateConfigForGrids(Closure gridsConfigClosure) {
        new EasygridBuilder(grailsApplication).evaluate gridsConfigClosure
    }

}
