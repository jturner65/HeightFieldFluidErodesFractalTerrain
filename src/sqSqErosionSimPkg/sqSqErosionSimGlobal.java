package sqSqErosionSimPkg;


import processing.core.*;

import java.awt.event.KeyEvent;
import java.util.*;

public class sqSqErosionSimGlobal extends PApplet {
		//class objects
	//the camera object that controls the view of the scene
	public myCam cam;
	//the gui layer
	private myGui gui;
	//the erosion engine
	private mySqSqEroder eng;
	//the subdivision engine
	private mySqSqSubdivider sub;
	//engine to run matlab
	public myFEMSolver mat;	
	//demo stuff : 
	//array of booleans to hold global flags - idxed by constants defined in myConsts class
	private boolean[] flags;
	//the type of mesh being displayed
	public int meshType;
	//playlist TODO :
	public int currPlayList;
	
	//////////////
	//  subdivision related variables
	//////////////

	//running count of objects in current scene - used for obj id derivation when objects are made
	public int objCount = 0;
	//multiplier to face normal to change direction (if -1)
	private int faceNormalMult = 1;
	
	//for recording mesh data to prepopulate relevant data for each mesh as mesh is called
	//unique id of current mesh being built - holds value defined in myConsts for mesh_ consts TODO : Used for initial flag/variable setup for each mesh for demo
	public int currMeshID;
	//array of structures that holds mesh data
	public HashMap<Integer, myMeshDescriber> mesh_vals;
	//names of booleans set to true
	public ArrayList<String> mesh_flagNames;

	//erosion consts
	public double[] mesh_erosionConstsData;
	//sim consts
	public double[] mesh_simulationCntlData;

	//int representing type of primitive poly - 9 is TRIANGLES, 16 is QUADS
	public final int polyType = PApplet.QUADS;
	//type of subdivision most recently executed
	public int subDivType = -1;
	//number of supported subdivision types
	public static final int numSubTypes = 3;
	//string describing most recent subdivision executed
	public String subDivTypeString = "";
//	//torus vals - slices
//	public final int torusSlices = 96;
//	//torus vals - facets
//	public final int torusFacets = 48;
//	//torus primary radius
//	public final int torusRad1 = 5;
//	//torus secondary radius
//	public final int torusRad2 = 3;
//	//knot radius
//	public final int knotRad = 7;
	
	//////////////
	//  end subdivision related variables
	//////////////

	/////////////////
	//simulation related variables
	////////////////
	
	//number of threads/processors available on cpu
	public final int numProcs = Runtime.getRuntime().availableProcessors();

//	//amt of time that passes between iterations
	private double deltaT = myConsts.baseDeltaT;
	//calculated deltaT using bridson's eqs.
	public double idealDeltaT;

	//number of cycles of simulation that have been executed
	public long simCycles = 0;
	//number of repeating button-held sim cycles - repeats as button is held, independent of automated sim
	public int clickSimCycles = 0;
	//count simCycles per TW execution - 0 for no TW execution
	public int simTWCycles = myConsts.initSimTWCycles;
	//count simCycles per HE execution - 0 for no HE execution
	public int simHECycles = myConsts.initSimHECycles;
	//count simCycles per Rain execution - 0 for no Rain execution
	public int simRainCycles = myConsts.initSimRainCycles;
	
	//number of draw cycles
	public long drawCycles = 0;

	//global min and max value aras
	public double[] globMaxAra;
	public double[] globMinAra;
	public double[] globTotValAra;				//total amounts of water/sediment in scene due to (h2o) rain or (sed) erosion/deposition
	public double[] globTurnTotValAra;			//total amounts of water/sediment in scene due to transport.
	//index in minmax arrays currently chosen by user for visualisation display
	public int globMinMaxIDX;

	//how many times does the sim repeat every draw cycle
	public double globSimRepeats = 4;

	//scale amount to multiply velocity vectors by for display purposes in pipes model
	public double vVecScaleFact = 1;

	//checked when not simulating to see if the previous draw cycle the simulation was running
	public boolean wasSimulating = false;

	///////////////
	//end simulation related variables
	///////////////

	//////////////
	//gui display-related variables and constants
	//////////////	
	
	//global values for displaying a sub-graph(slice) of the mesh in either x or z - used if dispCutAwayX or dispCutAwayZ are set to true
	public double dispCoordX = 0;
	public double dispCoordZ = 0;
	//global values for displaying a sub-graph(slice) of the mesh - size of the slice, in either x or z
	public double dispSizeX = 0;
	public double dispSizeZ = 0;
	
	//camera results from cam object
	public float[] cAra = new float[9];	
	//used to control visible object in window - click-based rotation
	public double rotationX = 0;
	public double rotationY = 0;
//	public double rotationZ = 0;
	public double elevationY = 0;
	//used to handle gui input to modify simulator values
	public double[] xGuiModDel;
	public double[] yGuiModDel;
	
	public boolean keyDown = false;                        //whether any key is currently pressed - used to limit key overlap/stutter - might be superfluous depending on keypress handling
	public boolean keyFocusDown = false;                    //whether the focus is being assigned - used to only reset the values of the camera orientation 1 time per focus-shift

	//location of mouse click
	public double xClickLoc = 0;
	public double yClickLoc = 0;		

	/**
	 * jos stam-inspired 3d fluid model solver variables
	 */
	//cell size for stam solver
	public final int stamCellSize = 4;
	//solver object for 2D area, 3d volume, current solver
	public myStamSolver stamSolver2D, stamSolver3D, currSolver;
	//number of cells per dimension in 3D stam solver 
	public int stamSolverDim3D = 30;
	//number of cells per dimension in 2D stam solver 
	public int stamSolverDim2D = 50;
	//structure holding voxels for stam solver
	public myVoxel[] voxelGrid2D, voxelGrid3D;
	//counter to add velocities for stam solver
	public int stamSolverCounter;
	
	//sailboat related code
	//container of sailboat objects - can modify the position of the last one added
	public HashMap<Integer, mySailBoat> sailBoatList;
	
	/////////////////////////  
	//code starts
	/////////////////////////
	/**
	*  initialize variables for first run of program 1 time only
	*/
	public void initProgram(){
		drawCycles = 0;
		currPlayList = -1;			//demo stuff - playlist selected, to be played
		clampDeltaT();
		initOnce();                  //run only once to init global variables
		mat = null;					//set when pressing matlab buttons in gui
		cam = new myCam(this);
		eng = new mySqSqEroder(this, null, null);
		gui = new myGui(this, eng, null);
		sub = new mySqSqSubdivider(this, this.gui, this.eng);
			//set references to other components
		eng.setGui(gui);
		if (null != mat) {mat.setGui(gui);}
		eng.setSubdivider(sub);
		gui.setSubdivider(sub);
		this.mesh_vals = new HashMap<Integer,myMeshDescriber>();	//hashmap that holds values for initial flag settings for each mesh
		for(int i = 0; i< myConsts.numTotMeshes; ++i){		this.mesh_vals.put(i, new myMeshDescriber(this,this.gui, this.eng, this.sub));	}
		initStam();	  
		initVars();
	}//initProgram
	
	public void initBoat(){ this.sailBoatList = new HashMap<Integer, mySailBoat>();	}

	public void initOnce(){
		imageMode(CORNER); 
		globMinMaxIDX = myConsts.H2O;              // can be changed/chosen by user
			//dataVis = "Data visualisation is On for" + getTypeFromConst(globMinMaxIDX);
		globMinAra = new double[myConsts.numGlobValVars];
		globMaxAra = new double[myConsts.numGlobValVars];
		globTotValAra = new double[myConsts.numGlobValVars];			//total water and sediment as determined by additions and subtractions 
		globTurnTotValAra = new double[myConsts.numGlobValVars];		//total water and sediment as stored at end of each turn
		xGuiModDel = new double[myGui.numLblConsts];
		yGuiModDel = new double[myGui.numLblConsts];
	  
		resetAllMinMaxArrays();
		initGlobalFlags();
	}//initOnce
	//draw gui with orthographic projection
	public void drawGui(){
		pushMatrix();
			//ortho(0, displayWidth,0, displayHeight, -10, 1000);
			float dWidth = displayWidth*.5f, dHeight = displayHeight * .5f;
			ortho(-dWidth, dWidth,-dHeight, dHeight, -10, 1000);
			camera(myConsts.globZPlane,0,myConsts.globZPlane,0,0,0, 0,-1,0);
			if(!keyPressed){	  	setFlags(myConsts.shiftKeyPressed, false);}//clear unless key is currently pressed
			gui.setUpGui();				//gui display
		popMatrix();
	    if ((flags[myConsts.savePic]) || ((flags[myConsts.recordVideo] && flags[myConsts.simulate]))) {  	
	    	if(!flags[myConsts.simulate]){ setFlags(myConsts.savePic, false);} 
	    	gui.savePic(); 
	    }//if saving a pic-if not simulating, set savepic flag false, only save sequence of pics if simulation is running
	}
	
	/**
	*  initialize display for each cycle of draw
	*/
	public void initDisplay(){
		resetMatrix();  // set the transformation matrix to the identity 
		background(0);  // clear the screen to black  
		initLight();     
		perspective (PI * .333f, (1.05f * displayWidth)/(displayHeight), 0.01f, 1000.0f);  
		cAra = cam.moveCamera();
		camera(cAra[0],cAra[1],cAra[2],cAra[3],cAra[4],cAra[5],cAra[6],cAra[7],cAra[8]);
		cam.setCamDirPos(new myVector(0,0,0));
		scale (1.0f, -1.0f, 1.0f);  // change to right-handed coordinate system
		rotate (PI/2.0f, 0.0f, 0.0f, 1.0f);  
		rotate (PI/2.0f, 1.0f, 0.0f, 0.0f);   
		if (flags[myConsts.TWcalc] && flags[myConsts.simulate] && (simTWCycles != 0)){ dispWindArrow();}   //if executing a thermal weathering cycle, display the wind direction arrow
	}//initdisplay

