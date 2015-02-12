package org.grails.plugin.easygrid

import org.grails.datastore.mapping.query.api.Criteria

import static org.grails.plugin.easygrid.FilterOperatorsEnum.*
import static org.grails.plugin.easygrid.Filter.v

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
            case EQ: return v(value){ eq(property, value) }
            case NE: return v(value){ ne(property, value) }
            case LT: return v(value){ lt(property, value) }
            case LE: return v(value){ le(property, value) }
            case GT: return v(value){ gt(property, value) }
            case GE: return v(value){ ge(property, value) }
            case BW: return v(value){ ilike(property, "${value}%") }
            case BN: return v(value){ not { ilike(property, "${value}%") } }
            case IN: return v(value){ 'in'(property, value) }
            case NI: return v(value){ not { 'in'(property, value) } }
            case EW: return v(value){ ilike(property, "%${value}") }
            case EN: return v(value){ not { ilike(property, "%${value}") } }
            case CN: return v(value){ ilike(property, "%${value}%") }
            case NC: return v(value){ not { ilike(property, "%${value}%") } }
            case NU: return { isNull(property) }
            case NN: return { isNotNull(property) }
        }
    }

}
