package sqSqErosionSimPkg;
import java.util.*;


/**
*  object to hold height information for height map
*  also class to hold tuple of x,z values for key to hashmap holding heights
*/

public class myHeightNode{
	private sqSqErosionSimGlobal p;
	private myGui gui;
	private mySqSqEroder eng;
//	private mySqSqSubdivider sub;
	private boolean isNodeJustMade;									//true only when node is first created - once passed through 1 cycle of fluid transport, set to false
	public final int ID;              								//id of this height node - should be unique id, independent of other object id numbering - equal to source vert ID
	public myVertex source;     									//vertex that serves as source of this heightnode
	
	public double lclKc, lclKd, lclKw, lclKs;						//local values of erosion constants - populated in initNode
	
	public myVector originalCoords;	
	private double originalHeight;									//height of this node when it is first made
	private boolean useOriginalHeight;								//if true then only erode this node from height added via deposition, not below original height - so deposit normally
	
	private double sedConcVis;										//visualisation value for sediment concentration, if sed concentration is enabled
	
	private double sedCapPipes;										//the current sediment capacity of this node
	private double[] latSedCapPipes;								//used to hold capacity of sediment available due to mass wasting for each potential direction from this node to a wet node
	private boolean[] useLocalErosionConstants;        				//override global mat type with this node's mat type constants
	private double[] erosionVals;									//this node's erosion constants if used - idx 0:Kc, 1:Kd, 2:Ke, 3:Ks
	private boolean boundaryNode;									//whether or not this node is a boundary node between nodes with water and nodes without water
																	//true if this node has water and it has at least one neighbor who does not have any water
	public boolean boundaryDryNode;									//this node is a dry node that has an adjacent wet node
		//mirror node idxs : 0 = x, 1 = z
	private boolean[] isMirrorNode;									//whether or not another node feeds its water and sediment into this node - need to modify for diags
		
	private double[] avgNeighborHeightDiff;							//average height difference between this node and its actual adjacent neighbor and this node's mirror node and it's adjacent node
	
	private HashMap<Integer, Integer> boundaryAdjNodeDirByID;  		//direction of each adj node that is a boundary node : key node id, val 0- 3
	private int[] boundaryAdjNodeIDByDir;                         	//the ID of the n,e,s,and w neighbors of this node - idx = dir, value = adjNode ID
	private myHeightNode[] boundaryAdjNodeByDir;					//ara holding boundary node references based on direction
	
	private ArrayList<Integer> oldDonors;         					//list of adj node id's that gave water to this node immediately previous to a cycle (non-clone nodes)	  

	  //arrays are intended to hold intermediate steps in calculations during deltat time step - primary values are in idx 0
	private double[][] distToNeighbors;								//lateral distance to this node's neighbors.  takes place of sub.deltaNodeDist : idx 0 for each step in sim, idx 1 for each lateral direction
	private double[] heightWater;          							//the height of the column of water currently at this node
	private double[] sediment;         							 	//the amount of sediment held in the volume of water currently at this node

	private double matFEMval;										//value from matlab fem calculation
	
	private myVector sedSource;										//the sediment source for this node - used in pipes calculation to move sediment
	private double sedLeftOver;										//sediment not transported in semi-lagrangian transport step
	private double setTransportedDebug;								//the sediment that has been transported out of this node - used for debug purposes
	private int sedTransportSrcIDX = 1;								//index that sediment transport pulls from - maintained so that we can monitor sediment transport
	
	//quantities to implement pipes based transport model
	private double[][] flux;            							//outflow flux from this node to neighbors - idx's are first step then direction
	private double[] adjWaterShare;     							//ratio of total current water allocated via outflow to lower neighbors, idxed by neighbor number
	private myVector velocity;        								//velocity in 2D along surface of terrain - negative gradient - of either water(idx0) or sediment(idx1) 
	private HashMap<Integer,myHeightNode> adjNodes;
	private HashMap<Integer, Integer> adjNodeDirByID;  			//direction of each adj node : key node id, val 0- 3
	private int[] adjNodeIDByDir;                         			//the idx of the n,e,s,and w neighbors of this node - idx = dir, value = adjNode ID
	
	private HashMap<Integer,Double> pipeLengths;  					//lengths of virtual pipes to each neighbor, idxed by neighbor's id
	private HashMap<Integer,Double> adjHeightDiffs; 				//heightDifferences to each neighbor, idxed by neighbor's id - terrain and h2o
	private HashMap<Integer,Double> adjHeightDiffsTer; 				//terrainHeightDifferences to each neighbor, idxed by neighbor's id
	
	public final static int numDir = 4;//change to 8 when diagonal pipes implemented
	public final static int N = 0, E = 1, S = 2, W = 3;//, NW = 4, NE = 5, SE = 6, SW = 7;					//for use in flux array (nswe) 
	private int[] scaledVal;
	
	//normal for this object
	public myVector H2O_Norm, Sed_Norm, SedCap_Norm, SedConc_Norm;					//display normals for each of these 3 quantites, calculated by the same quantities amts of each of 4 adj verts avged.

	private boolean kScaledNode;										//this node has been scaled by k the most recent flux calculation - debug only
	private double kVal;												//the value of k (velocity scaling) at this node
	
		//erosion constants determination values
	private double[] neighborSlope;										//abs val of terrain height diff with neighbors - used for determining efficacy of erosion consts, idxed by dir
	
	private myVector fluxForce;											//flux outflow from this node, in x and z, to use as a force for sailboat visualization
	
	private double[] windForce;
		  
	public myHeightNode(sqSqErosionSimGlobal _p, myGui _g, mySqSqEroder _e, mySqSqSubdivider _s, myVertex _source, double _curHeight){
		this.p = _p;this.gui = _g;this.eng = _e;//this.sub = _s;
		this.source = _source;
		this.ID = this.source.ID;      							//make height node's ID a function of source vertex's ID
		this.source.setOwningNode(this);					//set the vertex for this node's owning node to this node
		this.heightWater = new double[5];					//use higher idxs to calculate higher order accurate models
	    this.sedCapPipes = 0;
		this.sediment = new double[5];
		this.flux = new double[5][numDir];            				//flux for each cardinal direction, for t and deltaT, first step then direction
		this.fluxForce = new myVector();
		this.distToNeighbors = new double[5][numDir];      		 	//distance to each neighbor, potentially modified if lateral erosion occurs
		this.velocity = new myVector();					//2 velocity vectors maintained currently, water (idx 0) and sediment (idx 1)
		this.useOriginalHeight = this.source.getUseOriginalHeight();
		this.originalHeight = _curHeight;					//original height will be saved for use with custom erosion values
		this.originalCoords = this.source.coords.cloneMe();
		this.useLocalErosionConstants = new boolean[4];
		this.erosionVals = new double[4];
		for (int i = 0; i< 4; ++i){
			this.useLocalErosionConstants[i] = this.source.isUseNodeErosionConstants(i);
			if(this.useLocalErosionConstants[i]){this.erosionVals[i] = this.source.getErosionVals(i);}
		}
		
			//for mirroring across x and z : idx 0 = x, idx 1 = z
		
		this.isMirrorNode = new boolean[2];		
		this.avgNeighborHeightDiff = new double[2];												//only used by mirror nodes for wrap around height differences
		for(int idx = 0; idx < 2; ++idx){
			this.isMirrorNode[idx] = this.source.getIsMirrorVert(idx);								//whether or not this node receives water and sed from another node (boundary-crossing for rivers)
			this.avgNeighborHeightDiff[idx] = this.source.getHeightDiffWrapAround(idx);
		}//for each possible wrap-around dir, x and z
		
		this.sedSource = new myVector(this.source.coords.x, this.source.coords.y, this.source.coords.z);		//source starts at this node's coords
		this.sedLeftOver = 0;
		this.scaledVal = new int[myConsts.numGlobValVars];
		this.adjNodes = new HashMap<Integer,myHeightNode>();
		this.adjNodeDirByID = new HashMap<Integer,Integer>();
		this.adjNodeIDByDir = new int[numDir];		
		
		this.boundaryAdjNodeDirByID = new HashMap<Integer,Integer>();
		this.boundaryAdjNodeIDByDir = new int[numDir];
		this.boundaryAdjNodeByDir = new myHeightNode[numDir];						//idx'ed by dir, holds refs to boundary nodes, or null
		
		this.adjWaterShare = new double[numDir];
		this.neighborSlope = new double[numDir];
		this.latSedCapPipes = new double [numDir];
		this.windForce = new double[numDir];
		for(int i = 0; i < numDir; ++i){		
			this.adjNodeIDByDir[i] = -1;	
			this.boundaryAdjNodeByDir[i] = null;
		}
		this.pipeLengths = new HashMap<Integer,Double> ();
		this.adjHeightDiffs = new HashMap<Integer,Double> ();
		this.adjHeightDiffsTer = new HashMap<Integer,Double> ();
		//this.adjTalusAnglesArcSine = new HashMap<Integer,Double> ();
	    for (int i = 0; i < myConsts.numGlobValVars; ++i){   this.scaledVal[i] = -1;   }
	    this.oldDonors = new ArrayList<Integer>();
	    this.initValsPipes();
	    this.setGlobVals(0);
		this.boundaryNode = false;									//whether or not this node is a boundary node between nodes with water and nodes without water
																	//true if this node has water and it has at least one neighbor who does not have any water
		this.boundaryDryNode = false;
		
		this.isNodeJustMade = true;									//set to false once 1 pass of calc node geometry has executed
		this.kScaledNode = false;
		this.kVal = 1;
		this.H2O_Norm = new myVector();
		this.Sed_Norm = new myVector(); 
		this.SedCap_Norm = new myVector();
		this.SedConc_Norm = new myVector();
		this.sedConcVis = 0;

	}//constructor 2 arg

