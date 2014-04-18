package org.grails.plugin.easygrid

class OwnerTest {

    String name
    String address
    String city
    String telephone

    static hasMany = [pets: PetTest]

    static constraints = {
        address nullable: true
        telephone nullable: true
    }

    static namedQueries = {
        namedJohn{
            eq('name','John')
        }
    }
}
