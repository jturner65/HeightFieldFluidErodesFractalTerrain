package sqSqErosionSimPkg;

import processing.core.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * class that holds all routines for fluid transport and erosion
 * @author John
 *
 */
public class mySqSqEroder{
	private sqSqErosionSimGlobal p;
	private mySqSqSubdivider sub;
	private myGui gui;
	private myFEMSolver mat;
	
	//partial clones of the current vertex list of the entire structure - maintained for erosion calculations that rely on previous time-step
	//values of vertexes
	public HashMap<Integer,myHeightNode> oldCloneHeightNodeList;
	//array of node clones
	//public myHeightNode[][] oldCloneHeightNodeAra;

	//list of displayed fluxes and water volumes for a set of nodes, indexed by x position
	public HashMap<Double, myDebugObj> debugFluxVolVals;
	//debug variable holding offset into debug structure
	public final double DebugHashOffset = 10000;
	
	//2d array of per-node velocities after single bfecc step (back and then forward
	public myVector[][] velBar;

	//structure idxed by vert id holding array deques of height nodes for heightmap - hack - 
	//array deque intended to be able to hold vertical nodes sharing same x-z coords
	public HashMap<Integer, ArrayDeque<myHeightNode>> heightMap;	
	
//	//tmp debug vars
//	private double minTval = Double.MAX_VALUE, maxTval = Double.MIN_VALUE;
	
	//hashmap holding x,z coords of every rain-on-me node of mesh, for boat adding
	private HashMap<Integer, Integer[]> wetNodeCoords;
	
	//replacement for heightMap - 2 d array of nodes for O(1) sequential access
	public myHeightNode[][] heightMapAra;
	//array of copy of height data, to be used to compare two meshes graphically
	public myVector[][] oldHeightMapAraData;
	//comparison of old height map data with current height map
	public myVector[][] compareHeightMapAraData;
		
	//x-idxed hashmap of hashmaps, idxed by z value, of ArrayDeques of myHeightNode. 
	//the intent is to build a structure than can be used similar to a 2d array based on x-y-z location
	private HashMap<Double, HashMap<Double, ArrayDeque<myHeightNode>>> ara2DHeightMap;
	//private Double[] xCoords, zCoords;
	
	//gravity multiplier to modify velocity - velocity is proportional to sqrt of grav : v = sqrt(2*g *h) where g is grav and h is height of column of water
	public double gravMult = 1;
	//global material type for terrain - 0 for no erosion processes, can be overridden for multiple types of material in myHeightNode
	public int globMatType = 0;
	//effective volume/height of rainwater per rain drop - approx 1/1000th the height of the vertex 
	public double globRaindropMult = .001;
	//scaling factor for transport mechanism of water between adjacent nodes in first order musgrave-inspired water transport
	public double globScaleMult = .24;
	//scaling factor for aeolian erosion to maintain consistency with fluvial - strength of effect from aeolian erosion process
	public double globAeolErosionMult = .24;
	//scaling factor for aeolian erosion to maintain consistency with fluvial - strength of effect from aeolian erosion process
	public double globLatErosionMult = 1;
	//constant value array holding array of constants used by first order hydraulic erosion - currently holds 4 values
	//implemented as array to handle multiple layers/types of materials
	public final int Kc = 0, Kd = 1, Kw = 2, Ks = 3;	//constants defining the idx of particular multipliers in the material type array
	public final int numMatVals = 4;					//number of erosion multiplier values in matTypeAra
	public double[][] matTypeAra = {
			{ 0,  0.35,  0.35,  0.35,  0.35,  0.35,  0.035, 0.035, 0.035, 0.035, 0.035},			//kc						
			{ 0,  0.25,  0.25,  0.35,  0.35,  0.35,  0.25,  0.25,  0.25,  0.35,  0.25},			//kd	
			{ 0,  0.45,  0.35,  0.45,  0.35,  0.25,  0.25,  0.15,  0.5,   0.35,  0.25},			//kw
			{ 0,  0.05,  0.15,  0.05,  0.15,  0.15,  0.25,  0.15,  0.15,  0.15,  0.25}};		//ks
	                                                         
	//used for first order thermal weathering calculation - angle above which material moves to lower altitude neighbors
	private static final double GlobTalusAngle = 0;//PI/6.0;  
	//used to determine how much material should be moved for thermal weathering calculation - magic number encoding force value
	public static final double Talus_C = 1; 
	//used to determine how much material should be removed from river banks due to massWasting
	public static final double  MassWasting_C = 1; 
	//angle describing possible area of random motion around advection angle for sediment deposit
	public static final double sedDepAdvectAngle = Math.PI/12.0;
	//incoming wind angle - 0 is positive x axis
	public double globWindAngle = 0;	
	//lowest k (scaling factor for flux cacls) found for any particular flux calculation, to be applied globally if flags[useLowestKGlobally] is true
	public double globalKVal = 1;
	
	public final int numRainStormCycles = 20;
	//number of cycles for complete rain cycle to occur - rain fall will drop chosen raindrop size on every vert over rainstormscycles # of cycles
	public int rainStormCycles = 0;
		
	//defining local consts to make ara access easier
	public final int diffxIDX = 0, diffzIDX = 1, newXIDX = 2, newZIDX = 3;
	public final int lowXlowZ = 0, lowXhighZ = 1, highXlowZ = 2, highXhighZ = 3;
	//ratio of total number of mesh verts to use as maximum amount of rain to pour on mesh if rainfall is enabled
	public double rainVertMult = 1.0;
	

	//hashmap to hold sediment values for FAST paper implementation of sediment transport, using a stam-y mechanism and an array-like implementation
	//public HashMap<Double[], Double> sedTransportVals;	
	
	public mySqSqEroder(sqSqErosionSimGlobal _p, myGui _g, myFEMSolver _mat){
		this.p = _p;
		this.gui = _g;
		this.mat = _mat;
		this.oldCloneHeightNodeList = new HashMap<Integer,myHeightNode>();  
		this.heightMap = null;//make null to rebuild heightmap if selected
		this.heightMapAra = null;
		this.ara2DHeightMap = null;
		p.setFlags(myConsts.heightMapMade, this.heightMap != null);
		this.debugFluxVolVals = new HashMap<Double, myDebugObj>();
		this.wetNodeCoords = new HashMap<Integer,Integer[]>();
		this.oldHeightMapAraData = null;
		this.compareHeightMapAraData = null;
	}//constructor
	
	public void setFEMSolver( myFEMSolver _mat){this.mat = _mat;}	
	public void setGui(myGui _g){ this.gui = _g; }	
	public void setParent(sqSqErosionSimGlobal _p){ this.p = _p;}
	public void setSubdivider(mySqSqSubdivider _s){ this.sub = _s;}

	/**
	*  global functions and variables for first order erosion calculations
	*/
	
	/**
	 * initialize engine values
	 */
	public void initEngVars(){
		this.globWindAngle = 0;
		this.globalKVal = 1;
		this.gravMult = 1;
		this.oldCloneHeightNodeList = new HashMap<Integer,myHeightNode>();  
		//this.oldCloneHeightNodeAra = null;
		this.heightMap = null;//make null to rebuild heightmap if selected
		this.heightMapAra = null;
		this.ara2DHeightMap = null;
		this.debugFluxVolVals = new HashMap<Double, myDebugObj>();
	}
	
	/**
	*  set up to run sim
	*/
	public void launchSim(){
		p.simCycles = 0;
		for (int i =0; i < myGui.numNotifyLights; ++i){ gui.lightCounter[i] = myGui.lightCountCycles; }  
		if (!p.allFlagsSet(myConsts.heightMapMade)){
			p.setFlags(myConsts.heightProc,true);
			p.setFlags(myConsts.heightProc,false);
		}
		runSim();
	}//launchFirstOrderSim() function

	/**
	*  run a simulation of the firstOrder or 2nd order algorithms
	*/
	public void runSim(){
		if((p.allFlagsSet(myConsts.staggerRainFall)) && (p.allFlagsSet(myConsts.Raincalc)) && (this.rainStormCycles > 0) ){	
			this.rainStormCycles--;	
			//gui.print2Cnsl("rain storm cycles : " + this.rainStormCycles);
			if(p.globTotValAra[myConsts.H2O] > this.heightMapAra.length * this.heightMapAra.length * this.rainVertMult){
				gui.print2Cnsl("rain vert mult set rain off : " + this.rainVertMult);
				p.setFlags(myConsts.Raincalc, false);}//turn off raincalc if amt rain > rainVertMult *  total # verts	
			p.pickCorrectErosion("Rain");	
		}//if using rainstorm to drop rain
		else if ((p.allFlagsSet(myConsts.Raincalc)) && (p.simRainCycles !=0) && ((p.simCycles % p.simRainCycles) == 0)){
			if(p.allFlagsSet(myConsts.staggerRainFall)){		this.rainStormCycles = numRainStormCycles;	}//trigger staggered rainfall "storm"			
			if(p.globTotValAra[myConsts.H2O] > this.heightMapAra.length * this.heightMapAra.length * this.rainVertMult){p.setFlags(myConsts.Raincalc, false);}//turn off raincalc if amt rain > rainVertMult *  total # verts	
			p.pickCorrectErosion("Rain");	
		}//dropping rain all at once
		if ((p.allFlagsSet(myConsts.HEcalc)) && (p.simHECycles !=0) && ((p.simCycles % p.simHECycles) == 0)){ 	p.pickCorrectErosion("HydraulicErosion");	}
		if ((p.allFlagsSet(myConsts.TWcalc)) && (p.simTWCycles !=0) && ((p.simCycles % p.simTWCycles) == 0)){	p.pickCorrectErosion("ThermalWeathering");}
		gui.decrNotifyLights();
		p.simCycles++;	  
	}//runFirstOrderSim function

	/**
	*  find the amount of water to be passed from a particular node to its neighbor based - this is part of a 1st order diffusion sim
	*  very loosely on the musgrave algorithm for hydraulic erosion.  named "del w" in musgrave lit,
	*  defined as min(h20@V, (h20 + height)@V - (h20+height)@U, where V and U are 
	*  the vertex in question and one of its neighbors, respectively
	*
	*  if this value is less than 0 then some amount of sediment suspended in this water should be deposited
	*  @param the vertex in question - aV in musgrave lit
	*  @param the vertex's neighbor - aU in musgrave lit
	*  @param the possible ration of water for this adj node, based on total water at aV divided by ration of height diff of aU-aV over total
	*          height diff of aV to all adjacent verts
	*  @return the amount of water to be passed - may be negative
	*/
	public double calc1stOrderPassedH20(myHeightNode aV, myHeightNode aU, double h2oRation){
		double calcDiff = calcNodeHeightDiffIncH2O(aV, aU, 0); 
		if (calcDiff < h2oRation ) { return calcDiff;}
		return h2oRation;        
	}//calcPassedH20 function

	/**
	*  deposit some sediment currently held in water at node to node itself (increase its height, based on global constant)
	*/
	public double depositSiltAtNode(myHeightNode tmpNode, double sedAmt, double Kconst){
		if ( Kconst > 1) { gui.print2Cnsl("Error : depositing too much sedmient due to Kconst : " + Kconst + " being too high for node : " + tmpNode.toString());}
		double modAmt = Kconst * sedAmt;
		tmpNode.addHeightByVal(modAmt);
		return modAmt;
	}//depositSiltHydraulicErosion func

	/**
	*  displays a message if the src node and its clone don't have the same amt of water
	*/
	public void check1stOrderNodeH2O(myHeightNode srcHNode, myHeightNode srcHNodeOld){
		if (Math.abs(srcHNode.getHeightWater() - srcHNodeOld.getHeightWater()) > myConsts.epsValCalc){ 
			gui.print2Cnsl("1st order h2o isn't consistent between srcnode : " + srcHNode.ID + " | " + srcHNode.getHeightWater() + " and clone : " + srcHNodeOld.ID + " | " + srcHNodeOld.getHeightWater());   
		}
	}//checkNodeH2O method

	/**
	*  simulates rain on every height node for first order hydraulic erosion
	*/
	public void rain1stOrder(double delT){
		if (this.heightMap != null){
			int cloneVertDotID;
			double heightMod = 0.0;
			ArrayDeque<myHeightNode> tmpDeck;
			HashMap<Integer, myHeightNode> tmpOldCloneHeightNodeList = new HashMap<Integer, myHeightNode>(); 
			HashMap<Integer, ArrayDeque<myHeightNode>> tmpHeightMap = new HashMap<Integer, ArrayDeque<myHeightNode>>();
			myHeightNode srcHNodeOld, putResultVert;
			//initialize global min/max for h2o
			//resetGMinMaxAra(H2O);
			//if (!p.flags[myConsts.simulate]){gui.print2Cnsl("Rain on terrain");}
			for (Integer IDX : this.heightMap.keySet()){                  //get each key and value from height map -keys are unique ints but not necessarily sequential
				tmpDeck = new ArrayDeque<myHeightNode>();         //make new deque to put modfied nodes in
	      
				for (myHeightNode srcHNode : this.heightMap.get(IDX)){                           //get nodes from old deque at heightmap at position idx - prevent concurrent mod
					cloneVertDotID = calcCloneID(srcHNode.source.ID);        
					srcHNodeOld = this.oldCloneHeightNodeList.get(cloneVertDotID);  //get clone node
					if (srcHNodeOld == null) { gui.print2Cnsl("error - no old/clone height node with id : " + cloneVertDotID);}        
					
					srcHNode.addRain(heightMod, rainMult(), delT, false);        //add rain to each node
					srcHNodeOld.addRain(heightMod, rainMult(), delT, true);
	 
					setHNode(tmpDeck, srcHNode);                                                               //puts new node in temp deque
					putResultVert = tmpOldCloneHeightNodeList.put(cloneVertDotID, srcHNodeOld); //put copy of newly calculated rained-on node in rebuilt oldclonevertlist, for next iteration
					if(putResultVert == null){gui.print2Cnsl("");}
					check1stOrderNodeH2O ( srcHNode, srcHNodeOld);  
				}//for each node in deque
				tmpHeightMap.put(IDX, tmpDeck);  
			}//for each k-v pair in heightmap
			//need to modify verts in tmpOldCloneVertList to reflect newly calculated heights
			this.oldCloneHeightNodeList = tmpOldCloneHeightNodeList; 
			this.heightMap = tmpHeightMap;
		}//if not null heightmap  
	}//rain1stOrder func

	/**
	*  calculates the talus angle between two vertices, in radians
	*  @param  primary vertex - aV in literature
	*  @param  a neighboring vertex - aU in literature
	*/
	public double calcTalusAngle(myVertex aV, myVertex aU){
		double result = 0.0; 
		double heightDiff = 0.0; 
		double distaVaU = myVector._dist(aV.coords,aU.coords);      //euclidean distance between 2 points - hypotenuse between two terrain nodes
		if (distaVaU < 0){//negative distance is bad mmkay
			return 0.0;
		}else{
			heightDiff = calcHeightDiff(aV, aU);
			result = Math.asin(heightDiff/distaVaU);   
		}//if dist not <0
		return result;
	}//calcTalusAngle method

	/**
	*  calculate the angle between these nodes solely in x/z plane, to determine amount affected by wind or lateral erosion
	*/
	public double calcAngleDiff(myVertex aV, myVertex aU){
		if (aV.coords.x == aU.coords.x) {
			if (aV.coords.z > aU.coords.z) { return 3*PApplet.PI/2.0; }//neighbor to south
			else                           { return PApplet.PI/2.0;}   //neighbor to north
		} else {
			if (aV.coords.x > aU.coords.x) { return PApplet.PI; } 		//neighbor to west
			else                           { return 0; }				//neighbor to east
		}
	}//funcCalcWindAngle	
	
	/**
	*  find angle between nodes based on direction from source to adjancent
	*/
	public double calcAngleDiffByDir(int dir){
		switch (dir){
			case myHeightNode.S : { return 3*Math.PI/2.0; }//neighbor to south
			case myHeightNode.N : { return Math.PI/2.0;}   //neighbor to north
			case myHeightNode.W : { return Math.PI; } 		//neighbor to west
			case myHeightNode.E : { return 0; }				//neighbor to east
		}
		return -1;
	}//funcCalcWindAngle
	
	/**
	*  handles getting wind direction input for thermal weathering
	*/
	public void handleThermalChoice(boolean butPress, boolean firstPressed){
		if (p.allFlagsSet(myConsts.erosionProc)){ 
			double xMod = (p.mapD(p.mouseX-p.pmouseX,-100,100,-2,2)), yMod = (p.mapD(p.mouseY-p.pmouseY,-100,100,-2,2));
			double ModModVal = ((Math.max(xMod + yMod, xMod - yMod) > Math.abs(Math.min(xMod + yMod, xMod - yMod))) ? (Math.max(xMod + yMod, xMod - yMod)) : (Math.min(xMod + yMod, xMod - yMod)) );
			//double ModModVal = Math.max(Math.max(xMod + yMod, xMod - yMod), Math.abs(Math.min(xMod + yMod, xMod - yMod)));
			double ModVal = ((Math.max(xMod, yMod) > Math.abs(Math.min(xMod, yMod))) ? (Math.max(xMod, yMod)) : (Math.min(xMod, yMod)));
			ModVal += ModModVal;
			globWindAngle = (globWindAngle + ModVal) % (2*PApplet.PI);
			globWindAngle = ((globWindAngle < 0) ? (globWindAngle + ( 2 * PApplet.PI)) : globWindAngle);
			if (!p.allFlagsSet(myConsts.simulate) || (butPress && (p.simTWCycles == 0))){		p.dispWindArrow();   } //displays big red arrow over terrain showing direction of wind for erosion, when not already simulating (dont' show 2x)
			//want erosion calculation to happen when button is held down, or number key is held down, at a rate determined by simTWCycles, regardless of sim state
			if (firstPressed || (butPress && p.allFlagsSet(myConsts.simulate)) || ((p.simTWCycles != 0) && (p.clickSimCycles % p.simTWCycles == 0 ))){
				p.pickCorrectErosion("ThermalWeathering");				
			}//if flags sufficient to calc erosion (only when not simulating and key first pressed or cycles
			p.clickSimCycles++;
		}//
	}//handleThermalInput func		
	
	/**
	*  process erosion calculation on passed node based on musgrave thermal weathering algorithm
	*
	*  srcHeightNodeOld : time t (old) height node, for reading info but not modified
	*  tmpNode : time t+1 (new) height node, receives new value but isn't read
	*
	*  @param tmpNode is the node to erode
	*  @param srcVertOld is the clone of the vertex that has the original height and adjacent vert heights stored in it
	*  @param windAngle is angle wind is coming from
	*  @param delT is incremental time step multiplier
	*/
	public void calc1stOrderAeolianErosion(myHeightNode srcHNode, myHeightNode srcHNodeOld, double windAngle, double delT){//use old src vert for height and adjcent vert height info, but modify originals
		double totModAmt = 0.0, calcTAngle, calcWAngle, TalusAngle, wAngleModRatio, locModAmt;     //total amount taken away from this node
		double angleDiff;                                                                          //angle difference between the incoming wind and the orientation of 2 verts
		myVertex srcVert = srcHNode.source;                                                        //this is what will be modified, but not read - time t+1
		myVertex srcVertOld = srcHNodeOld.source;  
		//myVertex adjVert;
		myHeightNode adjHNode;
//		  myHeightNode adjHNodeOld;
		p.resetGMinMaxAra(myConsts.SED);
		p.resetGMinMaxAra(myConsts.COORD_Y);
		//this will be read, but not modified - time t
		for (myVertex adjVertOld : srcVertOld.adjVerts){
			//adjVert = srcVert.getAdjVertByID(calcCloneID(adjVertOld.ID));                  //this is what will be modified, but not read - time t+1
			adjHNode = getHNode(this.heightMap.get(calcCloneID(adjVertOld.ID)));                //get the heightnode corresponding to a particular vert ID for a current adj vert
//		    adjHNodeOld = this.oldCloneHeightNodeList.get(adjVertOld.ID);                       //get the heightnode corresponding to a particular vert ID for an old clone vert
	    
			calcTAngle = calcTalusAngle(srcVertOld,adjVertOld);                            //read from old values
			calcWAngle = calcAngleDiff(srcVertOld,adjVertOld);
			angleDiff = Math.abs(calcWAngle - windAngle);                                       //find the angle from the adj node to src node vector to the incident wind - can use calculated dir for adj node
	    
			if (angleDiff > Math.PI) { angleDiff = -1*((2*Math.PI) - angleDiff); }                   //normalize to be between PI and -PI
			wAngleModRatio = Math.cos(angleDiff);
			wAngleModRatio = ((wAngleModRatio > 0) ? wAngleModRatio : 0);    //wind behind the node does nothing
//	 	    gui.print2Cnsl("src " + srcVertOld.coords + " | adj " + adjHNodeOld.source.coords + " \n\t wind angle : " + windAngle + " vertAngle " + calcWAngle + " angle mod " + wAngleModRatio + " angle difference " + angleDiff);
			TalusAngle = mySqSqEroder.GlobTalusAngle * randTalusAngleMult();
			if (calcTAngle > TalusAngle){                                                  //difference between src and neighbor is sufficient for erosion
				locModAmt = (mySqSqEroder.Talus_C * (calcTAngle - TalusAngle) * delT) * wAngleModRatio * this.globAeolErosionMult;     //delT is timestep multiplier for sim 
				adjHNode.addHeightByVal(locModAmt);//reCalcHeightOfVert(adjVert,locModAmt);                                               //modify actual adj vertex to gain some small amount of either height or water, or both
				//adjHNode.calcHeight(0);        //recalculate height of node
				totModAmt += locModAmt;
			}//if calculated angle > modified talus angle    
		}//for each adjacent vert to srcVert
		//remove from source vert amount moved to neighbors
		reCalcHeightOfVert(srcVert, -1 * totModAmt);                                             //modify actual src vertex to loose amount equal to all gains
		//srcHNode.calcHeight(0);
	}//calc1stOrderAeolianErosion method
	