	//initialize node's erosion vals for each step of erosion/fluid simulation	
	public void initCycleVals(){
		//erosion constants - may implement changes during erosion process
		this.lclKc = (this.useLocalErosionConstants[eng.Kc] ? (this.erosionVals[eng.Kc]) : (eng.matTypeAra[eng.Kc][eng.globMatType]));		
		this.lclKd = (this.useLocalErosionConstants[eng.Kd] ? (this.erosionVals[eng.Kd]) : (eng.matTypeAra[eng.Kd][eng.globMatType]));		
		this.lclKw = (this.useLocalErosionConstants[eng.Kw] ? (this.erosionVals[eng.Kw]) : (eng.matTypeAra[eng.Kw][eng.globMatType]));		
		this.lclKs = (this.useLocalErosionConstants[eng.Ks] ? (this.erosionVals[eng.Ks]) : (eng.matTypeAra[eng.Ks][eng.globMatType]));
//	  	this.lclKs = (this.useLocalErosionConstants ? (this.erosionVals[eng.Ks]) : (eng.matTypeAra[eng.Ks][eng.globMatType]))/ 
//	  			(((p.allFlagsSet(myConsts.modSoilSoftness)) && (this.originalHeight - this.source.coords.y > 0 )) ? (1 + Math.log1p( this.originalHeight - this.source.coords.y)) : (1));//scale soil softness to get harder the deeper we go
	  			//enable scaling by depth from original height
	}//initNode
	
	/**
	 * this will reset the values in this node received from the source vertex this node is built from - this is done to make sure that height values that may change, from 
	 * actions such as smoothing of terrain vertices, will be propagated properly into the height nodes
	 */
	public void resetSrcVertVals(myVertex _source, double _curHeight){
		this.source = _source;
		this.useOriginalHeight = this.source.getUseOriginalHeight();
		for (int i = 0; i< 4; ++i){
			this.useLocalErosionConstants[i] = this.source.isUseNodeErosionConstants(i);
			if(this.useLocalErosionConstants[i]){this.erosionVals[i] = this.source.getErosionVals(i);}
		}
		for(int idx = 0; idx < 2; ++idx){
			this.isMirrorNode[idx] = this.source.getIsMirrorVert(idx);								//whether or not this node receives water and sed from another node (boundary-crossing for rivers)
			this.avgNeighborHeightDiff[idx] = this.source.getHeightDiffWrapAround(idx);
		}//for each possible wrap-around dir, x and z	
		this.sedSource = new myVector(this.source.coords.x, this.source.coords.y, this.source.coords.z);		//source starts at this node's coords
		this.originalHeight = _curHeight;					//original height will be saved for use with custom erosion values
//		this.curHeight[0] = this.originalHeight;  					//passed seperately in case of non-planar object height
		//this.calc
	}//resetSrcVertVals

	/**
	 * calculates various geometry-related values for this height node required for fluid and erosion calculations
	 * 		-builds a structure containinig all neighbors, if it doesn't exist already
	 * 		-calculates pipe lengths for pipes-based fluid model
	 * 		-calculates and saves the height difference between this node and its neighbors
	 * @param h2oIdx - index to use to determine height difference 
	 * @param isClone
	 */
//	public void calcNodeGeometryOld(boolean isClone, int h2oIdx){
//		//xIDX == 0  -> no west neighbor; xIDX == aralen-1 -> no east neighbor
//		//zIDX == 0	 -> no south neighbor; zIDX == aralen-1 -> no north neighbor
//		//for diag neighbors, need to take into account both idxs - if x,z idx ==0 no sw neighbor, etc
//		//i.e., looking down on mesh, axes are in southwest corner
//		myHeightNode neighborNodeW,neighborNodeE,neighborNodeN,neighborNodeS;
//		//myHeightNode neighborNode[] = new myHeightNode[numDir];
//		
//		this.initCycleVals();				//reinitialize all erosion-cycle values that may change (like erosion constants, debug values, etc)
//		
//		if(this.source.xIDX == 0){
//			if(this.isMirrorNode[0]){		//if wrap around, and at xidx 0, make adj node equal to node with same z on opposite side of mesh, along with 2 neighbors of wrap node
//				if (isClone){  neighborNodeW = eng.oldCloneHeightNodeAra[eng.oldCloneHeightNodeAra.length-1][this.source.zIDX]; } //get adj node from height map structure
//				else        {  neighborNodeW = eng.heightMapAra[eng.heightMapAra.length-1][this.source.zIDX]; }		
//				//neighborNodeWID = neighborNodeW.ID;				
//			}
//			else {		neighborNodeW = null;		/*neighborNodeWID = -1;*/}
//		} else {
//			if (isClone){  neighborNodeW = eng.oldCloneHeightNodeAra[this.source.xIDX-1][this.source.zIDX]; } //get adj node from height map structure
//			else        {  neighborNodeW = eng.heightMapAra[this.source.xIDX-1][this.source.zIDX]; }
//			//neighborNodeWID = neighborNodeW.ID;
//		}
//		
//		if(this.source.xIDX == eng.heightMapAra.length-1){
//			if(this.isMirrorNode[0]){			//if wrap around, and at xidx at aralen-1, make adj node equal to node with same z on opposite side of mesh, along with 2 neighbors of wrap node
//				if (isClone){  neighborNodeE = eng.oldCloneHeightNodeAra[0][this.source.zIDX]; } //get adj node from height map structure
//				else        {  neighborNodeE = eng.heightMapAra[0][this.source.zIDX]; }
//				//neighborNodeEID = neighborNodeE.ID;
//			} 
//			else {		neighborNodeE = null;		/*neighborNodeEID = -1;*/}
//		} else {		
//			if (isClone){  neighborNodeE = eng.oldCloneHeightNodeAra[this.source.xIDX+1][this.source.zIDX]; } //get adj node from height map structure
//			else        {  neighborNodeE = eng.heightMapAra[this.source.xIDX+1][this.source.zIDX]; }
//			//neighborNodeEID = neighborNodeE.ID;
//		}
//	
//		if(this.source.zIDX == 0){
//			if(this.isMirrorNode[1]){		//if wrap around, and at xidx 0, make adj node equal to node with same z on opposite side of mesh
//				if (isClone){  neighborNodeS = eng.oldCloneHeightNodeAra[this.source.xIDX][eng.oldCloneHeightNodeAra.length-1]; } //get adj node from height map structure
//				else        {  neighborNodeS = eng.heightMapAra[this.source.xIDX][eng.heightMapAra.length-1]; }		
//		//		neighborNodeSID = neighborNodeS.ID;				
//			}
//			else {			neighborNodeS = null; 		/*neighborNodeSID = -1;*/}
//		} else {
//			if (isClone){  neighborNodeS = eng.oldCloneHeightNodeAra[this.source.xIDX][this.source.zIDX-1]; } //get adj node from height map structure
//			else        {  neighborNodeS = eng.heightMapAra[this.source.xIDX][this.source.zIDX-1]; }
//		//	neighborNodeSID = neighborNodeS.ID;
//		}
//		
//		if(this.source.zIDX == eng.heightMapAra[0].length-1){
//			if(this.isMirrorNode[1]){		//if wrap around, and at xidx 0, make adj node equal to node with same z on opposite side of mesh
//				if (isClone){  neighborNodeN = eng.oldCloneHeightNodeAra[this.source.xIDX][0]; } //get adj node from height map structure
//				else        {  neighborNodeN = eng.heightMapAra[this.source.xIDX][0]; }		
//			//	neighborNodeNID = neighborNodeN.ID;				
//			}
//			else {			neighborNodeN = null;		/*neighborNodeNID = -1;*/}
//		} else {
//			if (isClone){  neighborNodeN = eng.oldCloneHeightNodeAra[this.source.xIDX][this.source.zIDX+1]; } //get adj node from height map structure
//			else        {  neighborNodeN = eng.heightMapAra[this.source.xIDX][this.source.zIDX+1]; }
//		//	neighborNodeNID = neighborNodeN.ID;
//		}
//				
//		//west
//		if(neighborNodeW != null){	calcNodeGeometry_PipeLength(neighborNodeW,W,h2oIdx);}
//		else {		if(this.isNodeJustMade){	setAdjNodeDirIDVals(W, -1);}}		
//		//east
//		if(neighborNodeE != null){	calcNodeGeometry_PipeLength(neighborNodeE,E,h2oIdx);}
//		else {		if(this.isNodeJustMade){	setAdjNodeDirIDVals(E, -1);}}		
//		//north
//		if(neighborNodeN != null){	calcNodeGeometry_PipeLength(neighborNodeN,N,h2oIdx);}
//		else {		if(this.isNodeJustMade){	setAdjNodeDirIDVals(N, -1);}}		
//		//south
//		if(neighborNodeS != null){	calcNodeGeometry_PipeLength(neighborNodeS,S,h2oIdx);}
//		else {		if(this.isNodeJustMade){	setAdjNodeDirIDVals(S, -1);}}		
//
//	}//calcNodeGeometry	
	
