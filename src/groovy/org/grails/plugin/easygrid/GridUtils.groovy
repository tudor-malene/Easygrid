package org.grails.plugin.easygrid

import org.grails.plugin.easygrid.grids.ClassicGridService
import org.grails.plugin.easygrid.grids.DatatableGridService
import org.grails.plugin.easygrid.grids.JqueryGridService
import org.grails.plugin.easygrid.grids.VisualizationGridService
import org.grails.plugin.easygrid.datasource.CustomDatasourceService
import org.grails.plugin.easygrid.datasource.GormDatasourceService
import org.grails.plugin.easygrid.datasource.ListDatasourceService
import org.codehaus.groovy.control.ConfigurationException

/**
 * utility methods
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class GridUtils {

/*
    public static function encode($value)
    {
        if (is_string($value)) {
            if (strpos($value, 'js:') === 0) return substr($value, 3); else   return '"' . self::quote($value) . '"';
        } else if ($value === null) return "null"; else if (is_bool($value)) return $value ? "true" : "false"; else if (is_integer($value)) return "$value"; else if (is_float($value)) {
            if ($value === -INF) return 'Number.NEGATIVE_INFINITY'; else if ($value === INF) return 'Number.POSITIVE_INFINITY'; else   return "$value";
        } else if (is_object($value)) return self::encode(get_object_vars($value)); else if (is_array($value)) {
        $es = array();
        if (($n = count($value)) > 0 && array_keys($value) !== range(0, $n - 1)) {
            foreach ($value as $prop => $val) $es[] = '"' . self::quote($prop) . '":' . self::encode($val);
            return "{" . implode(',', $es) . "}";
        } else {
            foreach ($value as $val) $es[] = self::encode($val);
            return "[" . implode(',', $es) . "]";
        }
    } else   return "";
    }
*/

    /**
     * copy the values from the first map to the second only if they are not defined first
     * works on n levels
     * @param from
     * @param to
     * @param level - on how many level to copy ( -1 = infinite)
     * @return
     */
    static def copyProperties(from, to, level = -1) {
        from.each { prop, val ->
            if ((val instanceof Map)) {
                if (level <= -1 || level > 1) {
                    if (to[prop] == null) {to[prop] = [:]}
                    if (!(to[prop] instanceof Map)) {
                        throw new ConfigurationException("Trying to copy properties from ${val} to ${to[prop]}. ${to[prop]} should be a map")
                    }
                    copyProperties(val, to[prop], level - 1)
                }
            } else {
                if (to[prop] == null) {
                    to[prop] = val
                }
            }
        }
    }

    /**
     * searches for actual grid implementations in the config section
     * @param config
     * @return
     */
    static def findImplementations(Map config) {
        config.gridImplementations.collect {it.key}
    }

    /**
     * stores the last search parameters
     * in the session , to be retrieved on the next return on the page
     *
     * in case the restoreSearch mark is set, will restore the parameters
     * @param session
     * @param params
     * @param id
     * @return
     */
    static def restoreSearchParams() {
        String searchParamsAttrName = "searchParams_${EasygridContextHolder.gridConfig.id}".toString()

        if (EasygridContextHolder.session.getAttribute('restoreLastSearch')) {
            def localParams = EasygridContextHolder.session.getAttribute(searchParamsAttrName) ?: EasygridContextHolder.params
            EasygridContextHolder.storeParams(localParams)
            EasygridContextHolder.session.removeAttribute('restoreLastSearch')
        } else {
            //save the current search param
            EasygridContextHolder.session.setAttribute(searchParamsAttrName, EasygridContextHolder.params)
        }
    }

    /**
     * when exporting a table or returning from an add/update screen and you want to save the old search use this
     * @return
     */
    static def markRestorePreviousSearch() {
        EasygridContextHolder.session.setAttribute('restoreLastSearch', true)
    }

    /**
     * workaround for bug - http://jira.grails.org/browse/GRAILS-8652
     */
    static def addMixins() {
        EasygridService.mixin EasygridContextHolder
        ClassicGridService.mixin EasygridContextHolder
        VisualizationGridService.mixin EasygridContextHolder
        DatatableGridService.mixin EasygridContextHolder
        JqueryGridService.mixin EasygridContextHolder
        EasyGridExportService.mixin EasygridContextHolder

        GormDatasourceService.mixin EasygridContextHolder
        ListDatasourceService.mixin EasygridContextHolder
        CustomDatasourceService.mixin EasygridContextHolder

//        GridUtils.mixin GridConfigHolder
    }

    /**
     * hack to navigate neste objects
     *
     * @param expression
     * @param object
     * @return
     */
    static def getNestedPropertyValue(String expression, object) {
        expression.tokenize('.').inject object, {obj, prop -> obj[prop]}
    }

}
