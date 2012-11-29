package org.grails.plugin.easygrid

/**
 * test class for the EasyGridASTTransformation transformation
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class EasygridTransformationTest extends GroovyTestCase {

    void testMethodsAdded() {

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
}
