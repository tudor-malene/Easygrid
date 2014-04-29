package org.grails.plugin.easygrid

import java.beans.Introspector

import static org.grails.plugin.easygrid.GridUtils.getNestedPropertyValue

/**
 * class that will dispatch method calls to the appropriate implementation
 * will check first if the method was defined - in case the methods are optional
 */
class EasygridDispatchService {
    static transactional = false

    def grailsApplication

    def services = ['GridImpl': 'gridImplService', 'DS': 'dataSourceService', 'AC': 'autocomplete.autocompleteService', 'Export': 'export.exportService', 'FFF': 'filterForm.filterFormService']

    /**
     * calls methods of the form: call{GridService}{capitalizedMethod}(arguments)
     * @param name
     * @param args
     * @return
     */
    def methodMissing(String name, args) {
        assert name.startsWith('call')
        assert args //at least 1 argument

        def root = name[4..-1]
        def service = services.find { root.startsWith(it.key) }
        assert service
        def methodName = Introspector.decapitalize(root[service.key.length()..-1])

        def gridConfig = args[0]
        def serviceInstance = grailsApplication.mainContext.getBean(getNestedPropertyValue(service.value, gridConfig))
        if (serviceInstance.respondsTo(methodName)) {
            serviceInstance."${methodName}"(*args)
        }
    }
}
