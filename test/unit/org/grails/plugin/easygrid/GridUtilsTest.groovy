package org.grails.plugin.easygrid

import org.codehaus.groovy.control.ConfigurationException
import org.springframework.mock.web.MockHttpSession

import javax.servlet.http.HttpSession

/**
 * tests for util methods
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class GridUtilsTest extends GroovyTestCase {

    void testCopyProperties() {

        def to = [a: 1]
        GridUtils.copyProperties([a: 2, b: 2], to)
        assertEquals 2, to.size()
        assertEquals 1, to.a
        assertEquals 2, to.b

        to = [a: 1]
        GridUtils.copyProperties([a: 2, b: [ba: 1, bb: 2]], to)
        assertEquals 2, to.size()
        assertEquals 1, to.a
        assertEquals 2, to.b.size()

        to = [a: 1, b: [ba: 2, bc: 3]]
        GridUtils.copyProperties([a: 2, b: [ba: 1, bb: 2]], to)
        assertEquals 2, to.size()
        assertEquals 1, to.a
        assertEquals 3, to.b.size()
        assertEquals 2, to.b.ba

        shouldFail(ConfigurationException) {
            to = [a: 1, b: 2]
            GridUtils.copyProperties([a: 2, b: [ba: 1, bb: 2]], to)
        }

        to = [a: 1, b: [ba: 2, bc: 3]]
        GridUtils.copyProperties([a: 2, b: [ba: 1, bb: 2], c: 3, d: [da: 1]], to, 1)
        assertEquals 3, to.size()
        assertEquals 1, to.a
        assertEquals 2, to.b.size()
        assertEquals 2, to.b.ba
        assertNull to.d

        to = [a: 1, b: [ba: 2, bc: 3]]
        GridUtils.copyProperties([a: 2, b: [ba: 1, bb: 2], c: 3, d: [da: 1]], to, 2)
        assertEquals 4, to.size()
        assertEquals 1, to.a
        assertEquals 3, to.b.size()
        assertEquals 2, to.b.ba
        assertEquals 1, to.d.size()

    }

    void nestedPropertyValueTest() {
        assertEquals 2, GridUtils.getNestedPropertyValue('b.ba', [a: 1, b: [ba: 2, bc: 3]])
        assertEquals 1, GridUtils.getNestedPropertyValue('a', [a: 1, b: [ba: 2, bc: 3]])
        assertEquals 1, GridUtils.getNestedPropertyValue('b.bb.bbb', [a: 1, b: [ba: 2, bc: 3, bb: [bbb: 1]]])
    }

    void testRestoreSearchParams() {
        HttpSession session = new MockHttpSession()

        //simulates a series of requests
/*
        def params = 1
        assertEquals 1, GridUtils.restoreSearchParams(session, params, 'grid1')

        params = 2
        assertEquals 2, GridUtils.restoreSearchParams(session, params, 'grid1')

        // when exporting a table or returning from an add/update screen and you want to save the old search use this
        GridUtils.markRestorePreviousSearch(session)
        params = 3
        assertEquals 2, GridUtils.restoreSearchParams(session, params, 'grid1')

        params = 4
        assertEquals 4, GridUtils.restoreSearchParams(session, params, 'grid1')
*/
    }

}
