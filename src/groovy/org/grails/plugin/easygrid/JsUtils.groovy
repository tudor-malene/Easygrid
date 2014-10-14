package org.grails.plugin.easygrid

import grails.converters.JSON
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller
import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.json.JSONException

//import static org.grails.plugin.easygrid.JqGridUtils.JqgridType.*

/**
 * provides a tighter integration with jqgrid
 * some validations
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class JsUtils {

    static def registerMarshallers() {

        JSON.registerObjectMarshaller(new ObjectMarshaller<JSON>() {
            @Override
            boolean supports(Object object) {
                JSFunction.isAssignableFrom(object.getClass())
            }

            @Override
            void marshalObject(Object object, JSON converter) throws ConverterException {
                try {
                    converter.getWriter().value(new JSONObject() {
                        //used by older grails
                        String toString() {
                            object.toString()
                        }

                        Writer writeTo(Writer out) throws IOException {
                            out.write(object.toString())
                        }
                    })
                }
                catch (JSONException e) {
                    throw new ConverterException(e);
                }
            }
        }, 1)


        JSON.registerObjectMarshaller(LazyString, 2) {
            it.call()
        }
    }


    static def jqgridFilterOperatorConverter(FilterOperatorsEnum op) {
        op.toString().toLowerCase()
    }

    static traverseValues(values, Map actions) {
        if (values instanceof Map) {
            return values.collectEntries { k, v ->
                Closure action = actions.find { cond, act -> cond(k, v) }?.value
                def val
                if (action) {
                    val = action(k, v)
                } else {
                    val = traverseValues(v, actions)
                }
                [(k): val]
            }
        }
        values
    }

    static boolean isString(val) {
        val instanceof CharSequence
    }

    static String convertToJs(Map values, String gridId, boolean include = false) {
        def newValues = traverseValues(values, [
                ({ k, v -> isString(v) && v.startsWith('f:') })                    : { k, v -> new JSFunction(v) },
                ({ k, v -> isString(v) && v.startsWith('g:') })                    : { k, v -> new JSFunction(v, gridId) },
                ({ k, v -> isString(v) && (v.toLowerCase() in ['true', 'false']) }): { k, v -> Boolean.valueOf(v) }
        ])

        JSON json = newValues as JSON

        String val = json.toString()
        if (include) {
            //truncate the parans
            val[1..-2]
        } else {
            val
        }
    }

    /**
     * used mainly for jqgrid - to load the data in a selectbox - for filtering
     * @param property
     * @param listClosure
     * @return
     */
    static def convertListToString(String property = 'name', Closure listClosure) {
        new LazyString({
            (
                    [[(property): 'All', id: '']] + listClosure()).collect { "${it.id}:${it[property]}" }.join(';')
        })
    }

