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
import org.avenga.parser.token.TokenAnalyzer;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Mojo(name = "steam-language-gen", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class SteamLanguageGenMojo extends AbstractMojo {

    @Parameter(readonly = true, defaultValue = "${project}")
    private MavenProject project;

    public void execute() throws MojoExecutionException {
        try {
            File inputFile = new File(project.getBasedir().getAbsolutePath(), Constant.INPUT_DIR);
            File outputFile = new File(project.getBuild().getDirectory(), Constant.OUTPUT_DIR);
            var tokens = LanguageParser.tokenizeString(IOUtils.toString(new FileInputStream(inputFile), StandardCharsets.UTF_8), inputFile.getName());
            var root = TokenAnalyzer.analyze(tokens, inputFile.getParent());

            var enums = root.getChildNodes().stream().filter(child -> child instanceof EnumNode).collect(Collectors.toList());
            var classes = root.getChildNodes().stream().filter(child -> child instanceof ClassNode).collect(Collectors.toList());

            Set<String> flagEnums = new HashSet<>();

            FileUtils.deleteDirectory(outputFile);

            enums.forEach(node -> {
                var javaGen = new JavaGen(node, Constant.BASE_PACKAGE_NAME + ".enums", new File(outputFile, " enums"), flagEnums);
                javaGen.emit();
                javaGen.flush();
                javaGen.close();

            });

            classes.forEach(node -> {
                var javaGen = new JavaGen(node, Constant.BASE_PACKAGE_NAME + ".generated", new File(outputFile, "generated"), flagEnums);
                javaGen.emit();
                javaGen.flush();
                javaGen.close();

            });
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

    }
}
