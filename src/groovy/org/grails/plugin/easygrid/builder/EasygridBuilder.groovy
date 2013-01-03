package org.grails.plugin.easygrid.builder

import grails.util.ClosureToMapPopulator
import org.grails.plugin.easygrid.ColumnConfig
import org.grails.plugin.easygrid.GridConfig
import org.grails.plugin.easygrid.GridUtils
import org.grails.plugin.easygrid.ColumnsConfig

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
        buildWithDelegate(gridsClosure) { name, args ->
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

        def defaultValues = grailsApplication?.config?.easygrid
        //build the grid
        buildWithDelegate(gridClosure, { String name, Object args ->
            switch (name) {

                case (GridUtils.findImplementations(defaultValues)):    //handle simple key-value properties for the different implementations
                    gridConfig[name] = new ClosureToMapPopulator().populate(args[0])

                    break;


                case ('columns'):   //handle the columns section
                    gridConfig.columns = new ColumnsConfig()

                    // handle the columns section
                    buildWithDelegate(args[0])
                            { colName, colArgs -> // method missing
                                def column = new ColumnConfig()
                                column.name = colName

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

                                assert column.name
                                gridConfig.columns.add(column)
                            }
                            { colName ->     // handle property missing, the case when you only define the label of the column
                                def column = new ColumnConfig(property: colName, name: colName)
                                assert column.name
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
     * utility method that invokes the builderClosure closure with the missingMethod of the delegate set as the delegate closure
     * @param builderClosure - the builderClosure DSL
     * @param delegate - the method missing delegate
     * @param propertyDelegate - the property missing delegate
     * @return
     */
    def buildWithDelegate(Closure builderClosure, Closure delegate, Closure propertyDelegate = null) {
//        builderClosure.delegate = [invokeMethod: delegate, getProperty: propertyDelegate] as GroovyObject

        builderClosure.delegate = new GroovyObjectSupport() {
            @Override
            Object invokeMethod(String name, Object args) {
                delegate.call(name, args)
            }

            @Override
            public Object getProperty(String property) {
                propertyDelegate?.call(property)
            }

        }
        builderClosure.resolveStrategy = Closure.DELEGATE_FIRST
        try {
            builderClosure()
        } finally {
            builderClosure.delegate = null
        }
    }

}