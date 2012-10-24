package org.grails.plugin.easygrid.datasource

import groovy.util.logging.Log4j

import org.grails.plugin.easygrid.EasygridContextHolder
import org.springframework.web.servlet.support.RequestContext
import org.springframework.web.context.request.RequestContextHolder

/**
 * Datasource implementation
 * the rows are stored in a context ( by default 'session')
 *
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Log4j
@Mixin(EasygridContextHolder)
class ListDatasourceService {


    def verifyGridConstraints(gridConfig) {
        def errors = []

        if (!gridConfig.columns) {
            errors.add("if the type of the grid is not 'domain' then you must define the columns")
        }

        if (!gridConfig.attributeName) {
            errors.add("if the type of the grid is 'list' then you must define a custom 'attributeName' attribute, that will return the list from the specified context")
        }

        errors
    }

    /**
     * returns the list of rows
     * by default will return all elements
     * @param listParams - ( like  rowOffset maxRows sort order
     * @param filters - the search filters
     * @return
     */
    def list(Map listParams, filters = null) {

        def tempList = filters.inject(list) {list, search ->
            list.findAll search.curry(params)
        }

        if (tempList) {
            def end = (listParams.rowOffset + listParams.maxRows > tempList.size()) ? tempList.size() - 1 : listParams.rowOffset + listParams.maxRows - 1
            if (end > listParams.rowOffset) {
                return tempList[listParams.rowOffset..end]
            }
        }
        []
    }

    /**
     * returns the total no of rows
     * @param gridConfig
     * @param filters - when type==domain - it will be a criteria
     * @return
     */
    def countRows(filters = null) {
        filters.inject(list) {list, search ->
            list.findAll search.curry(params)
        }.size()

    }

    // inlineEdit implementations

    /**
     * default method called on updating a grid element
     */
    def updateRow = {

        def instance = list[params.id as int]
        if (!instance) {
            return 'default.not.found.message'
        }

/*
        if (params.version) {
            def version = params.version.toLong()
            if (instance.version > version) {
                return 'default.optimistic.locking.failure'
            }
        }
*/

        //default returns params
        gridConfig.beforeSave(params).each {k, v ->
            instance[k] = v
        }
/*

        if (!instance.save(flush: true)) {
            return instance.errors
        }
*/
    }

    /**
     * default method  called on saving a new grid element
     */
    def saveRow = {
        list.add gridConfig.beforeSave params
    }

    /**
     * default method  called on deleting a grid element
     */
    def delRow = {
        def instance = list[params.id as int]

        if (!instance) {
//            Errors errors = new
            return 'default.not.found.message'
        }
        list.remove(params.id as int)
    }


    def getList(){
        def ctx
        switch (gridConfig.context){
            case null:
            case 'session':
                ctx = session
                break
            case 'applicationContext':
                ctx = RequestContextHolder.currentRequestAttributes().applicationContext
                break
            case 'request':
                ctx = request
                break
            case 'flash':
                ctx = flashScope
                break

        }

        ctx[gridConfig.attributeName]
    }

}
