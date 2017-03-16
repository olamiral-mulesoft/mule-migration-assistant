package com.mulesoft.munit.tools.migration;

import com.mulesoft.munit.tools.migration.task.MigrationTask;
import com.mulesoft.munit.tools.migration.task.steps.*;
import org.jdom2.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static com.mulesoft.munit.tools.migration.helpers.DocumentHelpers.getDocument;
import static com.mulesoft.munit.tools.migration.helpers.DocumentHelpers.restoreTestDocument;
import static org.junit.Assert.*;

public class MigrationJobTest {
    private MigrationJob migrationJob;
    private Document docRestoreFile1;
    private Document docRestoreFile2;

    @Before
    public void setUp() throws Exception {
        ArrayList<String> filePath1 = new ArrayList<String>();
        filePath1.add("src/test/resources/sample-file.xml");
        ArrayList<String> filePath2 = new ArrayList<String>();
        filePath2.add("src/test/resources/set-payload.xml");

        migrationJob = new MigrationJob();
        migrationJob.setDocuments(filePath1);

        docRestoreFile1 = getDocument(filePath1.get(0));
        docRestoreFile2 = getDocument(filePath2.get(0));
    }

    @Ignore
    @Test
    public void testExecuteEmptySteps() throws Exception {
        migrationJob.execute();
        assertNotNull(migrationJob);
    }

    @Ignore
    @Test
    public void checkStepExecution() throws Exception {

        MigrationTask task = new MigrationTask("//munit:test");
        MigrationStep step;

        step = new AddAttribute("description", "MyNewDescription2");

        task.addStep(step);

        migrationJob.addTask(task);
        migrationJob.execute();
    }

//    @Test
//    public void checkMultipleStepExecution() throws Exception {
//
//        MigrationTask task = new MigrationTask("//munit:test");
//        MigrationStep step;
//
//        step = new AddAttribute("description", "MyNewDescription4");
//
//        task.addStep(step);
//
//        step = new AddAttribute("enable", "false");
//
//        task.addStep(step);
//
//        migrationJob.addTask(task);
//        migrationJob.execute();
//
//        List<Element> nodesModified = getElementsFromDocument(migrationJob.getDocument(), "//munit:test");
//        assertEquals(34, nodesModified.size());
//    }

//    @Test
//    public void checkMoveSetMessagePayloadExecution() throws Exception {
//
//        migrationJob = new MigrationJob();
//        migrationJob.setDocuments(new ArrayList<String>(Arrays.asList("src/test/resources/set-payload.xml")));
//
//        SetTasksForSetMessageNodesMigration();
//
//        migrationJob.execute();
//
//        List<Element> nodesModified = getElementsFromDocument(migrationJob.getDocument(), "//munit:test/munit:set-event");
//        assertEquals(4, nodesModified.size());
//    }


//    @Test
//    public void changeAssertDSL() throws Exception {
//        migrationJob = new MigrationJob();
//        migrationJob.setDocuments(new ArrayList<String>(Arrays.asList("src/test/resources/set-payload.xml")));
//
//        SetTasksForAssertsNodesMigration();
//
//        migrationJob.execute();
//
//        List<Element> nodesModified = getElementsFromDocument(migrationJob.getDocument(), "//munit:test/assert:that");
//        assertEquals(14, nodesModified.size());
//    }

//    @Test
//    public void executeMultipleTasks() throws Exception {
//        migrationJob = new MigrationJob();
//        migrationJob.setDocuments(new ArrayList<String>(Arrays.asList("src/test/resources/set-payload.xml")));
//
//        SetTasksForAssertsNodesMigration();
//        SetTasksForSetMessageNodesMigration();
//
//        migrationJob.execute();
//
//        List<Element> nodesModified = getElementsFromDocument(migrationJob.getDocument(), "//munit:test/assert:that");
//        assertEquals(14, nodesModified.size());
//    }

    @Ignore
    @Test
    public void migrateMultipleFiles() throws Exception {

        ArrayList<String> files = new ArrayList<String>(Arrays.asList("src/test/resources/set-payload.xml","src/test/resources/sample-file.xml"));

        SetTasksForAssertsNodesMigration();
        SetTasksForSetMessageNodesMigration();

        migrationJob.setDocuments(files);
        migrationJob.execute();

    }

    @Test
    public void jobWithTasksOnConfigFile() throws Exception {
        ArrayList<String> files = new ArrayList<String>(Arrays.asList("src/test/resources/set-payload.xml","src/test/resources/sample-file.xml"));

        migrationJob.setDocuments(files);
        migrationJob.setConfigFilePath("src/test/resources/migrationPOJO.json");
        migrationJob.execute();
    }


    @After
    public void restoreFileState() throws Exception {
        restoreTestDocument(docRestoreFile1,"src/test/resources/sample-file.xml");
        restoreTestDocument(docRestoreFile2,"src/test/resources/set-payload.xml");
    }

