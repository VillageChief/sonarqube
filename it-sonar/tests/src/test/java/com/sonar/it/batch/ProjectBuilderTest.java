/*
 * Copyright (C) 2009-2012 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package com.sonar.it.batch;

import com.sonar.it.ItUtils;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test the extension point org.sonar.api.batch.bootstrap.ProjectBuilder
 * <p/>
 * A Sonar plugin can override the project definition injected by build-tool.
 * Example: C# plugin loads project structure and modules from Visual Studio metadata file.
 *
 * @since 2.9
 */
public class ProjectBuilderTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
      .addPlugin(ItUtils.locateTestPlugin("project-builder-plugin"))
      .build();

  @Test
  public void shouldDefineProjectFromPlugin() {
    MavenBuild build = MavenBuild.create(ItUtils.locateProjectPom("batch/project-builder"))
        .setCleanSonarGoals()
        .setProperty("sonar.dynamicAnalysis", "false");
    orchestrator.executeBuild(build);

    checkProject();
    checkSubProject("project-builder-module-a");
    checkSubProject("project-builder-module-b");
    checkFile("project-builder-module-a", "HelloA");
    checkFile("project-builder-module-b", "HelloB");
    assertThat(getResource("com.sonarsource.it.projects.batch:project-builder-module-b:[default].IgnoredFile")).isNull();
  }

  private void checkProject() {
    Resource project = getResource("com.sonarsource.it.projects.batch:project-builder");

    // name has been changed by plugin
    assertThat(project.getName()).isEqualTo("Name changed by plugin");

    assertThat(project).isNotNull();
    assertThat(project.getMeasureIntValue("files")).isEqualTo(2);
    assertThat(project.getMeasureIntValue("lines")).isGreaterThan(10);
  }

  private void checkSubProject(String subProjectKey) {
    Resource subProject = getResource("com.sonarsource.it.projects.batch:" + subProjectKey);
    assertThat(subProject).isNotNull();
    assertThat(subProject.getMeasureIntValue("files")).isEqualTo(1);
    assertThat(subProject.getMeasureIntValue("lines")).isGreaterThan(5);
  }

  private void checkFile(String subProjectKey, String fileKey) {
    Resource file = getResource("com.sonarsource.it.projects.batch:" + subProjectKey + ":[default]." + fileKey);
    assertThat(file).isNotNull();
    assertThat(file.getMeasureIntValue("lines")).isGreaterThan(5);
  }

  private Resource getResource(String key) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(key, "lines", "files"));
  }
}
