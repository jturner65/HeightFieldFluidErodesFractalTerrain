package sqSqErosionSimPkg;
//import processing.core.*;
//import processing.opengl.*;

public class myCam {
	private sqSqErosionSimGlobal p;

	public final double timeIncr = .05f;               //amount time increments per cycle of draws

	public final double minCamDist = .5f;                          //closest we want to get to the scene
	public final double groundRadius = 100;
	public final double maxCamDist = groundRadius * 5f;          //furthest we want to get to the scene
	public final double initEyeDistance = groundRadius/5;                     //where the eye should start, x and y, relative to the center of the scene

	//constants - indexes
	//camera array indexes
	public final int xIdx = 0;                                   //indexes in arrays for x, y and z coord
	public final int yIdx = 1;
	public final int zIdx = 2;

	//clickara indexes                                    
	public final int EyeMoveAzmIDX = 4;
	public final int EyeMoveAltIDX = 5;
	
	private float[] resAra = new float[3];		//used for camera movement results
	private float[] res9Ara = new float[9];		//used for camera movement results

	public final int clickAraSize = 20;
		//value to tell the camera to move back, set by subdivision to counter doubling in size of mesh
	public double camMoveBackVal = 1;
	
	//scenery values and structures
//	private myVector globAmbientColor;                       //ambient color, used by all objects

	//camera processing structures
	private double[] lastEyePos = new double[3];              //vector arrays holding most recent eye location, cam direction and up dir values
	private double[] lastCamCtrPos = new double[3];
	private double[] lastUpPos = new double[3];

//	private boolean camFocused = false;                     //whether the camera is focused on a new location at this time
//	private double camHeight = 30;                           //specific z height for camera and eye - may be made constant
	private double radDistMod = 0;                           //initializing radius distance modifier used when camera zoom keys are pressed
	private double scaleFact = .7f;                           //the controls the speed of the movement of the camera
	public double radSpin;                                  //the spin radius of the current camera eye

	//keypress mod values
	private boolean camAzmSpin = false;                        //whether our camera is currently spinning around the center of the scene
	private boolean camAzmSpinCW = false;                      //whether our camera is currently moving in the clockwise direction
	public boolean keyAzmSpin = false;                        //whether a key telling our cameraeye to rotate in the horizontal plane is currently pressed or not

	private boolean camAltSpin = false;                        //whether our camera is currently spinning around the center of the scene
	private boolean camAltSpinCW= false;                      //whether our camera is currently moving in the clockwise direction
	public boolean keyAltSpin = false;                        //whether a key telling our camera eye to rotate vertically around the center is currently pressed or not

	private double[] clickAra = new double[clickAraSize];     //array holding on/off versions of time, 
	                                                //for when certain animations need to be stopped and restarted

	public myCam(sqSqErosionSimGlobal p) {
		this.p = p;
		initCamGlobals();
		
	}

	
	///////////////camera stuff
	/////////////////////////////////////////////////////////////from snowmen project
	/**
	*  initialize global variables
	*  
	*/
	public void initCamGlobals(){
	  
	  //initialize the array that holds the various sequencer click elements
	  //that are related to the various animating objects in the scene
	  for (int i = 0; i < clickAraSize; ++i){
	    clickAra[i] = 0;             
	  }//for clickarasize
	  //initialize altitude click to start at pi/4 degrees from z axis (vertical)
	  clickAra[EyeMoveAltIDX] = (Math.PI/(4.01f * scaleFact));
//	  globAmbientColor = new myVector(20,20,20);
	}//initglobals

	/**
	*  initialize the 3 camera-related arrays of values
	*  for the eye position, the center/focus of the eye position
	*  and the orientation of what we consider "up" to be
	*/
	public void initCam(){
	  //initialize array holding most recent camera direction x,y 

	  //initialize last eye position x,y
	  //setEyePosCtr(0,0,initEyeDistance);

	  //initialize array of x,y,z values defining what "up" is.
	  lastUpPos[xIdx]= 0.0f;  
	  lastUpPos[yIdx]= 1.0f;  
	  lastUpPos[zIdx]= 0.0f;  
	  
	}//init cam method

	/*
	*  sets camera center position 
	*  @params x,y,z values of desired cam center position
	*/
	public void setCamDirPos(myVector camDirPos){
	  lastCamCtrPos[xIdx] = camDirPos.x;
	  lastCamCtrPos[yIdx] = camDirPos.y;
	  lastCamCtrPos[zIdx] = camDirPos.z;
	}//setCamDirPos
	
	public myVector getCamDirPos(){	return new myVector(lastCamCtrPos[xIdx],lastCamCtrPos[yIdx],lastCamCtrPos[zIdx]);}

	/*
	*  sets initial viewing eye position based on camera position with regard to screen center
	*  @params x,y,z values of desired eye position from camera scene view center
	*/
	public void setEyePosCtr(double initEyeDistX,double initEyeDistY,double initEyeDistZ){
	  //print2Cnsl("setEyePos initCam");
	  lastEyePos[xIdx] = initEyeDistX + lastCamCtrPos[xIdx];
	  lastEyePos[yIdx] = initEyeDistY + lastCamCtrPos[yIdx];
	  lastEyePos[zIdx] = initEyeDistZ + lastCamCtrPos[zIdx];
	}//setEyePos
	