	/**
	*  calculates sediment capacity of deltaW - amount of sediment to be removed from aV.sed @ t and given to aU.sed @ t+1
	*  @param the amount of water being considered for moving
	*  @return the amount to move
	*/
	public double calcCsDelW(double delW, double KcVal){  return KcVal * delW; }

	/**
	*  calculate the rain-related erosion via position based hydraulic erosion mechanism for a single iteration
	*    -first, we rain on every vertex and calculate how much silt at that vertex will become loose and mix with the rain there
	*    -next, recalculate height nodes' sediment, water-content and height, based on neighbors
	*
	*  srcHeightNodeOld : time t (old) height node, for reading info but not modified
	*  tmpNode : time t+1 (new) height node, receives new value but isn't read
	*
	*  process erosion calculation on passed node based on musgrave hydraulic/fluvial algorithm
	*  @param tmpNode is the node to erode
	*  @param srcVertOld is the clone of the vertex that has the original height and adjacent vert heights stored in it
	*  @param order is a chooser for either first or 2nd order equation (position based or pipes/velocity based)
	*  @param delT is the time step multiplier
	*/
	public void calc1stOrderFluvialBase(myHeightNode srcHNode, myHeightNode srcHNodeOld, double epsVal, double delT){//use old src vert for height and adjcent vert height info, but modify originals

		double srcNodeH2O = srcHNodeOld.getHeightWater();      //amt of water at this node to start with
		if (srcNodeH2O > epsVal){                          //if there's water at the srcHNodeOld - if not, no need to do this
		    //srcNode -related variables : tot amt taken away from this node; tot hght diff between this node and neighbors beneath it; 
		    //tot hght diff between this node and neigh above it; total height differences - abs val of above and below
		    //tot sedamt on src node, amount that is staying, amount that is going, variable used as passed value
		    //amt of water to be passed to lower nodes, amt of water to stay at this node this round
			double totHeightDiffPos = 0.0f,  totHeightDiffNeg = 0.0f, totHeightDiff = 0.0f;  
//		    //type of material at src node - to be used to access particular constants arrays (k arrays)
//		    int matType = srcHNodeOld.getMatType();

		    //adjHNode to be modified, adjHNodeOld to be read    
		    myHeightNode adjHNode, adjHNodeOld;
		    double heightDiff = 0;
		    HashMap<Integer,Double> heightDiffNeighborVals         = new HashMap<Integer,Double>();             //height differences for all old neighbors                                                                                     
		    HashMap<Integer,myHeightNode> heightDiffNeighbors     = new HashMap<Integer,myHeightNode>();      //all neighbor height nodes : t+1 version (to write to)
		    HashMap<Integer,myHeightNode> heightDiffNeighborsOld  = new HashMap<Integer,myHeightNode>();      //all neighbor height nodes : t version (to read from)
	    
		    //first calculate the different heights between the src node and 
		    //all its neighbors
		    //srcHNodeOld.calcHeight(0);                                                                        //make sure this node's height is accurate
		    for (myVertex adjVertOld : srcHNodeOld.getSource().adjVerts){                                     //adjacent verts to old vert - read but not modified : time t
		    	adjHNode = getHNode(this.heightMap.get(calcCloneID(adjVertOld.getID())));                     //get the heightnode corresponding to a particular vert ID for a current adj vert
		    	adjHNodeOld = this.oldCloneHeightNodeList.get(adjVertOld.getID());                            //get the heightnode corresponding to a particular vert ID for an old clone vert
		    	//adjHNodeOld.calcHeight(0);                                                                    //make sure this node's height is accurate
	
		    	heightDiff = calcNodeHeightDiffIncH2O(srcHNodeOld, adjHNodeOld, 0);                                   //calculate the difference in altitude between the two nodes
		    	heightDiffNeighbors.put(adjHNode.ID, adjHNode);                                                 //t+1 node for receiving modifications
		    	heightDiffNeighborsOld.put(adjHNode.ID, adjHNodeOld);                                           //t version of adjacent height node
		    	heightDiffNeighborVals.put(adjHNode.ID, heightDiff);                                            //get height differences based on old height values (time t)
		    	if (heightDiff > 0) { totHeightDiffPos += heightDiff; }                                         //total heightdifference between this node and all those beneath it
		    	if (heightDiff < 0) { totHeightDiffNeg += heightDiff; }                                         //total heightdifference between this node and all those above it
	      
		    }// for each adjacent vertex

		    totHeightDiff = Math.abs(totHeightDiffNeg) + totHeightDiffPos;
		    //if no height differences between this node (alt + h2o) and its neighbors (alt + h2o) then exit
		    if (Math.abs(totHeightDiff) < epsVal){
		    	//gui.print2Cnsl("no height differences between node : " + srcHNode.toString() + "\nand its neighbors"); 
		    	return;}   
		    calc1stOrderFluvialDetail(srcHNode, srcHNodeOld, delT, totHeightDiffPos,  totHeightDiffNeg , totHeightDiff, heightDiffNeighborVals, heightDiffNeighbors, heightDiffNeighborsOld);
		}//if water at this src node
	}//calc1stOrderFluvialBase method


	/**
	*  calculate fluid and sediment movement based on a positional model
	*/
	public void calc1stOrderFluvialDetail(myHeightNode srcHNode, myHeightNode srcHNodeOld, 
	                        double delT, double totHeightDiffPos,  double totHeightDiffNeg , double totHeightDiff,
	                        HashMap<Integer,Double> heightDiffNeighborVals,                                                                                            
	                        HashMap<Integer,myHeightNode> heightDiffNeighbors,   
	                        HashMap<Integer,myHeightNode> heightDiffNeighborsOld){
	                          
		double srcNodeSedAmt = srcHNodeOld.getSediment(), srcNodeH2O = srcHNodeOld.getHeightWater(), sedAmtStay = 0.0f, sedAmtGo = 0.0f, sedAmt = 0.0f;
		double heightDiff = 0.0f, ratioPos, deliverH2O, deliverSed,  sedCalc, sedResult;                                                                          
		//double ratio, calcDiff, totModAmt = 0.0f, h2oAmtStay = 0.0f, h2oAmtGo = 0.0f, sedCapacity = srcNodeH2O * srcHNodeOld.lclKc; 
	  
		double cS, multiplier, delW = 0.0f;                                                                                 //placeholder delW to hold calculated value for one neighbor
		myHeightNode adjHNode, adjHNodeOld;
	  
		//1st, need to calculate amount of sediment in water at srcHNold - if there's too much sediment in the water, based on the water's
		//ability to transport sediment, then the over-amt of sediment should settle on the node. 
	   
	  
		if (srcNodeSedAmt > myConsts.epsValCalc){
			sedAmtStay = srcNodeSedAmt * (totHeightDiffNeg / totHeightDiff);
			sedAmtGo = srcNodeSedAmt * (totHeightDiffPos / totHeightDiff);
		}
	  
//		h2oAmtStay = srcNodeH2O * (totHeightDiffNeg / totHeightDiff);
//		h2oAmtGo = srcNodeH2O * (totHeightDiffPos / totHeightDiff);
	  
		//now go through each adjacent height node and calculate whether it should get a particular amount of water or not
		//modify t+1 vals (tmpHNode, adjHNode) and read from t vals (srcOldNode, adjHNodeOld
		for (Integer IDX : heightDiffNeighborVals.keySet()){//idx's are obj id's - not sequential - this gives each adjHNode to src hnode
			heightDiff = heightDiffNeighborVals.get(IDX);
			adjHNode = heightDiffNeighbors.get(IDX);
			adjHNodeOld = heightDiffNeighborsOld.get(IDX);
			//gui.print2Cnsl("height diff : " + heightDiff + " for nodes : " +srcHNode.ID + " and adj : " + adjHNode.ID);
			if(heightDiff > 0){                                              //this node is above adjacent node - move some amount of water and sediment from this node to neighbordeposit some amount of this node's sediment here on itself
				ratioPos = heightDiff/totHeightDiffPos;                 //+/- amount of material/water each adj impacts based on it's height diff w/src
//				ratio = heightDiff/totHeightDiff;                        //ratio of water - all water will leave node 
	      
				deliverH2O = ratioPos * srcNodeH2O;
	      
				delW = calc1stOrderPassedH20(srcHNodeOld, adjHNodeOld, deliverH2O);
				if (delW < 0){
					gui.print2Cnsl("\t---------------delW < 0--------------- oldSRC : " + srcHNodeOld.toString() + " oldAdj : " + adjHNodeOld.toString());

				} else {
					//fix for instability in water delivery at high delta t's
					multiplier = ((srcHNodeOld.checkDonors(adjHNode.getID())) ? this.globScaleMult : this.globScaleMult);              //globScaleMult;   //scale down amount of water being delivered to prevent oscillation
					deliverH2O = delW * multiplier;
					sedAmtGo *= multiplier;                                        //scale down amount of water being delivered to prevent oscillation
					deliverSed = ratioPos * sedAmtGo;
//	   	 	        deliverH2O = delW * globScaleMult;                             
					cS = calcCsDelW(deliverH2O, srcHNode.lclKc);                          	//sediment capacity - necessary to determine if some altitude here will leave
	                                                                       			//based on how much sediment the water -can- carry compared to how much is available to carry          
					srcHNode.addWater(-1 * deliverH2O * delT, 0);                //remove portion of this node's water from node - only delT ratio for simulation
					adjHNode.addWater(deliverH2O * delT, 0);                     //add portion of this node's water to adj node, based on ratio of height diff - delT for sim
					adjHNode.addDonor(srcHNode.getID());
					sedAmt = (deliverSed - cS)  ;                                  //amt of sediment to be converted to altitude
	        
					if (sedAmt >= 0){                                               //expecting to deliver more sediment than the capacity of the water we are delivering can hold (some sediment will be left behind
						//gui.print2Cnsl("want to deliver more sedAmt than water can hold - all sed goes : ");
						adjHNode.addSediment(cS * delT,0,0);
						sedResult = depositSiltAtNode(srcHNode, sedAmt*delT, srcHNode.lclKd);		//TODO make depositSiltAtNode a method of heightnode
						srcHNode.addSediment(-1 * sedResult,0,0);         //sedResult has delT included in it from above
	          
					} else {                                            //water can hold all the sediment this node has to deliver and then some - make more sediment and add to payload
						//gui.print2Cnsl("water is eroding - can carry more sediment than available : ");
						sedCalc = srcHNode.lclKs * sedAmt;             //will be negative
						if(sedCalc > 0){gui.print2Cnsl("\t----------\tdanger, sedcalc calculation wrong");}
						adjHNode.addSediment((deliverSed - sedCalc) * delT,0,0);       //sedCalc is negative
						sedResult = depositSiltAtNode(srcHNode, sedAmt * delT, srcHNode.lclKs);
//	     			       gui.print2Cnsl("difference in heights : before : " + tmpHeight + " after : " + srcHNode.getCurHeight());
						srcHNode.addSediment(-1 * deliverSed * delT,0,0);//entire ratio of sediment removed
					}//if sedAmt<0
				}//if delW < 0
			}                                                        //this node is above adjacent node
	  }//for each adj heightnode
	  //deposit sediment contributed by all nodes above this one - increase altitude to t+l src node
	  //returns portion of sediment used to increase altitude - need to subtract from this node's sediment amount
	  if (sedAmtStay > 0){  //if there's some sediment that is staying, drop it on the node
		  //gui.print2Cnsl("valley node - all sediment stays : ");
		  sedResult = depositSiltAtNode(srcHNode, sedAmtStay * delT, srcHNode.lclKd);
		  srcHNode.addSediment(-1 * sedResult,0,0);
	  }  
	  
	}// calc1stOrderFluvial() function	
	
	/**
	*  calculate the effects of various types of erosion, based on passed type parameter
	*/
	public void calc1stOrderErosion(String type, double delT){
		//use the neighbors method for thermal weathering 
		//--check each heightNode's corresponding source vertex's adjacent list to find neighbors
		//String typeString = "";
		if (this.heightMap != null){
			gui.setNotifyLights(type);    
			if (type.equals("Rain")) { rain1stOrder(delT); } 
			else if (type.equals("COMBOFirstOrder")){ calcErosionCombo(delT);} 
			else {
				Integer newIDX;
				int cloneVertDotID, tmpScaledVal, realVertID;
				myHeightNode cloneSrcNode, putResultVert, tmpSrcNode;
				HashMap<Integer, myHeightNode> tmpOldCloneHeightNodeList = new HashMap<Integer, myHeightNode>(); 
				HashMap<Integer, ArrayDeque<myHeightNode>> tmpHeightMap = new HashMap<Integer, ArrayDeque<myHeightNode>>();
				ArrayDeque<myHeightNode> tmpDeck;
				//get each deque    
				for (Integer IDX : this.heightMap.keySet()){//get each key and value from height map - keys are unique ints but not necessarily sequential
					newIDX = IDX;
					tmpDeck = new ArrayDeque<myHeightNode>();         //make new deque to put modfied nodes in
	  
					for (myHeightNode tmpNode : this.heightMap.get(IDX)){                           //get nodes from old deque at heightmap at position idx - prevent concurrent mod
						cloneVertDotID = calcCloneID(tmpNode.source.ID);        
						cloneSrcNode = this.oldCloneHeightNodeList.get(cloneVertDotID); 
						if (cloneSrcNode == null) {gui.print2Cnsl("error - no height node with id : " + cloneVertDotID);}        
	      	
						//tmpNode.calcHeight(0);
						//cloneSrcNode.calcHeight(0);
	          
						if (type.equals("ThermalWeathering")) {
							calc1stOrderAeolianErosion(tmpNode, cloneSrcNode, this.globWindAngle, delT);  
							//typeString = "First order aeolian erosion (Thermal Weathering) ";      
						} else if (type.equals("HydraulicErosion")){
							calc1stOrderFluvialBase(tmpNode, cloneSrcNode, myConsts.epsValCalc ,delT);
						//	typeString = "First order fluid transport and fluvial erosion";  
	            
						} else { gui.print2Cnsl("unknown erosion type :" + type); }
	          
						tmpScaledVal = tmpNode.calcScaledVal(p.globMinMaxIDX,0);           //sets scaled val internally, used for visualizing data 
						if(tmpScaledVal < 0){}
						setHNode(tmpDeck, tmpNode);
						putResultVert = tmpOldCloneHeightNodeList.put(cloneVertDotID, cloneSrcNode); //put copy of newly calculated eroded vertex in rebuilt oldclonevertlist, for next iteration
						if(putResultVert == null){}
					}
					tmpHeightMap.put(newIDX, tmpDeck);  
				}//for each k-v pair in heightmap
				//need to modify verts in tmpOldCloneVertList to reflect newly calculated heights
				//     this.oldCloneHeightNodeList.clear();
				p.resetAllMinMaxArrays();
				this.heightMap = tmpHeightMap;
				for (myHeightNode tmpOldHNode : tmpOldCloneHeightNodeList.values()){
					realVertID = calcCloneID(tmpOldHNode.source.ID);                          //get "clone" id of clone node = real node's id
					tmpSrcNode = getHNode(tmpHeightMap.get(realVertID));                      //copy of real node
					tmpOldHNode.source.setCoords(tmpSrcNode.source.coords);                   //set "old clone"'s coords to be equal to newly calculated height node coords, 
	                                                                                  //this round's erosion values for next iteration
					tmpSrcNode.source.calcNormal(); 
					tmpOldHNode.copyDonors(tmpSrcNode.getOldDonors());                                                                         
					//tmpSrcNode.evaporateWater(0);										//driven by Ke constant
					tmpOldHNode.setNewVals(tmpSrcNode);                                       //get accurate values in clone list for all values
					this.oldCloneHeightNodeList.put(tmpOldHNode.source.ID, tmpOldHNode);           //rebuild oldCloneVertList              
				}//for each clone vert   
				this.heightMap = tmpHeightMap;
				//gui.print2Cnsl("Erosion processing complete for : " + typeString);
			}//type check to see if raining or combo calculation
		}//if not null heightmap
	}//calcErosion func	

	/**
	*  recalculate the vertex height based on erosion effects - useful for all erosion routines, modifies vertex directly
	*  @param the vertex whose height is being modified
	*  @param the amount to modfiy the vertex's height 
	*/
	public void reCalcHeightOfVert(myVertex source, double locModAmt){
		if (!p.allFlagsSet(myConsts.multByNormal)){//using only y-direction heights
			//gui.print2Cnsl("mod amt for planar obj : " + modAmt);
			source.coords._add(0,locModAmt,0);
		} else {//using normal-direction heights.    
			myVector tmpNorm = new myVector(0,0,0);
			tmpNorm.set(source.getCoords());
			tmpNorm._sub(source.getBaseCoords());
			tmpNorm._normalize();
			tmpNorm._mult(locModAmt);
			source.coords._add(tmpNorm);
		}//if using normal for height calculation or not  
	}//reCalcHeightOfVert method

	/**
	*  calculates the height difference between two vertices
	*  @return the difference in height between the two passed vertices, along appropriate axis based on type of mesh
	*/
	public double calcHeightDiff(myVertex aV, myVertex aU){
		if (!p.allFlagsSet(myConsts.multByNormal)){                                                              //using only y-direction heights
			return ((aV.coords.y) - (aU.coords.y));
		} else {//using normal-direction heights.    
			double aVlen = myVector._dist(aV.coords, aV.baseCoords);
			double aUlen = myVector._dist(aU.coords, aU.baseCoords);
			return (aVlen - aUlen);
		}//if using normal for height calculation or not  
	}//calcHeightDiff

	/**
	 * calculates the height difference between two height nodes src and adj
	 * @return the difference
	 */
	public double calcNodeHeightDiff(myHeightNode src, myHeightNode adj){return (src.source.coords.y - adj.source.coords.y);}	
	public double calcNodeHeightDiffLatErosion(myHeightNode src, myHeightNode adj){	return ((src.source.coords.y + src.getHeightWater(2)) - adj.source.coords.y);}	
	
	/**
	*  calculates the height difference between two nodes, taking into account the amt of water on each 
	*  @param src is the src node
	*  @param adj is the adjacent node
	*  @param idx is the index of the nodes' volwater arrays we are querying for source of h2o
	*  return the height difference - if negative means adjacent node is higher than source node
	*/
	public double calcNodeHeightDiffIncH2O(myHeightNode src, myHeightNode adj, int idx){
		double retVal = (src.getHeightWater(idx) + src.source.coords.y) - (adj.getHeightWater(idx) + adj.source.coords.y);
//		retVal = (Math.abs(retVal) > 1000*myConsts.epsValDisplay) ? retVal : 0;
		return retVal;  
	}//calcHeightNodeDiff function

	/**
	*  rain on scene and perform a certain number of iterations of thermal weathering and hydraulic erosion
	*/
	public void calcErosionCombo(double delT){
		rain1stOrder(delT);
		int upperBound = (int)p.random(65,100);
		for (int i = 0; i < upperBound; ++i){
			if (i % 10 == 0) { rain1stOrder(delT);}
            calc1stOrderErosion("HydraulicErosion", p.getDeltaT()); 
			calc1stOrderErosion("ThermalWeathering", p.getDeltaT());
		}//for each cycle of interations
		gui.print2Cnsl("\nfinished with simulation - " + upperBound + " iterations");
	}//calcErosionCombo

	/**
	*  process erosion calculation on passed node for use with 2nd order equations - modifications are put in t+delt slot in array equivalent to 
	*  changes path performed via fluvial erosion
	*  
	*  srcHeightNodeOld : time t (old) height node, for reading info but not modified
	*  tmpNode : time t+1 (new) height node, receives new value but isn't read
	*	
	* ---	old mechanism - replaced below   ---
	*
	*  @param tmpNode is the node to erode
	*  @param srcVertOld is the clone of the vertex that has the original height and adjacent vert heights stored in it
	*  @param windAngle is angle wind is coming from
	*  @param delT is incremental time step multiplier
	*/
	public void calc2ndOrderAeolianErosion(myHeightNode srcHNode, double windAngle, double delT){//use old src vert for height and adjcent vert height info, but modify originals
		double totModAmt = 0.0f, calcTAngle, calcWAngle, TalusAngle, wAngleModRatio, locModAmt;     //total amount taken away from this node
		double angleDiff; //angle difference between the incoming wind and the orientation of 2 verts
		if (srcHNode.getHeightWater() < myConsts.epsValCalc){		//if there's any water on this vert, don't conduct aeolian erosion
			myVertex srcVert = srcHNode.source;                                                        //this is what will be modified, but not read - time t+1
			myHeightNode adjHNode;
			p.resetGMinMaxAra(myConsts.SED);
			p.resetGMinMaxAra(myConsts.COORD_Y);
			for (myVertex adjVert : srcVert.adjVerts){
				adjHNode = getHNode(this.heightMap.get(adjVert.ID));                //get the heightnode corresponding to a particular vert ID for a current adj vert
		    
				calcTAngle = calcTalusAngle(srcVert,adjVert);                       //read from old values
				calcWAngle = calcAngleDiff(srcVert,adjVert);
				angleDiff = Math.abs(calcWAngle - windAngle);                                       //find the angle from the adj node to src node vector to the incident wind
				
				if (angleDiff > Math.PI) { angleDiff = -1*((2*Math.PI) - angleDiff); }                   //normalize to be between PI and -PI
				wAngleModRatio = Math.cos(angleDiff);
				//wAngleModRatio = ((wAngleModRatio > 0) ? wAngleModRatio : 0);    //wind behind the node does nothing
				TalusAngle = mySqSqEroder.GlobTalusAngle * randTalusAngleMult();
				if (calcTAngle > TalusAngle){                                                  //difference between src and neighbor is sufficient for erosion
					locModAmt = (mySqSqEroder.Talus_C * (calcTAngle - TalusAngle) * delT) * wAngleModRatio * this.globAeolErosionMult;     //delT is timestep multiplier for sim 
					adjHNode.addHeightByVal(locModAmt);					//reCalcHeightOfVert(adjVert,locModAmt);                                               //modify actual adj vertex to gain some small amount of either height or water, or both
					//adjHNode.calcHeight(0);        //recalculate height of node
					totModAmt += locModAmt;
				}//if calculated angle > modified talus angle    
			}//for each adjacent vert to srcVert
			//remove from source vert amount moved to neighbors
			reCalcHeightOfVert(srcVert, -1 * totModAmt);                                             //modify actual src vertex to loose amount equal to all gains
			//srcHNode.calcHeight(0);
			srcHNode.setWindForce(new myVector(3 * Math.sin(windAngle) * delT, 0, 3 * Math.cos(windAngle) * delT));
		}
	}//calc2ndOrderAeolianErosion method
	

