package org.grails.plugin.easygrid.grids

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty

class TestGridService {

    static transactional = false

    def easygridService
    def grailsApplication

    def dynamicCalls = [:]

    /**
     * called during the dynamic generation phase  for each column
     * @param gridConfig
     * @param prop
     * @param column
     */
    def dynamicProperties(GrailsDomainClassProperty prop, column) {
        dynamicCalls[prop] = column
    }

    def addDefaultValues(Map defaultValues) {
        println 'addDefaultValues'
    }

    def filters() {
        println 'filters'
    }

    def listParams() {
        def result = [:]
        result
    }

    def transform(rows, nrRecords, listParams) {
        rows
    }
}
