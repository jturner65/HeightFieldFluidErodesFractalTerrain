package sqSqErosionSimPkg;

import java.util.*;
/*
*  class to hold a vertex object
*/
public class myVertex extends mySceneObject{
		//ID of vertex in file
	public int vertNum;
	  	//coords of vertex
	public myVector coords;
		//index in vert 2d ara - corresponds to int version of x and z coords
	public int xIDX, zIDX;	
//		//consts idx into coord ara
//	private final int _X = 0, _Y = 1, _Z = 2;
		//original base coordinates of this vertex, before subdivision
	public myVector baseCoords;
		//whether or not to calculate new position - set when adjacent vertices change
	public boolean reCalc;
		//list of vertexes adjacent to this vertex, original adjacent vertices, for when subdivision occurs
	public ArrayList<myVertex> adjVerts;
	  	//list of vertexes adjacent to this vertex, original adjacent vertices, for when subdivision occurs
	public ArrayList<myVector> oldAdjVertCoords;
	  	//old vertices that were used to create this vertex - used in sq square subdivision
	  	//idx 0 is "uncle" (vertex ccw from parent), idx 1 is "parent" and idx 2 is "aunt" - vertex cw from parent
	public ArrayList<myVertex> ancestorVerts;
	  	//idx1 0 is "uncleTo" array, idx 1 is "parentTo" array and idx 2 is "auntTo" array
	  	//idx2 is face ID of descendant.
	public HashMap<Integer,myVertex>[] descendantVerts;
	  	//number of ancestors and descendants, for use with sqSquare subdivision - each should be 3 after iteration
	public int numAncestors, numDescendants; 
	  	//whether this vertex gets rain water or not
	private boolean rainOnMe;
		//whether the height node made from this vertex should use specific erosion constants or global ones - and which ones
	private boolean[] useNodeErosionVals;
		//what erosion vals for a particular height node, if using them
	private double[] erosionVals;
	
//		//if both the following are true then both nodes will be treated as pipes-style neighbors
		//whether the node from this vert is a wrap-around node, for boundary
	private boolean[] isMirrorVert;			//id 0 is x, 1 is z

		//height difference from mesh generation for mirror vertex from this vertex - only set once
	private double[] heightDiffWrapAround;
		//use original height of node as some kind of "steel floor" for erosion calculations
	private boolean useOriginalHeight;
	private myHeightNode owningNode;				//if heightMap generated, then this is the node that owns this vertex
	
	public int vertDim;				//sqrt of number of verts in mesh
  
	@SuppressWarnings("unchecked")
	public myVertex(sqSqErosionSimGlobal _p, myGui _g, mySqSqEroder _e, mySqSqSubdivider _s, int _vertNum, double _x, double _y, double _z, int _numAdjFaces, int _numAdjVerts){
	    super(_p, _g, _e, _s);
	    this.vertNum = _vertNum;
	    this.coords = new myVector(_x,_y,_z);
	    this.baseCoords = new myVector(_x,_y,_z);
	    this.numAdjFaces = _numAdjFaces;
	    this.numAdjVerts = _numAdjVerts;
	    this.adjVerts = new ArrayList<myVertex>();
	    this.oldAdjVertCoords = new ArrayList<myVector>();
	    this.ancestorVerts = new ArrayList<myVertex>();
	    this.descendantVerts = new HashMap[3];
	    for (int i = 0; i < 3; ++i){ this.descendantVerts[i] = new HashMap<Integer,myVertex>(); }    
	    this.reCalc = true;
	    this.numAncestors = 0;
	    this.numDescendants = 0;
	    this.rainOnMe = true;  
	    this.owningNode = null;
	    this.useNodeErosionVals = new boolean[4];	    
	    this.isMirrorVert = new boolean[2]; 
	    this.isMirrorVert[0] = false; this.isMirrorVert[1] = false;
	    this.heightDiffWrapAround = new double[2];						//for diagonal mirror verts, get height diff from cardinal mirror and then from cardinal mirror to their neighbor that equals diagonal vert
	    this.heightDiffWrapAround[0] = 0; this.heightDiffWrapAround[1] = 0;
	    
	    this.erosionVals = new double[4];
	    this.useOriginalHeight = false;//if true then erosion on this vert's node will only erode height that is added via sediment, will not go beyond original height
	    this.vertDim = 0;
	    //set global x, y and z coord min and max values
	    if (p!= null){ p.setGlobCoordVals(coords); }
	}//constructor 10 arg

