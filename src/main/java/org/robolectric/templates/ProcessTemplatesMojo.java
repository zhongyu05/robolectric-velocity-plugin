package org.robolectric.templates;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.VelocityException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Plugin used to process Robolectric shadow template files.
 *
 * Inspired by: https://code.google.com/p/velocity-maven-plugin/
 */
@Mojo(name = "process-templates")
public class ProcessTemplatesMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  @Parameter(defaultValue = "${project.build.directory}")
  private File outputDirectory;

  @Parameter(required = true, readonly = true)
  private Integer apiLevel;

  @Parameter(required = true, readonly = true)
  private FileSet templateFiles;

  public void execute() throws MojoExecutionException {
    try {
      Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM, new LogHandler(this));
      Velocity.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH, project.getBasedir().getAbsolutePath());

      Velocity.init();

      VelocityContext context = new VelocityContext();
      context.put("apiLevel", apiLevel);
      if (apiLevel >= 21) {
        context.put("ptrClass", "long");
        context.put("ptrClassBoxed", "Long");
      } else {
        context.put("ptrClass", "int");
        context.put("ptrClassBoxed", "Integer");
      }

      for (File file : getFiles()) {
        processTemplate(context, file);
      }

    } catch (Exception e) {
      throw new MojoExecutionException("Error processing file", e);
    }
  }

  private List<File> getFiles() throws IOException {
    final FileSetManager manager = new FileSetManager();

    final List<File> files = new ArrayList<>();
    for (String file : manager.getIncludedFiles(templateFiles)) {
      files.add(new File(file));
    }

    return files;
  }

  private void processTemplate(VelocityContext context, File file) throws VelocityException, MojoExecutionException, IOException {
    try {
      final File inputFile = new File(templateFiles.getDirectory(), file.getPath());
      getLog().debug("Input file: " + inputFile.getPath());

      final StringWriter sw = new StringWriter();
      Template template = Velocity.getTemplate(inputFile.getPath(), "UTF-8");
      template.merge(context, sw);

      final File outputFile = new File(outputDirectory.getAbsoluteFile(), file.getPath().replace(templateFiles.getDirectory(), "").replace(".vm", ""));
      getLog().debug("Output file: " + outputFile.getPath());

      outputFile.getParentFile().mkdirs();
      outputFile.createNewFile();

      try (FileOutputStream os = new FileOutputStream(outputFile)) {
        os.write(sw.toString().getBytes("UTF-8"));
      }
    } catch (Exception e) {
      throw new MojoExecutionException("Error processing template file: " + file.getAbsolutePath() + ": " + e);
    }
  }

  public void setProject(MavenProject project) {
    this.project = project;
  }

  public void setApiLevel(Integer apiLevel) {
    this.apiLevel = apiLevel;
  }

  public void setOutputDirectory(File outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  public void setTemplateFiles(FileSet templateFiles) {
    this.templateFiles = templateFiles;
  }
}