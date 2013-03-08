package org.grails.plugin.easygrid

import grails.plugin.spock.IntegrationSpec
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.grails.plugin.easygrid.builder.EasygridBuilder
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.context.request.RequestContextHolder
import spock.lang.Shared
import javax.servlet.http.HttpServletResponse

/**
 * base class for integration tests
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
abstract class AbstractBaseTest extends IntegrationSpec{

    static transactional = true

    def easygridService
    @Shared def grailsApplication

    def params
    HttpServletResponse response
    MockHttpServletRequest request

    @Shared def defaultValues

    def setupSpec() {
        GridUtils.addMixins()

        assert grailsApplication?.domainClasses?.size() >= 1
        defaultValues = grailsApplication.config?.easygrid

        if (this.respondsTo('initGrids')){
            this.initGrids()
        }

    }

    def setup(){
        EasygridContextHolder.session.setAttribute('listData', (1..200).collect { [col1: it, col2: "$it"] })
        params = new GrailsParameterMap(new MockHttpServletRequest())
        response = new MockHttpServletResponse()
        RequestContextHolder.currentRequestAttributes().params = params
        RequestContextHolder.currentRequestAttributes().response = response
//        RequestContextHolder.currentRequestAttributes().request = request
        request = RequestContextHolder.currentRequestAttributes().request
        EasygridContextHolder.resetParams()
    }

    /**
     * generates a config from a grid closure
     * @param gridConfigClosure
     * @return
     */
    def generateConfigForGrid(Closure gridConfigClosure) {
        new EasygridBuilder(grailsApplication).evaluateGrid gridConfigClosure
    }


    private populateTestDomain(N = 100) {
//        def N = 100
        (1..N).each {
            new TestDomain(id: it, testStringProperty: "$it", testIntProperty: it).save(failOnError: true)
        }
        assert N == TestDomain.count()
    }
}