	/**
	*  initialize lightsources
	*/
	public void initLight(){
			// create an ambient light source
		ambientLight(102, 102, 102);  
			// create 4 directional light sources
		pushMatrix();
			translate (0,10,30);
			lightSpecular(255, 255, 255);
			directionalLight(102f, 102f, 102f, 0f,0f, -1f);
			directionalLight(152f, 152f, 152f, 0f, 0.7f, 0.7f);
			translate (20,0,0);
			lightSpecular(255, 255, 255);
			directionalLight(102f, 102f, 102f, 0f, 0f, -1f);
			directionalLight(152f, 152f, 152f, 0f, 0.7f, 0.7f);
		popMatrix();
	}//initLight
	
	/**
	*  initialize global boolean flags
	*/
	public void initGlobalFlags(){
		flags = new boolean[myConsts.numFlags];
		//flagColor = new myVector[myConsts.numFlags];
		for (int i = 0; i < myConsts.numFlags; ++i){
			flags[i] = false;
			//flagColor[i] = new myVector(random(0,255),random(0,255),random(0,255));
		}//for each flags
		//all flags that are true at start
		flags[myConsts.sqSqSub] = true;
		flags[myConsts.Pipes] = true;
		flags[myConsts.terrainMeshDisplay] = true;
		flags[myConsts.subEnabled] = true;  
		flags[myConsts.dataVisualization] = true;
		flags[myConsts.H2OMeshDisplay] = true;
		flags[myConsts.randomHeight] = true;
		flags[myConsts.renderGui] = true;
		flags[myConsts.renderDemoGui] = true;
		flags[myConsts.useSedConc] = true;
		flags[myConsts.useLowestKGlobally] = true;
		flags[myConsts.scaleSedPipes] = true;
		flags[myConsts.debugMode] = true;
		flags[myConsts.useStoredErosionMeshVals] = true;
		//flags[myConsts.demoDevMode] = true;
	}//initGlobalFlags function	
	
	/**
	 * initialize the camera for a specific position based on which mesh is loaded
	 * @param meshNumber the number of the mesh
	 */
	public void initObjCam(){
		Double[] coordAra = new Double[3];
		coordAra[0] = 30.0;
		coordAra[1] = 3.0;
		coordAra[2] = 30.0;
		coordAra = myConsts.initCamPositions.get(currMeshID);			//use mesh ID
		gui.print2Cnsl("cam eye pos : " +  coordAra[0]+"|"+ coordAra[1] +"|"+ coordAra[2] + " mesh type/# : " + this.meshType + "/" + myConsts.meshConstNames[currMeshID]);
		this.rotationX = coordAra[0];
		this.rotationY = coordAra[1];
		this.cam.setEyePos(0, 0, coordAra[2]);	
	}//initCam	
	
	
	/**
	 * initialize the camera for a specific position based on which mesh is loaded
	 * @param meshType whether the mesh is a generated or file-based mesh
	 * @param meshNumber the number of the mesh
	 */
	public void initObjCamOld(int meshNumber){
		Double[] coordAra = new Double[3];
		coordAra[0] = 30.0;
		coordAra[1] = 3.0;
		coordAra[2] = 30.0;
		switch (this.meshType){
			case myConsts.damMesh 	: {	coordAra = myConsts.initGenDamCamPositions.get(meshNumber);		break;	}
			case myConsts.genMesh 	: {	coordAra = myConsts.initGenDamCamPositions.get(meshNumber);		break;	}
			case myConsts.riverMesh : {	coordAra = myConsts.initRiverCamPositions.get(meshNumber);		break;	}
			case myConsts.fileMesh 	: {	coordAra = myConsts.initFileCamPositions.get(meshNumber);		break;	}
			case myConsts.stamMesh 	: {	coordAra = myConsts.initStamCamPositions.get(meshNumber);		break;	}
			default : {	break;}		
		}//switch
		gui.print2Cnsl("cam eye pos : " +  coordAra[0]+"|"+ coordAra[1] +"|"+ coordAra[2] + " mesh type/# : " + this.meshType + "/" + meshNumber);
		this.rotationX = coordAra[0];
		this.rotationY = coordAra[1];
		this.cam.setEyePos(0, 0, coordAra[2]);	
	}//initCam	

	/**
	*  initialize variables used for each new drawing of meshes
	*/
	public void initVars(){
		initBoat();//initialize structure holding sailboats
		gui.initGuiVars();
		//initialize engine variables holding height map-related information
		eng.initEngVars();
		wasSimulating = false;
		clickSimCycles = 0;
		deltaT = myConsts.baseDeltaT;
		objCount = 0;
		faceNormalMult = 1;
		//initialize after construction of subdivided/mesh object
		sub.initValues();

		globMinMaxIDX = myConsts.H2O;              // can be changed/chosen by user
		//dataVis = "Data visualisation is On for" + getTypeFromConst(globMinMaxIDX);
		globMinAra = new double[myConsts.numGlobValVars];
		globMaxAra = new double[myConsts.numGlobValVars];
		globTotValAra = new double[myConsts.numGlobValVars];			//total water and sediment as determined by additions and subtractions 
		globTurnTotValAra = new double[myConsts.numGlobValVars];		//total water and sediment as stored at end of each turn
	
		this.setFlags(myConsts.heightMapMade, false);
		this.setFlags(myConsts.wrapAroundMesh, false);	  
		this.initVarsBasedOnFlags(myConsts.Pipes);
	  
		this.resetAllMinMaxArrays();
		this.resetAllTotValArrays();
		rotationX = 0;
		rotationY = 0;
		//rotationZ = 0;
		elevationY = 0;  
		xGuiModDel = new double[myGui.numLblConsts];		//mouse button-click-to-modify values in bottom row
		yGuiModDel = new double[myGui.numLblConsts];  
		cam.initCam();
	}//initVars
	
	
	 // initialize all flag-dependent variables based on passed flag idx settings by calling setFlags with flag's current value, so it doesn't change but it does execute things based on this value
	public void initVarsBasedOnFlags(int... idxAra){for (int idx : idxAra){ setFlags(idx, flags[idx]);}}//sets flag to its current value (doesn't change flag value) and performs other functions dependent on flag setting		

	/**
	* data/debug visualisation routines
	*/

	//  returns the scaled val of a particular height node given its id	
	public int getScaledValByID(int objID, int minMaxIDX){
		if ((minMaxIDX > -1) && (eng.heightMap != null)){
			myHeightNode tmpNode = eng.getHNode(eng.heightMap.get(objID));
			if (tmpNode == null) { gui.print2Cnsl("error - node does not exist in height map : id : " + objID); return 0; }   
			return tmpNode.calcScaledVal(minMaxIDX,0);
		}
		return 0;  
	}

	/**
	*  will determine a particular scaled value using map based on passed params and return a scaled value between 0 and 255
	*  @param idx of global min/max arrays to use to determine mapping
	*  @param value to map within global min/max range
	*/
	public int calcScaledVal_255(final int IDX, double value){   
		this.globMinAra[IDX] = Math.min(this.globMinAra[IDX], value);
		this.globMaxAra[IDX] = Math.max(this.globMaxAra[IDX], value);
		if (this.globMinAra[IDX] == this.globMaxAra[IDX]){return 0;}
		return ( (int) mapD(value, this.globMinAra[IDX], this.globMaxAra[IDX], 0, 255)); 
	}//calcScaledVal method

	/**
	*  sets the appropriate global min/max values based on the altitude 
	*/
	public void setGlobCoordVals(myVector coords){
		globMaxAra[myConsts.COORD_X] = Math.max(globMaxAra[myConsts.COORD_X], coords.x);
		globMinAra[myConsts.COORD_X] = Math.min(globMinAra[myConsts.COORD_X], coords.x);
		globMaxAra[myConsts.COORD_Y] = Math.max(globMaxAra[myConsts.COORD_Y], coords.y);
		globMinAra[myConsts.COORD_Y] = Math.min(globMinAra[myConsts.COORD_Y], coords.y);
		globMaxAra[myConsts.COORD_Z] = Math.max(globMaxAra[myConsts.COORD_Z], coords.z);
		globMinAra[myConsts.COORD_Z] = Math.min(globMinAra[myConsts.COORD_Z], coords.z);
	}//setGlobH2Ovals	
	
	public void setGlobalVals(int idx, double val){
  		globMaxAra[idx] = Math.max(globMaxAra[idx], val);
  		globMinAra[idx] = Math.min(globMinAra[idx], val);
	}//setGlobalVals
	
	// reinitializes global min/max aras for passed value for index
	public void resetGMinMaxAra(int idx){ this.globMinAra[idx] = Integer.MAX_VALUE; this.globMaxAra[idx] = Integer.MIN_VALUE; }

	public void resetGTotValAra(int idx){this.globTotValAra[idx] = 0;}
	public void resetGTurnTotValAra(int idx){this.globTurnTotValAra[idx] = 0;}
	
	// reinitializes all global min/max aras of modifiable values (h2o, sed, elevation)
	public void resetAllMinMaxArrays(){ resetGMinMaxAra(myConsts.H2O); resetGMinMaxAra(myConsts.SED); resetGMinMaxAra(myConsts.SEDCAP); resetGMinMaxAra(myConsts.SEDCONC);}

	//reinitialize all the global total value aras of modifiable values
	public void resetAllTotValArrays(){ resetGTotValAra(myConsts.H2O); resetGTotValAra(myConsts.SED); resetGTotValAra(myConsts.SEDCAP); resetGTotValAra(myConsts.SEDCONC);}
	
	//reset all the turn-driven global total value aras of modifiable values 
	public void resetAllTurnTotValArrays(){ resetGTurnTotValAra(myConsts.H2O); resetGTurnTotValAra(myConsts.SED); resetGTurnTotValAra(myConsts.SEDCAP); resetGTurnTotValAra(myConsts.SEDCONC);}
	
	//reset all the values in the global minmax arrays pertaining to the x,y,z coords of the mesh
	public void resetGMinMaxAraGlobCoords(){resetGMinMaxAra(myConsts.COORD_X); resetGMinMaxAra(myConsts.COORD_Y); resetGMinMaxAra(myConsts.COORD_Z);}
	