	/**
	*  process erosion calculation on passed node for use with 2nd order equations - modifications are put in t+delt slot in array equivalent to 
	*  changes path performed via fluvial erosion
	*
	*  srcHeightNodeOld : time t (old) height node, for reading info but not modified
	*  tmpNode : time t+1 (new) height node, receives new value but isn't read
	*  need to erode from up-slope and deposit on downslope
	*
	*  @param tmpNode is the node to erode
	*  @param windAngle is angle wind is coming from
	*  @param delT is incremental time step multiplier
	*/
	public void calc2ndOrderAeolianErosionAra(myHeightNode srcHNode, double windAngle, double delT){//use old src vert for height and adjcent vert height info, but modify originals
		//gui.print2Cnsl("2nd order aeolian");
		double totModAmt = 0.0f, calcTAngle, calcWAngle, TalusAngle, wAngleModRatio, locModAmt;     //total amount taken away from this node
		double angleDiff; //angle difference between the incoming wind and the orientation of 2 verts
		srcHNode.setWindForce(new myVector(3 * Math.sin(windAngle) * delT, 0, 3 * Math.cos(windAngle) * delT));
		
		//if (srcHNode.getHeightWater() < myConsts.epsValCalc){		//if there's any water on this vert, don't conduct aeolian erosion
		myVertex srcVert = srcHNode.source, adjVert;
		p.resetGMinMaxAra(myConsts.SED);
		p.resetGMinMaxAra(myConsts.COORD_Y);
		for (myHeightNode adjHNode : srcHNode.getAdjNodes().values()){
			adjVert = adjHNode.source;
			calcTAngle = calcTalusAngle(srcVert,adjVert);                            //read from old values
			calcWAngle = calcAngleDiff(srcVert,adjVert);
			angleDiff = Math.abs(calcWAngle - windAngle);                                       //find the angle from the adj node to src node vector to the incident wind
			
			if (angleDiff > Math.PI) { angleDiff = -1*((2*Math.PI) - angleDiff); }                   //normalize to be between PI and -PI
			wAngleModRatio = Math.cos(angleDiff);
			wAngleModRatio = ((wAngleModRatio > 0) ? wAngleModRatio : this.globAeolErosionMult * wAngleModRatio);    //wind behind the node does nothing
			TalusAngle = mySqSqEroder.GlobTalusAngle * randTalusAngleMult();
			if (calcTAngle > TalusAngle){                                                  //difference between src and neighbor is sufficient for erosion
				locModAmt = (mySqSqEroder.Talus_C * (calcTAngle - TalusAngle) * delT) * wAngleModRatio * this.globAeolErosionMult;     //delT is timestep multiplier for sim 
				//if (srcHNode.getHeightWater() < myConsts.epsValCalc){		//if there's any water on this vert, don't conduct aeolian erosion
					adjHNode.addHeightByVal(locModAmt);					//reCalcHeightOfVert(adjVert,locModAmt);                                               //modify actual adj vertex to gain some small amount of either height or water, or both
				//}
				//else {//water on this node
					//find amount of force to apply to water at node, from .9*water height to 1.1*waterheight
				
					
				//}
				totModAmt += locModAmt;
			}//if calculated angle > modified talus angle    
		}//for each adjacent vert to srcVert
		//remove from source vert amount moved to neighbors
		reCalcHeightOfVert(srcVert, -1 * totModAmt);                                             //modify actual src vertex to loose amount equal to all gains
		//}//if water on node
		//else {//apply wind to water - TODO:wind on water
			//find amount of force to apply to water at node, from .9*water height to 1.1*waterheight
			
		//}
	}//calc2ndOrderAeolianErosion method

	/**
	 * this will calculate the lateral sediment capacity for boundary nodes, based on their velocity in the direction of their non-wet neighbors
	 * @param srcHNode src node doing the eroding/depositing
	 * @param dirV_comp direction component of velocity being used for lat sed cap calc
	 * @param srcNodeH2O water volume in original node
	 * @param scaleVol	abrasive action of sediment already in src node - varies between 1 and 2
	 * @return
	 */	
	public double calc2ndOrderLatSedCapacity(double adjNodeLclKc, double dirV_comp, double srcNodeH2O, boolean scaleVol, double abrasionSedMult){	
		double sedCapResult; 				
		//if((dirV_comp == 0) || (!scaleVol && (srcNodeH2O < myConsts.epsValCalc)) || (adjNodeLclKc == 0)){return 0; } 
		if((dirV_comp == 0) || (adjNodeLclKc == 0)){return 0; } 
		sedCapResult = adjNodeLclKc 		//use the adjacent node's lclKc value, in case adj node is a boulder
				* dirV_comp 				//this is directional value based on which component of velocity is incident to wall - may be negative
				* abrasionSedMult			//multiply effect by val between 1, depending on sed content in node
				* (scaleVol ? (srcNodeH2O
				//		/p.globMaxAra[myConsts.H2O]
								) : 1 )	//scale by volume - instead of scaling by max mesh vol, just divide by 10 to keep Kc from needing to get too small
		//		/p.globMaxAra[myConsts.H2O]				
			;
			//record this sediment capacity globally, to keep track of global capacity
		p.globTotValAra[myConsts.SEDCAP] += sedCapResult;
		p.setGlobalVals(myConsts.SEDCAP,sedCapResult);
		return sedCapResult;	
	}//calc2ndOrderLatSedCapacity
	
	/**
	 * calculate sediment capacity for the passed node
	 * @param srcHNode
	 * @param srcNodeH2O
	 * @param scaleVol
	 * @return
	 */	
	public double calc2ndOrderSedCapacity(myHeightNode srcHNode, double srcNodeH2O, boolean scaleVol){	//kc for mississippi ~.35-.4 in tons/day/cfs vs h2o @cfs
		double sedCapResult; 
		myVector srcV = srcHNode.getVelocity();		

		//if((srcV.sqMagn == 0) || (srcNodeH2O < myConsts.epsValCalc)){return 0; } 
		if(srcV.sqMagn == 0) {return 0; } 
		//determine whether or not to use y component of velocity for sedCap calc
		myVector vVec = (!p.allFlagsSet(myConsts.ignoreYVelocity)) ? (srcV.cloneMe()) : (new myVector(srcV.x, 0, srcV.z)) ; //whether or not to use y comp for calculations
		double origVecMag = vVec._mag();
		sedCapResult = srcHNode.lclKc
				* origVecMag 
				* (scaleVol ? (srcNodeH2O
									) : 1 )	//scale by volume - instead of scaling by max mesh vol, just divide by 10 to keep Kc from needing to get too small
			;													//all alone down here to be able to comment out middle line
		
			//determine whether or not to limit h2o capable of having sediment capacity to some capped value by dividing by volume that is greater than 
		if(p.allFlagsSet(myConsts.limitWaterSedCap)){
			sedCapResult /= ((srcNodeH2O <= 1 ) ? (1) : (srcNodeH2O));
			}
			//record this sediment capacity globally, to keep track of global capacity
		p.globTotValAra[myConsts.SEDCAP] += sedCapResult;
		p.setGlobalVals(myConsts.SEDCAP,sedCapResult);
		return sedCapResult;
	}//calcSedCap	
	
	/**
	 * this will convert a passed amount of sediment from altitude at this node
	 * @param convertNewHeight the amount of altitude to convert to sediment - these values vary to facilitate boundaries.
	 * @param convertNewSed amount going to sediment array
	 * @param destsIdx the destination idx in the sediment array that will receive this sediment
	 * need useSedFlux instead of global flag because lat erosion overrides global setting and always acts like flux transport
	 * @param massWasting if masswasting don't use advection for sed erosion
	 */
	public void convertHeightToSed(myHeightNode heightSrc, myHeightNode sedSrc, double convertNewHeight, double convertNewSed, boolean useSedFlux, boolean massWasting){
		int destIdx = ((useSedFlux) ? 2 : 1);
			//take from height of node - using return value is due to possibility of having "steel floor" for erosion to prevent it from eroding any lower than some threshold
		if ((heightSrc != null) && (convertNewHeight != 0)){
			if((p.allFlagsSet(myConsts.advectErosionDep)) 
					//&& (!massWasting)
					){	
				//this.advectSedDep(heightSrc, sedSrc.getVelocity().cloneMe(), -1 * convertNewHeight, p.getDeltaT());
				this.advectSedDep(sedSrc, sedSrc.getVelocity().cloneMe(), -1 * convertNewHeight, p.getDeltaT());
			} else {											
				heightSrc.addHeightByVal( -1 * convertNewHeight);	
			}			//modifying existing height, remove sediment from height or deposit sediment to height			
		}
		//this.sedToDrop = this.sedMult * calcNewSed;						
			//add to sediment at node to idx0 vals, put result in idx1
		if((sedSrc != null) && (convertNewSed != 0)) {
			int idxIncr = (massWasting ? 1 : 0);
			sedSrc.addSediment( convertNewSed,  destIdx-1 + idxIncr, destIdx);								//add sediment to sediment availability from idx srcIDX to idx srcIDX+ 1, or same idx if MassWasting
			p.addToTotalVal(myConsts.SED, sedSrc.getSediment(destIdx + idxIncr));
		}
	}//convertHeightToSed	
	
	/**
	 * if using global lowest k then apply it to all flux vals equally
	 * @param srcHNode
	 * @param pXSection
	 * @param nodeArea
	 * @param delT
	 */
	public void calc2ndOrderPipesFluxValsApplyGlobalK(myHeightNode srcHNode, double pXSection, double nodeArea, double delT){
		double sumFlux = srcHNode.getFluxSum(1);  //this is sum of flux leaving source node
		double ratio1;//, ratio2;
		if(srcHNode.getkVal() >= .99){//only process reasonable k's globally
			if (sumFlux != 0){
				for (int tDir = 0; tDir < myHeightNode.numDir; ++tDir){
					ratio1 = srcHNode.getFluxByDir(1,tDir) / sumFlux;
					srcHNode.setAdjWaterShare(tDir, ratio1);
					if(this.globalKVal != 1){			srcHNode.scaleFlux(1,tDir,this.globalKVal);		}
				}//for each direction
				if(this.globalKVal != 1) {
					if(srcHNode.getkVal() != this.globalKVal){			
						srcHNode.setkScaledNode (false);			//boolean holding whether k is 1 or not
						srcHNode.setkVal(1);
					} else {	srcHNode.setkScaledNode (true);	 }//only display non-unity k vals for nodes causing k to be lowest value  		//boolean holding whether k is 1 or not					

				}
			}//if sumFlux != 0		
			//set values for debugging
			if ((p.nodeInSliceRange(srcHNode.source.coords.x, srcHNode.source.coords.z))//node is in slice range
			  && (p.allFlagsSet(myConsts.debugModeCnsl)))				  //only save debug data if in debug mode  
			{//we are debugging and these coords are being displayed in a slice
				myDebugObj tmpDebugObj = new myDebugObj(this.p, this.gui, p.simCycles, delT, srcHNode.getHeightWater(1), this.globalKVal );
				tmpDebugObj.buildDebugData(srcHNode, 1);		//1 is idx of relevant flux data
				Double tmpKey = srcHNode.source.coords.x + (this.DebugHashOffset * srcHNode.source.coords.z);
				this.debugFluxVolVals.put(tmpKey, tmpDebugObj);  //holds flux in each of 4 card dirs, plus vol water at node in idx 4 and k at idx 6
			}//if we are debugging and at z = 0 on the object
		}
	}//calc2ndOrderPipesFluxValsApplyGlobalK	

	/**
	 * calculate Midpoint/Trap rule methods flux values to go in each pipe - flux idxs go from 0 to 1 - no k calculation here
	 * @param srcHNode the source height node at t+delT (end result node)
	 * @param pXSection the cross section of the pipe for the flux model
	 * @param deltaNodeDist the distance between adjacent nodes/vertexes in mesh
	 * @param epsVal the current global epsilon value
	 * @param delT the current global delta t
	 */
	public void calc2ndOrderPipesFluxValsMidPoint(myHeightNode srcHNode, double pXSection, double nodeArea, double delT){
		double heightDiff;
		myHeightNode adjHNode;
		double tmpVal;
		int dir;     //0-3 for n,e,s,w
		double srcNodeH2O_1 = srcHNode.getHeightWater(1);
		double calcFluxAmt; //amount of calculated flux

		for (Integer IDX : srcHNode.getAdjNodes().keySet()){//idx's are obj id's - not sequential - this gives each adjHNode to src hnode
			//tmpVal = 0;
			adjHNode = srcHNode.getAdjNodes().get(IDX);
			heightDiff = srcHNode.getAdjHeightDiffsByIDX(adjHNode.ID);

			dir = srcHNode.getAdjNodeDirByID().get(IDX);      //dir is 0-3 for n,e,s,w
				//flux always positive - only outgoing flux.  incoming flux covered by other nodes' outgoing flux
			calcFluxAmt = srcHNode.getFluxByDir(0,dir) + (delT * (pXSection * myConsts.grav * this.gravMult * heightDiff)) / (srcHNode.getPipeLength(IDX));//using pythagorean len
			if (calcFluxAmt > 0){ tmpVal = calcFluxAmt; }
			else { 			      tmpVal = 0;}

				//puts flux in ara idx 1
			srcHNode.setFlux(1, dir, tmpVal);
		}//for each index in neighbors keyset                          
//now calculate ratio of node's current water allocated to each pipe based on velocity
		//want sum of flux after round 1
		double sumFlux = srcHNode.getFluxSum(1);  //this is sum of flux leaving source node
		double ratio1;//, ratio2;
		//if the flux leaving times the timestep delT is greater than the vol of water - srcNodeH2O_1 is height of water at node, deltaNodeDist * deltaNodeDist is area of node
		double volAtNode = srcNodeH2O_1 * nodeArea;	//this is water in volume of source node
		if (sumFlux != 0){
			for (int tDir = 0; tDir < myHeightNode.numDir; ++tDir){
				ratio1 = srcHNode.getFluxByDir(1,tDir) / sumFlux;
				srcHNode.setAdjWaterShare(tDir, ratio1);
			}//for each direction
		}//if sumFlux != 0	
		//set values for debugging
	}//procCalcPipeFluxVals function
	