	/**
	 * calculates various geometry-related values for this height node required for fluid and erosion calculations
	 * 		-builds a structure containinig all neighbors, if it doesn't exist already
	 * 		-calculates pipe lengths for pipes-based fluid model
	 * 		-calculates and saves the height difference between this node and its neighbors
	 * @param h2oIdx - index to use to determine height difference 
	 * @param notUsed - used to specify whether clone or not
	 */
	public void calcNodeGeometry(boolean notUsed, int h2oIdx){
		//xIDX == 0  -> no west neighbor; xIDX == aralen-1 -> no east neighbor
		//zIDX == 0	 -> no south neighbor; zIDX == aralen-1 -> no north neighbor
		//for diag neighbors, need to take into account both idxs - if x,z idx ==0 no sw neighbor, etc
		//i.e., looking down on mesh, axes are in southwest corner
		myHeightNode neighborNodeW,neighborNodeE,neighborNodeN,neighborNodeS;
		
		this.initCycleVals();				//reinitialize all erosion-cycle values that may change (like erosion constants, debug values, etc)
		
		if(this.source.xIDX == 0){
			if(this.isMirrorNode[0]){		//if wrap around, and at xidx 0, make adj node equal to node with same z on opposite side of mesh, along with 2 neighbors of wrap node
				neighborNodeW = eng.heightMapAra[eng.heightMapAra.length-1][this.source.zIDX]; 		
			}
			else {		neighborNodeW = null;		/*neighborNodeWID = -1;*/}
		} else {
			neighborNodeW = eng.heightMapAra[this.source.xIDX-1][this.source.zIDX]; 
		}
		
		if(this.source.xIDX == eng.heightMapAra.length-1){
			if(this.isMirrorNode[0]){			//if wrap around, and at xidx at aralen-1, make adj node equal to node with same z on opposite side of mesh, along with 2 neighbors of wrap node
				neighborNodeE = eng.heightMapAra[0][this.source.zIDX]; 
			} 
			else {		neighborNodeE = null;		/*neighborNodeEID = -1;*/}
		} else {		
			neighborNodeE = eng.heightMapAra[this.source.xIDX+1][this.source.zIDX]; 
		}
	
		if(this.source.zIDX == 0){
			if(this.isMirrorNode[1]){		//if wrap around, and at xidx 0, make adj node equal to node with same z on opposite side of mesh
				neighborNodeS = eng.heightMapAra[this.source.xIDX][eng.heightMapAra.length-1]; 		
			}
			else {			neighborNodeS = null; 		/*neighborNodeSID = -1;*/}
		} else {
			neighborNodeS = eng.heightMapAra[this.source.xIDX][this.source.zIDX-1]; 
		}
		
		if(this.source.zIDX == eng.heightMapAra[0].length-1){
			if(this.isMirrorNode[1]){		//if wrap around, and at xidx 0, make adj node equal to node with same z on opposite side of mesh
				neighborNodeN = eng.heightMapAra[this.source.xIDX][0]; 		
			}
			else {			neighborNodeN = null;		/*neighborNodeNID = -1;*/}
		} else {
			neighborNodeN = eng.heightMapAra[this.source.xIDX][this.source.zIDX+1]; 
		}
				
		//west
		if(neighborNodeW != null){	calcNodeGeometry_PipeLength(neighborNodeW,W,h2oIdx);}
		else {		if(this.isNodeJustMade){	setAdjNodeDirIDVals(W, -1);}}		
		//east
		if(neighborNodeE != null){	calcNodeGeometry_PipeLength(neighborNodeE,E,h2oIdx);}
		else {		if(this.isNodeJustMade){	setAdjNodeDirIDVals(E, -1);}}		
		//north
		if(neighborNodeN != null){	calcNodeGeometry_PipeLength(neighborNodeN,N,h2oIdx);}
		else {		if(this.isNodeJustMade){	setAdjNodeDirIDVals(N, -1);}}		
		//south
		if(neighborNodeS != null){	calcNodeGeometry_PipeLength(neighborNodeS,S,h2oIdx);}
		else {		if(this.isNodeJustMade){	setAdjNodeDirIDVals(S, -1);}}	
	}//calcNodeGeometry		
	
	/**
	 * handles calculating the specific length of the pipe from this node to its neighbors
	 * @param neighborNode
	 * @param dir
	 */
	private void calcNodeGeometry_PipeLength(myHeightNode neighborNode, int _dir, int h2oIdx){
		int neighborNodeID = neighborNode.ID;
		//reset boundary node structures
		double neighborWater = neighborNode.getHeightWater(h2oIdx);
		if(this.isNodeJustMade){
			setAdjNodeDirIDVals(_dir, neighborNodeID);
			this.distToNeighbors[0][_dir] = 1; 		
			this.adjNodes.put(neighborNodeID, neighborNode);
			calcBoundaryNode(neighborNode, _dir, neighborWater, h2oIdx);
		}
		
		double len, high, heightTerrain;		//heightTerrain is height of terrain
			//check if wrap around in x dir
		boolean addWrapHeightDiff = ((this.isMirrorNode[0]) && (((this.source.xIDX == 0) && (neighborNode.source.xIDX == eng.heightMapAra.length-1)) || 
																((neighborNode.source.xIDX == 0) && (this.source.xIDX == eng.heightMapAra.length-1))));
	
		//double diffSed = this.getSediment() - neighborNode.getSediment();	//add to height for height calcs
		heightTerrain = this.source.coords.y + (addWrapHeightDiff ? this.avgNeighborHeightDiff[0] : 0) - neighborNode.source.coords.y;// + diffSed;	//positive height difference means this node is higher than neighbor
		high = heightTerrain + this.getHeightWater(h2oIdx) - neighborWater;	//(this.getCurHeight() + this.getHeightWater(0)) - (neighborNode.getCurHeight() + neighborWater);
		len = (double)Math.pow((1 + (high * high)), .5);	//1 -> wide * wide, all cells are 1 apart from neighbors
		this.pipeLengths.put(neighborNodeID, len);        //idxed by same idx-class as src - if clone use cloneid conversion to get id
		this.adjHeightDiffs.put(neighborNodeID, high);
		this.adjHeightDiffsTer.put(neighborNodeID, heightTerrain);		
		this.neighborSlope[_dir] = Math.abs(heightTerrain);		//used to calculate "bumpiness" of terrain after erosion
			//recalculate the display normals for each of the 3 main display values : water, sediment and sed capacity
		this.reCalcDispNormals();
	}//handlePipeLengthCalculations
	
	/**
	 * calculate all boundary (no water) neighbor nodes based on direction
	 * @param h2oIdx index in the water height ara we are currently at
	 */	
	public void calcBoundaryNodes(int h2oIdx){
		this.boundaryAdjNodeDirByID = new HashMap<Integer,Integer>();
		this.boundaryAdjNodeIDByDir = new int[numDir];
		this.boundaryAdjNodeByDir = new myHeightNode[numDir];						//idx'ed by dir, holds refs to boundary nodes, or null
		for (int _dir = 0; _dir < numDir; ++_dir){
			myHeightNode neighborNode = this.getAdjNodesByPassedDir(_dir);	
			if(neighborNode != null){			calcBoundaryNode(neighborNode, _dir, neighborNode.heightWater[h2oIdx], h2oIdx);	}
			//else {gui.print2Cnsl("null neighbor node : " + this.ID + " neighbor in dir : " + _dir);}
		}//for each direction
	}//calcBoundaryNodes

	
	// calc if this node is a boundary node relative to the passed neighbor
	private void calcBoundaryNode(myHeightNode neighborNode, int _dir, double neighborWater, int idx){		
		if (this.getHeightWater(idx) > neighborWater * myConsts.NoWater){//if this node has more than 1000x as much water as the neighbor node, then consider it a boundary node
			this.boundaryNode = true;
			neighborNode.boundaryDryNode = true;
			this.setBoundaryAdjNodeDirIDVals(_dir, neighborNode.ID);
		}//only boundary if has water and has a neighbor with no water
		else {	this.setBoundaryAdjNodeDirIDVals(_dir, -1);}//set to id -1 so that we don't automatically consider node 0 for all non-boundary nodes
	}//calcBoundaryNode
	
	/**
	 * this will calculate the node normal for a particular display quantity at this node
	 * @param dispVal the quantity being displayed - idx from myConsts, either h2o, sed, or sedcap
	 * @return the new normal
	 */
	public myVector calcDispNodeNorm(myVector nodeNorm, int dispVal){
		nodeNorm.set(0,0,0,0);
		myVector thisSrcCoords = this.source.coords.cloneMe();
		thisSrcCoords._add(0,this.getVisualisationVal(dispVal,0),0);
		
		myVector[] adjCoords = new myVector[4];	
		myVector[] crossProds = new myVector[4];
		
		if (this.source.zIDX != eng.heightMapAra[0].length-1){//north is legal	
			adjCoords[N] = this.adjNodes.get(this.adjNodeIDByDir[N]).source.coords.cloneMe();			
			adjCoords[N]._add(0, this.adjNodes.get(this.adjNodeIDByDir[N]).getVisualisationVal(dispVal,0), 0);
		} else {	adjCoords[N] = thisSrcCoords;	}
		
		if (this.source.xIDX != eng.heightMapAra.length-1){//east is legal
			adjCoords[E] = this.adjNodes.get(this.adjNodeIDByDir[E]).source.coords.cloneMe(); 
			adjCoords[E]._add(0, this.adjNodes.get(this.adjNodeIDByDir[E]).getVisualisationVal(dispVal,0), 0);
		} else {	adjCoords[E] = thisSrcCoords;	}	
		
		if (this.source.xIDX != 0){ //west is legal
			adjCoords[W] = this.adjNodes.get(this.adjNodeIDByDir[W]).source.coords.cloneMe();
			adjCoords[W]._add(0, this.adjNodes.get(this.adjNodeIDByDir[W]).getVisualisationVal(dispVal,0), 0);
		}  else {	adjCoords[W] = thisSrcCoords;	}
		
		if (this.source.zIDX != 0) {//south is legal
			adjCoords[S] = this.adjNodes.get(this.adjNodeIDByDir[S]).source.coords.cloneMe();
			adjCoords[S]._add(0, this.adjNodes.get(this.adjNodeIDByDir[S]).getVisualisationVal(dispVal,0), 0);
		}  else {	adjCoords[S] = thisSrcCoords;	}
		
		crossProds[0] = myVector._cross(adjCoords[N].x - thisSrcCoords.x, adjCoords[N].y - thisSrcCoords.y, adjCoords[N].z - thisSrcCoords.z, 
				adjCoords[E].x - thisSrcCoords.x, adjCoords[E].y - thisSrcCoords.y, adjCoords[E].z - thisSrcCoords.z);
		crossProds[1] = myVector._cross(adjCoords[W].x - thisSrcCoords.x, adjCoords[W].y - thisSrcCoords.y, adjCoords[W].z - thisSrcCoords.z, 
				adjCoords[N].x - thisSrcCoords.x, adjCoords[N].y - thisSrcCoords.y, adjCoords[N].z - thisSrcCoords.z);
		crossProds[2] = myVector._cross(adjCoords[S].x - thisSrcCoords.x, adjCoords[S].y - thisSrcCoords.y, adjCoords[S].z - thisSrcCoords.z, 
				adjCoords[W].x - thisSrcCoords.x, adjCoords[W].y - thisSrcCoords.y, adjCoords[W].z - thisSrcCoords.z);
		crossProds[3] = myVector._cross(adjCoords[E].x - thisSrcCoords.x, adjCoords[E].y - thisSrcCoords.y, adjCoords[E].z - thisSrcCoords.z, 
				adjCoords[S].x - thisSrcCoords.x, adjCoords[S].y - thisSrcCoords.y, adjCoords[S].z - thisSrcCoords.z);
		for(int i = 0; i < 4; ++i){		nodeNorm._add(crossProds[i]);	}		
		nodeNorm._normalize();		
		return nodeNorm;	//returns newlycalculated normal also - sets whichever vector is passed via nodeNorm	
	}//calcDispNodeNorm
	