	//finds interpolation t of val between valMin and valMax and applies to mapMin and mapMax
	public double mapD(double val, double valMin, double valMax, double mapMin, double mapMax){
		if (Math.abs((valMax - valMin)) < myConsts.epsValCalc) return 0;
		double minMaxDiff = valMax-valMin;
		double t = (val-valMin);		
		double calc = ((minMaxDiff-t) * mapMin + (t * mapMax)) / (1.0 * minMaxDiff);
			//gui.print2Cnsl("val : " + (val) + " val min : " +  (valMin) + " val max : " + (valMax) + " mapMin : " + (mapMin) + " mapMax : " + (mapMax) + " t =  " + (t) + " calc = " + (calc));
		return calc;
	}//mapD

	/**
	 * uses bridson's method to calculate max deltaT : delT = delX/sqrt(g * D) where D is max depth of water
	 * @return
	 */
	public double calcIdealDeltaT(){ 
		this.idealDeltaT = sub.getDeltaNodeDist()/(Math.sqrt(myConsts.grav * this.globMaxAra[myConsts.H2O])); 
		return this.idealDeltaT;
	}
	
	/**
	 * sorts the passed set, returning a sorted set
	 */
	public SortedSet<Double> sortSet(Set<Double> srcSet){
		SortedSet<Double> result = new TreeSet<Double>();
		for (Double val : srcSet){result.add(val);}		
		return result;		
	}//SortedSet
	
	/**
	 * stam solver routines
	 */
	/**
	 * run at begining of program to initialize voxelGrid
	 */
	public void initStam(){
		//first initialize and set up 3 d solver
		this.stamSolver3D = new myStamSolver(this, this.gui, this.eng, this.stamSolverDim3D, this.voxelGrid3D, true);
		this.currSolver = this.stamSolver3D;
		this.voxelGrid3D = new myVoxel[this.stamSolverDim3D * this.stamSolverDim3D * this.stamSolverDim3D];
		this.stamSolver3D.setVoxelGrid(this.voxelGrid3D);
		double dk, dj, di;
		int k, j, i;
		for(k = 0, dk = (-1.0 * (this.stamSolverDim3D/2.0) * this.stamCellSize); k <this.stamSolverDim3D; ++k, dk+= this.stamCellSize){
			for(j = 0, dj = (-1.0 * (this.stamSolverDim3D/2.0)* this.stamCellSize); j <this.stamSolverDim3D ; ++j, dj+= this.stamCellSize){
				for(i = 0, di = (-1.0 * (this.stamSolverDim3D/2.0) * this.stamCellSize); i <this.stamSolverDim3D; ++i, di+= this.stamCellSize){
					this.voxelGrid3D[this.IX(i,j,k, this.stamSolver3D.getDim())] = new myVoxel(this, this.gui, this.eng, this.stamSolver3D, di, dj, dk, i, j, k, this.stamCellSize, 0.0, true);
				}//for i
			}//for j
		}//for k			
		
		//init 2 D solver here.
		this.stamSolver2D = new myStamSolver(this, this.gui, this.eng, this.stamSolverDim2D, this.voxelGrid2D, false);
		this.voxelGrid2D = new myVoxel[this.stamSolverDim2D * this.stamSolverDim2D];
		this.stamSolver2D.setVoxelGrid(this.voxelGrid2D);
		for(j = 0, dj = (-1.0 * (this.stamSolverDim2D/2.0)* this.stamCellSize); j <this.stamSolverDim2D ; ++j, dj+= this.stamCellSize){
			for(i = 0, di = (-1.0 * (this.stamSolverDim2D/2.0) * this.stamCellSize); i <this.stamSolverDim2D; ++i, di+= this.stamCellSize){
				this.voxelGrid2D[this.IX(i,j,this.stamSolver2D.getDim())] = new myVoxel(this, this.gui, this.eng, this.stamSolver2D, di, 0, dj, i, 0, j, this.stamCellSize, 0.0, false);
			}//for i
		}//for j
		
	}//initStam
	private int IX(int i, int j, int k, int dimMult){ return  i + ((dimMult ) * j) + ((dimMult *dimMult) * k);}
	private int IX(int i, int j, int dimMult){ return  i + (dimMult  * j);}
	
	public void launchStamSolver(myStamSolver solverToLaunch){
		this.currSolver = solverToLaunch;
		solverToLaunch.reInitAras();
		this.deltaT = .001;
		this.setFlags(myConsts.useStamSolver, true);
		this.meshType = myConsts.stamMesh;
		this.initObjCamOld(1);
		this.gui.print2Cnsl("Stam solver initialized for : " + ((this.currSolver.getIs3D()) ? ("3D") : ("2D")));
	}
	
	/**
	 * turns passed sorted set into array of doubles
	 */
	public Double[] setToDouble(SortedSet<Double> passedSet){Double[] result = passedSet.toArray(new Double[0]);return result;}
	
	/**
	 * clean up the currently running stam solver - clear out ara values and any other data used to display solver
	 */
	public void cleanUpStamSolver(){	this.currSolver.reInitAras();	}	
	
	/**
	 * initiate the counter-driven modification to the velocity grid 
	 */
	public void addVelAndDensStamSolver(){ this.stamSolverCounter = 0;	this.setFlags(myConsts.addVelStamSolver, true);}

	/**
	 * end stam solver routines
	 */
	
	/**
	 * needs to return whether the current min max value being recorded and displayed 
	 * is one that has a corresponding velocity calc in each height node.  currently only calculating velocity and sediment transport.  
	 * @return whether or not h2o or sedmient transport are currently being calced/displayed
	 */
	public boolean validMeshDisplay(){	return ((globMinMaxIDX == myConsts.H2O) || (globMinMaxIDX == myConsts.SED) || (globMinMaxIDX == myConsts.SEDCAP));}
	/**
	 * display a small line on the screen reflecting the vector being passed at a particular node (for use with 2nd order simualtion velocities or with surface normals
	 * @param v vector at a node - will give dir of arrow
	 * @param c the center of the node - used to display tail of arrow and to calculate head of arrow
	 */
	public void dispVectorArrow(myVector v, myVector c, double heightOffset, int colorConst, boolean flipDir){
		myVector vNew = new myVector(v);
		vNew._mult(this.deltaT);													//get a displacement in each direction over time deltaT
		if (flipDir){vNew._mult(-1);}												//flip direction of displayed vector
		double vMag = vNew._mag(); 		
		myVector vHead = new myVector(c.x + (this.vVecScaleFact * vNew.x) , c.y + (this.vVecScaleFact * vNew.y) , c.z + (this.vVecScaleFact * vNew.z) );//displacement of head
		if(!(this.allFlagsFalse(myConsts.dispCutAwayX, myConsts.dispCutAwayZ))){	//if we have slice visualisation enabled
			if (!(this.nodeInSliceRange(c.x, c.z))) {  heightOffset -= 200;	}
		}//if displaying a slice			
		pushMatrix();
			gui.setColorValStroke(colorConst);	
			if (vMag > myConsts.epsValDisplay){ 
				line((float)c.x, (float)(c.y + heightOffset), (float)c.z, (float)vHead.x, (float)(vHead.y + heightOffset), (float)vHead.z); 
			}//if not a zero velocity
			else {				
				line((float)c.x, (float)(c.y + heightOffset), (float)c.z, 
						(float)(c.x + myConsts.epsValDisplay), (float)(c.y + heightOffset+ myConsts.epsValDisplay), (float)(c.z+ myConsts.epsValDisplay)); 
			}//draw a dot
			noStroke();
		popMatrix();
	}//dispH2OVelocityArrow
	
	/**
	 * this will draw a line from one passed vector location to another - used for sed source
	 * @param v dest location
	 * @param c source location
	 * @param colorConst - the color to display
	 */
	public void dispLine(myVector v, myVector c, int colorConst){
		double heightOffset = 0;
		if(!(this.allFlagsFalse(myConsts.dispCutAwayX, myConsts.dispCutAwayZ))){	//if we have slice visualisation enabled
			if (!(this.nodeInSliceRange(c.x, c.z))) {  heightOffset -= 200;	}
		}//if displaying a slice			
		pushMatrix();
			gui.setColorValStroke(colorConst);	
			line((float)c.x, (float)(c.y + heightOffset), (float)c.z, (float)v.x , (float)(v.y + heightOffset), (float)v.z); 
			noStroke();
		popMatrix();
	}//
	
