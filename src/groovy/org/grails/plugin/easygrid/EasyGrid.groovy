package org.grails.plugin.easygrid;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import org.codehaus.groovy.transform.GroovyASTTransformationClass;

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
@java.lang.annotation.Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@GroovyASTTransformationClass("org.grails.plugin.easygrid.EasyGridASTTransformation")
public @interface EasyGrid {
}