	public myVertex(sqSqErosionSimGlobal _p, myGui _g, mySqSqEroder _e,  mySqSqSubdivider _s, int _vertNum, double _x, double _y, double _z,  int _numAdjFaces, int _numAdjVerts, boolean _rainOnMe){
	    this(_p, _g, _e, _s, _vertNum, _x,_y,_z,_numAdjFaces,_numAdjVerts);    
	    this.rainOnMe = _rainOnMe;
	}//constructor 11 args
	
	//constructor called by sqsqsubdivision
	public myVertex(sqSqErosionSimGlobal _p, myGui _g, mySqSqEroder _e,  mySqSqSubdivider _s, int _vertNum, myVector _coords){
		this(_p, _g, _e, _s, _vertNum, _coords.x, _coords.y, _coords.z,0,0);    
	}//constructor 6 args
  
	public myVertex(sqSqErosionSimGlobal _p, myGui _g, mySqSqEroder _e,  mySqSqSubdivider _s, double _x, double _y, double _z){
	    this(_p, _g, _e, _s, 99999, _x,_y,_z,0,0);  
	}//constructor 7 arg for building initial verticies in subdivision

  	/**
  	 *  draw this height node as a point on the screen
  	 */
  	public void drawMe(){
   		float x = (float)this.coords.x;
  		float y = (float)this.coords.y;
  		float z = (float)this.coords.z;
  		gui.setColorValStroke(myGui.gui_Cyan);
  		p.pushMatrix();
  			p.translate(x, y, z);  
  			p.scale(.01f,.01f,.01f);
  			p.box(1);
  		p.popMatrix();
  	}//drawMe

		// replace null parent with actual parent - for jni functionality
	public void setParent(sqSqErosionSimGlobal _p){this.p = _p;  if (p!= null){	p.setGlobCoordVals(coords);  }}

	/**
	*  adds newVert to list of adjacent verticies, if not there already -returns if successful or not
	*/
	public boolean addAdjVertex(myVertex newVert){
	    boolean addVertTest = false;
	    addVertTest = (this.adjVerts.contains(newVert) || (this.ID == newVert.ID));
	    if (!addVertTest){ 
	      addVertTest = this.adjVerts.add(newVert);
	      boolean sanityCheck = newVert.getAdjVerts().add(this);
	      if(!sanityCheck){gui.print2Cnsl("Error adding adjacent vertex"); }
	    }
	    this.numAdjVerts = this.adjVerts.size();
	    return addVertTest;    
	}//addAdjVertex method
  
	/**
	*  remove passed vert from existing list, if there - returns if successful or not
	*/
	public boolean removeAdjVertex(myVertex oldVert){
	    boolean removeVertTest = false;
	    removeVertTest = this.adjVerts.remove(oldVert);
	    this.numAdjVerts = this.adjVerts.size();
	    gui.print2Cnsl("removing an adj vertex");
	    return removeVertTest;
	}
 
	/**
	*  add descendent to this vertex's hashmap array - used in square square to build linking faces between 
	*  subdivided faces 
	*  @param idx - idx should be 0,1,2 if uncleTo,parentTo,auntTo respectively
	*  @param pos - 0,1,2,3 corresponding to order in face of the descendant vertex - needed to preserve poly orientation
	*  @param ancestorVert - ancestor of this vertex
	*/ 
	public void addDescendantVert(int idx, int pos, myVertex descendantVert){
	    myVertex putDescResult = this.descendantVerts[idx].put(pos, descendantVert);
	    if (putDescResult != null){};//{gui.print2Cnsl ("overwrote a value in the descendant's array for idx : " + idx + " at pos : " + pos + " \n\tthis vertex : " + this.toString() + " \told vertex : " + putDescResult.toString() + " \tnew vertex : " + descendantVert.toString()); }
	    this.numDescendants++;
	}//addDescendantVert
  
	/**
	*  add ancestor to this vertex - used in square square to build linking faces between 
	*  subdivided faces
	*  @param idx - idx should be 0,1,2 if uncle,parent,aunt respectively
	*  @param pos - id of new face this vert is a corner of - this is after subdivision but before connecting polys are made 
	*  @param ancestorVert - ancestor of this vertex
	*/ 
	public void addAncestorVert(int idx, int pos, myVertex ancestorVert){
	    this.ancestorVerts.add(idx,ancestorVert);
	    this.numAncestors++;
	    ancestorVert.addDescendantVert(idx,pos,this);
	}
	/**
	 * this will normalize the x, y and z coords of this vertex so that the distance between each vert in x and z is 1
	 */
	public void normalizeCoordDist(){	
		double deltNodeDist = sub.getDeltaNodeDist();
		
		if((deltNodeDist != 1) && (deltNodeDist != 0)){
			this.coords._div(deltNodeDist);
		}
	    if (p!= null){ p.setGlobCoordVals(coords); }
	}
  
