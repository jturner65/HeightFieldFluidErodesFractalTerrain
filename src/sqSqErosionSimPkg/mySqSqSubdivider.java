package sqSqErosionSimPkg;

import java.util.HashMap;

import processing.core.PApplet;

/**
 * class that holds all routines for various subdivision algorithms
 * 
 * @author John
 *
 */
public class mySqSqSubdivider {
	private sqSqErosionSimGlobal p;
	private mySqSqEroder eng;
	private myGui gui;
	
	//list of every object being rendered via subdivision
	public HashMap<Integer,myFace> polyList;
	//all the current vertices in this scene - should be redundant, should be able to get all verticies as unique members of polyList.getVerts
	public HashMap<Integer,myVertex> vertList;
	
	//2d ara of all vertices - idxed by x and z val from - to + translated to 0 to numXvals and 0 to numZVals
	public myVertex[][] vertListAra;
	//2d ara of all faces, idxed by coord of lower left vertex
	public myFace[][] faceListAra;
	
	//number of verts on a side of square generated mesh 
	public int numVertsPerSide;
	//slope of generated river mesh - incline per vert dist
	public double genRiverSlope;
	
	//distance between adjacent nodes/verts in mesh - set in mesh builder
	private double deltaNodeDist = 0;
	//number of verts in current mesh
	public int globalNumVerts;
	//number of verts in x dir in mesh
	public int numXVerts;
	//number of verts in z dir in mexh
	public int numZVerts;
	//number of faces in current mesh
	public int globalNumFaces;
	//current depth of iteration
	public int currSqSqIterDepth = 1;	
	//number of automated subdivisions, if set to true
	public int autoSubdivideCount = 0;
	
	//arrays of values for jni computation
	//array of the int values of the vert nums
	public int[] vertIDsInSeq;
	//array of the int values of the face id's
	public int[] faceIDsInSeq;
	//array of face ids with each having 4 vert ids, in array order of instantiation
	public int[][] faceIDOwnsVertIDs;
	//array of vert idxs in vertIDsInSeq with their coords
	public double[][] vertIDWithCoords;
	//period multiplier for endless river wrap around - needs to be int to keep cyclic nature
	public int riverWrapAroundPeriodMult;
	//how wide a river mesh is, as a multiple of # of vertices per side (should be less than .5)
	public double riverWidth;
	
	public boolean pipesNeighborMeshX, 		//whether the wrap around nodes on this mesh act as actual neighbors (sharing everything)
			pipesNeighborMeshZ; 
	
	public mySqSqSubdivider(sqSqErosionSimGlobal _p, myGui _g, mySqSqEroder _e) {
		this.p = _p;
		this.gui = _g;
		this.eng = _e;
		this.polyList = new HashMap<Integer,myFace>();
		this.vertList = new HashMap<Integer,myVertex>();  
		this.vertListAra = null;
		this.globalNumVerts = 0;
		this.globalNumFaces = 0;
		this.currSqSqIterDepth = 1;	
		this.riverWrapAroundPeriodMult = 1;
		this.riverWidth = .333333;
		this.numVertsPerSide = 20;
		this.genRiverSlope = 80;
	}
	
	public void setGui(myGui _g){ this.gui = _g; }	
	public void setParent(sqSqErosionSimGlobal _p){ this.p = _p;}
	public void setEroder(mySqSqEroder _eng){ this.eng = _eng;}
	
	/**
	 * called by parent object to initialize subdivision values
	 */
	public void initValues(){
		this.deltaNodeDist = 0;
		this.currSqSqIterDepth = 1;
		this.polyList = new HashMap<Integer,myFace>();
		this.vertList = new HashMap<Integer,myVertex>(); 
		this.vertListAra = null;
		this.faceListAra = null;
		this.pipesNeighborMeshX = false; 	
		this.pipesNeighborMeshZ = false;         
	}//initvalues
	
	/**
	*  Read polygon mesh from .ply file - only used for loop subdivision - needs triangle polys
	*/
	public void readMeshLoop (String filename){
		String[] words;  
		String lines[] = p.loadStrings(filename);//processing function
		int numVertices, numFaces;
		
		words = PApplet.split (lines[0], " ");
		numVertices = Integer.parseInt(words[1]);
	  
		words = PApplet.split (lines[1], " ");
		numFaces = Integer.parseInt(words[1]);
		//gui.print2Cnsl ("number of faces = " + numFaces);
		
		// read in the vertices
		for (int vertIDX = 0; vertIDX < numVertices; vertIDX++) {
			words = PApplet.split (lines[vertIDX+2], " ");
			float x = Float.parseFloat(words[0]), y = Float.parseFloat(words[1]), z = Float.parseFloat(words[2]);
	    
			myVertex tmpVert = new myVertex(p, gui, this.eng, this, vertIDX,x,y,z,0,0);
			//return val putResult used to test put operation
			myVertex putResult = this.vertList.put(vertIDX,tmpVert);
			if (putResult != null) { gui.print2Cnsl("vertex " + vertIDX + " : (" + x + ", " + y + ", " + z + ") already in list");   }   
		}//for each vertex
		this.globalNumVerts = this.vertList.size();
	  
		//sanity check
		this.globalNumVerts = numVertices;
		if (this.globalNumVerts != this.vertList.size()){gui.print2Cnsl("error creating vertex list");}   
	  
		// read in the faces
		for (int faceNum = 0; faceNum < numFaces; faceNum++) {
			int j = faceNum + numVertices + 2;						//calculate correct offset in file for where faces start
			words = PApplet.split (lines[j], " ");
	    
			int nVerts = Integer.parseInt(words[0]);
			int[] index = new int[nVerts];
			//verticies used by this face
			myVertex[] tmpVertArray = new myVertex[nVerts];

			if (nVerts != 3) { gui.print2Cnsl ("error: this face is not a triangle."); p.exit(); }

			for (int idx = 0; idx < nVerts; idx++){
				//find vertex in vertList hashmap based on vertex index, which is how each vertex of each face is referenced in description file
				index[idx] = Integer.parseInt(words[idx+1]);
				tmpVertArray[idx] = vertList.get(index[idx]);
			}//for each vertex in face
			myFace tmpFace = this.buildFace(faceNum, tmpVertArray.length, tmpVertArray, true, false);		//TODO change to put in ara
			if(tmpFace == null){gui.print2Cnsl("null face in read_mesh loop method ");}
			
		}//for each face
		for(myFace face : polyList.values()){face.findAndSetAdjFaces(); }//set all adjacent faces now that all faces are loaded
		//for (myVertex vert : vertList.values()){gui.print2Cnsl(vert.toStringWithFaces());}//for each vert
		this.globalNumFaces = polyList.size();
	}//read_mesh method

	/**
	*  add newface to each vertex in array's adjface list
	*/
	public boolean addFaceToVertAdjList(myFace newFace, myVertex[] tmpVertArray){
		boolean faceAdded = false;
		for (int idx = 0; idx < tmpVertArray.length; idx++){        // vert : tmpVertArray){
			if (newFace.containsVertex(tmpVertArray[idx]) != -1){ 
				faceAdded = tmpVertArray[idx].addAdjFace(newFace); 
				tmpVertArray[idx].calcNumAdjFaces();
				tmpVertArray[idx].calcNormal();  
			}//if the passed face contains this vertex then add passed face to this vertex's adj list
		}//for each vertex
		return faceAdded;
	}//addFacetoVertAdjList

	/**
	*  makes a new vertex on the edge connecting v0 and v1, where v2 and v3 are opposite
	*  each other across the edge.
	*/
	public myVertex calcHalfVertex(myVertex v0, myVertex v1, myVertex v2, myVertex v3){
		myVector newCoords = new myVector(0,0,0);
		myVector tmpVal = new myVector(0,0,0);
		myVertex result = null;
		
		newCoords.set(v0.getCoords());
		newCoords._add(v1.getCoords());
		newCoords._mult((3.0f/8.0f));
		
		tmpVal.set(v2.coords);
		tmpVal._add(v3.coords);  
		tmpVal._mult((1.0f/8.0f));
	  
		newCoords._add(tmpVal);
		
		//see if vertex exists with these coords - if so then return it, don't make a new one
		result = findVertexWithCoords(newCoords);
		if (result == null){//no vertex exists, make a new one and add to global vert list
			int newIDX = this.globalNumVerts;
			result = new myVertex(this.p, this.gui, this.eng, this, newIDX, newCoords);
			this.globalNumVerts++;
			//don't recalc this vertex until next subdivision
			myVertex putResult = vertList.put(newIDX, result);
			if (putResult != null) { gui.print2Cnsl("vertex already in list : \n" + putResult.toStringWithFaces());   }        
		}//if result is null  
		result.reCalc = false;
		//add two new known adjacent verticies - the endpoints of the old edge passed to this function
		//no longer adjacent verticies, since they're being split by result
		v0.removeAdjVertex(v1);
		v1.removeAdjVertex(v0);
		
		return result;    
	}//calcHalfVertex method

	/**
	*  given a set of coords, finds a vertex in the vertList 
	*  that matches those coords within a certain epsilon value and returns it, or null
	*/
	public myVertex findVertexWithCoords(myVector coords){
		myVertex result = null;  
		for(myVertex vert : vertList.values()){
			if ((Math.abs(vert.coords.x - coords.x) < myConsts.epsValCalc) 
					&& (Math.abs(vert.coords.y - coords.y) < myConsts.epsValCalc) 
					&& (Math.abs(vert.coords.z - coords.z) < myConsts.epsValCalc)){
				//this vertex is within epsilon of the desired vertex
				result = vert;
			}//if coords match    
		}//for each vertex in global list
		return result;
	}//findVertexWithCoords method
	

	/**
	*  calculates the subdivided face ids from the old parent face id
	*/
	public int calcFaceID(int oldfaceNum, int offset){ return ((oldfaceNum * 4) + offset); }
	
//	public native int calcSqSqFaceIDJNI(int oldfaceNum, int offset);
		
	/**
	*  subdivides the mesh using loop subdivision
	*/
	public void subdivide_mesh(){
		HashMap<Integer,myFace> tmpPolyList = new HashMap<Integer,myFace>();
		//int numPolys = this.polyList.size();
		//first generate new faces
		//gui.print2Cnsl(numPolys);
		
		for (myVertex verts : this.vertList.values()){	verts.setOldAdjVerts();	}  
		for (myFace face : this.polyList.values()){
			//generate 4 new faces from old face, including new verticies
			myFace[] resAra;
			resAra = face.subdivideLoop();
			boolean removeFaceFromVertsAdjList = false;
			for (myVertex vert : face.getVerts()){ removeFaceFromVertsAdjList = vert.removeAdjFace(face); }
			if(removeFaceFromVertsAdjList){}
			for (myFace newFace : resAra){
				if (tmpPolyList.get(newFace.getFaceNum()) == null){
					tmpPolyList.put(newFace.getFaceNum(),newFace);
				} else {
					gui.print2Cnsl("Danger - faceNum " + newFace.getFaceNum() + " already exists in polyList");
				}
			}//for each new face
		}//for each old face 
	  
		//by here tmpPolyList holds all the new faces
		this.polyList = tmpPolyList;
		this.globalNumFaces = this.polyList.size();
	  
		for(myFace face : this.polyList.values()){//set all adjacent faces now that all faces are determined
			face.findAndSetAdjFaces();
//		    gui.print2Cnsl("face : " + face.faceNum );//" list : " + face.listAdjFaces());
		}    
		//second, reposition all old verticies only (not new ones) based on new neighbors
		//following the given equation from class - remember to set the verticies that are not recalced to be true
		//so they are recalced next time
		for (myVertex vert : this.vertList.values()){
			if (vert.reCalc){
				vert.reCalcPosition();    
			} else {
				//gui.print2Cnsl("not recalc" + vert.ID);
				vert.reCalc = true;    
			}//if recalc true or false   
		}//for each vertex
		//gui.print2Cnsl("subdivided");  
	}//subdivide_mesh	
	
	/**
	*  reads in a single quad and splits it into 4 for the sqsq process to work correctly
	*  @param words text from file describing quad
	*/
	public void readSingleQuad(String[] words){
	  
		int nVerts = Integer.parseInt(words[0]);
		int[] vertIndex = new int[nVerts];
		//verticies used by this face
		//including halfway verts and center vert
		myVertex[] tmpVertArray = new myVertex[nVerts];
		myVertex[] halfVertArray = new myVertex[nVerts];
		myVertex centerVert = null, putResult;

		if (nVerts != 4) { gui.print2Cnsl ("error: this face is not a quad."); p.exit(); }

		for (int idx = 0; idx < nVerts; idx++){
			//find vertex in vertList hashmap based on vertex index, which is how each vertex of each face is referenced in description file
			vertIndex[idx] = Integer.parseInt(words[idx+1]);
			tmpVertArray[idx] = this.vertList.get(vertIndex[idx]);
		}//for each vertex in face
		//use this to find center vertex - sum center lines and take average
		myVector coordSum = new myVector(0,0,0);
	  
		for (int idx = 0; idx < nVerts; idx++){
			halfVertArray[idx] = new myVertex(this.p, this.gui, this.eng, this, this.globalNumVerts, myVector._mult(myVector._add(tmpVertArray[idx].coords,tmpVertArray[((idx + 1 ) % nVerts)].coords),.5f)); 
			//return val putResult used to test put operation
			putResult = this.vertList.put(this.globalNumVerts,halfVertArray[idx]);
			this.globalNumVerts++; 
			coordSum._add(tmpVertArray[idx].coords);
		}//for each base vertex, find half-way vertex
		coordSum._mult(.25f);
		coordSum._add(new myVector(0,2,0));//raise center above plane, to check subdivision
		centerVert = new myVertex(this.p, this.gui, this.eng, this, this.globalNumVerts, coordSum);
		putResult = this.vertList.put(this.globalNumVerts, centerVert);
		if(putResult == null){}
		this.globalNumVerts++;
		
		myFace[] tmpFaceAra = new myFace[4];
		//myFace[] putFaceResult = new myFace[4];
		//boolean[] calcAdjacency = new boolean[9];
	 
		myVertex[][] faceVerts = new myVertex[4][4];//array of vert arrays, 1 entry for each face
		myVertex[] tmpVerts0 = { tmpVertArray[0], halfVertArray[0], centerVert, halfVertArray[3]};
		faceVerts[0] = tmpVerts0;  
		myVertex[] tmpVerts1 = { halfVertArray[0], tmpVertArray[1], halfVertArray[1], centerVert};
		faceVerts[1] = tmpVerts1;  
		myVertex[] tmpVerts2 = { halfVertArray[3], centerVert, halfVertArray[2], tmpVertArray[3]};
		faceVerts[2] = tmpVerts2;  
		myVertex[] tmpVerts3 = { centerVert, halfVertArray[1], tmpVertArray[2], halfVertArray[2]};
		faceVerts[3] = tmpVerts3;  
		//myVertex[] tmpCenterArray = {centerVert};
		for (int faceNum = 0; faceNum < 4; faceNum++){			tmpFaceAra[faceNum] = this.buildFace(faceNum, faceVerts.length, faceVerts[faceNum], true, false);	}	//TODO change to put in ara	}	
		this.globalNumFaces = this.polyList.size();
	}//readSingleQuad


