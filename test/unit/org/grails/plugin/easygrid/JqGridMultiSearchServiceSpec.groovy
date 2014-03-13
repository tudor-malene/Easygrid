package org.grails.plugin.easygrid

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import groovy.json.JsonSlurper
import org.grails.plugin.easygrid.grids.JqGridMultiSearchService
import spock.lang.Specification

/**
 * jqgrid impl tests
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@TestFor(JqGridMultiSearchService)
@Mock(TestDomain)
class JqGridMultiSearchServiceSpec extends Specification {

    def "complex search "() {
        when:
        String filters = '''
{   "groupOp":"AND",
     "rules":    [{"field":"filterFlag","op":"eq","data":"xxx"},{"field":"filterFlag","op":"cn","data":"xxx"},{"field":"filterFlag","op":"eq","data":"xxx"}],
     "groups":   [{   "groupOp":"AND",
     "rules":    [{"field":"filterFlag","op":"ne","data":"xx"}],
     "groups":   [{   "groupOp":"OR",
     "rules":    [{"field":"filterFlag","op":"bw","data":"xx"}],
     "groups":   []}]}]}'''
        String resultClosure = service.translate(new JsonSlurper().parseText(filters) as Map)

        then:
        '''{ params ->
\tand {
\t\teq('filterFlag','xxx')
\t\tilike('filterFlag','%xxx%')
\t\teq('filterFlag','xxx')
\t\tand {
\t\t\tne('filterFlag','xx')
\t\t\tor  {
\t\t\t\tilike('filterFlag','xx%')
\t\t\t}
\t\t}
\t}
}''' == resultClosure

    }


}