	/**
	*  calculate normal for this object - use list of all adjancent verticies, calculate normal based on them
	*/
	public void calcNormal(){
		myVector tmpVec = new myVector(0,0,0);
//    calculates for each face 
		for (myFace face : this.adjFaces){	tmpVec._add(face.N);	}//for each adjface
		tmpVec._normalize();
		this.N.set(tmpVec);    
	}//calcNormal
  
	/**
	 * calculate the vertex normal for the material on the node that owns this vertex
	 */
	public myVector calcMaterialNormal(int material){
		myVector tmpVec = new myVector(0,0,0), tmpVec2;
		
		for (myFace face : this.adjFaces){
			tmpVec2 = face.calcNormalMaterialFace(material);
			tmpVec._add(tmpVec2);	}//for each adjface
		tmpVec._normalize();
		return tmpVec;
	}
	
	/**
	*   copy current vertex adjacency list to old list - perform right before subdivision
    */
	public void setOldAdjVerts(){
	    ArrayList<myVector> tmpOldAdjVerts = new ArrayList<myVector>();
	    for (myVertex tmpVert : this.adjVerts){
	      tmpVert.reCalc = true;
	      myVector tmpCoords = new myVector(0,0,0);
	      tmpCoords.set(tmpVert.coords);
	      tmpOldAdjVerts.add(tmpCoords);
	    }//for each vert
	    this.oldAdjVertCoords = tmpOldAdjVerts;
	    //print2Cnsl("vert ID : " + this.vertNum + "\n num verts : " + oldAdjVertCoords.size() + "\n" +this.oldAdjVertCoords + " \n___\n" + this.adjVerts +"\n____");
	}//setOldAdjVerts method
  
  /**
  *  returns an adjacent vertex whose ID is passed as a param, or null if none are found
  */
	public myVertex getAdjVertByID(int findID){
	    for (myVertex tmpVert : this.adjVerts){
	        if (tmpVert.ID == findID) { return tmpVert;}
	    }//for each adjancent vert
	    return null;
    }//getAdjVertByID method
  
  /**
  *  will clone this vertex for deep copies - adjacency lists need to be recalculated 
  *  after all verts are cloned due to new vert id of adjacent verts (copy into lists)
  *  vertNum and ID will be MAXINT - this.vertNum and MAXINT - this.ID, respectively
  */
	public myVertex clone(){
	    //make a copy of this vert, with embedded vert id in it's vert id
	    myVertex result = new myVertex(this.p, this.gui, this.eng, this.sub, this.eng.calcCloneID(this.vertNum),this.coords);  
	    result.ID = eng.calcCloneID(this.ID);
	    //decrementing obj count since either "this" or the clone will be deleted.
	    p.objCount--;
	    //retain base coordinates in the clone.
	    result.baseCoords.set(this.baseCoords);
	    //set and recalculate normal, in case of change due to erosion
	    result.N.set(this.N);
	    result.rainOnMe = this.rainOnMe;
	    //mirror vert for wrap around - both x and z represented in array idx 0 = x, 1 = z
	    result.isMirrorVert[0] = this.isMirrorVert[0];
	    result.isMirrorVert[1] = this.isMirrorVert[1];
	    result.heightDiffWrapAround[0] = this.heightDiffWrapAround[0];
	    result.heightDiffWrapAround[1] = this.heightDiffWrapAround[1];

	    return result;
    }//clone method
  
	/**
	*  will set this vertex's adjacencies to the cloned version of the adjacent verts in oldVert - this assumes this vertex is a clone as well
	*  (meaning verts with id of Integer.MAX_INT - adjVert.adjVert.ID
	*  -doing this to preserve all vertex values through an iteration of erosion algorithms, since they rely on
	*  previous time-stamp values of all neighbors.
	*/
	public void resetCloneAdjacencies(myVertex oldVert, HashMap<Integer, myHeightNode> tmpOldCloneNodeList){
	    for (myVertex oldAdjVert : oldVert.adjVerts){//put the clone'd version of each adjacent vertex into this vert's adj list.
	      this.adjVerts.add(tmpOldCloneNodeList.get(eng.calcCloneID(oldAdjVert.ID)).source); 
	    }//for each old adj vert
	    this.numAdjVerts = this.adjVerts.size();  
	    if (this.numAdjVerts > 4){ gui.print2Cnsl("error in vert adj calc with vert : " + oldVert.toStringWithFaces()); gui.print2Cnsl("-----");}  
	}//resetcloneadjacencies method
  