	public void dispWindArrow(){
		double windDir = this.eng.globWindAngle;
		double arrowHeight = Math.max(globMaxAra[myConsts.COORD_Y] * 1.2, 3);
		double arrowThick = .5f, arrowLen = 4;
		//draw arrow pointing at center of terrain, above terrain, showing wind direction
		double[][] headVerts = new double[6][3];      //each of 6 verts for x,y,z
		double[][] tailVerts = new double[8][3];      //each of 8 verts for x,y,z
		  
		  //build head verts
		float xCalc = 1, zCalc = 1;
		  //head center vert
		headVerts[0][0] = 0;
		headVerts[0][1] = arrowHeight;
		headVerts[0][2] = 0;
		headVerts[3][0] = 0;
		headVerts[3][1] = arrowHeight + arrowThick;
		headVerts[3][2] = 0;
		
		headVerts[1][0] = xCalc * Math.cos(windDir + PI/6);
		headVerts[1][1] = arrowHeight;
		headVerts[1][2] = zCalc * Math.sin(windDir + PI/6);
		headVerts[4][0] = xCalc * Math.cos(windDir + PI/6);
		headVerts[4][1] = arrowHeight + arrowThick;
		headVerts[4][2] = zCalc * Math.sin(windDir + PI/6);
		  
		headVerts[2][0] = xCalc * Math.cos(windDir - PI/6);
		headVerts[2][1] = arrowHeight;
		headVerts[2][2] = zCalc * Math.sin(windDir - PI/6);
		headVerts[5][0] = xCalc * Math.cos(windDir - PI/6);
		headVerts[5][1] = arrowHeight + arrowThick;
		headVerts[5][2] = zCalc * Math.sin(windDir - PI/6);
		
		  //build tail verts
		tailVerts[0][0] = (headVerts[2][0] * (2.0/3)) + (headVerts[1][0] * (1.0/3));
		tailVerts[0][1] = arrowHeight; 
		tailVerts[0][2] = (headVerts[2][2] * (2.0/3)) + (headVerts[1][2] * (1.0/3));
		tailVerts[4][0] = (headVerts[2][0] * (2.0/3)) + (headVerts[1][0] * (1.0/3));
		tailVerts[4][1] = arrowHeight + arrowThick; 
		tailVerts[4][2] = (headVerts[2][2] * (2.0/3)) + (headVerts[1][2] * (1.0/3));
		 
		tailVerts[1][0] = (headVerts[2][0] * (1.0/3)) + (headVerts[1][0] * (2.0/3));
		tailVerts[1][1] = arrowHeight; 
		tailVerts[1][2] = (headVerts[2][2] * (1.0/3)) + (headVerts[1][2] * (2.0/3));
		tailVerts[5][0] = (headVerts[2][0] * (1.0/3)) + (headVerts[1][0] * (2.0/3));
		tailVerts[5][1] = arrowHeight + arrowThick; 
		tailVerts[5][2] = (headVerts[2][2] * (1.0/3)) + (headVerts[1][2] * (2.0/3));
		   
		tailVerts[2][0] = arrowLen * tailVerts[1][0];
		tailVerts[2][1] = arrowHeight; 
		tailVerts[2][2] = arrowLen * tailVerts[1][2];
		tailVerts[6][0] = arrowLen * tailVerts[1][0];
		tailVerts[6][1] = arrowHeight + arrowThick; 
		tailVerts[6][2] = arrowLen * tailVerts[1][2];
		  
		tailVerts[3][0] = arrowLen * tailVerts[0][0];
		tailVerts[3][1] = arrowHeight; 
		tailVerts[3][2] = arrowLen * tailVerts[0][2];
		tailVerts[7][0] = arrowLen * tailVerts[0][0];
		tailVerts[7][1] = arrowHeight + arrowThick; 
		tailVerts[7][2] = arrowLen * tailVerts[0][2];
		  
		pushMatrix();
		pushStyle();
		    gui.setColorArgs(255,0,0,255);
		    noStroke();  
		    //draw arrow head
		    beginShape(TRIANGLES);
		      	for (int idx = 0; idx < 3; ++idx){  vertex ((float)headVerts[idx][0],(float)headVerts[idx][1],(float)headVerts[idx][2]); }
		    endShape(CLOSE);  
		    beginShape(TRIANGLES);
		      	for (int idx = 3; idx < 6; ++idx){  vertex ((float)headVerts[idx][0],(float)headVerts[idx][1],(float)headVerts[idx][2]); }
		    endShape(CLOSE); 
		    for(int idx = 0; idx < 3; ++idx){
		    	beginShape(QUADS);
		        	vertex ((float)headVerts[idx][0],					(float)headVerts[idx][1],				(float)headVerts[idx][2]);
		        	vertex ((float)headVerts[((idx + 1) % 3)][0],		(float)headVerts[((idx + 1) % 3)][1],	(float)headVerts[((idx + 1) % 3)][2]);
		        	vertex ((float)headVerts[((idx + 1) % 3) + 3][0],	(float)headVerts[((idx + 1) % 3)+3][1],	(float)headVerts[((idx + 1) % 3)+3][2]);
		        	vertex ((float)headVerts[idx+3][0],					(float)headVerts[idx+3][1],				(float)headVerts[idx+3][2]);
		        endShape(CLOSE);      
		    }//for idx
		    
		    //draw arrow stem
		    for(int idx = 0; idx < 4; ++idx){
		    	beginShape(QUADS);
		        	//println("x ["+ idx+"] = " + tailVerts[idx][0]);
		        	vertex ((float)tailVerts[idx][0],                  (float)tailVerts[idx][1],                  (float)tailVerts[idx][2]);
		        	vertex ((float)tailVerts[((idx + 1) % 4)][0],      (float)tailVerts[((idx + 1) % 4)][1],      (float)tailVerts[((idx + 1) % 4)][2]);
		        	vertex ((float)tailVerts[((idx + 1) % 4) + 4][0],  (float)tailVerts[((idx + 1) % 4)+4][1],    (float)tailVerts[((idx + 1) % 4)+4][2]);
		        	vertex ((float)tailVerts[idx+4][0],                (float)tailVerts[idx+4][1],                (float)tailVerts[idx+4][2]);
		        endShape(CLOSE);
		    }//for idx
		    
		    beginShape(QUADS);
		      	for (int idx = 0; idx < 4; ++idx){ vertex ((float)tailVerts[idx][0],(float)tailVerts[idx][1],(float)tailVerts[idx][2]);} 
		    endShape(CLOSE);
		    beginShape(QUADS);
		      	for (int idx = 4; idx < 8; ++idx){ vertex ((float)tailVerts[idx][0],(float)tailVerts[idx][1],(float)tailVerts[idx][2]);} 
		    endShape(CLOSE);	
		popStyle();
		popMatrix();
	}//dispWindArrow func
	
		//modifies total amount of -type- by -amt-
	public void addToTotalVal(int type, double amt){this.globTotValAra[type] += amt;}
	//modifies total turn-based amount of -type- by -amt-
	public void addToTotalTurnVal(int type, double amt){this.globTurnTotValAra[type] += amt;}
		//sets total turn-based amount of -type- by -amt-
	public void setTotalTurnVal(int type, double amt){this.globTurnTotValAra[type] = amt;}
		//method checks if any passed flags are set
	public boolean anyFlagsSet(int... flagIDXAra){ for (int flagIDX : flagIDXAra){	if (flags[flagIDX]){return true;}} return false;}
		//method checks if all passed flags are set
	public boolean allFlagsSet(int... flagIDXAra){ for (int flagIDX : flagIDXAra){	if (!flags[flagIDX]){return false;}} return true;}	
		//method checks if any passed flags are false
	public boolean anyFlagsFalse(int... flagIDXAra){ for (int flagIDX : flagIDXAra){	if (!flags[flagIDX]){return true;}} return false;}
		//method checks if all passed flags are set
	public boolean allFlagsFalse(int... flagIDXAra){ for (int flagIDX : flagIDXAra){	if (flags[flagIDX]){return false;}} return true;}	

