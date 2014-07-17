
/*
**    Copyright (C) 2003-2014 Institute for Systems Biology
**                            Seattle, Washington, USA.
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.systemsbiology.biofabric.layoutTools;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/****************************************************************************
**
** Generate Random 3-Mode DAG
*/

public class GenRand3ModeDAG {

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  private final static int NUM_NODES_ = 100;
  private final static int NUM_LINKS_ = 200;
  private final static int NUM_NODE_CLASSES_ = 3;
  private final static int RAND_SEED_ = 17;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public GenRand3ModeDAG() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

   /***************************************************************************
  **
  ** Output Smaller sif file
  */

  public void writeSif(Set<Link> linkList, String outfile, String tag, Map<String, Integer> nodeToClass) throws IOException {

    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outfile), "UTF-8")));
    Iterator<Link> nit = linkList.iterator();
    while (nit.hasNext()) {
      Link toks = nit.next();
      Integer cs = nodeToClass.get(toks.getSrc());
      Integer cl = nodeToClass.get(toks.getTrg());
      out.print(toks.getSrc() + "@" + cs.toString());
      out.print("\t");
    
      out.print(cs);
      out.print("-");
     
      out.print(tag);
      out.print("-");
      out.print(cl);
      out.print("\t");
      out.println(toks.getTrg() + "@" + cl.toString());
    }
    out.close();
    return;
  }

 /***************************************************************************
  **
  ** Test frame
  */

  public void makeDaDag(Map<String, Integer> nodeToClass, Set<Link> links) {
    Random randGen = new Random(RAND_SEED_);
    while (links.size() < NUM_LINKS_) {
      int randie1 = randGen.nextInt(NUM_NODES_);
      int randie2 = randie1;
      while (randie2 == randie1) {
        randie2 = randGen.nextInt(NUM_NODES_);
      }
      if (randie1 > randie2) {
        int tempoRand = randie1;
        randie1 = randie2;
        randie2 = tempoRand;
      }
      Integer c4n = nodeToClass.get(Integer.toString(randie1));
      if (c4n == null) {
        c4n = Integer.valueOf(randGen.nextInt(NUM_NODE_CLASSES_));
        nodeToClass.put(Integer.toString(randie1), c4n);
      }
      c4n = nodeToClass.get(Integer.toString(randie2));
      if (c4n == null) {
        c4n = Integer.valueOf(randGen.nextInt(NUM_NODE_CLASSES_));
        nodeToClass.put(Integer.toString(randie2), c4n);
      }
      Link daLink = new Link(Integer.toString(randie1), Integer.toString(randie2));
      links.add(daLink);
    }
    return;
  }
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Test frame
  */

  public static void main(String[] argv) {
    GenRand3ModeDAG cp = new GenRand3ModeDAG();
    try {
      String sifOut = "C:\\Users\\wlongaba\\Desktop\\TriModeDAG\\TriMode" + NUM_NODES_ + "-" + NUM_LINKS_ + ".sif";
      Map<String, Integer> nodeToClass = new HashMap<String, Integer>();
      HashSet<Link> links = new HashSet<Link>();
      cp.makeDaDag(nodeToClass, links);
      cp.writeSif(links, sifOut, "to", nodeToClass);
    } catch (Exception ex) {
      System.err.println("Caught exception:" + ex);
    }
    return;
  }
}
