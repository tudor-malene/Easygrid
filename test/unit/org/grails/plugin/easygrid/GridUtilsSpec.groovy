package org.grails.plugin.easygrid

import org.codehaus.groovy.control.ConfigurationException
import spock.lang.Specification

/**
 * tests for util methods
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class GridUtilsSpec extends Specification{

    def "test Copy Properties"() {

        when:
        def to = [a: 1]
        GridUtils.copyProperties([a: 2, b: 2], to)

        then:
        2 == to.size()
        1 == to.a
        2 == to.b


        when:
        to = [a: 1]
        GridUtils.copyProperties([a: 2, b: [ba: 1, bb: 2]], to)

        then:
        2 == to.size()
        1 == to.a
        2 == to.b.size()


        when:
        to = [a: 1, b: [ba: 2, bc: 3]]
        GridUtils.copyProperties([a: 2, b: [ba: 1, bb: 2]], to)

        then:
        2 == to.size()
        1 == to.a
        3 == to.b.size()
        2 == to.b.ba


        when:
        to = [a: 1, b: 2]
        GridUtils.copyProperties([a: 2, b: [ba: 1, bb: 2]], to)

        then:
        thrown(ConfigurationException)


        when:
        to = [a: 1, b: [ba: 2, bc: 3]]
        GridUtils.copyProperties([a: 2, b: [ba: 1, bb: 2], c: 3, d: [da: 1]], to, 1)

        then:
        3 == to.size()
        1 == to.a
        2 == to.b.size()
        2 == to.b.ba
        to.d == null


        when:
        to = [a: 1, b: [ba: 2, bc: 3]]
        GridUtils.copyProperties([a: 2, b: [ba: 1, bb: 2], c: 3, d: [da: 1]], to, 2)

        then:
        4 == to.size()
        1 == to.a
        3 == to.b.size()
        2 == to.b.ba
        1 == to.d.size()

    }

    def "nested Property Value Test"() {
        expect:
        2 == GridUtils.getNestedPropertyValue('b.ba',[a: 1, b: [ba: 2, bc: 3]])
        1 == GridUtils.getNestedPropertyValue('a', [a: 1, b: [ba: 2, bc: 3]])
        1 == GridUtils.getNestedPropertyValue('b.bb.bbb', [a: 1, b: [ba: 2, bc: 3, bb: [bbb: 1]]])
    }

}
