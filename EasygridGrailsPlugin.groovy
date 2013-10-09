import groovy.text.SimpleTemplateEngine
import org.grails.plugin.easygrid.EasygridInitService
import org.grails.plugin.easygrid.GridConfig

class EasygridGrailsPlugin {

    def version = "1.3.0"

    def grailsVersion = "2.0 > *"

    def loadAfter = ['services', 'controllers']

    def pluginExcludes = [
            'grails-app/controllers/org/grails/plugin/easygrid/TestDomainController.groovy',
            'grails-app/domain/org/grails/plugin/easygrid/TestDomain.groovy',
            'grails-app/domain/org/grails/plugin/easygrid/OwnerTest.groovy',
            'grails-app/domain/org/grails/plugin/easygrid/PetTest.groovy',
            'grails-app/services/org/grails/plugin/easygrid/grids/TestGridService.groovy',
            'grails-app/views/templates/_testGridRenderer.gsp',
    ]

//    def dependsOn = [
//            'jquery-ui': "1.8.14 > *"
//    ]

    def observe = ["controllers", "services"]

    def title = "Easygrid Plugin"
    def author = "Tudor Malene"
    def authorEmail = "tudor.malene@gmail.com"
    def description = '''
        This plugin provides a convenient and agile way of defining Data Grids.
        It also provides a powerful selection widget ( a direct replacement for drop-boxes )
        '''

    def documentation = "https://github.com/tudor-malene/Easygrid"

    def license = "APACHE"
    def issueManagement = [system: "GITHUB", url: "https://github.com/tudor-malene/Easygrid/issues"]
    def scm = [url: "https://github.com/tudor-malene/Easygrid"]

    def doWithDynamicMethods = { ctx ->
    }

    def onChange = { event ->
        event.ctx.getBean(EasygridInitService).initializeGrids()
    }

    def doWithApplicationContext = { appCtx ->
        appCtx.getBean(EasygridInitService).initializeGrids()
    }
}