	//calculate all the normals for this node for displaying sedcap, water and sediment meshes
	public void reCalcDispNormals(){
		if((this.heightWater[0] != 0) && (p.allFlagsSet(myConsts.H2OMeshDisplay))){			this.calcDispNodeNorm(this.H2O_Norm, myConsts.H2O);	}
		if((this.sediment[0] 	!= 0) && (p.allFlagsSet(myConsts.sedMeshDisplay))){			this.calcDispNodeNorm(this.Sed_Norm, myConsts.SED);	}
		if((this.sedCapPipes != 0) && (p.allFlagsSet(myConsts.sedCapMeshDisplay))){			this.calcDispNodeNorm(this.SedCap_Norm, myConsts.SEDCAP);	}
		if((this.sedConcVis != 0) && (p.allFlagsSet(myConsts.sedConcMeshDisplay))){			this.calcDispNodeNorm(this.SedConc_Norm, myConsts.SEDCONC);	}
	}//reCalcDispNormals
	
	//replaced with ara-version
	public void calcNodeGeometryOld(boolean isClone){}//calcNodeGeometryOld	
	// finds the direction and x-z distance to this node's passed neighbor
	public void calcDistAndDirToNeighbor(myHeightNode neighborNode){
	    int val = neighborNode.getID();
	    if (this.source.getCoords().x - neighborNode.getSource().getCoords().x > 0) { 	  setAdjNodeDirIDVals(W, val);this.distToNeighbors[0][W] = 1;}//this.source.getCoords().x - neighborNode.getSource().getCoords().x; }  //neighbor.x smaller, neighbor is to west (to left)
	    else if (this.source.getCoords().x - neighborNode.getSource().getCoords().x < 0) {setAdjNodeDirIDVals(E, val);this.distToNeighbors[0][E] = 1;}//= neighborNode.getSource().getCoords().x -  this.source.getCoords().x;} //neighbor.x greater - neighbor is to east
	    else if (this.source.getCoords().z - neighborNode.getSource().getCoords().z < 0) {setAdjNodeDirIDVals(N, val);this.distToNeighbors[0][N] = 1;}//= neighborNode.getSource().getCoords().z - this.source.getCoords().z; } //neighbor.z greater - neighbor is to North (toward viewer)
	    else if (this.source.getCoords().z - neighborNode.getSource().getCoords().z > 0) {setAdjNodeDirIDVals(S, val);this.distToNeighbors[0][S] = 1;}//= this.source.getCoords().z - neighborNode.getSource().getCoords().z;	}  //neighbor.z smaller, neighbor is to south
	    else {gui.print2Cnsl("Error when building adjNodeDirIdx ara for node : " + this.ID);}//error condition		
	}//calcDistAndDirToNeighbor

	//sets local arrays based on passed direction and adj node id
	public void setAdjNodeDirIDVals(int dir, int val){
		//if(dir == -1){gui.print2Cnsl("illegal mirror vert dir for base node : " + this.ID + " at "+ this.source.coords.toString() + " and adj node id : " + val);}
		this.adjNodeIDByDir[dir] = val;  this.adjNodeDirByID.put(val, dir); }//setAdjNodeDirIDXVals  

	 // sets local arrays appropriately for boundary nodes at particular direction and id values
	public void setBoundaryAdjNodeDirIDVals(int dir, int val){ 
		this.boundaryAdjNodeIDByDir[dir] = val;  
		if(val != -1) {		this.boundaryAdjNodeDirByID.put(val, dir);	}
	}//setBoundaryAdjNodeDirIDXVals  
	
	public void setBoundaryNode(boolean _bnd){this.boundaryNode = _bnd;}	
	//sets the water share for the adjacent node after current flux value determined - used as a ratio
	public void setAdjWaterShare(int dir, double val){ this.adjWaterShare[dir] = val; }  
	//  returns sum of flux values for time t (idx 0) or t+deltaT (idx 1)  
	public double getFluxSum(int idx){   double sum = 0;   for (int dir = 0; dir < numDir; ++dir){  sum += this.flux[idx][dir]; }   return sum;}//getFluxSum
	//  scales flux value by passed multiplier
	public void scaleFlux(int step, int dir, double val){ this.flux[step][dir] *= val;}	
	//  scales sediment at idx srcIDX by passed multiplier - used by bandaid solution
	public void scaleSedimentPipes(int srcIDX, double val){ this.sediment[srcIDX] *= val;}
	
	/**
	 * calculates the velocity vector of this heightnode based on the flux in from its neighbors minus the flux out in each direction to its neighbors of some quantity
	 * @param xFluxInL - flux in from the left neighbor in the x direction (west)
	 * @param xFluxInR - flux in from the right neighbor in the x direction
	 * @param zFluxInT - flux in from the top neighbor, in the z direction
	 * @param zFluxInB - flux in from the bottom neighbor, in the z direction
	 * @param fluxIDX the progress index in the flux arrays to use to calculate the delta W
	 * @param quantity the array of values whose flux is to be calculated 
	 * @param sIDX the target idx of the quantity to compare - compare to sIDX-1 : for water this is 2 and 1, respectively
	 * @param delV - deltaV in fast paper - change in vol of water at node
	 */
	public void calcAndSetVelocity(double xFluxInL, double xFluxInR, double zFluxInT, double zFluxInB, int fluxIDX, double[] quantity, int sIDX, double delV){
		double delWxWest = 0, delWxEast= 0, delWzNorth = 0, delWzSouth = 0, delWx = 0, delWy = 0, delWz = 0, avgQuant, udelT = 0, vdelT = 0, wdelT = 0;
		avgQuant = (quantity[sIDX-1] + quantity[sIDX])/2.0;
		//if (avgH2O < 0){gui.print2Cnsl("DANGER : NEGATIVE H2O at node : " + this.toString());}
		if (avgQuant > 0){	
			delWxWest  = xFluxInL - this.flux[fluxIDX][W];
			delWxEast  = this.flux[fluxIDX][E] - xFluxInR;
			delWzNorth = this.flux[fluxIDX][N] - zFluxInT;
			delWzSouth = zFluxInB - this.flux[fluxIDX][S];

			delWy = delV;// change of volume in node 
			
			delWx = (delWxWest  + delWxEast)/2.0;
			delWz = (delWzNorth + delWzSouth)/2.0;
				
			udelT = delWx/(avgQuant);
			wdelT = delWy;///(avgQuant);
			vdelT = delWz/(avgQuant);
			//used for visualization		
		} //if change in water less than epsval then velocity is 0
		this.fluxForce.set(this.flux[fluxIDX][E] - this.flux[fluxIDX][W], avgQuant, this.flux[fluxIDX][N] - this.flux[fluxIDX][S]);		
		this.velocity.set(udelT, wdelT, vdelT);
	}//calcAndSetVelocity
	  
	/**
	*  initialize pipes values for start of pipes based sim
	*/
	public void initValsPipes(){
	    for (int i = 0; i < 5; ++i){
	    	this.heightWater[i] = 0;
	    	this.sediment[i] = 0;
	    	for (int dir = 0; dir < numDir; ++dir){ this.flux[i][dir] = 0; }//for dir    
    	}//for each step present
   		this.velocity = new myVector();   	    
	}//set new values for pipes-based water transport and erosion
	
	
	/**
	 * calculate and return the change in volume of a particular material in this node between start idx and end idx
	 * @param matIDX 	what the material is to examine change in - use idx's defined in global for H2O or SED
	 * @param startIDX	what index to use as start of material change
	 * @param endIDX	what index to use as end of material change
	 * @return			value of difference
	 */
	public double calcChangeInVal(int matIDX, int startIDX, int endIDX){
		switch (matIDX) {
			case myConsts.H2O : { return (this.heightWater[endIDX] - this.heightWater[startIDX]); }
			case myConsts.SED : { return (this.sediment[endIDX] - this.sediment[startIDX]); }
			default : {						  return 0;	}		
		}//switch
	}//calcChangeInVol
 