	/**
	 * calculate flux values to go in each pipe - flux idxs go from 0 to 1
	 * @param srcHNode the source height node at t+delT (end result node)
	 * @param pXSection the cross section of the pipe for the flux model
	 * @param deltaNodeDist the distance between adjacent nodes/vertexes in mesh
	 * @param epsVal the current global epsilon value
	 * @param delT the current global delta t
	 */
	public void calc2ndOrderPipesFluxVals(myHeightNode srcHNode, double pXSection, double nodeArea, double delT){
		double heightDiff;
		myHeightNode adjHNode;
		double K, tmpVal;
		int dir;     //0-3 for n,e,s,w
		double srcNodeH2O_1 = srcHNode.getHeightWater(1);
		double calcFluxAmt; //amount of calculated flux

		for (Integer IDX : srcHNode.getAdjNodes().keySet()){//idx's are obj id's - not sequential - this gives each adjHNode to src hnode
			//tmpVal = 0;
			adjHNode = srcHNode.getAdjNodes().get(IDX);
			heightDiff = srcHNode.getAdjHeightDiffsByIDX(adjHNode.ID);

			dir = srcHNode.getAdjNodeDirByID().get(IDX);      //dir is 0-3 for n,e,s,w
				//flux always positive - only outgoing flux.  incoming flux covered by other nodes' outgoing flux
			calcFluxAmt = srcHNode.getFluxByDir(0,dir) + (delT * (pXSection * myConsts.grav * this.gravMult * heightDiff)) / (srcHNode.getPipeLength(IDX));//using pythagorean len
			if (calcFluxAmt > 0){ tmpVal = calcFluxAmt; }
			else { 			      tmpVal = 0;}

				//puts flux in ara idx 1
			srcHNode.setFlux(1, dir, tmpVal);
		}//for each index in neighbors keyset                          
//now calculate ratio of node's current water allocated to each pipe based on velocity
		//want sum of flux after round 1
		double sumFlux = srcHNode.getFluxSum(1);  //this is sum of flux leaving source node
		double ratio1;//, ratio2;
		//if the flux leaving times the timestep delT is greater than the vol of water - srcNodeH2O_1 is height of water at node, deltaNodeDist * deltaNodeDist is area of node
		double volAtNode = srcNodeH2O_1 * nodeArea;	//this is water in volume of source node
		if ((sumFlux * delT) >= volAtNode){   
			K = volAtNode / (sumFlux * delT);			K *= .9999999999999;
		}//needed for flat surfaces to eventually stop water from moving
		else {		 					      K = 1; }		
		if((p.allFlagsSet(myConsts.useLowestKGlobally)) && (K >=.99)){
			this.globalKVal = Math.min(this.globalKVal, K);//ignore tiny values
			srcHNode.setkScaledNode (K!=1);			//boolean holding whether k is 1 or not - used only to tell where lowest k originated from
			srcHNode.setkVal(K); 
		} else {//not using global lowest k val		
			if (sumFlux != 0){
				for (int tDir = 0; tDir < myHeightNode.numDir; ++tDir){
					ratio1 = srcHNode.getFluxByDir(1,tDir) / sumFlux;
					srcHNode.setAdjWaterShare(tDir, ratio1);
					if(K != 1){
						srcHNode.scaleFlux(1,tDir, K);
					}
				}//for each direction
				srcHNode.setkScaledNode (K!=1);			//boolean holding whether k is 1 or not
				srcHNode.setkVal(K);			
			}//if sumFlux != 0	
			//set values for debugging
			if ((p.allFlagsSet(myConsts.debugModeCnsl)) && (p.nodeInSliceRange(srcHNode.source.coords.x, srcHNode.source.coords.z)))//only save debug data if in debug mode and node is in slice range			  				   
			{//we are debugging and these coords are being displayed in a slice
				myDebugObj tmpDebugObj = new myDebugObj(this.p, this.gui, p.simCycles, delT, srcHNode.getHeightWater(1), K );
				tmpDebugObj.buildDebugData(srcHNode, 1);		//1 is idx of relevant flux data
				Double tmpKey = srcHNode.source.coords.x + (this.DebugHashOffset * srcHNode.source.coords.z);
				this.debugFluxVolVals.put(tmpKey, tmpDebugObj);  //holds flux in each of 4 card dirs, plus vol water at node in idx 4 and k at idx 6
			}//if we are debugging and at z = 0 on the object
		}//if not using lowest k globally
	}//procCalcPipeFluxVals function

	
	/**
	 * calculate water values for midpoint method - this only calculates values, doesnt actually move water to new idxs
	 *  calculate water values and velocity fields - get outflow flux of neighbors into this src, sum and subtract this node's outflow
	 *  to get h2o vol change - vol h2o idx's go from 1 to 2
	 * @param srcHNode the source height node at t+delT (end result node)
	 * @param srcHNodeOld the source height node at t (beginning node)
	 * @param deltaNodeDist the distance between adjacent nodes/vertexes in mesh
	 * @param delT the current global delta t	*  
	*/
	public void calc2ndOrderPipesWaterValsMidPoint(myHeightNode srcHNode, double nodeArea, double delT){
		double delV;  
		final int dirN = 0, dirE = 1, dirS = 2, dirW = 3;  
		double inFluxSum = 0, 
				inFluxAdj,
				srcH2OAmt = srcHNode.getHeightWater(1),
				outFluxSum = srcHNode.getFluxSum(1);	//sum of outgoing flux values
		myHeightNode[] tmpAdjHNode = new myHeightNode[myHeightNode.numDir];								//adjheight nodes by direction
		double[] fluxIns = new double[myHeightNode.numDir];												//calculate in-fluxes based on each neighbor node, some of which may be null
	  
		for (int dir = 0; dir < myHeightNode.numDir; ++dir){      											//dir is n,e,s,w, this is summing all influxes
			tmpAdjHNode[dir] = srcHNode.getAdjNodesByPassedDir(dir);						//get adj node at particular direction fromm source
			if (tmpAdjHNode[dir] != null) { 
				inFluxAdj = tmpAdjHNode[dir].getFluxByDir(1,((dir+2) % 4)); 				//get the adj node to the dir direction's (dir+2)%4 direction flux - that's his flux back into me			
				fluxIns[dir] = inFluxAdj;													//flux into this node from neighbor to the dir direction
				inFluxSum += inFluxAdj;
//		 	     if ((inFluxAdj != 0) || (outFluxSum != 0)) { gui.print2Cnsl("adj node : " + tmpAdjHNodeOld.ID + " src node " + srcHNodeOld.ID +  " : inFlux : " + inFluxAdj + " outfluxsum : " + outFluxSum);      }
			}//if not null node
			else { 	fluxIns[dir] = 0; }
		}//for each direction                          
		delV = delT * (inFluxSum - outFluxSum);
		double newWaterHeight = srcH2OAmt + (delV/(nodeArea)); 
		srcHNode.setHeightWater(newWaterHeight, 4, false);		
	}//calc2ndOrderPipesWaterValsMidPoint function		
	
	
	/**
	*  calculate water values and velocity fields - get outflow flux of neighbors into this src, sum and subtract this node's outflow
	*  to get h2o vol change - vol h2o idx's go from 1 to 2
	 * @param srcHNode the source height node at t+delT (end result node)
	 * @param srcHNodeOld the source height node at t (beginning node)
	 * @param deltaNodeDist the distance between adjacent nodes/vertexes in mesh
	 * @param delT the current global delta t	*  
	*/
	public void calc2ndOrderPipesWaterVals(myHeightNode srcHNode, double nodeArea, double delT){
		double delV, delSed;  
		final int dirN = 0, dirE = 1, dirS = 2, dirW = 3;  
		double inFluxSum = 0, 
				inFluxAdj,
				srcH2OAmt = srcHNode.getHeightWater(1),
				outFluxSum = srcHNode.getFluxSum(1);	//sum of outgoing flux values
				//using this method to move sediment too
		double inH2OAdj, inSedSum = 0,	inSedAdj,
				srcSedAmt = 0,// = getSedTransportVal(srcHNode, 0, 1),	
				outSedSum = 0;
		if (p.allFlagsSet(myConsts.useSedFlux) ){
			srcSedAmt = getSedTransportVal(srcHNode, 0, 1);	
			if(srcH2OAmt >0){			outSedSum = outFluxSum * srcSedAmt/srcH2OAmt; 	} 			//amount of sediment in this node that is leaving - determined by the ratio of water leaving to total water
		}
		myHeightNode[] tmpAdjHNode = new myHeightNode[myHeightNode.numDir];								//adjheight nodes by direction
		double[] fluxIns = new double[myHeightNode.numDir];												//calculate in-fluxes based on each neighbor node, some of which may be null
	  
		for (int dir = 0; dir < myHeightNode.numDir; ++dir){      											//dir is n,e,s,w, this is summing all influxes
			tmpAdjHNode[dir] = srcHNode.getAdjNodesByPassedDir(dir);						//get adj node at particular direction fromm source
			if (tmpAdjHNode[dir] != null) { 
				inFluxAdj = tmpAdjHNode[dir].getFluxByDir(1,((dir+2) % 4)); 				//get the adj node to the dir direction's (dir+2)%4 direction flux - that's his flux back into me			
				fluxIns[dir] = inFluxAdj;													//flux into this node from neighbor to the dir direction
				inFluxSum += inFluxAdj;
					//if no water, then no sediment is coming in either
				if (p.allFlagsSet(myConsts.useSedFlux) ){
					inH2OAdj = tmpAdjHNode[dir].getHeightWater(1);
					inSedAdj = ((inH2OAdj == 0) ? 0 : ((inFluxAdj/inH2OAdj) * (getSedTransportVal(tmpAdjHNode[dir],0,1))));
					inSedSum += inSedAdj;
				}
//		 	     if ((inFluxAdj != 0) || (outFluxSum != 0)) { gui.print2Cnsl("adj node : " + tmpAdjHNodeOld.ID + " src node " + srcHNodeOld.ID +  " : inFlux : " + inFluxAdj + " outfluxsum : " + outFluxSum);      }
			}//if not null node
			else { 	fluxIns[dir] = 0; }
		}//for each direction                          
		delV = delT * (inFluxSum - outFluxSum);
		double newWaterHeight = srcH2OAmt + (delV/(nodeArea)); 
		srcHNode.setHeightWater(newWaterHeight,2, true);
		
		if (p.allFlagsSet(myConsts.useSedFlux) ){
			delSed = delT * (inSedSum - outSedSum);	//delta t already accounted for in sed calculations
			double newSedVal = srcSedAmt + (delSed /(nodeArea));	
			if(srcHNode.lclKc != 0) {			setSedTransportVal(srcHNode,1,2,newSedVal);	}//sed transport either sed or conc
		}
			// now set source heightnode's velocity vector
		srcHNode.calcAndSetVelocity(fluxIns[dirW], fluxIns[dirE],fluxIns[dirN],fluxIns[dirS], 1, srcHNode.getVolWaterAra(), 2, delV/delT);
		if(p.allFlagsSet(myConsts.useSedFlux)){
			myVector sedVelocity = srcHNode.getVelocity().cloneMe(), startLoc = srcHNode.source.coords.cloneMe();
			sedVelocity._mult(-1 * delT);
			startLoc._add(sedVelocity);
			srcHNode.setSedSource(startLoc);
		}
		srcHNode.boundaryDryNode = false;
		srcHNode.setBoundaryNode(false);										//set true if any neighbor has no water and this has water
		
	}//procCalcPipeFluxVals function		

//	/**
//	 * search a double ara for the index of the value closest to the search-for val
//	 * @param ara ara to search
//	 * @param val to search for
//	 * @param idx index to search at - start at aralen/2
//	 * @param startIDX - start of ara search area, starts at 0
//	 * @param endIDX end index in search section of ara - starts at ara.len-1
//	 * @return idx
//	 */	
//	private int binarySearchDoubleAra(Double[] ara, double val, int idx, int startIDX, int endIDX){
//		if ((ara[idx] == val) || (startIDX + 1 == endIDX)){		return idx;}//if value found or if searching at same idx we searched at last time
//		else if (ara[idx] > val) {								return binarySearchDoubleAra(ara, val, (startIDX + idx)/2, startIDX, idx);}
//		else if (ara[idx] < val) {								return binarySearchDoubleAra(ara, val, (endIDX + idx)/2, idx, endIDX);}//if ara val equal, greater, less than val
//		return -1;
//	}//binarySearchDoubleAra method
//	
	
	/**
	 *	NOT USED - now using ara version
	 * fast method for pipes transport of sediment, using stam-y lagrangian method - put in each node at srcIDX + 1
	 * calculate by finding the sediment at t + delt that came to this node during t time step from a node at x - u*delT, z - v*delT
	 * @param srcHNode the source node to draw information from about sediment
	 * @param srcHNodeOld the old source node - gets copy of result
	 * @param xCoords array of all x coords of this mesh - in order
	 * @param zCoords array of all z coords of this mesh - in order
	 * @param delT time step	
	 * @param srcIDX index to pull sed from
	 */
	public void calc2ndOrderPipesSedTransport(myHeightNode srcHNode, /*myHeightNode srcHNodeOld,*/ double delT){}//calc2ndOrderPipesSedTransport
	
	/**
	 * this method determines the 4 surrounding nodes for a particular x and z value for location, account for wrap around, putting them in resAra
	 * the nodes will be idx 0 - lowXlowZ; 1 - lowXhighZ; 2 - highXlowZ; 3 - highXhighZ
	 * @param newXIDX the location for x direction on the mesh
	 * @param newZIDX the location for z direction on the mesh
	 * @return the array containing the 4 nodes that surround the xVal,zVal location
	 */		
	public void linInterpNodes(double newX, double newZ, boolean betweenWrapNodesX, myHeightNode[] resAra){
		//idx in result ara
		//final int lowXlowZ = 0, lowXhighZ = 1, highXlowZ = 2, highXhighZ = 3;
		
		int xLowCoordIDX, xHighCoordIDX, zLowCoordIDX, zHighCoordIDX, 
		//intActXLow = -1, intActXHigh = -1, intActZLow = -1, intActZHigh = -1,			//actual coordinates used in array lookup, set when lookup completed Debug use
		xHNodeIDXMod = 0, 
		zHNodeIDXMod = 0;		
		
			//find virtual index in ara of sediment to pull - actual idx's will will be ints on either side of these doubles, barring boundry conditions
		double newXIDXLoc =  newX - p.globMinAra[myConsts.COORD_X]; 
		double newZIDXLoc =  newZ - p.globMinAra[myConsts.COORD_Z];	
		
			//new index location corresponding to the greatest value less than newXIDX/newZIDX - floor is low idx, ceil is high idx, float part is interpolant
		xLowCoordIDX = ((gui.inRange((float)newXIDXLoc, 0, this.heightMapAra.length - 1 )) ?  (int)newXIDXLoc :			//if in valid range, then use int rounding of val, otherwise use boundary vals, based on value 
						(newXIDXLoc < 0) ? 0 : this.heightMapAra.length - 1) ;
		xHighCoordIDX = xLowCoordIDX + ((xLowCoordIDX >= (this.heightMapAra.length - 1 ) /* || (newXIDXLoc < 0)*/) ? 0 : 1);
		if (newX < p.globMinAra[myConsts.COORD_X]){ 	xHNodeIDXMod = -1;}//means the value of x is lower than the lowest value
		if (newX > p.globMaxAra[myConsts.COORD_X]){		xHNodeIDXMod = 1;}

		zLowCoordIDX = ( (gui.inRange((float)newZIDXLoc, 0, this.heightMapAra[0].length - 1 ) ) ?  (int)newZIDXLoc :			//if in valid range, then use int rounding of val, otherwise use boundary vals, based on value 
			(newZIDXLoc < 0) ? 0 : this.heightMapAra[0].length - 1) ;				
				
		zHighCoordIDX = zLowCoordIDX + ((zLowCoordIDX >= (this.heightMapAra[0].length - 1)/* || (newZIDXLoc < 0)*/ ) ? 0 : 1);
		if (newZ < p.globMinAra[myConsts.COORD_Z]){			zHNodeIDXMod = -1;}		//means use low coord as high coord - value is lower than lowest possible coord - and 0 for low coord sediment
		if (newZ > p.globMaxAra[myConsts.COORD_Z]){			zHNodeIDXMod = 1;}		//means use 0 for high sediment
		
		//make sure low and high coords are legal

		if(betweenWrapNodesX){
			xLowCoordIDX = this.heightMapAra.length - 1;
			xHighCoordIDX = 0;
			zHighCoordIDX = zLowCoordIDX + 1;
			if(zLowCoordIDX >= 0) {
				resAra[lowXlowZ] 		= (this.heightMapAra[xLowCoordIDX][zLowCoordIDX]);
				resAra[highXlowZ]	 	= (this.heightMapAra[xHighCoordIDX][zLowCoordIDX]);
			} else {
				resAra[lowXlowZ] 		= null;
				resAra[highXlowZ]	 	= null;
			}
			
			if(zLowCoordIDX < this.heightMapAra[0].length - 1) {
				resAra[lowXhighZ] 		= (this.heightMapAra[xLowCoordIDX][zHighCoordIDX]);		
				resAra[highXhighZ]  	= (this.heightMapAra[xHighCoordIDX][zHighCoordIDX]);	
	
			} else {
				resAra[lowXhighZ] 		= null;		
				resAra[highXhighZ]  	= null;	
			}
				//debug values
//			intActXLow	= this.heightMapAra.length - 1;
//			intActXHigh = 0;
//			intActZLow 	= zLowCoordIDX;
//			intActZHigh	= zLowCoordIDX+1;
		
		} else {
			if((xHNodeIDXMod != -1) && (zHNodeIDXMod != -1)){//means both low coords are valid
				resAra[lowXlowZ] = (this.heightMapAra[xLowCoordIDX][zLowCoordIDX]);
//				intActXLow	= xLowCoordIDX;
//				intActZLow 	= zLowCoordIDX;
				}
			
			if((xHNodeIDXMod != 1) && (zHNodeIDXMod != 1)){//means both high coords coords are valid
				resAra[highXhighZ] = (this.heightMapAra[xHighCoordIDX][zHighCoordIDX]);		
//				intActXHigh = xHighCoordIDX;
//				intActZHigh	= zHighCoordIDX;
			}
			
			if((xHNodeIDXMod != 1) && (zHNodeIDXMod != -1)){//means high x and low z coords are valid
				resAra[highXlowZ] = (this.heightMapAra[xHighCoordIDX][zLowCoordIDX]);
//				intActXHigh = xHighCoordIDX;
//				intActZLow 	= zLowCoordIDX;
			}
			
			if((xHNodeIDXMod != -1) && (zHNodeIDXMod != 1)){//means lowx and high z are valid
				resAra[lowXhighZ] = (this.heightMapAra[xLowCoordIDX][zHighCoordIDX]);		
//				intActXLow	= xLowCoordIDX;
//				intActZHigh	= zHighCoordIDX;
			}						
		}//if wrap in x, else if not		
	}//linInterpNodes
	
	/**
	 * this function will move an amount of material chosen to be deposited forward some amount based on the velocity at the node, some random amount between 0 and 1 
	 * representing where in the column of fluid the particle is depositing from, and some constant negative velocity based on the amount of the material being 
	 * deposited and gravity.  once the destination is calculated, deposited amount will be shared amongst the 4 nodes it is found to land in the midst of.
	 * @param srcHNode the source of the velocity.  general algorithm is similar to the transport of sediment in suspension in the water.
	 * @param sedToDrop amount of sediment to lift up
	 * @param delT time step
	 */
	public void advectSedDep(myHeightNode srcHNode, myVector depDir, double sedToDrop, double delT){
		//idx in result ara
		myHeightNode[] cornerNodesAra = new myHeightNode[4];
		double //newX, newZ, 
		diffx, diffz;//values of 4 neighbors to newXIDX/newZIDX coord
		//4 nodes surrounding source of sediment
			//find multiplier for height - is going to be used to "smear" deposit based on random height in column of water where deposit will be coming from
			//sed particle size affects surface area to mass/vol ratio, causing them to drop slower or faster
		//sedDepAdvectAngle
		if(depDir.sqMagn != 0){	
			depDir._div(depDir._mag());
			if(Math.abs(depDir.sqMagn - 1) > myConsts.epsValCalc ) {gui.print2Cnsl("error in advect sed dep - deposit dir vector not magnitude 1 : " + depDir.sqMagn );}
		//	gui.print2Cnsl("non-0 vel vector in advect sed dep");			
		}//retain direction, but should make unit length
		//else {gui.print2Cnsl("\t0 mag vel vector in advect sed dep");}
		
		double randVal = Math.random();
		double waterHeight = srcHNode.getHeightWater(2);
		double weightedHeight = ((1-(Math.log((randVal * waterHeight)+1) / Math.log(waterHeight+1)))/2.0) * waterHeight;
		depDir._mult(weightedHeight);
		double[] diffVals = findDiffValsForNodeAra(srcHNode.source.coords, depDir, cornerNodesAra, delT, 1);

		diffx = diffVals[0];
		diffz = diffVals[1];
//		newX  = diffVals[2];
//		newZ  = diffVals[3];				
		
		this.calc2ndOrderSpreadDeposit(diffx, diffz, sedToDrop, cornerNodesAra);
	}//calc2ndOrderAdvectSedDeposition 
	
	/**
	 * this will find, from a given x,z coordinate, the velocity at that coordinate by looking at its 3 neighbors
	 * @param posX the delta dist in x dir
	 * @param posZ the delta dist in z dir
	 * @param srcHNode the node we use as the base coordinate
	 * @return an array holding the x and z components of the velocity at the passed location
	 */
	public myVector calc2ndOrderGetVelocityAtPos(double diffx, double diffz, myHeightNode[] cornerNodesAra){
		myVector lxlzVal = cornerNodesAra[lowXlowZ].getVelocity(), 
				lxhzVal = cornerNodesAra[lowXhighZ].getVelocity(), 
				hxlzVal = cornerNodesAra[highXlowZ].getVelocity(), 
				hxhzVal = cornerNodesAra[highXhighZ].getVelocity();
		return this.GetInterpVal(diffx, diffz, lxlzVal, lxhzVal, hxlzVal, hxhzVal, false);
	}//calc2ndOrderGetVelocityAtPos
	
	/**
	 * this will spread deposit depVal among 4 nodes held in cornerNodesAra based on the passed interpolants
	 * @param diffxIDX
	 * @param diffzIDX
	 * @param depVal
	 * @param cornerNodesAra
	 */
	public void calc2ndOrderSpreadDeposit(double diffx, double diffz, double depVal, myHeightNode[] cornerNodesAra){
		cornerNodesAra[lowXlowZ].addHeightByVal(((1-diffx) * (1-diffz) * depVal));
		cornerNodesAra[lowXhighZ].addHeightByVal(((1-diffx) * (diffz) * depVal));
		cornerNodesAra[highXlowZ].addHeightByVal(((diffx) * (1-diffz) * depVal));
		cornerNodesAra[highXhighZ].addHeightByVal(((diffx) * (diffz) * depVal));		
	}
	
	/**
	 * checks if a particular new coordinate on the grid surpasses a wrap-around boundary, and if so deals with it
	 * @param wrapAra boolean array used to pass a needed value by reference
	 * @param newXIDX the coordinate - a single x, y or z value
	 * @param araConst which coord constant to use as a source for comparison to min and max
	 * @return the newly modified x (if it needs to be modified) and whether or not this is a wrap around array in wrapAra
	 */	
	public double checkIfWrap(boolean[] wrapAra, double newX, int araConst){
		wrapAra[0] = false;
		if (p.allFlagsSet(myConsts.wrapAroundMesh)){
			if(newX > (p.globMaxAra[araConst])) {//calculated x location is greater than max coord - will either wrap around mesh or pull from between highet and lowest node in wrap 		
				newX = (p.globMinAra[araConst] - ((p.globMaxAra[araConst] + 1) - newX));
				wrapAra[0] = true;				
			}	
			else if(newX < (p.globMinAra[araConst] )) {//wrap	
				newX = (p.globMaxAra[araConst] + (newX - (p.globMinAra[araConst] - 1)));	
				wrapAra[0] = true;				
			}
		}//if wrap around mesh		
		return newX;		
	}//checkIfWrap

	/**
	 * determine the appropriate interpolants for x and z given a particular node, velocity and velocity direction (+/-1 for forward/back step), for calculating the correct location of the resultant
	 * vector between 4 adjacent nodes.  this also will return the correct nodes in the cornernodesara structure
	 * @param srcHNode
	 * @param srcVel
	 * @param cornerNodesAra 
	 * @param delT
	 * @param velDir
	 * @return
	 */
	public double[] findValidBoatLocs(myVector srcHNodeCoords){
		double[] resAra = new double[4];		//idx 0 : diffxIDX, 1:diffzIDX, 2:newx, 3:newz

		resAra[0] = srcHNodeCoords.x;		//location in the mesh in x and z where sediment is coming from
		resAra[1] = srcHNodeCoords.y;
		resAra[2] = srcHNodeCoords.z;

		boolean[] betweenWrapNodesX = new boolean[1];	//using an array to pass by reference		
		resAra[0] = checkIfWrap(betweenWrapNodesX, resAra[0], myConsts.COORD_X);	
		resAra[2] = checkIfWrap(new boolean[]{false}, resAra[2], myConsts.COORD_Z);	
		
		if (betweenWrapNodesX[0]){//wrap around in x, so recalculate y value
			resAra[0] += 1;
			resAra[1] += this.heightMapAra[this.heightMapAra.length-1][0].getAvgNeighborHeightDiff(0);
		}
		
		return resAra;
	}//findDiffValsForNodeAra	
	
	/**
	 * determine the appropriate interpolants for x and z given a particular node, velocity and velocity direction (+/-1 for forward/back step), for calculating the correct location of the resultant
	 * vector between 4 adjacent nodes.  this also will return the correct nodes in the cornernodesara structure
	 * @param srcHNode
	 * @param srcVel
	 * @param cornerNodesAra 
	 * @param delT
	 * @param velDir
	 * @return
	 */
	public double[] findDiffValsForNodeAra(myVector srcHNodeCoords, myVector srcVel, myHeightNode[] cornerNodesAra, double delT, int velDir){
		double[] resAra = new double[4];		//idx 0 : diffxIDX, 1:diffzIDX, 2:newx, 3:newz
		double 	delVx = (srcVel.x * delT * velDir),	//velDir is negative for sed transport, positive for sed deposit
				delVz = (srcVel.z * delT * velDir);
		
		resAra[newXIDX] = srcHNodeCoords.x + delVx;		//location in the mesh in x and z where sediment is coming from
		resAra[newZIDX] = srcHNodeCoords.z + delVz;

		boolean[] betweenWrapNodesX = new boolean[1];	//using an array to pass by reference		
		resAra[newXIDX] = checkIfWrap(betweenWrapNodesX, resAra[newXIDX], myConsts.COORD_X);	
				
		this.linInterpNodes(resAra[newXIDX], resAra[newZIDX], betweenWrapNodesX[0], cornerNodesAra);
		//by here we have the 4 nodes around the location newXIDX,newZIDX
	
		//interpolate velocity from resAra nodes
			//get decimal part for interpolant
		resAra[diffxIDX] = resAra[newXIDX] % 1;
		resAra[diffzIDX] = resAra[newZIDX] % 1;		
		
		//make sure low and high coords are legal		
		if (resAra[diffxIDX] < 0){	resAra[diffxIDX] +=1;}// gui.print2Cnsl("diffxIDX less than 0 : "+ diffxIDX + " newx : " + newXIDX); //handles wrap-around 
		if (resAra[diffzIDX] < 0){  resAra[diffzIDX] +=1;}// gui.print2Cnsl("diffzIDX less than 0 : "+ diffzIDX + " newz : " + newZIDX);

		if((resAra[diffzIDX] > 1) || (resAra[diffxIDX] > 1)){			gui.print2Cnsl("error - diff too big " + " diffzIDX : " + resAra[diffzIDX] + " | diffxIDX : " + resAra[diffxIDX]);	} 
		else if((resAra[diffzIDX] < 0) || (resAra[diffxIDX] < 0)){	gui.print2Cnsl("error - diff too small " + " diffzIDX : " + resAra[diffzIDX] + " | diffxIDX : " + resAra[diffxIDX]);	}			
		
		return resAra;
	}//findDiffValsForNodeAra
	
