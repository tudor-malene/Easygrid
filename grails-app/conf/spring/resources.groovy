import org.grails.plugin.easygrid.builder.ColumnDelegate
import org.grails.plugin.easygrid.builder.ColumnsDelegate
import org.grails.plugin.easygrid.builder.GridDelegate
import org.grails.plugin.easygrid.builder.GridsDelegate

beans = {
    columnDelegate(ColumnDelegate) {
        grailsApplication = ref('grailsApplication')
        it.scope = 'prototype'
    }
    columnsDelegate(ColumnsDelegate) {
        grailsApplication = ref('grailsApplication')
        columnDelegate = ref('columnDelegate')
        it.scope = 'prototype'
    }
    gridDelegate(GridDelegate) {
        grailsApplication = ref('grailsApplication')
        columnsDelegate = ref('columnsDelegate')
        it.scope = 'prototype'
    }
    gridsDelegate(GridsDelegate) {
        gridDelegate = ref('gridDelegate')
        it.scope = 'prototype'
    }
}