	/**
	*  set member vals related to fluvial erosion to match those of passed in node for pipes model
	*/
	public void setNewValsPipes(myHeightNode tmpSrcNode, boolean isClone){
	    this.heightWater[0] = tmpSrcNode.getHeightWater(2);
	    if(p.allFlagsSet(myConsts.debugModeCnsl)) {    checkValidVal(0, "Water - set", this.heightWater[0], 0);}					//verify water vol
	    if(!isClone){this.p.addToTotalTurnVal(myConsts.H2O, this.heightWater[0]);}    	
	    this.source.coords.y = tmpSrcNode.source.coords.y;													//only need y value	
	    this.sediment[0] = tmpSrcNode.getSediment(2);														//idx 2 holds most recent sediment value
	    if(this.sediment[0] < 0){this.sediment[0] = 0;}	//TODO
	    tmpSrcNode.sedLeftOver = tmpSrcNode.sediment[1] - tmpSrcNode.setTransportedDebug;					//sediment calculated for this step minus sediment transported out of node
	    tmpSrcNode.sediment[1] = 0;
	    tmpSrcNode.sediment[2] = 0;																			//clear out passed node's sediment in idx2
	    if(!isClone){this.p.addToTotalTurnVal(myConsts.SED, this.sediment[0]);    }							//add this new sediment amt to total turn-based value" 
	    this.velocity.set( tmpSrcNode.getVelocity());	    	
	    for (int dir = 0; dir < numDir; ++dir){	      
	    	this.flux[0][dir] = tmpSrcNode.getFluxByDir(1,dir);	
	    	//if(!isClone){this.flux[1][dir] = 0;}
	    }//clear flux for next round
	    this.flux[1] = new double[numDir];
	    this.scaledVal = tmpSrcNode.getScaledValAra();
	    this.setGlobVals(0);
	    this.setGlobValsSlope();//for calculating efficacy of ks and kd values
	    p.setGlobCoordVals(source.coords);	 
	    this.windForce = new double[numDir];
	}//set new values for pipes-based water transport and erosion
	
	public void clearSedFromDryNode(){
	    //convert all sediment to height if on a dry node, make close-to-zero water val = 0
    	this.addHeightByVal(this.sediment[0]);
    	this.sediment[0] = 0;  
	}//clearSedFromDryNode

	/**
	*  set volume of water for pipes algorithm
	*/
	public void setHeightWater(double vol, int idx, boolean setGlobal){
	    this.heightWater[idx] = vol;
    	if(setGlobal) {this.setGlobValsH2O(idx);} 
	}//setvolWater using idx

	/**
	*  add an initial amount of water in a block - only called by damsetup
	*/
	public void addRainByAmount(double amt, boolean isClone){
		this.addWater(amt, 0); 
		if(!isClone){p.addToTotalVal(myConsts.H2O, amt);}
	}//addRainByAmount

	
	/**
	*     add a specific amount of water to current water amount - used only in rain cycle of pipes sim - only adds to clone nodes, used as sources for calculations
	*/
	public void addWaterPipes(double vol, int srcIDX, int destIDX){ 
	    this.heightWater[destIDX] = vol + this.heightWater[srcIDX];
	  //  p.addToTotalVal(myConsts.H2O, vol);
	}//addVolWater

	public void setWindForce(myVector force){ 
		this.windForce[N] = force.z;
		this.windForce[S] = -force.z;
		this.windForce[W] = force.x;
		this.windForce[E] = -force.x;	
		/*this.windForce[NW] = force.z;
		this.windForce[NE] = -force.z;
		this.windForce[W] = force.x;
		this.windForce[E] = -force.x;*/	
	}
	
	/**
	 * add sediment to value in srcIDX, put total in destIDX
	 * @param addAmt	amount to add
	 * @param srcIDX original sediment value being added to
	 * @param destIDX destination sediment location
	 */	
	public void addSediment(double addAmt, int srcIDX, int destIDX){
		//checkValidVal(srcIDX, "sediment - Add " + addAmt + " to " + this.sediment[srcIDX], this.sediment[srcIDX], addAmt);
		this.sediment[destIDX] = addAmt + this.sediment[srcIDX];	
		if(this.sediment[destIDX] < 0){
			gui.print2Cnsl("negative sediment : Node ID " + this.ID + " ara loc : " + this.source.xIDX +"|" + this.source.zIDX );
			p.setFlags(myConsts.simulate, false);		
		}
		this.setGlobValsSED(destIDX);
	}//addSediment	
	
	/**
	 *  add a specific sediment amount to current sediment amount
	 */
	public void setSedimentPipes(double d, int idx){
		this.setSediment(d, idx);
		this.setGlobValsSED(idx);
	}//addSediment	
	
	public void setSediment(double _amt, int idx){	this.sediment[idx] = _amt;		}
	
	/**
	 *  add a specific amount of water to current water amount
	 */
	public void addWater(double vol, int idx){ 
		this.heightWater[idx] += vol;
		//checkValidVol(vol, idx, "Water - Add " + vol + " to " + this.volWater[idx], this.volWater[idx]);
		//if (Math.abs(this.volWater[idx]) < myConsts.epsVal){this.volWater[idx] = 0;}
		if (idx == 0){			this.setGlobValsH2O(idx);}
	}//addVolWater
	/**
	 *
	 */
	public void clearPipesErosionVals(){ initValsPipes();}//clear pipeserosionVals 
  
	/**
	 *  clears out all erosion-related values in this node
	 */
	public void clearErosionVals(){
		this.heightWater = new double[5];
		this.sediment = new double[5];
		this.flux = new double[5][4];            //flux for each cardinal direction, for every step of the process
		this.velocity = new myVector();
	}//clearErosionVals
  
//	/**
//	 *  some water evaporates 
//	 */
//	public void evaporateWater(int idx){
//		double waterToEvaporate = this.heightWater[idx] *  this.getKe() * p.getDeltaT();
//		this.addWater(-1* waterToEvaporate, idx);   
//	}//evaporate water
  
	/**
	 *  calculates and sets this object's scaled value based on the visualisation idx passed
	 */
	public int calcScaledVal(final int visIdx, int idx){
		if (visIdx != -1){      //no data visualisation
			this.scaledVal[visIdx] = p.calcScaledVal_255(visIdx, this.getVisualisationVal(visIdx, idx));
			return this.scaledVal[visIdx];
		} else { return 0; }//if idx is -1
	}//calcScaledVal method
  
	/**
	 *  returns a value to build the visualisation on
	 */
	public double getVisualisationVal(final int visIdx, int idx){
		double value;
		String visStr = "";
		if (visIdx != -1){      //no data visualisation
			switch (visIdx){
				case myConsts.H2O 		: { visStr = "h2o";value = this.heightWater[idx]; break; }//h2o
				case myConsts.SED 		: { visStr = "sed";value = this.sediment[idx]; break; }//sediment
				case myConsts.SEDCAP	: { visStr = "sedcap";value = this.sedCapPipes; break;	}
				case myConsts.SEDCONC	: { visStr = "sedconc";value = this.sedConcVis; break;	}
				default : { value = 0; break;}
			}//switch on IDX - type of scaled value		
			return value;
		} else { return 0; }//if idx is -1        
	}//geVisualisationVal

	/**
	 *  verifies that there's no peculiarities with the levels that need to be non-negative - returns true if they are not negative
	 */ 
	public boolean checkValidVal(int idx, String type, double origVal, double volToAdd){
		if (origVal + volToAdd < -1 * myConsts.epsValCalc) {
			gui.print2Cnsl("\t---error can't have negative " + type );
			gui.print2Cnsl("\t---at Node : " + this.ID + " x: " + this.source.coords.x +" z: " + this.source.coords.z + " : existing amt :" + origVal + " | modamt : " + volToAdd);
			return false;}
		return true;
	}//checkVolWater method 
   
	/**
	 *  sets the appropriate global values based on the water in this node
	 */
	public void setGlobVals(int idx){
		this.setGlobValsH2O(idx);
		this.setGlobValsSED(idx);
		this.setGlobValsSEDCAP(idx);
		this.setGlobValsSEDCONC(idx);
 	 }//setGlobH2Ovals
  
  	/**
  	 *  sets the appropriate global values based on the water in this node
  	 */
  	public void setGlobValsH2O(int idx){
  		p.globMaxAra[myConsts.H2O] = Math.max(p.globMaxAra[myConsts.H2O], this.heightWater[idx]);
  		p.globMinAra[myConsts.H2O] = Math.min(p.globMinAra[myConsts.H2O], this.heightWater[idx]);
  	}//setGlobH2Ovals
 
  	/**
  	 *  sets the appropriate global values based on the sediment in this node
  	 */
  	public void setGlobValsSED(int idx){
  		p.globMaxAra[myConsts.SED] = Math.max(p.globMaxAra[myConsts.SED], this.sediment[idx]);
  		p.globMinAra[myConsts.SED] = Math.min(p.globMinAra[myConsts.SED], this.sediment[idx]);
  	}//setGlobH2Ovals
  	
  	/**
  	 *  sets the appropriate global values based on the sediment capacity in this node
  	 */
  	public void setGlobValsSEDCAP(int idx){
  		p.globMaxAra[myConsts.SEDCAP] = Math.max(p.globMaxAra[myConsts.SEDCAP], this.sedCapPipes);
  		p.globMinAra[myConsts.SEDCAP] = Math.min(p.globMinAra[myConsts.SEDCAP], this.sedCapPipes);
  	}//setGlobH2Ovals  	
    	
