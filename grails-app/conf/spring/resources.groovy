import org.grails.plugin.easygrid.builder.ColumnDelegate
import org.grails.plugin.easygrid.builder.ColumnsDelegate
import org.grails.plugin.easygrid.builder.GridDelegate
import org.grails.plugin.easygrid.builder.GridsDelegate
import org.grails.plugin.easygrid.builder.AutocompleteDelegate

//todo - duplicate code - find a way to remove
beans = {
    columnDelegate(ColumnDelegate) {
        grailsApplication = ref('grailsApplication')
        it.scope = 'prototype'
    }
    columnsDelegate(ColumnsDelegate) {
        columnDelegate = ref('columnDelegate')
        grailsApplication = ref('grailsApplication')
        it.scope = 'prototype'
    }
    autocompleteDelegate(AutocompleteDelegate) {
        grailsApplication = ref('grailsApplication')
        it.scope = 'prototype'
    }
    gridDelegate(GridDelegate) {
        grailsApplication = ref('grailsApplication')
        columnsDelegate = ref('columnsDelegate')
        autocompleteDelegate = ref('autocompleteDelegate')
        it.scope = 'prototype'
    }
    gridsDelegate(GridsDelegate) {
        gridDelegate = ref('gridDelegate')
        it.scope = 'prototype'
    }
}