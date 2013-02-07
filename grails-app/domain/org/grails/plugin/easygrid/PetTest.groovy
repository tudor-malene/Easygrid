package org.grails.plugin.easygrid

class PetTest {
    String name
    Date birthDate
    OwnerTest owner

    static constraints = {
        birthDate nullable: true
    }
}
