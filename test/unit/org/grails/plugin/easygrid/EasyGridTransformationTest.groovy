package org.grails.plugin.easygrid

/**
 * User: Tudor
 * Date: 06.10.2012
 * Time: 11:26
 * test class for the EasyGridASTTransformation transformation
 */
class EasyGridTransformationTest extends GroovyTestCase {


    public void testMethodsAdded() {

        def c = new GroovyClassLoader().parseClass('''
            package com.example
            import org.grails.plugin.easygrid.EasyGrid
            @EasyGrid
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

    }


}
