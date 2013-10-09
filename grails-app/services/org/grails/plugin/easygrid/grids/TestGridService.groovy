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
    def dynamicProperties(gridConfig, column) {
        dynamicCalls[gridConfig] = column
    }

    def addDefaultValues(gridConfig, Map defaultValues) {
        println 'addDefaultValues'
    }

    def filters(gridConfig) {
        println 'filters'
    }

    def listParams(gridConfig) {
        def result = [:]
        result
    }

    def transform(gridConfig, rows, nrRecords, listParams) {
        rows
    }
}
