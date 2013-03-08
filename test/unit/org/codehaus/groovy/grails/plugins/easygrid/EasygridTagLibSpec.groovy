package org.codehaus.groovy.grails.plugins.easygrid

import grails.test.mixin.TestFor
import org.grails.plugin.easygrid.EasygridService
import org.grails.plugin.easygrid.GridConfig
import spock.lang.Specification

/**
 * tests the taglib
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@TestFor(EasygridTagLib)
class EasygridTagLibSpec extends Specification {

    def "test grid taglib"() {

        given:
        tagLib.easygridService = Mock(EasygridService)

        tagLib.easygridService.getGridConfig(_, _) >> new GridConfig(id: 'testGrid', gridRenderer: '/templates/testGridRenderer')
        tagLib.easygridService.overwriteGridProperties(_, _, _) >> { args -> args[0] }
        tagLib.easygridService.overwriteGridProperties(_,_) >> {args -> args[0]}
        tagLib.easygridService.htmlGridDefinition(_) >> { args -> [gridConfig:args[0] ]}

        expect:
        applyTemplate('<grid:grid name="testGrid"/> ').trim() == 'testGrid'
//        applyTemplate('<grid:exportButton name="testGrid"/> ')

    }
}
