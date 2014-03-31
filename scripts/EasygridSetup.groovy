includeTargets << grailsScript("_GrailsInit")

target(easygridSetup: "The description of the script goes here!") {
    //todo - create EasygridConfig file
    File configFile = new File(basedir, 'grails-app/conf/EasygridConfig.groovy')
    if (!configFile.exists()) {
        configFile.createNewFile()
        configFile << """
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
            """.stripIndent()
    }

    // copy the templates
    copyTemplates(['jqGridRenderer', 'classicGridRenderer', 'dataTablesGridRenderer', 'visualizationGridRenderer', 'filterFormRenderer'])


}

setDefaultTarget(easygridSetup)

private copyTemplates(templates) {
    try {
        File dest = new File(basedir, 'grails-app/views/templates/easygrid')
        if (!dest.exists()) {
            dest.mkdir()
        }
        templates.each {
            ant.copy file: new File(easygridPluginDir, "grails-app/views/templates/easygrid/_${it}.gsp"), todir: dest, overwrite: false
        }
    } catch (Exception e) {
        e.printStackTrace()
    }
}