	/**
	*  function to set particular status flags for the simulator, with special code for each individual flag if needed
	*  @param idx the index of the flag being modfied
	*  @param val the value to set the flag to
	*/
	public void setFlags(int idx, boolean val){
		boolean oldVal = flags[idx];
		flags[idx] = val;
			//special settings for each flag
		switch (idx) {
		  	//subdivision and terrain-related
		  	case myConsts.sqSqSub           		: { if (val) {subDivType = myConsts.sqSqSub;   setFlags(myConsts.catClkSub, false); setFlags(myConsts.loopSub, false);}   break;}       //0;   
		    case myConsts.catClkSub         		: { if (val) {subDivType = myConsts.catClkSub; setFlags(myConsts.sqSqSub, false);   setFlags(myConsts.loopSub, false);}   break;}       //0;   
		    case myConsts.loopSub           		: { if (val) {subDivType = myConsts.loopSub;   setFlags(myConsts.sqSqSub, false);   setFlags(myConsts.catClkSub, false);}   break;}       //0;     
		    case myConsts.subEnabled        		: { 
		    	if (val) {
		    		flags[myConsts.erosionProc] = false; 
		    		flags[myConsts.simulate] = false;
		    		flags[myConsts.Pipes] = false;
		    		flags[myConsts.showH2OVelocity] = false;
		    		} break;}                                  
		    
		    case myConsts.heightProc        		: { if (val) { 
		    		globMinMaxIDX = myConsts.H2O;  
		    		if(sub.vertListAra != null){//if the ara is not null then use it to build height map
		    			gui.print2Cnsl("building heightmap ara");
		    			eng.buildHeightMapAra();		    			
		    		} else {
		    			gui.print2Cnsl("building heightmap DEPRECATED");
		    			eng.buildHeightMap();		
		    		}
		    		setFlags(myConsts.Pipes, true);
		    		setFlags(myConsts.erosionProc, true); } 
		    	break;}                                  
		    case myConsts.heightMapMade     		: {  break;}         
		    case myConsts.erosionProc       		: { 
		    	if ((val) && (!flags[myConsts.heightMapMade])){//can't set to erosion unless there's a heightmap
		    		flags[myConsts.erosionProc] = false;
		    	} else{      
		    		setFlags(myConsts.subEnabled,!val);	    		
		    		if (!(val)){
		    			setFlags(myConsts.simulate, false);
		    			setFlags(myConsts.Pipes, false);
			    		setFlags(myConsts.randomHeight, true);
		    		}//if erosion turned off, turn off simulation if executing
		    	} 
		    	break;}    
		    
		    case myConsts.subdivRiverMesh			: { break;}		//whether or not to subdivide the pregenerated wrap-around river meshes
		    case myConsts.meshIsGenerated  			: { break;}  
		    case myConsts.multByNormal      		: { break;}       
		    case myConsts.randomHeight      		: { break;}     
		    case myConsts.vertTerrainNormals	    : { break;} 
		    case myConsts.vertH2ONormals  			: { break;}
		    case myConsts.terrainMeshDisplay	 	: { break;}
		    case myConsts.wrapAroundMesh 			: {	break;}			//wrap around mesh
		    case myConsts.dispNon1KVals 			: { break;}		//show where k is not 1		
		    case myConsts.dispFluxVals				: { break;}		//show outgoing flux for each node
		    case myConsts.dispSedSrcVecs 			: { break;}		//show sediment source vectors for each node	    
		    
		    case myConsts.dispCutAwayX				: {	this.dispSizeX = sub.getDeltaNodeDist(); 	break;}		//initialize visualisation slice to be single row
		    case myConsts.dispCutAwayZ				: { this.dispSizeZ = sub.getDeltaNodeDist();   	break;}		//initialize visualisation slice to be single row
		    
		    //fluid simulation related		    
		    case myConsts.H2OMeshDisplay      		: { if(!validMeshDisplay()) {flags[myConsts.H2OMeshDisplay] = false;} break;}     
		    case myConsts.strokeDisplayH2O 			: { if(!validMeshDisplay()) {flags[myConsts.H2OMeshDisplay] = false;} break;}
		    case myConsts.strokeDisplayTerrain		: { break;}
		    case myConsts.simulate          		: { 
		    	if (val) { setFlags(myConsts.subEnabled,false); eng.launchSim();  }
		    	else     { setFlags(myConsts.subEnabled,!flags[myConsts.erosionProc]);}//if sim is now running or not
		    	break;}                                   
		    case myConsts.TWcalc            		: { 
		    	if (oldVal != val){ 
		    		if(!val) { 					simTWCycles = 0;	}
		    		else if (simTWCycles == 0){	simTWCycles = 1;	}//if val and already set to != 0 don't change value
		    	}
		    	break;}        //initialize # of cycles in sim to automatically execute erosion proc
		    		
		    case myConsts.HEcalc            		: { 
		    	if (oldVal != val){ 
		    		if(!val) { 					simHECycles = 0;	}
		    		else if (simHECycles == 0){	simHECycles = 1;	}//if val and already set to != 0 don't change value
		    	}
		    	break; }      //initialize # of cycles in sim to automatically execute erosion proc
		    
		    case myConsts.Raincalc         			: { 
		    	if (oldVal != val){ 
		    		if(!val) { 					simRainCycles = 0;	}
		    		else if (simRainCycles == 0){	simRainCycles = 1;	}//if val and already set to != 0 don't change value
		    	}   
		    	break;}  //initialize # of cycles in sim to automatically execute erosion proc
		    case myConsts.Pipes             		: { 
		    	deltaT = ((val) ? .1f : 1); 
		    	if (val) {setFlags(myConsts.randomHeight,false);}//if turning on pipes, turn off random heights by default - can be turned back on if user wishes
		    	break;}
		    case myConsts.showTerrainNormals		: { break;}
		    case myConsts.showH2ONormals			: { break;}
		    case myConsts.showH2OVelocity	   		: { if ((val) && (!allFlagsSet(myConsts.Pipes, myConsts.erosionProc))) {	setFlags(myConsts.showH2OVelocity,false);}//if not pipes and erosion processing then turn off this flag
		    	break;}//show velocity vector - currently only applicable if pipes simulation  
		    case myConsts.calcLateralErosion		: { break;}
			//stam solver flags
		  	case myConsts.addVelStamSolver  		: {	break;}
		    //debug and gui related
		  	case myConsts.shiftKeyPressed   		: { break;}		  	
		  	case myConsts.debugMode					: { break;}		  	//enable debug functionality - disp and enable debug options
		  			  	
		    case myConsts.debugModeCnsl : {//if true open file, if false and file is open, close and save file		    	
		    	if (val){
		    		if (!flags[myConsts.debugOutputFileOpen]) {//if true, open file for output
		    			flags[myConsts.debugOutputFileOpen] = true;
		    			gui.fileWriterDebug_Create();
		    		}
		    	} else if (flags[myConsts.debugOutputFileOpen]) {//if false, and file open, save and close file
		    		flags[myConsts.debugOutputFileOpen] = false;
		    		gui.fileWriterDebug_Close();	    		
		    	}  	break;	}
		   
		    case myConsts.debugSubdivision : {
		    	if ((val) && (flags[myConsts.debugMode])){
		    		sub.printFaces(); sub.printVerts(); sub.printOutVertFaceAras(); //print out vert and face data
		    	}
		    	break;
		    }
		    case myConsts.dataVisualization          : {//what quantities should be displayed via color - water height, sediment amt, altitude, normals
		    	globMinMaxIDX   = (globMinMaxIDX + 2) % (myConsts.numGlobVisVars + 1) - 1;
		    	gui.setDataVisStr();      
		    	flags[myConsts.dataVisualization] = (globMinMaxIDX != -1) ? true : false;
		    	if (!flags[myConsts.dataVisualization]){setFlags(myConsts.H2OMeshDisplay, false);setFlags(myConsts.strokeDisplayH2O, false);}	//clear out mesh display and mesh stroke display flags
		    	break;}                                 
		    case myConsts.renderGui         		: { flags[myConsts.mseHotspotMenusActive] = !val;  	break;}      
		    case myConsts.renderDemoGui         	: { flags[myConsts.mseHotspotMenusActive] = !val;  	break;}      
//		    case myConsts.mseHotspotMenusActive		: {
//		    	flags[myConsts.renderGui] = !val;
//		    	flags[myConsts.renderDemoGui] = !val;
//		    	break; }
		    case myConsts.savePic           		: {  break;}       	    
						//whether or not the debug output file is currently open
			case myConsts.debugOutputFileOpen 		: {  break; }     // 46;
						//display sed values at nodes remaining after sed transport step in pipes model
			case myConsts.dispSedTransLeftoverVals 	: {  break; }     // 50;
						//whether to scale sediment	w/greg's bandaid						
			case myConsts.scaleSedPipes				: {  break; }     // 52;
						//limit the amount of water to be used to determine sediment capacity - any volume above a certain amount is ignored when calculating capacity
			case myConsts.limitWaterSedCap			: {  break; }     // 53;
						//ignore y component of velocity in sediment caluclation
			case myConsts.ignoreYVelocity			: {  break; }     // 54;
						//display tilt component of height nodes, sin of angle of tilt - using surface normal and velocity vector of water to get angle
			case myConsts.dispOriginalMesh			: {  break; }     // 55;
						//display number of adj nodes at particular node as mesh height
			case myConsts.dispAdjNodes				: {  break; }     // 56;
						//display vector to node that this node wraps to	
			case myConsts.dispWrapToNodes			: {  break; }     // 57;
						//display pipe lengths values for each vertex	
			case myConsts.dispPipeLengths			: {  break; }     // 58;
						//display height differences between each node and each of its adjacent nodes
			case myConsts.dispHeightDiffs			: {  break; }     // 60;
						//apply lowest k value across mesh for all flux values
			case myConsts.useLowestKGlobally		: {  break; }     // 61;	
						//whether or not to rain on terrain next cycle
			case myConsts.rainOnTerrain1Cyc			: {  break; }     // 63;
						//whether or not to use the flux mechanism to transport sediment
			case myConsts.useSedFlux				: {  break; }     // 64;
						//whether or not to use sed concentration for sed transport mechanism
			case myConsts.useSedConc				: {  break; }     // 65;
						//whether or not to pull sed/sedConc from dry nodes at river edges
			case myConsts.erodeDryNodes				: {  break; }     // 66;
						//choose alternate sediment advection mechanism - needs to be implemented
			case myConsts.advectErosionDep			: {  break; }     // 67;				
						//operate in batch mode, to attempt to determine optimal ks/kd values
			case myConsts.batchMode					: {  break; }     // 68;			    
		    default 								: {	 break; }
		}//switch
	}//setFlags proc
	
	/**
	 * checks whether we are displaying slices and whether the passed vert is in the range of current display slice values
	 * @param tmpVert
	 * @return
	 */	
	public boolean nodeInSliceRange(double x, double z){
		if (((this.allFlagsSet(myConsts.dispCutAwayX) && (!gui.inRange(x, this.dispCoordX,this.dispCoordX + this.dispSizeX))) && (this.allFlagsFalse(myConsts.dispCutAwayZ))) ||
			((this.allFlagsSet(myConsts.dispCutAwayZ) && (!gui.inRange(z,this.dispCoordZ, this.dispCoordZ + this.dispSizeZ))) &&  (this.allFlagsFalse(myConsts.dispCutAwayX))) ||
			(this.allFlagsSet(myConsts.dispCutAwayX, myConsts.dispCutAwayZ) && 
						((!gui.inRange(x, this.dispCoordX,this.dispCoordX + this.dispSizeX)) || 
						(!gui.inRange(z, this.dispCoordZ, this.dispCoordZ + this.dispSizeZ))))){ 
			//if x set, z false, and x not in range or z set, x is false and z not in range or x and z set, and x or z not in range
					return false; }
		return true;
	}//nodeNotInSliceRange
	
	//this will select the appropriate erosion functionality to execute based on flag settings
	public void pickCorrectErosion(String procVal){
		if (allFlagsSet(myConsts.Pipes)){ 
			//if(eng.checkHeightMapAra()){	 	
				eng.calc2ndOrderErosionAra(procVal, deltaT);
			//	}
			}//use the 2d ara version
			//else {																	eng.calc2ndOrderErosion(procVal, deltaT); }}		
		else {             					eng.calc1stOrderErosion(procVal, deltaT); }
	}//pickCorrectErosion
	
	////////
	//handle dev mod mouse input
	////////	
	private int handleClickDevMode(){
		int resultVals = -1, resultBoolIDX = -1, resultButtons = -1;
		if((this.allFlagsSet(myConsts.renderGui)) || (this.allFlagsSet(myConsts.mseHotspotMenusActive) &&  gui.popUpMenuOn)){
			//check if modifiable values have been modified - if one has happened, then no other needs to be checked
			if (((gui.inRange(xClickLoc, gui.guiModValsULBlockX, gui.guiModValsUnderMenuBlockX)) && (yClickLoc > gui.guiModValsULBlockY)) || 
					((xClickLoc > gui.guiModValsUnderMenuBlockX) && (yClickLoc > gui.guiModValsUnderMenuBlockY))){//value clicked in 
				resultVals = gui.checkInGuiVals(xClickLoc, yClickLoc);//might still be -1
			} else if (gui.inRange(yClickLoc,gui.minModBoolLocY,gui.maxModBoolLocY) && (xClickLoc > gui.minModBoolLocX)){
				//check if click in boolean flag region
				resultBoolIDX = gui.checkInGuiBools(xClickLoc, yClickLoc);//might still be -1
			} else if (gui.inRange(xClickLoc, gui.butULBlockX, gui.butLRBlockX) && gui.inRange(yClickLoc, gui.butULBlockY, gui.butLRBlockY)){
				resultButtons = gui.checkInGuiButtons(xClickLoc, yClickLoc, (allFlagsSet(myConsts.shiftKeyPressed)) ? 1 : 0);//might still be -1		
			} else if ((this.allFlagsSet(myConsts.debugMode)) 
					&& (gui.inRange(xClickLoc, gui.debugULBlockX, gui.debugLRBlockX)) && (gui.inRange(yClickLoc, gui.debugULBlockY, gui.debugLRBlockY))){	
				resultBoolIDX = gui.checkDebugBools(xClickLoc, yClickLoc);		
			}//if in range of modifiable buttons, else
		}//if gui rendered
		
		//if any value has been changed then set up the variables to handle it
		if ((resultBoolIDX != -1) || (resultVals != -1) || (resultButtons != -1)){
			gui.guiDataChanged = true; 
			if (resultBoolIDX != -1) {gui.handleGuiChoiceBool(resultBoolIDX);}
			else if (resultVals != -1) { gui.guiDataValueChanged = resultVals;}
			else if (resultButtons != -1){ 
				gui.butPressed = resultButtons;
				gui.handleGuiChoiceButtons(resultButtons, true);
			}
			return -1;
		} else  {
			return 0;
		}//if gui modified via click or not	
	}//handleclickdevmode
	
