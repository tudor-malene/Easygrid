/*
 * Copyright (c) 2014. PathOS Variant Curation System. All rights reserved.
 *
 * Organisation: Peter MacCallum Cancer Centre
 * Author: doig ken
 */

package org.grails.plugin.easygrid.grids

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.grails.plugin.easygrid.Filter
import org.grails.plugin.easygrid.FilterOperatorsEnum
import org.grails.plugin.easygrid.Filters
import org.grails.plugin.easygrid.FiltersEnum
import org.grails.plugin.easygrid.GridConfig

/**
 * Created for PathOS.
 *
 * Description:
 *
 * Services to support jqgrid multi-clause search into a Criteria Closure for filtering
 *
 * User: doig ken
 * Date: 22/02/2014
 * Time: 12:37 PM
 */
@Slf4j
class JqGridMultiSearchService {

    static transactional = false
    def filterService

    /**
     * Translate a jqgrid multi-clause search into a Criteria Closure for filtering
     * Example: {   "groupOp":"OR",
     *              "rules":    [
     *{"field":"consequence","op":"cn","data":"intron"},
     *{"field":"consequence","op":"eq","data":"missense_variant"}*                          ]
     *}*
     * @param searchRules JSON format rules String
     * @return Filters object
     */
    public Filters multiSearchToCriteriaClosure(GridConfig gridConfig, String searchRules) {
        def slurper = new JsonSlurper()
        Map r = slurper.parseText(searchRules) as Map

        log.info("In MultiSearchService.multiSearchToCriteriaClosure(): ${r}")

        translate(gridConfig, r)
    }

    /**
     * Translate a Map of jqgrid rules into a Filters structure
     *
     * @param rules Map of rules from jqgrid
     * @return
     */
    public Filters translate(gridConfig, Map rules) {
        def out = new Filters()
        doBlock(gridConfig, out, rules)
        out
    }

    /** Example complex rule of clauses (redundant) in JSON format
     *{   "groupOp":"AND",
     "rules":    [{"field":"filterFlag","op":"eq","data":"xxx"},{"field":"filterFlag","op":"cn","data":"xxx"},{"field":"filterFlag","op":"eq","data":"xxx"}],
     "groups":   [{   "groupOp":"AND",
     "rules":    [{"field":"filterFlag","op":"ne","data":"xx"}],
     "groups":   [{   "groupOp":"OR",
     "rules":    [{"field":"filterFlag","op":"bw","data":"xx"}],
     "groups":   []}]}]}*
     */

    /**
     * Recursively parse the query
     *
     * @param out StringBuffer to append to
     * @param rules Block of the query <block> = [groupOp:AND|OR rules:[<field>...], groups:[<block>...]]
     */
    private void doBlock(gridConfig, Filters out, Map rules) {
        log.debug("doBlock: " + rules)

        def noofelements = (rules.rules?.size() ?: 0) + (rules.containsKey('groups') ? 1 : 0)

        //  Opening operation of group if more than one element
        //
        if (noofelements > 1) {
            switch (rules.groupOp) {
                case 'AND': out.type = FiltersEnum.and; break
                case 'OR': out.type = FiltersEnum.or; break
            }
        }

        //  Process nested field operations
        if (rules.rules) {
            assert rules.rules instanceof List
            doRules(gridConfig, out, rules.rules)
        }

        //  Process nested groups of rules
        if (rules.groups) {
            assert rules.groups instanceof List
            doGroups(gridConfig, out, rules.groups)
        }

    }

    /**
     * Recursively parse the query
     *
     * @param out Filters to append to
     * @param fields Rule to process [field: <fieldName>, op: <op>, data: <data> ]
     */
    private void doRules(gridConfig, Filters out, List fields) {
        log.debug("doRules: " + fields)

        for (field in fields) {
            def column = gridConfig.columns[field.field]
            assert column
            out << filterService.createFilterFromColumn(gridConfig, column, getFilterOperator(field.op), field.data)
        }
    }

    private FilterOperatorsEnum getFilterOperator(String  op) {
        FilterOperatorsEnum.valueOf(op.toUpperCase())
    }

/**
     * Recursively parse the query
     *
     * @param out to append to
     * @param fields Rule to process [<block>...]
     */
    private void doGroups(gridConfig, Filters out, List blocks) {
        log.debug("doGroups: " + blocks)

        for (block in blocks) {
            Filters newBlock = new Filters()
            out.filters << newBlock
            doBlock(gridConfig, newBlock, block as Map)
        }
    }
}
