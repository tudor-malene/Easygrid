package org.grails.plugin.easygrid

import grails.util.Environment
import grails.util.GrailsNameUtils
import groovy.text.SimpleTemplateEngine
import groovy.util.logging.Slf4j
import org.codehaus.groovy.control.ConfigurationException
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsControllerClass
import org.codehaus.groovy.grails.exceptions.GrailsConfigurationException
import org.grails.plugin.easygrid.builder.EasygridBuilder
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus

import java.lang.reflect.Field
import java.lang.reflect.Modifier

import static org.codehaus.groovy.grails.commons.GrailsClassUtils.getStaticFieldValue
import static org.grails.plugin.easygrid.EasygridContextHolder.getParams
import static org.grails.plugin.easygrid.GridUtils.cloneGrid

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

        // on each annotated controller - first invoke the builder and construct the GridConfigs and then register the grid methods
        grailsApplication.controllerClasses?.findAll { it.clazz.isAnnotationPresent(Easygrid) }?.each { controller ->
            Map<String, GridConfig> gridsConfig = initControllerGrids(controller)
            grids[controller.logicalPropertyName] = gridsConfig
            log.debug "${gridsConfig.size()} grids found in ${controller}"
            gridsConfig.each this.&registerControllerMethods.curry(controller)
        }

        easygridService.setGridRepository(grids)
        log.debug 'Finished initializing Easygrid '
    }


    def registerControllerMethods(controller, String gridName, GridConfig gridConfig) {
        def easyGridLogger = LoggerFactory.getLogger(controller.clazz)
        def closureMap = [
                html  : {
                    easyGridLogger.debug("entering ${gridName}Html")
                    def model = easygridService.htmlGridDefinition(gridConfig)
                    if (model) {
                        model.attrs = [id: params.gridId ?: gridConfig.id, params: params]
                        render(template: gridConfig.gridRenderer, model: model)
                    }
                },
                rows  : {
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

                def response = easygridService.inlineEdit(gridConfig)
                render easygridDispatchService.callGridImplTransformInlineResponse(gridConfig, response)
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

    def registerClosures(controller, closureMap) {
        //register the closures so they can be retreived by : metaProperty = controller.getMetaClass().getMetaProperty(actionName);
        // in MixedGrailsControllerHelper
        closureMap.each { action, closure ->
            def metaClass = controller.clazz.metaClass
            def mp = metaClass.getMetaProperty(action)
            //only register the first time when not in development mode
/*
            if (Environment.developmentMode || (controller.clazz.metaClass.methods.find {
                it.name == methodName
            } == null)) {
*/
            if (Environment.developmentMode || (mp == null)) {
                metaClass."${GrailsClassUtils.getGetterName(action)}" = { ->
                    Closure newClosure = closure.clone()
                    newClosure.delegate = delegate
                    newClosure.resolveStrategy = Closure.DELEGATE_FIRST
                    newClosure
                }
                controller.registerMapping action
            }
        }
    }

    /**
     * initializes a grid defined at runtime
     * @param controller - the controller where to add the grid
     * @param gridConfig
     */
    def GridConfig initializeGrid(controller, gridName, domainClass) {
        def grid = addDefaultValues(new GridConfig(id: gridName, dataSourceType: 'gorm', domainClass: domainClass))
        registerControllerMethods(controller, grid.id, grid)
        easygridService.setGridConfig(controller.logicalPropertyName, grid.id, grid)
    }

    /**
     * constructs the configuration from the builder
     * @param controller - the controller bean
     * @return the initialized grids structure for the controller
     */
    def Map<String, GridConfig> initControllerGrids(GrailsControllerClass controller, controllerBean = null) {

        def controllerClass = controller.clazz
        controllerBean = controllerBean ?: grailsApplication.mainContext.getBean(controllerClass)

        log.debug("   Run init grids for ${controllerClass}")
        def controllerGridsMap = [:]

        // add all the grids defined in the static grids section
        def staticGridsClosure = getStaticFieldValue(controllerClass, GRIDS_FIELD)
        if (staticGridsClosure) {
            //set the owner of the closures to the controller - so that services injected in the controller, or params, session, etc, can be used
            staticGridsClosure = staticGridsClosure.dehydrate().rehydrate(null, controllerBean, staticGridsClosure.thisObject)
            controllerGridsMap.putAll initializeFromClosure(staticGridsClosure)
        }

        // add all the grids defined in the external file
        def externalGridsClass = controllerClass.getAnnotation(Easygrid)?.externalGrids()
        if (externalGridsClass) {
            def externalGridsClosure = getStaticFieldValue(externalGridsClass, GRIDS_FIELD)
            if (externalGridsClosure) {
                controllerGridsMap.putAll initializeFromClosure(externalGridsClosure)
            }
        }

        //add all the grids defined in closures ending with 'Grid'
        controllerClass.declaredFields.findAll { Field field -> field.name.endsWith(GRID_SUFFIX) && !Modifier.isStatic(field.modifiers) && (controllerBean[field.name] instanceof Closure) }.each { Field field ->
            def name = field.name[0..-GRID_SUFFIX.length() - 1]
            controllerGridsMap[name] = initializeFromClosureMethod(name, controllerBean[field.name])
            //remove the action
            //todo
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

    public static final String DEFAULT_DATASOURCE = 'gorm'
    /**
     * adds the default values specified in the config file - to the grid configuration
     * also adds convention rules
     * todo - refactor ( extract the conventions & document properly)
     * @param originalGridConfig
     * @return a grid with the default values
     */
    def GridConfig addDefaultValues(GridConfig originalGridConfig) {
        log.debug "start adding default values: $originalGridConfig"

        Map defaultValues = grailsApplication?.config?.easygrid
        GridConfig gridConfig = cloneGrid originalGridConfig

        assert gridConfig.id

        //convention - if no dataSourceType then set 'gorm'
        if (!gridConfig.dataSourceType && !gridConfig.dataSourceService) {
            gridConfig.dataSourceType = DEFAULT_DATASOURCE
        }

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


        gridConfig.beforeApplyingColumnRules?.call(gridConfig)

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

            //convention - set the property to the name
            if (!column.property && !column.value) {
                log.debug("set default property: ${column.name}")
                column.property = column.name
            }

            //set enableFilter false in case the global setting is false
            if (gridConfig.enableFilter == false) {
                column.enableFilter = false
            }

            //set the filterProperty in case
            if (column.enableFilter && !column.filterProperty && !column.filterClosure) {
//                column.filterProperty = column.property ?: column.name
                column.filterProperty = column.property
            }


            if (column.label == null) {
                def prefix = gridConfig.labelPrefix
                if (gridConfig.domainClass) {
                    prefix = prefix ?: GrailsNameUtils.getPropertyNameRepresentation(gridConfig.domainClass)
                    assert prefix
                }
                if (prefix) {
                    column.label = gridConfig.labelFormatTemplate.make(labelPrefix: prefix, column: column, gridConfig: gridConfig)
                } else {
                    column.label = ''
                }
            }

        }

        if (gridConfig.filterForm) {
            gridConfig.filterForm.fields.each { FilterFieldConfig filterFieldConfig ->
                //todo - types
                GridUtils.copyProperties defaultValues?.filterForm?.defaults, filterFieldConfig, 0
            }
        }

        //calls the "addDefaultValues" method of the service class for the specific implementation of the grid
        easygridDispatchService.callDSAddDefaultValues(gridConfig, defaultValues)
        easygridDispatchService.callGridImplAddDefaultValues(gridConfig, defaultValues)
        if (gridConfig.export) {
            easygridDispatchService.callExportAddDefaultValues(gridConfig, defaultValues)
        }
//        easygridDispatchService.callFFAddDefaultValues(gridConfig, defaultValues)

/*
        gridConfig.columns.each { ColumnConfig column ->
            // add default filterClosure
            if (column.enableFilter && column.filterClosure == null && column.filterFieldType) {
//                assert !column.property.contains('.'): "Currently default properties are supported only for simple properties. Please add the filter closure for ${column.name}"
                def defaultFilterClosure = defaultValues?.dataSourceImplementations?."${gridConfig.dataSourceType}"?.filters?."${column.filterFieldType}"
//                assert filterClosure: "no default filterClosure defined for '${column.filterFieldType}'"

                column.filterClosure = defaultFilterClosure
//                if (column.property.indexOf('.') > -1) {
//                    column.filterClosure = GridUtils.buildClosure(column.property.split('\\.')[0..-2], defaultFilterClosure)
//                } else {
//                    column.filterClosure = defaultFilterClosure
//                }
            }
        }
*/

        gridConfig.afterInitialization?.call(gridConfig)

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
