package sqSqErosionSimPkg;

import processing.core.*;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
//import java.util.Set;
import java.util.SortedSet;
//import java.util.TreeSet;

/**
 * class holding all the gui functionality for the erosion simulator
 * @author John
 */
public class myGui {
	private sqSqErosionSimGlobal p;
	private mySqSqEroder eng;
	private mySqSqSubdivider sub;
	private myFEMSolver mat;
	//sketch x and y dimensions  displayWidth, displayHeight
	
	private final int globClickX;		//originally 1380 - actual sketch width 
	private final int globClickY;		//originally 980 - actual sketch height
	//private final int globClickZ = (int)((globClickX + globClickY)/2.0f);	//possibly use in translations to give relative relation to original z	= 1180 originally
	
	//gui color for display of flag value
	public myVector[] flagColor;

	//constant path strings for different file types
	public static final String fileDelim = "\\";
	public static final String polyPath = "polyFiles";
	public static final String iconPath = "icons";
	public Calendar now;
	//counts how many saves have been made for current pic
	private int saveClickCount = 0;
	//text strings to display various boolean flag values
	private String[] flagNames;
	//array of images used to display information regarding current subdivision process being on
	private HashMap<Integer, PImage> dispSubBoolIcons;
	//array of images used to display information regarding current subdivision process being off
	private HashMap<Integer, PImage> dispSubBoolIconsOff;
	//width of iconsOld, height of iconsOld used to display bool vals
	//private float boolIconSizeX = globClickX  * 18/1920.0f, boolIconSizeY = globClickY * 18/1200.0f;// 18 x 15;
	private float boolIconSizeX, boolIconSizeY;
	//number of subdivision boolean display iconsOld
	private static final int numDispIcons = 5;
	//array of images for display on gui as buttons
//	//old arrays for holding icons based on 2 pages of icons
//	public PImage[][] iconsOld;
//	//array of images for display on gui as buttons being pressed
//	public PImage[][] iconsInvOld;
	//arraylist of alt iconsOld and inverse alt iconsOld for buttons
	//most recent mesh chosen, button id therof
	private int chosenMeshChoiceButID = -1;

	//array of images for display on gui as buttons
	public PImage[][][] icons;
	//array of images for display on gui as buttons being pressed
	public PImage[][][] iconsInv;
	//arraylist of alt icons and inverse alt icons for buttons
	
	//public HashMap<Integer,PImage> altIcons;
	//num rows iconsOld - related to 2 pages of icons
	public static final int iconRowsOld = 12;
	//num cols iconsOld
	public static final int iconCols = 6;
	
	//num rows icons - single page
	public static final int iconRows = 9;
	//these are primary icons, using same icons as in dev mode
	public static final int demoIconRows = 2;
	public static final int demoIconCols = 18;
	//size of sphere used for notification lights - in pixels in gui
	public final float sphereRad = 5;
	
	//DEMO 
	//multiplier for demo icon size
	public final float icoScale = 1.35f;//amount to divide main icon size and pad by
	//multiplier for small demo icon size
	public final float smIconScale = 2.0f;
	
	
	//these are for 2ndary icons, using new icons : idx 0 = terrain, 1 = h2o, 2 = sed, etc
	public PImage[][] demoSMMeshBoolIcons;
	public PImage[][] demoSMMeshBoolIconsInv;
	
	//these icons are for small simulation booleans
	public PImage[][] demoSMSimButIcons;
	public PImage[][] demoSMSimButIconsInv;
	
	//idxs for each type of icon result: 
	public final int dSmIcoMesh = 0;
	public final int dSmIcoEdge = 1;
	public final int dSmIcoNorm = 2;
	//idx for each type of icon
	public final int dSmIcoTer 		= 0;
	public final int dSmIcoH2O 		= 1;
	public final int dSmIcoSed 		= 2;
	public final int dSmIcoSedCap 	= 3;	
	public final int dSmIcoSedConc 	= 4;
	
	public boolean popUpMenuOn;							//this is true if any menu is on
	//flags to tell whether or not particular pop up menus are locked as being on
	public boolean[] lockedPopUpMenuFlags;
	
	//color of a particular open lock
	public int[][] lockColor;
	
	//flags to tell whether or not particular pop up menus are being displayed
	public boolean[] popUpMenuFlags;
	//menu visibility in gui for dev via switch/mouse-over
	//both dev and demo
	//display dev/demo mode window
	public static final int dispDevDemoButtonMenu		= 0;
	//dev
	//boolean window
	public static final int dispDevBoolsMenu			= 1;
	//button menu
	public static final int dispDevButtonMenu			= 2;
	//debug menu
	public static final int dispDevDebugMenu 			= 3;
	//sim values (view only, upper left)
	public static final int dispDevDataDispMenu			= 4;
	//data entry values
	public static final int dispDevDataEntryMenu		= 5;
	//console
	public static final int dispDevConsoleMenu			= 6;
	//demo
	//demo mesh button menu
	public static final int dispDemoButtonMenu			= 7;
	//demo mesh bools button menu
	public static final int dispDemoMeshBoolButtonMenu	= 8;
	//demo sim bools button menu
	public static final int dispDemoSimBoolButtonMenu	= 9;
	//add more demo here	
	//number of mouse-over menus
	public static final int numMouseMenus				= 10;
	
	
	public static final float dbColSepVal = 22;//(globX * 0.015942029f);		//22
	public static final float dbFinColSep = 15;//(globX * 0.01086956522f);	//15
	public static final float[] debugColSeps			= {dbColSepVal, dbColSepVal, dbColSepVal, dbColSepVal, dbColSepVal, dbFinColSep};
	//consts defining locations of each row and column in the debug window

	//arrays holding idxs of pop up zones to check for mouse movement
	public final int[] devPopUpMenus = {dispDevDemoButtonMenu, dispDevBoolsMenu, dispDevButtonMenu, dispDevDebugMenu, dispDevDataDispMenu, dispDevDataEntryMenu, dispDevConsoleMenu};
	public final int[] demoPopUpMenus = {dispDevDemoButtonMenu, dispDemoButtonMenu, dispDemoMeshBoolButtonMenu, dispDemoSimBoolButtonMenu};	
	

	public static final int modSubBoolsIDX 		= 0;
	public static final int	modSimBoolsIDX 		= 1;
	public static final int modTerBoolsIDX 		= 2;
	public static final int modH2OBoolsIDX 		= 3;
	public static final int modSedBoolsIDX 		= 4;
	public static final int modSedCapBoolsIDX 	= 5;
	public static final int modSedConcBoolsIDX 	= 6;
	public static final int modMiscBoolsIDX 	= 7;
	public static final int modDispBoolsIDX 	= 8;
	public static final int debugBoolsIDX 		= 9;		//displayed only when debugMode enabled
	
	public static final int[] modBoolsDispNoShift 		= {modSimBoolsIDX, modTerBoolsIDX, modH2OBoolsIDX, modSedBoolsIDX, modSedConcBoolsIDX, modSedCapBoolsIDX, modMiscBoolsIDX};
	public static final int[] modBoolsDispShift 		= {modSubBoolsIDX, modDispBoolsIDX, modMiscBoolsIDX};	
		
	public static final int[] modBoolsDispNoShiftNoMisc = {modSimBoolsIDX, modTerBoolsIDX, modH2OBoolsIDX, modSedBoolsIDX, modSedConcBoolsIDX, modSedCapBoolsIDX};
	public static final int[] modBoolsDispShiftNoMisc 	= {modSubBoolsIDX, modDispBoolsIDX};	

	public HashMap<Integer, HashMap<Integer, Float[]>> modBoolLocsMap;
		//idx of label array
	public static final int lbl_MaxH2O       	     	= 0;
	public static final int lbl_MaxSed       	     	= 1;
	public static final int lbl_MaxAlt       	     	= 2;
	public static final int lbl_MinH2O       	     	= 3;
	public static final int lbl_MinSed       	     	= 4;
	public static final int lbl_MinAlt          	 	= 5;
	public static final int lbl_deltaT      	     	= 6;
	public static final int lbl_aeolianScaleFact   	 	= 7;
	public static final int lbl_cycPerRain       	 	= 8;
	public static final int lbl_cycPerDraw        	 	= 9;
	public static final int lbl_RaindropVol      	 	= 10;
	public static final int lbl_cycPerHE       	  	 	= 11;
	public static final int lbl_cycPerTW         	 	= 12;
	public static final int lbl_globMatType      	 	= 13;
	public static final int lbl_Kc                   	= 14;
	public static final int lbl_Kd                	 	= 15;
	public static final int lbl_Kw                	 	= 16;
	public static final int lbl_Ks               	 	= 17;
	public static final int lbl_CurrVisType       	 	= 18;
	public static final int lbl_NumNodes         	 	= 19;
	public static final int lbl_SimCycles        	 	= 20;
	public static final int lbl_WindDir          	 	= 21;
	public static final int lbl_deltaXZ           	 	= 22;		//deltaNodeDist
	public static final int lbl_RainfallCountdown	 	= 23;
	public static final int lbl_vVectorScaleFact  	 	= 24;
	public static final int lbl_MaxX            	 	= 25;
	public static final int lbl_MinX            	 	= 26;
	public static final int lbl_MaxY            	 	= 27;
	public static final int lbl_MinY            	 	= 28;
	public static final int lbl_MaxZ             	 	= 29;
	public static final int lbl_MinZ            	 	= 30;
	//public static final int lbl_RiverMeshPeriod		 	= 31;	
	public static final int lbl_RiverWrapMeshPeriod  	= 32; 
	public static final int lbl_TotH2O				 	= 33;
	public static final int lbl_TotSed					= 34;
	public static final int lbl_DebugSliceX				= 35;
	public static final int lbl_DebugSliceZ				= 36;	
	public static final int lbl_DebugSliceSizeX			= 37;
	public static final int lbl_DebugSliceSizeZ			= 38;	
	public static final int lbl_SedCap					= 39;
	public static final int lbl_latErosionScaleFact 	= 40;
	public static final int lbl_calcMaxDeltaT		 	= 41;
	public static final int lbl_vIncrByGravMult		 	= 42;
	public static final int lbl_globalKVal				= 43;	
	public static final int lbl_numVertsPerSide			= 44;
	public static final int lbl_genRiverSlope			= 45;
	public static final int lbl_RiverWidth				= 46;
	public static final int lbl_dmoShadeByVerts			= 52;
	public static final int lbl_dmoDispMesh				= 53;
	public static final int lbl_dmoDispEdges			= 54;
		
	public static final int numLblConsts = 55;//one more than last label idx above
		//gui labels for display values/constants
	public String[] constLabels;	
	//these are small icons, for shading by normal, show mesh, show edges
	public final int demoSmMeshBoolIconRows = 3, demoSmMeshBoolIconCols = 5;
	
	//these are small icons, for shading by normal, show mesh, show edges
	public final int demoSmSimButIconRows = 2, demoSmSimButIconCols = 6;

	public float iconSizeX,iconSizeY,iconPadX,iconPadY,demoIconSizeX,demoIconSizeY,demoIconPadX,
				demoIconPadY,demoSmIconSizeX,demoSmIconSizeY,demoSmIconPadX,demoSmIconPadY,lockClickXsize,lockClickYsize;
	
	//clickable region
	public int demoButtonULClickX,demoButULSmMeshBoolX,demoButULSmSimBoolX,demoButtonULClickY, 
				demoButtonLRClickX,demoButLRSmMeshBoolX, demoButLRSmSimBoolX, demoButtonLRClickY,
				devDemoButL, devDemoButR, devDemoButT, devDemoButB, guiDispButL, guiDispButR, guiDispButT, guiDispButB;
	
	//
	//	mouse hotspot zones for menu activation - lowx/hix/lowy/hiy
	//	
	//mouse zones
	public float[][] popUpMenuZones ={{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0}};  				//dispDemoSimBoolsMenu		TODO:
	//x/y locations of upper left corner of lock for each menu
	public float[][] popUpMenuLockLocs = {{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0}};

	//dev/demo mode iconsOld
	public PImage[] demoDevIcons;
	public final int DemoMode = 0;
	public final int DevMode = 1;

	//dev mode
	public float butULBlockX, butULBlockY, butLRBlockX, butLRBlockY, butDistX, butDistY, debugBoxX1, debugBoxX2, debugBoxY1, debugBoxY2; 		//= (globY * 0.09693878f);//95f;


	//new vals : 525, 690, 855, 1025, 1190, 1375
	public float[] dbCol						= {0,0,0,0,0,0};//{335, 480, 624, 766, 915, 1095};
	//new vals : 40, 65, 91, 117, 142
	public float[] dbRow						= {0,0,0,0,0,0};
	public static final int NumDebugBoolRows			= 4;
	
	//click location of debug menu
	public float debugULBlockX, debugULBlockY, debugLRBlockX, debugLRBlockY, guiModValsULBlockX, guiModValsUnderMenuBlockX, guiModValsULBlockY, guiModValsUnderMenuBlockY;
	//size of text
	public float fontSize;// = this.globClickY * (17.0f/1200.0f);

	//boolean selection area bounds
	public float clkWidth, minModBoolLocY, maxModBoolLocY, minModBoolLocX, modBoolSep, miscBoolsLoc, modSimBoolsLocY, modTerBoolsLocY, modH2OBoolsLocY,
						modSedBoolsLocY, modSedConcBoolsLocY, modSedCapBoolsLocY, modMiscBoolsLocY, modSubBoolsLocY, modDispBoolsLocY;//
	
	public float[] minModBoolLocYAra = {0, 0, 0, 0, 0, 0, 0, 0, 0};//modify this to change display group hotclick locations
		
	//gui data entry value hotspot locations

	//y block 1
	public int guiValYblock1,guiValYblock2, guiValYblock3;
	
	//xBlock(col) 1
	public int guiValXblock1, guiValXblock2, guiValXblock3, guiValXblock4, guiValXblock5, guiValXblock6, guiValXblock7, guiValXblock8, guiValXblock9;//
	
	
	//lightCounter holds a countdown to determine how many cycles a particular notification light stays active
	public int[] lightCounter;
	//number of cycles the lights should stay active once triggered
	public static final int lightCountCycles = 5;
	//array of light colors for notification lights
	public int[][] lightCountColors = {{0, 0, 255}, {0, 255, 0},{255, 0, 0}};
	//array of labels for cylce count notification lights
	public String[] lightCountLbls = {"Rain", "Fluvial", "Aeolian"};
	//number of notification lights on scene
	public static final int numNotifyLights = 3;
	//idx's for each type of notification light
	public static final int ltRainIDX = 0;
	public static final int ltFluvialIDX = 1;
	public static final int ltAeolianIDX = 2;

	//console output, to take the place of println
	public String[] dispConsoleAra;
	//number of lines to store in console
	public static final int numConsoleLines = 30;
	//width of characters for screen console
	public static final int consoleWidth = 120;
	//color used for tiers of terrain
	public myVector[] vecTerrainColorFill = {  //150 78 13
		new myVector (50, 39, 27),
        new myVector (100, 45, 33),
        new myVector (125, 57, 33),
        new myVector (150, 78, 55),
		new myVector (189, 109, 60), 
        new myVector (190, 118, 80),
        new myVector (225, 194, 150),	
		new myVector (200, 160, 120),
	};//vecFEMColorFill
	
	//color used for tiers of finite element calculation
	public myVector[] vecFEMColorFill = {  
			new myVector (0,0,100),
			new myVector (0,0,255),
	        new myVector (0, 100, 225),
	        new myVector (20, 150, 200),
	        new myVector (100, 200, 155),
			new myVector (50, 255, 50), 
			new myVector (155, 200, 100), 
	        new myVector (200, 150, 20),
	        new myVector (225, 100, 0),	
			new myVector (255,0,0),
			new myVector (100,0,0),
		};	
	
	//color used for different depths of water		
	public myVector[] vecWaterColorFill = {  
		new myVector (0, 160,180),		//alpha : 80
		new myVector (0,50,120)			//alpha : 220
	};
	//color used for tiers of terrain		TODO :
	public myVector[] vecWaterColorAmbient = {  
		new myVector (0, 160,180),		//alpha : 80
		new myVector (0, 50, 120)			//alpha : 220
	};
	public float[] alphaWaterColorFill = {
		80,
		220	
	};
	
	//placeholder for dispText when passing unneeded color value
	public static final int gui_null = -1;
	//color indexes
	public static final int gui_White = 0;

	public static final int gui_Gray = 1;
	public static final int gui_Red = 2;
	public static final int gui_Blue = 3;
	public static final int gui_Green = 4;
	public static final int gui_Yellow = 5;
	public static final int gui_Cyan = 6;
	public static final int gui_Magenta = 7;
	
	public static final int gui_LightGray = 8;
	public static final int gui_LightRed = 9;
	public static final int gui_LightBlue = 10;
	public static final int gui_LightGreen = 11;
	public static final int gui_LightYellow = 12;
	public static final int gui_LightCyan = 13;
	public static final int gui_LightMagenta = 14;

	public static final int gui_DarkMagenta = 15;
	public static final int gui_DarkCyan = 16;
	public static final int gui_DarkYellow = 17;
	public static final int gui_DarkGreen = 18;
	public static final int gui_DarkBlue = 19;
	public static final int gui_DarkRed = 20;
	public static final int gui_DarkGray = 21;

	public static final int gui_TransBlack = 22;
	
	public static final int gui_FaintGray = 23;
	public static final int gui_FaintRed = 24;
	public static final int gui_FaintBlue = 25;
	public static final int gui_FaintGreen = 26;
	public static final int gui_FaintYellow = 27;
	public static final int gui_FaintCyan = 28;
	public static final int gui_FaintMagenta = 29;
	public static final int gui_Black = 30;
	public static final int gui_TerrainColor = 31;
	public static final int gui_WaterColorDeep = 32;
	public static final int gui_WaterColorShallow = 33;
	public static final int gui_SedimentColor = 34;
	public static final int gui_KDispColor = 35;
	public static final int gui_Sed1DispColor = 36;
	public static final int gui_SedSrcNodeColor = 37;
	public static final int gui_SedCapColor = 38;
	public static final int gui_SedConcColor = 39;
	public static final int gui_TerrainVertColor = 40;	
	
	public static final int gui_TransRed = 41;
	public static final int gui_TransBlue = 42;
	public static final int gui_TransGreen = 43;
	public static final int gui_TransYellow = 44;
	public static final int gui_TransCyan = 45;
	public static final int gui_TransMagenta = 46;
	public static final int gui_TransGray = 47;
	public static final int gui_BoulderColor = 48;
	
	public static final int gui_boatBody = 49;
	public static final int gui_boatSail = 50;
	public static final int gui_boatEmblem = 51;
	
	//string to hold data being displayed about visualisation
	public String dataVis; 
	//text font to display on sim screen
	public PFont font1;
	//formatters
	public DecimalFormat df1,df2,df3,df4,df5,df6,df7,df8;
	//if an icon button was pressed, will equal what button, -1 if no button pressed
	public int butPressed = -1;
	//whether or not gui input has changed data values
	public boolean guiDataChanged = false;
	//index of value changed by gui input
	public int guiDataValueChanged = -1;		

	//names of alt icon images on disk
	public static final ArrayList<String> altIconNamesAra = new ArrayList<String>(Arrays.asList (new String[]  {"\\ico412", "\\ico1012"} ));
	
	//debug data
	//file name for debug text file
	private String debugPipesTxtFileName;
	//writer object 
	private PrintWriter debugOutput;
	//mesh settings file
	//file name for debug text file
	private String meshVarsSettingsTxtFileName;
	//writer object 
	private PrintWriter meshVarsOutput;
	
	//for mouose movement
	private double xMod, yMod;
	
	//debug node ID - used to highlight a particular node
	public int debugNodeID = 0;


	/////////////////
	///end gui-related variables
	/////////////////	
	public myGui(sqSqErosionSimGlobal p, mySqSqEroder e, myFEMSolver _mat){
		this.p = p;
		this.eng = e;
		this.mat = _mat;
		
		globClickX = p.displayWidth;		//originally 1380 - actual sketch width 
		globClickY = p.displayHeight;		//originally 980 - actual sketch height

		//sets variables that rely upon globClickX and globClickY
		setSizeDepVars();
		
		System.out.println(p.sketchPath() + fileDelim +"Data" + fileDelim );
		//font1 = p.createFont("AgencyFB", 96);					//changing to newer version of processing libraries changed how fonts worked so this doesn't work anymore
		font1 = p.createFont("AgencyFB", this.fontSize);
		p.textFont(font1,this.fontSize);	
		p.textMode(PApplet.MODEL);
		p.textAlign(PApplet.RIGHT);
		//get time used for file saving
		now = Calendar.getInstance();		
		dispConsoleAra = new String[numConsoleLines];
		for (int i = 0; i < numConsoleLines; ++i){ dispConsoleAra[i] = ""; }
		//initialize light counter
		lightCounter = new int[3];
		//init gui-related variables and structures
		//formatters
		df1 = new DecimalFormat("####.####");
		df2 = new DecimalFormat("#.##");
		df3 = new DecimalFormat("##00.0000");
		df4 = new DecimalFormat("###.00000");
		df5 = new DecimalFormat("##.#########");
		df6 = new DecimalFormat("#.#########E0");
		df7 = new DecimalFormat("#.#######E0");
		df8 = new DecimalFormat("###.##");
		//popup menu flags init to false
		popUpMenuFlags = new boolean[myGui.numMouseMenus];
		lockedPopUpMenuFlags = new boolean[myGui.numMouseMenus];
		popUpMenuOn = false;
		lockColor = new int[myGui.numMouseMenus][2];
		for(int i = 0; i < myGui.numMouseMenus; ++i){
			lockColor[i][0] = (int) p.random(15,21);
			lockColor[i][1] = (int) p.random(1,7);			
		}
		loadIcons();
		setDataVisStr();
		initGuiOnce();
	}//constructor
	
