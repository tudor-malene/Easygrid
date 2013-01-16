package org.grails.plugin.easygrid

import groovy.util.logging.Slf4j
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.messages.LocatedMessage
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * AST transformation that adds specific grid methods to annotated controllers
 *
 * @author <a href='mailto:tudor.malene@gmail.com'>Tudor Malene</a>
 */
@Slf4j
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class EasygridASTTransformation extends AbstractASTTransformation {

    void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source)
        addGridStuff(nodes[1])
    }

    /**
     * adds methods for displaying eachGrid
     * @param source
     * @return
     */
    void addGridStuff(ClassNode source) {
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
                    import org.slf4j.Logger
                    import org.slf4j.LoggerFactory

                    class ${source.nameWithoutPackage} {

                        final static Logger easyGridLogger = LoggerFactory.getLogger(${source.nameWithoutPackage}.class)

                        // added getters and setters  - because autowiring doesn't work without. (why?)
                        def easygridService
                        public EasygridService getEasygridService(){
                            return easygridService
                        }
                        public void setEasygridService(EasygridService easygridService){
                            this.easygridService=easygridService
                        }

                        def autocompleteService
                        public AutocompleteService getAutocompleteService(){
                            return autocompleteService
                        }
                        public void setAutocompleteService(AutocompleteService autocompleteService){
                            this.autocompleteService=autocompleteService
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

            // for each grid - inject the  methods
            gridNames.each { gridName ->
                def gridAst = new AstBuilder().buildFromString(phase, false,
                        $/
                    package ${source.packageName}

                    import org.grails.plugin.easygrid.*

                    class ${source.nameWithoutPackage} {

                        // renders the html code
                        def ${gridName}Html () {
                            easyGridLogger.debug("entering ${gridName}Html")
                            def gridConfig = gridsConfig['${gridName}']
                            def model = easygridService.htmlGridDefinition(gridConfig.deepClone())
                            if (model) {
                                model.attrs = [id : "$${gridConfig.id}" ]
                                render(template: gridConfig.gridRenderer, model: model)
                            }
                        }

                        // renders the elements to be displayed by the grid
                        def ${gridName}Rows () {
                            easyGridLogger.debug("entering ${gridName}Rows")
                            render easygridService.gridData(gridsConfig['${gridName}'].deepClone())
                        }

                         // export the elements
                        def ${gridName}Export () {
                            easyGridLogger.debug("entering ${gridName}Export")
                            easygridService.export(gridsConfig['${gridName}'].deepClone())
                        }

                        //inline Edit
                        def ${gridName}InlineEdit (){
                            easyGridLogger.debug("entering ${gridName}InlineEdit")
                          if(easygridService.supportsInlineEdit(gridsConfig['${gridName}'].deepClone())){
                               def result = easygridService.inlineEdit(gridsConfig['${gridName}'].deepClone())
//                               render(template: gridsConfig['${gridName}'].editRenderer, model: result?.model)
                               render(template: gridsConfig['${gridName}'].editRenderer)
                          }else{
                            throw new UnsupportedOperationException("Inline edit not available for this type of grid: ${gridName}")
                          }
                        }

                        //autocomplete
                        def ${gridName}AutocompleteResult (){
                          easyGridLogger.debug("entering ${gridName}AutocompleteResult")

                          if(autocompleteService.supportsAutocomplete(gridsConfig['${gridName}'].deepClone())){
                              render autocompleteService.searchedElementsJSON(gridsConfig['${gridName}'].deepClone())
                          }else{
                            throw new UnsupportedOperationException("Autocomplete not available for this grid: ${gridName}")
                          }
                        }

                        def ${gridName}SelectionLabel (){
                          easyGridLogger.debug("entering ${gridName}SelectionLabel")
                          if(autocompleteService.supportsAutocomplete(gridsConfig['${gridName}'])){
                              render autocompleteService.label(gridsConfig['${gridName}'].deepClone())
                          }else{
                            throw new UnsupportedOperationException("Autocomplete not available for this grid: ${gridName}")
                          }
                        }
                    }
                    /$)
                gridAst[1].methods.each {
                    source.addMethod(it)
                }
            }
        } catch (any) {
            String messageText = "error adding grid methods to: ${source.nameWithoutPackage}"
//            Token token = Token.newString(
//                    expression.getText(),
//                    expression.getLineNumber(),
//                    expression.getColumnNumber())
            LocatedMessage message = new LocatedMessage(messageText, Token.NULL, sourceUnit)

            sourceUnit
                    .getErrorCollector()
                    .addError(message);
        }
    }
}
