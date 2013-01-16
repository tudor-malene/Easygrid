//includeTargets << grailsScript("_GrailsInit")
//
//target(main: "The description of the script goes here!") {
//    // TODO: Implement script here
//}
//
//setDefaultTarget(main)


eventCompileStart = { target ->
    compileAST(basedir, classesDirPath)
}

def compileAST(def srcBaseDir, def destDir) {
    ant.sequential {
        echo "Precompiling AST Transformations ..."
        echo "src ${srcBaseDir} ${destDir}"
        path id: "grails.compile.classpath", compileClasspath
        def classpathId = "grails.compile.classpath"
        mkdir dir: destDir
        groovyc(destdir: destDir,
                srcDir: "$srcBaseDir/src/ast",
                classpathref: classpathId,
                verbose: grailsSettings.verboseCompile,
                stacktrace: "yes",
                encoding: "UTF-8")
        echo "done precompiling AST Transformations"
    }
}