	//sets variables that rely upon globClickX and globClickY
	private void setSizeDepVars(){  			//TODO
		boolIconSizeX = globClickX  * 18/1920.0f;
		boolIconSizeY = globClickY * 18/1200.0f;// 18 x 15;
		//size of iconsOld x
		iconSizeX = (globClickX * 53.0f/1920.0f);//(globX * .03841f);	// 73;
		//size of iconsOld y
		iconSizeY = (globClickY * 45.0f/1200.0f);	//45;
		//size of icon pad x
		iconPadX = (globClickX * 55.0f/1920.0f);		//55;
		//size of icon pad y
		iconPadY = (globClickY * 47.0f/1200.0f); 	//47;	
			
		//DEMO 
		//x size of demo icons
		demoIconSizeX = iconSizeX/icoScale;
		//y size of demo icons
		demoIconSizeY = iconSizeY/icoScale;
		//x space between of demo icons
		demoIconPadX = (globClickX * 2/1920.0f);
		//y size of demo icons
		demoIconPadY = (globClickY * 2/1200.0f);

		//x size of small demo icons
		demoSmIconSizeX = iconSizeX/smIconScale;
		//y size of demo icons
		demoSmIconSizeY = iconSizeY/smIconScale;
		//x space between of demo icons
		demoSmIconPadX = (globClickX * 2/1920.0f);
		//y size of demo icons
		demoSmIconPadY = (globClickY * 2/1200.0f);
		
		//clickable region
		//upper left x coord for primary buttons
		demoButtonULClickX = (int)(globClickX * 5.0f/1920.0f);
		//upper left x coord for smaller buttons for mesh booleans
		demoButULSmMeshBoolX = (int)(globClickX * 900/1920.0f);		//at 900 on 1920-wide screen
		//upper left x coord for smaller buttons for simulation booleans
		demoButULSmSimBoolX = (int)(globClickX * 1200.0/1920.0f);		//at 800 on 1920-wide screen
		//upper left y coord
		demoButtonULClickY = (int)(globClickY * 1130.0f/1200.0f);//1130;
		//lower right x coord
		demoButtonLRClickX = demoButtonULClickX + (int)((demoIconCols * demoIconSizeX ) + ((demoIconCols-1) * demoIconPadX));				//(int)(globClickX * 740.0f/1920.0f);	
		//lower right x coord for small icons for mesh bools
		demoButLRSmMeshBoolX = demoButULSmMeshBoolX + (int)((demoSmMeshBoolIconCols * demoSmIconSizeX) + ((demoSmMeshBoolIconCols-1) * demoSmIconPadX));
		//lower right x coord for small icons for mesh bools
		demoButLRSmSimBoolX = demoButULSmSimBoolX + (int)((demoSmSimButIconCols * demoIconSizeX) + ((demoSmSimButIconCols-1) * demoIconPadX));
		//lower right y coord
		demoButtonLRClickY = globClickY;
		
		//
		//	mouse hotspot zones for menu activation - lowx/hix/lowy/hiy
		//	
		//mouse zones
		popUpMenuZones =new float [][]{			
				{this.globClickX * (1728.0f/1920.0f), this.globClickX, 						this.globClickY * (1120.0f/1200.0f), this.globClickY},          		//dispDevDemoButtonMenu       
				{this.globClickX * (1700.0f/1920.0f), this.globClickX,						0, this.globClickY * (665.0f/1200.0f)},     						//dispDevBoolsMenu
				{this.globClickX * (1500.0f/1920.0f), this.globClickX, 						this.globClickY * (672.0f/1200.0f), this.globClickY * (1110.0f/1200.0f)},   		//dispDevButtonMenu 
				{this.globClickX * (490.0f/1920.0f), this.globClickX * (1420.0f/1920.0f),	0, this.globClickY * (190.0f/1200.0f)},     						//dispDevDebugMenu           
				{0, this.globClickX * (450.0f/1920.0f),										0, this.globClickY * (250.0f/1200.0f)},          					//dispDevDataDispMenu                  
				{0, this.globClickX * (1710.0f/1920.0f),									this.globClickY * (1050.0f/1200.0f), this.globClickY},       			//dispDevDataEntryMenu                   
				{0, this.globClickX * (296.0f/1920.0f), 									this.globClickY * (540.0f/1200.0f), this.globClickY},					//dispDevConsoleMenu			
				{0, demoButULSmMeshBoolX,													this.globClickY * (1050.0f/1200.0f), this.globClickY},   				//dispDemoButtonMenu
				{demoButULSmMeshBoolX, demoButULSmSimBoolX, 								this.globClickY * (1050.0f/1200.0f), this.globClickY},				    //dispDemoMeshBoolsMenu
				{demoButULSmSimBoolX, this.globClickX * (1300.0f/1920.0f), 					this.globClickY * (1050.0f/1200.0f), this.globClickY}};  				//dispDemoSimBoolsMenu		TODO:
		//click zone to lock a menu on
		lockClickXsize = globClickX/128.0f;
		lockClickYsize = globClickY/48.0f;
		//x/y locations of upper left corner of lock for each menu
		popUpMenuLockLocs = new float[][]{
				{globClickX * (1790.0f/1920.0f),	globClickY * (1175.0f/1200.0f)},        //dispDevDemoButtonMenu   
				{globClickX * (1735.0f/1920.0f), 	globClickY * (15.0f/1200.0f)},       //dispDevBoolsMenu        
				{globClickX * (1900.0f/1920.0f),	globClickY * (1072.0f/1200.0f)},        //dispDevButtonMenu       
				{globClickX * (500.0f/1920.0f),  	globClickY * (20.0f/1200.0f)},       //dispDevDebugMenu        
				{globClickX * (280.0f/1920.0f), 	globClickY * (15.0f/1200.0f)},       //dispDevDataDispMenu     
				{globClickX * (140.0f/1920.0f), 	globClickY * (1110.0f/1200.0f)},        //dispDevDataEntryMenu    
				{0, 					   			globClickY * (562.0f/1200.0f)},       //dispDevConsoleMenu      
		        {0, 					   			globClickY * (1110.0f/1200.0f)},       		//dispDemoButtonMenu      
		        {demoButULSmMeshBoolX, 				globClickY * (1110.0f/1200.0f)},       //dispDemoButtonMenu  
		        {demoButULSmSimBoolX,				globClickY * (1110.0f/1200.0f)}
		};         
		//location of dev/demo mode icon
		devDemoButL = (int) (globClickX * (1867.0f/1920.0f));		//1335;
		devDemoButR = (int) (globClickX);   	
		devDemoButT = (int) (globClickY * (1145.0f/1200.0f)); 		//935;
		devDemoButB = (int) (globClickY);  
		//location of disp/hide gui icon
		guiDispButL = (int)(globClickX * (1801.0f/1920.0f)); 		//1295;
		guiDispButR = devDemoButL - 1; 					//1334;
		guiDispButT = (int)(globClickY * (1163.0f/1200.0f)); 		//950;
		guiDispButB = devDemoButB;  					//975;

		//dev mode
		//upper left corner of button block - x
		butULBlockX = (globClickX * (1583.0f/1920.0f));  		//1583
		//upper left corner of button block - y  -> new value is 670
		butULBlockY = (globClickY * (670.0f/1200.0f));		//	(globClickY * .625f);			
		//lower right corner of button block - x
		butLRBlockX = devDemoButR;   				//1920
		//lower right corner of button block - y
		butLRBlockY = (globClickY * (1100.0f/1200.0f));   			//1040 --> 1100
		//click distance between ul corners of buttons - x
		butDistX = (globClickX * (57.0f/1920.0f));				//57;
		//click distance between ul corners of buttons - y
		butDistY = (globClickY * (50.0f/1200.0f));			//50;	
		//draw location of debug menu
		debugBoxX1 		= -this.globClickX/4.0f;		// -(globX * 0.1050724638f); //-145f;
		debugBoxX2		= this.globClickX/4.0f; 		//= (globX * 0.2174f);//300f;
		debugBoxY1 		= 0;							//top is top
		debugBoxY2		= this.globClickY/6.8f; 		//= (globY * 0.09693878f);//95f;


		//new vals : 525, 690, 855, 1025, 1190, 1375
		dbCol						= new float[] {(globClickX * (525.0f/1920.0f)),  (globClickX * (690.0f/1920.0f)),
															(globClickX * (855.0f/1920.0f)), (globClickX * (1025.0f/1920.0f)), 
															(globClickX * (1190.0f/1920.0f)), (globClickX * (1375.0f/1920.0f))};//{335, 480, 624, 766, 915, 1095};
		//new vals : 40, 65, 91, 117, 142
		dbRow						= new float[] {(globClickY * (40.0f/1200.0f) ), (globClickY * (65.0f/1200.0f)), 
														   (globClickY * (91.0f/1200.0f) ), (globClickY * (117.0f/1200.0f)), 
														   (globClickY * (142.0f/1200.0f)), (globClickY * (167/1200.0f))};
		
		//click location of debug menu
		debugULBlockX			= dbCol[0];	
		debugULBlockY 			= dbRow[0];
		debugLRBlockX 			= dbCol[dbCol.length-1];
		debugLRBlockY 			= dbRow[dbRow.length-1];
			
		//click location of modfiable gui buttons- since at bottom of screen max x and max y are screen edges
		guiModValsULBlockX = 		(globClickX * (47.0f/1920.0f));				//24;
		guiModValsUnderMenuBlockX = 	butULBlockX;					//1000;			//where menu block begins
		guiModValsULBlockY = 		(globClickY * (1107.0f/1200.0f));				//904;
		guiModValsUnderMenuBlockY = 	(globClickY * (1141.0f/1200.0f));//932;
				
		//size of text
		fontSize = this.globClickY * (17.0f/1200.0f);

		//boolean selection area bounds
		clkWidth = globClickX;								//1380;  
		minModBoolLocY = (globClickY * 146.0f/1200.0f);		//148;//from top
		maxModBoolLocY = (globClickY * 660.0f/1200.0f);//(int)(globClickY * .607143);		//595;
		minModBoolLocX = (globClickX * 1700.0f/1920.0f);//(int)(globClickX * .833334);		//1150; 
		modBoolSep	   = 2;	
		miscBoolsLoc = (globClickY * 624.0f/1200.0f);	//location of miscBools box in y

		modSimBoolsLocY 	= minModBoolLocY;
		modTerBoolsLocY 	 = (modSimBoolsLocY + modBoolSep + ((fontSize + modBoolSep) * (myConsts.modBoolsAra[modSimBoolsIDX].length)));
		modH2OBoolsLocY 	 = (modTerBoolsLocY + modBoolSep + ((fontSize + modBoolSep) * (myConsts.modBoolsAra[modTerBoolsIDX].length)));
		modSedBoolsLocY 	 = (modH2OBoolsLocY + modBoolSep + ((fontSize + modBoolSep) * (myConsts.modBoolsAra[modH2OBoolsIDX].length)));
		modSedConcBoolsLocY = (modSedBoolsLocY + modBoolSep + ((fontSize + modBoolSep) * (myConsts.modBoolsAra[modSedBoolsIDX].length)));
		modSedCapBoolsLocY  = (modSedConcBoolsLocY + modBoolSep + ((fontSize + modBoolSep) * (myConsts.modBoolsAra[modSedConcBoolsIDX].length)));
		modMiscBoolsLocY 	 = (modSedCapBoolsLocY + modBoolSep + ((fontSize + modBoolSep) * (myConsts.modBoolsAra[modSedCapBoolsIDX].length)));//(int)(globClickY * .554062); 		// 542;

		modSubBoolsLocY = minModBoolLocY;
		modDispBoolsLocY = (modSubBoolsLocY + ((fontSize + modBoolSep) * (myConsts.modBoolsAra[modSubBoolsIDX].length)));		//224;			//display related bools
		
		minModBoolLocYAra = new float[] {modSubBoolsLocY, modSimBoolsLocY, modTerBoolsLocY, modH2OBoolsLocY, 
														modSedBoolsLocY, modSedCapBoolsLocY, modSedConcBoolsLocY, 
														modMiscBoolsLocY, modDispBoolsLocY};//modify this to change display group hotclick locations
		//gui data entry value hotspot locations
		//y block 1
		guiValYblock1 = (int)(globClickY * (1135.0f/1200.0f));		//1135
		//y block 2
		guiValYblock2 = (int) (globClickY * (1155.0f/1200.0f));		//1155
		//y block 3
		guiValYblock3 = (int) (globClickY *  (1178.0f/1200.0f));		//1178	
		
		//xBlock(col) 1
		guiValXblock1 = (int)(globClickX * (10.0f/1920.0f));		//10
		//xBlock 2 
		guiValXblock2 = (int)(globClickX * (200.0f/1920.0f));		//200
		//xBlock 3 
		guiValXblock3 = (int)(globClickX * (395.0f/1920.0f));		//395
		//xBlock 4
		guiValXblock4 = (int)(globClickX * (617.0f/1920.0f));		//617
		//xBlock 5 
		guiValXblock5 = (int)(globClickX * (810.0f/1920.0f));			//810
		//xBlock 6  
		guiValXblock6 = (int)(globClickX * (1000.0f/1920.0f));		//1000
		//xBlock 7 
		guiValXblock7 = (int)(globClickX * (1195.0f/1920.0f));		//1195	
		//xBlock 8 
		guiValXblock8 = (int)(globClickX * (1475.0f/1920.0f));		//1475
		//xBlock 9
		guiValXblock9 = (int)(globClickX * (1710.0f/1920.0f));	//1710
	}//
	
	private void loadIcons(){
		//iconsOld loaded from disk
		String imgName;
		String imgInvName;
		String sketchPath = p.sketchPath();
		//iconsOld used to display subdivision boolean data at top of screen
		dispSubBoolIcons = new HashMap<Integer,PImage>();
		dispSubBoolIconsOff = new HashMap<Integer,PImage>();
		for (int idx = 0; idx < numDispIcons; ++idx){
			imgName = sketchPath + fileDelim + iconPath + fileDelim + "infoIco" + ( (idx < 10) ? ("0") : ("")) + idx + ".png";
			imgInvName = sketchPath + fileDelim + iconPath + fileDelim + "infoIco" + ( (idx < 10) ? ("0") : ("")) + idx + "inv.png";
			dispSubBoolIcons.put(idx, p.loadImage(imgName));
			dispSubBoolIconsOff.put(idx, p.loadImage(imgInvName));
		}
		
		//iconsOld used to show whether demo or dev mode - display at bottom of screen, used to toggle which gui mode to display
		this.demoDevIcons = new PImage[4];	//0 is demo, 1 is dev
		for(int i = 0; i < 4; ++i){		
			imgName = sketchPath + fileDelim + iconPath + fileDelim + "icoDemoDev" + i + ".png";	
			this.demoDevIcons[i] = p.loadImage(imgName);
		}
				
		//icon button structures
		icons = new PImage[2][iconRows][iconCols];			//2 pages 9 rows of 6 icons
		iconsInv = new PImage[2][iconRows][iconCols];			//2 pages 9 rows of 6 icons
		PImage[][][] iconsRaw = new PImage[2][iconRowsOld][iconCols];          //6 rows of 6 iconsOld each per page 
		PImage[][][] iconsInvRaw = new PImage[2][iconRowsOld][iconCols];          //6 rows of 6 iconsOld each per page
		//initially set both pages to be the same data
		for(int page = 0; page < 2; page++){
			for (int row = 0; row < iconRowsOld; ++row){
			    for (int col = 0; col < iconCols; ++col){
			    	imgName = sketchPath + fileDelim + iconPath + fileDelim + "UIPage"+page + fileDelim + "ico" + row + col + "inv.png";				//reversed inv and reg
			        imgInvName = sketchPath + fileDelim + iconPath  + fileDelim + "UIPage"+page + fileDelim + "ico" + row + col + ".png";
			        iconsRaw[page][row][col] = p.loadImage(imgName);
			        iconsInvRaw[page][row][col] = p.loadImage(imgInvName);
			  }//for each col
			}//for each row
		}
		//build icons array - holding rows 0,1 of icons old, followed by 6,7,8, then followed by 2,3,4,5 for 9 rows
		for(int page = 0; page < 2; ++page){
			for (int row = 0; row < 3; ++row){
			    for (int col = 0; col < iconCols; ++col){
			    	icons[page][row][col] = iconsRaw[page][row][col];
			    	iconsInv[page][row][col] = iconsInvRaw[page][row][col];		    	
			    }//for each col
			}//for each row
	
			for (int row = 3; row < 6; ++row){
			    for (int col = 0; col < iconCols; ++col){
			    	icons[page][row][col] = iconsRaw[page][row+3][col];
			    	iconsInv[page][row][col] = iconsInvRaw[page][row+3][col];		    	
			    }//for each col
			}//for each row
			
			for (int row = 6; row < iconRows; ++row){
			    for (int col = 0; col < iconCols; ++col){
			    	icons[page][row][col] = iconsRaw[page][row-3][col];
			    	iconsInv[page][row][col] = iconsInvRaw[page][row-3][col];		    	
			    }//for each col
			}//for each row
		}//for each page
		
		
		//small demo mesh icons
		demoSMMeshBoolIcons = new PImage[this.demoSmMeshBoolIconRows][this.demoSmMeshBoolIconCols];
		demoSMMeshBoolIconsInv = new PImage[this.demoSmMeshBoolIconRows][this.demoSmMeshBoolIconCols];
		String[] dmoIcoNameSuffix1 = {"Ter", "H2O", "Sed",  "SedCap", "SedConc"};
		String[] dmoIcoNameSuffix2 = {"Mesh", "Edge", "Norm"};
		for(int row = 0; row < this.demoSmMeshBoolIconRows; ++row){
			for(int col = 0; col < this.demoSmMeshBoolIconCols; ++col){
				imgName = sketchPath + fileDelim + iconPath + fileDelim + "dmo" + dmoIcoNameSuffix1[col] + dmoIcoNameSuffix2[row] + ".png";
				imgInvName = sketchPath + fileDelim + iconPath + fileDelim + "dmo" + dmoIcoNameSuffix1[col] + dmoIcoNameSuffix2[row] +  "inv.png";
				demoSMMeshBoolIcons[row][col] = p.loadImage(imgName);
				demoSMMeshBoolIconsInv[row][col] = p.loadImage(imgInvName);			
			}			
		}//for each row
		//demo sim buttons
		this.demoSMSimButIcons = new PImage[this.demoSmSimButIconRows][this.demoSmSimButIconCols];
		this.demoSMSimButIconsInv = new PImage[this.demoSmSimButIconRows][this.demoSmSimButIconCols];
		String demoSMSimPrefix = "dmoSim";
		for(int row = 0; row < this.demoSmSimButIconRows; ++row){
			for(int col = 0; col < this.demoSmSimButIconCols; ++col){
				imgName = sketchPath + fileDelim + iconPath + fileDelim +demoSMSimPrefix + row + col +  ".png";
				imgInvName = sketchPath + fileDelim + iconPath + fileDelim + demoSMSimPrefix + row + col + "inv.png";
				demoSMSimButIcons[row][col] = p.loadImage(imgName);
				demoSMSimButIconsInv[row][col] = p.loadImage(imgInvName);			
			}			
		}//for each row		
		
//		//alt iconsOld replace primary iconsOld during certain program conditions
//		altIcons = new HashMap<Integer,PImage>();
//		for (String iconName : altIconNamesAra){
//			int iconKey = Integer.parseInt(iconName.substring(4));      //start of numeric value "---" in icon name : \\ico--- - to end of string
//			imgName = p.sketchPath + fileDelim + iconPath + fileDelim + iconName + "inv.png";
//			imgInvName = p.sketchPath + fileDelim + iconPath + fileDelim + iconName+ ".png";
//			altIcons.put((2 * iconKey), p.loadImage(imgName));      //doubling key value to make room for inverse icon
//			altIcons.put((2 * iconKey)+1,p.loadImage(imgInvName));    
//		}//for each icon name in alt icon array	
		
		
	}//loadIcons
	

	public void setSubdivider(mySqSqSubdivider _sub){this.sub = _sub;}
	
	/**
	 * called with each init vars
	 */
	public void initGuiVars(){
		saveClickCount = 0;
		//meshNamesIDX = -1;
		lightCounter = new int[numNotifyLights];
		for (int i =0; i<numNotifyLights; ++i){ lightCounter[i] = lightCountCycles; }				
	}
	/**
	 * initialize gui elements and variables one time on program start
	 */
	public void initGuiOnce(){
		initBoolClickArrays();
		initBoolLabels();
		initColors();		
		initFlagColors();
	}
	
	/**
	 * initialize boolean flag display colors
	 */
	private void initFlagColors(){
		flagColor = new myVector[myConsts.numFlags];
		for (int i = 0; i < myConsts.numFlags; ++i){
			flagColor[i] = new myVector(p.random(0,255),p.random(0,255),p.random(0,255));
		}//for each flags
		
	}
	/**
	 * initialize arrays of locations for boolean label click zones
	 * vertH2ONormals,
	 */
	public void initBoolClickArrays(){		
		//NOTE rearranging the order of these will screw up the display - they need to be displayed in this order or else the y coords need to be hardcoded
		this.modBoolLocsMap = new HashMap<Integer,HashMap<Integer,Float[]>>();
		
		HashMap<Integer,Float[]> tmpMap = new HashMap<Integer, Float[]>();	
		Float ULCorner = this.minModBoolLocYAra[modSubBoolsIDX];
		tmpMap.put(myConsts.subEnabled, new Float[]				{minModBoolLocX, ULCorner, 	this.clkWidth, ULCorner+=(int)fontSize});          //{1120, ULCorner, 	myGui.clkWidth, ULCorner+=modBoolHeight});            
		tmpMap.put(myConsts.randomHeight, new Float[]				{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize}); //{1148, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});   
		tmpMap.put(myConsts.heightProc, new Float[]				{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize}); //{1212, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});   
		tmpMap.put(myConsts.erosionProc, new Float[]				{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize});	//	{1102, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});		
		tmpMap.put(myConsts.useMidpointMethod, new Float[]			{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize}); //{1102, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});   
		tmpMap.put(myConsts.subdivRiverMesh, new Float[]			{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize}); //{1102, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});   
		this.modBoolLocsMap.put(modSubBoolsIDX, tmpMap);		                                                                      //                                                                          
		                                                                                                                              //                                                                          
		tmpMap = new HashMap<Integer, Float[]>();                                                                                   //                                                                          
		ULCorner = this.minModBoolLocYAra[modSimBoolsIDX];                                                                           //                                                                          
		tmpMap.put(myConsts.simulate, new Float[]         	  	{minModBoolLocX, ULCorner, this.clkWidth, ULCorner+=(int)fontSize});             //{1202, ULCorner, myGui.clkWidth, ULCorner+=modBoolHeight});               
		tmpMap.put(myConsts.TWcalc, new Float[]           	   	{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize}); //{1115, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});   
		tmpMap.put(myConsts.HEcalc, new Float[]           	   	{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize}); //{1128, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});   
		tmpMap.put(myConsts.Raincalc, new Float[]         	   	{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize}); //{1234, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});   
		tmpMap.put(myConsts.Pipes, new Float[]             	  	{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize}); //{1111, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});   
		tmpMap.put(myConsts.calcLateralErosion,new Float[]	  	{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize}); //{1102, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});   
		tmpMap.put(myConsts.modSoilSoftness,new Float[]			{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize});	//	{1111, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});		
		this.modBoolLocsMap.put(modSimBoolsIDX, tmpMap);		                                                                       //                                                                          
		                                                                                                                               //                                                                          
		tmpMap = new HashMap<Integer, Float[]>();                                                                                    //                                                                          
		ULCorner = this.minModBoolLocYAra[modTerBoolsIDX];                                                                            //                                                                          
		tmpMap.put(myConsts.terrainMeshDisplay,new Float[]       	{minModBoolLocX, ULCorner, this.clkWidth, ULCorner+=(int)fontSize});             //{1112, ULCorner, myGui.clkWidth, ULCorner+=modBoolHeight});               
		tmpMap.put(myConsts.strokeDisplayTerrain, new Float[]		{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize}); //{1112, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});   
		tmpMap.put(myConsts.showTerrainNormals, new Float[] 	 	{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize});	//	{1110, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});		
		this.modBoolLocsMap.put(modTerBoolsIDX, tmpMap);                                                                                //                                                                          
		                                                                                                                                //                                                                          
		tmpMap = new HashMap<Integer, Float[]>();                                                                                     //                                                                          
		//H2OMeshDisplay, strokeDisplayH2O, showH2ONormals,                                                                             //                                                                          
		//showBoundaryNodes, showH2OVelocity, dispSedSrcVecs                                                                            //                                                                          
		ULCorner = this.minModBoolLocYAra[modH2OBoolsIDX];                                                                             //                                                                          
		//tmpMap.put(myConsts.dataVisualization, new Integer[]       {minModBoolLocX, ULCorner, myGui.clkWidth, ULCorner+=18});                      //  {1105, ULCorner, myGui.clkWidth, ULCorner+=18});                        
		tmpMap.put(myConsts.H2OMeshDisplay, new Float[]        	{minModBoolLocX, ULCorner, this.clkWidth, ULCorner+=(int)fontSize});             //{1100, ULCorner, myGui.clkWidth, ULCorner+=modBoolHeight});               
		tmpMap.put(myConsts.strokeDisplayH2O, new Float[]  		{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize}); //{1073, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});   
		tmpMap.put(myConsts.showH2ONormals, new Float[]    		{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize}); //{1104, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});   
		tmpMap.put(myConsts.showBoundaryNodes, new Float[]   		{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize}); //{1163, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});   
		tmpMap.put(myConsts.showH2OVelocity, new Float[]   		{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize}); //{1125, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});   
		tmpMap.put(myConsts.dispSedSrcVecs, new Float[] 			{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize});	//	{1125, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});		
		this.modBoolLocsMap.put(modH2OBoolsIDX, tmpMap);                                                                                //                                                                          
		                                                                                                                                //                                                                          
		tmpMap = new HashMap<Integer, Float[]>();                                                                                     //                                                                          
		ULCorner = this.minModBoolLocYAra[modSedBoolsIDX];                                                                             //                                                                          
		tmpMap.put(myConsts.sedMeshDisplay,new Float[]       		{minModBoolLocX, ULCorner, this.clkWidth, ULCorner+=(int)fontSize});             //{1112, ULCorner, myGui.clkWidth, ULCorner+=modBoolHeight});               
		tmpMap.put(myConsts.strokeDisplaySED, new Float[]			{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize}); //{1112, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});   
		tmpMap.put(myConsts.showSEDNormals, new Float[] 	 		{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize});	//	{1110, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});		
		tmpMap.put(myConsts.useSedFlux, new Float[]				{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize});	//{1112, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});	  
		this.modBoolLocsMap.put(modSedBoolsIDX, tmpMap);                                                                                //                                                                          
                                                                                                                                        //                                                                          
		tmpMap = new HashMap<Integer, Float[]>();                                                                                     //                                                                          
		ULCorner = this.minModBoolLocYAra[modSedCapBoolsIDX];                                                                          //                                                                          
		tmpMap.put(myConsts.sedCapMeshDisplay,new Float[]       	{minModBoolLocX, ULCorner, this.clkWidth, ULCorner+=(int)fontSize});             //{1112, ULCorner, myGui.clkWidth, ULCorner+=modBoolHeight});               
		tmpMap.put(myConsts.strokeDisplaySedCap, new Float[]		{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize}); //{1112, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});   
		tmpMap.put(myConsts.showSedCapNormals, new Float[] 	 	{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize});	//	{1110, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});		
		this.modBoolLocsMap.put(modSedCapBoolsIDX, tmpMap);                                                                             //                                                                          
		                                                                                                                                //                                                                          
		tmpMap = new HashMap<Integer, Float[]>();                                                                                     //                                                                          
		ULCorner = this.minModBoolLocYAra[modSedConcBoolsIDX];                                                                         //                                                                          
		tmpMap.put(myConsts.sedConcMeshDisplay,new Float[]       	{minModBoolLocX, ULCorner, this.clkWidth, ULCorner+=(int)fontSize});             //{1112, ULCorner, myGui.clkWidth, ULCorner+=modBoolHeight});               
		tmpMap.put(myConsts.strokeDisplaySedConc, new Float[]		{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize}); //{1112, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});   
		tmpMap.put(myConsts.showSedConcNormals, new Float[] 	 	{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize});	//	{1110, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});		
		this.modBoolLocsMap.put(modSedConcBoolsIDX, tmpMap);                                                                            //                                                                          
		                                                                                                                                //                                                                          
		tmpMap = new HashMap<Integer, Float[]>();                                                                                     //                                                                          
		ULCorner = this.minModBoolLocYAra[modMiscBoolsIDX];	                                                                        //                                                                          
		tmpMap.put(myConsts.batchMode, new Float[]				{minModBoolLocX, ULCorner, this.clkWidth, ULCorner+=(int)fontSize});             //{1250, ULCorner, myGui.clkWidth, ULCorner+=modBoolHeight});               
		tmpMap.put(myConsts.debugMode, new Float[] 				{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize});	//	{1250, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});		
		tmpMap.put(myConsts.renderGui, new Float[]				{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize});	//{1170, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});	  
		this.modBoolLocsMap.put(modMiscBoolsIDX, tmpMap);                                                                               //                                                                          
		                                                                                                                                //                                                                          
		//display options - normal shading                                                                                              //                                                                          
		tmpMap = new HashMap<Integer, Float[]>();                                                                                     //                                                                          
		ULCorner = this.minModBoolLocYAra[modDispBoolsIDX];		                                                                    //                                                                          
		tmpMap.put(myConsts.vertTerrainNormals, new Float[] 		{minModBoolLocX, ULCorner, this.clkWidth, ULCorner+=(int)fontSize});		        //{1112, ULCorner, myGui.clkWidth, ULCorner+=modBoolHeight});		          
		tmpMap.put(myConsts.vertH2ONormals, new Float[]			{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize});	//{1170, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});	  
		tmpMap.put(myConsts.vertSEDNormals, new Float[] 			{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize});	//	{1112, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});		
		tmpMap.put(myConsts.vertSedCapNormals, new Float[]		{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize});	//{1112, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});					
		tmpMap.put(myConsts.vertSedConcNormals, new Float[]		{minModBoolLocX, ULCorner+=modBoolSep, this.clkWidth, ULCorner+=(int)fontSize});	//{1112, ULCorner+=modBoolSep, myGui.clkWidth, ULCorner+=modBoolHeight});					
		this.modBoolLocsMap.put(modDispBoolsIDX, tmpMap);                                                                                                                                                               
		
		//debug boolean map
		tmpMap = new HashMap<Integer, Float[]>();
		for(int colIDX = 0; colIDX < (1 + (myConsts.modBoolsAra[debugBoolsIDX].length/NumDebugBoolRows)); ++colIDX) {
			for(int rowIDX = 0; rowIDX < NumDebugBoolRows; ++rowIDX){
				int boolIdx = ((colIDX * NumDebugBoolRows) + rowIDX);
				if(boolIdx < myConsts.modBoolsAra[debugBoolsIDX].length){
					tmpMap.put( myConsts.modBoolsAra[debugBoolsIDX][boolIdx], 
						new Float[] { dbCol[colIDX], dbRow[rowIDX], dbCol[colIDX+1], dbRow[rowIDX+1]});	
				}
			}//for each row
		}//for each col
		
		this.modBoolLocsMap.put(debugBoolsIDX, tmpMap);
		
	}//initArrays
	
