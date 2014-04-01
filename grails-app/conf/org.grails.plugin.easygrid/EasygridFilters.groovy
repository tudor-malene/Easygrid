package org.grails.plugin.easygrid

import static org.grails.plugin.easygrid.EasygridContextHolder.resetParams
import static org.grails.plugin.easygrid.GridUtils.isControllerEasygridEnabled

class EasygridFilters {

    def filters = {
        easygridDisableCallingGridClosures(controller: '*', action: '*Grid') {
            before = {
                !isControllerEasygridEnabled(grailsApplication, controllerName)
            }
        }
        resetParamsAfterExport(controller: '*', action: '*Export') {
            afterView = { Exception e ->
                if (isControllerEasygridEnabled(grailsApplication, controllerName)) {
                    resetParams()
                }
            }
        }
    }
}
