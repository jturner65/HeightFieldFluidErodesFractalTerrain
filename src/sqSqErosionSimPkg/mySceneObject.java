package sqSqErosionSimPkg;

import java.util.*;
/*
*  a class to define a generic scene object.  abstract
*
*/

public abstract class mySceneObject{
	protected sqSqErosionSimGlobal p;
	protected myGui gui;
	protected mySqSqEroder eng;
	protected mySqSqSubdivider sub;
	//unique id number for this sceneObject
	protected int ID;
	//normal for this object
	protected myVector N;
	//list of adjacent faces to this object
	protected ArrayList<myFace> adjFaces;
	//number of faces adjacent to this object, number of verticies adjacent to this object
	protected int numAdjFaces, numAdjVerts;

  //passing specific objCount will not automatically increment global object count - use for temporary objects
	mySceneObject(sqSqErosionSimGlobal _p, myGui _g, mySqSqEroder _e, mySqSqSubdivider _s, int objCount){
		this.p = _p;
		this.gui = _g;
		this.eng = _e;
		this.sub = _s;
		this.ID = objCount;    
		this.adjFaces = new ArrayList<myFace>();
		N = new myVector(0,0,0);
	}

	public mySceneObject(sqSqErosionSimGlobal p, myGui g, mySqSqEroder e, mySqSqSubdivider _s){
	    this(p, g, e, _s, p.objCount);
	    p.objCount++;
	}//constructor - 0 args
  
	public void setParent(sqSqErosionSimGlobal _p){
		this.p = _p;	
	}
	
  //will calculate the normal for this sceneObject - different for vertexes and faces
	public abstract void calcNormal();
  
	/**
	*  gets object id
	*/
	public int getID(){ return this.ID;}
	public String listAdjFaces(){
		String result = " Adj faces to : " + this.ID + " : ";
		for (myFace adjFace : this.adjFaces){ result += adjFace.toString(); }
		return result;
	}//listADjFaces
	
	/**
	 * returns normal, recalculating first
	 */
	public myVector getN(){
		this.calcNormal();
		return this.N;
	}
	
	/**
	*  add face to adjacent faces list and maintain appropriate vertex lists and all counts - returns if occurs or not
	*/
	public boolean addAdjFace(myFace newFace){
		boolean addFaceTest = false;
		addFaceTest = this.adjFaces.contains(newFace);
	    if (!addFaceTest){addFaceTest = this.adjFaces.add(newFace);}
	    //add new face, recalc adjacent verticies
	    if (addFaceTest){ this.numAdjFaces++; }
	    return addFaceTest;
	}//addAdjFace
  
  /**
  *   remove old face that is no longer adjacent, and maintain appropriate vertex lists and all counts
  */
  public boolean removeAdjFace( myFace oldFace){
    boolean removeFaceTest = false;
    //remove old face if in list, recalc adjacent verticies
    removeFaceTest = this.adjFaces.remove(oldFace);
    if (removeFaceTest){ this.numAdjFaces--; }//if remove face
    return removeFaceTest;
  }//removeAdjFace

  public ArrayList<myFace> getAdjFaces(){ return this.adjFaces; }

}//mySceneObject