	/**
	 * will build a quad based poly mesh from the current vertList - built for old version of implementation with hashmap
	 * @param numPolyCols z values - minor (inner) axis in loop
	 * @param vertOffset x values - major (outer) axis in loop
	 */
	public void buildFacesFromVertList(int numPolyCols, int vertOffset){		
		myVertex[] vertAra;
		int faceNum;
		int vertNum;
		for(int xIDX = 0;  xIDX < numPolyCols-1; ++xIDX){
			for(int zIDX = 0; zIDX< vertOffset-1; ++zIDX){//build a face from vert[idx], vert[idx + 10], vert[idx+11], vert[idx + 1]
				vertNum = (xIDX * vertOffset) + zIDX;
				vertAra = new myVertex[4];
				vertAra[0] = this.vertList.get(vertNum);
				vertAra[1] = this.vertList.get(vertNum+vertOffset);
				vertAra[2] = this.vertList.get(vertNum+vertOffset + 1);
				vertAra[3] = this.vertList.get(vertNum+1);
				faceNum = vertNum;//use base vert id to number faces
				buildFace(faceNum, vertAra.length, vertAra, true, false);		//TODO change to put in ara
			}//for each i
		}//for incr	  
		
	}//buildMeshFromVerts function
	
	/**
	 * using 2D ara of verts - new version of heightmap as 2 d ara
	 * will build a quad based poly mesh from the current vertList
	 * @param numPolyCols z values - minor (inner) axis in loop
	 * @param vertOffset x values - major (outer) axis in loop
	 */
	public void buildFacesFromVertListAra(){		
		myVertex[] vertAra;
		int faceNum;
		for(int xIDX = 0;  xIDX < this.vertListAra.length-1; ++xIDX){
			for(int zIDX = 0; zIDX< this.vertListAra[0].length-1; ++zIDX){//build a face from vert[idx], vert[idx + 10], vert[idx+11], vert[idx + 1]
				vertAra = new myVertex[4];
				vertAra[0] = this.vertListAra[xIDX][zIDX];
				vertAra[1] = this.vertListAra[xIDX+1][zIDX];
				vertAra[2] = this.vertListAra[xIDX+1][zIDX+1];
				vertAra[3] = this.vertListAra[xIDX][zIDX+1];
				faceNum = this.vertListAra[xIDX][zIDX].ID;//use base vert id to number faces
				buildFace(faceNum, vertAra.length, vertAra, false, true);		
			}//for each i
		}//for incr	  		
	}//buildMeshFromVerts function	
	
	/**
	*  will build a quad based torus poly mesh from the current vertList
	*  @param numSlices - number of "slices" across torus mesh
	*  @param vertOffset - number of "facets" for each slice 
	*/
	public void buildTorusMeshFaces(int numPolyCols, int vertOffset){
		myVertex[] vertAra;
		int faceNum;
		int vertNum;
		for(int incr = 0;  incr < numPolyCols-1; ++incr){
			for(int i = 1; i< vertOffset; ++i){//build a face from vert[idx], vert[idx + 10], vert[idx+11], vert[idx + 1]
				vertNum = (incr * vertOffset) + i;			//upper left vert of face
				vertAra = new myVertex[4];
				vertAra[0] = this.vertList.get(vertNum);
				vertAra[1] = this.vertList.get(vertNum+vertOffset);
				vertAra[2] = this.vertList.get(vertNum+vertOffset-1);
				vertAra[3] = this.vertList.get(vertNum-1);
				faceNum = vertNum;//use base vert id to number faces
				//gui.print2Cnsl("build face " + faceNum);
				buildFace(faceNum, vertAra.length, vertAra, true, false);		//TODO change to put in ara
			}//for each i
			vertNum = ((incr) * vertOffset) ;
			vertAra = new myVertex[4];
			vertAra[0] = this.vertList.get(vertNum);
			vertAra[1] = this.vertList.get(vertNum + vertOffset);
			vertAra[2] = this.vertList.get(vertNum + vertOffset + (vertOffset-1));
			vertAra[3] = this.vertList.get(vertNum + (vertOffset -1));
			faceNum = vertNum;//use base vert id to number faces
			//gui.print2Cnsl("build face " + faceNum);
			buildFace(faceNum, vertAra.length, vertAra, true, false);		//TODO change to put in ara
		}//for incr	  
		//last slice of torus
		for(int i = 1; i < vertOffset; ++i){
			vertNum = ((numPolyCols-1) * (vertOffset)) + i;
			vertAra = new myVertex[4];
			vertAra[0] = this.vertList.get(vertNum);
			vertAra[1] = this.vertList.get(i);
			vertAra[2] = this.vertList.get(i-1);
			vertAra[3] = this.vertList.get(vertNum-1);
			faceNum = vertNum;//use base vert id to number faces
			//gui.print2Cnsl("build face " + faceNum);
			buildFace(faceNum, vertAra.length, vertAra, true, false);		//TODO change to put in ara
		}//for i
		//last poly
		vertNum = (numPolyCols-1) * (vertOffset);
		vertAra = new myVertex[4];
		vertAra[0] = this.vertList.get(vertNum);
		vertAra[1] = this.vertList.get(0);
		vertAra[2] = this.vertList.get(vertOffset-1);
		vertAra[3] = this.vertList.get(this.vertList.size()-1);
		faceNum = (numPolyCols-1) * (vertOffset);//use base vert id to number faces
		//gui.print2Cnsl("build face " + faceNum);
		buildFace(faceNum, vertAra.length, vertAra, true, false);		//TODO change to put in ara
	}//buildTorusMeshFromVerts function
	
	public double[] calcTorusCenterVals(int meshID, double rad1, double knotRad, double thetaVal){
		double[] result = new double[9];	//012 center, 345 derivative at center for tangent, 678 2nd deriv for k * normal
		double calcR, calcTheta, calcPhi;
		switch (meshID){
			case myConsts.mesh_torus1 	: {//regular torus
				result[0] = rad1 * Math.cos(thetaVal);
				result[1] = 0;
				result[2] = rad1 * Math.sin(thetaVal);
				result[3] = rad1 * -1 * Math.sin(thetaVal);//calc derivative w/regard to theta
				result[4] = 0;//calc derivative w/regard to theta
				result[5] = rad1 * Math.cos(thetaVal);//calc derivative w/regard to theta
				result[6] = rad1 * -1 * Math.cos(thetaVal);
				result[7] = 0;
				result[8] = rad1 * -1 * Math.sin(thetaVal);
				break;
				}
			case myConsts.mesh_torus2 : {//trefoil attempt 
				knotRad = 3;
				rad1 = 2;
				calcR = (2 + Math.cos(knotRad * thetaVal));
				result[0] = calcR * Math.cos(rad1 * thetaVal);
				result[1] = Math.sin(knotRad * thetaVal);
				result[2] = calcR * Math.sin(rad1 * thetaVal);
				result[3] = 0;//calc derivative w/regard to theta
				result[4] = 0;//calc derivative w/regard to theta
				result[5] = 0;//calc derivative w/regard to theta
				result[6] = 0;//calc 2nd derivative w/regard to theta for normal
				result[7] = 0;//calc 2nd derivative w/regard to theta for normal
				result[8] = 0;//calc 2nd derivative w/regard to theta for normal
				
				break;
			}
			case myConsts.mesh_torus3: {/**
				x = calcR * cos(calcPhi) * cos(calcTheta)
				z = calcR * cos(calcPhi) * sin(calcTheta)
						y = calcR * sin(calcPhi)
						
						x = (2+cos 3t)cos 2t
						z = (2+cos 3t)sin 2t
						y = sin 3t

				*/
				calcR = 0.8 + 1.6 * Math.sin(6 * thetaVal);
				calcTheta = 2 * thetaVal;
				calcPhi = 0.6 * Math.PI * Math.sin(12 * thetaVal);
			 	
				result[0] = calcR * Math.cos(calcPhi) * Math.cos(calcTheta);
				result[1] = calcR * Math.sin(calcPhi);
				result[2] = calcR * Math.cos(calcPhi) * Math.sin(calcTheta);
				result[3] = 0;//calc derivative w/regard to theta
				result[4] = 0;//calc derivative w/regard to theta
				result[5] = 0;//calc derivative w/regard to theta
				result[6] = 0;//calc 2nd derivative w/regard to theta for normal
				result[7] = 0;//calc 2nd derivative w/regard to theta for normal
				result[8] = 0;//calc 2nd derivative w/regard to theta for normal
				break;
			}
			
			case myConsts.mesh_torus4 	: {//regular torus
				result[0] = rad1 * Math.cos(thetaVal);
				result[1] = 0;
				result[2] = rad1 * Math.sin(thetaVal);
				result[3] = rad1 * -1 * Math.sin(thetaVal);//calc derivative w/regard to theta
				result[4] = 0;//calc derivative w/regard to theta
				result[5] = rad1 * Math.cos(thetaVal);//calc derivative w/regard to theta
				result[6] = rad1 * -1 * Math.cos(thetaVal);
				result[7] = 0;
				result[8] = rad1 * -1 * Math.sin(thetaVal);
				break;
				}
			case myConsts.mesh_torus5 	: {//regular torus
				result[0] = rad1 * Math.cos(thetaVal);
				result[1] = 0;
				result[2] = rad1 * Math.sin(thetaVal);
				result[3] = rad1 * -1 * Math.sin(thetaVal);//calc derivative w/regard to theta
				result[4] = 0;//calc derivative w/regard to theta
				result[5] = rad1 * Math.cos(thetaVal);//calc derivative w/regard to theta
				result[6] = rad1 * -1 * Math.cos(thetaVal);
				result[7] = 0;
				result[8] = rad1 * -1 * Math.sin(thetaVal);
				break;
				}
			case myConsts.mesh_torus6 	: {//regular torus
				result[0] = rad1 * Math.cos(thetaVal);
				result[1] = 0;
				result[2] = rad1 * Math.sin(thetaVal);
				result[3] = rad1 * -1 * Math.sin(thetaVal);//calc derivative w/regard to theta
				result[4] = 0;//calc derivative w/regard to theta
				result[5] = rad1 * Math.cos(thetaVal);//calc derivative w/regard to theta
				result[6] = rad1 * -1 * Math.cos(thetaVal);
				result[7] = 0;
				result[8] = rad1 * -1 * Math.sin(thetaVal);
				break;
				}
			
			default : {		
				result[0] = rad1 * Math.cos(thetaVal);
				result[1] = 0;
				result[2] = rad1 * Math.sin(thetaVal);
				result[3] = rad1 * -1 * Math.sin(thetaVal);	//calc derivative w/regard to theta
				result[4] = 0;								//calc derivative w/regard to theta
				result[5] = rad1 * Math.cos(thetaVal);		//calc derivative w/regard to theta
				result[6] = rad1 * -1 * Math.cos(thetaVal);
				result[7] = 0;
				result[8] = rad1 * -1 * Math.sin(thetaVal);
			}
		}//switch
		return result;
	}//calcTorusYVal
	
