/*
    Copyright 2009 Lunatech Research

    This file is part of jax-doclets.

    jax-doclets is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    jax-doclets is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with jax-doclets.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.lunatech.doclets.jax.jaxrs;

import java.util.List;
import java.util.regex.Pattern;

import com.lunatech.doclets.jax.JAXDoclet;
import com.lunatech.doclets.jax.Utils;
import com.lunatech.doclets.jax.jaxrs.model.JAXRSApplication;
import com.lunatech.doclets.jax.jaxrs.model.PojoTypes;
import com.lunatech.doclets.jax.jaxrs.model.Resource;
import com.lunatech.doclets.jax.jaxrs.tags.ExcludeTaglet;
import com.lunatech.doclets.jax.jaxrs.tags.HTTPTaglet;
import com.lunatech.doclets.jax.jaxrs.tags.IncludeTaglet;
import com.lunatech.doclets.jax.jaxrs.tags.InputWrappedTaglet;
import com.lunatech.doclets.jax.jaxrs.tags.RequestHeaderTaglet;
import com.lunatech.doclets.jax.jaxrs.tags.ResponseHeaderTaglet;
import com.lunatech.doclets.jax.jaxrs.tags.ReturnWrappedTaglet;
import com.lunatech.doclets.jax.jaxrs.writers.DataObjectIndexWriter;
import com.lunatech.doclets.jax.jaxrs.writers.IndexWriter;
import com.lunatech.doclets.jax.jaxrs.writers.PojoClassWriter;
import com.lunatech.doclets.jax.jaxrs.writers.SummaryWriter;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.SourcePosition;
import com.sun.tools.doclets.formats.html.ConfigurationImpl;
import com.sun.tools.doclets.formats.html.HtmlDoclet;
import com.sun.tools.doclets.internal.toolkit.AbstractDoclet;
import com.sun.tools.doclets.internal.toolkit.taglets.LegacyTaglet;

public class JAXRSDoclet extends JAXDoclet<JAXRSConfiguration> {

  public final HtmlDoclet htmlDoclet = new HtmlDoclet();

  public static int optionLength(final String option) {
    if ("-jaxrscontext".equals(option)
        || "-matchingpojonamesonly".equals(option)
        || "-matchingresourcesonly".equals(option)
        || "-pathexcludefilter".equals(option)) {
      return 2;
    }
    if ("-disablehttpexample".equals(option)
        || "-disablejavascriptexample".equals(option)
        || "-enablepojojson".equals(option)) {
      return 1;
    }
    return HtmlDoclet.optionLength(option);
  }

  public static boolean validOptions(final String[][] options, final DocErrorReporter reporter) {
    if (!HtmlDoclet.validOptions(options, reporter)) {
      return false;
    }
    List<String> values = Utils.getOptions(options, "-pathexcludefilter");
    for(String value : values){
      try {
        Pattern.compile(value);
      } catch (Throwable t) {
        reporter.printError("Invalid pattern for '-pathexcludefilter' option: '"+value+"' (not a valid regex)");
        return false;
      }
    }
    return true;
  }

  public static LanguageVersion languageVersion() {
    return AbstractDoclet.languageVersion();
  }

  public static boolean start(final RootDoc rootDoc) {
    new JAXRSDoclet(rootDoc).start();
    return true;
  }

  public JAXRSDoclet(RootDoc rootDoc) {
    super(rootDoc);
    htmlDoclet.configuration.tagletManager.addCustomTag(new LegacyTaglet(new ResponseHeaderTaglet()));
    htmlDoclet.configuration.tagletManager.addCustomTag(new LegacyTaglet(new RequestHeaderTaglet()));
    htmlDoclet.configuration.tagletManager.addCustomTag(new LegacyTaglet(new HTTPTaglet()));
    htmlDoclet.configuration.tagletManager.addCustomTag(new LegacyTaglet(new ReturnWrappedTaglet()));
    htmlDoclet.configuration.tagletManager.addCustomTag(new LegacyTaglet(new InputWrappedTaglet()));
    htmlDoclet.configuration.tagletManager.addCustomTag(new LegacyTaglet(new IncludeTaglet()));
    htmlDoclet.configuration.tagletManager.addCustomTag(new ExcludeTaglet());
  }

  @Override
  protected JAXRSConfiguration makeConfiguration(ConfigurationImpl configuration) {
    return new JAXRSConfiguration(configuration);
  }

  public void start() {
    JAXRSApplication app = new JAXRSApplication(conf);
    Resource rootResource = app.getRootResource();

    PojoTypes types = new PojoTypes(conf);

    rootResource.write(this, conf, app, types);
    new IndexWriter(conf, app, this).write();
    new SummaryWriter(conf, app, this).write();

    if (conf.enablePojoJsonDataObjects) {
      types.resolveSubclassDtos();
      new DataObjectIndexWriter(conf, app, this, types).write();
      for (ClassDoc cDoc : types.getResolvedTypes()) {
        new PojoClassWriter(conf, app, cDoc, types, rootResource, this).write();
      }
    }
    Utils.copyResources(conf);
  }

  public void warn(String warning) {
    conf.parentConfiguration.root.printWarning(warning);
  }

  public void printError(SourcePosition position, String error) {
    conf.parentConfiguration.root.printError(position, error);
  }
}