	/**
	 * given a passed x/z location in the mesh, return the interpolated fluxForce and other forces (gravity and buoancy maybe?) at that location
	 */
	public myVector getForceAtLocation(myVector coords, double mass, double vol){
		double attrK = 10, replK = 5;//repulsion constant
		myHeightNode[] cornerNodesAra = new myHeightNode[4];
		//calculate the interpolants and the new x/z idxs corresponding to the coords being passed - this returns the 4 corner nodes around the passed coords
		double[] diffVals = findDiffValsForNodeAra(coords, myVector.ZERO.cloneMe(), cornerNodesAra, 0, 0);//by passing 0 as direction, we are getting interpolated values at passed coords			
	
		myVector lxlzVal =  (cornerNodesAra[lowXlowZ]   == null) ? myVector.ZERO.cloneMe() : cornerNodesAra[lowXlowZ].getFluxForce(); 
		myVector lxhzVal =  (cornerNodesAra[lowXhighZ]  == null) ? myVector.ZERO.cloneMe() : cornerNodesAra[lowXhighZ].getFluxForce(); 
		myVector hxlzVal =  (cornerNodesAra[highXlowZ]  == null) ? myVector.ZERO.cloneMe() : cornerNodesAra[highXlowZ].getFluxForce(); 
		myVector hxhzVal =  (cornerNodesAra[highXhighZ] == null) ? myVector.ZERO.cloneMe() : cornerNodesAra[highXhighZ].getFluxForce();
		
		myVector result = this.GetInterpVal(diffVals[0], diffVals[1], lxlzVal, lxhzVal, hxlzVal, hxhzVal, false);	//add flux force to forces on this boat - flux force determines boat direction

		//for each other boat, exert repelling force if within 3 units of each other and attracting force if greater than 9
		for(mySailBoat b : p.sailBoatList.values()){
			myVector boatCoords = b.getCoordsAra()[0].cloneMe(), dirVec = myVector._sub(boatCoords, coords);
			if((dirVec.sqMagn > 0) && (dirVec.sqMagn < 9)){//too close to another boat, but not the same boat - apply repulsive force
				double multiplier = mass * b.getMass() * replK/dirVec.sqMagn;
				dirVec._normalize();
				myVector repelForce = myVector._mult(dirVec,-1* multiplier);
				//gui.print2Cnsl("repel force between boat at :" + coords + " and boat id : " + b.ID + " at coords : " + boatCoords);
				result._add(repelForce);				
			}//		
			if(dirVec.sqMagn > 81){//too close to another boat, but not the same boat - apply repulsive force
				double multiplier = mass * b.getMass() * attrK/dirVec.sqMagn;
				dirVec._normalize();
				myVector repelForce = myVector._mult(dirVec, multiplier);
				//gui.print2Cnsl("repel force between boat at :" + coords + " and boat id : " + b.ID + " at coords : " + boatCoords);
				result._add(repelForce);				
			}//		
		}
		
		result._add(myVector._mult(myConsts.gravVec, mass));	
		//get absolute water height at location to determine buoyancy - put value in x so that it will be calculated
		lxlzVal = new myVector(0, (cornerNodesAra[lowXlowZ]   == null) ? 0 : cornerNodesAra[lowXlowZ].getHeightWater() + cornerNodesAra[lowXlowZ].source.coords.y,     0); 
		lxhzVal = new myVector(0, (cornerNodesAra[lowXhighZ]  == null) ? 0 : cornerNodesAra[lowXhighZ].getHeightWater() + cornerNodesAra[lowXhighZ].source.coords.y,   0); 
		hxlzVal = new myVector(0, (cornerNodesAra[highXlowZ]  == null) ? 0 : cornerNodesAra[highXlowZ].getHeightWater() + cornerNodesAra[highXlowZ].source.coords.y,   0); 
		hxhzVal = new myVector(0, (cornerNodesAra[highXhighZ] == null) ? 0 : cornerNodesAra[highXhighZ].getHeightWater() + cornerNodesAra[highXhighZ].source.coords.y, 0);		
		
		myVector interpYVal = this.GetInterpVal(diffVals[0], diffVals[1], lxlzVal, lxhzVal, hxlzVal, hxhzVal, true);
		//at or below the surface of the water, add buoyancy force = weight(force) of displaced fluid to keep from sinking further
		if(coords.y-.5 <= interpYVal.y){
			double bRes = (mass * vol) * Math.min((interpYVal.y - coords.y+.5), 1);
			result._add(myVector._mult(myConsts.gravVec, (-1 * bRes)));//buoyancy in negative gravity dir	
		}
		result._mult(1.0/p.getDeltaT());
		return result;		
	}//getForceAtLocation	

	public myVector getWaterAtLocation(myVector coords){		
		myHeightNode[] cornerNodesAra = new myHeightNode[4];
		//calculate the interpolants and the new x/z idxs corresponding to the coords being passed - this returns the 4 corner nodes around the passed coords
		double[] diffVals = findDiffValsForNodeAra(coords, myVector.ZERO.cloneMe(), cornerNodesAra, 0, 0);//by passing 0 as direction, we are getting interpolated values at passed coords			
	
		myVector lxlzVal = new myVector(0,(cornerNodesAra[lowXlowZ]   == null) ? 0 : cornerNodesAra[lowXlowZ].getHeightWater(), 0), 
				lxhzVal = new myVector(0, (cornerNodesAra[lowXhighZ]  == null) ? 0 : cornerNodesAra[lowXhighZ].getHeightWater(), 0), 
				hxlzVal = new myVector(0, (cornerNodesAra[highXlowZ]  == null) ? 0 : cornerNodesAra[highXlowZ].getHeightWater(), 0),
				hxhzVal = new myVector(0,  (cornerNodesAra[highXhighZ] == null) ? 0 : cornerNodesAra[highXhighZ].getHeightWater(), 0);
		
		myVector result = this.GetInterpVal(diffVals[0], diffVals[1], lxlzVal, lxhzVal, hxlzVal, hxhzVal, true);
		return result;			
		
	}//getWaterAtLocation
	
	/**
	 * given a passed x/z location in the mesh, return the interpolated velocity of the water at that location, including y velocity
	 */
	public myVector getWaterVelAtLocation(myVector coords){
		myHeightNode[] cornerNodesAra = new myHeightNode[4];
		//calculate the interpolants and the new x/z idxs corresponding to the coords being passed - this returns the 4 corner nodes around the passed coords
		double[] diffVals = findDiffValsForNodeAra(coords, myVector.ZERO.cloneMe(), cornerNodesAra, 0, 0);//by passing 0 as direction, we are getting interpolated values at passed coords			
	
		myVector lxlzVal =  (cornerNodesAra[lowXlowZ]   == null) ? myVector.ZERO.cloneMe() : cornerNodesAra[lowXlowZ].getVelocity(); 
		myVector lxhzVal =  (cornerNodesAra[lowXhighZ]  == null) ? myVector.ZERO.cloneMe() : cornerNodesAra[lowXhighZ].getVelocity(); 
		myVector hxlzVal =  (cornerNodesAra[highXlowZ]  == null) ? myVector.ZERO.cloneMe() : cornerNodesAra[highXlowZ].getVelocity(); 
		myVector hxhzVal =  (cornerNodesAra[highXhighZ] == null) ? myVector.ZERO.cloneMe() : cornerNodesAra[highXhighZ].getVelocity();
		
		myVector result = this.GetInterpVal(diffVals[0], diffVals[1], lxlzVal, lxhzVal, hxlzVal, hxhzVal, true);
		return result;		
	}//getWaterVelAtLocation	
	
	/**
	 * given a passed x/z location in the mesh, return the interpolated surface normal of the water at that location
	 */
	public myVector getWaterNormAtLocation(myVector coords){
		myHeightNode[] cornerNodesAra = new myHeightNode[4];
		//calculate the interpolants and the new x/z idxs corresponding to the coords being passed - this returns the 4 corner nodes around the passed coords
		double[] diffVals = findDiffValsForNodeAra(coords, myVector.ZERO.cloneMe(), cornerNodesAra, 0, 0);//by passing 0 as direction, we are getting interpolated values at passed coords			
	
		myVector lxlzVal =  (cornerNodesAra[lowXlowZ]   == null) ? myVector.ZERO.cloneMe() : cornerNodesAra[lowXlowZ].H2O_Norm; 
		myVector lxhzVal =  (cornerNodesAra[lowXhighZ]  == null) ? myVector.ZERO.cloneMe() : cornerNodesAra[lowXhighZ].H2O_Norm; 
		myVector hxlzVal =  (cornerNodesAra[highXlowZ]  == null) ? myVector.ZERO.cloneMe() : cornerNodesAra[highXlowZ].H2O_Norm; 
		myVector hxhzVal =  (cornerNodesAra[highXhighZ] == null) ? myVector.ZERO.cloneMe() : cornerNodesAra[highXhighZ].H2O_Norm;
		
		myVector result = this.GetInterpVal(diffVals[0], diffVals[1], lxlzVal, lxhzVal, hxlzVal, hxhzVal, true);
		return result;		
	}//getWaterNormAtLocation
	
	/**
	 * given a passed x/z location in the mesh, return the interpolated surface normal of the water at that location
	 */
	public myVector getTerrainNormAtLocation(myVector coords){
		myHeightNode[] cornerNodesAra = new myHeightNode[4];
		//calculate the interpolants and the new x/z idxs corresponding to the coords being passed - this returns the 4 corner nodes around the passed coords
		double[] diffVals = findDiffValsForNodeAra(coords, myVector.ZERO.cloneMe(), cornerNodesAra, 0, 0);//by passing 0 as direction, we are getting interpolated values at passed coords			
	
		myVector lxlzVal =  (cornerNodesAra[lowXlowZ]   == null) ? myVector.ZERO.cloneMe() : cornerNodesAra[lowXlowZ].source.N; 
		myVector lxhzVal =  (cornerNodesAra[lowXhighZ]  == null) ? myVector.ZERO.cloneMe() : cornerNodesAra[lowXhighZ].source.N; 
		myVector hxlzVal =  (cornerNodesAra[highXlowZ]  == null) ? myVector.ZERO.cloneMe() : cornerNodesAra[highXlowZ].source.N;
		myVector hxhzVal =  (cornerNodesAra[highXhighZ] == null) ? myVector.ZERO.cloneMe() : cornerNodesAra[highXhighZ].source.N;
		
		myVector result = this.GetInterpVal(diffVals[0], diffVals[1], lxlzVal, lxhzVal, hxlzVal, hxhzVal, true);
		return result;		
	}//getTerrainNormAtLocation
	
	/**
	 * this will return an interpolated value from 4 vectors, interpolated via x and z interpolants
	 * @param posX the delta dist in x dir
	 * @param posZ the delta dist in z dir
	 * @param srcHNode the node we use as the base coordinate
	 * @return an array holding the x and z components of the velocity at the passed location
	 */
	private myVector GetInterpVal(double diffx, double diffz, myVector lxlzVal,myVector lxhzVal, myVector hxlzVal, myVector hxhzVal, boolean interpY ){
		myVector result = new myVector(), valXlowZ = new myVector(), valXHighZ = new myVector();
		
		valXlowZ.x 	= ((1-diffx) * lxlzVal.x)	+ (diffx * hxlzVal.x);
		valXHighZ.x	= ((1-diffx) * lxhzVal.x)	+ (diffx * hxhzVal.x);		
		result.x = ((1-diffz) * valXlowZ.x) + (diffz * valXHighZ.x);		
		
		if (interpY){
			valXlowZ.y 	= ((1-diffx) * lxlzVal.y)	+ (diffx * hxlzVal.y);
			valXHighZ.y	= ((1-diffx) * lxhzVal.y)	+ (diffx * hxhzVal.y);		
			result.y    = ((1-diffz) * valXlowZ.y) + (diffz * valXHighZ.y);			
		}//if interpolating y value too
		
		valXlowZ.z 	= ((1-diffx) * lxlzVal.z)	+ (diffx * hxlzVal.z);
		valXHighZ.z	= ((1-diffx) * lxhzVal.z)	+ (diffx * hxhzVal.z);
		result.z = ((1-diffz) * valXlowZ.z) + (diffz * valXHighZ.z);			
		return result;
	}//calc2ndOrderGetVelocityAtPos	
	/**
	 * depending on whether we are transporting sediment concentration or sediment itself, this will return the correct value
	 * @param node node pulling from
	 * @param sedIDX idx in sed ara for sediment
	 * @param h2oIDX idx in h2o ara for water, if used, otherwise ignored
	 * @return either the sediment or sediment concentration existing at this node
	 */
	private double getSedTransportVal(myHeightNode node, int sedIDX, int h2oIDX){
		if(p.allFlagsSet(myConsts.useSedConc)){			return node.getSedimentConcentration(sedIDX, h2oIDX);} 
		else {											return node.getSediment(sedIDX);}
	}//getSedTransportVal
	
	private void setSedTransportVal(myHeightNode node, int sedIDX, int h2oIDX, double conc){
		if(p.allFlagsSet(myConsts.useSedConc)){			node.setSedimentFromConcentration(sedIDX, h2oIDX, conc);}
		else {											node.setSedimentPipes(conc, sedIDX);}
	}//getSedTransportVal

	
	/**		RETURN EITHER SED OR SED CONC IN IDX 0, H2O IN IDX 1
	 * will return with "dry" node's contribution to sediment, determined by weighted interpolation of dry node's wet neighbors, if any exist
	 * --weight cardinal-dir nodes by 2, diags by 1, sum all weights and normalize
	 * @param dryNode
	 * @return
	 */
	private double[] getSedForDryNode(myHeightNode dryNode, int recLayer, myHeightNode callNode){
		myHeightNode compNode;
		int numRecLayers = 3;
		//double[] result = new double[2];
		int x = dryNode.source.xIDX, z = dryNode.source.zIDX, xIdx, zIdx, multiplier, totDivisor = 0;
		double sedAmt, h2oAmt, totSed = 0,	totH2O = 0;

		for(int xMod = -1; xMod < 2; ++xMod){
			xIdx = x + xMod;		//x is location of drynode, xmod is location of neighbor in x dir
			//for each potential neighbor in the x direction - check if drynode is a wrap around node to the other side of the mesh
			if ((!(dryNode.isMirrorNode(0))) && ((xIdx < 0) || (xIdx >= heightMapAra.length))){				continue; }//at edge for this x and not wrap around, so leave the loop
			if(dryNode.isMirrorNode(0)){//wrap around node	- adjust xIdx	
				if(xIdx < 0) 							{	xIdx = heightMapAra.length-1; }						
				else if (xIdx >= heightMapAra.length) 	{	xIdx = 0; }//wrapping to xidx = 0
			}
			
			for(int zMod = -1; zMod < 2; ++zMod){			//need to check boundaries of mesh for wrap around, or edges without neighbors
				zIdx = z + zMod;
				if ((xMod == 0) && (zMod == 0)){							continue;	}//don't process center node
				if ((zIdx < 0) || (zIdx >= heightMapAra[0].length)){		continue;	}//at edge for this z  leave the loop - no wrap on z				
				compNode = heightMapAra[xIdx][zIdx];
				h2oAmt = compNode.getHeightWater(2);
				sedAmt = this.getSedTransportVal(compNode, 1, 2); //compNode.getSediment(1);  //either sediment or sed conc
//				if((h2oAmt > 0) && (sedAmt > 0)){//wet node with sediment, use to further calculations					
				if(h2oAmt > 0){//wet node, use to further calculations					
					multiplier = (((xMod == 0) || (zMod == 0)) ? 2 : 1); //weight each node's contribution by either 2 or 1 based on location
					totDivisor += multiplier;			//keep total of multipliers, for weighting
					totSed +=  (multiplier * sedAmt);	//keep total of sediment copied from surrounding nodes
					totH2O += (multiplier * h2oAmt);
				}//if not a dry node	
			}//for z
		}//for x
		
		//perform calculation
		if(totDivisor == 0){//means a dry node surrounded by dry nodes - tot sed and toth2o should also be 0
			//recursively call this routine with this node's adjacent nodes, and then have their results contribute to build this node's results
			totSed = 0;
			totH2O = 0;
			if(recLayer < numRecLayers){									//set max # of recursive layers for any node to be less than 3
				for(int xMod = -1; xMod < 2; ++xMod){
					xIdx = x + xMod;
					if ((!(dryNode.isMirrorNode(0))) && ((xIdx < 0) || (xIdx >= heightMapAra.length))){				continue; }//at edge for this x  leave the loop
					if(dryNode.isMirrorNode(0)){//wrap around node	- adjust xIdx	
						if(xIdx < 0) 							{	xIdx = heightMapAra.length-1; }						
						else if (xIdx >= heightMapAra.length) 	{	xIdx = 0; }//wrapping to xidx = 0
					}
					for(int zMod = -1; zMod < 2; ++zMod){			//need to check boundaries of mesh for wrap around, or edges without neighbors
						zIdx = z + zMod;
						if (((xMod == 0) && (zMod == 0)) || ((zIdx < 0) || (zIdx >= heightMapAra[0].length))){		continue;	}//at edge for this z  leave the loop - no wrap on z, also don't process center node					
						compNode = heightMapAra[xIdx][zIdx];  									//dry neighbor of current dry node - recurse through all neighbors to find a wet node to contribute to this node's amounts
						if (compNode.ID == dryNode.ID) { continue;}								//don't check out calling node, we know this one is empty
						double[] results = new double[2];
						results = getSedForDryNode(compNode, recLayer + 1, dryNode);				
						sedAmt = results[0];
						h2oAmt = results[1];
						//if((h2oAmt > 0) && (sedAmt > 0)){										//wet node with sediment, use to further calculations					
						if(h2oAmt > 0){										//wet node with sediment, use to further calculations					
							multiplier = (((xMod == 0) || (zMod == 0)) ? 2 : 1); 				//weight each node's contribution by either 2 or 1 based on location
							totDivisor += multiplier;											//keep total of multipliers, for weighting
							totSed += (multiplier * sedAmt);									//keep total of sediment copied from surrounding nodes
							totH2O += (multiplier * h2oAmt);
						}//if not a dry node	
					}//for z
				}//for x
			} //else {}//only search
		}//if totDivisor == 0
		double[] results = new double[2];
		results[0] = (totSed/totDivisor);			//totals for this particular node - if called in recursive loop, this node's contribution will be lessened again
		results[1] = (totH2O/totDivisor);		
		return results;
	}//getSedForDryNode
	