	private void dragMouse(){
		if (mouseButton == LEFT){
			rotationX += mapD(mouseX-pmouseX,0,100,0,2);
			rotationY += mapD(mouseY-pmouseY,0,100,0,2);
		} else if (mouseButton == RIGHT){
			//rotationZ -= mapD(mouseX-pmouseX,0,100,0,2);
			elevationY -= mapD(mouseY-pmouseY,0,100,0,4);
		}		
	}

	private int handleDragDevMode(){
		if (!gui.guiDataChanged){
			dragMouse();
		} else {
			if (gui.guiDataValueChanged != -1){
				xGuiModDel[gui.guiDataValueChanged] = mouseX-pmouseX;
				yGuiModDel[gui.guiDataValueChanged] = -1*(mouseY-pmouseY);
				xGuiModDel[0] = gui.guiDataValueChanged;  //save which value has been modified for calculating new value
				yGuiModDel[0] = 1;                    //flag to show values changed
			}//if guidatachanged - if value mod or buttons pressed and held
		}//if gui changed or not changed		
		return 0;
	}
	
	private int handleReleaseDevMode(){
		gui.butPressed = -1;
		setFlags(myConsts.fluvialExec, false);
		setFlags(myConsts.aeolianExec, false);
		setFlags(myConsts.rainFallExec, false);
		elevationY = 0;
		if (gui.guiDataValueChanged != -1) {
			xGuiModDel[gui.guiDataValueChanged] = 0;
			yGuiModDel[gui.guiDataValueChanged] = 0;
			gui.guiDataValueChanged = -1;  
		}//if guidatavaluechanged		
		return 0;
	}
			
	/**
	 * handle demo mode mouse clicks
	 * @return
	 */
	private int handleClickDemoMode(){
		
		//change for demo-specific variables and locations
		int resultVals = -1, resultBoolIDX = -1, resultButtons = -1;
		if((this.allFlagsSet(myConsts.renderDemoGui))  || (this.allFlagsSet(myConsts.mseHotspotMenusActive) &&  gui.popUpMenuOn)){//if rendering demo gui or a pop up is on, check click location to see if interacting with clicable object
			//check if modifiable values have been modified - if one has happened, then no other needs to be checked
			if (gui.inRange(xClickLoc, gui.demoButtonULClickX, gui.demoButtonLRClickX) && gui.inRange(yClickLoc, gui.demoButtonULClickY, gui.demoButtonLRClickY)){
				resultButtons = gui.checkInDemoGuiButtons(xClickLoc, yClickLoc, (allFlagsSet(myConsts.shiftKeyPressed)) ? 1 : 0);//might still be -1		
			} else if (gui.inRange(xClickLoc, gui.demoButULSmMeshBoolX, gui.demoButLRSmMeshBoolX) && gui.inRange(yClickLoc, gui.demoButtonULClickY, gui.demoButtonLRClickY)){
				//check if click in boolean flag region
				resultBoolIDX = gui.checkInDemoGuiMeshBools(xClickLoc, yClickLoc);//might still be -1
			} else if (gui.inRange(xClickLoc, gui.demoButULSmSimBoolX, gui.demoButLRSmSimBoolX) && gui.inRange(yClickLoc, gui.demoButtonULClickY, gui.demoButtonLRClickY)){
				//check if click in button region 
				resultButtons = gui.checkInDemoGuiSimButtons(xClickLoc, yClickLoc);//might still be -1
			}
		}
		//if any value has been changed then set up the variables to handle it
		if ((resultBoolIDX != -1) || (resultVals != -1) || (resultButtons != -1)){
			gui.guiDataChanged = true; 
			if (resultBoolIDX != -1) {gui.handleGuiChoiceBool(resultBoolIDX);}
			else if (resultVals != -1) { gui.guiDataValueChanged = resultVals;}
			else if (resultButtons != -1){ 
				gui.butPressed = resultButtons;
				gui.handleGuiChoiceButtons(resultButtons, true);
			}
			return -1;
		} else  {
			return 0;
		}//if gui modified via click or not			
	}
	
	private int handleDragDemoMode(){
		if (!gui.guiDataChanged){
			dragMouse();
//			if (mouseButton == LEFT){
//				rotationX += mapD(mouseX-pmouseX,0,100,0,2);
//				rotationY += mapD(mouseY-pmouseY,0,100,0,2);
//			} else if (mouseButton == RIGHT){
//				//rotationZ -= mapD(mouseX-pmouseX,0,100,0,2);
//				elevationY -= mapD(mouseY-pmouseY,0,100,0,4);
//			}
		} else {
			if (gui.guiDataValueChanged != -1){
				xGuiModDel[gui.guiDataValueChanged] = mouseX-pmouseX;
				yGuiModDel[gui.guiDataValueChanged] = -1*(mouseY-pmouseY);
				xGuiModDel[0] = gui.guiDataValueChanged;  //save which value has been modified for calculating new value
				yGuiModDel[0] = 1;                    //flag to show values changed
			}//if guidatachanged - if value mod or buttons pressed and held
		}//if gui changed or not changed		
		return 0;
	}
	
	private int handleReleaseDemoMode(){		
		//replace with demo specifc code if needed		
		gui.butPressed = -1;
		setFlags(myConsts.fluvialExec, false);
		setFlags(myConsts.aeolianExec, false);
		setFlags(myConsts.rainFallExec, false);
		elevationY = 0;
		if (gui.guiDataValueChanged != -1) {
			xGuiModDel[gui.guiDataValueChanged] = 0;
			yGuiModDel[gui.guiDataValueChanged] = 0;
			gui.guiDataValueChanged = -1;  
		}//if guidatavaluechanged			
		return 0;
	}//handleReleaseDemoMode
			
	/////////
	//key and mouse routines
	/////////
	public void mousePressed(){
		xClickLoc = mouseX;
		yClickLoc = mouseY;
		if((xClickLoc <= 1) && (yClickLoc <= 1)){exit();}		//exit at upper left corner
			//if clicked on demo/dev mode gui swap button, change gui layout
		if ((gui.inRange(xClickLoc, gui.devDemoButL, gui.devDemoButR)) && (gui.inRange(yClickLoc, gui.devDemoButT, gui.devDemoButB))){this.setFlags(myConsts.demoDevMode, !this.flags[myConsts.demoDevMode]  );}
		else if ((flags[myConsts.mseHotspotMenusActive]) && (gui.checkInLockClickBox(xClickLoc, yClickLoc))) {;}//no op,
		else if ((gui.inRange(xClickLoc, gui.guiDispButL, gui.guiDispButR)) && (gui.inRange(yClickLoc, gui.guiDispButT, gui.guiDispButB))){
			int flagIDX = (this.flags[myConsts.demoDevMode] ? myConsts.renderDemoGui : myConsts.renderGui);
			this.setFlags(flagIDX, !this.flags[flagIDX]);}
		else {//check if click in gui zone
			int resVal = -1;
			if(this.allFlagsSet(myConsts.demoDevMode)){ 	resVal = handleClickDemoMode();	} 
			else {											resVal = handleClickDevMode();	}		
			if(resVal != -1){
				gui.guiDataChanged = false;  
				gui.guiDataValueChanged = -1;//means clicked on non-data entry region, so modify map view/camera orientation
			}
		}
	}//mousePressed	
	
	public void mouseReleased(){
		int resVal;
		if(this.allFlagsSet(myConsts.demoDevMode)){ 	resVal = handleReleaseDemoMode();	} 
		else {											resVal = handleReleaseDevMode();	}		
	}//mouseReleased function

	//if mouse-over menus active, then check mouse location if mouse moves
	public void mouseMoved(){	if(allFlagsSet(myConsts.mseHotspotMenusActive)) {	gui.checkMouseLocation();}}
	
	public void mouseDragged(){
		int resVal;
		if(this.allFlagsSet(myConsts.demoDevMode)){ 	resVal = handleDragDemoMode();	} 
		else {											resVal = handleDragDevMode();	}		
	}//mouseDragged function
	
