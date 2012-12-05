package org.grails.plugin.easygrid

import groovy.transform.AutoClone

/**
 * User: Tudor
 * Date: 29.11.2012
 * Time: 23:04
 * columns container that allows accessing columns by index or by key
 */
@AutoClone
class ColumnsConfig {

    //list of columns by index
    private List<ColumnConfig> columnList

    //map of columns by key
    private Map<String, ColumnConfig> columnMap

    ColumnsConfig() {
        columnList = []
        columnMap = [:]
    }

    def add(String key, ColumnConfig columnConfig) {
        columnList.add(columnConfig)
        columnMap.put(key, columnConfig)
    }

    def add(ColumnConfig columnConfig) {
        columnList.add(columnConfig)
        assert columnConfig.name
        columnMap[columnConfig.name] = columnConfig
    }


    // utility method - so that this container behaves like a list or like a map
    def ColumnConfig getAt(int idx) {
        columnList[idx]
    }

    def ColumnConfig getAt(String key) {
        columnMap[key]
    }

    def propertyMissing(String key) {
        this[key]
    }

    def asBoolean(){
        !columnList.isEmpty()
    }

    Iterator iterator() {
        columnList.iterator()
    }

    def size() {
        columnList.size()
    }

    def getSize() {
        size()
    }

    def deepClone() {
        ColumnsConfig clone = this.clone()
        clone.columnList = []
        clone.columnMap = [:]
        this.columnMap.each {key, value ->
            def v = value.deepClone()
            clone.columnList.add(v)
            clone.columnMap.put(key, v)
        }
        clone
    }

}
