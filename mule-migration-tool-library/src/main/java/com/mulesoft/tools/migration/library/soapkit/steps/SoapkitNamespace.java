/*
 * Copyright (c) 2020, Mulesoft, LLC. All rights reserved.
 * Use of this source code is governed by a BSD 3-Clause License
 * license that can be found in the LICENSE.txt file.
 */
package com.mulesoft.tools.migration.library.soapkit.steps;

import com.mulesoft.tools.migration.project.model.ApplicationModel;
import com.mulesoft.tools.migration.step.category.MigrationReport;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

/**
 * Migrates APIkit for SOAP namespace uri
 * @author Mulesoft Inc.
 * @since 1.0.0
 */
public class SoapkitNamespace extends AbstractSoapkitMigrationStep {

  private static final String OLD_SOAPKIT_NAMESPACE_URI = "http://www.mulesoft.org/schema/mule/apikit-soap";
  private static final String OLD_SOAPKIT_SCHEMA_LOCATION = OLD_SOAPKIT_NAMESPACE_URI + "/current/mule-apikit-soap.xsd";
  private static final Namespace OLD_SOAPKIT_NAMESPACE = Namespace.getNamespace(SOAPKIT_NS_PREFIX, OLD_SOAPKIT_NAMESPACE_URI);
  private static final String XPATH_SELECTOR = "//*[namespace-uri()='" + OLD_SOAPKIT_NAMESPACE_URI + "']";

  private boolean namespaceUpdated = false;

  @Override
  public String getDescription() {
    return "Update APIkit namespace";
  }

  public SoapkitNamespace() {
    this.setAppliedTo(XPATH_SELECTOR);
    getNamespacesContributions().add(OLD_SOAPKIT_NAMESPACE);
  }

  @Override
  public void execute(Element element, MigrationReport report) throws RuntimeException {
    final Document document = element.getDocument();

    if (!namespaceUpdated) {
      // Remove old namespace
      getApplicationModel().removeNameSpace(OLD_SOAPKIT_NAMESPACE, OLD_SOAPKIT_SCHEMA_LOCATION, document);

      // Add new namespace
      ApplicationModel.addNameSpace(SOAPKIT_NAMESPACE, SOAPKIT_SCHEMA_LOCATION, document);

      namespaceUpdated = true;
    }

    element.setNamespace(SOAPKIT_NAMESPACE);
  }

}
