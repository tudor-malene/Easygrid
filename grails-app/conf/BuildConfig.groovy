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
	}

	plugins {
		build(':release:2.0.4', ':rest-client-builder:1.0.2') {
			export = false
		}

		compile(":hibernate:$grailsVersion") {
			export = false
		}

		compile ':export:1.5'

		runtime ':jquery:1.8.0'
		runtime ':jquery-ui:1.8.24'
		runtime ':google-visualization:0.5.6'
	}
}