	/**
	*  will set this vertex's adjacencies to the cloned version of the adjacent verts in oldVert - this assumes this vertex is a clone as well
	*  (meaning verts with id of Integer.MAX_INT - adjVert.adjVert.ID
	*  -doing this to preserve all vertex values through an iteration of erosion algorithms, since they rely on
	*  previous time-stamp values of all neighbors.
	*/
	public void resetCloneAdjacenciesAra(myVertex oldVert, myHeightNode[][] tmpOldCloneNodeAra){
	    for (myVertex oldAdjVert : oldVert.adjVerts){//put the clone'd version of each adjacent vertex into this vert's adj list.
	      this.adjVerts.add(tmpOldCloneNodeAra[oldAdjVert.xIDX][oldAdjVert.zIDX].source); 
	    }//for each old adj vert
	    this.numAdjVerts = this.adjVerts.size();  
	    if (this.numAdjVerts > 4){ gui.print2Cnsl("error in vert adj calc with vert : " + oldVert.toStringWithFaces()); gui.print2Cnsl("-----");}  
	}//resetcloneadjacencies method	
	  /**
	  *  recalculates this vertex's position, based on its adjacent old verticies
	  */ 
	public void reCalcPosition(){
	    if (this.reCalc){
	      int k = this.oldAdjVertCoords.size() ;
	      float beta = (k > 3) ? (3.0f/ (8.0f * k)) : (3.0f/16.0f);
	      //if (k < 4) { print2Cnsl("low k : " + k + " beta : " + beta);}
	      myVector tmpVec = new myVector(this.coords.x, this.coords.y, this.coords.z);
	      tmpVec._mult(1 - (k*beta));
	      myVector tmpVec2 = new myVector(0,0,0);
	      for (myVector oldCoords : this.oldAdjVertCoords){
	        if (!(oldCoords.equals(this.coords))){
	          myVector tmpVec3 = myVector._mult(oldCoords, beta);
	          tmpVec2._add(tmpVec3);     
	        } 
	      }//for each vertex neighbor
	      tmpVec._add(tmpVec2);
	      this.coords.set(tmpVec);
	    }//if recalc is true
    }//reCalcPosition method
  
	public ArrayList<myVertex> getAdjVerts(){ return this.adjVerts; }
	  
	public void calcNumAdjFaces(){ this.numAdjFaces = this.adjFaces.size();}
	
	public double getX(){ return this.coords.x;} 
	public double getY(){ return this.coords.y;} 
	public double getZ(){ return this.coords.z;} 
	public myVector getCoords(){return this.coords;}
	public myVector getBaseCoords(){return this.baseCoords;}
	public boolean isRainOnMe(){return this.rainOnMe;}
	  
	public void setX(double x){ this.coords.x = x;}
	public void setY(double y){ this.coords.y = y;}
	public void setZ(double z){ this.coords.z = z;}
	public void setCoords (myVector pcoords) { 
	    this.coords.x = pcoords.x;
	    this.coords.y = pcoords.y;
	    this.coords.z = pcoords.z;
    }
	
	public myHeightNode getOwningNode(){return this.owningNode;}
	public void setOwningNode(myHeightNode _node){this.owningNode = _node;}
	
	public boolean isUseNodeErosionConstants(int idx){ return this.useNodeErosionVals[idx];}	
	public boolean isUseNodeKc() {	return this.useNodeErosionVals[eng.Kc];}
	public boolean isUseNodeKd() {	return this.useNodeErosionVals[eng.Kd];}
	public boolean isUseNodeKs() {	return this.useNodeErosionVals[eng.Ks];}
	public boolean isUseNodeKw() {	return this.useNodeErosionVals[eng.Kw];}

	public void setUseNodeKc(boolean useEVal) {	this.useNodeErosionVals[eng.Kc] = useEVal;}
	public void setUseNodeKd(boolean useEVal) {	this.useNodeErosionVals[eng.Kd] = useEVal;}
	public void setUseNodeKs(boolean useEVal) {	this.useNodeErosionVals[eng.Ks] = useEVal;}
	public void setUseNodeKw(boolean useEVal) {	this.useNodeErosionVals[eng.Kw] = useEVal;}
	
