package org.grails.plugin.easygrid

import org.springframework.core.NamedThreadLocal
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.plugins.web.taglib.ValidationTagLib

/**
 * utility class
 * - stores the current GridConfig to ThreadLocal
 * - is used by services to access http parameters, session, etc
 * - stores the parameters
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class EasygridContextHolder {

    // used to store the gridConfig between method calls
    private static final ThreadLocal<Grid> gridConfigHolder = new NamedThreadLocal<Grid>("gridConfigHolder");

    // in case we need to work with an old set of parameters ( ex: exporting data already filtered , or returning from an add/update page )
    private static final ThreadLocal restoredParamsHolder = new NamedThreadLocal("restoredParamsHolder");

    static Grid getGridConfig() {
        gridConfigHolder.get()
    }

    def static setLocalGridConfig(config) {
        gridConfigHolder.set(config)
    }

    def static  storeParams(params) {
        restoredParamsHolder.set(params)
    }

    def static resetParams() {
        restoredParamsHolder.remove()
    }

    static def getParams() {
        def params = restoredParamsHolder.get()
        params ? params : RequestContextHolder.currentRequestAttributes().params
    }

    static def getRequest() {
        RequestContextHolder.currentRequestAttributes().request
    }

    static def getResponse() {
        RequestContextHolder.currentRequestAttributes().currentResponse
    }

    static def getSession() {
        RequestContextHolder.currentRequestAttributes().session
    }

    static def getFlashScope() {
        RequestContextHolder.currentRequestAttributes().flashScope
    }

    /**
     * utility class for i18n
     * @param code
     * @return
     */
    static def message(code){
        new ValidationTagLib().message(code: code)
    }

    // used for development
    private static reloadGrids = true

    synchronized def reloadGrids() {
        def old = reloadGrids
        reloadGrids = false
        old
    }

    synchronized static def classReloaded() {
        reloadGrids = true
    }

}