	/**
	*  initialize labels for gui display
	*/
	public void initBoolLabels(){
		flagNames = new String[myConsts.numFlags];
		flagNames[myConsts.sqSqSub] 			    = "Square Square Subdivision";
		flagNames[myConsts.catClkSub] 			    = "Catmull-Clarke Subdivision";
		flagNames[myConsts.loopSub] 			    = "Loop Subdivision";
		flagNames[myConsts.subEnabled] 			    = "Subdivision Terrain gen enabled";
		flagNames[myConsts.heightProc] 			    = "Displaying Height Map";
		flagNames[myConsts.heightMapMade] 		    = "Height map exists";
		flagNames[myConsts.erosionProc] 		    = "Process Erosion Calculations";
		flagNames[myConsts.multByNormal] 		    = "Mesh is an Enclosed solid";
		flagNames[myConsts.subdivRiverMesh]			= "Subdivide generated river (wrap) mesh";
		                                            
		flagNames[myConsts.vertTerrainNormals] 	    = "Use Vert Normals for shading Terrain";   
		flagNames[myConsts.vertH2ONormals] 		    = "Use Vert Normals for shading H2O";   
		flagNames[myConsts.vertSEDNormals] 		    = "Use Vert Normals for shading Sediment";   
		flagNames[myConsts.vertSedCapNormals] 	    = "Use Vert Normals for shading Sed Cap";
		flagNames[myConsts.vertSedConcNormals] 	    = "Use Vert Normals for shading Sed Conc";
                                                    
		flagNames[myConsts.H2OMeshDisplay] 		    = "Display H2O Mesh";
		flagNames[myConsts.terrainMeshDisplay] 	    = "Display Terrain Mesh";
		flagNames[myConsts.sedMeshDisplay]		    = "Display Sediment Mesh";
		flagNames[myConsts.sedCapMeshDisplay]	    = "Display Sediment Cap Mesh";
		flagNames[myConsts.sedConcMeshDisplay]	    = "Display Sediment Conc Mesh";
		                                            
		flagNames[myConsts.strokeDisplayH2O] 	    = "Display Mesh Edges for H2O";
		flagNames[myConsts.strokeDisplayTerrain]    = "Display Mesh Edges for Terrain";
		flagNames[myConsts.strokeDisplaySED] 	    = "Display Mesh Edges for Sediment";
		flagNames[myConsts.strokeDisplaySedCap]     = "Display Mesh Edges for Sed Cap";		
		flagNames[myConsts.strokeDisplaySedConc]    = "Display Mesh Edges for Sed Conc";		

		flagNames[myConsts.showTerrainNormals] 	    = "Display Terrain Normals";
		flagNames[myConsts.showH2ONormals] 		    = "Display H2O Normals";
		flagNames[myConsts.showSEDNormals]			= "Display Sediment Normals";
		flagNames[myConsts.showSedCapNormals]		= "Display Sed Cap Normals";	
		flagNames[myConsts.showSedConcNormals]		= "Display Sed Conc Normals";	
		
		flagNames[myConsts.simulate] 			    = "Simulation is Executing";
		flagNames[myConsts.TWcalc] 				    = "Thermal Weathering calcs enabled";
		flagNames[myConsts.HEcalc] 				    = "Hydraulic Erosion calcs enabled";
		flagNames[myConsts.Raincalc] 			    = "Rainfall is enabled";
		flagNames[myConsts.Pipes] 				    = "Use Pipes-based transport model";
		flagNames[myConsts.calcLateralErosion] 	    = "Calc Mass Wasting (Lateral Erosion)";
		flagNames[myConsts.modSoilSoftness] 	    = "Decrease Ks based on eroded depth";
		flagNames[myConsts.randomHeight] 		    = "Add random heights to subdivision";
		flagNames[myConsts.showH2OVelocity]		    = "Display Velocities directions";
		flagNames[myConsts.renderGui] 			    = "Render On-Screen gui display";
		flagNames[myConsts.useStamSolver] 		    = "Use 3D stam solver";
		flagNames[myConsts.addVelStamSolver] 	    = "Add Velocity to Mesh in Stam Solver";
		flagNames[myConsts.shiftKeyPressed] 	    = "Shift Key Currently Pressed";
		flagNames[myConsts.dispSedSrcVecs]		    = "Show sediment source location";
		flagNames[myConsts.showBoundaryNodes] 	    = "Show Boundary Nodes of Water";	
		flagNames[myConsts.wrapAroundMesh]		    = "Mesh has >= 1 torroidal boundary";
		flagNames[myConsts.dataVisualization] 		= "Data Visualisation is on";		
		
			//debug related labels
		flagNames[myConsts.debugMode]				= "Inspector Mode";
		flagNames[myConsts.debugModeCnsl] 			= "Face/Vert Debug Info";
		flagNames[myConsts.dispCutAwayX]			= "Slice Mesh @ const X";
		flagNames[myConsts.dispCutAwayZ]			= "Slice Mesh @ const Z";
		flagNames[myConsts.dispNon1KVals]			= "K < 1";
		flagNames[myConsts.dispSedTransLeftoverVals]= "Sed after transport";
		flagNames[myConsts.debugSubdivision]		= "Subdivision Info";
		flagNames[myConsts.scaleSedPipes]			= "Scale Sed to fix leak";
		flagNames[myConsts.limitWaterSedCap]		= "Limit Sed Cap H2O vol";
		flagNames[myConsts.ignoreYVelocity]			= "Ignore Vy for Sed Cap";
		flagNames[myConsts.dispOriginalMesh]		= "Show Original Mesh";
		flagNames[myConsts.dispAdjNodes]			= "Num Adj Nodes";
		flagNames[myConsts.dispWrapToNodes]			= "Node Wrap Pairs";
		flagNames[myConsts.dispFluxVals]			= "Outgoing Flux";
		flagNames[myConsts.dispHeightDiffs]			= "Height diffs";
		flagNames[myConsts.dispPipeLengths]			= "Pipe Lengths";
		flagNames[myConsts.useLowestKGlobally]		= "Apply lowest K globally";
		flagNames[myConsts.useSedFlux]				= "Use flux/pipes to move sed";
		flagNames[myConsts.useSedConc]				= "Use Sed conc for transport";
		flagNames[myConsts.pullSedFromDryNodes]		= "Extrap Sed @ Dry Node";
		flagNames[myConsts.erodeDryNodes]			= "Erode Dry Land via pull";
		flagNames[myConsts.advectErosionDep]		= "Advect deposition/erosion";
		flagNames[myConsts.batchMode]				= "Run in Batch Mode";
		flagNames[myConsts.showWrapNodes]			= "Show Wrap nodes";
		flagNames[myConsts.staggerRainFall]			= "Stagger rainfall";		
		flagNames[myConsts.useMidpointMethod]		= "Use Midpoint Method";
		
		constLabels = new String[numLblConsts];
		constLabels[lbl_MaxH2O]					= "Max H20";
		constLabels[lbl_MaxSed] 				= "Max sediment"; 
		constLabels[lbl_MaxAlt] 				= "Max altitude"; 
		constLabels[lbl_MinH2O] 				= "Min H20"; 
		constLabels[lbl_MinSed] 				= "Min sediment";
		constLabels[lbl_MinAlt] 				= "Min altitude"; 
        constLabels[lbl_deltaT] 				= "DeltaT"; 
        constLabels[lbl_aeolianScaleFact] 		= "Aeolian Scaling factor";
        constLabels[lbl_latErosionScaleFact] 	= "Terrain Corrasion Suceptibility";
        constLabels[lbl_cycPerRain] 			= "Cycles per rainfall";
        constLabels[lbl_cycPerDraw] 			= "Erosion cycles per Draw";
        constLabels[lbl_RaindropVol] 			= "Raindrop Volume";
        constLabels[lbl_cycPerHE] 				= "Cycles per Hydraulic Erosion";
        constLabels[lbl_cycPerTW] 				= "Cycles per Thermal Erosion";
        constLabels[lbl_globMatType ] 			= "Global material type";
        constLabels[lbl_Kc] 					= "Sediment Capacity Per H2O";
        constLabels[lbl_Kd]  					= "Rate of Sediment Deposit";
        constLabels[lbl_Kw] 					= "Mass Wasting rate";
        constLabels[lbl_Ks] 					= "Soil softness";
        constLabels[lbl_CurrVisType] 			= "Visualisation is for ";
        constLabels[lbl_NumNodes] 				= "Number of nodes in current mesh";
        constLabels[lbl_SimCycles] 				= "Sim Steps | Draw Cycles";
        constLabels[lbl_WindDir] 				= "Wind Direction"; 
        constLabels[lbl_deltaXZ] 				= "X-Z distance between nodes" ;
        constLabels[lbl_RainfallCountdown] 		= "Rainfall in ...";
        constLabels[lbl_vVectorScaleFact] 		= "Vector display length scaling";
        constLabels[lbl_MaxX] 					= "Max X";
        constLabels[lbl_MinX]  					= "Min X"; 
        constLabels[lbl_MaxY] 					= "Max Y";
        constLabels[lbl_MinY]  					= "Min Y"; 
        constLabels[lbl_MaxZ]  					= "Max Z";
        constLabels[lbl_MinZ]  					= "Min Z";
        constLabels[lbl_RiverWrapMeshPeriod] 	= "Period Mult of Infinite Rvr";
        constLabels[lbl_RiverWidth]				= "Width of River Mesh";
	    constLabels[lbl_TotH2O]					= "Tot H2O on mesh";		
	    constLabels[lbl_TotSed]					= "Tot Sed";		
	    constLabels[lbl_SedCap]					= "Sediment Capacity | % leak to capacity";
	    constLabels[lbl_DebugSliceX]			= "X coord of Slice/Boat";
	    constLabels[lbl_DebugSliceZ]			= "Z coord of Slice/Boat";	
	    constLabels[lbl_DebugSliceSizeX]		= "X width of Debug Slice";
	    constLabels[lbl_DebugSliceSizeZ]		= "Z width of Debug Slice";	
	    constLabels[lbl_calcMaxDeltaT]			= "Calc Max Delta T";
	    constLabels[lbl_vIncrByGravMult]		= "Mod Vel by Changing Grav";			//gravMult
	    constLabels[lbl_globalKVal]				= "Globally Applied K val";
	    constLabels[lbl_numVertsPerSide]		= "# Verts/Side Gen River Mesh";
	    constLabels[lbl_genRiverSlope]			= "Gen River # verts/unit altitude";	
		constLabels[lbl_dmoShadeByVerts]		= "Shade by Vert Normal :";
		constLabels[lbl_dmoDispMesh]			= "Display Mesh : ";
		constLabels[lbl_dmoDispEdges]			= "Display Poly Edges :";
   
	}//initLabels method 

	/**
	*  init global colors on setup
	*/
	public void initColors(){
		p.stroke(255);
		p.smooth();
		p.fill(200, 200, 200);    
		p.ambient (200, 200, 200);  
		p.specular(0, 0, 0);  
		p.shininess(1.0f);    
	}//initColors function

	/**
	*  subdivision has been user-selected, launches appropriate algorithm
	*  @param type - type of function called.  if 1 use jni version of subdivision routine
	*/
	public void selectSubdivide(int type){
		if (p.allFlagsSet(myConsts.subEnabled)){
			if (p.allFlagsSet(myConsts.sqSqSub)) {  if (!p.allFlagsSet(myConsts.heightProc))     { 
				if(type==0){ 		sub.subdivide_sqSqMesh(); } 
				//else { 			eng.subdivide_sqSqMeshJNI();}	//eng.subdivide_sqSqMeshJNIara();} //jni subdivision 
				p.subDivType = myConsts.sqSqSub;   }}                    //if not currently height map display, subdivide
			else if (p.allFlagsSet(myConsts.catClkSub)) { if (!p.allFlagsSet(myConsts.heightProc))  { sub.subdivide_catClkMesh(); p.subDivType = myConsts.catClkSub; }} 
			else if (p.allFlagsSet(myConsts.loopSub)) { sub.subdivide_mesh();     p.subDivType = myConsts.loopSub;}     // perform loop mesh subdivision
		}//if subEnabled
	}//selectSubdivide function
	
	/**
	*  returns a string representation of the current data visualisation type
	*/
	public String getTypeFromConst(int idx){
		switch(idx){
			case -1 : return " OFF";
			case myConsts.H2O 			: return " H2O levels";
			case myConsts.SED 			: return " Sediment levels";
			case myConsts.SEDCAP		: return " Sediment Capacity";
			case myConsts.SEDCONC		: return " Sediment Concentration";
			default : return "Off" ;
		}  //switch  
	}//getTypeFromConst function
	  
	/**
	*  builds string describing what data visualisation is displayed
	*/
	public void setDataVisStr(){
		if (p.globMinMaxIDX != -1){ dataVis = "Data visualisation is On for" + getTypeFromConst(p.globMinMaxIDX);} 
		else { dataVis = "Data visualisation is Off"; }
	}//printDataVis func

	/**
	*  determines appropriate string based on most recent subdivision execution
	*/
	public String getSubTypeFromConst(int type){
		switch (type) {
			case myConsts.sqSqSub : { return "Square Square subdivision";}
			case myConsts.loopSub : { return "Loop subdivision";}
			case myConsts.catClkSub : { return "Catmull-Clarke subdivision";}
			default : return "";
		}//switch  
	}//getSubTypeFromConst

	/**
	*  seperators for gui text lines
	*/
	public void sepTxtLinesPxls(float cnt, boolean down){ if (down) {p.translate(0, cnt, 0);} else {p.translate(0,-cnt, 0);} }				//amount in pixels passed
	public void sepTxtLines(float cnt, boolean down){ if (down) {p.translate(0, cnt * this.fontSize, 0);} else {p.translate(0,-cnt * this.fontSize, 0);} }				//amount in pixels passed
	public void sepTxtLines(boolean down){ if (down) {p.translate(0,this.fontSize, 0);} else {p.translate(0, -this.fontSize, 0);} }	//one line of text

	/**
	*  seperators for gui text columns  
	*/
	public void sepTxtCols(boolean right){	if (right){   p.translate (this.globClickX/15,0);} else { p.translate (-this.globClickX/15,0);}}
	public void sepTxtCols(float cnt, boolean right){	if (right){   p.translate (cnt * this.globClickX/15.0f,0);} else { p.translate (-cnt * this.globClickX/15.0f,0);}}
	public void sepTxtColsPxls(float cnt, boolean right){	if (right){   p.translate (cnt,0);} else { p.translate (-cnt ,0);}}
	
	/**
	 * calculate x | y value based on current screen x|y and passed value used in 1920 |1200 high screen
	 */
	private float calcX(float xVal){return this.globClickX * (xVal/1920.0f);}	
	private float calcY(float yVal){return this.globClickY * (yVal/1200.0f);}

	//draw the locks that will maintain a menu on the screen
	private void dispLocks(){
		if(p.allFlagsSet(myConsts.demoDevMode)){//if demo mode
			for(int idx : demoPopUpMenus){
				if((this.popUpMenuFlags[idx]) || (this.lockedPopUpMenuFlags[idx])){
					drawLockAtLocation(popUpMenuLockLocs[idx][0] - globClickX/2.02f,popUpMenuLockLocs[idx][1] - globClickY/2.02f, idx, lockColor[idx][0], lockColor[idx][1]);
				}
			}		
		} else {						//if dev mode
			for(int idx : devPopUpMenus){
				if(((idx !=dispDevDataEntryMenu ) || ((idx == dispDevDataEntryMenu) && (p.allFlagsSet(myConsts.erosionProc)))) 
						&&( (this.popUpMenuFlags[idx]) || (this.lockedPopUpMenuFlags[idx]))){
					drawLockAtLocation(popUpMenuLockLocs[idx][0] - globClickX/2.02f,popUpMenuLockLocs[idx][1] - globClickY/2.02f, idx, lockColor[idx][0], lockColor[idx][1]);
				}
			}//for each dev menu	
		}//if dev
	}//draw locks
	
	//draw a lock, either opened or closed, at a particular location
	private void drawLockAtLocation(float x, float y, int idx, int dColor, int lColor){
		if(this.lockedPopUpMenuFlags[idx]){ 	this.lockClosed(x, y);} 
		else {									this.lockOpen(x, y, dColor, lColor); }//open lock	
	}//drawLockAtLocation	
	
	//display bottom menu for demo mode - give a few selections for meshes to display, hard code some erosion vals settings
	private void dispDemoBottomMenu(){//TODO:		
		p.pushMatrix();
		p.pushStyle();
		    p.translate(-this.globClickX/2.01f,this.globClickY/2.01f-(2*demoIconSizeY),0);					  
			float xOff = 0;
			float yOff = 0;
			//first paint mesh icons
			for (int row = 0; row < 5; ++row){
			    for (int col = 0; col < iconCols; ++col){
			    	//dispDemoMenuIcon(row, col, (xOff+((row>2 ? row-3 : row)*1.2f)), yOff, demoIconSizeX, demoIconSizeY);
			    	dispIcon(row, col, (xOff+((row>2 ? row-3 : row)*1.2f)), yOff, demoIconSizeX, demoIconSizeY);
			        xOff += demoIconSizeX + demoIconPadX;      
			    }//for col 
			    if (row == 2){
			    	xOff = 0;
			    	yOff += demoIconSizeY + demoIconPadY;
			    }
			}//for row					
			//display rewind/stop/run buttons
			int rowPB = 8;
			for (int col = 0; col < 3; ++col){
				//dispDemoMenuIcon(rowPB, col, xOff + (rowPB-6) * 1.2f, yOff, demoIconSizeX, demoIconSizeY);
				dispIcon(rowPB, col, xOff + (rowPB-6) * 1.2f, yOff, demoIconSizeX, demoIconSizeY);
				xOff += demoIconSizeX + demoIconPadX;
			}//for col 
			rowPB = 7;
			for (int col = 3; col < iconCols; ++col){
				//dispDemoMenuIcon(rowPB, col, xOff + (rowPB-5) * 1.2f, yOff, demoIconSizeX, demoIconSizeY);
				dispIcon(rowPB, col, xOff + (rowPB-5) * 1.2f, yOff, demoIconSizeX, demoIconSizeY);
				xOff += demoIconSizeX + demoIconPadX;
			}//for col 
			xOff = 0;
			yOff = 0;
		p.popStyle();
		p.popMatrix();

	}//dispDemoBottomMenu
	