    private void SetTasksForAssertsNodesMigration() {

        MigrationTask assertTask;
        MigrationStep step;

        assertTask  = new MigrationTask("//munit:test/*[contains(local-name(),'assert') or contains(local-name(),'fail') or contains(local-name(),'run-custom')]");
        step = new SetNodeNamespace("assert", "http://www.mulesoft.org/schema/mule/assert", "http://www.mulesoft.org/schema/mule/assert/current/mule-assert.xsd");
        assertTask.addStep(step);

        step = new ReplaceStringOnNodeName("assert-", "");
        assertTask.addStep(step);

        migrationJob.addTask(assertTask);

        assertTask = new MigrationTask("//munit:test/*[contains(local-name(),'payload-equals')]");
        step = new ReplaceNodesName("assert", "that");
        assertTask.addStep(step);
        step = new AddAttribute("expression", "#[payload]");
        assertTask.addStep(step);
        step = new UpdateAttributeName("expectedValue", "is");
        assertTask.addStep(step);

        migrationJob.addTask(assertTask);

        assertTask = new MigrationTask("//munit:test/*[contains(local-name(),'not-null')]");
        step = new ReplaceNodesName("assert", "that");
        assertTask.addStep(step);
        step = new AddAttribute("expression", "#[payload]");
        assertTask.addStep(step);
        step = new AddAttribute("is", "#[not(nullValue())]");
        assertTask.addStep(step);

        migrationJob.addTask(assertTask);

        assertTask = new MigrationTask("//munit:test/*[contains(local-name(),'null')]");
        step = new ReplaceNodesName("assert", "that");
        assertTask.addStep(step);
        step = new AddAttribute("expression", "#[payload]");
        assertTask.addStep(step);
        step = new AddAttribute("is", "#[nullValue()]");
        assertTask.addStep(step);

        migrationJob.addTask(assertTask);

        assertTask = new MigrationTask("//munit:test/*[contains(local-name(),'false')]");
        step = new ReplaceNodesName("assert", "that");
        assertTask.addStep(step);
        step = new UpdateAttributeName("condition", "expression");
        assertTask.addStep(step);
        step = new AddAttribute("is", "#[false]");
        assertTask.addStep(step);

        migrationJob.addTask(assertTask);

        assertTask = new MigrationTask("//munit:test/*[contains(local-name(),'true')]");
        step = new ReplaceNodesName("assert", "that");
        assertTask.addStep(step);
        step = new UpdateAttributeName("condition", "expression");
        assertTask.addStep(step);
        step = new AddAttribute("is", "#[true]");
        assertTask.addStep(step);

        migrationJob.addTask(assertTask);

        assertTask = new MigrationTask("//munit:test/*[contains(local-name(),'on-equals')]");
        step = new ReplaceNodesName("assert", "that");
        assertTask.addStep(step);
        step = new UpdateAttributeName("expectedValue", "is");
        assertTask.addStep(step);
        step = new UpdateAttributeName("actualValue", "expression");
        assertTask.addStep(step);

        migrationJob.addTask(assertTask);

        assertTask = new MigrationTask("//munit:test/*[contains(local-name(),'not-same')]");
        step = new ReplaceNodesName("assert", "that");
        assertTask.addStep(step);
        step = new UpdateAttributeName("expectedValue", "is");
        assertTask.addStep(step);
        step = new UpdateAttributeName("actualValue", "expression");
        assertTask.addStep(step);
        step= new NegateAttributeValue("is");
        assertTask.addStep(step);

        migrationJob.addTask(assertTask);
    }

    private void SetTasksForSetMessageNodesMigration() {

        MigrationTask task = new MigrationTask("//munit:test/munit:set");
        MigrationStep step;

        step = new ReplaceNodesName("munit", "set-event");
        task.addStep(step);
        step = new CreateChildNodeFromAttribute("payload");
        task.addStep(step);
        step = new MoveAttributeToChildNode("encoding", "payload");
        task.addStep(step);
        step = new MoveAttributeToChildNode("mimeType", "payload");
        task.addStep(step);

        migrationJob.addTask(task);

        task = new MigrationTask("/descendant::*[attribute::mimeType]");
        step = new UpdateAttributeName("mimeType", "mediaType");
        task.addStep(step);

        migrationJob.addTask(task);

        task = new MigrationTask("//munit:test/munit:set-event/munit:invocation-properties");
        step = new ReplaceNodesName("munit", "variables");
        task.addStep(step);

        migrationJob.addTask(task);

        task = new MigrationTask("//munit:test/munit:set-event/munit:variables/munit:invocation-property");
        step = new ReplaceNodesName("munit", "variable");
        task.addStep(step);

        migrationJob.addTask(task);
    }
}
