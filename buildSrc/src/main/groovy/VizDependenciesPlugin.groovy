import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.DefaultTask;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.Exec;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.tasks.Exec;

class VizDependenciesPlugin implements Plugin<Project> {
    public static final String TASK_NAME ='vizDependencies'
    
    def dep(project, scannedDependencies, projectDependencies) {
        if (scannedDependencies.contains(project)) {
            return
        }
        def allDependencies = []
        allDependencies.addAll(project.configurations.compile.dependencies)
        allDependencies.addAll(project.configurations.runtime.dependencies)
        allDependencies.each {dependency ->
            if(dependency instanceof ProjectDependency) {
                def projectDep = dependency.getDependencyProject()
                projectDependencies.add([project.name, projectDep.name])
                dep(projectDep, scannedDependencies, projectDependencies)
                scannedDependencies.add(project)
            }
        }
    }
    
    void apply(Project project) {
        configureDependenciesViz(project);                
    }
    private void configureDependenciesViz(Project project) {
        DefaultTask task = project.getTasks().create(TASK_NAME, DefaultTask.class);
        task.setDescription("creates png for project's dependencies, needs dot to be installed (sudo apt-get install graphviz)");
        task.doLast {
            def scannedDependencies = [] as Set
            def projectDependencies = [] as Set
            dep(project, scannedDependencies, projectDependencies)
            
            def dotGraph = "digraph Compile { \n"
            projectDependencies.each{ projectName, depProjectName ->
                dotGraph += "\"$projectName\" -> \"$depProjectName\"\n"
            }
            def outputDir = "${project.buildDir}/reports/project-dependencies"
            new java.io.File(outputDir).mkdirs()
            dotGraph += "}"
            def pw = new java.io.File("${outputDir}/${project.name}.dot").newPrintWriter()
            pw.print(dotGraph)
            pw.close()
            def command = "dot -Tpng ${outputDir}/${project.name}.dot -O"
            println command
            def proc = command.execute()                 
            proc.waitFor()    
        }
    }
}
