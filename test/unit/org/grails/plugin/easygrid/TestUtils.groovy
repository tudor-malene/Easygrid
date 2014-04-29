package org.grails.plugin.easygrid

import grails.util.Environment
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletResponse
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpSession
import org.springframework.validation.ObjectError

import java.beans.Introspector

/**
 * utility methods used in unit tests
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class TestUtils {

    /**
     * generates a config from a grid closure
     * @param gridConfigClosure
     * @return
     */
    static generateConfigForGrid(grailsApplication, dataSourceService = null, Closure gridConfigClosure) {
        grailsApplication.config.merge(new ConfigSlurper(Environment.current.name).parse(this.classLoader.loadClass('DefaultEasygridConfig')))
        def service = new EasygridInitService()
        service.easygridDispatchService = new MockEasygridDispatchService(dataSourceService)
        service.grailsApplication = grailsApplication
        service.initializeFromClosure gridConfigClosure
    }


    static populateTestDomain(N = 100) {
        (1..N).each {
            new TestDomain(id: it, testStringProperty: "$it", testIntProperty: it).save(failOnError: true)
        }
        assert N == TestDomain.count()
    }


    static List mockEasyGridContextHolder() {
        def response = new GrailsMockHttpServletResponse()
        def request = new GrailsMockHttpServletRequest()
        def session = new GrailsMockHttpSession()
        def params = [:]
        EasygridContextHolder.metaClass.static.getResponse = { -> response }
        EasygridContextHolder.metaClass.static.getRequest = { -> request }
        EasygridContextHolder.metaClass.static.getSession = { -> session }
        EasygridContextHolder.metaClass.static.getParams = { -> params }
        EasygridContextHolder.metaClass.static.messageLabel = { String code -> code }
        EasygridContextHolder.metaClass.static.errorLabel = { ObjectError err -> err.defaultMessage }
        [params, request, response, session]
    }

}

//calls only the datasource
class MockEasygridDispatchService {

    def dataSourceService

    MockEasygridDispatchService(dataSourceService) {
        this.dataSourceService = dataSourceService
    }
    MockEasygridDispatchService() {
    }

    /**
     * calls methods of the form: call{GridService}{capitalizedMethod}(arguments)
     * @param name
     * @param args
     * @return
     */
    def methodMissing(String name, args) {
        if (!dataSourceService) {
            return
        }
        assert name.startsWith('call')
        assert args //at least 1 argument

        def root = name[4..-1]
        if (root.startsWith('DS')) {
            def methodName = Introspector.decapitalize(root[2..-1])
            if (dataSourceService.respondsTo(methodName)) {
                dataSourceService."${methodName}"(*args)
            }

        }
    }
}
