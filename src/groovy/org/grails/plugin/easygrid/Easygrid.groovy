package org.grails.plugin.easygrid

import java.lang.annotation.*
/**
 * Annotation for Controllers at the class level.
 * The controller must have a static field named "grids" with the definitions of the grids that will be made available by this Controller
 * ( the format of the definitions is described in the Builder)
 *
 * The Controller will be injected with 3 methods for each Grid
 *    ${gridName}Rows ()        - will return the actual rows
 *    ${gridName}Export ()      - export
 *    ${gridName}InlineEdit ()  - will be called on submitting the inline form
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
//@GroovyASTTransformationClass("org.grails.plugin.easygrid.EasygridASTTransformation")
@interface Easygrid {

    /**
     * a class that must have a static grids field containing definitions of grids that will be served by the current controller
     * @return
     */
    Class externalGrids( )default Object.class
}
