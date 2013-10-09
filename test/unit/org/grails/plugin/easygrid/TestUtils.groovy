package org.grails.plugin.easygrid

import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletResponse
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpSession

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
//        mockGridConfigMethods(action)
        def service = new EasygridInitService()
        service.easygridDispatchService = [
                invokeMethod: { String name, Object args ->
                    //if a datasource provided - dispatch correctly
                    if (dataSourceService && name.startsWith('callDS')) {
                        def disp = new EasygridDispatchService()
                        disp.metaClass.getDSService = { gridConfig -> dataSourceService }
                        disp."${name}"(* args)
                    }
                }
        ] as GroovyInterceptable
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
        EasygridContextHolder.metaClass.static.getResponse = {-> response }
        EasygridContextHolder.metaClass.static.getRequest = {-> request }
        EasygridContextHolder.metaClass.static.getSession = {-> session }
        EasygridContextHolder.metaClass.static.getParams = {-> params }
        EasygridContextHolder.metaClass.static.messageLabel = { code -> code }
        [params, request, response, session]
    }

}
