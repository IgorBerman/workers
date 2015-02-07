import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.tasks.Exec;
import static groovy.io.FileType.FILES

class PythonPlugin implements Plugin<Project> {
    public static final String TEST_TASK_NAME = 'test';
    public static final String BUILD_TASK_NAME ='build'
    void apply(Project project) {
        project.apply plugin: 'eclipse'
        project.task('generatePydevProject') {
            def pythonVersion = 'python 2.7'
            def contents = """<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<?eclipse-pydev version="1.0"?><pydev_project>
<pydev_property name="org.python.pydev.PYTHON_PROJECT_VERSION">${pythonVersion}</pydev_property>
<pydev_property name="org.python.pydev.PYTHON_PROJECT_INTERPRETER">backend</pydev_property>
<pydev_pathproperty name="org.python.pydev.PROJECT_SOURCE_PATH">
<path>/\${PROJECT_DIR_NAME}</path>
</pydev_pathproperty>
</pydev_project>
"""
            def outputFile = "${project.projectDir}/.pydevproject"
            doLast {
                new File(outputFile).write( contents, 'UTF-8' )
            }
        }

        project.eclipseProject.dependsOn 'generatePydevProject'

        project.eclipse.project {
            natures 'org.python.pydev.pythonNature'

            buildCommand 'org.python.pydev.PyDevBuilder'

            file {
                withXml { xmlProvider ->
                    Node projectNode = xmlProvider.asNode()
                    Node filter = projectNode.appendNode('filteredResources').appendNode('filter')
                    filter.appendNode('id', 1329642025568) 
                    filter.appendNode('name', '')
                    filter.appendNode('type', 22)
                    Node matcher = filter.appendNode('matcher')
                    matcher.appendNode('id', 'org.eclipse.ui.ide.multiFilter')
                    matcher.appendNode('arguments', '1.0-name-matches-false-false-*.pyc')
                }
            }
        }

        project.afterEvaluate(new Action<Project>() {
            public void execute(Project p) {
                Configuration configuration = project.getConfigurations().getByName("python");
                configuration.getDependencies().each() { dep ->
                    project.eclipse.project.referencedProjects dep.getDependencyProject().name
                }
            }
        })

        project.apply plugin: 'distribution'
        project.distributions.main.contents {
            exclude('**/*.pyc')
            from (project.name) {
                //filter test dir from distribution
                exclude 'test/**'
            }
        }

        Configuration configuration = project.getConfigurations().findByName("python");
        if (configuration == null) {
            configuration = project.getConfigurations().create("python");
        }

        def testTaskName = configureTest(project);
        configureBuild(project, testTaskName);      
    }
    private void configureBuild(Project project, String testTaskName) {
        String taskName = BUILD_TASK_NAME
        DefaultTask baseBuildTask = project.getTasks().findByName(taskName)
        if (baseBuildTask != null) {
            return;
        }
        DefaultTask buildTask = project.getTasks().create(taskName, DefaultTask.class);
        buildTask.setDescription("Assembles and tests python part of the project");
        buildTask.setGroup(BasePlugin.BUILD_GROUP);
        if (testTaskName != null) {
            buildTask.dependsOn(testTaskName);
        }
        buildTask.dependsOn('distZip');
    }
    private String configureTest(Project project) {
        //by convention, python projects will contain already name with _, java projects will have subdir with python code with _
        String pythonDir = "${project.name}".replace('-', '_')
        String testDir   = pythonDir + "/test"
        String integTestDir = pythonDir + "/integration_test"
        String outputFile = "build/test-results/test-results.xml"
        def intTests = Boolean.getBoolean('integration.test')
        def testsExist = new File("${project.name}/${testDir}").exists()
        def intTestsExist = new File("${project.name}/${integTestDir}").exists()
        if (!testsExist && !(intTests && intTestsExist)) {
            // project has no tests
            return null
        }
        
        String taskName = TEST_TASK_NAME
        DefaultTask baseTestTask = project.getTasks().findByName(taskName)
        if (baseTestTask != null) {
            taskName = taskName+"Python"
        }
        Exec testExecTask = project.getTasks().create(taskName, Exec.class);
        if (baseTestTask != null) {
            baseTestTask.dependsOn(taskName)
        }
        testExecTask.setDescription("Tests python part of the project.");
        testExecTask.setGroup(BasePlugin.BUILD_GROUP);        
        testExecTask.setWorkingDir(".")
        new File("${project.name}/build/test-results").mkdirs()
        def commandLineArgs = []
        if ("${System.env.VIRTUAL_ENV}" != "null") {
            commandLineArgs += "${System.env.VIRTUAL_ENV}/bin/nosetests"
        } else {
            commandLineArgs += "nosetests"
        }
        commandLineArgs += ['--with-xunit', "--xunit-file=${outputFile}"]

        if (testsExist) {
            commandLineArgs += testDir
        }
        if (intTests && intTestsExist) {
            commandLineArgs += integTestDir
        }        
        testExecTask.commandLine(commandLineArgs)
        testExecTask.doFirst {
            new File("${project.name}/build/test-results").mkdirs()
            println 'removing old pyc-s'
            new File("${project.name}/${pythonDir}").eachFileRecurse(FILES) {
                if(it.name.endsWith('.pyc')) {
                    it.delete()
                }
            }
        }

        def ft = project.fileTree(dir: project.name).matching {
            include('**/*.py')
        }
        testExecTask.inputs.files ft.files
        testExecTask.outputs.file new File(project.name, "build/test-results/test-results.xml")

        final Configuration configuration = project.getConfigurations().getByName("python");
        testExecTask.dependsOn(configuration.getTaskDependencyFromProjectDependency(true, taskName))

        project.afterEvaluate(new Action<Project>() {
            public void execute(Project p) {
                configuration.dependencies.each() { dep ->
                    def depFT = p.fileTree(dir: "../" + dep.getDependencyProject().name).matching {
                        include('**/*.py')
                        exclude('*/test/**')
                    }

                    testExecTask.inputs.files depFT.files
                }
            }
        })

        return taskName;
    }
}
