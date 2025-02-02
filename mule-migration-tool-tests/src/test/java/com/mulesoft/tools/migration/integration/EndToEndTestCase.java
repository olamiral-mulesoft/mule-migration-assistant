/*
 * Copyright (c) 2020, Mulesoft, LLC. All rights reserved.
 * Use of this source code is governed by a BSD 3-Clause License
 * license that can be found in the LICENSE.txt file.
 */
package com.mulesoft.tools.migration.integration;

import static java.io.File.separator;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.System.getProperty;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.junit.Assert.fail;
import static org.mule.runtime.deployment.model.api.application.ApplicationDescriptor.MULE_APPLICATION_CLASSIFIER;
import static org.mule.test.infrastructure.maven.MavenTestUtils.installMavenArtifact;

import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

import com.mulesoft.mule.distributions.server.AbstractEeAppControl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the whole migration process, starting with a Mule 3 source config, migrating it to Mule 4, packaging and deploying it to
 * a standalone runtime.
 */
public abstract class EndToEndTestCase extends AbstractEeAppControl {

  private static final String DELETE_ON_EXIT = getProperty("mule.test.deleteOnExit");

  protected static final String ONLY_MIGRATE = getProperty("mule.test.migratorOnly");

  private static final String RUNTIME_VERSION = getProperty("mule.version");

  private static final String DEBUG_RUNNER = getProperty("mule.test.debugRunner");
  private static final String DEBUG_MUNIT = getProperty("mule.test.debugMUnit");

  private static final String MMT_UPLOAD_REPORT = getProperty("mule.test.mmtUploadReport");

  @ClassRule
  public static TemporaryFolder mmaBinary = new TemporaryFolder();

  private static File mmaBinaryFolder;

  private static Logger logger = LoggerFactory.getLogger(EndToEndTestCase.class);

  @Rule
  public TemporaryFolder migrationResult = new TemporaryFolder();

  @BeforeClass
  public static void prepareMma() throws IOException {
    mmaBinaryFolder = mmaBinary.newFolder();

    try (ZipFile zip = new ZipFile(new File(getProperty("migrator.runner")))) {
      Enumeration<? extends ZipEntry> zipFileEntries = zip.entries();
      ZipEntry root = zipFileEntries.nextElement();
      File mmaRootFile = new File(mmaBinaryFolder, root.getName());
      mmaRootFile.mkdirs();
      while (zipFileEntries.hasMoreElements()) {
        ZipEntry entry = zipFileEntries.nextElement();
        File destFile = new File(mmaBinaryFolder, entry.getName());
        if (entry.isDirectory()) {
          destFile.mkdir();
        } else {
          copyInputStreamToFile(zip.getInputStream(entry), destFile);
        }
      }
    }

  }

  public void simpleCase(String appName, String... muleArgs) throws Exception {
    String outPutPath = migrate(appName);

    if (ONLY_MIGRATE != null) {
      return;
    }

    BundleDescriptor migratedAppDescriptor = new BundleDescriptor.Builder().setGroupId("org.mule.migrated")
        .setArtifactId(appName).setVersion("1.0.0-M4-SNAPSHOT").setClassifier(MULE_APPLICATION_CLASSIFIER).build();

    Properties props = new Properties();
    // We do this so that the tests that run MUnit tests use the intended runtime
    props.put("runtimeVersion", RUNTIME_VERSION);
    if ("true".equals(DEBUG_MUNIT)) {
      props.put("munit.debug.default", "true");
    }

    startStopMule(migratedAppDescriptor, installMavenArtifact(outPutPath, migratedAppDescriptor, props), muleArgs);
  }

  protected void startStopMule(BundleDescriptor migratedAppDescriptor, File migratedAppArtifact, String... muleArgs) {
    try {
      getMule().start(muleArgs);
      assertAppNotDeployed(migratedAppDescriptor.getArtifactFileName());
      deployArtifactsToMule(migratedAppArtifact);
      assertAppIsDeployed(migratedAppDescriptor.getArtifactFileName());
    } finally {
      getMule().stop();
      if (isEmpty(DELETE_ON_EXIT) || parseBoolean(DELETE_ON_EXIT)) {
        getMule().undeployAll();
      }
    }
  }

  protected void deployArtifactsToMule(File migratedAppArtifact) {
    getMule().deploy(migratedAppArtifact.getAbsolutePath());
  }

  /**
   * Runs the migration tool on the referenced project.
   *
   * @param projectName
   * @return the path where the migrated project is located.
   * @throws URISyntaxException
   * @throws IOException
   * @throws InterruptedException
   */
  protected String migrate(String projectName, String... additionalParams)
      throws URISyntaxException, IOException, InterruptedException {
    String projectBasePath =
        new File(EndToEndTestCase.class.getClassLoader().getResource("e2e/" + projectName).toURI()).getAbsolutePath();

    String outPutPath = migrationResult.getRoot().toPath().resolve(projectName).toAbsolutePath().toString();

    // Run migration tool
    final List<String> command = buildRunnerCommand(projectBasePath, outPutPath);
    for (String additionalParam : additionalParams) {
      command.add(additionalParam);
    }
    ProcessBuilder pb = new ProcessBuilder(command);

    pb.redirectErrorStream(true);
    Process p = pb.start();

    Runtime.getRuntime().addShutdownHook(new Thread(p::destroy));

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        System.out.println("Migrator: " + line);
      }
    }

    if (p.waitFor() != 0) {
      fail("Migration failed");
    }
    return outPutPath;
  }

  private List<String> buildRunnerCommand(String projectBasePath, String outPutPath) {
    final List<String> command = new ArrayList<>();
    command.add("java");

    if (DEBUG_RUNNER != null) {
      command.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000");
    }

    if (!Boolean.valueOf(MMT_UPLOAD_REPORT)) {
      command.add("-Dmmt.disableUsageStatistics");
    }

    command.add("-jar");
    command.add(mmaBinaryFolder.getAbsolutePath() + separator
        + "mule-migration-assistant-runner-" + getProperty("mma.version") + ".jar");
    command.add("-projectBasePath");
    command.add(projectBasePath);
    command.add("-destinationProjectBasePath");
    command.add(outPutPath);
    command.add("-muleVersion");
    command.add(RUNTIME_VERSION);

    return command;
  }

  @After
  public void copyMigrationResult() {
    File migratedApps = new File("apps", getClass().getSimpleName());
    try {
      copyDirectory(migrationResult.getRoot(), migratedApps);
    } catch (IOException e) {
      logger.warn("Could not copy migration result.");
    }
  }

  @Override
  public int getTestTimeoutSecs() {
    if (DEBUG_RUNNER == null) {
      return super.getTestTimeoutSecs() * 2;
    } else {
      return MAX_VALUE / 1000;
    }
  }
}
