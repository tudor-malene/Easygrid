package org.grails.plugin.easygrid

class PetTest {
    Integer id
    String name
    Date birthDate
    OwnerTest owner

    String someTransientProp

    static transients = 'someTransientProp'
    static constraints = {
        birthDate nullable: true
        someTransientProp nullable: true
    }

    static mapping = {
        id  name: 'id'
    }
}