	public void keyPressed(){
		switch (key){
			case '\\' :
			case '|' :{ setFlags(myConsts.H2OMeshDisplay, !flags[myConsts.H2OMeshDisplay]); break;}   
			case 'e' :
			case 'E' : { if (flags[myConsts.sqSqSub] && (eng.heightMap!= null)){ setFlags(myConsts.erosionProc, !flags[myConsts.erosionProc]); } break;}//if key e
			case 't' :
			case 'T' : { if(flags[myConsts.sqSqSub]){ setFlags(myConsts.heightProc,!flags[myConsts.heightProc]);} break;}
			case 'p' :
			case 'P' : { faceNormalMult *= -1; break; }//if p pressed  toggle normal direction for per-face normal
			case ' ' :
			case ',' :
			case '.' : { setFlags(myConsts.simulate,!flags[myConsts.simulate]); break;}   // toggle simulation mode
			case 'n' :
			case 'N' :  { setFlags(myConsts.vertTerrainNormals, !flags[myConsts.vertTerrainNormals]);  break;}         // toggle per-vertex normals
			case 'm' :
			case 'M' :  { setFlags(myConsts.showH2OVelocity, false); eng.clearAllWater(); break;}   // clear all water from map
			case 'o' :
			case 'O' : { if ((flags[myConsts.sqSqSub]) && (flags[myConsts.heightMapMade])){ setFlags(myConsts.dataVisualization, true);} break; } //toggle through visualisation - set val in function
			case 'v' :
			case 'V' : { if (allFlagsSet(myConsts.Pipes,myConsts.heightMapMade)){ setFlags(myConsts.showH2OVelocity, !flags[myConsts.showH2OVelocity]);} break;} //toggle velocity vectors
			case '-' : 
			case '_' : { deltaT -= .01; if (deltaT <= 0) {deltaT = 0;} break;}
			case '+' :
			case '=' : { deltaT += .01; if (deltaT >= myConsts.globDeltaTMax) {deltaT = myConsts.globDeltaTMax;} break; }		
				
			case 's' :
			case 'S' : {
				if (!flags[myConsts.simulate])  { setFlags(myConsts.sqSqSub, true); gui.selectSubdivide(0); } 
				break;
			}
			case ';' : { simHECycles += 1; if (simHECycles >= 100)      {simHECycles = 100;} break;	}
			case ':' : { simHECycles -= 1; if (simHECycles <= 0)        {simHECycles = 0;} break;	}
			case 'l' : { simTWCycles += 1; if (simTWCycles >= 100)      {simTWCycles = 100;} break;	}
			case 'L' : { simTWCycles -= 1; if (simTWCycles <= 0)        {simTWCycles = 0;} break;	}
			case '\'' : { simRainCycles += 1; if (simRainCycles >= 100) {simRainCycles = 100;}break;}
			case '\"' : { simRainCycles -= 1; if (simRainCycles <= 0)   {simRainCycles = 0;} break;	}		
				
			default : {break;}
		}
		if (keyDown == false){ keyDown = cam.handleCamera(keyCode); }//if !keydown - handle camera movement 
		setFlags(myConsts.shiftKeyPressed, (keyCode == KeyEvent.VK_SHIFT));//check if shift pressed or not - need to modify if support for other coded keys is added	
	}//keypressed method
	
	/**
	 * handle playing playlist
	 */	
	public void playCurrentPlaylist(){//TODO : demo playlist
		if(currPlayList != -1){
			
			
			
		}//if currently valid playlist
	}//playCurrentPlaylist	
	
	/**
	 * will capture current boolean flag settings, erosion constants, and any other data settings relevant to current mesh, and print out to a file, to prep each mesh for demo
	 */
	public void captureMeshData(){
		myMeshDescriber meshDesc = mesh_vals.get(this.currMeshID);
		if(meshDesc == null){			meshDesc = new myMeshDescriber(this, this.gui, this.eng, this.sub);	} 
		else {							meshDesc.resetMeshDescriber();}
		mesh_flagNames = new ArrayList<String>();
		//erosion consts
		mesh_erosionConstsData = new double[4];
		mesh_simulationCntlData = new double[14];
		//first record flag values - names of all flags that are "on"
		for(int idx = 0; idx < myConsts.numFlags - 5; ++idx){	
			if(this.flags[idx]){
				mesh_flagNames.add((myConsts.flagNamesVals[idx] != null ? myConsts.flagNamesVals[idx] : ""));//use the name to list the values that are set true
			}
		}//for each bool flag idx
		//now record erosion variable values
		for(int idx = 0; idx < 4; ++idx){mesh_erosionConstsData[idx] = eng.matTypeAra[idx][eng.globMatType];}	
		//now record simulation variables
		mesh_simulationCntlData[0] = this.deltaT;						//deltaT
		mesh_simulationCntlData[1] = sub.riverWrapAroundPeriodMult;		//wrap around multiplier
		mesh_simulationCntlData[2] = eng.gravMult;
		mesh_simulationCntlData[3] = sub.numVertsPerSide;
		mesh_simulationCntlData[4] = sub.genRiverSlope;
		mesh_simulationCntlData[5] = sub.riverWidth;
		mesh_simulationCntlData[6] = eng.globLatErosionMult;
		mesh_simulationCntlData[7] = this.globSimRepeats;
		mesh_simulationCntlData[8] = this.vVecScaleFact;
		mesh_simulationCntlData[9] = eng.globRaindropMult;
		mesh_simulationCntlData[10] = eng.globAeolErosionMult;
		mesh_simulationCntlData[11] = this.simRainCycles;
		mesh_simulationCntlData[12] = this.simHECycles;
		mesh_simulationCntlData[13] = this.simTWCycles;
		mesh_simulationCntlData[14] = eng.rainVertMult;
		
		//set describer data
		meshDesc.setErosionKConsts(mesh_erosionConstsData);
		meshDesc.setErosionSimConsts(mesh_simulationCntlData);	
		meshDesc.setFlagNames(mesh_flagNames);
		this.mesh_vals.put(this.currMeshID, meshDesc);
		
	}//captureBoolFlagData
	
	//check if a passed flag idx is valid to set or clear - don't want to turn off gui-hiding, for instance, or demo/dev mode
	//want to return true if not to be ignored
	public boolean isValidDescriberFlag(int i){	return (!myConsts.ignoreFlagNames.contains(i));}	
	
	//use myConsts ara to set flags
	public void setGlobalFlagsFromDescriber(){
		for(int idx = 0; idx < this.flags.length; ++idx){ this.flags[idx] = (isValidDescriberFlag(idx) ?  false : this.flags[idx]);}//ignore if not valid flag
		for(int idx : myConsts.initMeshBoolFlagNames.get(this.currMeshID)){	if(isValidDescriberFlag(idx)){this.setFlags(idx, true);	}}
		this.flags[myConsts.useStoredErosionMeshVals] = true;
	}//setGlobalFlagsFromDescriber
	
	/**
	 * sets all global data from meshDescriber idx
	 */
	public void setGlobalsFromDescriber(){
		//erosion consts   
		if(this.flags[myConsts.useStoredErosionMeshVals]){//use the values defined in the constants structures for this mesh
			mesh_erosionConstsData = this.mesh_vals.get(this.currMeshID).erosionKConsts;
			mesh_simulationCntlData = this.mesh_vals.get(this.currMeshID).erosionSimConsts;
			//now record erosion variable values
			eng.globMatType = 0;
			for(int idx = 0; idx < 4; ++idx){			eng.matTypeAra[idx][eng.globMatType] = myConsts.initMeshErosionConsts.get(this.currMeshID)[idx];}
			
			//now record simulation variables
			this.deltaT							 = myConsts.initMeshSimulationConsts.get(this.currMeshID)[0] ;
			sub.riverWrapAroundPeriodMult		 = (int)Math.round(myConsts.initMeshSimulationConsts.get(this.currMeshID)[1]) ;
			eng.gravMult          	             = myConsts.initMeshSimulationConsts.get(this.currMeshID)[2] ;
			sub.numVertsPerSide                  = (int)Math.round(myConsts.initMeshSimulationConsts.get(this.currMeshID)[3]);
			sub.genRiverSlope                    = myConsts.initMeshSimulationConsts.get(this.currMeshID)[4] ;
			sub.riverWidth                       = myConsts.initMeshSimulationConsts.get(this.currMeshID)[5] ;
			eng.globLatErosionMult               = myConsts.initMeshSimulationConsts.get(this.currMeshID)[6] ;
			this.globSimRepeats                  = myConsts.initMeshSimulationConsts.get(this.currMeshID)[7] ;
			this.vVecScaleFact                   = myConsts.initMeshSimulationConsts.get(this.currMeshID)[8] ;
			eng.globRaindropMult                 = myConsts.initMeshSimulationConsts.get(this.currMeshID)[9] ;
			eng.globAeolErosionMult              = myConsts.initMeshSimulationConsts.get(this.currMeshID)[10];
			this.simRainCycles                   = (int)Math.round(myConsts.initMeshSimulationConsts.get(this.currMeshID)[11]);
			this.simHECycles                     = (int)Math.round(myConsts.initMeshSimulationConsts.get(this.currMeshID)[12]);
			this.simTWCycles 		             = (int)Math.round(myConsts.initMeshSimulationConsts.get(this.currMeshID)[13]);
			eng.rainVertMult					 = myConsts.initMeshSimulationConsts.get(this.currMeshID)[14];
		}
	}
	
	/**
	*  handle key releases
	*/
	public void keyReleased(){//prevent key-repeat, reset any keypress-related variables
		keyDown = false;
		cam.setRadDistMod(0);
		cam.keyAzmSpin = false;
		cam.keyAltSpin = false;
	}//keyReleased method	
	
	/**
	*  handle numeric keys being pressed in simulation mode - deprecated
	*/
	public void handleNumberKeyPressed(){}//numberKeyPressed sim	
	
	/**
	 * clear current mesh-related variables - should leave screen empty
	 */
	public void clearCurrentMesh(){
		eng.initEngVars();
		sub.initValues();	
	}

	/**
	*  initializes the system for non-loaded, generated mesh
	*/
	public void initVarsForGeneratedMesh(boolean multNormal){
		setFlags(myConsts.multByNormal,  multNormal);
			//use special code algorithm for square square subdivision
		setFlags(myConsts.sqSqSub, true);
			//draw QUADS as the type of polygon primitive
	}//initVarsForGeneratedMesh function	
	
	/**
	 * make all meshes here
	 */
	public void makeMeshes(){
		initVars();
		meshType = myConsts.meshType.get(currMeshID);
		initObjCam();
		if(myConsts.fileMesh == meshType){//file-based meshes - don't automatically make height map and add water
			setFlags(myConsts.meshIsGenerated,false);
			setFlags(myConsts.randomHeight, true);//
			//sub.read_quadMesh(myConsts.meshFileName.get(currMeshID), myConsts.quadFileSolid.contains(myConsts.meshFileName.get(currMeshID)) );	
			sub.read_quadMesh(myConsts.meshFileName.get(currMeshID),((null != myConsts.meshIsSolid.get(currMeshID)) && (myConsts.meshIsSolid.get(currMeshID))) );	
		} else {							//generated meshes
			if(myConsts.meshIsSolid.get(currMeshID)){//make solid/torus mesh
				 makeSolidMesh();				
			} else {
				setFlags(myConsts.meshIsGenerated,true);
				setFlags(myConsts.Pipes,true);
				sub.buildTestMesh(currMeshID, myConsts.meshIsSolid.get(currMeshID)); 
				eng.makeRiverDamMeshHeightMap(currMeshID, currMeshID );			
			}
		}
		
	}//makeMeshes

