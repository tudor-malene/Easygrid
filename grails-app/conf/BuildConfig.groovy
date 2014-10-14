grails.project.work.dir = 'target'
grails.project.source.level = 1.6

grails.project.dependency.resolver = "maven" // or ivy

grails.project.dependency.resolution = {

    inherits 'global'
    log 'warn'

    repositories {
        grailsCentral()
        mavenCentral()
        mavenRepo "http://repo.grails.org/grails/core"
    }

    dependencies {
        compile('com.google.visualization:visualization-datasource:1.1.1') {
            exclude(group: 'commons-logging', name: 'commons-logging')
            exclude(group: 'commons-lang', name: 'commons-lang')
        }
        compile 'commons-beanutils:commons-beanutils:1.9.2'
        compile 'com.esotericsoftware.kryo:kryo:2.24.0'
        test 'cglib:cglib-nodep:3.1'
    }


    plugins {

        //plugins used for the actual functionality
        runtime ":jquery:1.11.1"
        compile ':export:1.6'
        runtime ':google-visualization:0.7'

        //local plugins
//        runtime(":resources:1.2.8") { export = false }
        compile(":scaffolding:2.1.2") { export = false }
        build(":release:3.0.1") { export = false }
        compile(':hibernate4:4.3.5.4') { export = false }
//        compile(":asset-pipeline:1.9.9") { export = false }
    }
}