	/**
	 * --uses 2d ara--
	 * fast method for pipes transport of sediment, using stam-y lagrangian method - put in each node at srcIDX + 1
	 * calculate by finding the sediment at t + delt that came to this node during t time step from a node at x - u*delT, z - v*delT
	 * @param srcHNode the source node to draw information from about sediment
	 * @param srcHNodeOld the old source node - gets copy of result
	 * @param xCoords array of all x coords of this mesh - in order
	 * @param zCoords array of all z coords of this mesh - in order
	 * @param delT time step	
	 * @param srcIDX index to pull sed from
	 */
	public void calc2ndOrderPipesSedTransportAra(myHeightNode srcHNode, double delT){
		//idx in result ara
		myHeightNode[] resAra = new myHeightNode[4];
		for(int i = 0; i<4; ++i){resAra[i] = null;}
		double newSedXLowZ, newSedXHighZ, newX, newZ,diffx, diffz,
		srcH2O = srcHNode.getHeightWater(2),
				LLh2o,LHh2o,HLh2o,HHh2o,
		srcSed = srcHNode.getSediment(1),
		newSedlowXlowZ = 0,newSedhighXlowZ = 0, newSedlowXhighZ = 0,newSedhighXhighZ = 0,newSedVal = 0;//values of 4 neighbors to newXIDX/newZIDX coord
		
		//if(srcSed > srcH2O){	gui.print2Cnsl("2) : more sed than water in transport : "  + srcSed + " > " + srcH2O + " on node : " + srcHNode.ID + " at x,z idx : " + srcHNode.source.xIDX + "," + srcHNode.source.zIDX);	}
			//only used by boundary nodes
		boolean boundLLConv = false, boundLHConv = false,	boundHLConv = false,	boundHHConv = false;
		
		//calculates newXIDX,Z and diffxIDX,z for this srcHNode and its velocity, also populates resAra with 4 appropriate nodes that surround the back-step location 
		//idx in diff vals : 0 = diffxIDX, 1 = diffzIDX, 2 = newXIDX, 3 = newZIDX
		myVector srcVel =  srcHNode.getVelocity();
		double[] diffVals = findDiffValsForNodeAra(srcHNode.source.coords, srcVel, resAra, delT, -1);//-1 means backwards step

		diffx = diffVals[0]; 	diffz = diffVals[1]; 	newX  = diffVals[2]; 	newZ  = diffVals[3];
			//sed values are all inited to 0

		LLh2o = 1;		HHh2o = 1;	HLh2o = 1;	LHh2o = 1;

		if(resAra[lowXlowZ] 	!= null){		newSedlowXlowZ  =  getSedTransportVal(resAra[lowXlowZ], 1, 2); 		LLh2o = resAra[lowXlowZ].getHeightWater(2);}				
		if(resAra[highXhighZ] 	!= null){		newSedhighXhighZ = getSedTransportVal(resAra[highXhighZ], 1, 2);	HHh2o = resAra[highXhighZ].getHeightWater(2);}	
		if(resAra[highXlowZ] 	!= null){		newSedhighXlowZ = getSedTransportVal(resAra[highXlowZ], 1, 2);		HLh2o = resAra[highXlowZ].getHeightWater(2);}			
		if(resAra[lowXhighZ] 	!= null){		newSedlowXhighZ  = getSedTransportVal(resAra[lowXhighZ], 1, 2);		LHh2o = resAra[lowXhighZ].getHeightWater(2);}						

		//if nodes to pull from are dry and not outside bounds of mesh
		if(p.allFlagsSet(myConsts.pullSedFromDryNodes)){
			if((resAra[lowXlowZ] != null)  && (newSedlowXlowZ == 0) && (resAra[lowXlowZ].getHeightWater(2) == 0)){ 		//sanity check - this node has no water and no sed-
				double[] dryTransRes = calc2ndOrderSedTransDryNode(lowXlowZ, resAra, srcH2O, srcSed);
				newSedlowXlowZ 	= dryTransRes[0];			//sed or sedConc
				LLh2o 			= dryTransRes[1];			
				boundLLConv = p.allFlagsSet(myConsts.erodeDryNodes);//true;
			}
			if((resAra[lowXhighZ] != null) && (newSedlowXhighZ == 0) && (resAra[lowXhighZ].getHeightWater(2) == 0)){//* 1000 < srcH2O)){ 		//sanity check - this node has no water
				double[] dryTransRes = calc2ndOrderSedTransDryNode(lowXhighZ, resAra, srcH2O, srcSed);
				newSedlowXhighZ = dryTransRes[0];			//sed or sed conc
				LHh2o 			= dryTransRes[1];
				
				boundLHConv = p.allFlagsSet(myConsts.erodeDryNodes);//true;
			}
			if((resAra[highXlowZ] != null) && (newSedhighXlowZ == 0) && (resAra[highXlowZ].getHeightWater(2) == 0)){ 		//sanity check - this node has no water
				double[] dryTransRes = calc2ndOrderSedTransDryNode(highXlowZ, resAra, srcH2O, srcSed);
				newSedhighXlowZ = dryTransRes[0];			//sed or sed conc
				HLh2o 			= dryTransRes[1];
				boundHLConv = p.allFlagsSet(myConsts.erodeDryNodes);//true;			
			}
			if((resAra[highXhighZ] != null) && (newSedhighXhighZ == 0) && (resAra[highXhighZ].getHeightWater(2) == 0)){ 		//sanity check - this node has no water
				double[] dryTransRes = calc2ndOrderSedTransDryNode(highXhighZ, resAra, srcH2O, srcSed);
				newSedhighXhighZ = dryTransRes[0];			//sed or sed conc
				HHh2o 			 = dryTransRes[1];		
				boundHHConv = p.allFlagsSet(myConsts.erodeDryNodes);//true;
			}		
		}


			//record amount of sediment/sed concentration moved and also redeposit unused sediment from dry nodes - converts back to sediment in this method if sed conc
		if(resAra[lowXlowZ] != null) { 		this.calc2ndOrderSedTransAmt((1-diffx),  (1-diffz), newSedlowXlowZ, resAra[lowXlowZ], LLh2o, boundLLConv);}//			sedInterpMult = (1-diffxIDX) * (1-diffzIDX);
		if(resAra[lowXhighZ] != null) {		this.calc2ndOrderSedTransAmt((1-diffx), diffz, newSedlowXhighZ, resAra[lowXhighZ], LHh2o,  boundLHConv);}		
		if(resAra[highXlowZ] != null) {		this.calc2ndOrderSedTransAmt(diffx, (1-diffz), newSedhighXlowZ, resAra[highXlowZ], HLh2o, boundHLConv);}
		if(resAra[highXhighZ] != null) { 	this.calc2ndOrderSedTransAmt(diffx, diffz, newSedhighXhighZ, resAra[highXhighZ], HHh2o, boundHHConv);}

		srcHNode.setSedSource(new myVector(newX, srcHNode.source.coords.y, newZ));		

		//interpolation for low z and high z first
		newSedXLowZ 	= ((1-diffx) * newSedlowXlowZ)	+ (diffx * newSedhighXlowZ);
		newSedXHighZ 	= ((1-diffx) * newSedlowXhighZ)	+ (diffx * newSedhighXhighZ); 
		
		newSedVal = ((1-diffz) * newSedXLowZ) + (diffz * newSedXHighZ);
		
		setSedTransportVal(srcHNode, 2, 2, newSedVal);			//set either sediment transported or sediment concentration transported as sediment on destination node
	}//calc2ndOrderPipesSedTransport	
	
	/**
	 * determine the sediment/sed conc amt to use from a dry node based on the nearest wet nodes' values
	 * @param dryNodeIDX
	 * @param nodeAra			//need to not go beyond edge of mesh
	 * @param srcH2O
	 * @param srcSed
	 * @return
	 */	
	private double[] calc2ndOrderSedTransDryNode(int dryNodeIDX, myHeightNode[] nodeAra, double srcH2O, double srcSed){
		double[] result = new double[2]; 
		double[] tmpVal;
		double interpSed, waterVal;
		tmpVal =  getSedForDryNode(nodeAra[dryNodeIDX], 0, null);
		//interpSed = ((tmpVal[0] <= 0) || (tmpVal[1] <= myConsts.epsValCalc ) ? srcSed : tmpVal[0]);// /(((p.allFlagsSet(myConsts.useSedConc))) ? (tmpVal[1]) : 1));
		interpSed = tmpVal[0];// /(((p.allFlagsSet(myConsts.useSedConc))) ? (tmpVal[1]) : 1));
		//LLh2o = 1;
		//waterVal = (((p.allFlagsSet(myConsts.useSedConc))) ? ((tmpVal[0] <= 0) || (tmpVal[1] <= myConsts.epsValCalc )  ? srcH2O : tmpVal[1]) : 1);
		waterVal = (((p.allFlagsSet(myConsts.useSedConc))) ? (tmpVal[1]) : 1);//either amt of water here or 1, depending on concentration calculation or pure sediment calculation
		if((interpSed < 1000) && (interpSed >= 0)){
			result[0] = interpSed;		
			result[1] = waterVal;
			convertHeightToSed(nodeAra[dryNodeIDX], nodeAra[dryNodeIDX], p.allFlagsSet(myConsts.erodeDryNodes) ? interpSed : 0, interpSed, false, false);
		} else {
			//p.setFlags(myConsts.simulate, false);
			//if(p.allFlagsSet(myConsts.debugMode)){	gui.print2Cnsl("3):srcSed/conc and h2o : " + gui.df7.format(srcSed)+" & "+ gui.df7.format(srcH2O)	+ " sed/conc !trans : " + interpSed + " water !trans : " + result[1]);}
			result[0] = 0;
			result[1] = 1;
		}
		return result;		
	}//calc2ndOrderSedTransDryNode
	
	/**
	 * set amount of either sediment or sediment concentration being transported
	 * 
	 * @param diffx
	 * @param diffz
	 * @param newSedAmt
	 * @param interpNode
	 * @param boundaryConv
	 */	
	private void calc2ndOrderSedTransAmt(double diffx, double diffz, double origSedAmt, myHeightNode interpNode, double interpH2O, boolean boundaryConv){
		double sedInterpMult, transAmt, reDepAmt;
		sedInterpMult = diffx * diffz;
		transAmt = sedInterpMult * origSedAmt * (((p.allFlagsSet(myConsts.useSedConc))) ? interpH2O : 1);//multiply by water at dest node to convert back to sediment
		interpNode.addSedTransported(transAmt);			//sedTransported is not used for any primary sediment calculations, only transportation and sed debugging/display
		if(boundaryConv){								//if we are depositing in the transport step to account for eroding the dry land we get the sediment from					
			reDepAmt = -1 * (1-sedInterpMult) * origSedAmt * (((p.allFlagsSet(myConsts.useSedConc))) ? interpH2O : 1);//multiply by amt of water if using sed conc
			convertHeightToSed(interpNode, interpNode, p.allFlagsSet(myConsts.erodeDryNodes) ? reDepAmt : 0 , reDepAmt, false, false);		//deposit either dep amt or 0 to height of node
		}//means some amount of this node was converted to sediment to act as a transportation source	
	}//calc2ndOrderSetTransAmt	

	/**
	 * REPLACED with alt method using sed cap
	 */
	public void calc2ndOrderPipesLateralErosionVals(myHeightNode srcHNode, double delT){
		double angleDiff, locModAmt, wAngleModRatio, totModAmt = 0;							//angle between src node and adj node that is a boundary node
		myVector srcNormVelocity = new myVector(srcHNode.getVelocity());											//copy of velocity vector
		myHeightNode adjHNode;
		srcNormVelocity._normalize();			//get direction of velocity
		//double velocityAngle = (srcNormVelocity.z >= 0) ? (Math.acos(srcNormVelocity.x)) : ((2*Math.PI) - Math.acos(srcNormVelocity.x));							//give angle in radians
		double delVol = (srcHNode.calcChangeInVal(myConsts.H2O, 0, 2) ) / delT;						//delta volume is change in volume divided by delta t
		myVector impactForce = myVector._mult(srcHNode.getVelocity(), delVol);						//impactForce is vector force to be applied to boundary nodes 
		for(Integer idx : srcHNode.getBoundaryAdjNodeDirByID().keySet()){							//for each index of node that is boundary node to this node
			
			adjHNode = srcHNode.getAdjNodes().get(idx);
			myVector terNorm = adjHNode.source.N;			//vertex normal of terrain
			angleDiff = myVector._angleBetween(srcNormVelocity, terNorm) - (Math.PI);				
			if (angleDiff > Math.PI) { angleDiff = -1*((2*Math.PI) - angleDiff); }                   			//normalize to be between PI and -PI
			wAngleModRatio = Math.cos(angleDiff);
			wAngleModRatio = ((wAngleModRatio > 0) ? wAngleModRatio : 0);   									//velocity behind the node does nothing

			if ( //(heightDiff < 0) &&																	//if adj node's height greater than this node's height (if not then not a mass-wasting boundary)
				 (wAngleModRatio > 0) ){																//if direction of velocity at this node impacting adj node
				locModAmt = (mySqSqEroder.MassWasting_C * impactForce._mag() * Math.abs(srcHNode.getAdjHeightDiffsTerByIDX(idx)) * delT) * wAngleModRatio  * this.globLatErosionMult;     //delT is timestep multiplier for sim 
				adjHNode.addHeightByVal(-1 * locModAmt);  
				totModAmt += locModAmt;
			}//only apply force if greater in height and if in direction from this node to be affected by velocity
		//add to idx 0 so that node can be affected by this turn's erosion calculations
			//put some on node as sediment, rest as altitude
			double totAmtSed = srcHNode.lclKs * totModAmt;
			srcHNode.addSediment(totAmtSed, 2, 2);
			srcHNode.addHeightByVal(totModAmt - totAmtSed);
		//	srcHNode.calcHeight(0);		
		}//for 
	}//calc2ndOrderPipesLateralErosion
	
	/**
	*  calculate the effects of various types of erosion, based on passed type parameter - old, based on hashmap, replaced by ara version
	*/
//	public void calc2ndOrderErosion(String type, double delT){
//		//--check each heightNode's corresponding source vertex's adjacent list to find neighbors
//		if (this.heightMap != null){
//			this.globalKVal = 1;
//			p.resetGTotValAra(myConsts.SED);
//			p.resetGTotValAra(myConsts.SEDCAP);
//			p.resetAllMinMaxArrays();
//			p.resetAllTurnTotValArrays();
//			gui.setNotifyLights(type);    
//				//move water to idx 1
//			rainPipesOld(delT, 1, 0);  
//			//cross section of virtual pipe - circular pipe of radius deltaNodeDist/2
//			double nodeArea = sub.getDeltaNodeDist() * sub.getDeltaNodeDist();
//			double pXSection = nodeArea;//((nodeArea)/4)*Math.PI;	
//			//next set all pipe flux values   
//			for (Integer IDX : this.heightMap.keySet()){//get each key and value from height map - keys are unique ints but not necessarily sequential
//			    for (myHeightNode tmpNode : this.heightMap.get(IDX)){                           //get nodes from old deque at heightmap at position idx - prevent concurrent mod
//				   // tmpNode.setTiltComponent(0);//clearing out tilt component for this node.
//				    if (type.equals("ThermalWeathering")){        	
//				    	calc2ndOrderAeolianErosion(tmpNode, this.globWindAngle, delT); 
//				    	tmpNode.addWaterPipes (0, 1, 2);//move water into idx 2 so that it is preserved
//				    } 
//				    else if ((type.equals("HydraulicErosion"))  && (tmpNode.getHeightWater() != 0))	//ignore nodes with no water
//				    								 {       	calc2ndOrderPipesFluxVals(tmpNode, pXSection, nodeArea,delT);}	         //setup pipes
//				    else if(type.equals("Rain")){
//				    	if((tmpNode.source.isRainOnMe()) || (tmpNode.getHeightWater() > 0)){
//				    		tmpNode.addWaterPipes (this.globRaindropMult, 1, 2);
//				    		p.addToTotalVal(myConsts.H2O, this.globRaindropMult);	    		
//				    	}//if node rainonme		
//				    }//if type is rain					    
//			    }//for each node in deque			    
//			}//for each k-v pair in heightmap   
//			
//				//now deliver water via pipes, use tmpHeightMap as source for nodes, tmpHeightMap2 as dest
//			if (type.equals("HydraulicErosion")){//if not performing aeolian on this call
//				if(p.allFlagsSet(myConsts.useLowestKGlobally)){
//					for (Integer IDX : this.heightMap.keySet()){//get each key and value from height map - keys are unique ints but not necessarily sequential
//						for (myHeightNode tmpNode : this.heightMap.get(IDX)){                           //get nodes from old deque at heightmap at position idx - prevent concurrent mod
//				
//							calc2ndOrderPipesFluxValsApplyGlobalK(tmpNode, pXSection, nodeArea,delT);
//						}	         //setup pipes via global least k val
//					}
//				}//use lowestkglobally
//				
//				for (Integer IDX : this.heightMap.keySet()){//get each key and value from height map - keys are unique ints but not necessarily sequential
//					for (myHeightNode tmpNode : this.heightMap.get(IDX)){                           //get nodes from old deque at heightmap at position idx - prevent concurrent mod
//							//water transport   
//						calc2ndOrderPipesWaterVals(tmpNode, nodeArea, delT);						
//							//erosion calculation - pickup, deposit
//						tmpNode.calcPipesSedErosionDeposition(delT, false, p.allFlagsSet(myConsts.useSedFlux), 2);
//					}//for each node in map
//				}//for each k-v pair in heightmap
//				//now calculate sediment transport by stam-y semi-lagrangian method - needed to have entire grid of sediment 
//				//calculated to determine sediment since velocity is used to determine where sediment came from - advection ala stam, via euler backstep.
//				for (Integer IDX : this.heightMap.keySet()){//get each key and value from height map - keys are unique ints but not necessarily sequential
//					for (myHeightNode tmpNode : this.heightMap.get(IDX)){                           //get nodes from old deque at heightmap at position idx - prevent concurrent mod
//							//recalculate node's normal - used in mass wasting
//						tmpNode.source.calcNormal(); 						
//							//calculate mass-wasting effect - water impacts on side of terrain causing terrain to collapse
//						if (p.allFlagsSet(myConsts.calcLateralErosion) && 
//								(tmpNode.isBoundaryNode())){	
//							this.calc2ndOrderPipesLateralErosionVals(tmpNode, delT);}//handle if this node is a boundary node - bang into neighbors							
//							//erosion calculation : sediment transport, using stam-y lagrangian method
//						if(tmpNode.getKc() != 0){
//							calc2ndOrderPipesSedTransport(tmpNode, delT);
//						}
//					}//for each node in map
//				}//for each k-v pair in heightmap
//			}//if type is HydraulicErosion
//			
//				//move all calculated values to base locations for next time step
//			for (Integer IDX : this.heightMap.keySet()){//get each key and value from height map - keys are unique ints but not necessarily sequential
//				for (myHeightNode tmpNode : this.heightMap.get(IDX)){                           //get nodes from old deque at heightmap at position idx - prevent concurrent mod
//					tmpNode.setNewValsPipes(tmpNode, false);                                  //get accurate values in original node's base idx's
//				}
//			}//for each node
//			//recalculate all node geometries
//		for (Integer IDX : this.heightMap.keySet()){//get each key and value from height map - keys are unique ints but not necessarily sequential
//			for (myHeightNode tmpNode : this.heightMap.get(IDX)){                           //get nodes from old deque at heightmap at position idx - prevent concurrent mod
//				tmpNode.calcNodeGeometryOld(false);
//			}
//		}//for each node			
//	    	//recalc geometry given new values for this node
//				//find difference value for sediment between transport and eroded/deposited, put in SEDCAP idx of totalTurnVal ara
//			p.setTotalTurnVal(myConsts.SEDCAP, p.globTotValAra[myConsts.SED] - p.globTurnTotValAra[myConsts.SED]);					
//			//greg's bandaid solution for sediment loss - add sediment back to node scaled by ratio of total eroded to total not transported in transport stage
//			if(p.allFlagsSet(myConsts.scaleSedPipes)){
//				for (Integer IDX : this.heightMap.keySet()){//get each key and value from height map - keys are unique ints but not necessarily sequential
//					for (myHeightNode tmpNode : this.heightMap.get(IDX)){                           //get nodes from old deque at heightmap at position idx - prevent concurrent mod
//						tmpNode.scaleSedimentPipes(0,((p.globTotValAra[myConsts.SED])/(p.globTurnTotValAra[myConsts.SED])));         //get accurate values in original node's base idx's
//					}
//				}//for each clone vert   
//			}//if scale sed		
//		}//if not null heightmap
//	}//calcErosion func
	
	/**
	*  rate that rain falls per unit time - multiplied by delta t in rainPipes controlling rain elsewhere 
	*/
	public double rainRate(double x, double y){ return 0; }//1.0 * globRaindropMult; }

	/**
	 * REDONE TO SUPPORT 2d heightmapara
	*  simulates rain on every vertex for Pipes model's hydraulic erosion simulation
	*  currently adds 0, to preserve integrity of pipe-length calculation, and because we add rain elsewhere
	*  so this only serves to move rain from idx 0 to idx 1
	*  @param delT - current deltaT (time-step size)
	*/
	public void rainPipesOld(double delT, int idx, double rainToAdd){
		for (Integer IDX : this.heightMap.keySet()){//get each key and value from height map - keys are unique ints but not necessarily sequential
		    for (myHeightNode tmpNode : this.heightMap.get(IDX)){  if((tmpNode.source.isRainOnMe()) || (tmpNode.getHeightWater() > 0)){   		tmpNode.addWaterPipes (rainToAdd, 0, idx);    	}    }                         //get nodes from old deque at heightmap at position idx - prevent concurrent mod
		}
	}//rainPipes - without using old node	
	