	/**
	 * build the torus generated mesh
	 */
	private void makeSolidMesh(){
		setFlags(myConsts.sqSqSub,true);
		setFlags(myConsts.multByNormal, true);
		//make torus or other solid mesh - currently only torus/knot
		sub.buildTorusMeshVerts(myConsts.torusValues.get(currMeshID).get("slices"), 
				myConsts.torusValues.get(currMeshID).get("facets"), 
				currMeshID, 
				myConsts.torusValues.get(currMeshID).get("radius1"), 
				myConsts.torusValues.get(currMeshID).get("radius2"), 
				myConsts.torusValues.get(currMeshID).get("knotRad")); 

//		eng.makeHeightMap();		
	}//makeTorus
	
	/**
	 * pass through for cam radius
	 */
	public double getCamRadSpin(){return this.cam.radSpin;}
	
	public void clampDeltaT(){
		//double oldDT = this.deltaT;
		this.deltaT *= 100000;
		long tmp = Math.round(this.deltaT);
		this.deltaT = tmp/100000.0;
	}//clampDeltaT
	
	/**
	 * getters setters	
	 */
	public double getDeltaT() {return this.deltaT;}
	public int getFaceNormalMult(){return this.faceNormalMult;}

	public void setDeltaT(double _delT) { 	this.deltaT = _delT; this.clampDeltaT();}
	
	/**
	 * main section
	 */
	public void settings(){
		fullScreen(P3D);
	}
	public void setup() {
		//size(displayWidth, displayHeight,P3D);
		//size(displayWidth, displayHeight,P3D);
		//hint(DISABLE_STROKE_PERSPECTIVE);//needed for new version of processing to enable stroke to work properly
		//this.requestFocus(); //request focus on the app to bypass broken focus setting in PApplet.runSketch
		initProgram();
		frameRate(120);		
	}//setup
	//run full screen
	//public boolean sketchFullScreen() {	  return true;	}		//TODO set to true for fullscreen

	// Draw the scene
	public void draw() {   
		initDisplay();
		scale(1,1,-1);//make axes right handed
//		if (this.flags[myConsts.useStamSolver]){	runStamFromDraw();		drawStamScene();} TODO : 3d fluid solver experiment removed for demo
//		else {
			drawCycles++;
			if (gui.inRange(gui.butPressed, myConsts.butIDRain,  myConsts.butIDThermal)){ 			gui.handleGuiChoiceButtons(gui.butPressed, false);}//handle repeating due to holding button down for rain or erosion
			runSimFromDraw();//execute sim if appropriate
			if((allFlagsFalse(myConsts.demoDevMode)) && (allFlagsSet(myConsts.debugMode, myConsts.renderGui))){  paintAxesByMesh();	}//if !demodev and debug and render gui, paint axes by mesh
			drawScene();//render results
			drawGui();
//		}//if stam else		
	}//draw	
	
	//paints axes by mesh for reference
	public void paintAxesByMesh(){
		pushMatrix();
		translate((float)(1.1 * this.globMinAra[myConsts.COORD_X]),0,(float)(1.1 * this.globMinAra[myConsts.COORD_Z]));	//move 10% past the lowest x and z coord, to paint axes
		cam.drawAxes();
		popMatrix();			
	}

	/**
	 * add a sailboat to the sailboat fleet
	 */
	public void addSailBoat(){
		myVector boatCoords = eng.findWaterCoords();
		mySailBoat sailBoat = new mySailBoat(this, gui, eng, sub, cam, boatCoords);
		this.sailBoatList.put(sailBoat.ID, sailBoat);	
	}//addsailboat
	/**
	 * remove sailboat from the fleet
	 */
	public void removeSailBoat(){
		if(this.sailBoatList.size() > 0){ 
			this.sailBoatList.remove(mySailBoat.IDcount - 1);
			mySailBoat.IDcount--;	
			if(this.sailBoatList.size() == 0){	this.setFlags(myConsts.sailBoat, false);}
		}		
	}//remove sailboat
	/**
	 * cycle through list of sailboats, evaluating their integerators
	 */
	private void moveSailBoat(){	for(mySailBoat boat : this.sailBoatList.values()){	boat.evalTimeStep(deltaT); boat.updateBoat(); }	}
	/**
	 * cycle through list of sailboats, evaluating their integerators
	 */
	private void drawSailBoat(){	for(mySailBoat boat : this.sailBoatList.values()){	boat.drawMe((float)(4*deltaT));	}	}
	/**
	 * execute stam fluid solver if appropriate
	 */
	public void runStamFromDraw(){
    	if (allFlagsSet(myConsts.addVelStamSolver)){//incrementally add velocity and density to stam solver
    		this.currSolver.addInitVelAndDensity(); 	
    	}
    	this.currSolver.calcStamCycle();//calculates the current cycle's worth of stam-solver velocity flow		
	}//runStamFromDraw
	
	public void drawStamScene(){
		if(allFlagsSet(myConsts.useStamSolver)){//if using stam solver, draw voxel grid
    		int i = 0;
    		//noStroke();
    		for(myVoxel vox : this.currSolver.getVoxelGrid()){ 
    			if ((!this.currSolver.getIs3D()) && (i >= this.currSolver.getAraSize())){ System.out.println(i + " is out of range");}
    			else {  							vox.drawMe();  		}//if not 3d, else 
    			i++;
    		}//for each voxel	    	
    	} 			
	}//drawStamScene

	/**
	*  draws the actual scene, called from draw()
	*
	*/
	public void drawScene(){
		pushMatrix();  
			if (yGuiModDel[0] == 1 ) {//handle gui selection
	    		int val = (int)Math.round(xGuiModDel[0]);
	    		gui.handleGuiChoiceVal( val , xGuiModDel[val], yGuiModDel[val]); 
	    		yGuiModDel[0] = 0;
	    	}	    
    		stroke(255);   
    		//draw polys - may be triangles or quads
//			rotate(-(float)rotationX,0,1,0);
//			rotate((float)rotationY,1,0,0);
			if (flags[myConsts.heightProc]){//if height map exists draw
    			if (flags[myConsts.heightMapMade]){//make sure height map ara or height map is not null, otherwise don't try to draw it
    				if((eng.heightMapAra != null) && (eng.heightMapAra.length != 0) && (eng.heightMapAra[0].length != 0)){  //check first for 2d ara
    					for(int xIDX = 0; xIDX < eng.heightMapAra.length; ++xIDX ){
    						for(int zIDX = 0; zIDX < eng.heightMapAra[0].length; ++zIDX){
    							eng.heightMapAra[xIDX][zIDX].drawMe();		    							
    						}
    					}	    					
    				} else if ((eng.heightMap != null) && (eng.heightMap.size() !=0)){
    					gui.print2Cnsl("drawing hash height map");
    					for (ArrayDeque<myHeightNode> heightDeque : eng.heightMap.values()){
    						for (myHeightNode heightNode : heightDeque){  heightNode.drawMe(); }//for each height node
    					}//for each height value      
    				}
    			}//if heightmap not null      
    		} else {//draw faces
				if((sub.faceListAra != null) && (sub.faceListAra.length != 0) && (sub.faceListAra[0].length != 0)){
					for(int xIDX = 0; xIDX < sub.faceListAra.length; ++xIDX ){
						for(int zIDX = 0; zIDX < sub.faceListAra[0].length; ++zIDX){
							if(sub.faceListAra[xIDX][zIDX] != null){sub.faceListAra[xIDX][zIDX].drawMe();}			
						}
					}
				} 
				else { 	    			
					for (myFace poly : sub.polyList.values()){       poly.drawMe();    }//for each poly
				}
    		}//if not heightMap  
	    popMatrix();
		if(allFlagsSet(myConsts.sailBoat)){this.drawSailBoat();}		//sailboat
//	    if ((flags[myConsts.savePic]) || ((flags[myConsts.recordVideo] && flags[myConsts.simulate]))) {  	
//	    	if(!flags[myConsts.simulate]){ setFlags(myConsts.savePic, false);} 
//	    	gui.savePic(); 
//	    }//if saving a pic-if not simulating, set savepic flag false, only save sequence of pics if simulation is running
		if(this.flags[myConsts.autoSubdivide]){	checkAndRunAutoSubdivide();}	    
	    cam.updateCamCounters();//update rotation/movement counters
	}//drawScene function
	
	/**
	 * if set to automated, animated subdivision, execute each subdivision cycle
	 */
	public void checkAndRunAutoSubdivide(){		
		//if iterating through automated subdivision for demo
		if ((sub.autoSubdivideCount > 0) && ( this.drawCycles % 30 == 0)){
			sub.autoSubdivideCount--;
			sub.subdivide_sqSqMesh();					
		} else if  (sub.autoSubdivideCount == 0){
			flags[myConsts.autoSubdivide] = false;
			eng.makeHeightMap();				
		}
	}//checkAndRunAutoSubdivide

	/**
	 * execute sim if appropriate, set relevant flags either way
	 */
	public void runSimFromDraw(){
		if (flags[myConsts.simulate]){//run simulation
			wasSimulating = true;
		    for (int i = 0; i < globSimRepeats; ++i){
		    	eng.runSim(); 
		    	if(allFlagsSet(myConsts.sailBoat)){moveSailBoat();}	
		    }//for each sim repeat
		    if(allFlagsSet(myConsts.useMatlabFESolve) && (null != this.mat)){ 	eng.calc2ndOrderFEMMatLab();}//run 1 per draw only			
		}//simulating
		else {//not currently simulating
			if (wasSimulating) { //turn off lights at end of simulating run
				wasSimulating = false;
				gui.offNotifyLights();
		    }//if we were simulating last draw, clean up 
			
		}//else not simulating		
	}//drawRunSim
}//sqSqErosionSimGlobal class

