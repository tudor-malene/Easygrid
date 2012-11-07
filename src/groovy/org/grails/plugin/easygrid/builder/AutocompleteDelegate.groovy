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

    def textBoxSearchClosure(implClosure) {
        autocomplete.textBoxSearchClosure = implClosure
    }

    def labelValue(implClosure) {
        autocomplete.labelValue = implClosure
    }

    def constraintsSearchClosure(implClosure) {
        autocomplete.constraintsSearchClosure = implClosure
    }

    def methodMissing(String property, value) {
        autocomplete[property] = value[0]
    }

}
