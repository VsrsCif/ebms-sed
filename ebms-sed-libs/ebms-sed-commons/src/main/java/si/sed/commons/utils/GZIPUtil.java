/*
 * Copyright 2016, Supreme Court Republic of Slovenia
 * 
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * 
 * https://joinup.ec.europa.eu/software/page/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package si.sed.commons.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author sluzba
 */
public class GZIPUtil {

  /**
   *
   * @param srcFn
   * @param trgFn
   * @throws IOException
   */
  public void compressGZIP(final File srcFn, final File trgFn) throws IOException {

    try (FileInputStream fis = new FileInputStream(srcFn);
        GZIPOutputStream gos = new GZIPOutputStream(new FileOutputStream(trgFn))) {
      compressGZIP(fis, gos);
    }

  }

  /**
   *
   * @param src
   * @param trg
   * @throws IOException
   */
  public void compressGZIP(final InputStream src, final GZIPOutputStream trg) throws IOException {
    final byte[] buffer = new byte[1024];

    int i;
    while ((i = src.read(buffer)) > 0) {
      trg.write(buffer, 0, i);
    }
    src.close();
    trg.finish();
    trg.close();
  }

  /**
   *
   * @param srcFn
   * @param trgFn
   * @throws IOException
   */
  public void decompressGZIP(final File srcFn, final File trgFn) throws IOException {

    try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(srcFn));
        FileOutputStream fos = new FileOutputStream(trgFn)) {
      decompressGZIP(gis, fos);
    }

  }

  private void decompressGZIP(final GZIPInputStream gis, final OutputStream os) throws IOException {

    final byte[] buffer = new byte[1024];
    int i;
    while ((i = gis.read(buffer)) > 0) {
      os.write(buffer, 0, i);
    }
    gis.close();
    os.close();
  }

}