	/**
	 * this will build a torus mesh centered at 0,0,0, with the given primary rotation axis (currently assume axis is 0,1,0)
	 * with the passed number of "slices" (around primary circle) and "facets" (polies per slice), and given primary and secondary radii
	 * @param slices number of rings around the torus
	 * @param facets number of faces on each ring
	 * @param centerEq the equation describing the center
	 * @param rad1 the radius around the primary circle of the torus
	 * @param rad2 the radius of the tube of the torus
	 */
	public void buildTorusMeshVerts(int slices, int facets, int centerEq, double rad1, double rad2, double knotRad){
		double sliceTheta, sliceThetaNext, facetPhi;//,kurvature, fx, fy, fz;	
		double[] calcAra = null, calcAraNext = null;
		
		int vertNum = 0;
		myVector sliceCenter = new myVector (0,0,0);											//center of each slice - around which each ring of facets will be built
		myVector sliceCenterNext = new myVector(0,0,0);
		myVector facetVec = new myVector(0,0,0);
		myVector facetRes = new myVector(0,0,0);
		double dotResult = 0.0;

//		myVector xProd1 = new myVector(0,1,0);													//x-prod vectors, use first unless colinear with tangent, then use 2nd
//		myVector xProd2 = new myVector(1,0,0);
		
		myVector tangent = new myVector(0,0,0);
//		myVector normal = new myVector(0,0,0);													//normal to tangent - found via x-product of tangent and some unit vector
//		myVector binorm = new myVector(0,0,0);													//perp to norm and tangent = x-prod.
		
		if (slices <24){slices = 24;}															//minimum of 12 slices, 12 facets per slice for our torus
		if (facets <24){facets = 24;}
//		slices = 6;
//		facets = 3;
		//rad2 = 1;
		for (int slice = 0; slice < slices; ++slice){
			sliceTheta = slice * (2 * Math.PI/(slices));																//get angular position currently being calculated 0 -> slices-1 ==> 0 -> 2pi
			sliceThetaNext = (slice+1) * (2 * Math.PI/(slices));															//get angular position currently being calculated 0 -> slices-1 ==> 0 -> 2pi
			calcAra = (calcAraNext == null) ? calcTorusCenterVals(centerEq, rad1, knotRad, sliceTheta) : calcAraNext;		//get coords of current center - use previous "next" coords if there
			calcAraNext = calcTorusCenterVals(centerEq, rad1, knotRad, sliceThetaNext);									//get coords of next center
			
			sliceCenter.set(calcAra[0],calcAra[1],calcAra[2]);															//center is a point on equation around y axis at radius rad1
			sliceCenterNext.set(calcAraNext[0],calcAraNext[1],calcAraNext[2]);
			tangent.set(calcAra[3],calcAra[4],calcAra[5]);																//points at sliceTheta of derivative
			//tangent.set(calcAra[3],calcAra[4],calcAra[5]);

//			tangent.set(myVector._add(sliceCenterNext, new myVector (-1 * sliceCenter.x, -1 * sliceCenter.y, -1 * sliceCenter.z)));
//			tangent._normalize();

//			normal = ((tangent._dot(xProd1) == 0) ? (tangent._cross(xProd2) ) : (tangent._cross(xProd1)));				//if == 0 then colinear, need different unit to find xprod
//			kurvature = normal._mag();																					//kurvature is magnitude of derivative w/respect to arclength
//			normal._normalize();																							
//			binorm = tangent._cross(normal);																			//binormal
//			binorm._normalize();
			
			//need to build tangent by looking at adjacent center points, and then use this tangent to build facet circle since tangent is normal to plane containing desired slice
			//so use v1 - dot(v1,tan)*tan where v1 is calculated points on facet circle
			for(int facet = 0; facet < facets; ++facet){
				vertNum = slice*facets + facet;													//the number of this vertex
				facetPhi = facet * (2 * Math.PI/(facets));										//angular position around center of slice : 0->facets-1 ==> 0 -> 2pi
				
				facetVec.set(sliceCenter.x + (((rad1 + rad2*Math.cos(facetPhi)) * Math.cos(sliceTheta+(1/365.0))) ),			//swap sin and cos slice theta for infinity sign
						(sliceCenter.y + ((rad2*Math.sin(facetPhi))) ),
						sliceCenter.z + ((rad1 + rad2*Math.cos(facetPhi)) * Math.sin(sliceTheta+(1/365.0)) )
						);
				dotResult = facetVec._dot(tangent);																//build projection onto play described by tangent - need normal in here too
				myVector tmpRes = myVector._mult(tangent, -1*dotResult);
				facetRes = myVector._add(facetVec, tmpRes);
				buildVert(vertNum, facetRes.x, facetRes.y, facetRes.z, true,24);											//build a vertex with these coords
				this.vertList.get(vertNum).setBaseCoords(sliceCenter);
			}//for each facet	
			
			
		}//for each slice
			//by here vertlist has all verts of torus, with each slice having facets verts and there being slices number of slices
		this.globalNumVerts = this.vertList.size();
		buildTorusMeshFaces(slices, facets );
		this.globalNumFaces = this.polyList.size();
		
	}//buildTorusMesh 

	/**
	 * this will build various test meshes to test fluid sim and erosion sim
	 * @param type the type of mesh to build
	 * @param multNormal whether the mesh is an enclosed solid
	 * @param numVerts 1/2 the number of verticies per dimension for this mesh (determines the final size)
	 * @param meshOffset determines if the passed type idx needs to be offset due to being a river mesh
	 */
	//public void buildTestMesh(int meshID, boolean multNormal, int meshOffset){
	public void buildTestMesh(int meshID, boolean multNormal){
		p.initVarsForGeneratedMesh(multNormal);
		double[] resAra;
		int vertNum = 0;
		this.numZVerts = this.numVertsPerSide;
		this.numXVerts = this.numVertsPerSide;
//		this.pipesNeighborMeshX = ((meshOffset !=0) && myConsts.riverGenWrapAroundPipesX.get(meshID));		//whether the wrap around nodes on this mesh act as actual neighbors (sharing everything)
//		this.pipesNeighborMeshZ = ((meshOffset !=0) && myConsts.riverGenWrapAroundPipesZ.get(meshID));		//whether the wrap around nodes on this mesh act as actual neighbors (sharing everything)
		this.pipesNeighborMeshX = ((null != myConsts.meshWrapX.get(meshID)) && myConsts.meshWrapX.get(meshID));		//whether the wrap around nodes on this mesh act as actual neighbors (sharing everything)
		this.pipesNeighborMeshZ = ((null != myConsts.meshWrapZ.get(meshID)) && myConsts.meshWrapZ.get(meshID));		//whether the wrap around nodes on this mesh act as actual neighbors (sharing everything)
		boolean wrapAroundVertX = false,		//whether or not this vert wraps around to another in the x direction
				wrapAroundVertZ = false;		//whether or not this vert wraps around to another in the z direction
		
		for (int x = (-1*this.numXVerts); x <= this.numXVerts; ++x){
			for (int z = (-1*numZVerts); z <= numZVerts; ++z){
				//resAra = calcMeshHeightAndRain(x, z, this.numXVerts, numZVerts, meshID + meshOffset);		
				resAra = calcMeshHeightAndRain(x, z, this.numXVerts, numZVerts, meshID );		
				
				//the following calculations are predicated on the order of mesh generation - if the x values are swept through before the z values, this will break
				if (pipesNeighborMeshX){//this node will act like an adjacent node/"pipes"-style neighbor to another node with the same non-wrapping (currently z) coordinate
					p.setFlags(myConsts.wrapAroundMesh, true);
					p.setFlags(myConsts.pullSedFromDryNodes, true);
					if ((x == -1*this.numXVerts) || (x == this.numXVerts)) {		wrapAroundVertX = true;		/*gui.print2Cnsl("wrap node in x : " + x + "," + z);*/	}
					else {															wrapAroundVertX = false;}
				}//if pipes neighbor mesh in X
				
				if (pipesNeighborMeshZ){//this node will act like an adjacent node/"pipes"-style neighbor to another node with the same non-wrapping (currently z) coordinate
					p.setFlags(myConsts.wrapAroundMesh, true);
					p.setFlags(myConsts.pullSedFromDryNodes, true);
					if ((z == -1*numZVerts) || (z == numZVerts)) {					wrapAroundVertZ = true;} 
					else { 															wrapAroundVertZ = false;}
				}//if pipes neighbor mesh in Z
						
				buildVert(vertNum, x, resAra[0] , z,		//res[0] = y val 
						((resAra[1] == 1) ? (true) : (false)),				//whether or not node gets rained on/water 
						this.numVertsPerSide,
						wrapAroundVertX,									//whether or not this node wraps around to another node in x
						wrapAroundVertZ,									//whether to wrap to this node and from this node in z
						resAra);
				vertNum++;  
			}//for each z
		}//for each x  
		
		buildVertAndFaceLists();	
	}//buildTestMesh function
	
	/**
	 * after generating vert list, build corresponding mesh
	 */
	private void buildVertAndFaceLists(){
		this.globalNumVerts = this.vertList.size();
		this.buildFacesFromVertList(numZVerts*2 + 1, this.numXVerts*2 + 1);
		this.globalNumFaces = this.polyList.size();
		
		double vertDimCount = Math.sqrt(this.globalNumVerts);
		if((int)vertDimCount != vertDimCount){	gui.print2Cnsl("illegal mesh geometry - needs to be square. vert count : " + this.globalNumVerts + " verts per side : " + vertDimCount );}
		else {									gui.print2Cnsl(this.globalNumVerts + " , " + this.vertList.size() + " in a square with " + vertDimCount + " vertices per side");	}
		//all verts that will be made are now built.  we need to shift every vertex's coords so that they are 1 unit away from their neighbors and populate verts in 2d vert ara based on coords in x and z shifted to start at 0
		this.vertListAra = new myVertex[(int)vertDimCount][(int)vertDimCount];
		this.faceListAra = new myFace[(int)(vertDimCount)-1][(int)(vertDimCount)-1];
		gui.print2Cnsl("deltaNodeDist : " + this.deltaNodeDist);
		this.buildVertAraFromVertList((int)vertDimCount);//builds ara from vertlist
		//now build face 2d ara
		this.buildFacesFromVertListAra();
		gui.print2Cnsl("vert list ara and face list ara size before subdivide : x : " + this.vertListAra.length + " z : " + this.vertListAra[0].length + "  | x : " + this.faceListAra.length + " z : " + this.faceListAra[0].length);
			//subdivide to make edges smoother, if enabled, and
			//set vertex wrap around if wrap around mesh
		if(p.allFlagsSet(myConsts.subdivRiverMesh)){
			this.subdivide_sqSqMeshAra();
			gui.print2Cnsl("vert list ara and face list ara size after subdivide : x : " + this.vertListAra.length + " z : " + this.vertListAra[0].length + "  | x : " + this.faceListAra.length + " z : " + this.faceListAra[0].length);
			vertDimCount = this.vertListAra.length;
			gui.print2Cnsl("wrap around mesh after subdivide : " + p.allFlagsSet(myConsts.wrapAroundMesh) + " vertDimCount : "+ vertDimCount);
		}
		if(p.allFlagsSet(myConsts.wrapAroundMesh)){				this.setVertWrapAround(vertDimCount);	}
			//set cam distance mult appropriately
		p.cam.camMoveBackVal = numXVerts/13.0;
	}
	
	/**
	 * handle connecting wrap around verts so they perceive each other as neighbors
	 * @param vertDimCount
	 */
	private void setVertWrapAround(double vertDimCount){
		double 	heightDiffX = (this.vertListAra[(int) (vertDimCount-1)][0].coords.y) - (this.vertListAra[0][0].coords.y); 
		double  heightDiffZ = (this.vertListAra[0][(int) (vertDimCount-1)].coords.y) - (this.vertListAra[0][0].coords.y);
		gui.print2Cnsl("height diffs : " + gui.df4.format(heightDiffX) + "|" + gui.df4.format(heightDiffZ) + " neighbors to x : " + this.pipesNeighborMeshX + " to z : " + this.pipesNeighborMeshZ + " vertDimCount : " + vertDimCount);
		gui.print2Cnsl("vert ara id's : [" + (int) (vertDimCount-1) + ",0] = " + this.vertListAra[(int) (vertDimCount-1)][0].ID + "|[0,0] = " + this.vertListAra[0][0].ID + " [0," +(int) (vertDimCount-1) + "] = " +   this.vertListAra[0][(int) (vertDimCount-1)].ID);
			//set height differences between wrap around verts
		double vert2vertHeightDiffx = (this.vertListAra[1][0].coords.y  ) - (this.vertListAra[0][0].coords.y  );  
		//double vert2vertHeightDiffz = this.vertListAra[0][1].coords.y - this.vertListAra[0][0].coords.y;

		if(this.pipesNeighborMeshX){
			for (int zIDX = 0; zIDX < this.vertListAra[0].length; ++zIDX){
				
//				double errorHeightLow = this.vertListAra[0][0].coords.y - 
//										this.vertListAra[0][zIDX].coords.y; //difference from this height to the height used to calculate height diff on entire mesh on low side
//				double errorHeightHigh = 	this.vertListAra[(int) (vertDimCount-1)][0].coords.y - 
//											this.vertListAra[(int) (vertDimCount-1)][zIDX].coords.y; //difference from this height to the height used to calculate height diff on entire mesh on low side
				//if low node is higher then height diff should be less, if high node is higher then height diff should be more
					//need to account for error heights on low and high side
				myVertex wrapLowIDXtoHighIDX = vertListAra[0][zIDX];
				myVertex wrapHighIDXtoLowIDX = vertListAra[this.vertListAra.length-1][zIDX];
					//height of lowidx node w/respect to highidx node
				wrapLowIDXtoHighIDX.setIsMirrorVert(0);
				wrapLowIDXtoHighIDX.setHeightDiffWrapAround(heightDiffX //+ errorHeightDiff 
						+ vert2vertHeightDiffx, 0);
					//height of highidx node w/respect to lowidx node
				wrapHighIDXtoLowIDX.setIsMirrorVert(0);
				wrapHighIDXtoLowIDX.setHeightDiffWrapAround(-1*(heightDiffX //+ errorHeightDiff 
						+ vert2vertHeightDiffx), 0);
			}//populate -x pipes wrap-around nodes with their +x wrap around neighbors
		}//if pipesNeighborMeshX		
		//wrap from low to high z not currently supported
	}//setVertWrapAround	

