/*
 * #%L
 * OME Bio-Formats package for reading and converting biological file formats.
 * %%
 * Copyright (C) 2005 - 2017 Open Microscopy Environment:
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import loci.common.RandomAccessInputStream;
import loci.formats.CoreMetadata;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.meta.MetadataStore;
import loci.formats.tiff.IFD;
import loci.formats.tiff.PhotoInterp;
import loci.formats.tiff.TiffParser;
import ome.units.UNITS;
import ome.units.quantity.Length;
import loci.formats.meta.IMinMaxStore;
import java.util.Arrays;


/**
 * PyramidTiffReader is the file format reader for pyramid TIFFs.
 */
public class PyramidTiffReader extends BaseTiffReader {

  // -- Constants --

  /** Logger for this class. */
  private static final Logger LOGGER =
    LoggerFactory.getLogger(PyramidTiffReader.class);

  // -- Fields --

  // -- Constructor --

  /** Constructs a new pyramid TIFF reader. */
  public PyramidTiffReader() {
    super("Pyramid TIFF", new String[] {"tif", "tiff"});
    domains = new String[] {FormatTools.EM_DOMAIN};
    suffixSufficient = false;
    suffixNecessary = false;
    equalStrips = true;
    noSubresolutions = true;
    canSeparateSeries = false;
  }

  // -- IFormatReader API methods --

  /* @see loci.formats.IFormatReader#isThisType(RandomAccessInputStream) */
  @Override
  public boolean isThisType(RandomAccessInputStream stream) throws IOException {
    TiffParser parser = new TiffParser(stream);
    parser.setAssumeEqualStrips(equalStrips);
    IFD ifd = parser.getFirstIFD();
    if (ifd == null) return false;
    String software = ifd.getIFDTextValue(IFD.SOFTWARE);
    if (software == null) return false;
    return software.indexOf("Faas") >= 0;
  }

  /**
   * @see loci.formats.IFormatReader#openBytes(int, byte[], int, int, int, int)
   */
  @Override
  public byte[] openBytes(int no, byte[] buf, int x, int y, int w, int h)
    throws FormatException, IOException
  {
    FormatTools.checkPlaneParameters(this, no, buf.length, x, y, w, h);
    int index = getCoreIndex();
    tiffParser.setAssumeEqualStrips(equalStrips);
    tiffParser.getSamples(ifds.get(index), buf, x, y, w, h);
    return buf;
  }

  /* @see loci.formats.IFormatReader#getOptimalTileWidth() */
  @Override
  public int getOptimalTileWidth() {
    FormatTools.assertId(currentId, true, 1);
    try {
      return (int) ifds.get(getCoreIndex()).getTileWidth();
    }
    catch (FormatException e) {
      LOGGER.debug("", e);
    }
    return super.getOptimalTileWidth();
  }

  /* @see loci.formats.IFormatReader#getOptimalTileHeight() */
  @Override
  public int getOptimalTileHeight() {
    FormatTools.assertId(currentId, true, 1);
    try {
      return (int) ifds.get(getCoreIndex()).getTileLength();
    }
    catch (FormatException e) {
      LOGGER.debug("", e);
    }
    return super.getOptimalTileHeight();
  }

  // -- Internal BaseTiffReader API methods --

  /* @see loci.formats.in.BaseTiffReader#initStandardMetadata() */
  @Override
  protected void initStandardMetadata() throws FormatException, IOException {
    int seriesCount = ifds.size();

    // repopulate core metadata
    core.clear();
    core.add();
    for (int s=0; s<seriesCount; s++) {
      CoreMetadata ms = new CoreMetadata();
      core.add(0, ms);

      if (s == 0) {
        ms.resolutionCount = seriesCount;
      }

      IFD ifd = ifds.get(s);

      PhotoInterp p = ifd.getPhotometricInterpretation();
      int samples = ifd.getSamplesPerPixel();
      ms.rgb = samples > 1 || p == PhotoInterp.RGB;

      long numTileRows = ifd.getTilesPerColumn() - 1;
      long numTileCols = ifd.getTilesPerRow() - 1;

      ms.sizeX = (int) ifd.getImageWidth();
      ms.sizeY = (int) ifd.getImageLength();
      ms.sizeZ = 1;
      ms.sizeT = 1;
      ms.sizeC = ms.rgb ? samples : 1;
      ms.littleEndian = ifd.isLittleEndian();
      ms.indexed = p == PhotoInterp.RGB_PALETTE &&
        (get8BitLookupTable() != null || get16BitLookupTable() != null);
      ms.imageCount = 1;
      ms.pixelType = ifd.getPixelType();
      ms.metadataComplete = true;
      ms.interleaved = false;
      ms.falseColor = false;
      ms.dimensionOrder = "XYCZT";
      ms.thumbnail = s > 0;
    }
  }

  /* @see loci.formats.BaseTiffReader#initMetadataStore() */
  @Override
  protected void initMetadataStore() throws FormatException {
    super.initMetadataStore();

    MetadataStore store = makeFilterMetadata();
    
    if (store instanceof IMinMaxStore) {
        IMinMaxStore minMaxStore = (IMinMaxStore) store;
        LOGGER.info("Setting minmax");
        minMaxStore.setChannelGlobalMinMax(0, 3000, 8000, series);
    }
    
    IFD ifd = ifds.get(getCoreIndex());
    
    // min max values
    double min = ifd.getIFDIntValue(IFD.MIN_SAMPLE_VALUE);
    double max = ifd.getIFDIntValue(IFD.MAX_SAMPLE_VALUE);
    
    if (store instanceof IMinMaxStore) {
        IMinMaxStore minMaxStore = (IMinMaxStore) store;
        minMaxStore.setChannelGlobalMinMax(0, min, max, series);
    }
    
    // Get pixel size
    double x = ifd.getXResolution();
    double y = ifd.getYResolution();
    
    Length lx = FormatTools.getPhysicalSizeX(x, UNITS.NM);
    Length ly = FormatTools.getPhysicalSizeX(y, UNITS.NM);
    
    store.setPixelsPhysicalSizeX(lx, coreIndex);
    store.setPixelsPhysicalSizeY(ly, coreIndex);
    
    // Get conversion matrix from GeoDoubleParamsTag
    Object value = ifd.getIFDValue(34736);
    double[] matrix = null;
    if (value instanceof double[]) {
        matrix = (double[]) value;
    }
    
    // Store to non structured metadata
    addGlobalMeta("Pixel size x (nm)", x);
    addGlobalMeta("Pixel size y (nm)", y);
    addGlobalMeta("Min", min);
    addGlobalMeta("Max", max);
    addGlobalMeta("Conversion matrix", Arrays.toString(matrix));

    for (int i=0; i<getSeriesCount(); i++) {
      store.setImageName("Series " + (i + 1), i);
    }
  }

}
