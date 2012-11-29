package org.grails.plugin.easygrid.builder

import grails.util.ClosureToMapPopulator
import org.grails.plugin.easygrid.ColumnConfig
import org.grails.plugin.easygrid.GridConfig
import org.grails.plugin.easygrid.GridUtils

/**
 * implementation for the EasyGrid DSL
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class EasygridBuilder {

    def grailsApplication

    EasygridBuilder(grailsApplication) {
        this.grailsApplication = grailsApplication
    }

    /**
     * will evaluate the closure and produce a list of grids
     *
     * @param gridsClosure The closure that defines the Grid DSL
     */
    Map<String, GridConfig> evaluate(Closure gridsClosure) {
        def grids = [:]
        buildWithDelegate(gridsClosure) {name, args ->
            grids[name] = evaluateGrid args[0]
        }
        grids
    }

    /**
     * evaluates the closure and returns a GridConfig
     * @param gridClosure
     * @return
     */
    GridConfig evaluateGrid(Closure gridClosure) {
        def gridConfig = new GridConfig()

        //build the grid
        buildWithDelegate(gridClosure, { String name, args ->
            switch (name) {

                case (GridUtils.findImplementations(grailsApplication?.config?.easygrid)):    //handle simple key-value properties for the different implementations
                    gridConfig[name] = new ClosureToMapPopulator().populate(args[0])
                    break;


                case ('columns'):   //handle the columns section
                    gridConfig.columns = []

                    // handle the columns section
                    buildWithDelegate(args[0])
                            { colName, colArgs -> // method missing
                                def column = new ColumnConfig(label: colName)
                                gridConfig.columns.add(column)

                                buildWithDelegate(colArgs[0]) { colProperty, colValue ->
                                    switch (colProperty) {
                                        case 'value':
                                            column.value = colValue[0]
                                            break
                                        case 'export':
                                            column.export = new ClosureToMapPopulator().populate(colValue[0])
                                            break
                                        default:
                                            if (colProperty in GridUtils.findImplementations(grailsApplication?.config?.easygrid)) {
                                                column[colProperty] = new ClosureToMapPopulator().populate(colValue[0])
                                            } else {
                                                column[colProperty] = colValue[0]
                                            }
                                            break
                                    }
                                }
                            }
                            { colName ->     // handle property missing, the case when you only define the label of the column
                                assert gridConfig.labelPrefix || gridConfig.domainClass
                                def prefix = gridConfig.labelPrefix ?: grails.util.GrailsNameUtils.getPropertyNameRepresentation(gridConfig.domainClass)
                                assert prefix

                                def label = grailsApplication?.config?.easygrid?.defaults?.labelFormat?.make(prefix: prefix, column: colName)
                                assert label

                                def column = new ColumnConfig(label: label, property: colName)
                                gridConfig.columns.add(column)
                            }
                    break

                case ('autocomplete'): //handle the autocomplete section
                    gridConfig.autocomplete = new ClosureToMapPopulator().populate(args[0])
                    break

                default:   // handle other properties
                    gridConfig[name] = args[0]
                    break;
            }
        })
        gridConfig
    }

    /**
     * utility method that invokes the builder closure with the missingMethod of the delegate set as the delegate closure
     * @param builder - the builder DSL
     * @param delegate - the method missing delegate
     * @param propertyDelegate - the property missing delegate
     * @return
     */
    def buildWithDelegate(Closure builder, Closure delegate, Closure propertyDelegate = null) {
        builder.delegate = [invokeMethod: delegate, getProperty: propertyDelegate] as GroovyObjectSupport
        builder.resolveStrategy = Closure.DELEGATE_FIRST
        try {
            builder()
        } finally {
            builder.delegate = null
        }
    }

}