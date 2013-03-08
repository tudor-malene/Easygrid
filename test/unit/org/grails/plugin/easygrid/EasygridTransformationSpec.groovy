package org.grails.plugin.easygrid

import spock.lang.Specification


/**
 * test class for the EasyGridASTTransformation transformation
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class EasygridTransformationSpec extends Specification {

    def "test Easygrid Controller Transformation"() {

        given:
        def c = new GroovyClassLoader().parseClass('''
            package com.example
            import org.grails.plugin.easygrid.Easygrid

            @Easygrid
            class TestController {

                static grids = {
                    'testGrid'  {
                        type 'domain'
                        domainClass TestDomain
                        gridRenderer '/templates/testGridRenderer'
                        jqgrid{
                            width 300
                            height 150
                        }
                    }

                    'visGrid'  {
                        type 'domain'
                        domainClass TestDomain
                        gridImpl 'visualization'
                    }
                }
            }
        ''')

        def instance = c.newInstance()

        expect:
        instance.hasProperty('easygridService')

        instance.hasProperty('gridsConfig')

        instance.respondsTo('testGridRows')
        instance.respondsTo('visGridRows')

        instance.respondsTo('testGridExport')
        instance.respondsTo('visGridExport')

        instance.respondsTo('testGridInlineEdit')
        instance.respondsTo('visGridInlineEdit')

        instance.respondsTo('testGridAutocompleteResult')
        instance.respondsTo('visGridAutocompleteResult')

        instance.respondsTo('testGridHtml')
        instance.respondsTo('visGridHtml')
    }

    def "test GridConfig"(){
        given:
        GridConfig config = new GridConfig()

        when:
        config.blabla = 1
        then:
        1== config.blabla

        when:
        ColumnConfig cc = new ColumnConfig()
        cc.blabla = 1
        then:
        1== cc.blabla
    }


    def "test Dynamic Config Annotation"() {

        given:
        def c = new GroovyClassLoader().parseClass('''
            package com.example
            import org.grails.plugin.easygrid.ast.DynamicConfig
            import groovy.transform.AutoClone

            @DynamicConfig
            @AutoClone
            class TestConfig {
                String prop1
            }
        ''')

        def instance = c.newInstance()

        when: "static"
        instance.prop1 = '1'
        then:
        '1' == instance.prop1

        when: "dynamic"
        instance.prop2 = '2'
        then:
        '2' == instance.prop2
    }
}