	/**
	*  calculate height and whether rain should go at a particular grid location in the generated mesh - 
	*  idx 0 is terrain, idx 1 is water, idx 2 is whether this node uses custom erosion vals (0 if no, !0 then this is Kc value for node)
	*  idx 3 is Kd or 0, idx 4 is Ke or 0, idx 5 is Ks or 0 
	* @param x x coord of this location
	* @param z z coord of this location
	* @param xOff 1/2 num verts in x direction
	* @param zOff 1/2 num verts in z direction
	* @param type
	* @return
	*/
	public double[] calcMeshHeightAndRain(double x, double z, int xOff, int zOff, int meshID){
		double[] result = new double[7];
		double 	rvrShallowSlope = this.genRiverSlope, 
				rvrShallowOffset = 4.0, 
				bankShallowOffset = zOff/4.0, 
				bankSteepOffset = zOff/3.0,
				rvrGenWidth = this.riverWidth * zOff;		//width of the river is multiplier times # of nodes per side
		boolean calcComparison, useSteelBottom = false;//useSteelBottom enables/disables the no-erosion of the river bed
		
		//final int nodeHeight = 0, getsH2O = 1, wrapNode = 6, erodes = 7, heightDiff = 8;		
		result[0] = 0;//height at this node
		result[1] = 0;//whether or not this node gets starting/rained on water - amount set in setupDam()
		result[2] = -1;//Kc : -1 here means it uses global value
		result[3] = -1;//Kd : -1 here means it uses global value
		result[4] = -1;//Kw : -1 here means it uses global value
		result[5] = -1;//Ks : -1 here means it uses global value
		result[6] = 0;//whether this node erodes at all - if set to -1 then this node will not erode except for sediment that is converted to height here. 
		
		switch (meshID){
			case myConsts.mesh_ramp	:{ //build dual ramps test mesh
				double mult = (gui.inRange(Math.abs(z), zOff/3.0, 2*zOff/3.0) ? (gui.inRange(z, zOff/3.0, 2*zOff/3.0) ? .25 : .5) : 0);//mult determines how low ramp goes per unit alt
				//depth of mesh
				result[0] = (xOff/5.0) + (mult!= 0 ? -2 - (mult * (xOff + x)) : 3);
				//rained on or not
				result[1] = ((x == -1*xOff) && (mult!= 0) ? 1 : 0);
				break;}
			case myConsts.mesh_coneUp     : {//cone point up
				result[0] = (xOff/5.0) + calcYFromXZCone(x, 0, 1, z, 0, 1, -1 * (xOff/2.1), (xOff/2.0), true);
				result[1] = (((Math.abs(x) < (xOff/7.0)) && (Math.abs(z) < (xOff/7.0f))) ? 1 : 0); 
				break;} //0;
			case myConsts.mesh_coneDown   : {//ice cream cone
				result[0] = (-xOff/2.0) +  Math.sqrt((x*x) + (z*z)) - (xOff/4.0f);    
				result[1] = (((Math.abs(x) == xOff) || (Math.abs(z) == zOff) || (x == (xOff)-1) || (z == (zOff)-1)) ? 1 : 0); 
				break;} //1;
			case myConsts.mesh_2coneDown  : {    
				result[0] = (-xOff/2.0) +  ( Math.min(calcYFromXZCone(x, 3*xOff/10.0, 1.8, z, zOff/5.0, 1.8, 0, xOff/4.0, false),calcYFromXZCone(x, -3*xOff/10.0, 1.5, z,-1*zOff/5.0, 1.5, 0, xOff/10.0, false)));      
				result[1] = (((Math.abs(x) == xOff) || (Math.abs(z) == zOff) || (x == (xOff)-1) || (z == (zOff)-1)) ? 1 : 0); 
				break;} //2;
			case myConsts.mesh_volcano    : {
				double floor = -1 * (xOff/2.1);
				double mountain = calcYFromXZCone(x, 0, 1.5, z, 0, 1.5, floor, xOff/1.1, true),
						bowl = Math.max(calcYFromXZCone(x,-xOff/8.0, 1.7, z, 0, 1.7, xOff/5.0, -1.5*xOff/5.0, false), 3 * xOff/10.0 );
				result[0] =  Math.min(mountain, bowl);
				//result[0] += ((result[0] > floor) && (Math.abs(z * z) < zOff/20.0) && (x >= 2*xOff/8.0) ? (-Math.sqrt((xOff/20.0)*(xOff/20.0)) + (z * z)) : (0));
				result[1] = (((Math.abs(x - xOff/8.0) < xOff/10.0) && (Math.abs(z) < zOff/10.0)) ? 1 : 0); 
				result[2] = ((result[1] == 1) ? 0 : -1);	//no erosion in center of volcano
				break;} //3;
			case myConsts.mesh_2volcano   : { 
				double floor = -1 * (xOff/2.1);
				double mountain1 = calcYFromXZCone(x, xOff/6.0, 1.5, z, 0, 1.5, floor, xOff/1.1, true),
						mountain2 = calcYFromXZCone(x, -xOff/3.0, 1.5, z, 0, 1.5, floor, xOff/2.5, true),
						bowl = Math.max(calcYFromXZCone(x,xOff/8.0, 1.7, z, 0, 1.7, xOff/5.0, -1.5*xOff/5.0, false), 5 * xOff/10.0 );
				result[0] =  Math.max(Math.min(mountain1, bowl), mountain2);
				result[1] = (((Math.abs(x+xOff/8.0) < xOff/10.0) && (Math.abs(z) < zOff/10.0)) ? 1 : 0);   
				result[2] = ((result[1] == 1) ? 0 : -1);	//no erosion in center of volcano
				break;} //4;  
			case myConsts.mesh_dam : {
				result[1] = ((z > 0) ? (1) : (0));
				break;}  
			case myConsts.mesh_damWall : {
				result[0] = (gui.inRange(z,-(2*zOff/7.0),-(2*zOff/6.0)) ?   (Math.min(zOff/2.5 , Math.abs(x*z))) : 0);
				result[1] = ((z > 0) ? (1) : (0));
				break;}  
			case myConsts.mesh_riverLedge : {
				result[0] = ((z < -(zOff/5.0)) ? (zOff/2.0) : (0));
				result[1] = ((z > (zOff/10.0)) ? (1) : (0));
				break;}  			
			case myConsts.mesh_column : {
				result[0] = 1;
				result[1] = (((Math.abs(x + xOff/4.0) <= xOff/5.0 ) && (Math.abs(z+ zOff/4.0) <= zOff/5.0)) ? (1) : (0));
				break; }    
			case myConsts.mesh_tiltColumn : {
				result[0] = Math.min(-x + xOff/10.0, -z+zOff/10.0);
				result[0] = (3 * xOff/20.0) + Math.min(result[0],0);
				result[1] = (((Math.abs(x) <= xOff/10.0 ) && (Math.abs(z) <= zOff/10.0)) ? (1) : (0));
				break; }    
			case myConsts.mesh_tiltShelfColumn : {
				result[0] = Math.min(-x+1,-z+1);
				result[0] = (((gui.inRange(-x,-xOff * .3f,-xOff *.325f) && -z > -zOff *.325f) || (gui.inRange(-z,-zOff * .3f,-zOff *.325f) && (-x > -xOff *.325f))) ? -1 : result[0] );
				result[0] = Math.min(result[0],0) + xOff/20.0f;
				result[1] = (((Math.abs(x) <= xOff/10.0 ) && (Math.abs(z) <= zOff/10.0)) ? (1) : (0));
				break;}    
//wrap around rivers : rvrGenWidth = rvrWidth * zOff (percentage of width of river)
			case myConsts.mesh_riverPrimary : { 
				calcComparison = rvrCalcComparison(rvrGenWidth, this.riverWrapAroundPeriodMult * 2 * Math.PI/(2.0 * xOff), rvrGenWidth, x, z);
				result[0] = rvrCalcIncline(rvrShallowOffset, rvrShallowSlope,0,x)  + (calcComparison ? -1 : rvrCalcDryLandVal(z, zOff, bankShallowOffset));
				result[1] = (calcComparison ? 1 : 0);
				if (useSteelBottom){result[6] = (calcComparison ? (-1): (0));}		//if base of river then only allow erosion of sediment added to original height, so deposit/erode only added height to original
				break;}
			case myConsts.mesh_riverStraightPier : {	//straight river with pier
				boolean pierDef = ((z <= 0) || (Math.abs(x) >= rvrGenWidth/4.0));
				calcComparison = rvrCalcComparison(0, 0 , rvrGenWidth ,x ,z);
				result[0] = rvrCalcIncline(rvrShallowOffset, rvrShallowSlope,0,x)  + (calcComparison && pierDef ? 0 : rvrCalcDryLandVal(z, zOff, bankShallowOffset));
				result[1] = ((calcComparison && pierDef)? 1 : 0);
				result[2] = (pierDef ? .0005 : -1);//make piers much harder to erode
				if (useSteelBottom){result[6] = (calcComparison ? (-1): (0));}		//if base of river then only allow erosion of sediment added to original height, so deposit/erode only added height to original
				break;}		
			case myConsts.mesh_riverStraightTributary : {	//straight river with side tributary
				boolean tribDef = ((z>0) && (Math.abs(x) < rvrGenWidth/2.0)) ;
				calcComparison = rvrCalcComparison(0, 0 , rvrGenWidth ,x ,z);
				result[0] = rvrCalcIncline(rvrShallowOffset, rvrShallowSlope,0,x)  + (calcComparison || tribDef ? 0 : rvrCalcDryLandVal(z, zOff, bankShallowOffset));
				result[1] = ((calcComparison)  ? 1 : 0);  //add && tribDef to pre-populate river trib with water
				if (useSteelBottom){result[6] = (calcComparison ? (-1): (0));}		//if base of river then only allow erosion of sediment added to original height, so deposit/erode only added height to original
				//result[8] = -1.0/rvrShallowSlope;
				break;}		
			case myConsts.mesh_riverStraight2Pier : {	//straight river with pier
				boolean pier1Def = ((z <= 0) || (Math.abs(x -  rvrGenWidth) >= rvrGenWidth/4.0 )) ;
				boolean pier2Def = ((z >= 0) || (Math.abs(x +  rvrGenWidth) >= rvrGenWidth/4.0 )) ;
				calcComparison = rvrCalcComparison(0, 0 , rvrGenWidth ,x ,z);
				result[0] = rvrCalcIncline(rvrShallowOffset, rvrShallowSlope,0,x)  + (calcComparison && pier1Def && pier2Def ? 0 : rvrCalcDryLandVal(z, zOff, bankShallowOffset));
				result[1] = ((calcComparison && pier1Def && pier2Def)? 1 : 0);
				result[2] = ((pier1Def  || pier2Def) ? .0005 : -1);//make piers much harder to erode
				if (useSteelBottom){result[6] = (calcComparison ? (-1): (0));}		//if base of river then only allow erosion of sediment added to original height, so deposit/erode only added height to original
				break;}		
			case myConsts.mesh_riverStraight2Tributary : {	//straight river with side tributary
				boolean trib1Def = ((z > 0) && (Math.abs(x +  rvrGenWidth ) < rvrGenWidth/3.0)) ;
				boolean trib2Def = ((z < 0) && (Math.abs(x -  rvrGenWidth ) < rvrGenWidth/3.0)) ;
				calcComparison = rvrCalcComparison(0, 0 , rvrGenWidth, x, z);
				result[0] = rvrCalcIncline(rvrShallowOffset, rvrShallowSlope,0,x)  + (calcComparison || trib1Def || trib2Def ? 0 : rvrCalcDryLandVal(z, zOff, bankShallowOffset));
				result[1] = ((calcComparison)  ? 1 : 0);  //add && tribDef to pre-populate river trib with water
				if (useSteelBottom){result[6] = (calcComparison ? (-1): (0));}		//if base of river then only allow erosion of sediment added to original height, so deposit/erode only added height to original
				break;}		

			case myConsts.mesh_riverStraight2Trib2Pier : {	//straight river 2trib 2 pier
				boolean pier1Def = ((z <= 0) || (Math.abs(x -  rvrGenWidth) >= rvrGenWidth/4.0 )) ;
				boolean pier2Def = ((z >= 0) || (Math.abs(x +  rvrGenWidth) >= rvrGenWidth/4.0 )) ;
				boolean trib1Def = ((z > 0) && (Math.abs(x +  rvrGenWidth ) < rvrGenWidth/3.0)) ;
				boolean trib2Def = ((z < 0) && (Math.abs(x -  rvrGenWidth ) < rvrGenWidth/3.0)) ;
				calcComparison = rvrCalcComparison(0, 0 , rvrGenWidth ,x ,z);
				result[0] = rvrCalcIncline(rvrShallowOffset, rvrShallowSlope,0,x)  + (((calcComparison || trib1Def || trib2Def) && pier1Def && pier2Def  ) ? 0 : rvrCalcDryLandVal(z, zOff, bankShallowOffset));
				result[1] = ((calcComparison && pier1Def && pier2Def )? 1 : 0);
				result[2] = ((pier1Def  || pier2Def) ? .0005 : -1);//make piers much harder to erode
				if (useSteelBottom){result[6] = (calcComparison ? (-1): (0));}		//if base of river then only allow erosion of sediment added to original height, so deposit/erode only added height to original
				break;}		
			//build parabolic valleys with rocks where water will build river
			case myConsts.mesh_riverFlatTerrain : {//no boulders, just valley with water starting at top 
				calcComparison = rvrCalcComparison(rvrGenWidth, this.riverWrapAroundPeriodMult * 2 * Math.PI/(2.0 * xOff), rvrGenWidth, x, z);
				result[0] = rvrCalcIncline(rvrShallowOffset, rvrShallowSlope,0,x)  + (calcComparison ? -1 * zOff/20.0 : zOff/3.0);
				result[1] = (calcComparison ? 1 : 0);
				if (useSteelBottom){result[6] = (calcComparison ? (-1): (0));}		//if base of river then only allow erosion of sediment added to original height, so deposit/erode only added height to original
				break;}
			
			case myConsts.mesh_river1Boulder : {//1 boulder
				double boulderRad1 = xOff/5.0;
				double xzCalc1 = (x * x) + (.5 * (z+zOff/8.0) * (z+zOff/8.0));
				boolean boulder1 = (Math.sqrt(xzCalc1) <= boulderRad1);
				result[0] = rvrCalcIncline(rvrShallowOffset, rvrShallowSlope,0,x)  + (rvrCalcDryLandVal(z/1.5, zOff, bankShallowOffset)) 
						+ ((boulder1) ? Math.sqrt((boulderRad1 * boulderRad1) - (xzCalc1)) : 0); 
						
				result[1] = ((z == 0) && (x == -1*xOff)) ? 1 : 0; 
				result[2] = (boulder1 ? 0 : -1);//set kc to be 0 if this is a boulder
				break; }
			
			case myConsts.mesh_river2Boulder : {//2 boulders
				double boulderRad1 = xOff/5.0, xzCalc1 = ((x-xOff/3.0) * (x-xOff/3.0)) + (.5 * (z+zOff/8.0) * (z+zOff/8.0));
				boolean boulder1 = (Math.sqrt(xzCalc1) <= boulderRad1);
				double boulderRad2 = xOff/4.0,  xzCalc2 = ((x+xOff/3.0) * (x+xOff/3.0)) + (.5 * (z-zOff/8.0) * (z-zOff/8.0));
				boolean boulder2 = (Math.sqrt(xzCalc2) <= boulderRad2);
				result[0] = rvrCalcIncline(rvrShallowOffset, rvrShallowSlope,0,x)  + (rvrCalcDryLandVal(z/1.5, zOff, bankShallowOffset)) 
						+ ((boulder1) ? Math.sqrt((boulderRad1 * boulderRad1) - (xzCalc1)) : 0) 
						+ ((boulder2) ? Math.sqrt((boulderRad2 * boulderRad2) - (xzCalc2)) : 0);
				result[1] = 1;//rain everywhere 
				result[2] = ((boulder1 || boulder2) ? 0 : -1);
				break; }
			
			case myConsts.mesh_river3Boulder : {//3 boulders
				double boulderRad1 = xOff/5.0, xzCalc1 = ((x-xOff/4.0) * (x-xOff/4.0)) + (.5 * (z+zOff/8.0) * (z+zOff/8.0));
				boolean boulder1 = (Math.sqrt(xzCalc1) <= boulderRad1);
				double boulderRad2 = xOff/4.0, xzCalc2 = ((x+xOff/3.0) * (x+xOff/3.0)) + (.5 * (z-zOff/8.0) * (z-zOff/8.0));
				boolean boulder2 = (Math.sqrt(xzCalc2) <= boulderRad2);
				double boulderRad3 = xOff/6.0, xzCalc3 = ((x-(3*xOff/4.0)) * (x - (3*xOff/4.0))) + (.5 * (z-zOff/8.0) * (z-zOff/8.0));
				boolean boulder3 = (Math.sqrt(xzCalc3) <= boulderRad3);
				result[0] = rvrCalcIncline(rvrShallowOffset, rvrShallowSlope,0,x)  + (rvrCalcDryLandVal(z/1.5, zOff, bankShallowOffset)) 
						+ ((boulder1) ? Math.sqrt((boulderRad1 * boulderRad1) - (xzCalc1)) : 0) 
						+ ((boulder2) ? Math.sqrt((boulderRad2 * boulderRad2) - (xzCalc2)) : 0)
						+ ((boulder3) ? Math.sqrt((boulderRad3 * boulderRad3) - (xzCalc3)) : 0);
				result[1] = 1;//rain everywhere
				result[2] = ((boulder1 || boulder2 || boulder3) ? 0 : -1);
				break; }
			
			case myConsts.mesh_riverGen4 : {//zig-zag boulders
				result[0] = rvrCalcIncline(rvrShallowOffset, rvrShallowSlope,0,x)  + (rvrCalcDryLandVal(z/1.5, zOff, bankShallowOffset));
				result[1] = 1; 	//rain everwhere
				result[2] = -1;	//erode everywhere - modify boulder spots in foor loop below
				int numBoulders = 8;
				double boulderRad = xOff/(numBoulders + 2.5),
						boulderHeight = xOff/12.0,
						xzCalc, 
						xDist = ((2 * (xOff))/(numBoulders)),
						xCtr = -xOff - (boulderRad), 
						zCtr = zOff/(numBoulders),
						zMult = .2;		//half radius from z centerline
				
				boolean boulderSpot; 
				
				for (int i = 0; i < numBoulders; ++i){
					xCtr += xDist;
					zCtr *=-1;		//to alternate on either side of the center line
					xzCalc = (((x+xCtr) * (x+xCtr)) + (zMult * (z+zCtr) * (z+zCtr)));
					boulderSpot = (Math.sqrt(xzCalc) <= boulderRad);
					result[0] += ((boulderSpot) ? boulderHeight * Math.sqrt(boulderRad - Math.sqrt(xzCalc)) : 0);
					result[2] = ((boulderSpot) ? 0 : result[2]);		//set to unerodeable if this is a spot on a boulder, otherwise keep what it was
				}

				break; }
			
			
			case myConsts.mesh_riverColumn : {//column of terrain in center, water all around
				result[0] = (((Math.abs(x) <= xOff/10.0 ) && (Math.abs(z) <= zOff/10.0)) ? (xOff) : (0));
				result[1] = (((x*x + z*z) > xOff/1.2f * zOff/1.2f) ? (1) : (0));//(((Math.abs(x) >= 4.0*xOff/5.0 ) || (Math.abs(z) >= 4.0*xOff/5.0 )) ? (1) : (0));
				break;
			}
			case myConsts.mesh_riverManyColumns : {
//				result[0] = ((((Math.abs(x+1) <= xOff/5.0 ) && (Math.abs(z+1) <= zOff/5.0)) && (((Math.abs(x+1) > xOff/10.0  ) && (Math.abs(z+1) > zOff/10.0)))) || 
//						(((Math.abs(x-3) <= xOff/5.0 ) && (Math.abs(z-3) <= zOff/5.0)) && (((Math.abs(x-3) > xOff/10.0  ) && (Math.abs(z-3) > zOff/10.0))))  
//					? (xOff/2.0f) : (0));
				result[0] = ((((Math.abs(x)+xOff/20.0 <= xOff/5.0 ) && (Math.abs(z)+zOff/20.0 <= zOff/5.0)) && (((Math.abs(x)+xOff/20.0 > xOff/10.0  ) && (Math.abs(z)+zOff/20.0 > zOff/10.0)))) || 
					(((Math.abs(x)-xOff/10.0 <= xOff/5.0 ) && (Math.abs(z)-zOff/10.0 <= zOff/5.0)) && (((Math.abs(x)-xOff/10.0 > xOff/10.0  ) && (Math.abs(z)-zOff/10.0 > zOff/10.0))))  
					? (xOff/3.0f) : (0));
				result[1] = (((x*x + z*z) > xOff/1.2f * zOff/1.2f) ? (1) : (0));
				break;
			}			
			case myConsts.mesh_columnWall : {
				result[0] = ((((x*x + z*z) > xOff *.4f * zOff *.4f ) && ((x*x + z*z) < xOff*.5f * zOff*.5f) ) ? xOff * .35f: (0));
				result[1] = (((Math.abs(x) <= xOff/8.0 ) && (Math.abs(z) <= xOff/8.0 )) ? (1) : (0));//(((x*x + z*z) < xOff *.1f * zOff *.1f ) ? (1) : (0));   
				break;
			}
			case myConsts.mesh_waterAroundColumnWall : {
				result[0] = ((((x*x + z*z) > xOff/2.0f * zOff/2.0f ) && ((x*x + z*z) < xOff*.6f * zOff*.6f) ) ? (xOff*.7f) : (0));
				result[1] = (((x*x + z*z) > xOff/1.2f * zOff/1.2f) || ((x*x + z*z) < xOff/8.0f * zOff/8.0f ) ? (1) : (0));  
				break;
			}
			case myConsts.mesh_damBetweenWalls: {
				result[0] = ((gui.inRange(Math.abs(z), zOff * .4f, zOff * .45f)) ? (2.5f * Math.min(4, Math.abs(x*z))) : 0);
				result[1] = ((Math.abs(z) < zOff * .15f) ? (1) : (0));
				break;
			}
			case myConsts.mesh_damBetweenLargeRiver : {
				calcComparison = (Math.abs(z) < zOff * .4f) && (Math.abs(z) > zOff/20.0f);
				result[0] = rvrCalcIncline(rvrShallowOffset, rvrShallowSlope,0,x)  + (calcComparison ? 0 : rvrCalcDryLandVal(z, zOff, bankSteepOffset));
				
				result[1] = ((((Math.abs(z + zOff/5.0f) < zOff/10.0f) && (x <= -xOff/20.0f)) || ((Math.abs(z - zOff/5.0f) <  zOff/10.0f) && (x >= xOff/20.0f))) ? (1) : (0));

				if (useSteelBottom){result[6] = (calcComparison ? (-1): (0));}		//if base of river then only allow erosion of sediment added to original height, so deposit/erode only added height to original
				break;
			}
			default : { result[0] = 0; result[1] = 1;}  //flat mesh with rain everywhere
		}//switch
		//force every wraparound mesh z edge to be unerodeable - only process if result[2] hasn't already been set
		if(result[2] == -1){result[2] = ((p.allFlagsSet(myConsts.wrapAroundMesh)) && (Math.abs(z) == zOff)) ? 0 : -1;}//kc setting - make z edges non-erodeable if wrap mesh	
		return result; 
	}//calcMeshHeightAndRain function
	
