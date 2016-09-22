/*
 * #%L
 * OME Bio-Formats package for reading and converting biological file formats.
 * %%
 * Copyright (C) 2016 Open Microscopy Environment:
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

package loci.formats.utests.in;

import java.util.List;
import java.util.ArrayList;

import loci.formats.CoreMetadata;
import loci.formats.in.ND2Handler;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link ND2Handler}.
 */
public class ND2HandlerTest {

  private List<CoreMetadata> core = new ArrayList<CoreMetadata>();
  private ND2Handler handler = new ND2Handler(core, 1);

  @Test
  public void testParsePhysicalSizeZ()
  {
    handler.parseKeyAndValue("- Step", "", "");
    assertEquals(handler.getPixelSizeZ(), null);
  }
}
