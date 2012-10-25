package org.grails.plugin.easygrid;


import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import groovy.util.logging.Log4j
import org.codehaus.groovy.ast.expr.ClosureExpression

/**
 * AST transformation that adds specific grid methods to annotated controllers
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Log4j
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class EasyGridASTTransformation extends AbstractASTTransformation {

    public EasyGridASTTransformation() {
    }

    public void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source);
        addGridStuff(nodes[1])
    }

    /**
     * adds methods for displaying eachGrid
     * @param source
     * @return
     */
    def addGridStuff(ClassNode source) {
        def phase = CompilePhase.SEMANTIC_ANALYSIS

        def gridNames = []

        assert source.getField('grids')

        source.getField('grids').initialExpression.code.statements.each {
            gridNames.add it.expression.method.value
        }


        try {
            //inject services & init method
            List<ASTNode> generalAST = new AstBuilder().buildFromString(phase, false,
                    $/
                    package ${source.packageName}

                    import org.grails.plugin.easygrid.*

                    class ${source.nameWithoutPackage} {

                        // added getters and setters  - because autowiring doesn't work without. (why?)
                        def easygridService
                        public EasygridService getEasygridService(){
                            return easygridService;
                        }
                        public void setEasygridService(EasygridService easygridService){
                            this.easygridService=easygridService;
                        }

                        def getGridsConfig(){
                            easygridService.initGrids(${source.nameWithoutPackage})
                        }

                        //remove the stored params
                        def afterInterceptor = { model ->
                           org.grails.plugin.easygrid.EasygridContextHolder.resetParams()
                        }
                        def getAfterInterceptor(){
                            afterInterceptor
                        }
                    }
                    /$)


            generalAST[1].fields.each {
                source.addField(it)
            }

            generalAST[1].methods.each {
                source.addMethod(it)
            }

            // for each grid - inject the 3 methods
            gridNames.each { gridName ->
                def gridAst = new AstBuilder().buildFromString(phase, false,
                        $/
                    package ${source.packageName}

                    import org.grails.plugin.easygrid.*

                    class ${source.nameWithoutPackage} {

                        // renders the elements to be displayed by the grid
                        def ${gridName}Rows () {
                            render easygridService.gridData(gridsConfig['${gridName}'])
                        }

                         // export the elements
                        def ${gridName}Export () {
                            easygridService.export(gridsConfig['${gridName}'])
                        }

                        //inline Edit
                        def ${gridName}InlineEdit (){
                          if(easygridService.supportsInlineEdit(gridsConfig['${gridName}'])){
                               def result = easygridService.inlineEdit(gridsConfig['${gridName}'])
//                               render(template: gridsConfig['${gridName}'].editRenderer, model: result?.model)
                               render(template: gridsConfig['${gridName}'].editRenderer)
                          }else{
                            throw new UnsupportedOperationException("Inline edit not available for this type of grid");
                          }
                        }
                    }
                    /$)
                gridAst[1].methods.each {
                    source.addMethod(it)
                }
            }
        } catch (Exception e) {
            e.printStackTrace()
            log.error("error adding grid methods to: ${source.nameWithoutPackage}", e)
            throw e
        }
    }
}