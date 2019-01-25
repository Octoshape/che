/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.factory.server.urlfactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.che.api.factory.server.urlfactory.URLFetcher.MAXIMUM_READ_BYTES;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import com.google.common.base.Strings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Testing {@link URLFetcher}
 *
 * @author Florent Benoit
 */
@Listeners(MockitoTestNGListener.class)
public class URLFetcherTest {

  /** Instance to test. */
  @InjectMocks private URLFetcher URLFetcher;

  /** Check that when url is null, NPE is thrown */
  @Test(expectedExceptions = NullPointerException.class)
  public void checkNullURL() throws Exception {
    URLFetcher.fetch((String) null);
  }

  /** Check that when url exists the content is retrieved */
  @Test
  public void checkGetContent() throws Exception {

    // test to download this class object
    URL urlJson = getClass().getClassLoader().getResource(".che.json");
    Assert.assertNotNull(urlJson);

    String content = URLFetcher.fetch(urlJson.toString());
    assertEquals(content, "Hello");
  }

  /** Check when url is invalid */
  @Test(
      expectedExceptions = IOException.class,
      expectedExceptionsMessageRegExp = "no protocol: hello world")
  public void checkUrlFileIsInvalid() throws Exception {
    String result = URLFetcher.fetch("hello world");
    assertNull(result);
  }

  /** Check Sanitizing of Git URL works */
  @Test
  public void checkDotGitRemovedFromURL() throws Exception {
    String result = URLFetcher.sanitized("https://github.com/acme/demo.git");
    assertEquals("https://github.com/acme/demo", result);

    result = URLFetcher.sanitized("http://github.com/acme/demo.git");
    assertEquals("http://github.com/acme/demo", result);
  }

  /** Check that when url doesn't exist */
  @Test(
      expectedExceptions = IOException.class,
      expectedExceptionsMessageRegExp = ".* \\(No such file or directory\\)")
  public void checkMissingContent() throws Exception {

    // test to download this class object
    URL urlJson = getClass().getClassLoader().getResource(".che.json");
    Assert.assertNotNull(urlJson);

    // add extra path to make url not found
    URLFetcher.fetch(urlJson.toString() + "-invalid");
  }

  /** Check when we reach custom limit */
  @Test
  public void checkPartialContent() throws Exception {
    URL urlJson = getClass().getClassLoader().getResource(".che.json");
    Assert.assertNotNull(urlJson);

    String content = new OneByteURLFetcher().fetch(urlJson.toString());
    assertEquals(content, "Hello".substring(0, 1));
  }

  /** Check when we reach custom limit */
  @Test
  public void checkDefaultPartialContent() throws IOException {
    URLConnection urlConnection = Mockito.mock(URLConnection.class);
    String originalContent = Strings.padEnd("", (int) MAXIMUM_READ_BYTES, 'a');
    String extraContent = originalContent + "----";
    when(urlConnection.getInputStream())
        .thenReturn(new ByteArrayInputStream(extraContent.getBytes(UTF_8)));
    String readcontent = URLFetcher.fetch(urlConnection);
    // check extra content has been removed as we keep only first values
    assertEquals(readcontent, originalContent);
  }

  /** Limit to only one Byte. */
  static class OneByteURLFetcher extends URLFetcher {
    /** Override the limit */
    @Override
    protected long getLimit() {
      return 1;
    }
  }
}
