package org.grails.plugin.easygrid

import org.springframework.validation.Errors

/**
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
class InlineResponse {

    //a global error message
    String message

    // the object instance  ( the instance.errors object will be used to render the errors)
    def instance

    //errors object -in case you want to create a custom one
    Errors errors

}