	/**
	 * this will determine whether a particular x/z pair is within the bounds of an equation defined by the coefficients of the form a sin (b * x) + c <= z || a sin (b * x) - c >= z 
	 * @return whether or not the given x/z pair are within the bounds of the equation defined by the constants
	 */
	public boolean rvrCalcComparison(double a, double b, double c, double x, double z){	return !((a * Math.sin(b * x) + c <= z) || (a * Math.sin(b * x) - c >= z));}
	
	public double rvrCalcDryLandVal(double z, double zOff, double bankOffset){		return (bankOffset +  bankOffset * ((z * z)/(.5*zOff*zOff)));}
	
	/**
	 * this will calculate the appropriate height value for a particular x value using the equation (-1 * (x + a)/b) + c;
	 * @return the height at a given x
	 */
	public double rvrCalcIncline(double a, double b, double c, double x){ 				return ((-1* (x + a)/b) + c);}

	/**
	*  calculate a y value based on passed x and z and orientation
	*/
	public double calcYFromXZCone(double x, double x0, double xMult, double z, double z0, double zMult, double thresh, double elev, boolean up){
		double result = 0;
		int multiplier = ((up) ? (-1) : (1));
		if (up){result = Math.max(thresh, multiplier * (Math.sqrt((xMult * (x + x0) * xMult * (x + x0)) + (zMult *(z + z0) * zMult *(z + z0))) - elev));} 
		else {	result = Math.sqrt((xMult * (x + x0) * xMult *(x + x0)) + (zMult *(z + z0) * zMult *(z + z0))) - elev;}
		return result;
	}//calcYFromXZCone
	
	/**
	 * prints out all the current verts' toString();
	 */
	public void printVerts(){
		if((this.vertListAra != null) && (this.vertListAra.length != 0) && (this.vertListAra[0].length != 0)){
			for (int i = 0; i < this.vertListAra.length; ++i){	
				for(myVertex vert : this.vertListAra[i]){ gui.print2Cnsl(vert.toString()); }//for each vert		
			}			
		} else {		for(myVertex vert : this.vertList.values()){ gui.print2Cnsl(vert.toString()); }} //for each vert				
		
	}//print verts
	
	/**
	 * prints out all current faces' toStringWithoutVerts
	 */
	public void printFaces(){
		if((this.faceListAra != null) && (this.faceListAra.length != 0) && (this.faceListAra[0].length !=0)){
			for(int i = 0; i < this.faceListAra.length; ++i){
				for(myFace face : this.faceListAra[i]){		gui.print2Cnsl(face.toStringWithoutVerts());	}
			}
		} else {
			for(myFace face : this.polyList.values()){		gui.print2Cnsl(face.toStringWithoutVerts());	}
		}
	}//printFaces//for each face
	
	/**
	 * print out the vert and face aras uses for jni functionality
	 * 
	 */
	public void printOutVertFaceAras(){//no jni
//		if(this.vertIDsInSeq != null){
//			for (int i = 0; i < this.vertIDsInSeq.length; ++i){
//				gui.print2Cnsl("VertID ara vertnum : " + vertIDsInSeq[i] + " at idx : " + i +  "| Coords for vert : " + this.vertIDWithCoords[i][0] + "|"+ this.vertIDWithCoords[i][1] +"|" + this.vertIDWithCoords[i][2]   );
//			}
//			gui.print2Cnsl("");
//			for (int i = 0; i < this.faceIDsInSeq.length; ++i){
//				gui.print2Cnsl("FaceID ara facenum: " + this.faceIDsInSeq[i] +  " at idx :  " + i + "| vert id's for this face : " 
//									+ this.faceIDOwnsVertIDs[i][0] + "|" + this.faceIDOwnsVertIDs[i][1] + "|" + this.faceIDOwnsVertIDs[i][2] + "|" + this.faceIDOwnsVertIDs[i][3] + "|" ); 
//			}
//		}
	}//printOutVertFaceAras - for jni debuging
	