	public void dispDemoMeshButtonMenu(){
		//now display small icons
		float xOff, yOff;
		p.pushMatrix();
		p.pushStyle();
			p.textAlign(PConstants.LEFT);
			p.translate(-this.globClickX/10f,this.globClickY/2.01f-(3*demoSmIconSizeY),0);	
			this.sepTxtLines(true);
			dispText(constLabels[lbl_dmoDispMesh], gui_White);	
			this.sepTxtLines(1.4f, true);
			dispText(constLabels[lbl_dmoDispEdges], gui_White);	
			this.sepTxtLines(1.4f, true);
			dispText(constLabels[lbl_dmoShadeByVerts], gui_White);
		p.popStyle();
		p.popMatrix();	
			   
		p.pushMatrix();
		p.pushStyle();
		    p.translate(-this.globClickX/32f,this.globClickY/2.01f-(3*demoSmIconSizeY),0);					  
			xOff = 0;
			yOff = 0;
			for (int row = 0; row < this.demoSmMeshBoolIconRows; ++row){
			    for (int col = 0; col < this.demoSmMeshBoolIconCols; ++col){
			    	if(p.allFlagsSet(myConsts.dmoSmMeshBoolValsByLoc[row][col])){  	p.image(demoSMMeshBoolIcons[row][col], xOff, yOff, demoSmIconSizeX, demoSmIconSizeY);}   
					else {                                                    	p.image(demoSMMeshBoolIconsInv[row][col], xOff, yOff, demoSmIconSizeX, demoSmIconSizeY); }
			        xOff += this.demoSmIconSizeX + demoIconPadX;      		    	
			    }//col
			    xOff = 0;
		    	yOff += demoSmIconSizeY + demoIconPadY;
			}//row			
		p.popStyle();
		p.popMatrix();	
		//display legend
		p.pushMatrix();
		p.pushStyle();
			p.textSize( this.fontSize * .75f);
		    p.translate(this.globClickX/22f,this.globClickY/2.01f-(3*demoSmIconSizeY),0);					  
			p.textAlign(PConstants.LEFT);
			this.sepTxtLines(.8f,true);
			dispText("Terrain", gui_TerrainColor);	
			this.sepTxtLines(.8f,true);
			dispText("Water", gui_WaterColorDeep);	
			this.sepTxtLines(.8f,true);
			dispText("Sediment", gui_SedimentColor);
			this.sepTxtLines(.8f,true);
			dispText("Sed Capacity", gui_SedCapColor);
			this.sepTxtLines(.8f,true);
			dispText("Sed Concentration", gui_SedConcColor);
		p.popStyle();
		p.popMatrix();			
	}//dispDemoMeshButtonMenu

	public void dispDemoSimButtonMenu(){
		//now display small icons
		float xOff, yOff;
			   
		p.pushMatrix();
		p.pushStyle();
		    p.translate(this.demoButULSmSimBoolX - this.globClickX/2.0f,this.globClickY/2.01f-(2*demoIconSizeY),0);					  
			xOff = 0;
			yOff = 0;
			for (int row = 0; row < this.demoSmSimButIconRows; ++row){
			    for (int col = 0; col < this.demoSmSimButIconCols; ++col){
			    	if(((row == 0) && (col < 4) && (p.allFlagsSet(myConsts.dmoSmSimBoolVals[col]))) || //1st row, bools
			    	((((row == 0) && (col >= 4)) || (row == 1)) && (butPressed != -1) && (butPressed == myConsts.dmoSmSimMenuValsByLoc[row][col]))){	//2nd row, just pressed  	
			    		p.image(demoSMSimButIcons[row][col], xOff, yOff, demoSmIconSizeX, demoSmIconSizeY);}   
					else {                                                    	
						p.image(demoSMSimButIconsInv[row][col], xOff, yOff, demoSmIconSizeX, demoSmIconSizeY); }
			        xOff += this.demoSmIconSizeX + demoIconPadX;      		    	
			    }//col
			    xOff = 0;
		    	yOff += (demoSmIconSizeY + demoIconPadY);
			}//row			
		p.popStyle();
		p.popMatrix();	

	}//dispDemoMeshButtonMenu

	
	/**
	 * display non-modifiable icon display for subdivision type flags, shift flag, debug flag
	 */	
	private void dispNonModFlagIcons(){
		//display subdivision info : iconsOld and flags
		p.pushMatrix();
		p.pushStyle();
			p.translate(this.globClickX/2.25f,-this.globClickY/2.14f,0);
			//display whether or not we are in debug mode
			p.pushMatrix();
				p.translate(boolIconSizeX * 1.1f,0);
				if(p.allFlagsSet(myConsts.debugMode)){	setGuiColorOn((int)flagColor[myConsts.debugMode].x, (int)flagColor[myConsts.debugMode].y, (int)flagColor[myConsts.debugMode].z);	}//if flag enabled, set sphere color 
				else {									setGuiColorOff(); }
		
				if (p.allFlagsSet(myConsts.debugMode)){		p.image(this.dispSubBoolIcons.get(4), 0,0,boolIconSizeX, boolIconSizeY );}//if flag enabled, set sphere color 
				else {										p.image(this.dispSubBoolIconsOff.get(4),0,0, boolIconSizeX, boolIconSizeY );}		
				
			for (int idx = 0; idx < myConsts.subDivFlags.length; ++idx){
				p.translate(boolIconSizeX * 1.1f,0);

				if (p.allFlagsSet(myConsts.subDivFlags[idx])){	setGuiColorOn((int)flagColor[myConsts.subDivFlags[idx]].x, (int)flagColor[myConsts.subDivFlags[idx]].y, (int)flagColor[myConsts.subDivFlags[idx]].z);	}//if flag enabled, set sphere color 
				else {											setGuiColorOff(); }											 //else set it gray

				if (p.allFlagsSet(myConsts.subDivFlags[idx])){	p.image(this.dispSubBoolIcons.get(idx), 0,0,boolIconSizeX, boolIconSizeY );}//if flag enabled, set sphere color 
				else {											p.image(this.dispSubBoolIconsOff.get(idx),0,0, boolIconSizeX, boolIconSizeY );}		
		
			}//for each subdivision flag
			p.popMatrix();

			if(p.allFlagsSet(myConsts.shiftKeyPressed)) {		p.image(this.dispSubBoolIcons.get(3), 0,0,boolIconSizeX, boolIconSizeY );}
			else  {												p.image(this.dispSubBoolIconsOff.get(3), 0,0,boolIconSizeX, boolIconSizeY );}//idx 3 corresponds to shift key icon
		p.popStyle();
		p.popMatrix();		
	}//dispNonModFLagIcons
	
	/**
	*  display non-modifiable boolean flag values
	*/
	public void dispNonModBoolFlags(){
		int dispColor;
		p.pushMatrix();
			p.translate(this.globClickX/2.01f,-this.globClickY/2.125f,0);
			dispText("Informational condition flags :", gui_Yellow);
		p.popMatrix();		
		dispNonModFlagIcons();
		p.pushMatrix();
			p.translate(this.globClickX/2.01f,-this.globClickY/2.3f,0);	
			for(int idx : myConsts.nonModBools){//for(int idx = 0; idx <  numFlags; idx++){
				dispColor = ((p.allFlagsSet(idx)) ? (gui_White) : (gui_Gray));	
				if (p.allFlagsSet(idx)){	setGuiColorOn((int)flagColor[idx].x, (int)flagColor[idx].y, (int)flagColor[idx].z);	}//if flag enabled, set sphere color 
				else {						setGuiColorOff(); }																			 //else set it gray
				dispText(flagNames[idx], dispColor);
				this.sepTxtLines(true);			
		}//for each nonmodifiable boolean flag
		p.popMatrix();
	}//dispNonModBoolFlags function

	/**
	*  display modifiable boolean flags - replaced with display methods for each group of functions
	*/
	public void dispModBoolFlagsHeader(){
	  //display modifiable booleans 2nd 
		p.pushMatrix();
			p.translate(this.globClickX/2.01f,-this.globClickY/2.6f,0);	
			dispText("User modifiable Global condition flags :", gui_Cyan); 
		p.popMatrix();
	}//dispModBoolFlags function	
	
	/**
	 * paint faint context boxes around boolean flags to help distinguish them
	 */
	public void dispModBoolContextBoxes(int... araOfIDXs){
		float x1 = this.globClickX/2.7f,
			x2 = this.globClickX/2.0f, 
			y1 = -this.globClickY/2.65f, 
			y2,//coordinates of context box y direction		
			y1Misc = -this.globClickY/2.0f + this.miscBoolsLoc + this.modBoolSep;//location of misc box, last to be made  miscBoolsLoc 
			p.pushMatrix();
			p.pushStyle();
				p.noStroke();
				
				for(int idx = 0; idx < araOfIDXs.length; ++idx ){//for each coordinate set in group of context boxes to be displayed
					if(araOfIDXs[idx] == myGui.modMiscBoolsIDX){//always put at end of potential display list
						y1 = y1Misc;//this is for modifying the y location of the last box of modifiable booleans					
					} 
					y2 = y1 + (myConsts.modBoolsAra[araOfIDXs[idx]].length * (this.fontSize + this.modBoolSep));
					
					p.beginShape();
						setColorValFill(gui_TransBlack);	
						p.vertex(x1, y1);
						p.vertex(x1, y2);
						setColorValFill(araOfIDXs[idx] + 13);	
						p.vertex(x2, y2);
						p.vertex(x2, y1);
					p.endShape(PConstants.CLOSE);
					y1 = y2 + this.modBoolSep;	//add pixels for space between boxes
				}
			p.popStyle();		
		p.popMatrix();	
	}//dispModBoolContextBoxes

	/**
	*  display modifiable boolean flags of a certain functionality group 
	*/
	public void dispModBoolFlagsGroup(int... araOfIDXs){
		//display modifiable booleans 2nd  
		p.translate(this.globClickX/2.02f, -this.globClickY/2.64f,0);
		for(int araIDX : araOfIDXs){
			if(araIDX == myGui.modMiscBoolsIDX){	sepTxtLinesPxls(24 * (this.fontSize + this.modBoolSep) + calcY(16),true);}//down to the bottom of the first page of flags  
			//if(araIDX == myGui.modMiscBoolsIDX){	sepTxtLinesPxls(24 * (this.fontSize + this.modBoolSep) + calcY(10),true);}//down to the bottom of the first page of flags			
			for(int idx : myConsts.modBoolsAra[araIDX]){//for(int idx = 0; idx <  numFlags; idx++){
				if (p.allFlagsSet(idx)){
					sepTxtLines(true);
					setGuiColorOn((int)flagColor[idx].x, (int)flagColor[idx].y, (int)flagColor[idx].z);
					p.pushMatrix();
						if ((idx == myConsts.showTerrainNormals) || (idx == myConsts.showH2ONormals) || (idx == myConsts.showSEDNormals) || (idx == myConsts.showSedCapNormals)){
									//idx+1 is supposed to be the index whether or not we are using vertex or face normals for shading
							dispText(flagNames[idx] + " from   ", ((p.allFlagsSet(idx+1)) ? ("Vertexes") : ("Faces")) , gui_White, gui_Cyan, false);
						}
						else { dispText(flagNames[idx], gui_White); }
					p.popMatrix();
					sepTxtLinesPxls(this.modBoolSep,true);
				} else {
					sepTxtLines(true);
					setGuiColorOff();
					//if (p.allFlagsSet(myConsts.renderGui) || (idx == myConsts.renderGui)){ /*p.sphere(1);*/ }
					p.pushMatrix();
						p.fill(100,100,100,100);
						if (idx == myConsts.dataVisualization) {dispText(dataVis, gui_Gray);} 
						else {dispText(flagNames[idx], gui_Gray);}
					p.popMatrix(); 
					sepTxtLinesPxls(this.modBoolSep,true);
				}//if flags
			}//for each modifiable boolean flag	
			sepTxtLinesPxls(this.modBoolSep,true);
		}//for each sub ara being displayed
	}//dispModBoolFlags function
	
	/**
	 * display the camera location
	 */
	public void dispCameraLoc(){
    	p.pushMatrix();
    	p.pushStyle();
    		p.textAlign(PConstants.RIGHT);
    		//if(p.allFlagsFalse(myConsts.debugMode)) {
        		p.translate(this.globClickX/2.01f, -this.globClickY/2.05f);
        		String[] camVals = this.printCamVals();
    			dispText("Current Camera Eye Location : " +camVals[0] + " | "  + camVals[1] + " | "  + camVals[2] , gui_FaintYellow);
    		//}
    	p.popStyle();
		p.popMatrix(); 		
	}//dispCameraLoc
	
	//handles data breaking sim - uses passed index in min max aras				    	gui.print2Cnsl("calc 2 ero therm : " + p.drawCycles);
	public void handleErrorData(int idx, boolean min){
		if(p.allFlagsSet(myConsts.debugMode)){this.print2Cnsl("error with min/max idx : " + idx + " with value : " + (min ? p.globMinAra[idx] : p.globMaxAra[idx]));}
		else {this.print2Cnsl("Simulation restarting");}
		p.setFlags(myConsts.simulate, false);		
		//handle resetting/rerunning sim
		this.handleGuiChoiceButtons(myConsts.butReset, true);
		this.handleGuiChoiceButtons(this.chosenMeshChoiceButID, true);
	}
	
	/**
	*  display data across top of screen
	*/
	public void dispTopDisplay(){
	    if (p.allFlagsSet(myConsts.sqSqSub)){   //if mesh is built 
	    	p.translate(-this.globClickX/2.01f, -this.globClickY/2.05f);
        	dispText("Currently displaying : " + this.getNameAndTypeOfMesh()   
  	              + (((sub.currSqSqIterDepth-1) != 0) ? (" with " + (sub.currSqSqIterDepth-1) + " Square-square subdivision" + (((sub.currSqSqIterDepth -1) > 1) ? ("s"):("") ) + " applied rendering " 
  	              + /* sub.vertList.size()*/ sub.globalNumVerts + " nodes and " + /*sub.polyList.size()*/ sub.globalNumFaces + " poly faces") 
  	              : (" rendering " +  /* sub.vertList.size()*/ sub.globalNumVerts + " nodes and " + /*sub.polyList.size()*/ sub.globalNumFaces + " poly faces") ), gui_FaintYellow);
  	        sepTxtLines(1.1f,true);
	    	dispText("Current terrain environment extreme values :", gui_Yellow);
	    	sepTxtLines(1.1f,true);
	        p.pushMatrix();
	        	for (int i = 0; i < 2; ++i){
	        		p.pushStyle();
	        		try{	
		        			if ((float)Math.abs(p.globMaxAra[i]) > Integer.MAX_VALUE/1.1){	dispText( constLabels[i] + " : 0 ", gui_White); }//don't show boundary values of minint
		        			else if (df1.format(p.globMaxAra[i]) == "") {  				handleErrorData(i,false);     			}
		        			else if (Float.parseFloat(df1.format(p.globMaxAra[i])) == p.globMaxAra[i]) { 		dispText( constLabels[i] + " : " + df1.format(p.globMaxAra[i]), gui_White); 				}
		        			else {	     																		dispText( constLabels[i] + " : " , df1.format(p.globMaxAra[i]),  gui_White, gui_LightGreen, true);	}
	        		} catch(Exception e)  {  				handleErrorData(i,false);     			}
	        		p.popStyle();
	        		p.pushMatrix();
	        		p.pushStyle();
	        			sepTxtCols(1.5f,true);
	        			try {//to trap divide by 0 errors in sediment
		        			if ((float)Math.abs(p.globMinAra[i]) > Integer.MAX_VALUE/1.1){ dispText( constLabels[i+3] + " : 0 ", gui_White); } //dont show boundary values of maxint
		        			else if (df1.format(p.globMinAra[i]) == "") {  				handleErrorData(i, true);     			}
		        			else if (Float.parseFloat(df1.format(p.globMinAra[i])) == p.globMinAra[i] ) { 	dispText( constLabels[i+3] + " : " + df1.format(p.globMinAra[i]), gui_White); 				}
		        			else {																				dispText( constLabels[i+3] + " : " , df1.format(p.globMinAra[i]),  gui_White, gui_LightGreen, true);}
	        			} catch(Exception e)  {  				handleErrorData(i, true);     			}	
		        	p.popStyle();
	        		p.popMatrix();
	        		sepTxtLines(1.1f,true);        
	        	}//for i 1 to 3
	        p.popMatrix();
	        sepTxtLines(2.2f,true);
	        p.pushMatrix();//display current amounts of water and sediment in play
	        	dispText(constLabels[lbl_TotH2O] + " : ", df1.format(p.globTotValAra[myConsts.H2O]) +  " | " + df1.format(p.globTurnTotValAra[myConsts.H2O]) , gui_Yellow, gui_White, true, this.fontSize/3.0f);
 	        	sepTxtLines(true);
	        	dispText(constLabels[lbl_TotSed] + " (@idx0|Trans|diff) : ", df1.format(p.globTotValAra[myConsts.SED]) +  " | " + 
	        				df1.format(p.globTurnTotValAra[myConsts.SED]) + " | " + df1.format(p.globTotValAra[myConsts.SED] - p.globTurnTotValAra[myConsts.SED]) , gui_Yellow, gui_White, true, this.fontSize/3.0f);
	        	sepTxtLines(true);
	        	dispText(constLabels[lbl_SedCap] + " : ",  df1.format(p.globTotValAra[myConsts.SEDCAP]) + "|" + df1.format(100.0*(p.globTurnTotValAra[myConsts.SEDCAP])/( p.globTotValAra[myConsts.SEDCAP])) + " %", gui_Yellow, gui_White, true, this.fontSize/3.0f);
	        p.popMatrix();
	        sepTxtLines(2.2f,true);
	        p.pushMatrix();
	            sepTxtLines(1.1f,true);
		      	for(int i = 25; i < 30; i+=2){//25 : x coord, 27 : y coord, 29 : z coord - this is displaying min and max x,y  and z coord for debugging
		      	    if ((float)Math.abs(p.globMaxAra[i-20]) > Integer.MAX_VALUE/2){dispText( constLabels[i] + " : " + df1.format(0), gui_White); }
		      	    else { dispText( constLabels[i] + " : " + df1.format(p.globMaxAra[((i+1)/2) - 8]), gui_White); }//need x_coord = 5, y_coord = 6, z_coord = 7
		      	    p.pushMatrix();
		      	    	sepTxtCols(true);
		        	    if ((float)Math.abs(p.globMinAra[i-20]) > Integer.MAX_VALUE/2){ dispText( constLabels[i+1] + " : " + df1.format(0), gui_White); } 
		        	    else { dispText( constLabels[i+1] + " : " + df1.format(p.globMinAra[((i+1)/2) - 8]), gui_White);}
	        			sepTxtCols(.75f,true);
		        	    if(i == 25){       	    												dispText(constLabels[lbl_calcMaxDeltaT] + " : " + df5.format(p.calcIdealDeltaT()) , gui_White );  	    } 
		        	    else if ((p.allFlagsSet(myConsts.useLowestKGlobally)) && (i == 27)){  	dispText(constLabels[lbl_globalKVal] + " : " + df5.format(eng.globalKVal) , gui_White );      	    }
		            p.popMatrix();
		            sepTxtLines(1.1f,true);        
		      	}
	        p.popMatrix();
	        sepTxtLines(4.4f,true);
	        p.pushMatrix();
	        	dispText(constLabels[lbl_SimCycles] + " : " + p.simCycles + "|" + p.drawCycles, gui_White);
	   			sepTxtCols(1.65f,true);
       			dispText(constLabels[lbl_WindDir] + " : " + df8.format(eng.globWindAngle), gui_White);
    			sepTxtCols(.9f, true);
	        	dispText("(" + df2.format(convertRad2Degrees((float)eng.globWindAngle)) + ")", gui_White);
	        p.popMatrix();
	    }//if sqsqsub  
	}//dispTopDisplay function
	
	/**
	 * display debug data for specific range of locations on mesh
	 */
	public void dispSideDebugData(){		
    	if(p.allFlagsSet(myConsts.debugModeCnsl)){
            p.pushMatrix();
            p.pushStyle();
            	p.textAlign(PApplet.LEFT);
		    	p.translate(-this.globClickX/2.01f,-this.globClickY/3.0f,0);
            	String[] pipesDebugDispAra = debug_printPipesData();
            	for(int i = 0; i < pipesDebugDispAra.length; ++i ){
            		dispText("debug data : " + i, gui_LightCyan);
            		sepTxtLines(true);
            		dispText("Pipes Debug : " + pipesDebugDispAra[i], gui_LightCyan);
            		sepTxtLines(true);
           	}
            p.popStyle();
            p.popMatrix();
    	}//if debug mode
	}//dispSidePipesdebug


	/**
	*  display data across bottom of screen
	*/
	public void dispBottomDisplay(){
	    if (p.allFlagsSet(myConsts.erosionProc)){//goes row by row
	    	p.pushMatrix();
			    p.translate(-this.globClickX/2.02f,this.globClickY/2.24f);
		    	sepTxtLines(.5f,false);    
		    	p.pushMatrix();
	    			dispText("Modifiable Sim Values :", gui_Cyan);
		    		sepTxtCols(3f,true);    		
	    			dispText("Modifiable Mesh Generation Values :", gui_Cyan);
		    		sepTxtCols(3.25f,true);    		
	    			dispText("Modifiable Mesh Inspection Values :", gui_Cyan);
		    		sepTxtCols(3,true);    		
	    			dispText("Modifiable Erosion Values :", gui_Cyan);
		    	p.popMatrix();
		    	sepTxtLines(1.5f,true);    
		    	p.pushMatrix();//second row
		    		dispText(constLabels[lbl_deltaT] + " : "+ df1.format(p.getDeltaT()), gui_White); 
		    		sepTxtCols(1.5f,true);
		    		sepTxtLinesPxls(8,false);
		    		sepTxtColsPxls(16,false);
		    		dispNotifyLight(0);
		    		sepTxtLinesPxls(8,true);
		    		sepTxtColsPxls(16, true);
		    		dispText(constLabels[lbl_cycPerRain] + " : "+ df1.format(p.simRainCycles), gui_White);       
		    		sepTxtCols(1.5f,true);
		    		dispText(constLabels[lbl_numVertsPerSide] + " : "+ (sub.numVertsPerSide * 2), gui_White);       
		    		sepTxtCols(1.75f,true);
	    			dispText(constLabels[lbl_RiverWrapMeshPeriod] + " : "+  sub.riverWrapAroundPeriodMult, gui_White);  
		    		sepTxtCols(1.5f,true);
		    		dispText(constLabels[lbl_DebugSliceX] + " : "+ df1.format(p.dispCoordX), gui_White);       
		    		sepTxtCols(1.5f,true);
		    		dispText(constLabels[lbl_DebugSliceSizeX] + " : "+ df1.format(p.dispSizeX), gui_White);       
		    	p.popMatrix();
		    	sepTxtLines(1.1f,true);    
		    	p.pushMatrix();//second row
		    		dispText(constLabels[lbl_RaindropVol] + " : "+ df1.format(eng.globRaindropMult), gui_White);
		    		sepTxtCols(1.5f,true);
		    		sepTxtLinesPxls(8,false);
		    		sepTxtColsPxls(16,false);
		    		dispNotifyLight(1);
		    		sepTxtLinesPxls(8,true);
		    		sepTxtColsPxls(16, true);
		    		dispText(constLabels[lbl_cycPerHE] + " : "+ df1.format(p.simHECycles), gui_White);   
		    		sepTxtCols(1.5f,true);
		    		dispText(constLabels[lbl_genRiverSlope] + " : "+ df1.format(sub.genRiverSlope), gui_White);   
		    		sepTxtCols(1.75f,true);
		    		dispText(constLabels[lbl_vIncrByGravMult] + " : "+ df1.format(eng.gravMult), gui_White); 
		    		sepTxtCols(1.5f,true);
		    		dispText(constLabels[lbl_DebugSliceZ] + " : "+ df1.format(p.dispCoordZ), gui_White);       
		    		sepTxtCols(1.5f,true);
		    		dispText(constLabels[lbl_DebugSliceSizeX] + " : "+ df1.format(p.dispSizeZ), gui_White);       
		    	p.popMatrix();
		    	sepTxtLines(1.1f,true);    
		    	p.pushMatrix();//third row
		    		dispText(constLabels[lbl_cycPerDraw] + " : "+ df1.format(p.globSimRepeats), gui_White);       
		    		sepTxtCols(1.5f,true);
		    		sepTxtLinesPxls(8,false);
		    		sepTxtColsPxls(16,false);
		    		dispNotifyLight(2);
		    		sepTxtLinesPxls(8,true);
		    		sepTxtColsPxls(16, true);
		    		dispText(constLabels[lbl_cycPerTW] + " : "+ df1.format(p.simTWCycles), gui_White);        
		    		sepTxtCols(1.5f,true);
		    		dispText(constLabels[lbl_latErosionScaleFact] + " : "+ df1.format(eng.globLatErosionMult), gui_White);       
		    		sepTxtCols(1.75f,true);
		    		dispText(constLabels[lbl_RiverWidth] + " : "+  df1.format(sub.riverWidth), gui_White);  
		    		sepTxtCols(1.5f,true);
		    		dispText(constLabels[lbl_vVectorScaleFact] + " : " + df1.format(p.vVecScaleFact), gui_White);
		    	p.popMatrix();
		    	sepTxtLines(2.2f,false); //back up for mat type/erosion vals block
	    		sepTxtCols(9.25f,true);//move material erosion vals to far right
		    	p.pushMatrix();//back to first row , move over for erosion consts 
		    		p.pushMatrix();
		    			dispText(constLabels[lbl_globMatType] + " : "+ df1.format(eng.globMatType), gui_White);  
			    		sepTxtCols(2.2f,true);
			    		dispText(constLabels[lbl_aeolianScaleFact] + " : "+ df1.format(eng.globAeolErosionMult), gui_White);       
		    		p.popMatrix();
		    		sepTxtLines(1.1f,true);  
		    		p.pushMatrix();
		    			dispText(constLabels[lbl_Kc], gui_White);  
		    			sepTxtCols(1.2f,true);
		    			dispText(" : Kc["+ df1.format(eng.globMatType)+"] = " + df4.format(eng.matTypeAra[eng.Kc][eng.globMatType]), gui_White); 
		    			sepTxtCols(true);
			    		dispText(constLabels[lbl_Kw], gui_White); 
			    		sepTxtCols(0.8f,true);
			    		dispText(" : Kw["+ df1.format(eng.globMatType)+"] = " + df4.format(eng.matTypeAra[eng.Kw][eng.globMatType]), gui_White); 
		    		p.popMatrix();
		    		p.pushMatrix();
		    			sepTxtLines(1.1f,true);
		    			dispText(constLabels[lbl_Kd], gui_White); 
			    		sepTxtCols(1.2f,true);
		    			dispText(" : Kd["+ df1.format(eng.globMatType)+"] = " + df4.format(eng.matTypeAra[eng.Kd][eng.globMatType]), gui_White);          
		    			sepTxtCols(true);
		    			dispText(constLabels[lbl_Ks], gui_White); 
			    		sepTxtCols(0.8f,true);
		    			dispText(" : Ks["+ df1.format(eng.globMatType)+"] = " + df4.format(eng.matTypeAra[eng.Ks][eng.globMatType]), gui_White); 
		    		p.popMatrix();
		    	p.popMatrix();    
	    	p.popMatrix();    
	    }//if erosionproc  	    
	}//dispBottomDisplay function
	