	public void setRadDistMod(double val){
		this.radDistMod = val;
	}
	
	/*
	*  sets initial viewing eye position based on camera position 
	*  @params x,y,z values of desired eye position from camera scene view center
	*/
	public void setEyePos(double initEyeDistX,double initEyeDistY,double initEyeDistZ){
	  //print2Cnsl("setEyePos initCam");
	  lastEyePos[xIdx] = initEyeDistX ;
	  lastEyePos[yIdx] = initEyeDistY ;
	  lastEyePos[zIdx] = initEyeDistZ ;
	}//setEyePos


	/**
	*  calculates the linear distance from the eye to the "lookat" point
	*  @global radDistMode = holds modifier to zoom in or out the camera when the appropriate keys are pressed
	*  @global minCamDist = minimum camera distance to prevent zooming in from going too close
	*
	*/
	public double distFromCenter(){
	  double result;
	  result = Math.pow(((lastEyePos[0] - lastCamCtrPos[0]) * (lastEyePos[0] - lastCamCtrPos[0])) + 
	               ((lastEyePos[1] - lastCamCtrPos[1]) * (lastEyePos[1] - lastCamCtrPos[1])) + 
	               ((lastEyePos[2] - lastCamCtrPos[2]) *(lastEyePos[2] - lastCamCtrPos[2])),.5f);
	  result += radDistMod - p.elevationY;
	  if(this.camMoveBackVal != 1){
		  result *= this.camMoveBackVal;
		  this.camMoveBackVal = 1;		  
	  }
	  
	  if (result < minCamDist){
	    result = minCamDist;
	  }
	  if (result > maxCamDist){
	    result = maxCamDist;
	  }
	  return result; 
	}

	/**
	*  calculates the current x, y, z location of the camera eye in the scene based solely on the click for each direction
	*  @return an array holding the current x,y, z location of the eye
	*/
	public float[] calcEyeMove(){
	  //calculate the cosine and sine multipliers to determine the eye position for around the center and height above it.
		  double spinCAzm = Math.cos(scaleFact * (clickAra[EyeMoveAzmIDX] + p.rotationX) );
		  double spinSAzm = Math.sin(scaleFact * (clickAra[EyeMoveAzmIDX] + p.rotationX) );
		  double spinCAlt = Math.cos(scaleFact * (clickAra[EyeMoveAltIDX] - p.rotationY));
		  double spinSAlt = Math.sin(scaleFact * (clickAra[EyeMoveAltIDX] - p.rotationY));

//		  double spinCAzm = Math.cos(scaleFact * (clickAra[EyeMoveAzmIDX]) );
//		  double spinSAzm = Math.sin(scaleFact * (clickAra[EyeMoveAzmIDX]) );
//		  double spinCAlt = Math.cos(scaleFact * (clickAra[EyeMoveAltIDX]));
//		  double spinSAlt = Math.sin(scaleFact * (clickAra[EyeMoveAltIDX]));

	  this.radSpin = distFromCenter();

	  //keep altitude between 0 and pi
	  if (Math.abs(scaleFact * clickAra[EyeMoveAltIDX]) < 0.00001) {
	    clickAra[EyeMoveAltIDX] = 0;
	    spinCAlt = 1;
	    spinSAlt = 0;    
	    camAltSpinCW = !(camAltSpinCW);
	  } else if (scaleFact * clickAra[EyeMoveAltIDX] > Math.PI){
	    clickAra[EyeMoveAltIDX] = 1.0*Math.PI/scaleFact;
	    spinCAlt = -1;
	    spinSAlt = 0;   
	    camAltSpinCW = !(camAltSpinCW);
	  }
	  
	  //figure out the new eye position based on location of current center
	  resAra[xIdx] = (float) ((spinSAlt * spinCAzm * radSpin) + lastCamCtrPos[xIdx] );
	  resAra[yIdx] = (float) ((spinSAlt * spinSAzm * radSpin) + lastCamCtrPos[yIdx] );
	  resAra[zIdx] = (float) ((spinCAlt * radSpin) + lastCamCtrPos[zIdx]);

	//  print2Cnsl(lastCamCtrPos[0] + "|" + lastCamCtrPos[1] + "|" + lastCamCtrPos[2]);
	//  print2Cnsl(resAra[0] + "|" + resAra[1] + "|" + resAra[2]);

	  for (int i = 0; i < 3; ++i){
	    lastEyePos[i] = resAra[i];
	  }
	  return resAra;
	}//calcEyeMove 

	/**
	*  calculates the current x, y, z location of the direction the camera eye is pointing at in the scene
	*  @return an array holding the current x,y, z direction of the eye gaze
	*/
	public float[] calcEyeDir(){
	  double[] tmpEyeDispVals = new double[3]; //temp value holding the displacement of the eye relative to the last camera position
	  //save eye -to-scene center values before calculating new center, to preserve distance for viewing and minimize the jarring transition
	  //focusing on new center
	  for (int i = 0; i < 3; ++i){
	    tmpEyeDispVals[i] = lastEyePos[i] - lastCamCtrPos[i];
	  }

	  for (int i = 0; i < 3; ++i){
	    resAra[i] = (float)lastCamCtrPos[i];
	  }
	  

	  for (int i = 0; i < 3; ++i){
	    lastCamCtrPos[i] = resAra[i];
	  }
	  return resAra;
	}//calcEyeDir 