	/**
	*  Read quad-based polygon mesh from .ply file, for use in sqsq subdivision
	*/
	public void read_quadMesh(String filename, boolean multNormal){
		p.setFlags(myConsts.multByNormal, multNormal);
		//string holding path to file
		String qualFileName = p.sketchPath() + myGui.fileDelim + myGui.polyPath + myGui.fileDelim + filename;
		//string arrays holding the words on a line, alt array holding words on a line, array holding lines of poly file
		String[] words, words1, lines = p.loadStrings(qualFileName);  
		//number of vertices in file, number of faces in file, tmp var holding location of a particular string in the file array, 
		//number of verts per face, set to 4 in quad mesh, offset to account for initial lines in file to determine where to start reading data
		int numVertices, numFaces, fileLine, nVerts = 4, fileOff;
		int[] index;
		double x,y,z;
		myVertex[] tmpVertArray;
		//boolean buildCenter = false;
		
		words = PApplet.split (lines[0], " ");//gives number of vertices
		numVertices = Integer.parseInt(words[1]);
		words = PApplet.split (lines[1], " ");//gives number of faces
		numFaces = Integer.parseInt(words[1]);
		if (lines[2].contains("center")){ fileOff = 3; /*buildCenter = true; */} 
		else { fileOff = 2; }
		
		//use special code algorithm for square square subdivision
		p.setFlags(myConsts.sqSqSub,true);
		//draw QUADS as the type of polygon primitive
		//p.polyType = PApplet.QUADS;
		// read in the vertices
		for (int vertNum = 0; vertNum < numVertices; vertNum++) {
			words = PApplet.split (lines[vertNum+fileOff], " ");
			x = Float.parseFloat(words[0]);
			y = Float.parseFloat(words[1]);
			z = Float.parseFloat(words[2]);
			
			buildVert(vertNum, x, y, z,true, (int)(Math.sqrt(numVertices))); 
		}//for each vertex
		this.globalNumVerts = this.vertList.size();
		
		//array of faces by id
		this.faceIDsInSeq = new int[((numFaces == 1) ? (4) : (numFaces))];
		//array of faces with the 4 verts that make up each face
		this.faceIDOwnsVertIDs = new int[((numFaces == 1) ? (4) : (numFaces))][4];
			
		//array of sequence of vert ids
		this.vertIDsInSeq = new int[this.vertList.size()];
		//array of vert ids with the face ids adjacent to them
		//	  vertIDOwnsFaceIDs = new int[this.vertList.size()][4];
		//array of vert id's with their coords	
		this.vertIDWithCoords = new double[this.vertList.size()][3];
		int incr = 0;
		for(Integer idx : this.vertList.keySet()){
			this.vertIDsInSeq[incr] = this.vertList.get(idx).vertNum;
			//get coords of all verts, put in coord array
			this.vertIDWithCoords[incr][0] = vertList.get(idx).coords.x;
			this.vertIDWithCoords[incr][1] = vertList.get(idx).coords.y;
			this.vertIDWithCoords[incr][2] = vertList.get(idx).coords.z;
			incr++;
		}  
		//sanity check
		this.globalNumVerts = numVertices;
		if (this.globalNumVerts != this.vertList.size()){gui.print2Cnsl("error creating vertex list");} 
		if (numFaces == 1) {
			fileLine = numVertices + fileOff;                      //calculate correct offset in file for where faces start
			words = PApplet.split (lines[fileLine], " ");
			readSingleQuad(words);                                //splits into 4 for square square subdivision
		} else { 
			// read in the faces from mesh file 
			for (int faceNum = 0; faceNum < numFaces; faceNum++) {
				fileLine = faceNum + numVertices + fileOff;//calculate correct offset in file for where faces start
				words = PApplet.split (lines[fileLine], " ");      
				index = new int[nVerts];
				//verticies used by this face
				tmpVertArray = new myVertex[nVerts];
				//gui.print2Cnsl("reading in face " + faceNum + "|" + numFaces); 
				for (int idx = 0; idx < nVerts - 1; idx++){
					//find vertex in vertList hashmap based on vertex index, which is how each vertex of each face is referenced in description file
					index[idx] = Integer.parseInt(words[idx+1]);
					tmpVertArray[idx] = this.vertList.get(index[idx]);
				}//for each vertex in face
				if (Integer.parseInt(words[0]) == 3) {//intended to build mesh originally of triangle polys, where subsequent line's last idx provides 4th idx for quad
					words1 = PApplet.split(lines[fileLine+1]," ");
					index[3] = Integer.parseInt(words1[3]);
					faceNum++;//skip next line - combining 2 consequtive lines in triangle mesh 
				} else if (Integer.parseInt(words[0]) == 4) {
					index[3] = Integer.parseInt(words[4]);
				}      
				tmpVertArray[3] = this.vertList.get(index[3]);  
				buildFace(faceNum, nVerts, tmpVertArray, true, false);		//TODO change to put in ara
				
			}//for each face
			this.globalNumFaces = this.polyList.size();
			incr = 0;
			for(Integer idx : this.polyList.keySet()){
				this.faceIDsInSeq[incr] = this.polyList.get(idx).getFaceNum();
				//get ids of all verts making up this face getVertByIdx
				this.faceIDOwnsVertIDs[incr][0] = this.polyList.get(idx).getVertByIDX(0).vertNum;
				this.faceIDOwnsVertIDs[incr][1] = this.polyList.get(idx).getVertByIDX(1).vertNum;
				this.faceIDOwnsVertIDs[incr][2] = this.polyList.get(idx).getVertByIDX(2).vertNum;
				this.faceIDOwnsVertIDs[incr][3] = this.polyList.get(idx).getVertByIDX(3).vertNum;
				incr++;
			}    
		}//if not only 1 face
		gui.print2Cnsl("dist between verts : " + this.deltaNodeDist + " mult by normal mesh " + ((p.allFlagsSet(myConsts.multByNormal)) ? " yes " : " no "));
		if(p.allFlagsFalse(myConsts.multByNormal)){//build 2d ara of verts and faces
			int dimVal = (int)Math.sqrt(this.vertList.size());
			gui.print2Cnsl("" +dimVal);
			this.faceListAra = new myFace[dimVal-1][dimVal-1];		//1 less face per dim than verts
			this.vertListAra = new myVertex[dimVal][dimVal];
			//ONLY USE FOR SQUARE MESHES		TODO:
			buildVertAraFromVertList(dimVal);
				//set up face 2dara
			for(myFace face : this.polyList.values()){	
				this.faceListAra[face.getVertByIDX(0).xIDX][face.getVertByIDX(0).zIDX] = face;	
				//gui.print2Cnsl("face id : "  + face.ID + " placed at x : " + face.getVertByIDX(0).xIDX + " z : " + face.getVertByIDX(0).zIDX);
			}
			
		} else {//set 2d aras to null for enclosed meshes
			this.faceListAra = null;
			this.vertListAra = null;
			
		}

	}//read quad_mesh method
	
	/**
	 * update mesh verts with new coords from jni
	 */
	public void sendArrayUpdateVertCoords(){
		gui.print2Cnsl("JNI invoked vert coord update");
		for (int idx = 0; idx < this.vertIDsInSeq.length; idx++){
			int vertNum = this.vertIDsInSeq[idx];
			this.vertList.get(vertNum).setCoords(new myVector(this.vertIDWithCoords[idx][0],this.vertIDWithCoords[idx][1],this.vertIDWithCoords[idx][2]));
		}//for each vert	
	}//updatevertcoords
	
	/**
	 * update mesh based on new array values - invoked by jni after jni updated array fields directly
	 */
	public void sendArrayUpdate(){
		gui.print2Cnsl("JNI invoked mesh update");
		this.vertList.clear();
		this.polyList.clear();
		//first make all verts from values in arrays
		for (int incr = 0; incr < this.vertIDsInSeq.length; incr++){
			myVertex tmpVert = new myVertex(this.p, this.gui, this.eng, this,  this.vertIDsInSeq[incr], this.vertIDWithCoords[incr][0],this.vertIDWithCoords[incr][1],this.vertIDWithCoords[incr][2],0,0 );
			this.vertList.put(tmpVert.vertNum, tmpVert);
		}//make all verts
		this.globalNumVerts = this.vertList.size();
		for(int incr = 0; incr < this.faceIDsInSeq.length; incr++){
			myVertex[] tmpVertAra = new myVertex[4];
			tmpVertAra[0] = this.vertList.get( this.faceIDOwnsVertIDs[incr][0]);
			tmpVertAra[1] = this.vertList.get( this.faceIDOwnsVertIDs[incr][1]);
			tmpVertAra[2] = this.vertList.get( this.faceIDOwnsVertIDs[incr][2]);
			tmpVertAra[3] = this.vertList.get( this.faceIDOwnsVertIDs[incr][3]);
			myFace tmpFace = this.buildFace(this.faceIDsInSeq[incr], tmpVertAra.length, tmpVertAra, true, false);		//TODO change to put in ara
			gui.print2Cnsl(tmpFace.toString());
//			myFace tmpFace = new myFace(this.p, this.gui, this.eng, this, this.faceIDsInSeq[incr], 4, tmpVertAra);
//			this.polyList.put(tmpFace.getFaceNum(), tmpFace);				
		}//make all faces, put in polylist
		this.globalNumFaces = this.polyList.size();
	}//sendArrayUpdate
	
	/**
	 * accepts results from jni function and populates the local arrays with these values
	 * @param _vertIDsInSeq
	 * @param _faceIDsInSeq
	 * @param _faceIDOwnsVertIDs
	 * @param _vertIDWithCoords
	 */
	public void sendArrayResults(int[] _vertIDsInSeq, int[] _faceIDsInSeq, int[][] _faceIDOwnsVertIDs, double[][] _vertIDWithCoords){
		this.vertIDsInSeq = _vertIDsInSeq;
		this.faceIDsInSeq = _faceIDsInSeq;
		this.faceIDOwnsVertIDs = _faceIDOwnsVertIDs;
		this.vertIDWithCoords = _vertIDWithCoords;	
		this.sendArrayUpdate();		//update mesh with new array values
	}//sendArrayResults
	
	/**
	 * function to build a vertex with passed id and values - overload without "useNodeErosionVals
	 * @param vertNum the number of this vertex - needs to be unique
	 * @param x x coord
	 * @param y y coord
	 * @param z z coord
	 * @param rainOnMe whether this vertex gets rain/water on it when its heightnode is generated
	 */
	public void buildVert (int vertNum, double x, double y, double z, boolean rainOnMe, int vertDim){this.buildVert(vertNum, x, y, z, rainOnMe,vertDim, false, false, null);}

	/**	 
	 * function to build a vertex with passed id and values
	 * @param vertNum the number of the vert to build
	 * @param x the x coord of the vert
	 * @param y y coord of vert
	 * @param z z coord of vert
	 * @param whether or not node gets rain and potentially an initial vol of h2o
	 * @param wrapInto - if this node is recipient of river wrap-around - with only h2o being copied
	 * @param wrapAround - if this node copies its water height value into another node 
	 * @param resAra array of information about generated meshes, with custom erosion vals, whether or not this node experiences any erosion, or only erosion of new sediment, is a wrap around node, etc
	 */
	public void buildVert (int vertNum, double x, double y, double z, boolean rainOnMe, int vertDim, boolean wrapAroundX, boolean wrapAroundZ, double[] resAra){
		myVertex tmpVert, putResult;
		boolean useNodeKc = false,useNodeKd = false,useNodeKs = false,useNodeKw = false, useOriginalHeight = false ;
		if(resAra != null){//if array not null         
			useNodeKc = (resAra[2] != -1); 		//TODO: fix this - should be able to modify each erosion val separately
			useNodeKd = (resAra[3] != -1); 		//TODO: fix this - should be able to modify each erosion val separately
			useNodeKw = (resAra[4] != -1); 		//TODO: fix this - should be able to modify each erosion val separately
			useNodeKs = (resAra[5] != -1); 		//TODO: fix this - should be able to modify each erosion val separately
			useOriginalHeight = (resAra[6] == -1);
		}
		tmpVert = new myVertex(this.p, this.gui, this.eng, this, vertNum,x,y,z,0,0, rainOnMe);
		if(useNodeKc){tmpVert.setUseNodeKc(true); tmpVert.setErosionVals(eng.Kc,resAra[2]);}
		if(useNodeKd){tmpVert.setUseNodeKd(true); tmpVert.setErosionVals(eng.Kd,resAra[3]);}
		if(useNodeKw){tmpVert.setUseNodeKw(true); tmpVert.setErosionVals(eng.Kw,resAra[4]);}
		if(useNodeKs){tmpVert.setUseNodeKs(true); tmpVert.setErosionVals(eng.Ks,resAra[5]);}
		if(wrapAroundX){tmpVert.setIsMirrorVert( 0);}		//this node sends fluid and sediment across wrap-around boundary in x
		if(wrapAroundZ){tmpVert.setIsMirrorVert( 1);}		//this node sends fluid and sediment across wrap-around boundary in z
		if(useOriginalHeight){tmpVert.setUseOriginalHeight(true);}//means only erode height that has been added to this node via sediment deposit - so can add here but not subtract beyond original height
		//return val putResult used to test put operation
		putResult = this.vertList.put(vertNum,tmpVert);
		if (putResult != null) { gui.print2Cnsl("vertex " + vertNum + " : (" + x + ", " + y + ", " + z + ") already in list");   } 
		//gui.print2Cnsl("vertNum : " + vertNum +  "| x,y,z : " + x +"|"+y+"|"+z); 	  
	}//buildVert

	/**
	*  function to build a face from passed Id, number of verts and vertex array, add face to each vert's adjacency list, and add face to global poly list
	*/
	public myFace buildFace(int faceNum, int nVerts, myVertex[] tmpVertArray, boolean addToPolyList, boolean addToPolyAra){
		myFace tmpFace = new myFace(this.p, this.gui, this.eng, this, faceNum, nVerts, tmpVertArray);
		//gui.print2Cnsl("=----FaceNum " + faceNum + " face : " + tmpFace.toString());
		//add face to each new vertex's adjacent list
		boolean calcAdjacency = this.addFaceToVertAdjList(tmpFace, tmpVertArray);
		if(calcAdjacency){}
		//return val putResult used to verify put operation
		if(addToPolyList){
			myFace putResult = this.polyList.put(faceNum,tmpFace);
			if (putResult != null) { gui.print2Cnsl("face " + faceNum + " already in list");   }
		}
		//add vert idx 0 to 2d ara if requested
		if(addToPolyAra){ 		this.faceListAra[tmpFace.getVertByIDX(0).xIDX][tmpFace.getVertByIDX(0).zIDX] = tmpFace;	}
		return tmpFace;
		//gui.print2Cnsl(tmpFace.toString());
	}//buildFace function

	/**
	*  calculates the subdivided face ids from the old parent face id for square square subdivision - will have more than 4 possible faces resulting - up to 8, pad to 10
	*  to be safe
	*  --replaced with JNI version calcSqSqFaceIDJNI
	*/
	public int calcSqSqFaceID(int oldfaceNum, int offset){ return ((oldfaceNum * 10) + offset); }

	/**
	*  returns the vertex that is in both passed hashmaps of vertexes, null if none
	*/
	public myVertex findIntersectVert(HashMap<Integer,myVertex> A, HashMap<Integer,myVertex> B){
		myVertex result = null;
		for (myVertex vert : A.values()){
			if (B.containsValue(vert)){
				return vert;
			}//check for contains in b
		}//for each vert in a  
		return result;
	}//findIntersection between vert arraylists method

	/**
	*  calculate random additions to height, either purely y or multipled to normal of vertex 
	*/
	public myVector calculateRandomHeight(myVertex[] oldVertAra, myVector base, int[] pIDX){
		myVector result = new myVector(0,0,0);
		result.set(base);
		double tmpx = result.x; 
		double tmpy = result.y;
		double tmpz = result.z;
		float k = 1f;
		double randAmt = 0;
		if (p.allFlagsSet(myConsts.randomHeight)){
			//double fractDim = (Math.random() * .4) + .8;// p.random(.8f,1.2f);//1.0;//randomize
			double fractDim = (p.noise((float)tmpx, (float)tmpy, (float)tmpz) * .4) + .8;// p.random(.8f,1.2f);//1.0;//randomize
			//randAmt = 2 * Math.pow(2, -1 * this.currSqSqIterDepth * fractDim) * ((rnd2*2*2*k) - k);// p.random(-k,k);
			//randAmt = 2 * Math.pow(2, -1 * this.currSqSqIterDepth * fractDim) * ((Math.random() *2*k) - k);// p.random(-k,k);
			randAmt = 2 * Math.pow(2, -1 * this.currSqSqIterDepth * fractDim) * ((Math.random() *2*k) - k);// p.random(-k,k);
		}
		if (p.allFlagsSet(myConsts.multByNormal)){//3d surface, terrain grows "out" not up - along direction of normal
			myVector tmpNormVect = new myVector(0,0,0);
			//get parent verts' normals and weight appropriately
			tmpNormVect = myVector._mult(oldVertAra[pIDX[0]].N,9);
			tmpNormVect._add(myVector._mult(oldVertAra[pIDX[1]].N,3));
			tmpNormVect._add(myVector._mult(oldVertAra[pIDX[2]].N,1));
			tmpNormVect._add(myVector._mult(oldVertAra[pIDX[3]].N,3));
			tmpNormVect._normalize();
			tmpNormVect._mult(.1f);
			tmpNormVect._mult(randAmt);//random amount in direction of normal
			tmpx += tmpNormVect.x;
			tmpy += tmpNormVect.y;
			tmpz += tmpNormVect.z;
		} else {//terrain grows up
			tmpy += (randAmt * this.currSqSqIterDepth); // tweaked random value : random between -2 and 2 times 2^-(iteration depth-1)
		}
		result.set(tmpx,tmpy,tmpz);
		return result;
	}//calculateRandomHeight
	
