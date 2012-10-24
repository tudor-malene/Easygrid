package org.grails.plugin.easygrid.grids

import grails.converters.JSON
import org.grails.plugin.easygrid.EasygridContextHolder

/**
 * implementation for Datatable
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Mixin(EasygridContextHolder)
class DatatableGridService {

    static transactional = false

    def easygridService
    def grailsApplication


    def filters() {
/*
        gridConfig.columns.eachWithIndex {col, i ->
            if (params["bSearchable_$i"] && params["sSearch_$i"]) {
                //todo - coloana like params["sSearch_$i"]
            }
        }
*/

        //todo
    }

    def listParams() {
        def maxRows = params.iDisplayLength ? (params.iDisplayLength as int) : grailsApplication.config?.easygrid?.defaults?.defaultMaxRows
        assert maxRows
//        def currentPage = 1 + (params.iDisplayStart ? (params.iDisplayStart as int) : 0) / maxRows


        def order = [:]
        if (params.iSortCol_0 != null) {

            (0..(params.iSortingCols as int) - 1).each {
                def sortCol = params["iSortCol_$it"] as int
                if (params["bSortable_${sortCol}"] == true) {
                    order[gridConfig.columns[sortCol]] = params["sSortDir_$it"]
                }
            }
        }

        /*
  * Filtering
  * NOTE this does not match the built-in DataTables filtering which does it
  * word by word on any field. It's possible to do here, but concerned about efficiency
  * on very large tables, and MySQL's regex functionality is very limited

   $sWhere = "";
   if ( isset($_GET['sSearch']) && $_GET['sSearch'] != "" )
   {
       $sWhere = "WHERE (";
       for ( $i=0 ; $i<count($aColumns) ; $i++ )
       {
           $sWhere .= "`".$aColumns[$i]."` LIKE '%".mysql_real_escape_string( $_GET['sSearch'] )."%' OR ";
       }
       $sWhere = substr_replace( $sWhere, "", -3 );
       $sWhere .= ')';
   }
        */

        if (params.sSearch) {
            //todo - de bagat la toate coloanele
        }

        /* Individual column filtering
        for ( $i=0 ; $i<count($aColumns) ; $i++ )
        {
            if ( isset($_GET['bSearchable_'.$i]) && $_GET['bSearchable_'.$i] == "true" && $_GET['sSearch_'.$i] != '' )
            {
                if ( $sWhere == "" )
                {
                    $sWhere = "WHERE ";
                }
                else
                {
                    $sWhere .= " AND ";
                }
                $sWhere .= "`".$aColumns[$i]."` LIKE '%".mysql_real_escape_string($_GET['sSearch_'.$i])."%' ";
            }
        }
           */


        def sort=null

        [rowOffset: params.iDisplayStart, maxRows: maxRows, sort: sort, order: order[0]]
    }

    def transform( rows, nrRecords, listParams) {
        [
                sEcho: params.sEcho,
                iTotalRecords: nrRecords,
                iTotalDisplayRecords: rows.size(),
                aaData: rows.collect { element ->
                    def cell = []
                    gridConfig.columns.eachWithIndex { col, row ->
                        cell.add easygridService.valueOfColumn(col, element, row + 1)
                    }
                    cell
                }
        ] as JSON
    }

}
