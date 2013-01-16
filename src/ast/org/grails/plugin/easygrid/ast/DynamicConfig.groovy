package org.grails.plugin.easygrid.ast

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * transforms the original config class and adds a dynamic map
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@java.lang.annotation.Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@GroovyASTTransformationClass("org.grails.plugin.easygrid.ast.DynamicConfigASTTransformation")
@interface DynamicConfig  {
}