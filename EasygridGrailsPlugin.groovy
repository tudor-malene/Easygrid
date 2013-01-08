import groovy.text.SimpleTemplateEngine
import org.grails.plugin.easygrid.GridUtils
import org.grails.plugin.easygrid.EasygridContextHolder

class EasygridGrailsPlugin {

    def version = "1.0.9.2"

    def grailsVersion = "2.0 > *"

    def loadAfter = ['services', 'controllers']

    def pluginExcludes = [
            'grails-app/controllers/org/grails/plugin/easygrid/TestDomainController.groovy',
            'grails-app/domain/org/grails/plugin/easygrid/TestDomain.groovy',
            'grails-app/services/org/grails/plugin/easygrid/grids/TestGridService.groovy',
            'grails-app/views/templates/_testGridRenderer.gsp',
    ]

    def dependsOn = [
            'jquery-ui': "1.8.14 > *"
    ]

    def observe = ["controllers", "services"]

    def title = "Easygrid Plugin"
    def author = "Tudor Malene"
    def authorEmail = "tudor.malene@gmail.com"
    def description = 'Provides a convenient and agile way of defining Data Grids. And also a powerful selection widget.'
    def documentation = "https://github.com/tudor-malene/Easygrid"

    def license = "APACHE"
    def issueManagement = [system: "GITHUB", url: "https://github.com/tudor-malene/Easygrid/issues"]
    def scm = [url: "https://github.com/tudor-malene/Easygrid"]

    def doWithDynamicMethods = { ctx ->
        GridUtils.addMixins()
    }

    def onChange = { event ->
        GridUtils.addMixins()
        EasygridContextHolder.classReloaded()
    }

    def doWithApplicationContext = { appCtx ->
        appCtx.grailsApplication.config.easygrid.defaults.labelFormatTemplate =
            new SimpleTemplateEngine().createTemplate(
                    appCtx.grailsApplication.config.easygrid.defaults.labelFormat.toString().replace('#', '$')
            )
    }
}