	/**
	*  simulates rain on every vertex for Pipes model's hydraulic erosion simulation
	*  currently adds 0, to preserve integrity of pipe-length calculation, and because we add rain elsewhere
	*  so this only serves to move rain from idx 0 to idx 1
	*  @param delT - current deltaT (time-step size)
	*  @param idx - destination idx
	*/
	public void rainPipes(double delT, double rainToAdd){
		if(rainToAdd == 0){//no rain, just moving water
			for(int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){
				for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){
					if (this.heightMapAra[xIDX][zIDX].getHeightWater() > 0){		this.heightMapAra[xIDX][zIDX].addWaterPipes (0, 0, 1);	}	
				}//for zidx
			}//for xidx					
		} else {//raining and moving water
			for(int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){
				for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){
					if ((this.heightMapAra[xIDX][zIDX].source.isRainOnMe())    
						&& (!p.allFlagsSet(myConsts.staggerRainFall))  
							|| ((p.allFlagsSet(myConsts.staggerRainFall)) 
									&& (((xIDX * zIDX) != 0) 
									&& (xIDX != this.heightMapAra.length-1) 
									&& (zIDX != this.heightMapAra[0].length-1)
									&& ((xIDX * zIDX) % this.numRainStormCycles == this.rainStormCycles)
									)))						
							{
						this.heightMapAra[xIDX][zIDX].addWaterPipes (rainToAdd, 0, 1);//adds water and moves to idx
		    			if (p.allFlagsSet(myConsts.simulate)){	//if simulating - means it's actual rain and not water being added to mesh manually
		    				//add velocity to node to calculate how much sediment is generated by raindrop falling
		    				this.heightMapAra[xIDX][zIDX].addVelocity(new myVector(0,-9 * (this.globRaindropMult /(this.globRaindropMult + this.heightMapAra[xIDX][zIDX].getHeightWater(1))),0));
		    				//this.heightMapAra[xIDX][zIDX].calcPipesSedErosionDeposition(delT, false,  p.allFlagsSet(myConsts.useSedFlux),1);
		    				this.heightMapAra[xIDX][zIDX].calcPipesSedErosionDeposition(delT, false, false,1);//will always use 0 as sed src
		    			}
		    			p.addToTotalVal(myConsts.H2O, rainToAdd);	    		
					} 
					//move water on every node that isn't rained on from 0 idx to 1 idx, for pipes method protocol
					else if (this.heightMapAra[xIDX][zIDX].getHeightWater() > 0){		this.heightMapAra[xIDX][zIDX].addWaterPipes (0, 0, 1);	}	
				}//for zidx
			}//for xidx
		}//else raining and moving water
	}//rainPipes - without using old node	
	
	private void calcMidPoint(double nodeArea, double pXSection, double modDelT){
		//calculate flux at delT/2, use this to "move" water (find deltavol), use -this- value to recalc height diff to then use as height diff for original calculation
		for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
			for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
					calc2ndOrderPipesFluxValsMidPoint(this.heightMapAra[xIDX][zIDX], pXSection, nodeArea, modDelT);
			  }	//for z         //setup pipes			    
		}//for x 
	
		for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){
			for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
				calc2ndOrderPipesWaterValsMidPoint(this.heightMapAra[xIDX][zIDX], nodeArea,modDelT);	////water transport   										
			}//for z
		}//for x	
		//now recalculate height differences
		for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){
			for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
				this.heightMapAra[xIDX][zIDX].calcNodeGeometry(false, 4);	//build new height data  										
			}//for z
		}//for x	
	}//calcMidPoint
	
	//perform all 2nd order calculations for hydraulic transport
	private void calc2ndOrderErosionHydraulic(double delT){			
		//cross section of virtual pipe - circular pipe of radius deltaNodeDist/2
		double nodeArea = 1.0;// sub.getDeltaNodeDist() * sub.getDeltaNodeDist();
		double pXSection = .79 * nodeArea;//((nodeArea)/4)*Math.PI;	
		double modDelT = delT*.5;
		//next set all pipe flux values  
		//implement midpoint method
		if(p.allFlagsSet(myConsts.useMidpointMethod)){  		calcMidPoint(nodeArea, pXSection, modDelT);		}//if using midpoint - use to recalc height diffs
		
		for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
			for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
				//this.heightMapAra[xIDX][zIDX].setTiltComponent(0);//clearing out tilt component for this node.
			    	calc2ndOrderPipesFluxVals(this.heightMapAra[xIDX][zIDX], pXSection, nodeArea,delT);
			  }	//for z         //setup pipes			    
		}//for x 
			
		if(p.allFlagsSet(myConsts.useLowestKGlobally)){
			for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
				for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index				
					calc2ndOrderPipesFluxValsApplyGlobalK(this.heightMapAra[xIDX][zIDX], pXSection, nodeArea,delT);	         //setup pipes via global least k val
				}//for z
			}//for x
		}//use if using lowest k globally			
		for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){
			for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
				calc2ndOrderPipesWaterVals(this.heightMapAra[xIDX][zIDX], nodeArea, delT);	////water transport   										
			}//for z
		}//for x				
		//recalc all boundary nodes based on moved water, for future sediment calculations
		for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
			for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
				this.heightMapAra[xIDX][zIDX].calcBoundaryNodes(2);
				this.heightMapAra[xIDX][zIDX].calcPipesSedErosionDeposition(delT,false,  p.allFlagsSet(myConsts.useSedFlux),2);	//usesedflux means don't use backstep
			}//for z
		}//for x

		for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
			for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
					//recalculate node's normal - used in mass wasting
				this.heightMapAra[xIDX][zIDX].source.calcNormal(); 
			}//for z
		}//for x
		//now calculate sediment transport by stam-y semi-lagrangian method - needed to have entire grid of sediment 
		//calculated to determine sediment since velocity is used to determine where sediment came from - advection ala stam, via semi-lagrangian backstep.
		if(!p.allFlagsSet(myConsts.useSedFlux)){
			for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
				for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
						//erosion calculation : sediment transport, using stam-y lagrangian method
					if (((this.heightMapAra[xIDX][zIDX].lclKc) != 0) &&  				//((this.heightMapAra[xIDX][zIDX].getKc() != 0) && 
							(this.heightMapAra[xIDX][zIDX].getHeightWater() > 0)){ 
						this.calc2ndOrderPipesSedTransportAra(this.heightMapAra[xIDX][zIDX], delT);			
					}//if kc != 0
				}//for z
			}//for x
		}//if not using flux engine to transport sed, transport it here
		
			//move all calculated values to base locations for next time step
		for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
			for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
				this.heightMapAra[xIDX][zIDX].setNewValsPipes(this.heightMapAra[xIDX][zIDX], false);                                  //get accurate values in original node's base idx's
			}//for z
		}//for x
			//recalculate all node geometries
		for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
			for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
				this.heightMapAra[xIDX][zIDX].calcNodeGeometry(false,0);
			}//for z
		}//for x			
		for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
			for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
				if ((this.heightMapAra[xIDX][zIDX].getHeightWater() <= myConsts.epsValCalc) && (this.heightMapAra[xIDX][zIDX].getSediment() > 0 )){
					this.heightMapAra[xIDX][zIDX].clearSedFromDryNode();
				}
			}//for z
		}//for x
			//find difference value for sediment between transport and eroded/deposited, put in SEDCAP idx of totalTurnVal ara for holding
		p.setTotalTurnVal(myConsts.SEDCAP, p.globTotValAra[myConsts.SED] - p.globTurnTotValAra[myConsts.SED]);					
		//bandaid solution for sediment inequities - scale sediment on mesh by ratio of total eroded to total not transported in transport stage
		if((p.allFlagsSet(myConsts.scaleSedPipes) && (this.Kc != 0))){
			for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
				for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
					if(p.globTotValAra[myConsts.SED] < p.globTurnTotValAra[myConsts.SED]){gui.print2Cnsl("moved more sediment than on mesh");}
					if(p.globTurnTotValAra[myConsts.SED] > myConsts.epsValCalc){
						this.heightMapAra[xIDX][zIDX].scaleSedimentPipes(0,(p.globTotValAra[myConsts.SED])/(p.globTurnTotValAra[myConsts.SED]));         //get accurate values in original node's base idx's
					}
				}
			}//for each clone vert   
		}//if scale sed		
	}//calc2ndOrderErosionHydraulic
	
	//calc erosion effects of wind and thermal weathering
	private void calc2ndOrderErosionThermal(double delT){
		for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
			for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
		    	calc2ndOrderAeolianErosionAra(this.heightMapAra[xIDX][zIDX], this.globWindAngle, delT);
		    	this.heightMapAra[xIDX][zIDX].addWaterPipes(0, 1, 2);//move water from idx1 to idx2, so that it gets preserved				    		
			}//for z
		}//for x
		
		for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
			for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
				this.heightMapAra[xIDX][zIDX].moveOldValsPipes(false,true);	
			}//for z
		}//for x
		
			//move all calculated values to base locations for next time step
		for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
			for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
				this.heightMapAra[xIDX][zIDX].setNewValsPipes(this.heightMapAra[xIDX][zIDX], false);                                  //get accurate values in original node's base idx's
			}//for z
		}//for x
			//recalculate all node geometries
		for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
			for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
				this.heightMapAra[xIDX][zIDX].calcNodeGeometry(false, 4);
			}//for z
		}//for x			
		for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
			for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
				if ((this.heightMapAra[xIDX][zIDX].getHeightWater() <= myConsts.epsValCalc) && (this.heightMapAra[xIDX][zIDX].getSediment() > 0 )){
					this.heightMapAra[xIDX][zIDX].clearSedFromDryNode();
				}
			}//for z
		}//for x
			//find difference value for sediment between transport and eroded/deposited, put in SEDCAP idx of totalTurnVal ara for holding
		p.setTotalTurnVal(myConsts.SEDCAP, p.globTotValAra[myConsts.SED] - p.globTurnTotValAra[myConsts.SED]);					
		//bandaid solution for sediment inequities - scale sediment on mesh by ratio of total eroded to total not transported in transport stage
		if((p.allFlagsSet(myConsts.scaleSedPipes) && (this.Kc != 0))){
			for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
				for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
					if(p.globTotValAra[myConsts.SED] < p.globTurnTotValAra[myConsts.SED]){gui.print2Cnsl("moved more sediment than on mesh");}
					if(p.globTurnTotValAra[myConsts.SED] > myConsts.epsValCalc){
						this.heightMapAra[xIDX][zIDX].scaleSedimentPipes(0,(p.globTotValAra[myConsts.SED])/(p.globTurnTotValAra[myConsts.SED]));         //get accurate values in original node's base idx's
					}//for z
				}//for x
			}//for each clone vert   
		}//if scale sed		
	}//calc2ndOrderErosionThermal
	
	//finish rain cycle - this is after any rain has been applied to the mesh and moved from idx 0 to 1
	private void calc2ndOrderErosionRain(double delT){
		for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
			for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
				this.heightMapAra[xIDX][zIDX].moveOldValsPipes(true,false);	
			}//for each node in map
		}//for each k-v pair in heightmap
		
			//move all calculated values to base locations for next time step
		for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
			for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
				this.heightMapAra[xIDX][zIDX].setNewValsPipes(this.heightMapAra[xIDX][zIDX], false);                                  //get accurate values in original node's base idx's
			}//for z
		}//for x
			//recalculate all node geometries
		for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
			for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
				this.heightMapAra[xIDX][zIDX].calcNodeGeometry(false,0);
			}//for z
		}//for x			
		for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
			for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
				if ((this.heightMapAra[xIDX][zIDX].getHeightWater() <= myConsts.epsValCalc) && (this.heightMapAra[xIDX][zIDX].getSediment() > 0 )){
					this.heightMapAra[xIDX][zIDX].clearSedFromDryNode();
				}
			}//for z
		}//for x
			//find difference value for sediment between transport and eroded/deposited, put in SEDCAP idx of totalTurnVal ara for holding
		p.setTotalTurnVal(myConsts.SEDCAP, p.globTotValAra[myConsts.SED] - p.globTurnTotValAra[myConsts.SED]);					
		//bandaid solution for sediment inequities - scale sediment on mesh by ratio of total eroded to total not transported in transport stage
		if((p.allFlagsSet(myConsts.scaleSedPipes) && (this.Kc != 0))){
			for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
				for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
					if(p.globTotValAra[myConsts.SED] < p.globTurnTotValAra[myConsts.SED]){gui.print2Cnsl("moved more sediment than on mesh");}
					if(p.globTurnTotValAra[myConsts.SED] > myConsts.epsValCalc){
						this.heightMapAra[xIDX][zIDX].scaleSedimentPipes(0,(p.globTotValAra[myConsts.SED])/(p.globTurnTotValAra[myConsts.SED]));         //get accurate values in original node's base idx's
					}
				}
			}//for each clone vert   
		}//if scale sed		
	}//calc2ndOrderErosionRain
	
	/**
	*  calculate the effects of various types of erosion, based on passed type parameter - using heightMapAra
	*/
	public void calc2ndOrderErosionAra(String type, double delT){
		//--check each heightNode's corresponding source vertex's adjacent list to find neighbors
		if(checkHeightMapAra()){
			this.globalKVal = 1;
			p.clampDeltaT();//get rid of extraneous values in deltaT
			p.resetGTotValAra(myConsts.SED);
			p.resetGTotValAra(myConsts.SEDCAP);
			p.resetGTotValAra(myConsts.SEDCONC);
			p.resetAllMinMaxArrays();
			p.resetAllTurnTotValArrays();
			p.resetGMinMaxAra(myConsts.COORD_Y);
			gui.setNotifyLights(type);
			//move water to idx 1
			rainPipes(delT, (type.equals("Rain") ? this.globRaindropMult : 0)); //if raining move global raindrop val to 2, otherwise move 0 to 1 for hydraulic erosion
			//if(p.allFlagsSet(myConsts.useMatlabFESolve) && (null != this.mat)){ 	calc2ndOrderFEMMatLab();}
			if(type.equals("HydraulicErosion")){			calc2ndOrderErosionHydraulic(delT);	}			
			else if(type.equals("ThermalWeathering")){		calc2ndOrderErosionThermal(delT);	}			
			else if(type.equals("Rain")){					calc2ndOrderErosionRain(delT);		}
			

		}//if not null heightmap
	}//calcErosion func	
	
	
	/**
	 * send mesh data to matlab runner
	 * @param delT
	 */
	public void calc2ndOrderFEMMatLab(){
		//send  height info, invoke fem solver
		int matIDX, triIDX;
		int bndIDX = 0;
		for(int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){
			for(int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//copy border x coords
				matIDX = (zIDX*this.heightMapAra.length) + xIDX;
				//array of triangles referencing idx's - 1 fewer poly than node in both x and z dir
				if((0 == zIDX) || (0 == xIDX) || (this.heightMapAra.length-1 == xIDX) || (this.heightMapAra[0].length-1 == zIDX)){
					mat.bndHt[bndIDX++][0] = this.heightMapAra[xIDX][zIDX].getFemHeight();	//for dirch bounds
				}
				
				if((xIDX < this.heightMapAra.length-1) && (zIDX < this.heightMapAra[0].length-1)){
					triIDX = ((zIDX*(this.heightMapAra.length-1)) + xIDX) * 2;
					mat.ht[triIDX][0] = (this.heightMapAra[xIDX][zIDX].getFemHeight() + 
										this.heightMapAra[xIDX+1][zIDX].getFemHeight() 
										+ this.heightMapAra[xIDX+1][zIDX+1].getFemHeight())/3.0;
					mat.ht[triIDX+1][0] = (this.heightMapAra[xIDX][zIDX].getFemHeight() + 
							this.heightMapAra[xIDX+1][zIDX+1].getFemHeight() 
							+ this.heightMapAra[xIDX][zIDX+1].getFemHeight())/3.0;
				}				
			}//for each z
		}//for each x	
		mat.setValsSet(true);
    	try{
    		this.mat.setMatlabEnvVals();
    		double[][] retVal = this.mat.callFEMFunc();
    		for(int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){
    			for(int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//copy border x coords
    	    		matIDX = (zIDX*this.heightMapAra.length) + xIDX;
    				this.heightMapAra[xIDX][zIDX].setMatFEMval(retVal[matIDX][0]);
    				//this.heightMapAra[xIDX][zIDX].setMatFEMval((0 == retVal[matIDX][0] ? 0 : Math.log(1 + retVal[matIDX][0])));
    			}//for each z
    		}//for each x
			
    	} catch(Exception e){
	    	gui.print2Cnsl("error attempting to call matlab code : " + e);		    		
    		p.setFlags(myConsts.useMatlabFESolve, false);
    	}
		//gui.print2Cnsl("building FEM vals");
	}//	calc2ndOrderFEMMatLab
	/**
	 * for each node, recalc all values that are dependent on source vert values, and then recalc all internal values that are depended on these
	 * used by mesh smoothing to address height node modifications after smoothing is run
	 */
	public void handleNodeRecalc(){
		for(int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){
			for(int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//copy border x coords
				this.heightMapAra[xIDX][zIDX].resetSrcVertVals(this.heightMapAra[xIDX][zIDX].source, this.heightMapAra[xIDX][zIDX].source.coords.y);//TODO change if using closed mesh
			}//for each z
		}//for each x
		for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
			for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
				this.heightMapAra[xIDX][zIDX].calcNodeGeometry(false,0);
			}
		}//for each node
	}//handleNodeRecalc

	/**
	*  build the height map for the currently built face/vert mesh
	*/
	public void makeHeightMap(){//calls this twice to appropriate set flags and return to non-height map display
	  p.setFlags(myConsts.heightProc,!p.allFlagsSet(myConsts.heightProc));
	  p.setFlags(myConsts.heightProc,!p.allFlagsSet(myConsts.heightProc));    
	}

	/**
	*  builds height map from sqsq subdivided quad surface held in global hashmap polyList
	*  2D array of stacks of height nodes
	*/
	public void buildHeightMap(){
		gui.print2Cnsl("Height map being made");
		if ((this.heightMap == null) || (this.heightMap.size() == 0)){
			p.resetAllMinMaxArrays();
			this.ara2DHeightMap = new HashMap<Double, HashMap<Double, ArrayDeque<myHeightNode>>>();	//put nodes in this idxed by x coord, then y coord, then z coord\
			HashMap<Double, ArrayDeque<myHeightNode>> tmpAraHeightMap = new HashMap<Double, ArrayDeque<myHeightNode>>();
			//declare outside of loop to speed up computation
			int heightNodeCloneDotID, heightNodeDotID, oldVertDotID;
			myVertex vertClone;
			myHeightNode tmpHeightNode, tmpCloneHeightNode, putResultVert;
			ArrayDeque<myHeightNode> tmpHeightDeque, putResult;
	    
			HashMap<Integer, myHeightNode> tmpOldCloneHeightNodeList = new HashMap<Integer, myHeightNode>(); 
			HashMap<Integer, ArrayDeque<myHeightNode>> tmpHeightMap = new HashMap<Integer, ArrayDeque<myHeightNode>>();
			ArrayList<myVertex> processedVerts = new ArrayList<myVertex>();
			for (myFace face : sub.polyList.values()){//for each face in polylist 
				for (myVertex vert : face.getVerts()){
					if (!processedVerts.contains(vert)){
						processedVerts.add(vert);
						//make primary height node
						if (!p.allFlagsSet(myConsts.multByNormal)){ tmpHeightNode = new myHeightNode(p, gui, this, this.sub, vert, vert.coords.y); } 
						else { tmpHeightNode = new myHeightNode(p, gui, this, this.sub, vert, myVector._dist(vert.coords, vert.baseCoords));}
						heightNodeDotID = tmpHeightNode.ID;           //using these incase we wish to change what keys the hashmap
							//make clone
						vertClone = vert.clone();                //make clone for erosion calc,add to struct oldCloneHeightNodeList, used for original source of height data

						if (!p.allFlagsSet(myConsts.multByNormal)){ tmpCloneHeightNode = new myHeightNode(p, gui, this, this.sub, vertClone, vertClone.coords.y); } 
						else { tmpCloneHeightNode = new myHeightNode(p, gui, this, this.sub, vertClone,myVector._dist(vertClone.coords, vertClone.baseCoords));}

						heightNodeCloneDotID = tmpCloneHeightNode.ID;
						if (tmpHeightMap.containsKey(heightNodeDotID)){
							setHNode(tmpHeightMap.get(heightNodeDotID),tmpHeightNode);
							gui.print2Cnsl("\t--height map contains deck already : " + heightNodeDotID);
						} else {//build new deque and add to tmpHeightMap
							tmpHeightDeque = new ArrayDeque<myHeightNode>();
							setHNode(tmpHeightDeque, tmpHeightNode);
							putResult = tmpHeightMap.put(heightNodeDotID,tmpHeightDeque);
								//put result in 2D ara-based hashmap - need to modify to handle 3-d structures with multiple x/z values
							if (this.ara2DHeightMap.get(tmpHeightNode.source.coords.x) != null){//hashmap exists at x value/y value already - retrieve it and add the arraydeque ref at its specific z coord
								tmpAraHeightMap = this.ara2DHeightMap.get(tmpHeightNode.source.coords.x);		
							} else {//hashmap for this x value does not exist yet, make it, put in deque at appropriate z, put it in ara2DHeightMap
								tmpAraHeightMap = new HashMap<Double, ArrayDeque<myHeightNode>>();
							}//if hashmap exists at x coord or not
								//check if z dimension hashmap has value or not yet
							if(tmpAraHeightMap.get(tmpHeightNode.source.coords.z) == null){//bad news if not null - means another value has same x and z coord.
								tmpAraHeightMap.put(tmpHeightNode.source.coords.z, tmpHeightDeque);			//put height node, in aradeque, in hashmap at correct z location,
								this.ara2DHeightMap.put(tmpHeightNode.source.coords.x, tmpAraHeightMap);	//and put that hashmap at correct x location for this node
							} else {
								gui.print2Cnsl("Danger : Height node : " + tmpHeightNode.ID + " at " + tmpHeightNode.source.coords.toString() + " is duplicated in height map");
							}//if aradeque exists at z coord or not							
							
							if (putResult != null) {gui.print2Cnsl("error - 2 verts with same ID : " + heightNodeDotID);}
						}//if key exists already in tmpHeightMap - whether or not to build deque
	  
						//put clone height node in structure
						putResultVert = tmpOldCloneHeightNodeList.put(heightNodeCloneDotID, tmpCloneHeightNode); 
						
						if (putResultVert != null) {gui.print2Cnsl("error storing cloned height node : clone : " + tmpCloneHeightNode + " of heightNode : " + tmpHeightNode);}
					}//if processed array doesn't contain vert already
				}//for each vert in face
			}//for each face in polylist    
			//have all verts cloned by now, now can make adj lists for clone source verts
			this.oldCloneHeightNodeList = tmpOldCloneHeightNodeList;
			this.heightMap = tmpHeightMap;
			gui.print2Cnsl("map size : " + heightMap.size());
				//build lists of indexes for each dimension of 2d array of nodes
			for (myHeightNode cloneHeightNode : tmpOldCloneHeightNodeList.values()){//can get real vertex that corresponds to clone vertex by subtracting ID from max val
				oldVertDotID = calcCloneID(cloneHeightNode.source.ID);
				tmpHeightNode = getHNode(tmpHeightMap.get(oldVertDotID));   
				tmpHeightNode.calcNodeGeometryOld(false);                                                  //build pipe lengths array based on adjacent heightnodes
				//set all adjacencies for source of clone height node
				cloneHeightNode.source.resetCloneAdjacencies(getHNode(tmpHeightMap.get(oldVertDotID)).source, tmpOldCloneHeightNodeList);
				cloneHeightNode.calcNodeGeometryOld(true);
				tmpHeightNode.setNodeJustMade(false);
				cloneHeightNode.setNodeJustMade(false);
			}//for each clone height node
			
			p.setFlags(myConsts.heightMapMade, ((this.heightMap != null) && (this.heightMap.size() != 0)));
			if(this.heightMap.size() != 0) {			gui.print2Cnsl("height map generated on : " + this.heightMap.size() + " vertices");}
			else {										gui.print2Cnsl("zero-size height map not made");}
		}//if heightmap was null
	}//buildheightmap method 
	
	/**
	 * save the current height map ara data - all the heights of every node, to be displayed as an overlay
	 */
	public void saveHeightMapAraData(){
		this.oldHeightMapAraData = new myVector[sub.vertListAra.length][sub.vertListAra[0].length];
		for(int xIDX = 0; xIDX < sub.vertListAra.length; ++xIDX){
			for (int zIDX = 0; zIDX < sub.vertListAra[0].length; ++zIDX){
				this.oldHeightMapAraData[xIDX][zIDX] = this.heightMapAra[xIDX][zIDX].source.coords.cloneMe();
			}//for zidx
		}//for xidx	
		gui.print2Cnsl("Current height map data stored for comparison");
	}//saveHeightMapAraData
	
	public boolean compareHeightMapAraDataExists(){	return ((this.compareHeightMapAraData != null) && (this.compareHeightMapAraData.length != 0) && (this.compareHeightMapAraData[0].length != 0));	}
	
	/**
	 * calculates the comparison between current height map and oldheightmaparadata and builds a 
	 */
	public void compareHeightMapAraData(){
		if((this.oldHeightMapAraData!= null) && (this.oldHeightMapAraData.length != 0) && (this.oldHeightMapAraData[0].length != 0)){ 
			this.compareHeightMapAraData = new myVector[sub.vertListAra.length][sub.vertListAra[0].length];
			for(int xIDX = 0; xIDX < sub.vertListAra.length; ++xIDX){
				for (int zIDX = 0; zIDX < sub.vertListAra[0].length; ++zIDX){
					this.compareHeightMapAraData[xIDX][zIDX] = new myVector (this.oldHeightMapAraData[xIDX][zIDX].x,this.heightMapAra[xIDX][zIDX].source.coords.y - this.oldHeightMapAraData[xIDX][zIDX].y, this.oldHeightMapAraData[xIDX][zIDX].z);
				}//for zidx
			}//for xidx		
			gui.print2Cnsl("Comparison with old data completed");
		}//if comparison data exists
	}//compareHeightMapAraData
	
	/**
	*  build 2d ara of height nodes.  remove aradeque reference - only 1 node per x-z coord pair
	*/
	public void buildHeightMapAra(){
		gui.print2Cnsl("Height map 2d ara being made");
		//if ((this.heightMapAra == null) || (this.heightMapAra.length == 0) || (this.heightMapAra[0].length == 0)){
		if(!checkHeightMapAra()){
			p.resetAllMinMaxArrays();
			this.heightMapAra 				= new myHeightNode[sub.vertListAra.length][sub.vertListAra[0].length];
			//this.oldCloneHeightNodeAra 		= new myHeightNode[sub.vertListAra.length][sub.vertListAra[0].length];
			//declare outside of loop to speed up computation
			myVertex vert;
			//myVertex vertClone;
			myHeightNode tmpHeightNode;
			//myHeightNode tmpCloneHeightNode; 
			
			for(int xIDX = 0; xIDX < sub.vertListAra.length; ++xIDX){
				for (int zIDX = 0; zIDX < sub.vertListAra[0].length; ++zIDX){
					vert = sub.vertListAra[xIDX][zIDX];
					//make primary height node
					if (!p.allFlagsSet(myConsts.multByNormal)){ tmpHeightNode = new myHeightNode(p, gui, this, this.sub, vert, vert.coords.y); } 
					else { tmpHeightNode = new myHeightNode(p, gui, this, this.sub, vert, myVector._dist(vert.coords, vert.baseCoords));}
						//make clone
//					vertClone = vert.clone();                //make clone for erosion calc,add to struct oldCloneHeightNodeList, used for original source of height data

//					if (!p.allFlagsSet(myConsts.multByNormal)){ tmpCloneHeightNode = new myHeightNode(p, gui, this, this.sub, vertClone, vertClone.coords.y); } 
//					else { tmpCloneHeightNode = new myHeightNode(p, gui, this, this.sub, vertClone,myVector._dist(vertClone.coords, vertClone.baseCoords));}

					//put heightnode in structure
					this.heightMapAra[xIDX][zIDX] = tmpHeightNode;
  
					//put clone height node in structure
					//this.oldCloneHeightNodeAra[xIDX][zIDX] = tmpCloneHeightNode; 
						
				}//for each vert in face
			}//for each face in polylist    
			//have all verts cloned by now, now can make adj lists for clone source verts
			gui.print2Cnsl("2d ara map size : " + (this.heightMapAra.length * this.heightMapAra[0].length) );
			
			for(int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){
				for(int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){
					tmpHeightNode = this.heightMapAra[xIDX][zIDX];
					tmpHeightNode.calcNodeGeometry(false,0);                                                  //build pipe lengths array based on adjacent heightnodes
					tmpHeightNode.setNodeJustMade(false);
				}				
			}	
//removing refs to oldCloneHeightNode baloney			
//			for(int xIDX = 0; xIDX < this.oldCloneHeightNodeAra.length; ++xIDX){
//				for(int zIDX = 0; zIDX < this.oldCloneHeightNodeAra[0].length; ++zIDX){
//					tmpCloneHeightNode =  this.oldCloneHeightNodeAra[xIDX][zIDX];
//					tmpHeightNode = this.heightMapAra[xIDX][zIDX];
//					tmpHeightNode.calcNodeGeometry(false,0);                                                  //build pipe lengths array based on adjacent heightnodes
//					//set all adjacencies for source of clone height node					
//					tmpCloneHeightNode.source.resetCloneAdjacenciesAra(tmpHeightNode.source, this.oldCloneHeightNodeAra);
//					tmpCloneHeightNode.calcNodeGeometry(true,0);
//					tmpHeightNode.setNodeJustMade(false);
//					tmpCloneHeightNode.setNodeJustMade(false);
//				}				
//			}			
				//set heightMapMade based on heightMapAra being made
			p.setFlags(myConsts.heightMapMade,(((this.heightMapAra != null) && (this.heightMapAra.length != 0) && (this.heightMapAra[0].length != 0))));
			if((this.heightMapAra.length != 0) && (this.heightMapAra[0].length != 0)){			gui.print2Cnsl("height map ara generated on : " + (this.heightMapAra.length * this.heightMapAra[0].length) + " nodes for mesh # : " + p.currMeshID + " : " + myConsts.meshConstNames[p.currMeshID]);}
			else {										gui.print2Cnsl("zero-size height map ara not made");}
		}//if heightmap was null
	}//buildheightmapAra method 	
	/**
	*  simulates rain on every vertex specific mappings of water - dam, column or other - 
	*/
	public void setupDam(int meshType){
		if (this.heightMap != null){
			int cloneVertDotID;
			ArrayDeque<myHeightNode> tmpDeck;
			HashMap<Integer, ArrayDeque<myHeightNode>> tmpHeightMap = new HashMap<Integer, ArrayDeque<myHeightNode>>();
			myHeightNode srcHNodeOld;

			for (Integer IDX : this.heightMap.keySet()){                  //get each key and value from height map -keys are unique ints but not necessarily sequential
				tmpDeck = new ArrayDeque<myHeightNode>();         //make new deque to put modfied nodes in      
				for (myHeightNode srcHNode : this.heightMap.get(IDX)){                           //get nodes from old deque at heightmap at position idx - prevent concurrent mod
					cloneVertDotID = calcCloneID(srcHNode.source.ID);        
					srcHNodeOld = this.oldCloneHeightNodeList.get(cloneVertDotID);  //get clone node
					if (srcHNodeOld == null) { gui.print2Cnsl("error - no height node with id : " + cloneVertDotID);} 
					if (srcHNode.getSource().isRainOnMe()){       
						srcHNode.addRainByAmount(calcDamSetupH2O(meshType, srcHNode.source.coords.y, srcHNode.source.vertDim), false);        //add rain to each node
						srcHNodeOld.addRainByAmount(calcDamSetupH2O(meshType, srcHNodeOld.source.coords.y, srcHNodeOld.source.vertDim), true);
					}//if node gets rained on 
				}//for each node in deque
				tmpHeightMap.put(IDX, tmpDeck);  
			}//for each k-v pair in heightmap
			//now recalc geometry
			for (Integer IDX : this.heightMap.keySet()){                  //get each key and value from height map -keys are unique ints but not necessarily sequential
				tmpDeck = new ArrayDeque<myHeightNode>();         //make new deque to put modfied nodes in	      
				for (myHeightNode srcHNode : this.heightMap.get(IDX)){                           //get nodes from old deque at heightmap at position idx - prevent concurrent mod
					cloneVertDotID = calcCloneID(srcHNode.source.ID);        
					srcHNodeOld = this.oldCloneHeightNodeList.get(cloneVertDotID);  //get clone node
					srcHNode.calcNodeGeometryOld(false);
					srcHNodeOld.calcNodeGeometryOld(true);
				}//for each node in deque	
			}//for each deque in heightmap
		}//if not null heightmap  
	}//setupDam func
	
	public void clampHeightMesh(){
		for(int xIdx = 0; xIdx < this.heightMapAra.length; ++xIdx){
			for(int zIdx = 0; zIdx < this.heightMapAra[xIdx].length; ++zIdx){
				if (this.heightMapAra[xIdx][zIdx].source.coords.y > (p.globMaxAra[myConsts.COORD_Y] * .55f)){
					this.heightMapAra[xIdx][zIdx].source.coords.y = p.globMaxAra[myConsts.COORD_Y] * .55f;
				}				
			}//zIdx					
		}//xIdx		
	}//clampHeight	
	
	/**
	*  calculates initial water configuration based on what kind of mesh is being built
	*  @param the mesh type
	*  @param the vertex corresponding to the height node being populated
	*  @return the amount of water calculated to be placed at this particular height node
	*/
	public double calcDamSetupH2O(int meshID, double height, int nodeCount){
		//gui.print2Cnsl("setting water up for mesh ID : " + meshID);
		switch (meshID){
			case myConsts.mesh_ramp						: {return Math.max(0,.25*(this.heightMapAra.length - height));}
			case myConsts.mesh_coneUp                   : {return Math.max(0,.25*(this.heightMapAra.length - height));}
			case myConsts.mesh_coneDown                 : {return Math.max(0,.25*(this.heightMapAra.length - height));}
			case myConsts.mesh_2coneDown                : {return Math.max(0,.25*(this.heightMapAra.length - height));}
			case myConsts.mesh_volcano                  :
			case myConsts.mesh_2volcano                 :
			case myConsts.mesh_dam 						: 
		    case myConsts.mesh_damWall 					: 
		    case myConsts.mesh_riverLedge 				: {return Math.max(0,(.25*(this.heightMapAra.length - height)));}  
		    case myConsts.mesh_column 					: 
		    case myConsts.mesh_tiltColumn 				: 
		    case myConsts.mesh_tiltShelfColumn 			: {return Math.max(0,(.8*(this.heightMapAra.length - height)));}
		    case myConsts.mesh_riverPrimary				: 
		    case myConsts.mesh_riverStraightPier		: 
		    case myConsts.mesh_riverStraightTributary	: 
		    case myConsts.mesh_riverStraight2Pier		: 
		    case myConsts.mesh_riverStraight2Tributary 	: 
		    case myConsts.mesh_riverStraight2Trib2Pier 	: {return Math.max(0,(.15*(this.heightMapAra.length - height)));}  		    
		    case myConsts.mesh_riverColumn 				: 
		    case myConsts.mesh_riverManyColumns			: {return Math.max(0,(.25*(this.heightMapAra.length - height)));} 
		    case myConsts.mesh_columnWall				: 
		    case myConsts.mesh_waterAroundColumnWall   	: {return Math.max(0,(1.2*(this.heightMapAra.length - height)));}
		    case myConsts.mesh_damBetweenWalls	    	: {return Math.max(0,(.7*(this.heightMapAra.length - height)));}
		    case myConsts.mesh_damBetweenLargeRiver  	: {return Math.max(0,(.7*(this.heightMapAra.length - height)));}
			case myConsts.mesh_riverFlatTerrain 		: {return Math.max(0,(.15*(this.heightMapAra.length - height)));}  
		    
		    //can add other setups here
		    default : return 0;
		}//switch 
	}//calcSetupH2O
	
	/**
	 * once river testmesh is built, convert to heightmap and put water in nodes that get it.
	 */
	public void makeRiverDamMeshHeightMap(int meshID, int picIdx){		
		makeHeightMap();	
		if((heightMapAra != null) && (heightMapAra.length != 0) && (heightMapAra[0].length != 0)) {
			setupDamAra(meshID); 
		} else {
			gui.print2Cnsl("using old heightmap structure for mesh currpic " + (picIdx));
			setupDam(meshID); //sets up water distribution for mesh using old structure - should never be called
		}		
	}//makeRiverMeshHeightMap
	
	/**
	* 2D ara version - simulates rain on every vertex specific mappings of water - dam, column or other - 
	*/
	public void setupDamAra(int meshID){
		if(checkHeightMapAra()){
			myHeightNode srcHNode;
			//myHeightNode srcHNodeOld;
			wetNodeCoords = new HashMap<Integer,Integer[]>();			//map that holds the coords of wet nodes, so we can pick some at random to start boat on
			Integer wncIdx = 0;
			double heightMod;
//			if(meshType == myConsts.mesh_columnWall){			
//				sub.smoothMesh();sub.smoothMesh();sub.smoothMesh();	
//				clampHeightMesh();
//			}
			for(int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){
				for(int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){
					srcHNode = this.heightMapAra[xIDX][zIDX];
					//srcHNodeOld = this.oldCloneHeightNodeAra[xIDX][zIDX];
					if(p.allFlagsSet(myConsts.wrapAroundMesh)) {
						//subtract from height at xz (x,0) Edge of mesh - this will never have water
						//all river meshes need to have this to maintain slope of mesh in pre-set water vol
						heightMod = (Math.max(this.heightMapAra[xIDX][0].source.coords.y, this.heightMapAra[0][0].source.coords.y) - srcHNode.source.coords.y);						
					} else {	heightMod = srcHNode.source.coords.y;		}//not wrap around mesh
					if (srcHNode.source.isRainOnMe()){       
						wetNodeCoords.put(wncIdx++, new Integer[]{xIDX,zIDX});//add these coords to the hashmap of initially wet coords for this mesh
						srcHNode.addRainByAmount(calcDamSetupH2O(meshID, heightMod, srcHNode.source.vertDim), false);        //add rain to each node
						//srcHNodeOld.addRainByAmount(calcDamSetupH2O(meshID, heightMod, srcHNodeOld.source.vertDim), true);
					}//if node gets rained on 
				}				
			}
			for(int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){
				for(int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){
					this.heightMapAra[xIDX][zIDX].calcNodeGeometry(false,0);
					//this.oldCloneHeightNodeAra[xIDX][zIDX].calcNodeGeometry(true,0);//calcBoundaryNodes					
				}//for z
			}//for x
			
			for(int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){
				for(int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){
					if(this.heightMapAra[xIDX][zIDX].source.isRainOnMe()){//if this node has water
						this.heightMapAra[xIDX][zIDX].calcBoundaryNodes(0);
						//this.oldCloneHeightNodeAra[xIDX][zIDX].calcBoundaryNodes(0);
					}
				}//for z
			}//for x
		}//if not null heightmap  
	}//setupDamara func	
	
	//verifies that a legal height map exists and is populated
	public boolean checkHeightMapAra(){		return ((this.heightMapAra != null) &&  (this.heightMapAra.length != 0) && (this.heightMapAra[0].length != 0));	}//checkHeightMapAra
	
	/**
	 * finds a node currently with water, or at least that can receive water, to put the sailboat on (within .5 node's distance of the wet node
	 * @return coords to put a new boat
	 */
	public myVector findWaterCoords(){
		Integer[] nodeIdxs =  this.wetNodeCoords.get((int) (Math.random() * (this.wetNodeCoords.size() - 1)));
		myVector result = null;
		if((checkHeightMapAra()) && (nodeIdxs != null)){
			result = this.heightMapAra[nodeIdxs[0]][nodeIdxs[1]].source.coords.cloneMe();	
			result.set(result.x + Math.random()-.5, result.y + Math.random()-.5, result.z + Math.random()-.5);//nodes all 1 dist from each other, so +-.5 on each coords
		}
		return result;	
	}
	
	/**
	*  removes all water from map nodes
	*/
	public void clearAllWater(){
		if (this.heightMap != null){//heightMap version
			int cloneVertDotID;
			myHeightNode srcHNodeOld;
			//initialize global min/max for h2o
			p.resetGMinMaxAra(myConsts.H2O);
			if (!p.allFlagsSet(myConsts.simulate)){  gui.print2Cnsl("Clear water from terrain");  }
			for (Integer IDX : this.heightMap.keySet()){                  //get each key and value from height map -keys are unique ints but not necessarily sequential
				for (myHeightNode srcHNode : this.heightMap.get(IDX)){                           //get nodes from old deque at heightmap at position idx - prevent concurrent mod
					cloneVertDotID = calcCloneID(srcHNode.source.ID);        
					srcHNodeOld = this.oldCloneHeightNodeList.get(cloneVertDotID);  //get clone node
					if (srcHNodeOld == null) { gui.print2Cnsl("error - no height node with id : " + cloneVertDotID);}        
					if(p.allFlagsSet(myConsts.Pipes)){
						srcHNode.clearPipesErosionVals();                  //clear erosion-related values
						srcHNodeOld.clearPipesErosionVals();  
					} else {
						srcHNode.clearErosionVals();                  //clear erosion-related values
						srcHNodeOld.clearErosionVals();  
					}
				}//for each node in deque
			}//for each k-v pair in heightmap
		}//if not null heightmap
		
		else if(checkHeightMapAra()){//heightmap ara version
			for (int xIDX = 0; xIDX < this.heightMapAra.length; ++xIDX){//for each x index
				for (int zIDX = 0; zIDX < this.heightMapAra[0].length; ++zIDX){//for each z index
					if(p.allFlagsSet(myConsts.Pipes)){
						this.heightMapAra[xIDX][zIDX].clearPipesErosionVals();    
						//this.oldCloneHeightNodeAra[xIDX][zIDX].clearPipesErosionVals();
					} else {
						this.heightMapAra[xIDX][zIDX].clearErosionVals();                         
						//this.oldCloneHeightNodeAra[xIDX][zIDX].clearErosionVals();
					}
				}//for each zidx
			}//for each xidx
		}//if heightmapara != null
	}//clearAllWater functon	

	/**
	*  returns the last element in the passed height deque - put in a separate method in case we wish to change the mechanism
	*/
	public myHeightNode getHNode(ArrayDeque<myHeightNode> heightDeque){ myHeightNode result = heightDeque.peekLast(); return result; }//getHNnode method

	/**
	*  returns the last element in the deck at the heightmap at the passed idx
	*/
	public myHeightNode getHNode(int idx){  ArrayDeque<myHeightNode> tmpDeck = this.heightMap.get(idx); return getHNode(tmpDeck); }//getHNnode method

	/**
	*  returns the last element in the clone heightnode map at the passed idx
	*/
	public myHeightNode getCloneHNode(int idx){ return this.oldCloneHeightNodeList.get(idx); }//getHNnode method

	/**
	*  adds an element to the passed height deque - put in a separate method in case we wish to change the mechanism
	*/
	public void setHNode(ArrayDeque<myHeightNode> heightDeque, myHeightNode tmpNode){  heightDeque.add(tmpNode); }//setHNnode method

	/**
	 * adds an element to the ara2DHeightNode structure
	 */
	
	/**
	*  returns maxInt - passed int value.  used for clone vertex id's, so that the original vert's id is encoded without threat of 
	*  duplicate verts.
	*/
	public int calcCloneID (int srcID) { return Integer.MAX_VALUE - srcID; }

	/**
	*  calculates a random offset for the talus angle to take into account inconsistencies in material
	*/
	public float randTalusAngleMult(){ return p.random(0.9f,1.1f);}

	/**
	*  returns some random rain multiplier, to be used on each vertex's rain water receipt
	*/
	public float rainMult(){ return 1.0f;}//random(.8,1.2);}//rainMultiplier function

	/**
	*  returns some random silt multiplier for each vertex
	*/
	public float siltMult(){ return 1.0f;}// random (.8,1.2);}//silt function	

}//mySqSqEroder class
