package org.grails.plugin.easygrid

import static org.junit.Assert.*

/**
 * test class for the EasyGridASTTransformation transformation
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class EasygridTransformationTest extends GroovyTestCase {

    void testEasygridControllerTransformation() {

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

        assert instance.hasProperty('easygridService')

        assert instance.hasProperty('gridsConfig')

        assert instance.respondsTo('testGridRows')
        assert instance.respondsTo('visGridRows')

        assert instance.respondsTo('testGridExport')
        assert instance.respondsTo('visGridExport')

        assert instance.respondsTo('testGridInlineEdit')
        assert instance.respondsTo('visGridInlineEdit')

        assert instance.respondsTo('testGridAutocompleteResult')
        assert instance.respondsTo('visGridAutocompleteResult')

        assert instance.respondsTo('testGridHtml')
        assert instance.respondsTo('visGridHtml')
    }

    /**
     * test the transformation with external grids
     */
    void testExternalEasygridControllerTransformation() {

        new GroovyClassLoader().parseClass('''
            package com.example
            import org.grails.plugin.easygrid.Easygrid

            import org.grails.plugin.easygrid.*

            class GridsConfig{
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

            @Easygrid(externalGrids = com.example.GridsConfig)
            class TestController {

            }

        ''')

        def instance = gcl.loadedClasses.find{it.name =='com.example.TestController'}.newInstance()

        assert instance.hasProperty('easygridService')

        assert instance.hasProperty('gridsConfig')

        assert instance.respondsTo('testGridRows')
        assert instance.respondsTo('visGridRows')

        assert instance.respondsTo('testGridExport')
        assert instance.respondsTo('visGridExport')

        assert instance.respondsTo('testGridInlineEdit')
        assert instance.respondsTo('visGridInlineEdit')

        assert instance.respondsTo('testGridAutocompleteResult')
        assert instance.respondsTo('visGridAutocompleteResult')

        assert instance.respondsTo('testGridHtml')
        assert instance.respondsTo('visGridHtml')
    }

    void testGridCOnfig(){
        GridConfig config = new GridConfig()
        config.blabla = 1
        assertEquals 1, config.blabla

        ColumnConfig cc = new ColumnConfig()
        cc.blabla = 1
        assertEquals 1, cc.blabla
    }


    void testDynamicConfigAnnotation() {

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

        //static
        instance.prop1 = '1'
        assertEquals '1', instance.prop1

        //dynamic
        instance.prop2 = '2'
        assertEquals '2', instance.prop2

    }
}
