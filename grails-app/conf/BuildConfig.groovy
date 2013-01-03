grails.project.work.dir = 'target'
grails.project.source.level = 1.6

grails.project.dependency.resolution = {

	inherits 'global'
	log 'warn'

	repositories {
		grailsCentral()
		mavenLocal()
		mavenCentral()
	}

	dependencies {
		compile 'com.google.visualization:visualization-datasource:1.1.1'
//        test "org.spockframework:spock-grails-support:0.7-groovy-2.0"
//        test "org.gebish:geb-spock:0.9.0-RC-1"
//        test("org.seleniumhq.selenium:selenium-htmlunit-driver:2.25.0") {
//            exclude "xml-apis"
//        }
//        test("org.seleniumhq.selenium:selenium-chrome-driver:2.25.0")
//        test("org.seleniumhq.selenium:selenium-firefox-driver:2.25.0")
//        build "net.sourceforge.nekohtml:nekohtml:1.9.17"
    }

	plugins {
		build(':release:2.2.0', ':rest-client-builder:1.0.2') {
			export = false
//            excludes 'nekohtml'
        }

		compile(":hibernate:$grailsVersion") {
			export = false
		}

		compile ':export:1.5'

        runtime ":resources:1.2.RC2"
//        runtime ":resources:1.1"
		runtime ':jquery:1.8.0'
		runtime ':jquery-ui:1.8.24'
		runtime ':google-visualization:0.5.6'
//        test(":spock:0.7") {
//            exclude "spock-grails-support"
//        }
//        test "org.grails.plugins:geb:0.9.0-RC-1"
	}
}
