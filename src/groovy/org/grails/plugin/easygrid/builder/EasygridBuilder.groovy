package org.grails.plugin.easygrid.builder

import grails.util.ClosureToMapPopulator
import org.grails.plugin.easygrid.*

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
        GridConfig gridConfig = new GridConfig()

        def defaultValues = grailsApplication?.config?.easygrid
        //build the grid
        buildWithDelegate(gridClosure, { String name, Object args ->
            switch (name) {

                case (GridUtils.findImplementations(defaultValues)):    //handle simple key-value properties for the different implementations
                    gridConfig[name] = new RecursiveClosureToMap().populate(args[0])
                    break;


                case ('columns'):   //handle the columns section
                    gridConfig.columns = new ListMapWrapper<ColumnConfig>('name')

                    // handle the columns section
                    buildWithDelegate(args[0])
                            { colName, colArgs -> // method missing
                                def column = new ColumnConfig()
                                column.name = colName

                                buildWithDelegate(colArgs[0])
                                        { colProperty, colValue ->
                                            switch (colProperty) {
                                                case 'value':
                                                    column.value = colValue[0]
                                                    break
                                                case 'export':
                                                    column.export = new ClosureToMapPopulator().populate(colValue[0])
                                                    break
                                                default:
                                                    if (colProperty in GridUtils.findImplementations(grailsApplication?.config?.easygrid)) {
                                                        column[colProperty] = new RecursiveClosureToMap().populate(colValue[0])
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

                case ('filterForm'):   //handle the filter form section
                    def filterFormConfigMap = new ClosureToMapPopulator().populate(args[0])
                    def fieldsClosure = filterFormConfigMap.remove('fields')
                    gridConfig.filterForm = new FilterFormConfig(filterFormConfigMap)

                    if (fieldsClosure) {
                        buildWithDelegate(fieldsClosure)
                                { ffName, ffArgs -> // method missing
                                    def filterField = new FilterFieldConfig()
                                    filterField.name = ffName

                                    buildWithDelegate(ffArgs[0])
                                            { ffProperty, ffValue ->
                                                switch (ffProperty) {
                                                    default:
                                                        if (ffProperty in GridUtils.findImplementations(grailsApplication?.config?.easygrid)) {
                                                            filterField[ffProperty] = new ClosureToMapPopulator().populate(ffValue[0])
                                                        } else {
                                                            filterField[ffProperty] = ffValue[0]
                                                        }
                                                        break
                                                }
                                            }

                                    assert filterField.name
                                    gridConfig.filterForm.fields.add filterField
                                }
                                { ffName ->
                                }
                    }
                    break

                case ('autocomplete'): //handle the autocomplete section
                    gridConfig.autocomplete = new AutocompleteConfig(new ClosureToMapPopulator().populate(args[0]))
                    break

                case ('export'): //handle the autocomplete section
                    gridConfig.export = new ExportConfig()
                    buildWithDelegate(args[0])
                            { expProperty, expValue ->
                                if (expValue[0] instanceof Closure) {
                                    gridConfig.export[expProperty] = new ClosureToMapPopulator().populate(expValue[0])
                                } else {
                                    gridConfig.export[expProperty] = expValue[0]
                                }
                            }
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
    static def buildWithDelegate(Closure builderClosure, Closure delegate, Closure propertyDelegate = null) {
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

class RecursiveClosureToMap extends GroovyObjectSupport {

    private Map map;

    public RecursiveClosureToMap(Map theMap) {
        map = theMap;
    }

    public RecursiveClosureToMap() {
        this(new HashMap());
    }

    public Map populate(Closure callable) {
        callable.setDelegate(this);
        callable.setResolveStrategy(Closure.DELEGATE_FIRST);
        callable.call();
        return map;
    }

    @Override
    public void setProperty(String name, Object o) {
        if (o != null) {
            map.put(name, o);
        }
    }

    @Override
    public Object invokeMethod(String name, Object o) {
        if (o != null) {
            if (o.getClass().isArray()) {
                Object[] args = (Object[]) o;
                if (args.length == 1) {
                    if (args[0] instanceof Closure) {
                        map.put(name, new RecursiveClosureToMap().populate(args[0]))
                    } else {
                        map.put(name, args[0]);
                    }
                } else {
                    map.put(name, Arrays.asList(args));
                }
            } else {
                map.put(name, o);
            }
        }
        return null;
    }
}