	/**
	 * display where the most recent click occurred
	 */
	private void dispClickLoc(){
		p.pushMatrix();
		p.pushStyle();
			p.textAlign(PConstants.RIGHT);
		   	p.translate(this.globClickX/2.01f,this.globClickY/2.2f,0);
		   	sepTxtLines(false);
		   	dispText("Mouse Position : " + "[" + p.mouseX + ", " + p.mouseY + "] ",((p.allFlagsSet(myConsts.shiftKeyPressed)) ? gui_DarkGreen :  gui_DarkRed));
		   	sepTxtLines(true);
		   	dispText("Gui clicked : " + df1.format(xMod) + "|"+ df1.format(yMod) + "|" + guiDataChanged +":" + "[" + p.xClickLoc + ", " + p.yClickLoc + "] ",((p.allFlagsSet(myConsts.shiftKeyPressed)) ? gui_DarkGreen :  gui_DarkRed));
		p.popStyle();
		p.popMatrix();		
	}//dispClickLoc
	
	/**
	*  display data across bottom of screen if shift pressed
	*/
	public void dispBottomDisplayShift(){
	    if (p.allFlagsSet(myConsts.erosionProc)){//goes row by row
	    	p.pushMatrix();
			    p.translate(-this.globClickX/2.02f,this.globClickY/2.22f);
		    	sepTxtLines(.5f,false);    
		    	p.pushMatrix();
	    			dispText("User Modifiable Simulation Values :", gui_Cyan);
		    		sepTxtCols(3f,true);    		
	    			dispText("User Modifiable Mesh Values :", gui_Cyan);
		    	p.popMatrix();
		    	sepTxtLines(1.5f,true);
		    	p.pushMatrix();//first row
		    	p.popMatrix();
		    	sepTxtLines(true);    
		    	p.pushMatrix();//second row
		    	p.popMatrix();
		    	sepTxtLines(true);    
		    	p.pushMatrix();//third row
		    	p.popMatrix();
	    	p.popMatrix();    
	    }//if erosionproc  
	}//dispBottomDisplayShift function	
	
	/**
	 * display the console
	 */
	public void dispConsole(){
	    if (p.allFlagsSet(myConsts.sqSqSub)){   //if mesh is built
	    	p.translate(-this.globClickX/2.0f, -this.globClickY/300f);
	    	p.pushMatrix();	
				//move to top of display, near center
				for(int i =  numConsoleLines-1 ; i >= 0; --i){	dispText(">: " + dispConsoleAra[i], gui_LightRed);  sepTxtLines(true);	}
			p.popMatrix();
	    }//if mesh is built
	}//dispConsole
	
	/**
	 * display the debug bounding box
	 */
	private void dispDebugBox(){
    	p.pushMatrix();	
			p.noStroke();
	    	p.pushStyle();
	    		//first 3 fades
	    		p.beginShape();
					setColorValFill(gui_TransBlack);	
					p.vertex(debugBoxX1, debugBoxY1-this.fontSize);
					p.vertex(debugBoxX1, debugBoxY2);
					setColorValFill(gui_FaintGray);							
					p.vertex(debugBoxX1+this.fontSize, debugBoxY2-this.fontSize);
					p.vertex(debugBoxX1+this.fontSize, debugBoxY1-this.fontSize);
	    		p.endShape(PConstants.CLOSE);
	    	
	    		p.beginShape();
					setColorValFill(gui_TransBlack);	
					p.vertex(debugBoxX1, debugBoxY2);
					p.vertex(debugBoxX2, debugBoxY2);
					setColorValFill(gui_FaintGray);							
					p.vertex(debugBoxX2-this.fontSize, debugBoxY2-this.fontSize);
					p.vertex(debugBoxX1+this.fontSize, debugBoxY2-this.fontSize);
	    		p.endShape(PConstants.CLOSE);
	    		
	    		p.beginShape();
					setColorValFill(gui_TransBlack);	
					p.vertex(debugBoxX2, debugBoxY1);
					p.vertex(debugBoxX2, debugBoxY2);
					setColorValFill(gui_FaintGray);							
					p.vertex(debugBoxX2-this.fontSize, debugBoxY2-this.fontSize);
					p.vertex(debugBoxX2-this.fontSize, debugBoxY1-this.fontSize);
	    		p.endShape(PConstants.CLOSE);
	    		//main box
				setColorValStroke(gui_DarkGray);
	    		
				p.beginShape();
					setColorValFill(gui_FaintGray);	
					p.vertex(debugBoxX1+this.fontSize, debugBoxY1);
					p.vertex(debugBoxX1+this.fontSize, debugBoxY2-this.fontSize);
					p.vertex(debugBoxX2-this.fontSize, debugBoxY2-this.fontSize);
					p.vertex(debugBoxX2-this.fontSize, debugBoxY1);
				p.endShape(PConstants.CLOSE);
			p.popStyle();
		p.popMatrix();		

	}//dispDebugBox
	
	/**
	 * display debug menu boolean values at top of screen
	 */	
	public void dispDebugMenu(){
	    if (p.allFlagsSet(myConsts.debugMode)){   //if in debug mode
	    	p.pushStyle();
	   		p.translate(0, -this.globClickY/2.0f);
	    	//dispDebugBox();
	    	sepTxtLines(true);
    		p.pushMatrix();
	    		p.translate(debugBoxX1 + this.fontSize, this.fontSize);
		    		dispText("Inspector functions : ", gui_Yellow);
	    	p.popMatrix();
	    	p.popStyle();
	    	sepTxtLines(1.5f,true);
	    	final int numPerCol = 4;
			sepTxtCols(3.4f,false);

	    	for(int colIDX = 0; colIDX < myConsts.modBoolsAra[myGui.debugBoolsIDX].length; colIDX +=numPerCol){	
 	    		p.pushMatrix();//1	    		
				for(int rowIDX = 0; rowIDX < numPerCol; ++rowIDX){ 	//for each row
					int idx0 = ((colIDX + rowIDX) < myConsts.modBoolsAra[myGui.debugBoolsIDX].length) ?  (myConsts.modBoolsAra[myGui.debugBoolsIDX][colIDX+rowIDX]) : -1; 
					if (idx0 != -1){
						boolean debugFlag = p.allFlagsSet(idx0);
				    	p.pushStyle();//1
				    		if(debugFlag){					setGuiColorOn((int)flagColor[idx0].x, (int)flagColor[idx0].y, (int)flagColor[idx0].z);	}//if flag enabled, set text color 
				    		else {							setGuiColorOff(); }					
				    		p.pushMatrix();//2
				    			p.pushMatrix();//3
				    				if (debugFlag){				p.image(this.dispSubBoolIcons.get(4), 0,0,boolIconSizeX, boolIconSizeY );}//if flag enabled, set sphere color 
				    				else {						p.image(this.dispSubBoolIconsOff.get(4),0,0, boolIconSizeX, boolIconSizeY );}	
				    			p.popMatrix();//3
					    			sepTxtLinesPxls(12,true);
					    			sepTxtColsPxls(22,true);
				    			dispText(flagNames[idx0], ((debugFlag)?  gui_White : gui_Gray));
							p.popMatrix();//2
						p.popStyle();//1
					}//if not -1
			    	sepTxtLines(1.5f,true);					
				}//for each row
 				p.popMatrix();//1
 	   			sepTxtCols(1.3f,true);					
			}//for each col
	    }//if debug mode
	}//dispDebugMenu

	//this displays the button that will keep the gui on or have it mouse-over only
	private void dispGuiOnButton(){
		p.pushMatrix();
			p.image(this.demoDevIcons[((p.allFlagsSet(myConsts.demoDevMode, myConsts.renderDemoGui) || 
					((p.allFlagsFalse(myConsts.demoDevMode))) && (p.allFlagsSet(myConsts.renderGui))) ? this.DemoMode+2 : this.DevMode+2)],this.globClickX/2.07f - 80, globClickY/2.05f - 20, 50,30);    //40,20); 	
		p.popMatrix();		
	}//dispGuiOnButton
	
	//this displays the button to select between dev and demo mode guis
	private void dispDemoDevMode(){
		p.pushMatrix();
			p.image(this.demoDevIcons[(p.allFlagsSet(myConsts.demoDevMode) ? this.DemoMode : this.DevMode)],this.globClickX/2.05f - 35, globClickY/2.05f - 35, 45,45);		
		p.popMatrix();			
	}//dispDevDemoButton
		
	private void setupDemoGui(){
		boolean checkMenus = p.allFlagsSet(myConsts.mseHotspotMenusActive);
		if(checkMenus){		dispLocks();	}
  		//if((this.lockedPopUpMenuFlags[dispDevConsoleMenu])  || ((checkMenus)  && ((popUpMenuFlags[dispDevConsoleMenu]))))	{p.pushMatrix();   	dispConsole();		p.popMatrix(); 	}//defaults to off	      		
		if ((!checkMenus)  || (this.lockedPopUpMenuFlags[dispDemoButtonMenu]) ||  (popUpMenuFlags[dispDemoButtonMenu])){						dispDemoBottomMenu();}
		if ((!checkMenus)  || (this.lockedPopUpMenuFlags[dispDemoMeshBoolButtonMenu]) ||  (popUpMenuFlags[dispDemoMeshBoolButtonMenu])){		this.dispDemoMeshButtonMenu();}
		if ((!checkMenus)  || (this.lockedPopUpMenuFlags[dispDemoSimBoolButtonMenu]) ||  (popUpMenuFlags[dispDemoSimBoolButtonMenu])){		this.dispDemoSimButtonMenu();}
	
	}//setupDemoGui
	/**
	 * setup the primary gui for the development environment
	 * 
	 */
	private void setupDevGui(){
		boolean checkMenus = p.allFlagsSet(myConsts.mseHotspotMenusActive);
		if(checkMenus){		dispLocks();	}
		p.pushMatrix();
		p.pushStyle();
				p.textAlign(PConstants.RIGHT);
	      		if((!checkMenus) || (this.lockedPopUpMenuFlags[dispDevBoolsMenu])  || (popUpMenuFlags[dispDevBoolsMenu])){
					p.pushMatrix();//non-mod bools
				    	{dispNonModBoolFlags();} 
				    p.popMatrix();
				    
				    p.pushMatrix();//modifiable bools
			    		dispModBoolFlagsHeader();//displays heading
			    		p.pushMatrix();
			    			if(p.allFlagsSet(myConsts.shiftKeyPressed)){
					      		dispModBoolContextBoxes(modBoolsDispShiftNoMisc);
					      		dispModBoolFlagsGroup(modBoolsDispShiftNoMisc);
					      	} else {
				    			dispModBoolContextBoxes(modBoolsDispNoShiftNoMisc);
				    			dispModBoolFlagsGroup(modBoolsDispNoShiftNoMisc);
					      	}
			    		p.popMatrix();
			    		p.pushMatrix();
			    			dispModBoolContextBoxes( modMiscBoolsIDX);
			       			dispModBoolFlagsGroup( modMiscBoolsIDX);
			       		p.popMatrix();
				    p.popMatrix(); 
	      		}//if display menus
			    
			    p.pushMatrix();
			    	dispSideDebugData();//debug data along left side of screen
			    p.popMatrix();
			    
		    	p.textAlign(PConstants.LEFT);
				setGuiColorOn(255,255,255);				
	      		if((!checkMenus) || (this.lockedPopUpMenuFlags[dispDevDataDispMenu])  || (popUpMenuFlags[dispDevDataDispMenu]))		{p.pushMatrix();	dispTopDisplay();	p.popMatrix(); 	}	      		
	      	//use this if we want to default console to off	//if((this.lockedPopUpMenuFlags[dispDevConsoleMenu])  || ((checkMenus)  && ((popUpMenuFlags[dispDevConsoleMenu]))))	{p.pushMatrix();   	dispConsole();		p.popMatrix(); 	}//defaults to off	      		
	      		if((!checkMenus) || (this.lockedPopUpMenuFlags[dispDevConsoleMenu])  || (popUpMenuFlags[dispDevConsoleMenu]))	{p.pushMatrix();   	dispConsole();		p.popMatrix(); 	}//defaults to off	      		
	      		if((!checkMenus) || (this.lockedPopUpMenuFlags[dispDevDebugMenu])     || (popUpMenuFlags[dispDevDebugMenu]))		{p.pushMatrix();	dispDebugMenu();	p.popMatrix();	}	      		
	      		if((!checkMenus) || (this.lockedPopUpMenuFlags[dispDevButtonMenu])    || (popUpMenuFlags[dispDevButtonMenu]))		{p.pushMatrix();   	dispSelectButtons();p.popMatrix(); 	}	      		
	      		if((!checkMenus) || (this.lockedPopUpMenuFlags[dispDevDataEntryMenu]) || (popUpMenuFlags[dispDevDataEntryMenu]))	{p.pushMatrix();    dispBottomDisplay();p.popMatrix(); 	}
			p.popStyle();
			p.popMatrix();
	}//setupDevGui
		
	/*
	*  make an interface on the display to allow for entering values via mouse clicks and displaying values in window instead of console
	*/
	public void setUpGui(){
		p.rotate(PConstants.PI/4.0f, 0,1,0);
		p.rotate(PConstants.PI, 0,0,1);
			if ((!p.allFlagsSet(myConsts.mseHotspotMenusActive)) || (this.lockedPopUpMenuFlags[dispDevDemoButtonMenu]) || (popUpMenuFlags[dispDevDemoButtonMenu])){	
			dispDemoDevMode();	
			dispGuiOnButton();
			p.pushStyle();
			dispClickLoc();				//show location of click
			p.popStyle();
		}
    	dispCameraLoc();			//show camera
    	
        p.pushStyle();
	    	if(p.allFlagsSet(myConsts.demoDevMode)){ if((p.allFlagsSet(myConsts.renderDemoGui)) || (p.allFlagsSet(myConsts.mseHotspotMenusActive))) {    				setupDemoGui();	}	}//if render demo gui
	    	else {									 if((p.allFlagsSet(myConsts.renderGui)) || (p.allFlagsSet(myConsts.mseHotspotMenusActive))) {				        setupDevGui();	}  	}
    	p.popStyle();
	}//setUpGui function
	/**
	*  display the various selectable action iconsOld
	*/
	public void dispSelectButtons(){
		//value from top: butULBlockY
		  //p.translate(this.globClickX/3.08f, this.globClickY/8.0f);
		p.translate(this.globClickX/3.08f, butULBlockY - this.globClickY/2.0f);
		float xOff = 0;
		float yOff = 0;
		for (int row = 0; row < iconRows; ++row){
			for (int col = 0; col < iconCols; ++col){
				dispIcon(row, col, xOff, yOff, iconSizeX, iconSizeY);
				xOff += iconPadX + 2;      
			}//for col 
			xOff = 0;
			yOff += iconPadY + 2;
		}//for row
	}//dispSelectButtons

	/**
	*  display an icon at a given location on the screen - if shift is pressed, display alternate iconsOld
	*/
	public void dispIcon(int row, int col, float xOff, float yOff, float xSize, float ySize){
	p.pushMatrix();
		int page = (p.allFlagsSet(myConsts.shiftKeyPressed) ? 1 : 0);	
		if ((butPressed != -1) && (myConsts.butToIconIDX.get(butPressed) == ((row * iconCols) + col + 1))){    p.image(iconsInv[page][row][col], xOff, yOff, xSize, ySize);}   
		else {                                                                      p.image(icons[page][row][col], xOff, yOff, xSize, ySize); }
	
	p.popMatrix();
	}//dispIcon 

	/**
	*   some text on the screen - single color arg
	*/
	public void dispText(String str, int colorVal){
		p.pushMatrix();
		p.pushStyle();
			setColorValFill(colorVal);
			p.text(str, 0,0);
		p.popStyle();
	    p.popMatrix();
	}//dispText function 2 args
	
	/**
	*   some text on the screen - single color arg, non-default size
	*/
	public void dispText(String str, int colorVal, float size){
		p.pushMatrix();
		p.pushStyle();
			p.textSize(size);
			setColorValFill(colorVal);
			p.text(str, 0,0);
		p.popStyle();
	    p.popMatrix();
	}//dispText function 2 args

	/**
	 * display 2 colors for data on screen, with passable tab/indention amount between str and data, using default indent amount
	 */
	public void dispText(String str, String data,  int colorVal1, int colorVal2, boolean forward){this.dispText(str, data, colorVal1, colorVal2, forward, 5.2f);}//dispText function 4 args
	
	/**
	 * display 2 colors for data on screen, with passable tab/indention amount between str and data
	 * @param str the descriptor of the data (label)
	 * @param data the data being displayed
	 * @param colorVal1 color of str
	 * @param colorVal2 color of data
	 * @param forward whether we are moving to the right or left with the label/data
	 * @param indent distance between label and data
	 */
	public void dispText(String str, String data,  int colorVal1, int colorVal2, boolean forward, float indent){
		p.pushMatrix();
		p.pushStyle();
			setColorValFill(colorVal1);
			if(forward){
				p.text(str, 0, 0);
				setColorValFill(colorVal2);	  
				p.text(data,indent*str.length(),0);				
			} else {
				p.text(str, -indent*data.length(),0);
				setColorValFill(colorVal2);	  
				p.text(data, 0,0);				
			}
		p.popStyle();
	    p.popMatrix();
	}//dispText function 4 args
	
	public String getNameAndTypeOfMesh(){
		if(-1==p.currMeshID){return "No Mesh Currently Selected";}
		switch (p.meshType) {
			case myConsts.genMesh : { 		return "Generated " + myConsts.meshDispName.get(p.currMeshID);}
			case myConsts.damMesh  : { 		return "Generated " + myConsts.meshDispName.get(p.currMeshID);}
			case myConsts.fileMesh : { 		return "File " + myConsts.meshDispName.get(p.currMeshID);}
			case myConsts.stamMesh  : { 	return "Stam-based generated fluid volume";}
			case myConsts.riverMesh  : { 	return "Generated " + myConsts.meshDispName.get(p.currMeshID);}
			default : {						return "Mesh Type Not Defined";		}
		}//switch
	}//getNameAndtypeOfMesh
	
	public int[] getColor(int colorVal){
		switch (colorVal){
    	case gui_Gray   		         : { return new int[] {120,120,120,255}; }
    	case gui_White  		         : { return new int[] {255,255,255,255}; }
    	case gui_Yellow 		         : { return new int[] {255,255,0,255}; }
    	case gui_Cyan   		         : { return new int[] {0,255,255,255};} 
    	case gui_Magenta		         : { return new int[] {255,0,255,255};}  
    	case gui_Red    		         : { return new int[] {255,0,0,255};} 
    	case gui_Blue			         : { return new int[] {0,0,255,255};}
    	case gui_Green			         : { return new int[] {0,255,0,255};}  
    	case gui_DarkGray   	         : { return new int[] {80,80,80,255};}
    	case gui_DarkRed    	         : { return new int[] {120,0,0,255};}
    	case gui_DarkBlue  	 	         : { return new int[] {0,0,120,255};}
    	case gui_DarkGreen  	         : { return new int[] {0,120,0,255};}
    	case gui_DarkYellow 	         : { return new int[] {120,120,0,255};}
    	case gui_DarkMagenta	         : { return new int[] {120,0,120,255};}
    	case gui_DarkCyan   	         : { return new int[] {0,120,120,255};}	   
    	case gui_LightGray   	         : { return new int[] {200,200,200,255};}
    	case gui_LightRed    	         : { return new int[] {255,110,110,255};}
    	case gui_LightBlue   	         : { return new int[] {110,110,255,255};}
    	case gui_LightGreen  	         : { return new int[] {110,255,110,255};}
    	case gui_LightYellow 	         : { return new int[] {255,255,110,255};}
    	case gui_LightMagenta	         : { return new int[] {255,110,255,255};}
    	case gui_LightCyan   	         : { return new int[] {110,255,255,255};}
    	case gui_Black			         : { return new int[] {0,0,0,255};}
    	case gui_TransBlack  	         : { return new int[] {1,1,1,255};}  	
    	case gui_FaintGray 		         : { return new int[] {110,110,110,255};}
    	case gui_FaintRed 	 	         : { return new int[] {110,0,0,255};}
    	case gui_FaintBlue 	 	         : { return new int[] {0,0,110,255};}
    	case gui_FaintGreen 	         : { return new int[] {0,110,0,255};}
    	case gui_FaintYellow 	         : { return new int[] {110,110,0,255};}
    	case gui_FaintCyan  	         : { return new int[] {0,110,110,255};}
    	case gui_FaintMagenta  	         : { return new int[] {110,0,110,255};}
    	case gui_TerrainColor	         : { return new int[] {190,118,63,255};}//faint brown - 96 4E 13 : 
    	case gui_WaterColorDeep	         : { return new int[] {0,60,90,0};}//transparent blue
    	case gui_WaterColorShallow       : { return new int[] {50,90,120,0};}//transparent blue
    	case gui_SedimentColor           : { return new int[] {255,0,0,0};}//transparent red
    	default         		         : { return new int[] {255,255,255,255};}
    
		}//switch
	}//getColor
	