  	/**
  	 *  sets the appropriate global values based on the sediment capacity in this node
  	 */
  	public void setGlobValsSEDCONC(int idx){
  		p.globMaxAra[myConsts.SEDCONC] = Math.max(p.globMaxAra[myConsts.SEDCONC], this.sedConcVis);
  		p.globMinAra[myConsts.SEDCONC] = Math.min(p.globMinAra[myConsts.SEDCONC], this.sedConcVis);
  	}//setGlobH2Ovals  	  	
  	/**
  	 * sets appropriate min/max arrays for velocity components and magnitude of velocity
  	 * @param the idx of the velocity in question, either p.H2O or p.SED
  	 */
  	public void setGlobValsVelocity(){
		p.globMaxAra[myConsts.Vel_X] = Math.max(p.globMaxAra[myConsts.Vel_X], this.velocity.x);
    	p.globMinAra[myConsts.Vel_X] = Math.min(p.globMinAra[myConsts.Vel_X], this.velocity.x);
		p.globMaxAra[myConsts.Vel_Y] = Math.max(p.globMaxAra[myConsts.Vel_Y], this.velocity.y);
    	p.globMinAra[myConsts.Vel_Y] = Math.min(p.globMinAra[myConsts.Vel_Y], this.velocity.y);
		p.globMaxAra[myConsts.Vel_Z] = Math.max(p.globMaxAra[myConsts.Vel_Z], this.velocity.z);
    	p.globMinAra[myConsts.Vel_Z] = Math.min(p.globMinAra[myConsts.Vel_Z], this.velocity.z);
		p.globMaxAra[myConsts.Vel_Mag] = Math.max(p.globMaxAra[myConsts.Vel_Mag], Math.sqrt(this.velocity.sqMagn));
    	p.globMinAra[myConsts.Vel_Mag] = Math.min(p.globMinAra[myConsts.Vel_Mag], Math.sqrt(this.velocity.sqMagn));
    	
  	}//setGlobalValsVelocity
  	
  	/**
  	 * sets global min and max vals for neighbor slope, used to determine efficacy of a set of erosion constants
  	 */
  	public void setGlobValsSlope(){
  		for(int i = 0; i < numDir; ++i){
  			p.globMaxAra[myConsts.ERODED_SLOPE] = Math.max(p.globMaxAra[myConsts.ERODED_SLOPE], this.neighborSlope[i]);
  			if(this.neighborSlope[i] >= (.9 * p.globMaxAra[myConsts.ERODED_SLOPE])){
  				++p.globMaxAra[myConsts.EXTREME_SLOPE];
  				//increment max ara for slope
  			}
  			p.globMinAra[myConsts.ERODED_SLOPE] = Math.min(p.globMinAra[myConsts.ERODED_SLOPE], this.neighborSlope[i]);
  			if(this.neighborSlope[i] <= (1.1 * p.globMinAra[myConsts.ERODED_SLOPE])){
  				++p.globMinAra[myConsts.EXTREME_SLOPE];
  				//increment min ara for slope vals
  			}
  		}
  	}
  	
  	
  	/**
  	 *  add rain to this node - calculate height ratio between the height of this node and the global max height to determine how much rain this node gets
  	 *  @param multiplier to height to add to amount of rain received - used to exponentially increase the amount the higher the altitude
  	 *  @param random multiplier to multiply rain amount received - currently set to 1
  	 */
  	public void addRain(double heightMod, double rainMult, double delT, boolean isClone){
  		if (this.source.isRainOnMe()){
  			double heightRatio = 1;
  			if (Math.abs(p.globMaxAra[myConsts.COORD_Y] - p.globMinAra[myConsts.COORD_Y]) <  myConsts.epsValCalc){//heights the same, 
  				heightRatio = 1;
  			} else {
  				heightRatio = ((this.source.coords.y - p.globMinAra[myConsts.COORD_Y]) / (p.globMaxAra[myConsts.COORD_Y] - p.globMinAra[myConsts.COORD_Y]));
  				if ((this.source.coords.y - p.globMinAra[myConsts.COORD_Y]) < 0 ) { gui.print2Cnsl("bad height ratio : " + this.ID + " :" + heightRatio + "|" + this.source.coords.y + "|" + p.globMinAra[myConsts.COORD_Y]);}
  			}
  			double rainDropVol = eng.globRaindropMult;
  			double rainDropVolMod = eng.globRaindropMult * heightRatio;
  			double rainToAdd = (((rainMult + (this.source.coords.y * heightMod)) * (rainDropVol + rainDropVolMod) )) * delT;
  			//add water to 0th index of the heightWater array
  			this.addWater(rainToAdd, 0);
  			if(!isClone){				p.addToTotalVal(myConsts.H2O, rainToAdd);}

  			//this.addVolWater(this.volWater[0], 1);//set up for pipes calculation by copying water in idx 0 to idx 1
  		}//if this node gets rain
  	}//addrain method  
 
  	/**
  	 * moves values from idx 0's to appropriate final idx's for non-hydraulic erosion so that values are not lost when nodes are updated
  	 */
  	public void moveOldValsPipes(boolean raining, boolean thermalWeathering){
  		if(!raining){		
  			this.heightWater[2] = this.heightWater[0];
  			this.sediment[2] = this.sediment[0];
  		} else {
  			this.heightWater[2] = this.heightWater[1];
  			this.sediment[2] = this.sediment[1];
  		}
  		this.sediment[1] = this.sedLeftOver;			//there is no sediment being transported in this cycle, so sed left over will be same as before and setTransportedDebug == 0
  
	    for (int dir = 0; dir < numDir; ++dir){	        	this.flux[1][dir] = this.flux[0][dir];}   	//if(!isClone){this.flux[1][dir] = 0;}
  	}//move values to appropriate idx's so that they are preserved when hyrdaulic erosion isn't executed but thermal and rain is
  	
  	/**
  	 *  set member vals related to fluvial erosion to match those of passed in node 
  	 */
  	public void setNewVals(myHeightNode tmpSrcNode){
  		this.heightWater[0] = tmpSrcNode.heightWater[0];
  		this.source.coords.y = tmpSrcNode.source.coords.y;
  		this.sediment[0] = tmpSrcNode.getSediment();
  		this.scaledVal = tmpSrcNode.getScaledValAra();
  		//this.source.coords.set(tmpSrcNode.source.coords);
  		this.setGlobVals(0);
  	}//setNewVals   
  	
//sediment-related 2nd order pipes methods
// 	/**
//	 *  calculate erosion effects 
//	 * @param delT - timestep
//	 * @param isClone - whether or not this is a clone node
//	 */
//	public void calcPipesSedErosionDeposition(double delT,  boolean isClone, boolean useSedFlux, int h2oIDX){
//		int sedIDX = (useSedFlux) ? 1 : 0;
//		double calcNewSed, calcK_Val;
//			//	this node's sediment capacity minus the total sediment on this node
//		this.sedCapPipes = eng.calc2ndOrderSedCapacity(this, this.heightWater[h2oIDX], true);	
//
//		double calcCapSedDiff = this.sedCapPipes - (this.sediment[sedIDX]);					
//			//determine which constant to use, Ks or Kd and how much sediment to generate
//		calcK_Val = ((calcCapSedDiff <= 0) ? (this.lclKd) :  (this.lclKs));
//			//capacity greater than sediment, erode some soil calcCapSedDiff > 0; sediment greater than capacity, deposit some soil - calcCapSedDiff <= 0		
//			//calcCapSedDiff is negative if too much sediment in water(uses kd to deposit), positive if not enough (uses ks to lift up amount)
//		calcNewSed = calcK_Val * calcCapSedDiff;   //negative for deposition, positive for erosion
//			//change either height into sediment, or vice versa
//		eng.convertHeightToSed(this, this, calcNewSed, calcNewSed, useSedFlux);
//		//if(!isClone){	    p.addToTotalVal(myConsts.SED, this.sediment[destIdx]);}//if not a clone node, store amount of sediment being modified in this method - this will hold the total amt of sediment lifted/dropped at this node
//			//for debug purposes, clear sediment transported at this stage so we can set it appropriately in transport stage TODO: verify necessity
//		this.setTransportedDebug = 0;
//	}//calc2ndOrderErosionValsPipes	  	
  	
