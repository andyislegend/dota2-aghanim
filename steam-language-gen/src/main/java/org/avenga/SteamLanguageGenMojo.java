package org.avenga;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.avenga.constant.Constant;
import org.avenga.generator.JavaGen;
import org.avenga.parser.LanguageParser;
import org.avenga.parser.node.ClassNode;
import org.avenga.parser.node.EnumNode;
import org.avenga.parser.node.Node;
import org.avenga.parser.token.TokenAnalyzer;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mojo(name = "steam-language-gen", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class SteamLanguageGenMojo extends AbstractMojo {

    /**
     * Specify base package name of the steamd generated java classes.
     *
     * @parameter property="packageName" default-value="com.avenga.steamclient"
     */
    @Parameter(property = "packageName", defaultValue = "com.avenga.steamclient")
    private String packageName;

    /**
     * Specify directory of the generated classes from Steam Language (*.steamd) files.
     * Output directory will be marked as source directory automatically.
     *
     * @parameter property="outputDirectory" default-value="${project.build.directory}/generated/source/steamd/main/java"
     */
    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/generated/source/steamd/main/java")
    private File outputDirectory;

    /**
     * Specify directory with Steam Language (*.steamd) files.
     *
     * @parameter property="inputDirectory" default-value="${project.basedir}/src/main/steamd"
     */
    @Parameter(property = "inputDirectory", defaultValue = "${project.basedir}/src/main/steamd")
    private File inputDirectory;

    @Parameter(readonly = true, defaultValue = "${project}")
    private MavenProject project;

    public void execute() throws MojoExecutionException {
        try {
            File inputFile = new File(inputDirectory.getAbsolutePath(), getInputFileSuffix());
            File outputFile = new File(outputDirectory.getAbsolutePath(), getPackagePath());
            var tokens = LanguageParser.tokenizeString(IOUtils.toString(new FileInputStream(inputFile), StandardCharsets.UTF_8), inputFile.getName());
            var root = TokenAnalyzer.analyze(tokens, inputFile.getParent());

            FileUtils.deleteDirectory(outputFile);
            addCompileSourceRoot(outputDirectory);

            Set<String> flagEnums = new HashSet<>();
            getChildNodes(root, EnumNode.class).forEach(node ->
                    generateJavaClasses(node, packageName, "enums", outputFile, flagEnums));
            getChildNodes(root, ClassNode.class).forEach(node ->
                    generateJavaClasses(node, packageName, "generated", outputFile, flagEnums));
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private String getPackagePath() {
        return packageName.replaceAll("\\.", "\\" + File.separator);
    }

    private String getInputFileSuffix() {
        return getPackagePath() + File.separator + Constant.STEAMD_INPUT_FILE;
    }

    private List<Node> getChildNodes(Node root, Class<? extends Node> clazz) {
        return root.getChildNodes().stream().filter(child -> child.getClass().equals(clazz) ).collect(Collectors.toList());
    }

    private void addCompileSourceRoot(File outputDirectory) {
        getLog().info("Adding generated sources: " + outputDirectory);
        project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
    }

    private void generateJavaClasses(Node node, String packageName, String packageSuffix, File outputFile, Set<String> flagEnums) {
        var javaGen = new JavaGen(node, packageName + "." + packageSuffix, packageName, new File(outputFile, packageSuffix), flagEnums);
        javaGen.emit();
        javaGen.flush();
        javaGen.close();
    }
}
