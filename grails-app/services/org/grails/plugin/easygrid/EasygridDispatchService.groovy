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

    /**
     * calls methods of the form: call{GridService}{capitalizedMethod}(arguments)
     * @param name
     * @param args
     * @return
     */
    def methodMissing(String name, args) {
        assert args //at least 1 argument
        def gridConfig = args[0]

        def (service, methodName) = retreiveServiceAndMethod(name)
        def serviceInstance = grailsApplication.mainContext.getBean(getNestedPropertyValue(service, gridConfig))
        assert serviceInstance

        if (serviceInstance.respondsTo(methodName)) {
            serviceInstance."${methodName}"(*args)
        } else {
            log.warn("No service method for: ${name}")
        }
    }

    //todo - add the mandatory methods & throw exception if not implemented
    static def services = [
            GridImpl: 'gridImplService',
            DS      : 'dataSourceService',
            AC      : 'autocomplete.autocompleteService',
            Export  : 'export.exportService',
            FF      : 'filterForm.filterFormService'
    ]

    //optimization - parse the method call
    static def retreiveServiceAndMethod = { String name ->
        assert name.startsWith('call')

        def root = name[4..-1]
        def service = services.find { root.startsWith(it.key) }
        assert service
        def methodName = Introspector.decapitalize(root[service.key.length()..-1])
        [service.value, methodName]
    }.memoize()

}