	public void setColorValFill(int colorVal){
		switch (colorVal){
	    	case gui_Gray   			: { p.fill(120,120,120,255); p.ambient(120,120,120); break;}
	    	case gui_White  			: { p.fill(255,255,255,255); p.ambient(255,255,255); break; }
	    	case gui_Yellow 			: { p.fill(255,255,0,255); p.ambient(255,255,0); break; }
	    	case gui_Cyan   			: { p.fill(0,255,255,255); p.ambient(0,255,255); break; }
	    	case gui_Magenta			: { p.fill(255,0,255,255); p.ambient(255,0,255); break; }
	    	case gui_Red    			: { p.fill(255,0,0,255); p.ambient(255,0,0); break; }
	    	case gui_Blue				: { p.fill(0,0,255,255); p.ambient(0,0,255); break; }
	    	case gui_Green				: { p.fill(0,255,0,255); p.ambient(0,255,0); break; } 
	    	case gui_DarkGray   		: { p.fill(80,80,80,255); p.ambient(80,80,80); break;}
	    	case gui_DarkRed    		: { p.fill(120,0,0,255); p.ambient(120,0,0); break;}
	    	case gui_DarkBlue   		: { p.fill(0,0,120,255); p.ambient(0,0,120); break;}
	    	case gui_DarkGreen  		: { p.fill(0,120,0,255); p.ambient(0,120,0); break;}
	    	case gui_DarkYellow 		: { p.fill(120,120,0,255); p.ambient(120,120,0); break;}
	    	case gui_DarkMagenta		: { p.fill(120,0,120,255); p.ambient(120,0,120); break;}
	    	case gui_DarkCyan   		: { p.fill(0,120,120,255); p.ambient(0,120,120); break;}		   
	    	case gui_LightGray   		: { p.fill(200,200,200,255); p.ambient(200,200,200); break;}
	    	case gui_LightRed    		: { p.fill(255,110,110,255); p.ambient(255,110,110); break;}
	    	case gui_LightBlue   		: { p.fill(110,110,255,255); p.ambient(110,110,255); break;}
	    	case gui_LightGreen  		: { p.fill(110,255,110,255); p.ambient(110,255,110); break;}
	    	case gui_LightYellow 		: { p.fill(255,255,110,255); p.ambient(255,255,110); break;}
	    	case gui_LightMagenta		: { p.fill(255,110,255,255); p.ambient(255,110,255); break;}
	    	case gui_LightCyan   		: { p.fill(110,255,255,255); p.ambient(110,255,255); break;}	    	
	    	case gui_Black			 	: { p.fill(0,0,0,255); p.ambient(0,0,0); break;}//
	    	case gui_TransBlack  	 	: { p.fill(0x00010100); p.ambient(0,0,0); break;}//	have to use hex so that alpha val is not lost    	
	    	case gui_FaintGray 		 	: { p.fill(77,77,77,77); p.ambient(77,77,77); break;}//
	    	case gui_FaintRed 	 	 	: { p.fill(110,0,0,100); p.ambient(110,0,0); break;}//
	    	case gui_FaintBlue 	 	 	: { p.fill(0,0,110,100); p.ambient(0,0,110); break;}//
	    	case gui_FaintGreen 	 	: { p.fill(0,110,0,100); p.ambient(0,110,0); break;}//
	    	case gui_FaintYellow 	 	: { p.fill(110,110,0,100); p.ambient(110,110,0); break;}//
	    	case gui_FaintCyan  	 	: { p.fill(0,110,110,100); p.ambient(0,110,110); break;}//
	    	case gui_FaintMagenta  	 	: { p.fill(110,0,110,100); p.ambient(110,0,110); break;}//
	    	case gui_TransGray 	 	 	: { p.fill(120,120,120,150); p.ambient(120,120,120); break;}//
	    	case gui_TransRed 	 	 	: { p.fill(255,0,0,150); p.ambient(255,0,0); break;}//
	    	case gui_TransBlue 	 	 	: { p.fill(0,0,255,150); p.ambient(0,0,255); break;}//
	    	case gui_TransGreen 	 	: { p.fill(0,255,0,150); p.ambient(0,255,0); break;}//
	    	case gui_TransYellow 	 	: { p.fill(255,255,0,150); p.ambient(255,255,0); break;}//
	    	case gui_TransCyan  	 	: { p.fill(0,255,255,150); p.ambient(0,255,255); break;}//
	    	case gui_TransMagenta  	 	: { p.fill(255,0,255,150); p.ambient(255,0,255); break;}//
	    	case gui_TerrainColor	 	: { p.fill(190,118,63,255); p.ambient(190,118,63); break;}//BE763f //faint brown - 964E13 : 150 78 13
	    	//case gui_TerrainColor	 	: { p.fill(220,220,220,255); p.ambient(220,220,220); break; }	//for screen shots for iconsOld
	    	case gui_TerrainVertColor	: { p.fill(200,200,200,255); p.ambient(200,200,200); break;}//very light gray - only used for no-height-map vert/poly display
	    	//case gui_WaterColor 	 	: { p.fill(0,0,255,180); p.ambient(0,0,255); break; }		//transparent blue
	    	case gui_WaterColorDeep 	: { p.fill(0,44,66,180); p.ambient(0,50,180); break; }		//transparent blue
	    	case gui_WaterColorShallow 	: { p.fill(0,44,66,180); p.ambient(0,50,180); break; }		//transparent blue
	    	case gui_SedimentColor   	: { p.fill(255,50,0,100); p.ambient(255,0,0); break; }		//transparent red
	    	case gui_KDispColor		 	: { p.fill(0,255,0,100); p.ambient(0,255,0); break; }		//transparent green
	    	case gui_Sed1DispColor		: { p.fill(255,0,255,100); p.ambient(255,0,255); break; }
	    	case gui_SedSrcNodeColor	: { p.fill(128,0,255,100); p.ambient(128,0,255); break; }
	    	case gui_SedCapColor		: { p.fill(100,0,255,100); p.ambient(55,55,255); break; }
	    	case gui_SedConcColor		: { p.fill(255,200,10,100); p.ambient(255,200,10); break; }
	    	case gui_BoulderColor	    : { p.fill(200,200,200,255); p.ambient(200,200,200); break;}	
	    	//case gui_BoulderColor	    : { p.fill(100,100,100,255); p.ambient(55,55,55); break;}	
	    	case gui_boatBody 	  		: { p.fill(130, 75, 40,255); 	p.ambient(130, 75, 40); break;}
	    	case gui_boatSail			: { p.fill(255,255,255,255); 	p.ambient(255,255,255); break;}
	    	case gui_boatEmblem 		: { p.fill(120,0,0,255); 		p.ambient(120,0,0); 	break;}	    	
	    	
	    	default         			: { p.fill(255,255,255,255); p.ambient(255,255,255); break; }
	    	    	
		}//switch	
	}//setcolorValFill
	public void setColorValStroke(int colorVal){
		switch (colorVal){
 	    	case gui_Gray   	 	    : { p.stroke(120,120,120,255); break;}
	    	case gui_White  	 	    : { p.stroke(255,255,255,255); break; }
	    	case gui_Yellow      	    : { p.stroke(255,255,0,255); break; }
	    	case gui_Cyan   	 	    : { p.stroke(0,255,255,255); break; }
	    	case gui_Magenta	 	    : { p.stroke(255,0,255,255);  break; }
	    	case gui_Red    	 	    : { p.stroke(255,120,120,255); break; }
	    	case gui_Blue		 	    : { p.stroke(120,120,255,255); break; }
	    	case gui_Green		 	    : { p.stroke(120,255,120,255); break; }
	    	case gui_DarkGray    	    : { p.stroke(80,80,80,255); break; }
	    	case gui_DarkRed     	    : { p.stroke(120,0,0,255); break; }
	    	case gui_DarkBlue    	    : { p.stroke(0,0,120,255); break; }
	    	case gui_DarkGreen   	    : { p.stroke(0,120,0,255); break; }
	    	case gui_DarkYellow  	    : { p.stroke(120,120,0,255); break; }
	    	case gui_DarkMagenta 	    : { p.stroke(120,0,120,255); break; }
	    	case gui_DarkCyan    	    : { p.stroke(0,120,120,255); break; }	   
	    	case gui_LightGray   	    : { p.stroke(200,200,200,255); break;}
	    	case gui_LightRed    	    : { p.stroke(255,110,110,255); break;}
	    	case gui_LightBlue   	    : { p.stroke(110,110,255,255); break;}
	    	case gui_LightGreen  	    : { p.stroke(110,255,110,255); break;}
	    	case gui_LightYellow 	    : { p.stroke(255,255,110,255); break;}
	    	case gui_LightMagenta	    : { p.stroke(255,110,255,255); break;}
	    	case gui_LightCyan   		: { p.stroke(110,255,255,255); break;}		   
	    	case gui_Black				: { p.stroke(0,0,0,255); break;}
	    	case gui_TransBlack  		: { p.stroke(1,1,1,1); break;}	    	
	    	case gui_FaintGray 			: { p.stroke(120,120,120,250); break;}
	    	case gui_FaintRed 	 		: { p.stroke(110,0,0,250); break;}
	    	case gui_FaintBlue 	 		: { p.stroke(0,0,110,250); break;}
	    	case gui_FaintGreen 		: { p.stroke(0,110,0,250); break;}
	    	case gui_FaintYellow 		: { p.stroke(110,110,0,250); break;}
	    	case gui_FaintCyan  		: { p.stroke(0,110,110,250); break;}
	    	case gui_FaintMagenta  		: { p.stroke(110,0,110,250); break;}
	    	case gui_TransGray 	 		: { p.stroke(120,120,120,120); break;}
	    	case gui_TransRed 	 		: { p.stroke(255,0,0,120); break;}
	    	case gui_TransBlue 	 		: { p.stroke(0,0,255,120); break;}
	    	case gui_TransGreen 		: { p.stroke(0,255,0,120); break;}
	    	case gui_TransYellow 		: { p.stroke(255,255,0,120); break;}
	    	case gui_TransCyan  		: { p.stroke(0,255,255,120); break;}
	    	case gui_TransMagenta  		: { p.stroke(255,0,255,120); break;}
	    	case gui_TerrainColor		: { p.stroke(150,78,13,255); break;}	//faint brown - 96 4E 13 : p.fill(120,120,120,255); p.ambient(120,120,120); break;
	    	//case gui_TerrainColor		: { p.stroke(220,220,220,220); break; }	//for screen shots for iconsOld
	    	case gui_TerrainVertColor	: { p.stroke(200,200,200,255); break;}	//light gray
	    	case gui_WaterColorShallow	: { p.stroke(0,0,255,255); break; }		
	    	case gui_WaterColorDeep 	: { p.stroke(0,0,255,255); break; }		
	    	case gui_SedimentColor  	: { p.stroke(255,0,50,255); break; }	//transparent red
	    	case gui_KDispColor			: { p.stroke(0,255,0,255); break; }		//transparent green
	    	case gui_Sed1DispColor  	: { p.stroke(255,0,255); break;	}
	    	case gui_SedSrcNodeColor	: { p.stroke(128,0,255); break;	}
	    	case gui_SedCapColor		: { p.stroke(100,255,255); break; }
	    	case gui_SedConcColor		: { p.stroke(255,200,10); break; }
	    	case gui_BoulderColor  	    : { p.stroke(200,200,200,255); break;}
	    	case gui_boatBody 	  		: { p.stroke(130, 75, 40); break;}
	    	case gui_boatSail			: { p.stroke(255,255,255); break;}
	    	case gui_boatEmblem 		: { p.stroke(120,0,0); 	break;}	    	
	    	default         			: { p.stroke(55,55,255); break; }
		}//switch	
	}//setcolorValFill	
	
	/**
	 * sets water color based on interpolating depth of the water
	 * @param d
	 */
	public void setWaterColorFill(double d){		
		myVector ncf = this.interpColor(this.vecWaterColorFill, d);
		myVector nca = this.interpColor(this.vecWaterColorAmbient, d);
				
		int re = 0, ge = 0, be = 44, af = interpAlpha(this.alphaWaterColorFill, d);
		
		p.fill((int)ncf.x,(int)ncf.y,(int)ncf.z, af); 
		p.ambient((int)nca.x,(int)nca.y,(int)nca.z);
		p.emissive(re,ge,be);
	}
	
	public int interpAlpha (float[] dAra, double d){
		int intIdx = (int)(d * (dAra.length-1));
		double interp = (d * (dAra.length-1)) % 1.0f;
		
		if(intIdx == dAra.length-1) {	return (int) dAra[intIdx];	}//interp value 1 - highest node on mesh 
		else {							return (int)(((1 - interp) * dAra[intIdx]) + (interp*dAra[intIdx+1]));	}//if interp != 1
	}	
	
	/**
	 * interpolate a color through an array of different colors by equally weighting each color in the array
	 * @param colorAra the array of vectors representing rgb colors
	 * @param d the interpolant - 0-1
	 * @return vector holding correct color
	 */
	public myVector interpColor(myVector[] colorAra, double d){
		if(d > 1){d=1;}
		else if(d<0){d=0;}
		int intIdx = (int)(d * (colorAra.length-1));
		double interp = (d * (colorAra.length-1)) % 1.0f;
		
		if(intIdx == colorAra.length-1) {	return colorAra[intIdx];	}//interp value 1 - highest node on mesh 
		else {
			return new myVector(((1 - interp) * colorAra[intIdx].x) + (interp*colorAra[intIdx+1].x),
			                    ((1 - interp) * colorAra[intIdx].y) + (interp*colorAra[intIdx+1].y),
			                    ((1 - interp) * colorAra[intIdx].z) + (interp*colorAra[intIdx+1].z));
		}//if interp != 1
	}//interpColor	
	
	/**
	 * sets terrain color based on interpolating height of terrain
	 * @param interp
	 */
	public void setTerrainColorFill(double passedInterp, int alpha){
		myVector nc = interpColor(vecTerrainColorFill, passedInterp); 
		p.fill((int)nc.x,(int)nc.y,(int)nc.z,alpha); 
		p.ambient((int)nc.x,(int)nc.y,(int)nc.z); 
	}
	
	public void setFemValFill(double passedInterp, int alpha){
		myVector nc = interpColor(vecFEMColorFill, passedInterp); 
		p.fill((int)nc.x,(int)nc.y,(int)nc.z,alpha); 
		p.ambient((int)nc.x,(int)nc.y,(int)nc.z); 
	}
	/**
	 * sets water color based on interp height
	 * @param interp
	 */
	public void setWaterColorStroke(float interp){	p.stroke(0,0,255,255);}//pure blue for stroke always
	
	/**
	*  displays a sphere to notify whether or not a particular erosion process is executing
	*/
	public void dispNotifyLight(int idx){
		if(p.allFlagsSet(myConsts.notifyLightFlagOffset + idx)){setGuiColorOn(lightCountColors[idx][0],lightCountColors[idx][1],lightCountColors[idx][2]);} 
		else {setGuiColorOff();}
		p.sphere(this.sphereRad);
	}//dispNotifyLight

	/**
	*  set the values for a particular notify light flag
	*/
	public void setNotifyLights(String type){
		if (type.equals("Rain")){
			p.setFlags(myConsts.rainFallExec, true);
			lightCounter[0] = PApplet.min(lightCountCycles,p.simRainCycles+1);
		} else if (type.equals("HydraulicErosion")){
			p.setFlags(myConsts.fluvialExec, true);
			lightCounter[1] = PApplet.min(lightCountCycles,p.simHECycles+1);
		} else if (type.equals("ThermalWeathering")) {
			p.setFlags(myConsts.aeolianExec, true);
			lightCounter[2] = PApplet.min(lightCountCycles,p.simTWCycles+1);
		}   
	}//setNotifyLights func

	/**
	*  decrement notify lights' counters, and turns them off if they have expired
	*/
	public void decrNotifyLights(){
		for (int incr = 0; incr < numNotifyLights; ++incr){
			if(!(myConsts.butIDerode[incr] == butPressed)){
				lightCounter[incr]--;
				if (lightCounter[incr] <= 0) { p.setFlags((incr + myConsts.notifyLightFlagOffset), false); }
			}
		}//for each light 
	}//clearNotifyLights

	/**
	*  force notification lights to be off
	*/
	public void offNotifyLights(){
		for (int incr = 0; incr < numNotifyLights; ++incr){
			lightCounter[incr] = 0;
			p.setFlags((incr + myConsts.notifyLightFlagOffset), false);
		}//for each light
	}//offNotifyLights func

	/**
	*  set a color for the gui display 
	*/
	public void setGuiColorOn(int r, int g, int b){
		p.stroke(r,g,b,50);
		p.fill(r,g,b,50);
		p.ambient(r,g,b);
	}//setGuiColorRed function

	/**
	*  set a color for the gui display 
	*/
	public void setGuiColorOff(){
		p.stroke(100,100,100,50);
		p.fill(100,100,100,50);
		p.ambient(100,100,100);
	}//setGuiColorRed function

	/**
	*  set color to gray
	*/
	public void setColorGray(){ 
		if (p.allFlagsSet(myConsts.heightProc)){ p.stroke(255, 255, 255); } 
		else { 
			//noStroke(); 
			p.fill(myConsts.globGrayVal, myConsts.globGrayVal, myConsts.globGrayVal);    
			p.ambient (myConsts.globGrayVal, myConsts.globGrayVal, myConsts.globGrayVal);  
		} 
	}//setColorGray function

	/**
	*  picks a color routine based on passed arg to use to visualize data
	*  @parachoice of which color routine to use
	*  @param the variable to be used to scale the color
	*/
	public void setColorPick(int visChoice, int scaledVal, int alphaVal){
		//alphaVal = 255;//processing doesn't do alpha well, so make opaque
		switch(visChoice){
			case -1 : {setColorGray(); break;}
			case myConsts.H2O : {setColorArgs((255-scaledVal), (255-scaledVal), 255, alphaVal); break;}                            //used to show water amounts at verts in blue
			case myConsts.SED : {setColorArgs(255, (255-scaledVal), (255-scaledVal),alphaVal); break;}
			case myConsts.SEDCAP : {setColorArgs((255-scaledVal), 255 ,(255-scaledVal),alphaVal); break;}
			default : { setColorGray(); }//default  
		}//switch
	}//setColorPick

	/**
	*  picks a color routine based on passed arg to use to visualize data
	*  @parachoice of which color routine to use
	*  @param the variable to be used to scale the color
	*/
	public void setColorNorms(myVector normals, int alphaVal){
		int r = (int) (float)Math.round(p.mapD(normals.x, -1,1, 0,280));
		int g = (int) (float)Math.round(p.mapD(normals.y, -1,1, 0,280));
		int b = (int) (float)Math.round(p.mapD(normals.z, -1,1, 0,280));
		setColorArgs(r,g,b, alphaVal);                   
	}//setColorPick

	/**
	*  rescale int val to fit in span of acceptable spans for black to global gray
	*/
	public int reMap(int val){ return ((int)p.mapD(val,0,255,0,myConsts.globGrayVal)); }
	
	/**
	 * converts radians to degrees for wind angle display
	 */
	public float convertRad2Degrees(float rads){return (rads/(2.0f*(float)Math.PI) * 360.0f);}

	/**
	*  set color to specific value corresponding to passed data
	*/
	public void setColorArgs(int newr, int newg, int newb, int alphaVal){
		if (p.allFlagsSet(myConsts.heightProc)){ p.stroke(newr, newg, newb); } 
		else { 
			//map to gray so that bright isn't too bright
			int r = reMap(newr), g = reMap(newg), b = reMap(newb);
			//noStroke();
			p.fill(r, g, b, alphaVal);
			p.ambient(r, g, b); 
		}//if not heightproc  
	}//setColorArgs

	/**
	*  range checking function - checks src to be between low value and high value inclusive
	*/
	public boolean inRange(float src, float valA, float valB){ return ((valB >= valA) ? ((src >= valA) && (src <= valB)) : ((src >= valB) && (src <= valA))); }

	/**
	*  range checking function - checks src to be between low value and high value inclusive
	*/
	public boolean inRange(double src, double valA, double valB){ return ((valB >= valA) ? ((src >= valA) && (src <= valB)) : ((src >= valB) && (src <= valA))); }

	/**
	 * check if clicked in bounding box of menu popup lock - only call if in mouse-over menu mode
	 */
	public boolean checkInLockClickBox(double x, double y){
		if(p.allFlagsSet(myConsts.demoDevMode)){//if demo mode
			for(int idx : demoPopUpMenus){
				float lockLocX = this.popUpMenuLockLocs[idx][0],
				lockLocY = popUpMenuLockLocs[idx][1];
				if ((inRange(x, lockLocX, lockLocX+this.lockClickXsize))  
						&& (inRange(y, lockLocY-this.lockClickYsize/2.0f, lockLocY+this.lockClickYsize/2.0f))){
					this.lockedPopUpMenuFlags[idx] = !this.lockedPopUpMenuFlags[idx];	
					return true;
				}//if in range
			}//for demo	
		} else {						//if dev mode
			for(int idx : devPopUpMenus){
				float lockLocX = this.popUpMenuLockLocs[idx][0],
				lockLocY = popUpMenuLockLocs[idx][1];
				if ((inRange(x, lockLocX, lockLocX+this.lockClickXsize))  
					&& (inRange(y, lockLocY-this.lockClickYsize/2.0f, lockLocY+this.lockClickYsize/2.0f))){
					this.lockedPopUpMenuFlags[idx] = !this.lockedPopUpMenuFlags[idx];	
					return true;
				}//if in range
			}//for dev
		}//else dev	
		return false;
	}//checkInLockClickBox
	
	/**
	 * check if mouse location is in hotspot for menu - only called if mouse-over menus are active and menu not currently displayed
	 */
	public void checkMouseLocation(){
		popUpMenuOn = false;
		if(p.allFlagsSet(myConsts.demoDevMode)){//if demo mode
			for(int idx : demoPopUpMenus){popUpMenuFlags[idx] = false;}
			for(int idx : demoPopUpMenus){
				popUpMenuOn = ((inRange(p.mouseX, popUpMenuZones[idx][0],popUpMenuZones[idx][1])) && (inRange(p.mouseY,  popUpMenuZones[idx][2],popUpMenuZones[idx][3])));
				if(popUpMenuOn){		popUpMenuFlags[idx] = true;	return;	}
			}		
		} else {						//if dev mode
			for(int idx : devPopUpMenus){popUpMenuFlags[idx] = false;}
			for(int idx : devPopUpMenus){
				popUpMenuOn = ((inRange(p.mouseX, popUpMenuZones[idx][0],popUpMenuZones[idx][1])) && (inRange(p.mouseY,  popUpMenuZones[idx][2],popUpMenuZones[idx][3])));
				if(popUpMenuOn){		popUpMenuFlags[idx] = true;	return;	}
			}		
		}		
	}//check mouse location, to see if pop up menus are activated
	
	/**
	*  check if mouseclick in user-modifiable portion of display screen - boolean flags
	*/
	public int checkInGuiBools(double x, double y){
		Float[] loc = new Float[4];
		int[] loopAra = (p.allFlagsSet(myConsts.shiftKeyPressed)) ? (modBoolsDispShift) : (modBoolsDispNoShift);
		for (int araIDX : loopAra){//for (int araIDX = 0; araIDX < myConsts.modBoolsAra.length; ++araIDX ){
			for (int idx : myConsts.modBoolsAra[araIDX]){
				loc = modBoolLocsMap.get(araIDX).get(idx);						//location of boolean click area given by idx in modSubBools array
				if (inRange(x, loc[0],loc[2])  && inRange(y,loc[1], loc[3])){	
					if(idx==myConsts.shiftKeyPressed){	this.print2Cnsl("shift idx changed : " + idx);}			
					return idx;		} 
			}//for each idx in subara
		}//for each subara of bools in array
		return -1;
	}//checkInGui function
	
	/**
	 * checks if mouseclick is in debug zone of screen.  only called if debug is enabled
	 * @param x x loc of mouse click
	 * @param y y loc of mouse click
	 * @return idx of flag being set/cleared
	 */
	public int checkDebugBools(double x, double y){
		Float[] loc = new Float[4];
		for (int idx : myConsts.modBoolsAra[myGui.debugBoolsIDX]){
			loc = modBoolLocsMap.get(myGui.debugBoolsIDX).get(idx);						//location of boolean click area given by idx in modSubBools array
			if (inRange(x, loc[0],loc[2])  && inRange(y,loc[1], loc[3])){	return idx;		} 
		}//for each idx in subara
		return -1;
	}
	
	/**
	 * check if selection in debug boolean selections
	 */
	public int checkInDebugGuiBools(double x, double y){
		Float[] loc = new Float[4];			//location of a particular flag's hotspot
		for (int idx : myConsts.modBoolsAra[5]){
			loc = modBoolLocsMap.get(5).get(idx);						//location of boolean click area given by idx in modSubBools array
			if (inRange(x, loc[0],loc[2])  && inRange(y,loc[1], loc[3])){	return idx;		} 
		}//for each idx in subara
		return -1;
	}//checkInDebugGuiBools

