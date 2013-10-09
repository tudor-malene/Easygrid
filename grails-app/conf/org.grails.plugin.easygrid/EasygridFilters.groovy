package org.grails.plugin.easygrid

import org.codehaus.groovy.grails.commons.ControllerArtefactHandler

class EasygridFilters {

    def filters = {
        all(controller: '*', action: '*') {
            before = {

            }
            after = { Map model ->

            }
            afterView = { Exception e ->
                if (!e && controllerName && grailsApplication.getArtefactByLogicalPropertyName(ControllerArtefactHandler.TYPE, controllerName)?.clazz?.isAnnotationPresent(Easygrid)) {
                    org.grails.plugin.easygrid.EasygridContextHolder.resetParams()
                }
            }
        }
    }
}
