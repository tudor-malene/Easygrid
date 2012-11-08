package org.grails.plugin.easygrid.builder

/**
 * builder for the autocomplete section
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class AutocompleteDelegate {

    //injected
    def grailsApplication

    def gridConfig
    def autocomplete

    def textBoxFilterClosure(implClosure) {
        autocomplete.textBoxFilterClosure = implClosure
    }

    def labelValue(implClosure) {
        autocomplete.labelValue = implClosure
    }

    def constraintsFilterClosure(implClosure) {
        autocomplete.constraintsFilterClosure = implClosure
    }

    def methodMissing(String property, value) {
        autocomplete[property] = value[0]
    }

}
