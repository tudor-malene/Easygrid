package org.grails.plugin.easygrid

import groovy.util.logging.Slf4j
import org.codehaus.groovy.grails.plugins.web.taglib.ValidationTagLib
import org.springframework.core.NamedThreadLocal
import org.springframework.web.context.request.RequestContextHolder

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

/**
 * utility class
 * - is used by services to access http parameters, session, etc
 * - stores the parameters
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
class EasygridContextHolder {

    // in case we need to work with an old set of parameters ( ex: exporting data already filtered , or returning from an add/update page )
    private static final ThreadLocal restoredParamsHolder = new NamedThreadLocal("restoredParamsHolder")

    static storeParams(params) {
        restoredParamsHolder.set(params)
    }

    static resetParams() {
        restoredParamsHolder.remove()
    }

    static getParams() {
        def params = restoredParamsHolder.get()
        (params != null) ? params : RequestContextHolder.currentRequestAttributes().params
    }

    static HttpServletRequest getRequest() {
        RequestContextHolder.currentRequestAttributes().request
    }

    static HttpServletResponse getResponse() {
        RequestContextHolder.currentRequestAttributes().currentResponse
    }

    static HttpSession getSession() {
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
    static messageLabel(String code) {
        new ValidationTagLib().message(code: code, default: code)
    }

    static errorLabel(error) {
        new ValidationTagLib().message([error: error])
    }


}