	/*
	*  moves the camera location and "look-at" direction
	*  over time based on a parametric 3d line equation
	*  @param the current time value used to tick the animation
	*/
	public float[] moveCamera(){
	  //indexed 0 = x, 1 = y, 2 = z
	  float[] iAra = calcEyeMove();             //eye locations
	  for(int i = 0; i < 3; ++i) {res9Ara[i] = iAra[i]; }
	  float[] cAra = calcEyeDir();             //target of eye (looking at) location
	  for(int i = 0; i < 3; ++i) {res9Ara[i+3] = cAra[i]; }
	  for(int i = 6; i < 8; ++i) {res9Ara[i] = 0.0f; }
	  res9Ara[8] = -1.0f;
	 //print2Cnsl("i:" + iAra[0] +"|"+ iAra[1] +"|"+ iAra[2] + "|c:" + cAra[0] +"|"+cAra[1] +"|"+cAra[2]); 
	  //instance the camera with current locations and directional orientation
	  //p.camera(iAra[xIdx],iAra[yIdx],iAra[zIdx],
	  //	         cAra[xIdx],cAra[yIdx],cAra[zIdx],
	  //	         0.0f, 0.0f, -1.0f);
	  return res9Ara;
	 
	}//move camera
	
	public double getCamClickAraVal(int idx){
		return this.clickAra[idx];
	}

	public void updateCamCounters(){
	  //increment elements of movement/rotation sequencer array based on conditions (whether certain animations are on or not)
	  //this sequencer element is used to calculate automated "strafing eye" with camera, which rotates
	  //around the center of the action
	  if (camAzmSpin || keyAzmSpin){
	    clickAra[EyeMoveAzmIDX] += (camAzmSpinCW) ?  .05 : -.05; 
	    if (Math.abs(clickAra[EyeMoveAzmIDX]) > 1000*Math.PI) {//prevent overflows
	      clickAra[EyeMoveAzmIDX] = 0;
	    }
	  }//if cam or key spin azm
	 
	  //this sequencer element is used to calculate automated "strafing eye" with camera, which rotates
	  //around the center of the action
	  if (camAltSpin || keyAltSpin){
	    clickAra[EyeMoveAltIDX] += (camAltSpinCW) ?  .05 : -.05; 
	    if (Math.abs(clickAra[EyeMoveAltIDX]) > 1000*Math.PI) {//prevent overflows
	      clickAra[EyeMoveAltIDX] = Math.PI/2;
	  
	    }
	  }//if cam or key spin Alt  
	  // maybe step forward in time (for object rotation)
	}

	/**
	*  handle camera related key events
	*/
	public boolean handleCamera(int keyCode){
	  //turn on azimuth spinning
	  if (keyCode == 'J'){camAzmSpin = !(camAzmSpin);}                                          //if j turn on strafing camera eye
	  if (keyCode == 'H'){camAzmSpinCW = !(camAzmSpinCW);}                                      //if h reverse direction of strafing camera eye 
	  //turn on altitude spinning
	  if (keyCode == 'U'){camAltSpin = !(camAltSpin);}                                          //if u turn on altitude camera eye
	  if (keyCode == 'Y'){camAltSpinCW = !(camAltSpinCW);}                                      //if h reverse direction of altitude camera eye
	  
	  //zoom in and out camera eye toward center 
	  if (keyCode == 'W'){if (distFromCenter() > minCamDist){radDistMod = -1;}}                //if w move camera eye toward center
	  if (keyCode == 'X'){radDistMod = 1;}                                                     //if x move camera eye away from center
	  
	  //control azm spin
	  if (keyCode == 'C'){keyAzmSpin = true;camAzmSpinCW = true;}                                  //if z rotate camera eye around center in ccw dir
	  if (keyCode == 'Z'){keyAzmSpin = true;camAzmSpinCW = false;}                                 //if c rotate camera eye around center in cw dir
	  //control alt spin
	  if (keyCode == 'D'){keyAltSpin = true; camAltSpinCW = false;}                              //if D pressed, rotate camera eye up around target point  
	  if (keyCode == 'A'){keyAltSpin = true; camAltSpinCW = true;}                               //if A pressed, rotate camera eye down around target point
	  return true;//old keydown val
	}//handleCamera

	  
	/**
	*  draws a set of x,y,z axes that are red,green and blue respectively  at what is considered the origin at time of call
	*/  
	public void drawAxes(){
	  //center x,y,z axes for reference
	  p.stroke(255,0,0);
	  p.line(0,0,0,10,0,0);
	  p.stroke(0,255,0);
	  p.line(0,0,0,0,10,0);
	  p.stroke(0,0,255);
	  p.line(0,0,0,0,0,10);
	  p.noStroke();
	}//draw axes method
}
