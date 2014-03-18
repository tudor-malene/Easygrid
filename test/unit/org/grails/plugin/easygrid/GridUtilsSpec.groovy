package org.grails.plugin.easygrid

import grails.validation.ValidationErrors
import org.codehaus.groovy.control.ConfigurationException
import org.codehaus.groovy.grails.web.binding.DataBindingUtils
import org.springframework.validation.FieldError
import spock.lang.Specification
import spock.lang.Unroll

/**
 * tests for util methods
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class GridUtilsSpec extends Specification {

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
        2 == GridUtils.getNestedPropertyValue('b.ba', [a: 1, b: [ba: 2, bc: 3]])
        1 == GridUtils.getNestedPropertyValue('a', [a: 1, b: [ba: 2, bc: 3]])
        1 == GridUtils.getNestedPropertyValue('b.bb.bbb', [a: 1, b: [ba: 2, bc: 3, bb: [bbb: 1]]])
    }


    def "failed data binding"() {
        when:
        def val = GridUtils.convertValueUsingBinding('string', Integer)

        then:
        null == val
    }


    @Unroll
    def "data binding"(String source, type, dest) {
        given:

        when:
        def val = GridUtils.convertValueUsingBinding(source, type)

        then:
        dest == val

        where:
        source | type | dest
        '1'   | Integer    | 1
        '1'   | String     | '1'
        '1.1' | BigDecimal | 1.1
//        '2010-01-01 00:00:00.000' | Date       | new Date(2010 - 1900, 1 - 1, 1)

    }


}
