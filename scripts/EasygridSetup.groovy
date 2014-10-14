includeTargets << grailsScript("_GrailsInit")

target(easygridSetup: "The description of the script goes here!") {
    // create EasygridConfig file
    File configFile = new File(basedir, 'grails-app/conf/EasygridConfig.groovy')
    Boolean writeConfig = (!configFile.exists()) ?: promptForOverwrite(configFile.name)
    if (writeConfig) {
        configFile.createNewFile() //If the file doesn't exist, create it
        configFile.setText("""
            easygrid{
               defaults{

                //un-comment if you use spring security or implement your own with your framework
                securityProvider = { grid, oper ->
                    return true
/*
                    if (!grid.roles) {
                        return true
                    }
                    def grantedRoles
                    if (Map.isAssignableFrom(grid.roles.getClass())) {
                        grantedRoles = grid.roles.findAll { op, role -> oper == op }.collect { op, role -> role }
                    } else if (List.isAssignableFrom(grid.roles.getClass())) {
                        grantedRoles = grid.roles
                    } else {
                        grantedRoles = [grid.roles]
                    }
                    SpringSecurityUtils.ifAllGranted(grantedRoles.join(','))
*/
                }

               }
            }
            """.stripIndent(), 'UTF-8')
    }

    // copy the templates
    copyTemplates(['jqGridRenderer', 'classicGridRenderer', 'dataTablesGridRenderer', 'visualizationGridRenderer', 'filterFormRenderer'])
}

setDefaultTarget(easygridSetup)

private copyTemplates(templates) {
    try {
        File dest = new File(basedir, 'grails-app/views/templates/easygrid')
        dest.mkdir() //Create dir if it doesn't exist
        Boolean writeTemplates = promptForOverwrite("the EasyGrid templates")
        templates.each {
            ant.copy file: new File(easygridPluginDir, "grails-app/views/templates/easygrid/_${it}.gsp"), todir: dest, overwrite: writeTemplates 
        }
    } catch (Exception e) {
        e.printStackTrace()
    }
}

private promptForOverwrite(fileName) {
    String propertyName = fileName.replaceAll('\\s','')
    Ant.input(message: """
        You already have a copy of $fileName in your project, possibly from a prior release of the plugin.
        Do you want to overwrite it with the default file?
        Any changes you have made to $fileName will be lost.""",
        validargs:"y,n",
        addproperty:"easygrid.overwrite.${propertyName}.warning")
    Ant.antProject.properties."easygrid.overwrite.${propertyName}.warning" == 'y'
}