  	/**
	 *  calculate erosion effects - including mass wasting effects if enabled
	 * @param delT - timestep
	 * @param isClone - whether or not this is a clone node
	 */
	public void calcPipesSedErosionDeposition(double delT, boolean isClone, boolean useSedFlux, int h2oIDX){
		int sedSrcIDX = (useSedFlux) ? 1 : 0;//src idx of sediment
		boolean scaleVol = true;	//must scale sedcap by volume
		double calcNewSed, calcK_Val, calcK_ValMW, calcMWNewSed;
			//	this node's sediment capacity minus the total sediment on this node
		this.sedCapPipes = eng.calc2ndOrderSedCapacity(this, this.heightWater[h2oIDX], scaleVol);	
		double calcCapSedDiff = this.sedCapPipes - (this.sediment[sedSrcIDX]);			
		//determine which constant to use, Ks or Kd and how much sediment to generate
		calcK_Val = ((calcCapSedDiff <= 0) ? (this.lclKd) :  (this.lclKs));
			//capacity greater than sediment, erode some soil calcCapSedDiff > 0; sediment greater than capacity, deposit some soil - calcCapSedDiff <= 0		
			//calcCapSedDiff is negative if too much sediment in water(uses kd to deposit), positive if not enough (uses ks to lift up amount)
		calcNewSed = calcK_Val * calcCapSedDiff;   //negative for deposition, positive for erosion
		//double oldSed = this.sediment[sedSrcIDX];
		//if(this.heightWater[2] < this.sediment[sedSrcIDX]){ gui.print2Cnsl("1) : sed > water before conv in : " + this.ID + " sed : " + oldSed + " calcnewsed : " + calcNewSed + " water : " + this.heightWater[2] );}
		//modify sediment and height
		eng.convertHeightToSed(this, this, calcNewSed, calcNewSed, useSedFlux, false);
		//if(this.heightWater[2] < this.sediment[sedSrcIDX+1]){ gui.print2Cnsl("1) : sed > water after conv in : " + this.ID + " sed cap : " + this.sedCapPipes + " old sed : " + oldSed + " new sed :  " + this.sediment[sedSrcIDX + 1] + " h2o : " + this.heightWater[2]);}

		if ((p.allFlagsSet(myConsts.calcLateralErosion)) && (this.boundaryNode)){
			myHeightNode adjNode;	
			for (int dir = 0; dir < numDir; ++dir){
				if (this.boundaryAdjNodeIDByDir[dir] != -1){//this means the adjacent node in dir direction is a boundary node
					adjNode = this.adjNodes.get(this.boundaryAdjNodeIDByDir[dir]);
					//if(adjNode == null){gui.print2Cnsl("in node : " + this.ID + "null adj node in dir : " + dir + " bound adj node : " + this.boundaryAdjNodeIDByDir[dir] );}
					double dirVComp;//directional component of velocity - takes the place of volume of water in sed cap calculation
					
					switch (dir){
						case N : {		dirVComp = this.velocity.z;		break;}
						case S : {		dirVComp = -this.velocity.z;	break;}
						case E : {		dirVComp = this.velocity.x;		break;}
						case W : {		dirVComp = -this.velocity.x;	break;}								
						default : {		dirVComp = 0;	gui.print2Cnsl("invalid direction : " + dir + " for node :" + this.ID);	break;}
					}//for each direction
					
					//describes the abrasive effect of sediment in the water on neighboring terrain - use sediment concentration in this node to scale, after previous dep/erosion process
					//not used if velocity is negative
					double latSedMult = ((dirVComp > 0) ? (1 + Math.abs((this.sediment[sedSrcIDX + 1]/this.heightWater[h2oIDX]))) : 1);
					//sed cap in a particular direction due to mass-wasting-causing properties	
					if (adjNode != null){
							this.latSedCapPipes[dir] = eng.calc2ndOrderLatSedCapacity(adjNode.lclKc, dirVComp, this.heightWater[h2oIDX], scaleVol, latSedMult) 
							* this.lclKw * eng.globLatErosionMult;
						calcK_ValMW = (this.latSedCapPipes[dir] <= 0 ? (this.lclKd) :  (this.lclKs + this.lclKw));
						
						calcMWNewSed = calcK_ValMW * (this.latSedCapPipes[dir] );
						//change either height into sediment, or vice versa
						if (calcMWNewSed > 0) {				eng.convertHeightToSed(adjNode, this, calcMWNewSed,  calcMWNewSed, true, true);}//pulling up from adj node and adding to this node's sed load
						else  {		//make sure we don't end up getting negative sediment
							double newSedModAmt;
							if((calcMWNewSed + this.sediment[((useSedFlux) ? 2 : 1)]) < 0) {	newSedModAmt = -this.sediment[((useSedFlux) ? 2 : 1)]/2.0f;}
							else {																newSedModAmt = calcMWNewSed;				}
							eng.convertHeightToSed(this, this, calcMWNewSed, newSedModAmt, useSedFlux, true);}
							//eng.convertHeightToSed(this, this, calcMWNewSed, calcMWNewSed, useSedFlux, true);}
					}
						
				}//if node in this direction is a boundary node
			}//for each direction		
		}//if lateral erosion				
		//if(!isClone){	    p.addToTotalVal(myConsts.SED, this.sediment[destIdx]);}//if not a clone node, store amount of sediment being modified in this method - this will hold the total amt of sediment lifted/dropped at this node
			//for debug purposes, clear sediment transported at this stage so we can set it appropriately in transport stage TODO: verify necessity
		this.setTransportedDebug = 0;
	}//calc2ndOrderErosionValsPipes		
	
//	/**
//	 *  calculate erosion effects inspired by FAST paper for adjacent nodes with no water - aka masswasting
//	 * @param delT - timestep
//	 * @param isClone - whether or not this is a clone node
//	 */
//	public void calcPipesSedErosionDepAdjNode(double delT,  double scaleFact, boolean isClone, boolean useSedFlux){
//		int destIdx = ((useSedFlux) ? 2 : 1);
//		double calcNewSed, calcK_Val;
//		myHeightNode adjNode;	
//		double calcCapSedDiff = this.sedCapPipes - (this.sediment[destIdx]);			
//		for (int dir = 0; dir < numDir; ++dir){
//			if (this.boundaryAdjNodeIDByDir[dir] != -1){//this means the adjacent node in dir direction is a boundary node
//				adjNode = this.adjNodes.get(this.boundaryAdjNodeIDByDir[dir]);
//				//if(adjNode == null){gui.print2Cnsl("in node : " + this.ID + "null adj node in dir : " + dir + " bound adj node : " + this.boundaryAdjNodeIDByDir[dir] );}
//				double dirVComp;//directional component of velocity - takes the place of volume of water in sed cap calculation
//				switch (dir){
//					case N : {		dirVComp = this.velocity.z;		break;}
//					case S : {		dirVComp = -this.velocity.z;	break;}
//					case E : {		dirVComp = this.velocity.x;		break;}
//					case W : {		dirVComp = -this.velocity.x;	break;}								
//					default : {		dirVComp = 0;	gui.print2Cnsl("invalid direction : " + dir + " for node :" + this.ID);	break;}
//				}//for each direction
//				
//				this.latSedCapPipes[dir] = eng.calc2ndOrderLatSedCapacity(this, dirVComp, this.heightWater[1], true, delT) * this.getKw() * scaleFact;	
//				if(this.latSedCapPipes[dir] > 0){//if masswasting sed cap is positive
//					//latSedCap will be negative if velocity flowing away, causing deposition
//					calcK_Val = ((this.latSedCapPipes[dir] + calcCapSedDiff <= 0) ? (this.getKd()) :  (this.getKs()));
//						//if lat sed cap less than 0 then 	
//						//this.latSedCapPipes[dir] is negative if too much sediment in water(uses kd to deposit), positive if not enough (uses ks to lift up amount)
//					calcNewSed = calcK_Val * (this.latSedCapPipes[dir] + calcCapSedDiff );   //negative for deposition, positive for erosion
//						//change either height into sediment, or vice versa
//					if(calcNewSed > 0){		eng.convertHeightToSed(adjNode, this, calcNewSed,  calcNewSed, true/*useSedFlux*/, true);	} 
//					else {					eng.convertHeightToSed(this, this, calcNewSed, calcNewSed, true /*useSedFlux*/, false);}
//				}
//			}//if node in this direction is a boundary node
//		}//for each direction
//	}//calc2ndOrderErosionValsPipes	
	
  	/**
  	 * modifies source vertex's height (y/"up" dir) with new val
  	 * @param newVal new height to add to current heigh
  	 */
  	private void addHeightModVert(double newVal){	
  		if (!p.allFlagsSet(myConsts.multByNormal)){//using only y-direction heights
  			this.source.coords.y += newVal;
  		} else {
  			myVector tmpNorm = new myVector(0,0,0);
  			tmpNorm.set(this.source.coords);
  			tmpNorm._sub(this.source.baseCoords);
  			tmpNorm._normalize();
  			tmpNorm._mult(newVal);
  			//tmpNorm._normalize();    
  			//print2Cnsl("mod amt for torus : " + modAmt + " mod vector : " + tmpNorm.toString());
  			this.source.coords._add(tmpNorm);
  		}		
  	}//addHeightModVert
  	
  	/**
  	 * address the case where this node is built with "steel" bottom, meaning it will never erode below starting values
  	 * @param newVal value to deposit or pull up
  	 * @param massWasting whether or not this is called from the masswasting routine
  	 * @return the amount of terrain actually eroded/deposited
  	 */
  	private double addHeightSteelBottom(double newVal){
  		double maxVal = 0.0;
  		if ((newVal < 0) &&														//if the newVal value is negative (meaning we wish to take away altitude
			(newVal < this.originalHeight - this.source.coords.y)){				//trying to take away more than is allowed at this node to take away - swap order of original height and curr height to account for sign of newval
			maxVal =  this.originalHeight - this.source.coords.y;				//then maxval we can modify this height is the amount of altitude added to the original node
  		} else {	maxVal = newVal; 		}//not eroding below original height
  		return maxVal;
 		
  	}//addHeightSteelBottom
  	
  	/**
  	 *  takes the passed value and adds it to this node's height appropriately, from idx inIDX to idx outIDX
  	 */
  	public double addHeightByVal(double newVal){
  		double maxVal; 
  		//check range based on type of material we are using for terrain
  		if (this.useOriginalHeight) {	
  			maxVal = this.addHeightSteelBottom(newVal);}
  		else { 							maxVal = newVal;	} 				
  			//modify source vertex's height
  		this.addHeightModVert(maxVal);//modify source vertex
  		return maxVal;				//return the amount we actually modified the system, so that we can adjust sediment values appropriately
  	}//addHeightsByVal
  
  	/**
  	 *  old donors structure remembers who gave us water last cycle - if we try to give them water this cycle, we give them less
  	 *  functions to handle donor arraylist
  	 */
  	public void clearDonors(){ this.oldDonors.clear(); }  
  	public void addDonor(int id){ this.oldDonors.add(id); }  
  	public void copyDonors(ArrayList<Integer> passedOldDonors){ this.oldDonors = new ArrayList<Integer>(passedOldDonors);} 
  	public boolean checkDonors(int id){ return (this.oldDonors.contains(id)); }
  	public ArrayList<Integer> getOldDonors(){ return this.oldDonors; }
	