	/**
	*  returns a new vertex coord based on the array of vertices passed, 
	*  following 9:3:3:1, normalized, standard
	*/
	public myVertex multSqSqVert(myVertex[] oldVertAra, int offset){
		myVector resCoords = new myVector(0,0,0);
		myVector baseCoords = new myVector(0,0,0);
		
		myVertex result;
	  
		int[] pIDX = new int[4];
		pIDX[0] = ((0 + offset) % oldVertAra.length); // multiplied by 9
		pIDX[1] = ((1 + offset) % oldVertAra.length); // multiplied by 3
		pIDX[2] = ((2 + offset) % oldVertAra.length); // multiplied by 1
		pIDX[3] = ((3 + offset) % oldVertAra.length); // multiplied by 3
		//create new vertex coords based on sqsq algorithm
		resCoords   = myVector._mult(oldVertAra[pIDX[0]].coords,9);
		resCoords._add(myVector._mult(oldVertAra[pIDX[1]].coords,3));
		resCoords._add(myVector._mult(oldVertAra[pIDX[2]].coords,1));
		resCoords._add(myVector._mult(oldVertAra[pIDX[3]].coords,3));
		resCoords._div(16);
		resCoords.set(calculateRandomHeight(oldVertAra,resCoords, pIDX));
  		
		result = new myVertex(this.p, this.gui, this.eng, this, this.globalNumVerts, resCoords);

		if(p.allFlagsSet(myConsts.multByNormal)){
			//create basecoord(starting coords w/out terrain generation) by weighting old coords using sqsq algorithm
			//check if multbynormal - otherwise we don't need base coords
			baseCoords   = myVector._mult(oldVertAra[pIDX[0]].baseCoords,9);
			baseCoords._add(myVector._mult(oldVertAra[pIDX[1]].baseCoords,3));
			baseCoords._add(myVector._mult(oldVertAra[pIDX[2]].baseCoords,1));
			baseCoords._add(myVector._mult(oldVertAra[pIDX[3]].baseCoords,3));
			baseCoords._div(16);
	
			result.baseCoords.set(baseCoords);
		}
			//set rain on me based on the 9/16's vert's rainonme val
		result.setRainOnMe(oldVertAra[pIDX[0]].isRainOnMe());
		return result;
//		myVector[] resara = {resCoords, baseCoords};
//		return resara;     
	}//multSqSqVert

	/**
	*  this will subdivide the passed face according to square square subdivision
	*  returning a new face, with the 4 new verticies each weighted on their 
	*  z component 9:3:3:1 (normalized) in height
	*  based on the exising verticies - closest, 2 middle, furthest away.
	*/
	public myFace subdividePassedSquare(myFace face){
		myFace result = null;
		//4 vertices of current face. end result : 
		//.________.
		//| .____. |
		//| |    | |
		//| .____. |
		//. ______ .
		
		//4 new verticies
		myVertex[] newVerts = new myVertex[4];
		int newFaceNum = calcSqSqFaceID(face.getFaceNum(),0);
		//boolean testAddAdjVert = false;
		int minus1IDX;
		//myVector[] resAra;
		myVertex putResult;
		for (int i = 0; i < face.getNumVerts(); ++i){
//			resAra = multSqSqVert(face.getVerts(), i);
//			//result = multSqSqVert(face.verts, i);
//			newVerts[i] = new myVertex(this.p, this.gui, this.eng, this, this.globalNumVerts, resAra[0] );
//			newVerts[i].baseCoords.set(resAra[1]);
//			
			newVerts[i] = multSqSqVert(face.getVerts(), i);
			//add old verts to ancestor list for new vertex
			minus1IDX = ((i + face.getNumVerts()-1) % face.getNumVerts());
			newVerts[i].addAncestorVert(0, newFaceNum,face.getVertByIDX(minus1IDX));                          //uncle - ccw from primary parent
			newVerts[i].addAncestorVert(1, newFaceNum,face.getVertByIDX(((minus1IDX+1) % face.getNumVerts())));    //primary parrent
			newVerts[i].addAncestorVert(2, newFaceNum,face.getVertByIDX(((minus1IDX+2) % face.getNumVerts())));    //aunt - cw from primary parent  
			
			putResult = this.vertList.put(this.globalNumVerts, newVerts[i]);                           //add vert to global vertex list
			if(putResult == null){}
			this.globalNumVerts++;   
		}//for each vertex in new face  
		
		minus1IDX = face.getNumVerts() - 1;                                                              //index of last vert
		result = this.buildFace(newFaceNum, newVerts.length, newVerts, false, false);
		//result = new myFace(this.p, this.gui, this.eng, this,  newFaceNum, 4, newVerts);
		//add result face to face adjacency list of each vertex
		//boolean calcAdjacency = this.addFaceToVertAdjList(result, newVerts);
		return result;
	}//subdivideSqSquare

	/**
	 *  finds a face in the new face array given 2 verts.  returns null if none found
	 */
	public myFace findGlobalFaceFromVerts(HashMap<Integer,myFace> tmpAraPolyList, myFace face, myVertex v1, myVertex v2){
		myFace result = null;
		for (myFace newFace : tmpAraPolyList.values()){
			if ((newFace.containsVertex(v1) != -1) && (newFace.containsVertex(v2) != -1) && (newFace != face)){
				return newFace;
			}//if newFace contains both verts    
		}//for each face in temporary array	  
		return result;	  
	}// findGlobalFaceFromVerts method
		
	//make connecting face to the side of existing face, using vertlist 2d ara and facelist 2d ara
	//builds all faces in mesh for subdivision
	public void makeSideConnectingFacesNew(HashMap<Integer,myFace> tmpPolyList, HashMap<Integer,myFace> tmpAraPolyList){
		myFace newFace, oldFace, oldFaceUp, oldFaceRight, oldFaceCorner;
		myVertex[] newVertAra = new myVertex[4];

		for(int xIDX = 0; xIDX < this.faceListAra.length; xIDX+=2){
			for (int zIDX = 0; zIDX < this.faceListAra[0].length; zIDX+=2){
				oldFace = this.faceListAra[xIDX][zIDX];					//the face we are building off of
				//gui.print2Cnsl("x : " + xIDX + " z : " + zIDX);
				//make face to "right" - skip last face in x dir	
				if(zIDX <= this.faceListAra[0].length-3){
					oldFaceRight = this.faceListAra[xIDX][zIDX+2];
					newVertAra[0] = oldFace.getVertByIDX(3);
					newVertAra[1] = oldFace.getVertByIDX(2);
					newVertAra[2] = oldFaceRight.getVertByIDX(1);
					newVertAra[3] = oldFaceRight.getVertByIDX(0);					
					newFace = this.buildFace( this.faceListAra[xIDX][zIDX].ID + this.faceListAra.length, 4, newVertAra, false, true);
					//this.faceListAra[xIDX][zIDX+1] = newFace;
					tmpAraPolyList.put(newFace.ID, newFace);
				}
				//make face to "top" - skip last face in z dir
				if(xIDX <= this.faceListAra.length-3){
					oldFaceUp = this.faceListAra[xIDX+2][zIDX];
					newVertAra[0] = oldFace.getVertByIDX(1);
					newVertAra[1] = oldFaceUp.getVertByIDX(0);
					newVertAra[2] = oldFaceUp.getVertByIDX(3);
					newVertAra[3] = oldFace.getVertByIDX(2);;				
					newFace = this.buildFace( this.faceListAra[xIDX][zIDX].ID + 1, 4, newVertAra, false, true);
					//this.faceListAra[xIDX+1][zIDX] = newFace;
					tmpAraPolyList.put(newFace.ID, newFace);					
				}
				//make face to "upper right" - skip last face in z and x dir
				if((xIDX <= this.faceListAra.length-3) && (zIDX <= this.faceListAra[0].length-3)){
					oldFaceRight = this.faceListAra[xIDX][zIDX+2];
					oldFaceUp = this.faceListAra[xIDX+2][zIDX];
					oldFaceCorner = this.faceListAra[xIDX+2][zIDX+2];
					newVertAra[0] = oldFace.getVertByIDX(2);
					newVertAra[1] = oldFaceUp.getVertByIDX(3);
					newVertAra[2] = oldFaceCorner.getVertByIDX(0);
					newVertAra[3] = oldFaceRight.getVertByIDX(1);					
					newFace = this.buildFace( this.faceListAra[xIDX][zIDX].ID + this.faceListAra.length + 1, 4, newVertAra, false, true);
					//this.faceListAra[xIDX+1][zIDX+1] = newFace;
					tmpAraPolyList.put(newFace.ID, newFace);
				}
			}//for each z idx
		}//for each x idx
	}//makeSideConnectingFacesNew
	
	//make connecting face to the side of existing face
	public void makeSideConnectingFaces(HashMap<Integer,myFace> tmpPolyList, HashMap<Integer,myFace> tmpAraPolyList){
		if(p.allFlagsSet(myConsts.debugMode)){gui.print2Cnsl("----->calling old version - makeSideConnectingFaces, using hashmap ");}
	    myFace newFace;
	    myVertex[] v; //v0 = null, v1 = null, v2 = null, v3 = null;
	    int faceIncr;
	    for (myFace face : tmpPolyList.values()){
	    	newFace = null;
	    	v = new myVertex[4]; //v0 = null, v1 = null, v2 = null, v3 = null;
	    	faceIncr = 1;//used to calculate new face id's
	    
	    	for (int i = 0; i < face.getNumVerts(); ++i){//make face adjacent to edge of face.verts[i]->face.verts[(i+1)%face.numverts] if doesn't exit
	    		int idxPlus1 = (i+1)%face.getNumVerts();
	    		myFace testFace = findGlobalFaceFromVerts(tmpAraPolyList, face, face.getVertByIDX(i), face.getVertByIDX(idxPlus1));
	    		if (testFace == null){//check to see if adjacent face to this vert and its next higher neighbor exists already
	    			boolean makeFace = false;
	    			v[0] = face.getVertByIDX(i);
	    			//uncle/uncleto is idx 0 parent/parentto is idx 1 aunt/auntto is idx 2
	    			//v1 : want v0.parent.child array and v0.aunt.uncleto array intersection
	    			v[1] = findIntersectVert(v[0].ancestorVerts.get(1).descendantVerts[1], v[0].ancestorVerts.get(2).descendantVerts[0]);
	    			if (v[1] != null){//no need to make these if v1 is null
	    				//v2 : want v0.parent.auntTo array and v0.aunt.child array intersection
	    				v[2] = findIntersectVert(v[0].ancestorVerts.get(1).descendantVerts[2], v[0].ancestorVerts.get(2).descendantVerts[1]);
	    				if (v[2] != null) { makeFace = true;}
	    				v[3] = face.getVertByIDX(idxPlus1);
	    			}
	    			if (makeFace){  
	    				newFace = this.buildFace( calcSqSqFaceID(face.getFaceNum(),faceIncr), v.length, v, false, false);
	    				//newFace = new myFace(this.p, this.gui, this.eng, this, calcSqSqFaceID(face.getFaceNum(),faceIncr), 4, v);
	    				faceIncr++;
	    				tmpAraPolyList.put(newFace.ID,newFace);
	    				//boolean calcAdjacency = this.addFaceToVertAdjList(newFace, newFace.getVerts());
	    			}//if makeface  
	    		}//if adjacent face not existing
	    	}// for each vert in vert list
	    }//for each face in poly list  
	}//makeSideconnectingFaces method

	/**
	*  makes center face of group of 9 faces by checking each vertex of original poly list and seeing if a caty-corner face already exists or not
	*/
	public void makeCenterConnectingFaces(HashMap<Integer,myFace> tmpPolyList, HashMap<Integer,myFace> tmpAraPolyList){
		if(p.allFlagsSet(myConsts.debugMode)){gui.print2Cnsl("----->calling old version - makeCenterConnectingFaces, using hashmap ");}
		for (myFace face : tmpPolyList.values()){
			myFace newFace = null;
			int faceIncr = 6;//used to calculate new face id's
			myVertex[] v = new myVertex[4]; //v0 = null, v1 = null, v2 = null, v3 = null;
			for (int i = 0; i < face.getNumVerts(); ++i){//make face adjacent to edge of face.verts[i]->face.verts[(i+1)%face.numverts] if doesn't exit
				boolean makeFace = false;
				//int idxPlus1 = (i+1)%face.getNumVerts();
				v[0] = face.getVertByIDX(i);
				//v1 has to be v0.parent.child intersected with v0.uncle.auntto
				v[1] = findIntersectVert(v[0].ancestorVerts.get(1).descendantVerts[1],v[0].ancestorVerts.get(0).descendantVerts[2]); 
				if (v[1] != null){//no need to make these if v1 is null
					//v3 : want v0.parent.child array and v0.aunt.uncleto array intersection
					v[3] = findIntersectVert(v[0].ancestorVerts.get(1).descendantVerts[1], v[0].ancestorVerts.get(2).descendantVerts[0]);
					if (v[3] != null){
						//v2 : want v3.parent.child array and v3.aunt.uncleto array intersection
						v[2] = findIntersectVert(v[3].ancestorVerts.get(1).descendantVerts[1], v[3].ancestorVerts.get(2).descendantVerts[0]);
						if (v[2] != null) { //if none of verts are null, see if face already exists
							myFace testFace = findGlobalFaceFromVerts(tmpAraPolyList, face, v[0], v[2]);
							makeFace = ((testFace == null)? ( true) : (false));
						}//if v2 != null
					}//v3 != null
				}//v1 !=  null    
				if (makeFace){
					myVertex[] tmpVertAra = {v[0], v[1], v[2], v[3]};//preserve order to make sure correct ancestor/descendent relationships are preserved
					newFace = this.buildFace(calcSqSqFaceID(face.getFaceNum(), faceIncr), tmpVertAra.length, tmpVertAra, false, false);
					//newFace = new myFace(this.p, this.gui, this.eng, this, calcSqSqFaceID(face.getFaceNum(), faceIncr), 4, tmpVertAra);
					faceIncr++;
					tmpAraPolyList.put(newFace.ID,newFace);
					//boolean calcAdjacency = this.addFaceToVertAdjList(newFace, newFace.getVerts());
				}//if make face
			}// for each vert in vert list
		}//for each face in poly list 
	}//makeCenterConnectingFaces method
	
