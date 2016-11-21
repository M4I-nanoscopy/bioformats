/*
 * #%L
 * OME Bio-Formats package for reading and converting biological file formats.
 * %%
 * Copyright (C) 2005 - 2015 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package loci.formats.in;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import loci.common.RandomAccessInputStream;
import loci.formats.CoreMetadata;
import loci.formats.FormatException;
import loci.formats.FormatReader;
import loci.formats.FormatTools;
import loci.formats.MetadataTools;
import loci.formats.meta.MetadataStore;

/**
 * FEIRawReader is the file format reader for FEI MAPS .raw files.
 */
public class FEIRawReader extends FormatReader {

  // -- Constants --

  // -- Fields --
    private int sizeX, sizeY;

  // -- Constructor --

  /** Constructs a new FEI reader. */
  public FEIRawReader() {
    super("FEI MAPS raw", "scios");
    suffixSufficient = true;
    domains = new String[] {FormatTools.SEM_DOMAIN};
  }

  // -- IFormatReader API methods --

  /**
   * @see loci.formats.IFormatReader#openBytes(int, byte[], int, int, int, int)
   */
  @Override
  public byte[] openBytes(int no, byte[] buf, int x, int y, int w, int h)
    throws FormatException, IOException
  {
    FormatTools.checkPlaneParameters(this, no, buf.length, x, y, w, h);
    in.seek(no * FormatTools.getPlaneSize(this));
    
    readPlane(in, x, y, w, h, buf);

    return buf;
  }

  /* @see loci.formats.IFormatReader#close(boolean) */
  @Override
  public void close(boolean fileOnly) throws IOException {
    super.close(fileOnly);
  }

  // -- Internal FormatReader API methods --

  /* @see loci.formats.FormatReader#initFile(String) */
  @Override
  protected void initFile(String id) throws FormatException, IOException {
    super.initFile(id);
    
    this.getDimensionsFromName(id);
    
    in = new RandomAccessInputStream(id);
    in.order(true);

    CoreMetadata m = core.get(0);

    m.sizeX = sizeX;
    m.sizeY = sizeY;

    // always one grayscale plane per file

    m.sizeZ = 1;
    m.sizeC = 1;
    m.sizeT = 1;
    m.imageCount = 1;
    m.littleEndian = true;
    m.pixelType = FormatTools.UINT16;
    m.rgb = false;
    m.indexed = false;
    m.interleaved = false;
    m.dimensionOrder = "XYCZT";

    MetadataStore store = makeFilterMetadata();
    MetadataTools.populatePixels(store, this);
  }
  
  /**
   * Reads 
   * 
   * @param name
   * @throws FormatException 
   */
  private void getDimensionsFromName(String name) throws FormatException {
    Pattern p = Pattern.compile("([0-9]+?)x([0-9]+?).scios");
    
    Matcher m = p.matcher(name);
    
    if ( !m.find() ) {
      throw new FormatException("No size pattern (****x***) found in file name");
    }
    
    sizeX = Integer.parseInt(m.group(1));
    sizeY = Integer.parseInt(m.group(2)); 
  }


}