	/**
	 * add passed vector to existing velocity vector (for use in sed cap calc)
	 * @param newVel
	 */
	public void addVelocity(myVector newVel){this.velocity._add(newVel);}	  

  	/**
  	 *  draw this height node as a point on the screen
  	 */
  	public void drawMe(){
	  	//int scaledVal = this.calcScaledVal(p.globMinMaxIDX,0);
	  	float x = (float)this.source.coords.x;
  		float y = (float)this.source.coords.y;
  		float z = (float)this.source.coords.z;
  		p.pushStyle();
  		gui.setColorValStroke(myGui.gui_TerrainColor);
  		p.pushMatrix();
  			p.translate(x, y, z);  
  			p.scale(.01f,.01f,.01f);
  			p.box(1);
  		p.popMatrix();
  		p.popStyle();
  		if(this.heightWater[0]> 0){
	  		p.pushStyle();
	  		if(this.boundaryNode){  gui.setColorValStroke(myGui.gui_White);	  			}
	  		else {					gui.setWaterColorStroke(1);  		}//water stroke color
	  		p.pushMatrix();
	  			p.translate(x, y+(float)this.heightWater[0], z);  
	  			p.scale(.01f,.01f,.01f);
	  			p.box(1);
	  		p.popMatrix();
	  		p.popStyle();
  		}
 	}//drawMe
   
  	public myVertex getSource(){ return this.source; }       
  	
	//setters/getters for erosion - all use idx 0 of arrays for actual values
  	public void setFlux(int step, int dir, double val){ this.flux[step][dir] = val;}
 	

	public double getMatFEMval() {return matFEMval;}
	public void setMatFEMval(double matFEMval) {this.matFEMval = matFEMval;	}
	
  	public double getFemHeight(){ return Math.max(0.0,this.heightWater[0] - this.source.coords.y); }  
  	public double getHeightWater(){ return this.heightWater[0]; }  
   	public double getHeightWater(int idx){ return this.heightWater[idx]; }  
   	public double getSediment(){ return this.sediment[0]; }  
	public double getSediment(int idx){ return this.sediment[idx]; }
		//return sediment concentration
	public double getSedimentConcentration(int sedIDX, int h2oIDX){
		this.sedConcVis = (this.heightWater[h2oIDX] < myConsts.epsValLarge) ? 0 :  (this.sediment[sedIDX]/this.heightWater[h2oIDX]);
		return this.sedConcVis;}	
	//this will set the sediment actual value here by adding an amount of sediment equal to concentration x vol of water here
	public void setSedimentFromConcentration(int sedIDX, int h2oIDX, double conc){this.setSedimentPipes(this.heightWater[h2oIDX] * conc, sedIDX);}
	
   	public myVector getSedSource() {return sedSource;}
   	public int getSedTransIDX(){return this.sedTransportSrcIDX;}

   	public double getSedLeftOver(){return this.sedLeftOver;}
   	public double getSedTransported(){return this.setTransportedDebug;}
  	
   	public double[] getVolWaterAra(){ return this.heightWater;}
  	public double[] getSedimentAra(){ return this.sediment;}
  	public double[][] getFlux(){return this.flux;}
  	public double getFluxByDir(int step, int dir){return this.flux[step][dir];}
  	public double getDistByDir(int step, int dir){return this.distToNeighbors[step][dir];}
  	public double getAdjWaterShare(int dir){ return this.adjWaterShare[dir]; }
  	public double getPipeLength(int idx){ return this.pipeLengths.get(idx);}//idx is id of adj node
  	
  	public double getOriginalHeight(){return this.originalHeight;}
  	public boolean isUseOriginalHeight(){return this.useOriginalHeight;}
  	
  	public double getAdjHeightDiffsByDir(int dir) {		int keyVal = this.adjNodeIDByDir[dir]; 	return this.adjHeightDiffs.get(keyVal);}
	public double getAdjHeightDiffsTerByDir(int dir) {	int keyVal = this.adjNodeIDByDir[dir];		return this.adjHeightDiffsTer.get(keyVal);}
	
  	public double getAdjHeightDiffsByIDX(int _idx) {	return this.adjHeightDiffs.get(_idx);}
	public double getAdjHeightDiffsTerByIDX(int _idx) {		return this.adjHeightDiffsTer.get(_idx);}
	
	public myVector getFluxForce(){	return this.fluxForce;}
	
		//get index of adj node to particular direction
	public int getAdjNodeIDByPassedDir(int dir) { return this.adjNodeIDByDir[dir];} 
		//get direction of adj node with particular index
	public int getAdjNodeDirByPassedIdx(int idx) { return this.adjNodeDirByID.get(idx);}
	
		//get index of boundary adj node to particular direction
	public int getBoundaryAdjNodeIdxByPassedDir(int _dir) { return this.boundaryAdjNodeIDByDir[_dir];} 
		//get direction of boundary adj node with particular index
	public int getBoundaryAdjNodeDirByPassedID(int _id) { return this.boundaryAdjNodeDirByID.get(_id);}
	
	public myVector getVelocity(){ return this.velocity; }
  	
  	public double getU(int idx) {return this.velocity.x;}
	public double getV(int idx) {return this.velocity.z;}

  	public HashMap<Integer, Integer> getAdjNodeDirByID(){return this.adjNodeDirByID;}
  	public HashMap<Integer, Integer> getBoundaryAdjNodeDirByID() {return boundaryAdjNodeDirByID;}

  	public HashMap<Integer, Double> getPipeLengths() {return this.pipeLengths;}
  	
  	public boolean isBoundaryNode(){return this.boundaryNode;}  	
  	public boolean isMirrorNode(int _idx){return this.isMirrorNode[_idx];}
  	
  	//returns the id of the node this node wraps around to
  	public int getMirrorNodeID(int _idx){
  		if(this.source.xIDX == 0) {	return eng.heightMapAra[eng.heightMapAra.length-1][this.source.zIDX].ID;}		//west wrap around, from 0 idx to max idx
  		else {						return eng.heightMapAra[0][this.source.zIDX].ID;}								//east wrap around, from max idx to 0 idx
  	}  	
  	//returns the direction the wrap-around takes place from this node in
  	public int getMirrorNodeDir(int _idx){	if(this.source.xIDX == 0) {	return myHeightNode.W;}	else {	return myHeightNode.E;}	}
  	//this is the initial height difference between two wrap-around nodes, as if they were adjacent instead of on opposite sides of the mesh from each other
  	//negative value means this node is lower than its wrap around companion
	public double getAvgNeighborHeightDiff(int _idx) {return avgNeighborHeightDiff[_idx];}

	public void setAvgNeighborHeightDiff(double _avgNeighborHeightDiff, int _idx) {this.avgNeighborHeightDiff[_idx] = _avgNeighborHeightDiff;}
	public HashMap<Integer,myHeightNode> getAdjNodes(){return this.adjNodes;}
	public int getBoundaryAdjNodeDirByID(int _id) {return this.boundaryAdjNodeDirByID.get(_id);}
	
	public myHeightNode getBoundaryAdjNodesByPassedDir(int _dir){	return this.adjNodes.get(this.boundaryAdjNodeIDByDir[_dir]);}
 	public myHeightNode getAdjNodesByPassedDir(int _dir){			return this.adjNodes.get(this.adjNodeIDByDir[_dir]);  	} 	
 	
	public void setPipeLengthsByID(int _ID, double _len){ this.pipeLengths.put(_ID, _len);}
	public void setAdjHeightDiffsByID(int _ID, double _high){this.adjHeightDiffs.put(_ID, _high);}
	public void setAdjHeightDiffsTerByID(int _ID, double _heightTerrain){this.adjHeightDiffsTer.put(_ID, _heightTerrain);}

  	public int getScaledVal(int idx){return this.scaledVal[idx];}
  	public int[] getScaledValAra(){return this.scaledVal.clone();}
   	public boolean isNodeJustMade() {return this.isNodeJustMade;}
	public boolean isUseLocalErosionConstants(int idx) {return useLocalErosionConstants[idx];}


	public void setNodeJustMade(boolean _isNodeJustMade) {this.isNodeJustMade = _isNodeJustMade;}
		//pipes related k values for this node - usually false and 1, respectively.  only non-1 kval if true kscalednode
	public boolean iskScaledNode() {return this.kScaledNode;	}
	public double getkVal() {	return this.kVal;}
	public double getSedConcVis() {	return sedConcVis;}

	public void setSedConcVis(double sedConcVis) {	this.sedConcVis = sedConcVis;}

	public void setkScaledNode(boolean _kScaledNode) {this.kScaledNode = _kScaledNode;}
	public void setkVal(double _kVal) {this.kVal = _kVal;}
	public void setSedSource(myVector _sedSource) {this.sedSource = _sedSource;}
	public void setSedTransportSrcIDX(int _srcIDX){this.sedTransportSrcIDX = _srcIDX;}
	public void setSedLeftOver(double _sedVal){this.sedLeftOver = _sedVal;}
	
	public void addSedTransported(double _sedTrans){this.setTransportedDebug += _sedTrans;}
	public void setSedTransported(double _sedTrans){this.setTransportedDebug = _sedTrans;}
	
  	public int getID() {return this.ID;}

	public String toString(){
  		String result = "Height node ID : " + this.ID + " corresponding to vertex : " + source.toString();
  		result += "Height : " + this.source.coords.y + " Vol H2O : " + this.heightWater[0] + " Sediment in H2O : " + this.sediment[0];  
  		result += "\nPipes : \n";
  		for (Integer IDX : this.pipeLengths.keySet()){
  			result += "\tlength of pipe to Node ID : " + IDX + " = " + pipeLengths.get(IDX) + "\n";
  		}
  		return result;  
  	}
  
}//myheightnode class
