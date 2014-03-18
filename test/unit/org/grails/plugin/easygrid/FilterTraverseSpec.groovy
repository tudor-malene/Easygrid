package org.grails.plugin.easygrid

import spock.lang.Specification

import static org.grails.plugin.easygrid.FiltersEnum.and
import static org.grails.plugin.easygrid.FiltersEnum.or

/**
 * Created by Tudor on 16.03.2014.
 */
class FilterTraverseSpec extends Specification {

    def "traverse filters"() {
        given:
        def filters = new Filters(
                type: and,
                filters: [
                        "filter1", "filter2", new Filters(
                        type: or,
                        filters: ['filters31', 'filters32', new Filters(
                                type: and,
                                filters: ['filters331', 'filters332']
                        )]
                ), "filter4",
                ])

        when:

        def seq = filters.postorder(
                { node, siblings -> [node.type] + siblings },
                { leaf -> leaf }
        )

        then:
        [and, [or, [and, 'filters331', 'filters332'], 'filters31', 'filters32'], 'filter1', 'filter2', 'filter4'] == seq

    }
}
