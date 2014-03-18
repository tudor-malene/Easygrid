package org.grails.plugin.easygrid

import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.grails.plugin.easygrid.grids.JqGridMultiSearchService
import spock.lang.Specification

import static org.grails.plugin.easygrid.FiltersEnum.and
import static org.grails.plugin.easygrid.TestUtils.generateConfigForGrid

/**
 * parse jqgrid filter tests
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@TestFor(JqGridMultiSearchService)
@Mock(FilterEntity)
class JqGridMultiSearchServiceSpec extends Specification {

    def "complex search "() {
        given:
        service.filterService = new FilterService()

        when:
        def filtersGridConfig = generateConfigForGrid(grailsApplication, service) {
            'filtersGridConfig' {
                dataSourceType 'gorm'
                domainClass FilterEntity
                columns {
                    id
                    filterFlag
                }
            }
        }.filtersGridConfig


        and:
        String filters = '''
{   "groupOp":"AND",
     "rules":    [{"field":"filterFlag","op":"eq","data":"xxx"},{"field":"filterFlag","op":"cn","data":"xxx"},{"field":"filterFlag","op":"eq","data":"xxx"}],
     "groups":   [{   "groupOp":"AND",
     "rules":    [{"field":"filterFlag","op":"ne","data":"xx"}],
     "groups":   [{   "groupOp":"OR",
     "rules":    [{"field":"filterFlag","op":"bw","data":"xx"}],
     "groups":   []}]}]}'''
        Filters result = service.multiSearchToCriteriaClosure(filtersGridConfig, filters)

        then:
        and == result.type
        4 == result.filters.size()

    }

}

@Entity
class FilterEntity {
    String filterFlag
}