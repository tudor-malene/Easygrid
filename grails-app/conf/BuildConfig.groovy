grails.project.work.dir = 'target'
grails.project.source.level = 1.6

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
            exclude (group: 'commons-logging', name: 'commons-logging')
            exclude (group: 'commons-lang', name: 'commons-lang')
        }
        compile('org.mvel:mvel2:2.1.3.Final')
        test "org.spockframework:spock-grails-support:0.7-groovy-2.0"
    }

    plugins {
        build(':release:2.2.0', ':rest-client-builder:1.0.2') {
            export = false
        }

        compile(":hibernate:$grailsVersion") {
            export = false
        }

        compile(':export:1.5')

        runtime(':jquery:1.8.0')
        runtime(':jquery-ui:1.8.24')
        runtime(':google-visualization:0.5.6')
//        runtime ":angularjs-resources:1.0.8"

        //only for 2.2.0
        runtime ":resources:1.2"
        compile ":dynamic-controller:0.4"

        test(":spock:0.7") {
            exclude "spock-grails-support"
        }
    }
}
