package org.grails.plugin.easygrid

import groovy.transform.AutoClone

/**
 * Utility class that allows accessing elements either by name or by index
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@AutoClone
class ListMapWrapper<T> {

    //list of elements by index
    private List<T> elementList

    //map of elements by key
    private Map<String, T> elementMap

    String nameProperty

    ListMapWrapper(nameProperty) {
        elementList = []
        elementMap = [:]
        this.nameProperty = nameProperty
    }

    def add(String key, T t) {
        elementList.add(t)
        elementMap.put(key, t)
    }

    def add(T t) {
        elementList.add(t)
        assert t[nameProperty]
        elementMap[t[nameProperty]] = t
    }

    // utility method - so that this container behaves like a list or like a map
    def T getAt(int idx) {
        elementList[idx]
    }

    def T getAt(String key) {
        elementMap[key]
    }

    def propertyMissing(String key) {
        this[key]
    }

    def asBoolean() {
        !elementList.isEmpty()
    }

    Iterator iterator() {
        elementList.iterator()
    }

    def size() {
        elementList.size()
    }

    def getSize() {
        size()
    }

    def deepClone() {
        ListMapWrapper<ColumnConfig> clone = this.clone()
        clone.elementList = []
        clone.elementMap = [:]
        this.elementMap.each { key, value ->
            def v = value.deepClone()
            clone.elementList.add(v)
            clone.elementMap.put(key, v)
        }
        clone
    }
}