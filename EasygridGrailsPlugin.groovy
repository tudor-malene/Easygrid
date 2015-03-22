import grails.util.Environment
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.grails.plugin.easygrid.EasygridInitService
import org.grails.plugin.easygrid.JsUtils

class EasygridGrailsPlugin {

    def version = "1.7.1"

    def grailsVersion = "2.2 > *"

    def loadAfter = ['services', 'controllers']

    def pluginExcludes = [
            'grails-app/controllers/org/grails/plugin/easygrid/TestDomainController.groovy',
            'grails-app/domain/org/grails/plugin/easygrid/TestDomain.groovy',
            'grails-app/domain/org/grails/plugin/easygrid/OwnerTest.groovy',
            'grails-app/domain/org/grails/plugin/easygrid/PetTest.groovy',
            'grails-app/services/org/grails/plugin/easygrid/grids/TestGridService.groovy',
            'grails-app/views/templates/easygrid/_testGridRenderer.gsp',
    ]

//    def dependsOn = [
//            'jquery-ui': "1.8.14 > *"
//    ]

    //the location of external grids config - to enable reloading
    def watchedResources = "file:./src/groovy/grids/**/*.groovy"

    def observe = ["controllers", "services"]

    def title = "Easygrid Plugin"
    def author = "Tudor Malene"
    def authorEmail = "tudor.malene@gmail.com"
    def description = '''
        Provides a declarative way of defining Data Grids.
        It works currently with jqGrid, google visualization and jquery dataTables.
        Out of the box it provides sorting, filtering, exporting and inline edit just by declaring a grid in a controller and adding a tag to your gsp.
        It also provides a powerful selection widget ( a direct replacement for drop-boxes )
        '''

    def documentation = "https://github.com/tudor-malene/Easygrid"

    def license = "APACHE"
    def issueManagement = [system: "GITHUB", url: "https://github.com/tudor-malene/Easygrid/issues"]
    def scm = [url: "https://github.com/tudor-malene/Easygrid"]

    def doWithDynamicMethods = { ctx ->
    }

    def doWithSpring = {
        loadEasygridConfig(application)
    }

    def onChange = { event ->
        event.ctx.getBean(EasygridInitService).initializeGrids()
    }

    def doWithApplicationContext = { appCtx ->
        JsUtils.registerMarshallers()
        appCtx.getBean(EasygridInitService).initializeGrids()
    }

    private ConfigObject loadEasygridConfig(GrailsApplication grailsApplication) {
        def config = grailsApplication.config
        GroovyClassLoader classLoader = new GroovyClassLoader(getClass().classLoader)

        // Merging default Easygrid config into main application config
        config.merge(new ConfigSlurper(Environment.current.name).parse(classLoader.loadClass('DefaultEasygridConfig')))

        // Merging user-defined Easygrid config into main application config if provided
        try {
            config.merge(new ConfigSlurper(Environment.current.name).parse(classLoader.loadClass('EasygridConfig')))
        } catch (any) {
            println 'Could not process the EasygridConfig file '
            // ignore, just use the defaults
        }

        return config
    }

}
