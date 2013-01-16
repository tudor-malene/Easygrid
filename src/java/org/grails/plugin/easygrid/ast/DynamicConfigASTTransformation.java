package org.grails.plugin.easygrid.ast;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.builder.AstBuilder;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.LocatedMessage;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import java.util.List;

/**
 * AST transformation that adds dynamic behavior to a config class
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class DynamicConfigASTTransformation extends AbstractASTTransformation {

    public void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source);
        System.out.println("nodes = " + nodes);
        addDynamicFields((ClassNode) nodes[1]);
    }

    void addDynamicFields(ClassNode source) {

        try {
            String inject = "                        package " + source.getPackageName() + "\n" +
                    "\n" +
                    "                        class  " + source.getNameWithoutPackage() + " {\n" +
                    "\n" +
                    "                            private Map dynamicProperties = [:]\n" +
                    "                            //setter\n" +
                    "                            def propertyMissing(String name, value) {\n" +
                    "                                dynamicProperties[name] = value\n" +
                    "                            }\n" +
                    "\n" +
                    "                            //getter\n" +
                    "                            def propertyMissing(String name) {\n" +
                    "                                dynamicProperties[name]\n" +
                    "                            }\n" +
                    "\n" +
                    "                        }\n";

//inject services & init method
            List<ASTNode> generalAST = new AstBuilder().buildFromString(CompilePhase.CANONICALIZATION, false, inject);


            for (FieldNode astNode : ((ClassNode) (generalAST.get(1))).getFields()) {
                source.addField(astNode);
            }
            for (MethodNode astNode : ((ClassNode) (generalAST.get(1))).getMethods()) {
                source.addMethod(astNode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            String messageText = "error adding grid methods to: " + source.getNameWithoutPackage();
            addError(messageText, source);
        }

    }
}
