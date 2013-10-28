package org.grails.plugin.easygrid

import java.lang.annotation.*
/**
 * Annotation for Controllers at the class level.
 * It specifies that this controller might contain definitions of grids.
 *
 *
 * The grids can be defined in multiple ways:
 * - a static field named "grids" with the definitions of the grids
 * - the grids defined in the class specified in 'externalGrids'
 * - a closure ending with 'Grid'
 *
 *
 * The Controller will be injected with multiple methods for each Grid, depending on the configuration
 *    ${gridName}Rows ()        - will return the actual rows
 *    ${gridName}Export ()      - export
 *    ${gridName}InlineEdit ()  - will be called on submitting the inline form
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface Easygrid {

    /**
     * a class that must have a static grids field containing definitions of grids that will be served by the current controller
     * @return
     */
    Class externalGrids() default Object.class
}
