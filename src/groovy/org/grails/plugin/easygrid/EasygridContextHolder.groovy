package org.grails.plugin.easygrid

import org.codehaus.groovy.grails.plugins.web.taglib.ValidationTagLib
import org.springframework.core.NamedThreadLocal
import org.springframework.web.context.request.RequestContextHolder

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
    private static final ThreadLocal<Grid> gridConfigHolder = new NamedThreadLocal<Grid>("gridConfigHolder")

    // in case we need to work with an old set of parameters ( ex: exporting data already filtered , or returning from an add/update page )
    private static final ThreadLocal restoredParamsHolder = new NamedThreadLocal("restoredParamsHolder")

    static Grid getGridConfig() {
        gridConfigHolder.get()
    }

    static setLocalGridConfig(config) {
        gridConfigHolder.set(config)
    }

    static  storeParams(params) {
        restoredParamsHolder.set(params)
    }

    static resetParams() {
        restoredParamsHolder.remove()
    }

    static getParams() {
        def params = restoredParamsHolder.get()
        params ? params : RequestContextHolder.currentRequestAttributes().params
    }

    static getRequest() {
        RequestContextHolder.currentRequestAttributes().request
    }

    static getResponse() {
        RequestContextHolder.currentRequestAttributes().currentResponse
    }

    static getSession() {
        RequestContextHolder.currentRequestAttributes().session
    }

    static getFlashScope() {
        RequestContextHolder.currentRequestAttributes().flashScope
    }

    /**
     * utility class for i18n
     * @param code
     * @return
     */
    static message(code){
        new ValidationTagLib().message(code: code)
    }

    // used for development
    private static reloadGrids = true

    synchronized reloadGrids() {
        def old = reloadGrids
        reloadGrids = false
        old
    }

    synchronized static classReloaded() {
        reloadGrids = true
    }
}
