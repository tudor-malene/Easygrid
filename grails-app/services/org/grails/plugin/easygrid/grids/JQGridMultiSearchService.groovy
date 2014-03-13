/*
 * Copyright (c) 2014. PathOS Variant Curation System. All rights reserved.
 *
 * Organisation: Peter MacCallum Cancer Centre
 * Author: doig ken
 */

package org.grails.plugin.easygrid.grids

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

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

    /**
     * Translate a jqgrid multi-clause search into a Criteria Closure for filtering
     * Example: {   "groupOp":"OR",
     *              "rules":    [
     *{"field":"consequence","op":"cn","data":"intron"},
     *{"field":"consequence","op":"eq","data":"missense_variant"}*                          ]
     *}*
     * @param searchRules JSON format rules String
     * @return Criteria Closure of rules
     */
    public Closure multiSearchToCriteriaClosure(String searchRules) {
        def slurper = new JsonSlurper()
        Map r = slurper.parseText(searchRules) as Map

        log.info("In MultiSearchService.multiSearchToCriteriaClosure(): ${r}")

        String clstr = translate(r)


        log.info("Translation: " + clstr)
        GroovyShell shell = new GroovyShell()
        Closure cl = shell.evaluate(clstr) as Closure

        assert cl instanceof Closure: "Check translation"

        return cl
    }

    /**
     * Translate a Map of jqgrid rules into a closure (in a String)
     *
     * @param rules Map of rules from jqgrid
     * @return String to be evaluated as a Groovy expression returning a Criteria closure
     */
    public String translate(Map rules) {
//        def out = new StringBuffer('Closure cl = { params -> ')
        def out = new StringBuffer('{ params ->\n')

        doBlock(out, rules)

        out << "}"

        return out.toString()
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
    private void doBlock(StringBuffer out, Map rules, int indent = 1) {
        log.debug("doBlock: " + rules)

        def noofelements = (rules.rules?.size() ?: 0) + (rules.containsKey('groups') ? 1 : 0)

        //  Opening operation of group if more than one element
        //
        if (noofelements > 1) {
            out << '\t' * indent
            switch (rules.groupOp) {
                case 'AND': out << "and {"; break
                case 'OR': out << "or  {"; break
                default: out << " {";
                    log.warn("Operation not supported " + rules.groupOp)
            }
            out << '\n'
        }

        //  Process nested field operations
        //
        if (rules.rules) {
            assert rules.rules instanceof List

            doRules(out, rules.rules, indent + 1)
        }

        //  Process nested groups of rules
        //
        if (rules.groups) {
            assert rules.groups instanceof List

            doGroups(out, rules.groups, indent)
        }

        //  Closing brace of group if more than one element
        //
        if (noofelements > 1) {
            out << '\t' * indent
            out << "}"
            out << '\n'
        }
    }

    /**
     * Recursively parse the query
     *
     * @param out StringBuffer to append to
     * @param fields Rule to process [field: <fieldName>, op: <op>, data: <data> ]
     */
    private void doRules(StringBuffer out, List fields, int indent) {
        log.debug("doRules: " + fields)

        for (field in fields) {
            out << '\t' * indent
            switch (field.op) {
            //  See http://grails.org/doc/latest/ref/Domain%20Classes/createCriteria.html
            //
            //                eq	equal
            //                ne	not equal
            //                lt	less
            //                le	less or equal
            //                gt	greater
            //                ge	greater or equal
            //                bw	begins with
            //                bn	does not begin with
            //                in	is in
            //                ni	is not in
            //                ew	ends with
            //                en	does not end with
            //                cn	contains
            //                nc	does not contain

                case 'eq':
                    if (field.data) {
                        out << "eq('${field.field}','${field.data}')"
                    } else {
                        out << "isNull('${field.field}')"
                    }
                    break
                case 'ne':
                    if (field.data) {
                        out << "ne('${field.field}','${field.data}')"
                    } else {
                        out << "isNotNull('${field.field}')"
                    }
                    break
                case 'lt': out << "lt('${field.field}','${field.data}')"; break
                case 'le': out << "le('${field.field}','${field.data}')"; break
                case 'gt': out << "gt('${field.field}','${field.data}')"; break
                case 'ge': out << "ge('${field.field}','${field.data}')"; break
                case 'bw': out << "ilike('${field.field}','${field.data}%')"; break
                case 'bn': out << "not{ilike('${field.field}','${field.data}%')}"; break
                case 'in': out << "'in'('${field.field}',${field.data})"; break
                case 'ni': out << "not{'in'('${field.field}',${field.data})}"; break
                case 'ew': out << "ilike('${field.field}','%${field.data}')"; break
                case 'en': out << "not{ilike('${field.field}','%${field.data}')}"; break
                case 'cn': out << "ilike('${field.field}','%${field.data}%')"; break
                case 'nc': out << "not{ilike('${field.field}','%${field.data}%')}"; break
                default: log.warn("Operation not supported [${field.op}]")
            }
            out << '\n'
        }
    }

    /**
     * Recursively parse the query
     *
     * @param out StringBuffer to append to
     * @param fields Rule to process [<block>...]
     */
    private void doGroups(StringBuffer out, List blocks, indent) {
        log.debug("doGroups: " + blocks)

        for (block in blocks) {
            doBlock(out, block as Map, indent + 1)
        }
    }
}
