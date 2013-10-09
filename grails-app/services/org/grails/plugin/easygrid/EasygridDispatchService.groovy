package org.grails.plugin.easygrid

/**
 * class that will dispatch method calls to the appropriate implementation
 * will check first if the method was defined - in case the methods are optional
 */
class EasygridDispatchService {
    static transactional = false

    def grailsApplication

    //dispatch to gridImpl
    def callGridImplFilters(gridConfig) {
        assert getGridImplService(gridConfig).respondsTo('filters')
        getGridImplService(gridConfig).filters(gridConfig)
    }

    def callGridImplListParams(gridConfig) {
        assert getGridImplService(gridConfig).respondsTo('listParams')
        getGridImplService(gridConfig).listParams(gridConfig)
    }

    def callGridImplTransform(gridConfig, rows, nrRecords, listParams) {
        assert getGridImplService(gridConfig).respondsTo('transform')
        getGridImplService(gridConfig).transform(gridConfig, rows, nrRecords, listParams)
    }

    def callGridImplDynamicProperties(gridConfig, column) {
        if (getGridImplService(gridConfig).respondsTo('dynamicProperties')) {
            getGridImplService(gridConfig).dynamicProperties(gridConfig, column)
        }
    }

    def callGridImplAddDefaultValues(gridConfig, defaultValues) {
        if (getGridImplService(gridConfig).respondsTo('addDefaultValues')) {
            getGridImplService(gridConfig).addDefaultValues(gridConfig, defaultValues)
        }
    }

    def callGridImplHtmlGridDefinition(gridConfig) {
        if (getGridImplService(gridConfig).respondsTo('htmlGridDefinition')) {
            getGridImplService(gridConfig).htmlGridDefinition(gridConfig)
        }
    }

    def callGridImplInlineEdit(gridConfig) {
        if (getGridImplService(gridConfig).respondsTo('inlineEdit')) {
            getGridImplService(gridConfig).inlineEdit(gridConfig)
        }
    }

    private getGridImplService(gridConfig) {
        grailsApplication.mainContext.getBean(gridConfig.gridImplService)
    }

    //dispatch to the datasource
    def callDSGenerateDynamicColumns(gridConfig) {
        if (getDSService(gridConfig).respondsTo('generateDynamicColumns')) {
            getDSService(gridConfig).generateDynamicColumns(gridConfig)
        }
    }

    def callDSAddDefaultValues(gridConfig, defaultValues) {
        if (getDSService(gridConfig).respondsTo('addDefaultValues')) {
            getDSService(gridConfig).addDefaultValues(gridConfig, defaultValues)
        }
    }

    def callDSVerifyGridConstraints(gridConfig) {
        if (getDSService(gridConfig).respondsTo('verifyGridConstraints')) {
            getDSService(gridConfig).verifyGridConstraints(gridConfig)
        }
    }

    def callDSList(gridConfig, Map listParams = [:], filters = null) {
        assert getDSService(gridConfig).respondsTo('list')
        getDSService(gridConfig).list(gridConfig, listParams, filters)
    }

    //todo - de vazut unde e apelat
    def callDSGetById(gridConfig, id) {
        assert getDSService(gridConfig).respondsTo('getById')
        getDSService(gridConfig).getById(gridConfig, id)
    }

    //todo - de vazut unde e apelat
    def callDSCountRows(gridConfig, filters = null) {
        assert getDSService(gridConfig).respondsTo('countRows')
        getDSService(gridConfig).countRows(gridConfig, filters)
    }

    def callDSUpdateRow(gridConfig) {
        if (getDSService(gridConfig).respondsTo('updateRow')) {
            getDSService(gridConfig).updateRow(gridConfig)
        }
    }

    def callDSSaveRow(gridConfig) {
        if (getDSService(gridConfig).respondsTo('saveRow')) {
            getDSService(gridConfig).saveRow(gridConfig)
        }
    }

    def callDSDelRow(gridConfig) {
        if (getDSService(gridConfig).respondsTo('delRow')) {
            getDSService(gridConfig).delRow(gridConfig)
        }
    }

    private getDSService(gridConfig) {
        grailsApplication.mainContext.getBean(gridConfig.dataSourceService)
    }

    //dispatch to autocomplete

    def callACFilters(GridConfig gridConfig) {
        assert getACService(gridConfig).respondsTo('filters')
        getACService(gridConfig).filters(gridConfig)
    }

    def callACSearchedElementsJSON(gridConfig) {
        assert getACService(gridConfig).respondsTo('searchedElementsJSON')
        getACService(gridConfig).searchedElementsJSON(gridConfig)
    }

    /**
     * for a given id , returns the label of that element
     * @param gridConfig
     * @return
     */
    def callACLabel(gridConfig) {
        assert getACService(gridConfig).respondsTo('label')
        getACService(gridConfig).label(gridConfig)
    }

    private getACService(gridConfig) {
        grailsApplication.mainContext.getBean(gridConfig.autocomplete.autocompleteService)
    }

    //dispatch to export

    def callExportAddDefaultValues(gridConfig, defaultValues) {
        if (getExportService(gridConfig).respondsTo('addDefaultValues')) {
            getExportService(gridConfig).addDefaultValues(gridConfig, defaultValues)
        }
    }

    def callExport(gridConfig) {
        assert getExportService(gridConfig).respondsTo('export')
        getExportService(gridConfig).export(gridConfig)
    }

    private getExportService(gridConfig) {
        grailsApplication.mainContext.getBean(gridConfig.export.exportService)
    }


    //dispatch to filter form

    def callFFFilters(GridConfig gridConfig) {
        assert getFilterFormService(gridConfig).respondsTo('filters')
        getFilterFormService(gridConfig).filters(gridConfig)
    }

    private getFilterFormService(GridConfig gridConfig) {
        grailsApplication.mainContext.getBean(gridConfig.filterForm.filterFormService)
    }

}