/*
    static enum JqgridType {
        string, function, array, mixed, object, number, bool, integer
    }

    // see http://www.trirand.com/jqgridwiki/doku.php?id=wiki:options
    static def modelOptions = [
            ajaxGridOptions  : object,
            ajaxSelectOptions: object,
            altclass         : string,
            altRows          : bool,
            autoencode       : bool,
            autowidth        : bool,
            caption          : string,
            cellLayout       : integer,
            cellEdit         : bool,
            cellsubmit       : string,
            cellurl          : string,
            cmTemplate       : object,
            colModel         : array,
            colNames         : array,
            data             : array,
            datastr          : string,
            datatype         : string,
            deepempty        : bool,
            deselectAfterSort: bool,
            direction        : string,
            editurl          : string,
            emptyrecords     : string,
            ExpandColClick   : bool,
            ExpandColumn     : string,
            footerrow        : bool,
            forceFit         : bool,
            gridstate        : string,
            gridview         : bool,
            grouping         : bool,
            headertitles     : bool,
            height           : mixed,
            hiddengrid       : bool,
            hidegrid         : bool,
            hoverrows        : bool,
            idPrefix         : string,
            ignoreCase       : bool,
            inlineData       : object,
            jsonReader       : array,
            lastpage         : integer,
            lastsort         : integer,
            loadonce         : bool,
            loadtext         : string,
            loadui           : string,
            mtype            : string,
            multikey         : string,
            multiboxonly     : bool,
            multiselect      : bool,
            multiselectWidth : integer,
            multiSort        : bool,
            page             : integer,
            pager            : mixed,
            pagerpos         : string,
            pgbuttons        : bool,
            pginput          : bool,
            pgtext           : string,
            prmNames         : array,
            postData         : array,
            reccount         : integer,
            recordpos        : string,
            records          : integer,
            recordtext       : string,
            resizeclass      : string,
            rowList          : array,
            rownumbers       : bool,
            rowNum           : integer,
            rowTotal         : integer,
            rownumWidth      : integer,
            savedRow         : array,
            searchdata       : array,
            scroll           : integer,
            scrollOffset     : integer,
            scrollTimeout    : integer,
            scrollrows       : bool,
            selarrrow        : array,
            selrow           : string,
            shrinkToFit      : bool,
            sortable         : bool,
            sortname         : string,
            sortorder        : string,
            subGrid          : bool,
            subGridOptions   : object,
            subGridModel     : array,
            subGridType      : mixed,
            subGridUrl       : string,
            subGridWidth     : integer,
            toolbar          : array,
            toppager         : bool,
            totaltime        : integer,
            treedatatype     : mixed,
            treeGrid         : bool,
            treeGridModel    : string,
            treeIcons        : array,
            treeReader       : array,
            tree_root_level  : number,
            url              : string,
            userData         : array,
            userDataOnFooter : bool,
            viewrecords      : bool,
            viewsortcols     : array,
            width            : number,
            xmlReader        : array,
    ]

    //see http://www.trirand.com/jqgridwiki/doku.php?id=wiki:colmodel_options
    static def colModelOptions = [
            align         : string,
            cellattr      : function,
            classes       : string,
            datefmt       : string,
            defval        : string,
            editable      : bool,
            editoptions   : array,
            editrules     : array,
            edittype      : string,
            firstsortorder: string,
            fixed         : bool,
            formoptions   : array,
            formatoptions : array,
            formatter     : function, //mixed,
            frozen        : bool,
            hidedlg       : bool,
            hidden        : bool,
            index         : string,
            jsonmap       : string,
            key           : bool,
            label         : string,
            name          : string,
            resizable     : bool,
            search        : bool,
            searchoptions : array,
            sortable      : bool,
            sortfunc      : function,
            sorttype      : mixed,
            stype         : string,
            surl          : string,
            template      : object,
            title         : bool,
            width         : number,
            xmlmap        : string,
            unformat      : function,
            viewable      : bool,
    ]

    // searching
    // see http://www.trirand.com/jqgridwiki/doku.php?id=wiki:search_config
    static def searchoptions = [
            dataUrl     : string,
            buildSelect : function,
            dataInit    : function,
            dataEvents  : array,
            attr        : object,
            searchhidden: bool,
            sopt        : array,
            defaultValue: string,
            value       : mixed,
            clearSearch : bool,
    ]

    static def searchrules = [
            required   : bool,
            number     : bool,
            integer    : bool,
            minValue   : number,
            maxValue   : number,
            email      : bool,
            url        : bool,
            date       : bool,
            time       : bool,
            custom     : bool,
            custom_func: function
    ]

    //see http://www.trirand.com/jqgridwiki/doku.php?id=wiki:custom_searching
    static def filtergrid = [
            gridModel   : bool,
            gridNames   : bool,
            gridToolbar : bool,
            filterModel : array,
            formtype    : string,
            autosearch  : bool,
            formclass   : bool,
            tableclass  : string,
            buttonclass : string,
            searchButton: string,
            clearButton : string,
            enableSearch: bool,
            enableClear : bool,
            beforeSearch: function,
            afterSearch : function,
            beforeClear : function,
            afterClear  : function,
            url         : string,
            marksearched: bool
    ]

    //see http://www.trirand.com/jqgridwiki/doku.php?id=wiki:pager
    static def pageroptions = [
            lastpage   : integer,
            pager      : mixed,
            pagerpos   : string,
            pgbuttons  : bool,
            pginput    : bool,
            pgtext     : string,
            reccount   : integer,
            recordpos  : string,
            records    : integer,
            recordtext : string,
            rowList    : array,
            rowNum     : integer,
            viewrecords: bool,
    ]

    //see http://www.trirand.com/jqgridwiki/doku.php?id=wiki:navigator
    static def navigatorOptions = [
            add          : bool,
            addicon      : string,
            addtext      : string,
            addtitle     : string,
            alertcap     : string,
            alerttext    : string,
            cloneToTop   : bool,
            closeOnEscape: bool,
            del          : bool,
            delicon      : string,
            deltext      : string,
            deltitle     : string,
            edit         : bool,
            editicon     : string,
            edittext     : string,
            edittitle    : string,
            position     : string,
            refresh      : bool,
            refreshicon  : string,
            refreshtext  : string,
            refreshtitle : string,
            refreshstate : string,
            afterRefresh : function,
            beforeRefresh: function,
            search       : bool,
            searchicon   : string,
            searchtext   : string,
            searchtitle  : string,
            view         : bool,
            viewicon     : string,
            viewtext     : string,
            viewtitle    : string,
            addfunc      : function,
            editfunc     : function,
            delfunc      : function,
    ]

    //editing

    //see http://www.trirand.com/jqgridwiki/doku.php?id=wiki:common_rules
    static def editoptions = [
            value         : mixed,
            dataUrl       : string,
            buildSelect   : function,
            dataInit      : function,
            dataEvents    : array,
            defaultValue  : mixed,
            NullIfEmpty   : bool,
            custom_element: function,
            custom_value  : function,
    ]

    //edit options
    static def editrules = [
            edithidden : bool,
            required   : bool,
            number     : bool,
            integer    : bool,
            minValue   : number,
            maxValue   : number,
            email      : bool,
            url        : bool,
            date       : bool,
            time       : bool,
            custom     : bool,
            custom_func: function,
    ]

    //formoptions
    static def formoptions = [
            elmprefix: string,
            elmsuffix: string,
            label    : string,
            rowpos   : number,
            colpos   : number,
    ]

    //see http://www.trirand.com/jqgridwiki/doku.php?id=wiki:inline_editing
    static def inlineEditOptions = [
            add        : bool,
            addicon    : string,
            addtext    : string,
            addtitle   : string,
            edit       : bool,
            editicon   : string,
            edittext   : string,
            edittitle  : string,
            position   : string,
            save       : bool,
            saveicon   : string,
            savetext   : string,
            savetitle  : string,
            cancel     : bool,
            cancelicon : string,
            canceltext : string,
            canceltitle: string,
            addParams  : object,
            editParams : object,
    ]
*/

    // see http://www.trirand.com/jqgridwiki/doku.php?id=wiki:form_editing

}

//used for json marshalling
class JSFunction implements JSONElement {
    String functionName
    String gridId
    boolean addGridName = false

    JSFunction() {
    }

    JSFunction(String functionName) {
        this.functionName = functionName[2..-1]
    }

    JSFunction(String functionName, gridId) {
        this.functionName = functionName[2..-1]
        this.addGridName = true
        this.gridId = gridId
    }


    @Override
    public String toString() {
        if (addGridName) {
            "${functionName}('${gridId}')"
        } else {
            functionName
        }
    }

    @Override
    Writer writeTo(Writer out) throws IOException {
        out.append(this.toString())
    }
}

//used for json marshalling
class LazyString {
    def lazyStringClosure

    LazyString() {
    }

    LazyString(lazyString) {
        this.lazyStringClosure = lazyString
    }

    def call() {
        lazyStringClosure.call()
    }
}