	public double getErosionVals(int idx){	return this.erosionVals[idx];	}
	public void setErosionVals(int idx, double Kval){this.erosionVals[idx] = Kval;}
	
	public void setErosionKc(double Kc){	this.erosionVals[eng.Kc] = Kc;}
	public void setErosionKd(double Kd){	this.erosionVals[eng.Kd] = Kd;}
	public void setErosionKs(double Ks){	this.erosionVals[eng.Ks] = Ks;}
	public void setErosionKw(double Kw){	this.erosionVals[eng.Kw] = Kw;}
	
	//sets the "center of mass" of this vertex, for solid surfaces
	public void setBaseCoords(myVector p){		this.baseCoords.set(p);}
	
	public boolean getIsMirrorVert(int idx){return this.isMirrorVert[idx];}

  	/**
  	 * sets the node that this node is mirrored to - used in river meshes to wrap water and sediment aroun
  	 * @param _mirrorNodeID
 	 * @param idx 0 = mirrors on x, 1 = mirrors on z
 	 */
  	public void setIsMirrorVert(int idx){this.isMirrorVert[idx] = true;}
  	
  	/**
  	 * sets the height difference between this node and its mirror node's adj neighbor (the location it would be if it were actually adjacent and not wrapping around
  	 * @param heightDiff the difference in height
  	 * @param 
  	 */
  	public void setHeightDiffWrapAround(double heightDiff, int idx){this.heightDiffWrapAround[idx] = heightDiff;}
  	public double getHeightDiffWrapAround(int idx){return this.heightDiffWrapAround[idx];}
	/**
	 * sets the node that will inherit this vert to only erode height added via sediment - will not convert any height into sediment beyond original height of node
	 */
	public void setUseOriginalHeight(boolean _useOriginalHeight){this.useOriginalHeight = _useOriginalHeight;}	
	public boolean getUseOriginalHeight(){return this.useOriginalHeight;}  	
	public double[] getErosionVals(){return this.erosionVals;}	
	public void setRainOnMe(boolean rainOnMe){this.rainOnMe = rainOnMe;}	  
	public void setvertNum(int vertNum){ this.vertNum = vertNum;}
  
  /**
  *  checks if two vertices are equal, as per standard
  */  
	public boolean equals(Object passedVert){
	    if (this == passedVert) return true;
	    if (!(passedVert instanceof myVertex)) return false;
	    myVertex thatVert = (myVertex)passedVert;
	    return ((this.coords.equals(thatVert.coords) &&
	           (this.numAdjFaces == thatVert.numAdjFaces) &&
	           (this.numAdjVerts == thatVert.numAdjVerts) &&
	           (this.adjVerts.equals(thatVert.adjVerts)) &&
	           (this.oldAdjVertCoords.equals(thatVert.oldAdjVertCoords)) &&
	           (this.reCalc == thatVert.reCalc)));
	}//override equals  
  
	public String toString(){ 
	    String result = " ID : " + this.ID + " | VertNum : "+ this.vertNum + " | Coords " + this.coords.toString()+ " IDXs : [" + this.xIDX + "]["+ this.zIDX+"]";
	    result += "\n---Wraps to another vertex in x : " + this.isMirrorVert[0];
	    return result;  
	}
  
	public String toStringWithFaces(){ 
	    String result =  this.toString() + "\n\tAdjacent Faces : " + this.numAdjFaces;
	    for (myFace adjFace : this.adjFaces){
	      result += "\n\t\tAdj Face : " + adjFace.toStringWithoutVerts();    
	    }//for each adj face  
	    result += "\n\tAdjacent verticies : " + this.numAdjVerts;
	    for (myVertex adjVert : this.adjVerts){
	      result += "\n\t\tAdj Vertex : " + adjVert.toString();    
	    }//for each adj vertex
	    result += "\n\tNumber of Ancestors : " + this.numAncestors;
	    for (myVertex ancestor : this.ancestorVerts){
	      result += "\n\t\tAncestor Vertex : " + ancestor.toString();    
	    }//for each ancestor
	    result += "\n";
	        
	    return result;  
	}//toStringWithFaces
  
	public String toStringWithAdjVerts(){
	    String result =  this.toString();    
	    result += "\n\tAdjacent verticies : " + this.numAdjVerts;
	    for (myVertex adjVert : this.adjVerts){
	      result += "\n\t\tAdj Vertex : " + adjVert.toString();    
	    }//for each adj vertex
	    
	    return result;
	}//toStringWithAdjVerts
}//myvertex
