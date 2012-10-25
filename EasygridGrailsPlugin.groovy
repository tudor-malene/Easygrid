import org.grails.plugin.easygrid.builder.ColumnDelegate
import org.grails.plugin.easygrid.builder.ColumnsDelegate
import org.grails.plugin.easygrid.builder.GridDelegate
import org.grails.plugin.easygrid.builder.GridsDelegate
import org.grails.plugin.easygrid.GridUtils
import org.grails.plugin.easygrid.EasygridContextHolder

class EasygridGrailsPlugin {
    // the plugin version
    def version = "0.9.9"

    static JQGRID_VERSION = "4.4.0"
    static DATATABLES_VERSION = "1.9.3"

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.0 > *"
    // the other plugins this plugin depends on
    def dependsOn = ['jquery-ui': "1.8.14 > *", 'export': "1.3 > *", 'google-visualization': "0.5.2 > *"]

    def loadAfter = ['services', 'controllers']

//    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp",
            'grails-app/controllers/org/grails/plugin/easygrid/TestDomainController.groovy',
            'grails-app/domain/org/grails/plugin/easygrid/TestDomain.groovy',
            'grails-app/services/org/grails/plugin/easygrid/grids/TestGridService.groovy',
            'grails-app/views/templates/_testGridRenderer.gsp',
    ]

    def watchedResources = ["file:./grails-app/controllers/**/*Controller.groovy","file:./grails-app/services/**/*Service.groovy",]

    def observe = ["controllers"]

    def title = "Easygrid Plugin"
    def author = "Tudor Malene"
    def authorEmail = "tudor.malene@gmail.com"
    def description = '''\
        EasyGrid provides a convenient and agile way of defining Data Grids.
    '''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/easygrid"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
//    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.grails-plugins.codehaus.org/browse/grails-plugins/" ]

    def doWithWebDescriptor = { xml ->
    }

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
        gridDelegate(GridDelegate) {
            grailsApplication = ref('grailsApplication')
            columnsDelegate = ref('columnsDelegate')
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

    def doWithApplicationContext = { applicationContext ->
    }

    def onChange = { event ->
        GridUtils.addMixins()
        EasygridContextHolder.classReloaded()
    }

    def onConfigChange = { event ->
    }

    def onShutdown = { event ->
    }
}