	/**
	*  check if mouseclick in user-modifiable portion of display screen - values - with or without shift 
	*  since every modifiable value has a label, use label id instead of hard constant as return value 

	*/
	public int checkInGuiVals(double x, double y){
		//if (!(inRange(x, 24, 1225)) || !(y > 904)){/* print2Cnsl("OORVal");*/return -1;}
		//if(!p.allFlagsSet(myConsts.shiftKeyPressed)){//shift is not pressed		nothing on shift page now
			if (inRange(y, guiValYblock1, guiValYblock2)) {
				if (inRange(x, guiValXblock1, guiValXblock2)) 		{  return lbl_deltaT;}              
				if (inRange(x, guiValXblock2, guiValXblock3)) 		{  return lbl_cycPerRain;}          
				if (inRange(x, guiValXblock3, guiValXblock4)) 		{  return lbl_numVertsPerSide;} 
				if (inRange(x, guiValXblock4, guiValXblock5)) 		{  return lbl_RiverWrapMeshPeriod;}         
				if (inRange(x, guiValXblock5, guiValXblock6)) 		{  return lbl_DebugSliceX;} 
				if (inRange(x, guiValXblock6, guiValXblock7)) 		{  return lbl_DebugSliceSizeX;}         
				if (inRange(x, guiValXblock7, guiValXblock8)) 		{  return lbl_globMatType;} 
				if (inRange(x, guiValXblock8, guiValXblock9)) 		{  return lbl_aeolianScaleFact;}         
			} else if (inRange(y, guiValYblock2 , guiValYblock3)) 	{                             
				if (inRange(x, guiValXblock1, guiValXblock2)) 		{  return lbl_RaindropVol;}              
				if (inRange(x, guiValXblock2, guiValXblock3)) 		{  return lbl_cycPerHE;}          
				if (inRange(x, guiValXblock3, guiValXblock4)) 		{  return lbl_genRiverSlope;} 
				if (inRange(x, guiValXblock4, guiValXblock5)) 		{  return lbl_vIncrByGravMult;}         
				if (inRange(x, guiValXblock5, guiValXblock6)) 		{  return lbl_DebugSliceZ;} 
				if (inRange(x, guiValXblock6, guiValXblock7)) 		{  return lbl_DebugSliceSizeZ;}         
				if (inRange(x, guiValXblock7, guiValXblock8)) 		{  return lbl_Kc;} 
				if (inRange(x, guiValXblock8, guiValXblock9)) 		{  return lbl_Kw;}         
			} else if (guiValYblock3 <= y){                                           
				if (inRange(x, guiValXblock1, guiValXblock2)) 		{  return lbl_cycPerDraw;}              
				if (inRange(x, guiValXblock2, guiValXblock3)) 		{  return lbl_cycPerTW;}          
				if (inRange(x, guiValXblock3, guiValXblock4)) 		{  return lbl_latErosionScaleFact;} 
				if (inRange(x, guiValXblock4, guiValXblock5)) 		{  return lbl_RiverWidth;}         
				if (inRange(x, guiValXblock5, guiValXblock6)) 		{  return lbl_vVectorScaleFact;} 
				if (inRange(x, guiValXblock6, guiValXblock7)) 		{  return -1;}         
				if (inRange(x, guiValXblock7, guiValXblock8)) 		{  return lbl_Kd;} 
				if (inRange(x, guiValXblock8, guiValXblock9)) 		{  return lbl_Ks;}         
			}		
		//} else {//shift key pressed                                         
		//}//shift key pressed
	  return -1;
	}//checkInGui function
	
	/**
	*	check whether any of the mesh/edge/normal shading buttons have been pressed
	*/	
	public int checkInDemoGuiMeshBools(double x, double y){
		float ySmDisp = demoSmIconSizeY + demoSmIconPadY/2.0f,
			xSmDisp = demoSmIconSizeX + demoSmIconPadX/2.0f;
		//want to return appropriate click value
		int row = (int)((y - this.demoButtonULClickY)/(ySmDisp)),			//y val
			col = (int)((x - this.demoButULSmMeshBoolX)/(xSmDisp));			//x val
		if((row <  myConsts.dmoSmMeshBoolValsByLoc.length) && (col < myConsts.dmoSmMeshBoolValsByLoc[0].length)){	return myConsts.dmoSmMeshBoolValsByLoc[row][col];}
		else 																							{ 	return -1;}
	}//checkInDemoGuiBools

	/**
	*  check if mouseclick in selectable buttons - if shift key pressed offset by 100
	*/
	public int checkInDemoGuiSimButtons(double x, double y){
		//TODO:
		
		float yDisp = demoSmIconSizeY + demoIconPadY/2.0f,
			xDisp = demoSmIconSizeX + demoIconPadX;
		
		if (inRange(y, demoButtonULClickY, demoButtonULClickY + yDisp)) {//first row
		    if (inRange(x, demoButULSmSimBoolX             , demoButULSmSimBoolX + xDisp))     		{ return myConsts.butMesh_showVelocity 		       ;}  
		    if (inRange(x, demoButULSmSimBoolX + xDisp    ,  demoButULSmSimBoolX  + (2*xDisp)  )) 	{ return myConsts.butMesh_showOldMesh        ;}  
		    if (inRange(x, demoButULSmSimBoolX + (2*xDisp) , demoButULSmSimBoolX + (3*xDisp)  )) 	{ return myConsts.butMesh_randomHeight   ;}  
		    if (inRange(x, demoButULSmSimBoolX + (3*xDisp) , demoButULSmSimBoolX + (4*xDisp)  )) 	{ return myConsts.butMesh_erosionProc       ;}  
		    if (inRange(x, demoButULSmSimBoolX + (4*xDisp) , demoButULSmSimBoolX + (5*xDisp)  )) 	{ return myConsts.butMesh_sqSqSubdivide   ;}   
		    if (inRange(x, demoButULSmSimBoolX + (5*xDisp) , demoButULSmSimBoolX + (6*xDisp)  ))  	{ return myConsts.butMesh_DemoGenHeightMap   ;}   
			
		} else if (inRange(y, demoButtonULClickY + yDisp , demoButtonULClickY + (2*yDisp))) {//2nd row
		    if (inRange(x, demoButULSmSimBoolX				,demoButULSmSimBoolX + xDisp))   		{ return myConsts.butIDRain     ;}  
		    if (inRange(x, demoButULSmSimBoolX + (1*xDisp) , demoButULSmSimBoolX  + (2*xDisp) ))	{ return myConsts.butIDHydraulic   ;}  
		    if (inRange(x, demoButULSmSimBoolX + (2*xDisp) , demoButULSmSimBoolX  + (3*xDisp) )) 	{ return myConsts.butIDThermal ;}  
		    if (inRange(x, demoButULSmSimBoolX + (3*xDisp) , demoButULSmSimBoolX  + (4*xDisp) ))  	{ return myConsts.butMesh_recordHMData;}  
		    if (inRange(x, demoButULSmSimBoolX + (4*xDisp) , demoButULSmSimBoolX  + (5*xDisp) )) 	{ return myConsts.butMesh_compareHMData  ;}   
		    if (inRange(x, demoButULSmSimBoolX + (5*xDisp) , demoButULSmSimBoolX  + (6*xDisp) ))  	{ return myConsts.butMesh_displayCmpHMData ;}   
		}
		return -1;
	}//checkdemoInGui function
	
	/**
	*  check if mouseclick in selectable buttons - if shift key pressed offset by 100
	*/
	public int checkInDemoGuiButtons(double x, double y, int pageIdx){
		float yDisp = demoIconSizeY + demoIconPadY/2.0f,
		xDisp = demoIconSizeX + demoIconPadX;
		//if (!(inRange(x, butULBlockX, butLRBlockX)) || !(inRange(y, butULBlockY, butLRBlockY))){/* println("OORBut");*/return -1;}
		int rowIdx = (int)( (y - demoButtonULClickY)/yDisp ), colIdx = (int)((x - demoButtonULClickX)/xDisp);
		print2Cnsl("Mouse demo mode click on idx : col = "+ colIdx + " row = " + rowIdx);
		if((rowIdx < 0) || (rowIdx > myConsts.butDemoUIMeshAra[pageIdx].length) || (colIdx<0) || (colIdx > myConsts.butDemoUIMeshAra[pageIdx][rowIdx].length)) {return -1;}
		return myConsts.butDemoUIMeshAra[pageIdx][rowIdx][colIdx];
	}//checkdemoInGui function
	/**
	*  check if mouseclick in selectable buttons - has 2 pages whether or not shift key is pressed
	*/
	public int checkInGuiButtons(double x, double y, int pageIdx){
		//if (!(inRange(x, butULBlockX, butLRBlockX)) || !(inRange(y, butULBlockY, butLRBlockY))){/* println("OORBut");*/return -1;}
		int rowIdx = (int)( (y - butULBlockY)/butDistY ), colIdx = (int)((x - butULBlockX)/butDistX);
		//print2Cnsl("Mouse click on idx : col = "+ colIdx + " row = " + rowIdx);
		if((rowIdx < 0) || (rowIdx > myConsts.butDevUIMeshAra[pageIdx].length) || (colIdx<0) || (colIdx > myConsts.butDevUIMeshAra[pageIdx][rowIdx].length)) {return -1;}
		return myConsts.butDevUIMeshAra[pageIdx][rowIdx][colIdx];
	}//checkInGui function
		
	/**
	*  process selection from gui if boolean flag given by choiceIDX - just toggle flag value
	*/
	public void handleGuiChoiceBool(int choiceIDX){  p.setFlags(choiceIDX, !p.allFlagsSet(choiceIDX)); }//handleGuiChoice func
	
	/**
	 * handles choosing a mesh from the button menu - generates/loads mesh, sets appropriate variables for mesh
	 * @param choice
	 */
	private void handleMeshChoice(int choice){
		if (!p.allFlagsSet(myConsts.erosionProc)){
    		this.chosenMeshChoiceButID = choice;
    		p.currMeshID = myConsts.butToMeshIDX.get(choice);
    		p.setGlobalsFromDescriber();
    		p.makeMeshes();
    		//set to execute subdivisions if they are defined for this mesh
    		sub.autoSubdivideCount = (null != myConsts.autoSubCnt.get(p.currMeshID) ? myConsts.autoSubCnt.get(p.currMeshID) : 0) ;		  			
    		p.setGlobalFlagsFromDescriber();    		
    		if(0 != sub.autoSubdivideCount){	//auto subdivision -- set flags
        		p.setFlags(myConsts.autoSubdivide, true);    	
        		p.setFlags(myConsts.randomHeight, true);   			
    		}
		}	
	}//handleMeshChoice

	/**
	*  process selection from gui if button is clicked		
	*/
	public void handleGuiChoiceButtons(int choice, boolean firstPressed){
		//first check if mesh buttons
		if (null!= myConsts.butToMeshIDX.get(choice)){
			handleMeshChoice(choice);			
		} else {
			//switch through functionality buttons
			switch (choice){	
			    case myConsts.butMesh_recordHMData     : {  if(eng.checkHeightMapAra()){eng.saveHeightMapAraData();}   	 break;}  
			    case myConsts.butMesh_compareHMData    : {	if(eng.checkHeightMapAra()){eng.compareHeightMapAraData(); }  	break; }//compare current mesh with saved mesh, if there is one		    
		    	case myConsts.butMesh_displayCmpHMData : { if((eng.checkHeightMapAra()) && (eng.compareHeightMapAraDataExists())){p.setFlags(myConsts.dispOldMeshVals, !p.allFlagsSet(myConsts.dispOldMeshVals)); } 	break; } //display saved mesh data, if there is any
		    	
			    case myConsts.butMesh_sqSqSubdivide : {if ((sub.vertListAra != null) && (sub.vertListAra.length != 0) && (sub.vertListAra[0].length != 0) &&  (!p.allFlagsSet(myConsts.simulate))) { p.setFlags(myConsts.sqSqSub,true); selectSubdivide(0); } break;}//select sqsquare subdivision
			    case myConsts.butMesh_catSubdivide  : {if ((sub.vertListAra != null) && (sub.vertListAra.length != 0) && (sub.vertListAra[0].length != 0) && (!p.allFlagsSet(myConsts.simulate))) { p.setFlags(myConsts.catClkSub,true); selectSubdivide(0); } break;} //select catmull-clarke subdivision
			    case myConsts.butMesh_genHeightMap  : {if (p.allFlagsSet(myConsts.sqSqSub)){ p.setFlags(myConsts.heightProc,!p.allFlagsSet(myConsts.heightProc));} break;}//generate height map	 	   
			    case myConsts.butMesh_smoothMesh    : {if ((sub.vertListAra != null) && (sub.vertListAra.length != 0) && (sub.vertListAra[0].length != 0) && (!p.allFlagsSet(myConsts.simulate))) {sub.smoothMesh();}	break;}	//executing mesh smoothing algorithm		    
			    case myConsts.butMesh_DebugMode     : { p.setFlags(myConsts.debugMode, !p.allFlagsSet(myConsts.debugMode));  break; }//magnifying glass - should toggle debug mode, enable debug functionality and display debug options		    
			    case myConsts.butMesh_SavePic       : { 
			    	now = Calendar.getInstance(); 
			    	p.setFlags(myConsts.savePic, true); 
			    	p.setFlags(myConsts.recordVideo, true); 
			    	break;} //save pic - rechoose now to guarantee unique file name
			    
			    case myConsts.butIDRain :{//rain on terrain
			    	if (p.allFlagsSet(myConsts.erosionProc) && (firstPressed || ((p.simRainCycles != 0) && (p.clickSimCycles % p.simRainCycles == 0)))){ 	p.pickCorrectErosion("Rain"); 	} 
			    	p.clickSimCycles++;
			    	break; } //execute erosion proc
		        
			    case myConsts.butIDHydraulic :{//execute first order hydraulic erosion or 2nd order, depending on whether using pipes or not 
			    	if (p.allFlagsSet(myConsts.erosionProc) && (firstPressed || ((p.simHECycles != 0) && (p.clickSimCycles % p.simHECycles == 0)))){ p.pickCorrectErosion("HydraulicErosion");	} 
			    	p.clickSimCycles++;
			    	break; } //execute erosion proc
		        
			    case myConsts.butIDThermal 		 : { eng.handleThermalChoice(true, firstPressed);   	break; }//execute thermal erosion first order  	      
			    case myConsts.butAddSailBoat 	 : { if ((eng.checkHeightMapAra()) && ((!p.allFlagsSet(myConsts.sailBoat)) || (p.sailBoatList.size() <= myConsts.maxNumSailBoats))){p.setFlags(myConsts.sailBoat, true); p.addSailBoat();}  	break;  	}  //add a sailboat
			    case myConsts.butRemoveSailBoat	 : { if (p.allFlagsSet(myConsts.sailBoat)){   		p.removeSailBoat();   	} 	break;   	}	//remove a sailboat
			    case myConsts.butReset 			 : {//visual reset - reset all values that affect visual orientation (camera, etc)
		    		p.initVars();
		    		sub.initValues();
		    		p.setFlags(myConsts.erosionProc, false);
		    		p.setFlags(myConsts.useMatlabFESolve, false);
		    		p.setFlags(myConsts.dispMatLabFEResults, false);
			    	break;	    	}		    
	
			    case myConsts.butRewindMeshSim  : {
					this.handleGuiChoiceButtons(myConsts.butReset, true);
					this.handleGuiChoiceButtons(this.chosenMeshChoiceButID, true);
			    	break; }//rewind - recall this proc passing this.chosenMeshChoiceButID as choice;
			    case myConsts.butStopMeshSim  : {
			    	if(p.allFlagsSet(myConsts.simulate)){    		
			    		p.setFlags(myConsts.simulate, false);    	
				    	p.setFlags(myConsts.recordVideo, false); 		    		
			    	}//stop recording if recording pictures
			    	break; }//stop - simulate off, reset mesh
			    case myConsts.butPlayPauseMeshSim  : {   		p.setFlags(myConsts.simulate, !p.allFlagsSet(myConsts.simulate));		    	
			    	break; }//play/pause - toggle simulate
			    case myConsts.butPlayplaylist  : {
			    	p.playCurrentPlaylist();
			    	break; } //play current playlist
			    case myConsts.butDispMatLabResults  : {//dispMatLabFEResults
			    	//enable display of matlab mesh
			    	if(null == this.mat) {    		initMatlabEng();   	}
			    	if(null != this.mat){
			    		p.setFlags(myConsts.dispMatLabFEResults, !p.allFlagsSet(myConsts.dispMatLabFEResults));
			    		print2Cnsl("Display matlab FE solver results on mesh : " +  p.allFlagsSet(myConsts.dispMatLabFEResults));
			    	}
			    	break; }
			    case myConsts.butCallMatLab  : {
			    	//set up for matlab code 
			    	if(null == this.mat) {    		initMatlabEng();   	}
			    	if(null != this.mat){
			    		p.setFlags(myConsts.useMatlabFESolve, !p.allFlagsSet(myConsts.useMatlabFESolve));
			    		print2Cnsl("Use matlab FE solver : " +  p.allFlagsSet(myConsts.useMatlabFESolve));
			    		if((eng.checkHeightMapAra()) && (p.allFlagsSet(myConsts.useMatlabFESolve))){//reinit myFEMSolver values if heightmap exists and if we are using the fem solver
			    			mat.initFEMVals(eng.heightMapAra.length, eng.heightMapAra[0].length, eng);
			    		} 
			    	} 
			    	break; }   
			    //buttons corresponding to bools for demo mode
			    case myConsts.butMesh_showVelocity : 		{ p.setFlags(myConsts.showH2OVelocity, !p.allFlagsSet(myConsts.showH2OVelocity)); break;} 		
			    case myConsts.butMesh_showOldMesh  : 		{ p.setFlags(myConsts.dispOriginalMesh, !p.allFlagsSet(myConsts.dispOriginalMesh)); break;} 	    	
			    case myConsts.butMesh_randomHeight :  		{ p.setFlags(myConsts.randomHeight, !p.allFlagsSet(myConsts.randomHeight)); break;}
			    case myConsts.butMesh_erosionProc :  		{ p.setFlags(myConsts.erosionProc, !p.allFlagsSet(myConsts.erosionProc)); 
			    	if(p.allFlagsSet(myConsts.erosionProc)) { 
				    	if (p.allFlagsSet(myConsts.sqSqSub)){ p.setFlags(myConsts.heightProc,!p.allFlagsSet(myConsts.heightProc));} 	
				    	if (p.allFlagsSet(myConsts.sqSqSub)){ p.setFlags(myConsts.heightProc,!p.allFlagsSet(myConsts.heightProc));}   		
			    	}
			    	break;}
			    case myConsts.butMesh_DemoGenHeightMap : 	{ //2 iterations, to make entire heightmap
			    	if (p.allFlagsSet(myConsts.sqSqSub)){ p.setFlags(myConsts.heightProc,!p.allFlagsSet(myConsts.heightProc));} 	
			    	if (p.allFlagsSet(myConsts.sqSqSub)){ p.setFlags(myConsts.heightProc,!p.allFlagsSet(myConsts.heightProc));} break;}	  	    
		    
		    	default : break;
			}//switch
		}//if not mesh button
	}//handleguichoicebuttons
	
	private void initMatlabEng(){		
		try {	//try to instantiate matlab engine
			print2Cnsl("Matlab engine not found, attempting to load.");			    		
			this.mat = new myFEMSolver(null, p);
    		eng.setFEMSolver(mat);
    		p.mat = mat;
    		if (null != mat) {mat.setGui(this);}		
    		print2Cnsl("Matlab engine loaded successfully.");	
		} catch (Exception e){//hit here if no appropriate matlab engine instantiated
			print2Cnsl("Unable to load MatLab engine. Exception : " + e.toString());	
			this.mat = null;
			eng.setFEMSolver(null);
		}//try-catch
	}//initMatlabEng
		
	/**
	*  resets gui display values to their base values
	*/
	public void resetGuiVals(){
		eng.globMatType = 0;
		eng.globRaindropMult = .001f;
		eng.globScaleMult = .24f;
		eng.globAeolErosionMult = .24f;		//strength of effect from lateral and aeolian erosion
		eng.gravMult = 1;		

		p.globSimRepeats = 4;
		p.simTWCycles = myConsts.initSimTWCycles;
		p.simHECycles = myConsts.initSimHECycles;
		p.simRainCycles = myConsts.initSimRainCycles;

		sub.numVertsPerSide = 20;
		sub.genRiverSlope = 80;
	}//resetGuiVals
	