	/**
	*    choose which subdivision routine to use based on whether mesh is enclosed surface or flat terrain
	*/
	public void subdivide_sqSqMesh(){
		if(p.allFlagsSet(myConsts.multByNormal)){ 	this.subdivide_sqSqMeshHashMap();} 
		else {										this.subdivide_sqSqMeshAra();} 		
	}
	
	/**
	 * this will set all coords so that deltanodedist is 1, and then 
	 * build the 2d ara from the vertlist hashmap, after setting each vertex's idx 
	 * in ara as the value of its coords scaled from -minVal to +maxval to 0 to numVertsInDim
	 */
	public void buildVertAraFromVertList(int vertDim){
		//reset extreme values for accuracy as we scale the vert coordinates to have a neighbor distance of 1
		p.resetGMinMaxAraGlobCoords();
		double oldVal = .25f;
		for(myVertex vert : this.vertList.values()){					vert.normalizeCoordDist(); vert.vertDim = vertDim;	}
		this.deltaNodeDist = 1;
		for (myVertex vert : this.vertList.values()){
			vert.xIDX = (int)Math.round(vert.coords.x ) - (int)Math.round(p.globMinAra[myConsts.COORD_X]); //will give idx value of 0 for lowest coord, length - 1 idx for highestcoord
			vert.coords.y -= oldVal; 
			vert.zIDX = (int)Math.round(vert.coords.z ) - (int)Math.round(p.globMinAra[myConsts.COORD_Z]);
			this.vertListAra[vert.xIDX][vert.zIDX] = vert;
		}//for each vertex		
		p.resetGMinMaxAra(myConsts.COORD_Y);
		for (myVertex vert : this.vertList.values()){
			p.globMinAra[myConsts.COORD_Y] = Math.min(vert.coords.y, p.globMinAra[myConsts.COORD_Y]);
			p.globMaxAra[myConsts.COORD_Y] = Math.max(vert.coords.y, p.globMaxAra[myConsts.COORD_Y]);
		}//for each vertex		
	}//buildVertAraFromVertList
	
	/**
	*    subdivides using gavin miller's 9-3-3-1 algorithm - uses 2d ara
	*/
	public void subdivide_sqSqMeshAra(){
		eng.heightMap = null;//make null to rebuild heightmap if selected 
		eng.heightMapAra = null;
		this.deltaNodeDist /= 2.0; 			//distance between nodes is halved in this process
		p.setFlags(myConsts.heightMapMade,(eng.heightMap != null));
		//clear out global vert list - old verts don't exist anymore
		this.vertList.clear();
		this.globalNumVerts = this.vertList.size();
		//reset min/max coords for x,y,z
		p.resetGMinMaxAraGlobCoords();
		HashMap<Integer,myFace> tmpPolyList = new HashMap<Integer,myFace>();
		HashMap<Integer,myFace> tmpAraPolyList = new HashMap<Integer,myFace>();
		// HashMap<Integer,myFace> tmpAraPolyList2 = new HashMap<Integer,myFace>();
		//int numPolys = this.polyList.size();
		myFace newFace;
		
		//1st make primary "center" faces from each old face
		for (myFace face : this.polyList.values()){					//TODO replace with 2d ara once properly initialized in file read-in
			newFace = subdividePassedSquare(face);
			tmpPolyList.put(newFace.ID,newFace);
		}//for each old face 
	  
		double vertDimCount = Math.sqrt(this.vertList.size());
		if((int)vertDimCount != vertDimCount){	gui.print2Cnsl("illegal mesh geometry - needs to be square. vert count : " + this.vertList.size() + " verts per side : " + vertDimCount );}
		else {									gui.print2Cnsl(this.globalNumVerts + " , " + this.vertList.size() + " in a square with " + vertDimCount + " vertices per side");	}
		//all verts that will be made are now built.  we need to shift every vertex's coords so that they are 1 unit away from their neighbors and populate verts in 2d vert ara based on coords in x and z shifted to start at 0
		this.vertListAra = new myVertex[(int)vertDimCount][(int)vertDimCount];
		this.faceListAra = new myFace[(int)(vertDimCount)-1][(int)(vertDimCount)-1];
		
		//set all coords so that delta node dist is 1
		this.buildVertAraFromVertList((int)vertDimCount);

		//set up face array holding list of faces with idxed by idx-0 vert's coords (lower left) - use newly made faces
		for (myFace face : tmpPolyList.values()){
			myVertex vert = face.getVertByIDX(0);
			this.faceListAra[vert.xIDX][vert.zIDX] = face;	
		}
		
		makeSideConnectingFacesNew(tmpPolyList, tmpAraPolyList);
		gui.print2Cnsl("Make new faces finished");
		
		p.cam.camMoveBackVal = 1.8;			//TODO : cam distance modifier for subdivision

		for (myFace face : tmpAraPolyList.values()){
			myVertex vert = face.getVertByIDX(0);
			this.faceListAra[vert.xIDX][vert.zIDX] = face;	
		}
		
		tmpPolyList.putAll(tmpAraPolyList);//(newFace.faceNum, newFace);

				
		gui.print2Cnsl("finished subdividing");
		
		//by here tmpPolyList holds all the new faces
		this.polyList = tmpPolyList;
		this.globalNumFaces = this.polyList.size();
		//increment iteration level for random variable height modification calculation
		//  gui.print2Cnsl("iter depth : " + currSqSqIterDepth++);
		this.globalNumVerts = this.vertList.size();

		
		//this.setupVertFaceArraysForJNI();
		if(p.allFlagsSet(myConsts.debugModeCnsl)) { printOutVertFaceAras();}
		this.currSqSqIterDepth++;

	}//subdivide_sqSqMeshAra method
	
	/**
	*    subdivides using gavin miller's 9-3-3-1 algorithm - uses hashmap (old) only use now for enclosed meshes
	*/
	public void subdivide_sqSqMeshHashMap(){
		eng.heightMap = null;//make null to rebuild heightmap if selected 
		p.setFlags(myConsts.heightMapMade,(eng.heightMap != null));
		//clear out global vert list - old verts don't exist anymore
		this.vertList.clear();
		this.globalNumVerts = this.vertList.size();
		//reset min/max coords for x,y,z
		p.resetGMinMaxAraGlobCoords();
		HashMap<Integer,myFace> tmpPolyList = new HashMap<Integer,myFace>();
		HashMap<Integer,myFace> tmpAraPolyList = new HashMap<Integer,myFace>();
		// HashMap<Integer,myFace> tmpAraPolyList2 = new HashMap<Integer,myFace>();
		//int numPolys = this.polyList.size();
		myFace newFace;
		
		//1st make primary "center" faces from each old face
		for (myFace face : this.polyList.values()){				
			newFace = subdividePassedSquare(face);
			tmpPolyList.put(newFace.ID,newFace);
		}//for each old face 
	  	gui.print2Cnsl("primary faces made");
		//2nd, for each new face, make its neighbors to the right, south (idx's 1 and 2) and southeast corner
		//have to add to new structure or will get iterator error
		makeSideConnectingFaces(tmpPolyList, tmpAraPolyList); 
		gui.print2Cnsl("side connections made");		
		//lastly have to make "center" face of every group of 9.
		makeCenterConnectingFaces(tmpPolyList, tmpAraPolyList);
		gui.print2Cnsl("center connections made");
		tmpPolyList.putAll(tmpAraPolyList);//(newFace.faceNum, newFace);
		for (myVertex vert : this.vertList.values()){
			if (vert.adjVerts.size() > 4){
				gui.print2Cnsl("--illegal vert list-");
				gui.print2Cnsl("vert : " + vert.toStringWithFaces());
				gui.print2Cnsl("--illegal vert list-");
			}  
		}
		gui.print2Cnsl("finished subdividing");
		//by here tmpPolyList holds all the new faces
		this.polyList = tmpPolyList;
		this.globalNumFaces = this.polyList.size();
		//increment iteration level for random variable height modification calculation
		//  gui.print2Cnsl("iter depth : " + currSqSqIterDepth++);
		this.globalNumVerts = this.vertList.size();
	  
		//this.setupVertFaceArraysForJNI();
		if(p.allFlagsSet(myConsts.debugModeCnsl)) { printOutVertFaceAras();}
		this.currSqSqIterDepth++;
	}//subdivide_sqSqMeshHashMap method
	
	/**
	 * sets up the arrays used by the jni library to modify the mesh
	 */
	public void setupVertFaceArraysForJNI(){
		
		//rebuild face and vert arrays for jni function
		this.faceIDsInSeq = new int[this.polyList.size()];
		//array of faces with the 4 verts that make up each face
		this.faceIDOwnsVertIDs = new int[this.polyList.size()][4];
	  
		//array of sequence of vert ids
		this.vertIDsInSeq = new int[this.vertList.size()];
		//array of vert ids with the face ids adjacent to them
//		  vertIDOwnsFaceIDs = new int[this.vertList.size()][4];
		//array of vert id's with their coords
		this.vertIDWithCoords = new double[this.vertList.size()][3];
		int incr = 0;
		for(Integer idx : this.vertList.keySet()){
			this.vertIDsInSeq[incr] = vertList.get(idx).vertNum;
			//get coords of all verts, put in coord array
			this.vertIDWithCoords[incr][0] = vertList.get(idx).coords.x;
			this.vertIDWithCoords[incr][1] = vertList.get(idx).coords.y;
			this.vertIDWithCoords[incr][2] = vertList.get(idx).coords.z;
			incr++;
		}
		incr = 0;
		for(Integer idx : this.polyList.keySet()){
			this.faceIDsInSeq[incr] = this.polyList.get(idx).getFaceNum();
			//get ids of all verts making up this face
			this.faceIDOwnsVertIDs[incr][0] = this.polyList.get(idx).getVertByIDX(0).vertNum;
			this.faceIDOwnsVertIDs[incr][1] = this.polyList.get(idx).getVertByIDX(1).vertNum;
			this.faceIDOwnsVertIDs[incr][2] = this.polyList.get(idx).getVertByIDX(2).vertNum;
			this.faceIDOwnsVertIDs[incr][3] = this.polyList.get(idx).getVertByIDX(3).vertNum;
			incr++;
		}
	}//setupVertFaceArraysForJNI()
	
	/**
	*    subdivides using catmull clarke-inspired algorithm
	*/
	public void subdivide_catClkMesh(){
		//perform on y only for flat mesh
		//add midpoint to every face equal to avg of all original face points,
		//add midpoint to ever edge equal to avg of edge's endpoints and edge's neighboring face points
		//add edge to every midpoint to every neighboring edgepoint
		//For each original point P, take the average F of all n (recently created) face points for 
		//faces touching P, and take the average R of all n edge midpoints for edges touching P, 
		//where each edge midpoint is the average of its two endpoint vertices. Move each original point to the point : 
		// (F + 2R + (n-3)P)/n  <--- This is the barycenter of P, R and F with respective weights (n-3), 2 and 1
		//Connect each new Vertex point to the new edge points of all original edges incident on the original vertex.
		//Define new faces as enclosed by edges

	}//subdivide_sqSqMesh method	
	
	
	/**
	 * this will not increase the number of nodes, but will move the nodes in the y direction to be halfway toward the midpoint of their neighbors, to smooth out the mesh
	 */
	public void smoothMesh(){
		if(this.vertListAra != null){
			double calcAvg;
			//tmp holding array to hold the newly calculated coords for all the vertices in the mesh
			double[][] newVertListYvals = new double[this.vertListAra.length][this.vertListAra[0].length];
			//copy all vert coords over to tmp list
			for(int xIDX = 0; xIDX < this.vertListAra.length; ++xIDX){
				for(int zIDX = 0; zIDX < this.vertListAra[0].length; ++zIDX){//copy border x coords
					newVertListYvals[xIDX][zIDX] = this.vertListAra[xIDX][zIDX].coords.y;
					newVertListYvals[xIDX][zIDX] += (.75 * ( this.vertListAra[xIDX][zIDX].coords.y - newVertListYvals[xIDX][zIDX])) ; 
				}//for each z
			}//for each x
			for(int xIDX = 1; xIDX < this.vertListAra.length-1; ++xIDX){
				for(int zIDX = 1; zIDX < this.vertListAra[0].length-1; ++zIDX){//copy border x coords
					calcAvg = (this.vertListAra[xIDX+1][zIDX].coords.y + this.vertListAra[xIDX-1][zIDX].coords.y + this.vertListAra[xIDX][zIDX+1].coords.y + this.vertListAra[xIDX][zIDX-1].coords.y) * .25; 
					newVertListYvals[xIDX][zIDX] += (.4 * (calcAvg - newVertListYvals[xIDX][zIDX]));
					//newVertListYvals[xIDX][zIDX] += 1;
				}//for each z		
			}//for each x
			
			for(int xIDX = 1; xIDX < this.vertListAra.length-1; ++xIDX){
				for(int zIDX = 1; zIDX < this.vertListAra[0].length-1; ++zIDX){//copy border x coords
					this.vertListAra[xIDX][zIDX].coords.y = newVertListYvals[xIDX][zIDX]; 
					}//for each z		
			}//for each x
			
			if(p.allFlagsSet(myConsts.wrapAroundMesh)){ 	gui.print2Cnsl("wrapAroundRecalc");		this.setVertWrapAround(this.vertListAra.length);	}		//if this is a wrap around mesh, recalculate relative heights and such for wrap.
			if(p.allFlagsSet(myConsts.heightMapMade)){		eng.handleNodeRecalc();		}//if the height map was made, reset all the height nodes' values received from the src vertexes
		}//if non-null vert list
	}//smoothMesh

	public double getDeltaNodeDist() {	return deltaNodeDist;}
	public void setDeltaNodeDist(double deltaNodeDist) {	this.deltaNodeDist = deltaNodeDist;}
}//mySqSqSubdivider
