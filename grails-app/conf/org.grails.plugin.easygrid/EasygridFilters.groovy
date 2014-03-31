package org.grails.plugin.easygrid

import static org.grails.plugin.easygrid.GridUtils.isControllerEasygridEnabled

class EasygridFilters {

    def filters = {
        easygridDisableCallingGridClosures(controller: '*', action: '*Grid') {
            before = {
                !isControllerEasygridEnabled(grailsApplication, controllerName)
            }
            after = { Map model ->

            }
            afterView = { Exception e ->
//                if (!e && controllerName && grailsApplication.getArtefactByLogicalPropertyName(ControllerArtefactHandler.TYPE, controllerName)?.clazz?.isAnnotationPresent(Easygrid)) {
//                    resetParams()
//                }
            }
        }
    }
}