	/**
	*  process selection from gui if value is selected via click  
	*/
	public void handleGuiChoiceVal(int choice, double _xMod, double _yMod){
		this.xMod = _xMod;//course grain
		this.yMod = _yMod;//fine grain
		double nodeDist = sub.getDeltaNodeDist();
		switch (choice) {
	    	case lbl_deltaT :{               //delta T : float 0 - myConsts.globDeltaTMax, .01, .5
			    p.setDeltaT(p.getDeltaT() + p.mapD(yMod, -1.0f, 1.0f, -.0001f, .0001f) + p.mapD(xMod, -1.0f, 1.0f, -.001f, .001f));
			    if (p.getDeltaT() > myConsts.globDeltaTMax) { p.setDeltaT(myConsts.globDeltaTMax); }
			    else if (p.getDeltaT() < 0) { p.setDeltaT(0); }
			    p.setFlags(myConsts.useStoredErosionMeshVals,false);
			    break;}      
		    case lbl_RaindropVol :{               //raindrop vol : float 0.0001 - 0.1f, .0001, .001
				eng.globRaindropMult += p.mapD(yMod, -1.0f, 1.0f, -.0001f, .0001f) + p.mapD(xMod, -1.0f, 1.0f, -.001f, .001f);
				if (eng.globRaindropMult > 1) { eng.globRaindropMult = 1; }
				else if (eng.globRaindropMult < 0.0001) { eng.globRaindropMult = 0.0001f; }
				break;}      
		    case lbl_globMatType :{               //material type : int 0 - 10, 1, 1
		    	eng.globMatType += (int)(p.mapD(yMod, -10.0f, 10.0f, -1.0f, 1.0f)) + (int)(p.mapD(xMod, -5.0f, 5.0f, -1.0f, 1.0f)) ;
		    	if (eng.globMatType > 10) { eng.globMatType = 10; }
		    	else if (eng.globMatType < 0) { eng.globMatType = 0; }
		    	break;}      
		    case lbl_cycPerRain :{               //cycles per rain : int 0 - 100, 1, 10
		    	p.simRainCycles += (int)(p.mapD(yMod, -10.0f, 10.0f, -1.0f, 1.0f)) + (int)(p.mapD(xMod, -2.0f, 2.0f, -1.0f, 1.0f));
		    	if (p.simRainCycles > 100) { p.simRainCycles = 100; }
		    	else if (p.simRainCycles < 0) { p.simRainCycles = 0; }
		    	if (p.simRainCycles !=0) {p.setFlags(myConsts.Raincalc, true);} else {p.setFlags(myConsts.Raincalc, false);}
		    	break;}      
		    case lbl_cycPerHE :{               //cycles per hydraulic : int 0 - 100, 1, 10
		    	p.simHECycles += (int)(p.mapD(yMod, -10.0f, 10.0f, -1.0f, 1.0f)) + (int)(p.mapD(xMod, -2.0f, 2.0f, -1.0f, 1.0f));
		    	if (p.simHECycles > 100) { p.simHECycles = 100; }
		    	else if (p.simHECycles < 0) { p.simHECycles = 0; }
		    	if (p.simHECycles !=0) {p.setFlags(myConsts.HEcalc, true);} else {p.setFlags(myConsts.HEcalc, false);}
		    	break;}      
		    case lbl_cycPerTW :{               //cycles per thermal : int 0 - 100, 1, 10
		    	p.simTWCycles += (int)(p.mapD(yMod, -10.0f, 10.0f, -1.0f, 1.0f)) + (int)(p.mapD(xMod, -2.0f, 2.0f, -1.0f, 1.0f));
		    	if (p.simTWCycles > 100) { p.simTWCycles = 100; }
		    	else if (p.simTWCycles < 0) { p.simTWCycles = 0; }
		    	if (p.simTWCycles !=0) {p.setFlags(myConsts.TWcalc, true);} else {p.setFlags(myConsts.TWcalc, false);}
		    	break;}      
		    case lbl_aeolianScaleFact :{               //aeolian scaling factor used to balance aeolian effect with fluvial effect   
		    	eng.globAeolErosionMult += p.mapD(yMod, -1.0f, 1.0f, -.001f, .001f) + p.mapD(xMod, -1.0f, 1.0f, -.01f, .01f);
		    	if (eng.globAeolErosionMult > 4) { eng.globAeolErosionMult = 4; }
		    	else if (eng.globAeolErosionMult < 0) { eng.globAeolErosionMult = 0; }
		    	break;}		   
		    case lbl_latErosionScaleFact :{               //aeolian scaling factor used to balance aeolian effect with fluvial effect   
		    	eng.globLatErosionMult += (p.mapD(yMod, -1.0f, 1.0f, -.001f, .001f) + p.mapD(xMod, -1.0f, 1.0f, -.01f, .01f) * eng.globLatErosionMult);
		    	if (eng.globLatErosionMult > 10) { eng.globLatErosionMult = 10; }
		    	else if (eng.globLatErosionMult < 0) { eng.globLatErosionMult = 0; }
		    	break;}      
		    case lbl_cycPerDraw :{               //erosion cycles per draw : int 1 - 10, 1, 1
		    	p.globSimRepeats += (int)(p.mapD(yMod, -10.0f, 10.0f, -1.0f, 1.0f)) + (int)(p.mapD(xMod, -5.0f, 5.0f, -1.0f, 1.0f));
		    	if (p.globSimRepeats > 50) { p.globSimRepeats = 50; }
		    	else if (p.globSimRepeats < 0) { p.globSimRepeats = 0; }
		    	break;}      
		    case lbl_Kc :{               //kc : float 0 - 10, .01, .1
		    	eng.matTypeAra[eng.Kc][eng.globMatType] += p.mapD(yMod, -1.0f, 1.0f, -.0001f, .0001f) + p.mapD(xMod, -1.0f, 1.0f, -.001f, .001f);
		    	if (eng.matTypeAra[eng.Kc][eng.globMatType] > 10) { eng.matTypeAra[eng.Kc][eng.globMatType] = 10; }
		    	else if (eng.matTypeAra[eng.Kc][eng.globMatType] < 0) { eng.matTypeAra[eng.Kc][eng.globMatType] = 0; }
		    	break;}      
		    case lbl_Kd :{               //kd : float 0 - 10, .01, .1
		    	eng.matTypeAra[eng.Kd][eng.globMatType] += p.mapD(yMod, -1.0f, 1.0f, -.0001f, .0001f) + p.mapD(xMod, -1.0f, 1.0f, -.001f, .001f);
		    	if (eng.matTypeAra[eng.Kd][eng.globMatType] > 10) {eng.matTypeAra[eng.Kd][eng.globMatType] = 10; }
		    	else if (eng.matTypeAra[eng.Kd][eng.globMatType] < 0) { eng.matTypeAra[eng.Kd][eng.globMatType] = 0; }
		    	break;}      
		    case lbl_Kw :{               //kw : float 0 - 10, .01, .1
		    	eng.matTypeAra[eng.Kw][eng.globMatType] += p.mapD(yMod, -1.0f, 1.0f, -.0001f, .0001f) + p.mapD(xMod, -1.0f, 1.0f, -.001f, .001f);
		    	if (eng.matTypeAra[eng.Kw][eng.globMatType] > 10) { eng.matTypeAra[eng.Kw][eng.globMatType] = 10; }
		    	else if (eng.matTypeAra[eng.Kw][eng.globMatType] < 0) {eng.matTypeAra[eng.Kw][eng.globMatType] = 0; }
		    	break;}      
		    case lbl_Ks :{               //ks : float 0 - 10, .01, .1
		    	eng.matTypeAra[eng.Ks][eng.globMatType] +=  p.mapD(yMod, -1.0f, 1.0f, -.0001f, .0001f) + p.mapD(xMod, -1.0f, 1.0f, -.001f, .001f);
		    	if (eng.matTypeAra[eng.Ks][eng.globMatType] > 10) { eng.matTypeAra[eng.Ks][eng.globMatType] = 10; }
		    	else if (eng.matTypeAra[eng.Ks][eng.globMatType] < 0) { eng.matTypeAra[eng.Ks][eng.globMatType] = 0; }
		    	break;}   
		    case lbl_vVectorScaleFact : {				//vVectorScaleFactor : float 1 - 100 	
		    	p.vVecScaleFact +=(p.mapD(yMod, -1.0f, 1.0f, -0.1f, .1f)) + (p.mapD(xMod, -1.0f, 1.0f, -0.5f, 0.5f));
		    	if (p.vVecScaleFact > 100) { p.vVecScaleFact = 100; }
		    	else if (p.vVecScaleFact < 1) { p.vVecScaleFact = 1; }
		    	break;}   
		    case lbl_RiverWrapMeshPeriod: {//period multiplier for wrap-around river meshes, int value 1-4
		    	sub.riverWrapAroundPeriodMult += (int) (p.mapD(yMod, -25.0f, 25.0f, -1.0f, 1.0f)) + (int)(p.mapD(xMod, -10.0f, 10.0f, -1.0f, 1.0f));
		    	if(sub.riverWrapAroundPeriodMult > 4){sub.riverWrapAroundPeriodMult = 4;}
		    	else if(sub.riverWrapAroundPeriodMult < 1){sub.riverWrapAroundPeriodMult = 1;}
			    p.setFlags(myConsts.useStoredErosionMeshVals,false);
		    	break;}
		    case lbl_RiverWidth: {//period multiplier for wrap-around river meshes, int value 1-4
		    	sub.riverWidth +=  (p.mapD(yMod, -5.0f, 5.0f, -.001f, .001f)) + (p.mapD(xMod, -5.0f, 5.0f, -.01f, .01f));
		    	if(sub.riverWidth > .75f){sub.riverWidth = .75f;}
		    	else if(sub.riverWidth < .01){sub.riverWidth = .01f;}
			    p.setFlags(myConsts.useStoredErosionMeshVals,false);
		    	break;}
		    case lbl_DebugSliceX : {//x coord of debug slice - min - max global x val
		    	p.dispCoordX += (p.mapD(yMod, -5, 5, -nodeDist*.5f, nodeDist*.5f)) + (p.mapD(xMod, -1.0f, 1.0f, -nodeDist*.5f, nodeDist*.5f));
		    	if(p.dispCoordX > p.globMaxAra[myConsts.COORD_X] - p.dispSizeX){p.dispCoordX = p.globMaxAra[myConsts.COORD_X] - p.dispSizeX;}
		    	else if(p.dispCoordX < p.globMinAra[myConsts.COORD_X]){p.dispCoordX = p.globMinAra[myConsts.COORD_X];}
		    	break;}
		    case lbl_DebugSliceSizeX : {//x size of debug slice - 0 - 2*maxGlobal x val
		    	p.dispSizeX += (p.mapD(yMod, -5, 5, -nodeDist*.5f, nodeDist*.5f)) + (p.mapD(xMod, -1.0f, 1.0f, -nodeDist*.5f, nodeDist*.5f));
		    	if(p.dispSizeX < 0 ){ p.dispSizeX = 0;}
		    	else if(p.dispSizeX > 2 * (p.globMaxAra[myConsts.COORD_X] + 1)){p.dispSizeX = 2 * (p.globMaxAra[myConsts.COORD_X] + 1);}
		    	break;}
		    case lbl_DebugSliceZ : {//z coord of debug slice - min - max global z val
		    	p.dispCoordZ += (p.mapD(yMod, -5, 5, -nodeDist*.5f, nodeDist*.5f)) + (p.mapD(xMod, -1.0f, 1.0f, -nodeDist*.5f, nodeDist*.5f));
		    	if(p.dispCoordZ > p.globMaxAra[myConsts.COORD_Z]-p.dispSizeZ){p.dispCoordZ = p.globMaxAra[myConsts.COORD_Z] - p.dispSizeZ;}
		    	else if(p.dispCoordZ < p.globMinAra[myConsts.COORD_Z]){p.dispCoordZ = p.globMinAra[myConsts.COORD_Z];}
		    	break;}
		    case lbl_DebugSliceSizeZ : {//z size of debug slice - 0 - 2*maxGlobal z val
		    	p.dispSizeZ += (p.mapD(yMod, -5, 5, -nodeDist*.5f, nodeDist*.5f)) + (p.mapD(xMod, -1.0f, 1.0f, -nodeDist*.5f, nodeDist*.5f));
		    	if(p.dispSizeZ < 0 ){ p.dispSizeZ = 0;}
		    	else if(p.dispSizeZ > 2 * (p.globMaxAra[myConsts.COORD_Z] + 1)){p.dispSizeZ = 2 *( p.globMaxAra[myConsts.COORD_Z] + 1);}
		    	break;}
		    case lbl_vIncrByGravMult : {//multiplier of gravity to increase/decrease velocity
		    	eng.gravMult += (( p.mapD(yMod, -1.0f, 1.0f, -.01f, .01f) + p.mapD(xMod, -1.0f, 1.0f, -.1f, .1f)) * myConsts.grav);
		    	if(eng.gravMult < 0){eng.gravMult = 0;}
		    	else if(eng.gravMult > 100){eng.gravMult = 100;}
			    p.setFlags(myConsts.useStoredErosionMeshVals,false);
		    	break;}
		    case lbl_numVertsPerSide : {//1/2 number of verts per side of mesh
		    	sub.numVertsPerSide += (int)((p.mapD(yMod, -15.0f, 15.0f, -.1f, .1f) + p.mapD(xMod, -5.0f, 5.0f, -.1f, .1f)) * sub.numVertsPerSide);
		    	if(sub.numVertsPerSide < 4){sub.numVertsPerSide = 4;}
		    	else if(sub.numVertsPerSide > 150){sub.numVertsPerSide = 150;}
			    p.setFlags(myConsts.useStoredErosionMeshVals,false);
		    	break;}
		    case lbl_genRiverSlope : {//number of verts laterally for each vert in altitude
		    	sub.genRiverSlope += (int)((p.mapD(yMod, -10.0f, 10.0f, -.05f, .05f) + (p.mapD(xMod, -10.0f, 10.0f, -.1f, .1f))) * sub.genRiverSlope);
		    	if(sub.genRiverSlope < 1){sub.genRiverSlope = 1;}
		    	else if(sub.genRiverSlope > 300){sub.genRiverSlope = 300;}
			    p.setFlags(myConsts.useStoredErosionMeshVals,false);
		    	break;}
		    default : break;
		}//switch
	}//handleGuiChoiceVal function

//	/**
//	*  returns the appropriate directory name to save image or file to
//	*  @param prefix context-related prefix for directory name
//	*/
//	public String getWriteToDirOld(String prefix){
//		String res1 = prefix  + ((p.allFlagsSet(myConsts.meshIsGenerated)) ? ( p.allFlagsSet(myConsts.wrapAroundMesh) ? ("RIVER_" + myConsts.riverGenName.get(meshNamesIDX)):("GEN_" + myConsts.terrainGenName.get(meshNamesIDX))) : ("PLY_" + myConsts.terrainFileName.get(meshNamesIDX) ));             
//		String result = "" + res1 + "." +now.get(Calendar.YEAR) + "." + (now.get(Calendar.MONTH)+1) + "." + now.get(Calendar.DAY_OF_MONTH) + "." + now.get(Calendar.HOUR) + "." + now.get(Calendar.MINUTE); 
//		return result;  
//	}//getWriteToDir
//
//	/**
//	*  returns the appropriate file name for the current image to be saved
//	*/
//	public String getStrFileNameOld(String prefix, String fileTypeExt){
//		String result = prefix + ((p.allFlagsSet(myConsts.meshIsGenerated)) ? ( p.allFlagsSet(myConsts.wrapAroundMesh) ? ("RIVER_" + myConsts.riverGenName.get(meshNamesIDX)):("GEN_" + myConsts.terrainGenName.get(meshNamesIDX))) : ("PLY_" + myConsts.terrainFileName.get(meshNamesIDX) ));   
//		result += "_sqSqSub" +  "_Vert_" + sub.vertList.size() + "_Poly_" + sub.polyList.size()+"." + now.get(Calendar.YEAR) + "." + (now.get(Calendar.MONTH)+1) 
//	              + "." + now.get(Calendar.DAY_OF_MONTH) + "." + now.get(Calendar.HOUR) + "." + now.get(Calendar.MINUTE) + "." + now.get(Calendar.SECOND) + fileTypeExt;// ".count." + saveClickCount + ".jpg";
//		return result;    
//	}//getStrFileName

	/**
	*  returns the appropriate directory name to save image or file to
	*  @param prefix context-related prefix for directory name
	*/
	public String getWriteToDir(String prefix){
		String res1 = prefix + getNameAndTypeOfMesh();             
		String result = "" + res1 + "." +now.get(Calendar.YEAR) + "." + (now.get(Calendar.MONTH)+1) + "." + now.get(Calendar.DAY_OF_MONTH) + "." + now.get(Calendar.HOUR) + "." + now.get(Calendar.MINUTE); 
		return result;  
	}//getWriteToDir

	/**
	*  returns the appropriate file name for the current image to be saved
	*/
	public String getStrFileName(String prefix, String fileTypeExt){
		String result = prefix + getNameAndTypeOfMesh();   
		result += "_sqSqSub" +  "_Vert_" + sub.vertList.size() + "_Poly_" + sub.polyList.size()+"." + now.get(Calendar.YEAR) + "." + (now.get(Calendar.MONTH)+1) 
	              + "." + now.get(Calendar.DAY_OF_MONTH) + "." + now.get(Calendar.HOUR) + "." + now.get(Calendar.MINUTE) + "." + now.get(Calendar.SECOND) + fileTypeExt;// ".count." + saveClickCount + ".jpg";
		return result;    
	}//getStrFileName
	
	
	/**
	 * creates and opens a debug writer object to write debug info to a file; close it when done
	 */
	public void fileWriterDebug_Create(){		
		debugPipesTxtFileName = this.getStrFileName("DEBUG_", ".txt");
		debugOutput = p.createWriter(this.getWriteToDir("DEBUG" + fileDelim) + fileDelim + debugPipesTxtFileName);
		eng.debugFluxVolVals.clear();
	}//createDebugWriter

	/**
	 * creates and opens a writer object to save mesh settings
	 */
	public void meshSettingsWriter_Create(){		
		this.meshVarsSettingsTxtFileName = this.getStrFileName("MeshSettings_" + p.currMeshID, ".txt");
		this.meshVarsOutput = p.createWriter(this.getWriteToDir("MeshSettings_" + p.currMeshID + "" + fileDelim) + fileDelim + meshVarsSettingsTxtFileName);
	}//createDebugWriter
	
	public void meshSettingsWriter_Close(){ meshVarsOutput.flush();meshVarsOutput.close();}	
	public void fileWriterDebug_Close(){ debugOutput.flush();debugOutput.close();}

	/**
	*  save current screenshot
	*/
	public void savePic(){
		print2Cnsl("start save");
		saveClickCount++;
		String saveName = p.sketchPath() + fileDelim + this.getWriteToDir("") + fileDelim + this.getStrFileName("", ".count." + saveClickCount + ".jpg");
		print2Cnsl("Save name is : " + saveName);
		p.save(saveName);
	}
	
	//checks if passed idx is in list of flags to be ignored/(values not saved to file)
	private boolean ignoreThisFlagName(int i){ 	return myConsts.ignoreFlagNames.contains(i); }
	
	//saves mesh settings to a file for each mesh, to be manually entered into myConsts class structures
	public void meshSettingsWriter_Write(){//write 3 groups of values in format as array
		this.meshVarsOutput.println();
		this.meshVarsOutput.println("Mesh flags by name : ");
		for(int mIdx = 0; mIdx < p.mesh_vals.size(); ++mIdx){
			String outStr = "";
			outStr += "{";
			ArrayList<String> mFlagsNames = p.mesh_vals.get(mIdx).getFlagNames();
			for(int i = 0; i<mFlagsNames.size(); ++i){
				if((i != mFlagsNames.size()-1) && (!ignoreThisFlagName(i))){			outStr += "" + mFlagsNames.get(i) + ", ";}
				else if(i == mFlagsNames.size()-1) {									outStr += "" + mFlagsNames.get(i);}					
			}//for each flag
			outStr += "},";
			this.meshVarsOutput.println("" + outStr  + "//\t\t"+ myConsts.meshConstNames[mIdx] + " : idx : " + mIdx);
		}//for each describer
		this.meshVarsOutput.println();
		this.meshVarsOutput.println("Mesh erosion vals : ");
		for(int mIdx = 0; mIdx < p.mesh_vals.size(); ++mIdx){
			String outStr = "";
			outStr += "{";
			double[] erosionVals = p.mesh_vals.get(mIdx).getErosionKConsts();
			for(int i = 0; i<erosionVals.length; ++i){
				if(i != erosionVals.length-1){			outStr += "" +  erosionVals[i] + ", ";}
				else {									outStr += "" +  erosionVals[i];}					
			}//for each flag
			outStr += "},";
			this.meshVarsOutput.println("" + outStr  + "//\t\t"+ myConsts.meshConstNames[mIdx] + " : idx : " + mIdx);
		}
		this.meshVarsOutput.println();
		this.meshVarsOutput.println("Mesh sim vals : ");
		for(int mIdx = 0; mIdx < p.mesh_vals.size(); ++mIdx){
			String outStr = "";
			outStr += "{";
			double[] simVals = p.mesh_vals.get(mIdx).getErosionSimConsts();
			for(int i = 0; i<simVals.length; ++i){
				if(i != simVals.length-1){			outStr += "" + simVals[i] + ", ";}
				else {								outStr += "" + simVals[i];}					
			}//for each flag
			outStr += "},";
			this.meshVarsOutput.println("" + outStr  + "//\t\t"+ myConsts.meshConstNames[mIdx] + " : idx : " + mIdx);
			
		}//for each sim val		
	}//meshSettingsWriter_write

	/**
	 * returns debug info for display for pipes model flux and h2o vol
	 * @return array holding string data for debug info
	 */
	public String[] debug_printPipesData(){
		String[] retString = new String[eng.debugFluxVolVals.size()];
		int retIdx = 0;
		boolean printedVals = false;

		if (eng.debugFluxVolVals != null){
			SortedSet<Double> keyValSorted = p.sortSet( eng.debugFluxVolVals.keySet());
			Double[] keyVals = p.setToDouble( keyValSorted);//= keyValSorted.toArray(new Float[0]);
			for (int idx = 0; idx < keyVals.length; ++idx){
				double xIDX = keyVals[idx];
				debugOutput.println(eng.debugFluxVolVals.get(xIDX).toString());
				if(!printedVals){printedVals = true;}
				retString[retIdx] = "x:"+ xIDX + " w:" + df2.format(eng.debugFluxVolVals.get(xIDX).volH2O);
				for (int dir = 0; dir < 4; ++dir){
					if ((dir == 3) || (dir==1)){//for only westbound flux or eastbound
					retString[retIdx] += " | F@ Dir:"+dir+" = " + df2.format(eng.debugFluxVolVals.get(xIDX).fluxVals[dir])+"    ";
					}
				}//for each dir		
				retString[retIdx] += " K = " + df2.format(eng.debugFluxVolVals.get(xIDX).calcK);
				++retIdx;
			}//for each keyVal - each x location being displayed
			if(printedVals){
				debugOutput.println("===================================================================================");
			}
		}//if not null object		
		return retString;
	}
	
	/**
	 * displays current camera position values
	 */
	public String[] printCamVals(){
		String[] results = {( "Rotation X : " + df3.format(p.rotationX) ), ("Rotation Y : " + df3.format(p.rotationY)) ,  ("Distance : " + df3.format(p.getCamRadSpin()))};
		return results;
	}
	
	/**
	*  puts a message in the display console array, at idx 0, first moving 0 to idx 1, etc
	*/
	public void print2Cnsl(String msg1){
		String msg = ((msg1.length() < consoleWidth) ? (msg1) : ( msg1.substring(0,consoleWidth).replace("\n"," ")));
		PApplet.println(">:" + msg1);//print to applet console for debugging purposes
		for (int i = numConsoleLines - 1; i > 0; i--){ dispConsoleAra[i] = dispConsoleAra[i-1]; }
		dispConsoleAra[0] = " : " +  msg;
		if (msg1.length() >= consoleWidth){print2Cnsl(msg1.substring(consoleWidth));}
	}
	//end key and mouse routines
	
	
	
	//will make a tiny graphic of a closed lock
	private void lockClosed(float x, float y){		
		p.pushMatrix();
		p.pushStyle();
			
			p.translate(x,y);
			LockBody(gui_Gray, gui_LightGray);
			LockBarClosed();
		
		p.popStyle();
		p.popMatrix();	
	}
	
	//will make a tiny graphic of an open lock
	private void lockOpen(float x, float y,int gDark, int gLight){
		p.pushMatrix();
		p.pushStyle();
			p.translate(x,y);
			LockBody(gDark, gLight);
			LockBarOpened(gDark, gLight);		
		p.popStyle();
		p.popMatrix();	
	}
	
	private void LockBody(int gDark, int gLight){
		this.setColorValFill(gLight);
		this.setColorValStroke(gDark);
		p.box(globClickX/150.0f,globClickY/120.0f,globClickX/450.0f);	
		p.translate(0, -globClickY/200.0f);
	}//lockbody
	
	private void LockBarClosed(){
		this.setColorValFill(gui_Gray);
		this.setColorValStroke(gui_LightGray);
		p.translate(globClickY/300.0f, -globClickY/500.0f);
		p.box(globClickX/480.0f,globClickY/200.0f,globClickX/400.0f);		
		p.translate(-globClickY/150.0f,0);
		p.box(globClickX/480.0f,globClickY/200.0f,globClickX/400.0f);		
		p.translate(globClickY/300.0f, -globClickY/230.0f);
		p.box(globClickX/180.0f, globClickY/300.0f,globClickX/400.0f);
	}
	
	private void LockBarOpened(int gDark, int gLight){
		this.setColorValFill(gDark);
		this.setColorValStroke(gLight);
		p.translate(globClickY/300.0f, -globClickY/200.0f);
		p.box(globClickX/480.0f,globClickY/200.0f,globClickX/400.0f);		
		p.translate(-globClickY/150.0f,0);
		p.box(globClickX/480.0f,globClickY/200.0f,globClickX/400.0f);		
		p.translate(globClickY/300.0f, -globClickY/230.0f);
		p.box(globClickX/180.0f, globClickY/300.0f,globClickX/400.0f);
	}
	

	/**
	*  print out values from rain erosion calculations
	*/
	public void debugRainErosion(myHeightNode tmpNode, 
	                      HashMap<Integer,Float> lowNeighHeightDiff, 
	                      HashMap<Integer,myHeightNode> lowerNeighbors, 
	                      HashMap<Integer,Float> delWNeighbors, 
	                      HashMap<Integer,Float> cS, 
	                      float totHeightDiff,
	                      HashMap<Integer,Float> heightDiff ){
		float lowerHeight, ratio;                        
	                        
		if (totHeightDiff > 0){ PApplet.println("Slope/Peak vert : " + tmpNode.ID);
	    	for (Integer idx : delWNeighbors.keySet()){
	    		PApplet.println("\tdelta W for node id : " + idx + " (adjacent to node ID : " + tmpNode.ID + " with water vol : " + tmpNode.getHeightWater() +  ") = " + delWNeighbors.get(idx));
	    		if (heightDiff.get(idx) > 0){
	    			lowerHeight = lowNeighHeightDiff.get(idx);
	    			ratio = lowNeighHeightDiff.get(idx)/totHeightDiff;
	    			PApplet.println("\t\tlower neighbor distance : " +  lowerHeight + " total height diff : " + totHeightDiff + " calc ratio : " +  ratio ); 
	    			//println("\t\tcalculated cS : " + cS.get(idx) + " node's sediment ration : " + tmpNode.getSediment()*ratio + " cs greater? : " +  (cS.get(idx) > tmpNode.getSediment()*ratio) );
	    		}//if positive delW
	    	}//for idx
		} else {
			PApplet.println("Valley vert : " + tmpNode.ID);
			for (Integer idx : delWNeighbors.keySet()){
				PApplet.println("\tdelta W for node id : " + idx + " adjacent to node ID : " + tmpNode.ID + " with water vol : " + tmpNode.getHeightWater() +  " : " + delWNeighbors.get(idx));
			}//for idx
		}
		PApplet.println();
	}//debugRainErosion	
	
}//myGui class

