package org.grails.plugin.easygrid.ast

import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * AST transformation that adds dynamic behavior to a config class
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class DynamicConfigASTTransformation extends AbstractASTTransformation {

    void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source)
        println nodes
        addDynamicFields(nodes[1])
    }

    void addDynamicFields(ClassNode source) {
        def phase = CompilePhase.CANONICALIZATION

        try {
//inject services & init method
            List<ASTNode> generalAST = new AstBuilder().buildFromString(phase, false,
                    $/
                        package ${source.packageName}

                        class ${source.nameWithoutPackage} {

                            private Map dynamicProperties = [:]
                            //setter
                            def propertyMissing(String name, value) {
                                dynamicProperties[name] = value
                            }

                            //getter
                            def propertyMissing(String name) {
                                dynamicProperties[name]
                            }

                        }
                        /$)


            generalAST[1].fields.each {
                source.addField(it)
            }

            generalAST[1].methods.each {
                source.addMethod(it)
            }
        } catch (any) {
            any.printStackTrace()
        }

    }
}
