package org.grails.plugin.easygrid

import org.grails.datastore.mapping.query.api.Criteria

import static org.grails.plugin.easygrid.FilterOperatorsEnum.*

/**
 * Created by Tudor on 20.03.2014.
 */
class GormUtils {

    /**
     * to be called in custom filters
     * @param criteria
     * @param operator
     * @param property
     * @param value
     * @return
     */
    static applyFilter(Criteria criteria, FilterOperatorsEnum operator, String property, Object value) {
        def c = createFilterClosure(operator, property, value)
        c.delegate = criteria
        c.call()
    }

    //thanks to doig ken
    static Closure createFilterClosure(FilterOperatorsEnum operator, String property, Object value) {
        switch (operator) {
            case EQ: return { eq(property, value) }
            case NE: return { ne(property, value) }
            case LT: return { lt(property, value) }
            case LE: return { le(property, value) }
            case GT: return { gt(property, value) }
            case GE: return { ge(property, value) }
            case BW: return { ilike(property, "${value}%") }
            case BN: return { not { ilike(property, "${value}%") } }
            case IN: return { 'in'(property, value) }
            case NI: return { not { 'in'(property, value) } }
            case EW: return { ilike(property, "%${value}") }
            case EN: return { not { ilike(property, "%${value}") } }
            case CN: return { ilike(property, "%${value}%") }
            case NC: return { not { ilike(property, "%${value}%") } }
        }
    }

}
