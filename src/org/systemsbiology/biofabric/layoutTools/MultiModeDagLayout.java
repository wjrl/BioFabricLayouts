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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/****************************************************************************
**
** This creates a node attributes file for laying out a multi-modal directed
** directed acyclic graph (DAG). E.g. a 3-mode DAG has three types of nodes.
** All the directed links will point down, and the different node types will
** be laid out in bands in order of highest parents first. It reads the same
** SIF file as the BioFabric import, with the special requirement that the
** interaction tag must be of the form "X-userSpecified-Y", where X is the
** source node class and Y is the target node class; XX and YY are integers
** N such that 0 <= N <= maxClassNum
*/

public class MultiModeDagLayout {

	////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////

  ////////////////////////////////////////////////////////////////////////////
  //
  // PRIVATE INSTANCE MEMBERS
  //
  ////////////////////////////////////////////////////////////////////////////

   private Map<String, Set<String>> l2p_;
   private Map<String, Integer> inDegs_;
   private Map<String, Integer> outDegs_;
   private ArrayList<String> placeList_;

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTRUCTORS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Constructor
  */

  public MultiModeDagLayout() {
     l2p_ = new HashMap<String, Set<String>>();
     inDegs_ = new HashMap<String, Integer>();
     outDegs_ = new HashMap<String, Integer>();
     placeList_ = new ArrayList<String>();
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Do reading of SIF file
  */

  public int readNodesAndLinks(File infile, Map<String, Integer> netNodes,
                               Set<Link> netLinks) throws IOException {
    //
    // Read in the lines.
    //

    int maxClass = -1;
    BufferedReader in = null;
    try {
      in = new BufferedReader(new FileReader(infile));
      String line = null;
      while ((line = in.readLine()) != null) {
        if (line.trim().equals("")) {
          continue;
        }
        String[] tokens = line.split("\t");
        String trueSrc = tokens[0].trim();
        String trueTrg = tokens[2].trim();
        // Reverse sense of the directed link to point back to source:
        Link link = new Link(trueTrg, trueSrc);
        netLinks.add(link);
        String[] classes = tokens[1].split("-");
        try {
          Integer srcClass = Integer.valueOf(classes[0]);
          Integer trgClass = Integer.valueOf(classes[classes.length - 1]);
          if (srcClass.intValue() > maxClass) {
            maxClass = srcClass.intValue();
          }
          if (trgClass.intValue() > maxClass) {
            maxClass = trgClass.intValue();
          }
          Integer exNodeClass = netNodes.get(trueSrc);
          if (exNodeClass != null) {
            if (!exNodeClass.equals(srcClass)) {
              System.err.println("Bad Src Class " + exNodeClass + " " + srcClass);
              System.err.println("BadLine " + line);
              throw new IOException();
            }
          } else {
            netNodes.put(trueSrc, srcClass);
          }
          exNodeClass = netNodes.get(trueTrg);
          if (exNodeClass != null) {
            if (!exNodeClass.equals(trgClass)) {
              System.err.println("Bad Trg Class " + exNodeClass + " " + trgClass);
              System.err.println("BadLine " + line);
              throw new IOException();
            }
          } else {
            netNodes.put(trueTrg, trgClass);
          }
        } catch (NumberFormatException nex) {
          throw new IOException();
        }
      }
    } finally {
      if (in != null) in.close();
    }
    return (maxClass);
  }

  /***************************************************************************
  **
  ** Build the set of guys we are looking at (our links, opposite of input file,
  ** are pointed backwards) and degrees
  */

  public Map<String, Set<String>> linksToSources(Set<String> nodeList, List<Link> linkList) {

    Iterator<String> nit = nodeList.iterator();
    while (nit.hasNext()) {
      String node = nit.next();
      l2p_.put(node, new HashSet<String>());
      inDegs_.put(node, Integer.valueOf(0));
      outDegs_.put(node, Integer.valueOf(0));
    }

    int numLink = linkList.size();
    for (int i = 0; i < numLink; i++) {
      Link link = linkList.get(i);
      String src = link.getSrc();
      String trg = link.getTrg();
      Set<String> toTarg = l2p_.get(src);
      toTarg.add(trg);
      Integer deg = outDegs_.get(src);
      outDegs_.put(src, Integer.valueOf(deg.intValue() + 1));
      deg = inDegs_.get(trg);
      inDegs_.put(trg, Integer.valueOf(deg.intValue() + 1));
    }
    return (l2p_);
  }

  /***************************************************************************
  **
  ** Add to list to place
  */

  public void addToPlaceList(List<String> nextBatch) {
    placeList_.addAll(nextBatch);
    return;
  }

  /***************************************************************************
  **
  ** Extract the root nodes in order from highest degree to low, but only for
  ** the specified class number
  */

  public List<String> extractRoots(Map<String, Integer> netNodes, int currClass) {

    Map<String, Integer> roots = new HashMap<String, Integer>();

    Iterator<String> lit = l2p_.keySet().iterator();
    while (lit.hasNext()) {
      String node = lit.next();
      Integer rootClass = netNodes.get(node);
      if (rootClass.intValue() != currClass) {
        continue;
      }
      Set<String> fn = l2p_.get(node);
      if (fn.isEmpty() ) {
        roots.put(node, Integer.valueOf(0));
      }
    }

    lit = l2p_.keySet().iterator();
    while (lit.hasNext()) {
      String node = lit.next();
      Set<String> fn = l2p_.get(node);
      Iterator<String> sit = fn.iterator();
      while (sit.hasNext()) {
        String trg = sit.next();
        Integer rs = roots.get(trg);
        if (rs != null) {
          roots.put(trg, Integer.valueOf(rs.intValue() + 1));
        }
      }
    }

    ArrayList<String> buildList = new ArrayList<String>();

    int count = 1;
    while (buildList.size() < roots.size()) {
      TreeSet<String> alpha = new TreeSet<String>(Collections.reverseOrder());
      alpha.addAll(roots.keySet());
      Iterator<String> rit = alpha.iterator();
      while (rit.hasNext()) {
        String node = rit.next();
        Integer val = roots.get(node);
        if (val.intValue() == count) {
          buildList.add(node);
        }
      }
      count++;
    }

    Collections.reverse(buildList);
    return (buildList);
  }

  /***************************************************************************
  **
  ** Find the next guys to go; only dumping the given class number
  */

  public List<String> findNextCandidates(Map<String, Integer> netNodes, int nextClass) {

    HashSet<String> quickie = new HashSet<String>(placeList_);

    TreeSet<SourcedNode> nextOut = new TreeSet<SourcedNode>(Collections.reverseOrder());

    Iterator<String> lit = l2p_.keySet().iterator();
    while (lit.hasNext()) {
      String node = lit.next();
      if (quickie.contains(node)) {
        continue;
      }
      Integer nodeClass = netNodes.get(node);
      if (nodeClass.intValue() != nextClass) {
        continue;
      }
      Set<String> fn = l2p_.get(node);
      boolean allThere = true;
      Iterator<String> sit = fn.iterator();
      while (sit.hasNext()) {
        String trg = sit.next();
        if (!quickie.contains(trg)) {
          allThere = false;
          break;
        }
      }
      if (allThere) {
        nextOut.add(new SourcedNode(node));
      }
    }

    ArrayList<String> retval = new ArrayList<String>();
    Iterator<SourcedNode> noit = nextOut.iterator();
    while (noit.hasNext()) {
      SourcedNode sn = noit.next();
      retval.add(sn.getNode());
    }
    return (retval);
  }


  /***************************************************************************
  **
  ** Output NOA
  */

  public void writeNOA(String outfile) throws IOException {
    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outfile), "UTF-8")));

    //
    // Write out the NOA file:
    //

    int numNode = placeList_.size(); 
    out.println("Node Row");   
    for (int i = 0; i < numNode; i++) {
      String node = placeList_.get(i);
      out.print(node);
      out.print(" = ");
      out.println(i);
    }
    out.close();
    return;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC STATIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////

  /***************************************************************************
  **
  ** Run program
  */

  public static void main(String[] argv) {
  	  	
  	if (argv.length != 2) {
  		System.out.println("Usage: java -cp BioFabricModalDAGLayout.jar org.systemsbiology.biofabric.layoutTools.MultiModeDagLayout sifInfile noaOutfile");
  		return;
  	}
  	
    MultiModeDagLayout cp = new MultiModeDagLayout();
    try {
      String sifIn = argv[0];
      String noaOut = argv[1];
      
      Map<String, Integer> nodeToClass = new HashMap<String, Integer>();
      HashSet<Link> links = new HashSet<Link>();
      int maxClass = cp.readNodesAndLinks(new File(sifIn), nodeToClass, links);
      HashSet<String> nodeSet = new HashSet<String>(nodeToClass.keySet());
      HashSet<String> nodesToGo = new HashSet<String>(nodeSet);
      cp.linksToSources(nodeSet, new ArrayList<Link>(links));
      for (int i = 0; i <= maxClass; i++) {
        List<String> placeList = cp.extractRoots(nodeToClass, i);
        cp.addToPlaceList(placeList);
        nodesToGo.removeAll(placeList);
      }

      //
      // Find the guys whose precursors have already been placed and place them:
      //

      while (!nodesToGo.isEmpty()) {
        for (int i = 0; i <= maxClass; i++) {
          List<String> nextBatch = cp.findNextCandidates(nodeToClass, i);
          cp.addToPlaceList(nextBatch);
          nodesToGo.removeAll(nextBatch);
          System.out.println("Nodes to Go = " + nodesToGo.size());
        }
      }

      cp.writeNOA(noaOut);
    } catch (Exception ex) {
      System.err.println("Caught exception:" + ex);
    }
    return;
  }
  /****************************************************************************
  **
  ** A class that allows us to sort nodes based on input order
  */

  public class SourcedNode implements Comparable<SourcedNode> {

    private String node_;


    public SourcedNode(String node) {
      node_ = node;
    }

    public String getNode() {
      return (node_);
    }

    @Override
    public int hashCode() {
      return (node_.hashCode());
    }

    @Override
    public String toString() {
      return (" node = " + node_);
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof SourcedNode)) {
        return (false);
      }
      SourcedNode otherDeg = (SourcedNode)other;
      return ((this.node_ == null) ? (otherDeg.node_ == null) : this.node_.equals(otherDeg.node_));
    }

    public int compareTo(SourcedNode otherDeg) {

      //
      // Same name, same node:
      //

      if (this.node_.equals(otherDeg.node_)) {
        return (0);
      }

      Set<String> mySet = l2p_.get(this.node_);
      Set<String> hisSet = l2p_.get(otherDeg.node_);

      TreeSet<Integer> myOrder = new TreeSet<Integer>();
      TreeSet<Integer> hisOrder = new TreeSet<Integer>();
      int numNode = placeList_.size();
      for (int i = 0; i < numNode; i++) {
        String node = placeList_.get(i);
        if (mySet.contains(node)) {
          myOrder.add(Integer.valueOf(i));
        }
        if (hisSet.contains(node)) {
          hisOrder.add(Integer.valueOf(i));
        }
      }

      ArrayList<Integer> myList = new ArrayList<Integer>(myOrder);
      ArrayList<Integer> hisList = new ArrayList<Integer>(hisOrder);

      int mySize = myOrder.size();
      int hisSize = hisOrder.size();
      int min = Math.min(mySize, hisSize);
      for (int i = 0; i < min; i++) {
        int myVal = myList.get(i).intValue();
        int hisVal = hisList.get(i).intValue();
        int diff = hisVal - myVal;
        if (diff != 0) {
          return (diff);
        }
      }

      int diffSize = hisSize - mySize;
      if (diffSize != 0) {
        return (diffSize);
      }

      int myIn = inDegs_.get(this.node_);
      int hisIn = inDegs_.get(otherDeg.node_);
      int diffIn = myIn - hisIn;
      if (diffIn != 0) {
        return (diffIn);
      }

      if (this.node_ == null) {
        return ((otherDeg.node_ == null) ? 0 : -1);
      }
      return (this.node_.compareTo(otherDeg.node_));
    }
  }
}
