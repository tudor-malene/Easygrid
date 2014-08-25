package org.grails.plugin.easygrid

import groovy.transform.Canonical

/**
 * Utility class that allows accessing elements either by name or by index
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Canonical
class ListMapWrapper<T> {

    //list of elements by index
    private List<T> elementList

    //map of elements by key
    private Map<String, T> elementMap

    // when adding an element - it will use this property as the key
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

    def add(String key, int idx, T t) {
        elementList.add(idx, t)
        elementMap.put(key, t)
    }

    def add(T t) {
        elementList.add(t)
        assert t[nameProperty]
        elementMap[t[nameProperty]] = t
    }

    def add(int idx, T t) {
        elementList.add(idx, t)
        assert t[nameProperty]
        elementMap[t[nameProperty]] = t
    }

/*
    def move(String key, int idx) {
        def tmpElem = elementList[idx]
        def oldElem = elementMap[key]
        def oldIdx = elementList.indexOf(oldElem)
        elementList[oldIdx] = tmpElem
        elementList[idx] = oldElem
    }
*/

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

    ListMapWrapper() {
    }
}