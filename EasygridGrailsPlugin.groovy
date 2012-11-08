import org.grails.plugin.easygrid.builder.ColumnDelegate
import org.grails.plugin.easygrid.builder.ColumnsDelegate
import org.grails.plugin.easygrid.builder.GridDelegate
import org.grails.plugin.easygrid.builder.GridsDelegate
import org.grails.plugin.easygrid.GridUtils
import org.grails.plugin.easygrid.EasygridContextHolder
import org.grails.plugin.easygrid.builder.AutocompleteDelegate

class EasygridGrailsPlugin {

    def version = "0.9.9"

//    static JQGRID_VERSION = "4.4.0"
//    static DATATABLES_VERSION = "1.9.3"

    def grailsVersion = "2.0 > *"

    def loadAfter = ['services', 'controllers']

    def pluginExcludes = [
            'grails-app/controllers/org/grails/plugin/easygrid/TestDomainController.groovy',
            'grails-app/domain/org/grails/plugin/easygrid/TestDomain.groovy',
            'grails-app/services/org/grails/plugin/easygrid/grids/TestGridService.groovy',
            'grails-app/views/templates/_testGridRenderer.gsp',
    ]

    def observe = ["controllers"]

    def title = "Easygrid Plugin"
    def author = "Tudor Malene"
    def authorEmail = "tudor.malene@gmail.com"
    def description = 'Provides a convenient and agile way of defining Data Grids.'
    def documentation = "http://grails.org/plugin/easygrid"

//    def license = "APACHE"
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]
    def scm = [ url: "https://github.com/tudor-malene/Easygrid" ]

    def doWithSpring = {
        columnDelegate(ColumnDelegate) {
            grailsApplication = ref('grailsApplication')
            it.scope = 'prototype'
        }
        columnsDelegate(ColumnsDelegate) {
            columnDelegate = ref('columnDelegate')
            grailsApplication = ref('grailsApplication')
            it.scope = 'prototype'
        }
        autocompleteDelegate(AutocompleteDelegate) {
            grailsApplication = ref('grailsApplication')
            it.scope = 'prototype'
        }
        gridDelegate(GridDelegate) {
            grailsApplication = ref('grailsApplication')
            columnsDelegate = ref('columnsDelegate')
            autocompleteDelegate = ref('autocompleteDelegate')
            it.scope = 'prototype'
        }
        gridsDelegate(GridsDelegate) {
            gridDelegate = ref('gridDelegate')
            it.scope = 'prototype'
        }
    }

    def doWithDynamicMethods = { ctx ->
        GridUtils.addMixins()
    }

    def onChange = { event ->
        GridUtils.addMixins()
        EasygridContextHolder.classReloaded()
    }
}
