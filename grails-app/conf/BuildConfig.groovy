grails.project.work.dir = 'target'
grails.project.source.level = 1.6

grails.project.dependency.resolver = "maven" // or ivy

grails.project.dependency.resolution = {

    inherits 'global'
    log 'warn'

    repositories {
        grailsCentral()
//		mavenLocal()
        mavenCentral()
    }


    dependencies {
        compile('com.google.visualization:visualization-datasource:1.1.1') {
            exclude(group: 'commons-logging', name: 'commons-logging')
            exclude(group: 'commons-lang', name: 'commons-lang')
        }
    }

    plugins {
        compile ":jquery-ui:1.10.3"
        runtime ":jquery:1.10.2.2"

        runtime (":resources:1.2.1"){
            export = false
        }

        compile ':export:1.5'

        runtime ':google-visualization:0.6.2'

        compile (":scaffolding:2.0.1"){
            export = false
        }
//        compile ":filterpane:2.3.0"
//        compile ":plugin-config:0.1.8"

        build(":release:3.0.1") {
            export = false
        }
        runtime(':hibernate:3.6.10.10') {
            export = false
        }
    }
}
