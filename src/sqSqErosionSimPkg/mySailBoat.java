package sqSqErosionSimPkg;

import processing.core.PConstants;

public class mySailBoat {
	private sqSqErosionSimGlobal p;
	private myGui gui;
	private mySqSqEroder eng;
	//private myCam cam;
	//private mySqSqSubdivider sub;
	//unique id number for this object
	public int ID;
	public static int IDcount = 0;
	private myVector[] coords;					//com coords
	private myVector nodeLocs;				//location using mesh idx "coords" - integral amounts are nearest lower idx, decimal amts are interpolated location between nodes
	private myVector[] orientation;			//R matrix - 3x3 orthonormal basis matrix - cols are bases for body frame orientation in world frame
	private double mass;	
	private double volume;			//volume of this boat
	private float oldRotAngle;
	private myVector[] velocity;
	private myVector[] forces;
	private myVector oldForce;
	//private final int cbody = 0, csail = 1, cmast = 2;	
	private myVector[][] verts;
	private int[] sailColor, emblemColor;
	
	public mySailBoat(sqSqErosionSimGlobal _p, myGui _gui,  mySqSqEroder _eng,  mySqSqSubdivider _sub, myCam _cam, myVector _coords){ 
		this.p = _p;
		this.gui = _gui;
		this.eng = _eng;
		//this.sub = _sub;
		//this.cam = _cam;
		this.ID = mySailBoat.IDcount++;
		this.nodeLocs = new myVector(0,0,0);
		this.orientation = new myVector[3];
		this.orientation[2] = myVector.RIGHT.cloneMe();
		this.orientation[1] = myVector.UP.cloneMe();
		this.orientation[0] = myVector._mult(myVector.FORWARD.cloneMe(), -1);
		this.mass = 1;
		this.volume = 2;	//number of cubic units of volume this boat's hull holds - for buoyancy calc
		this.coords = new myVector[2]; 
		this.velocity = new myVector[2];
		this.oldForce = new myVector(0,0,0);
		this.forces = new myVector[2];
		this.sailColor = new int[]{150 + (int)(Math.random() * 155),100+(int)(Math.random() * 200),210 + (int)(Math.random() * 55)};
		this.emblemColor = new int[]{155 + (int)(Math.random() * 155),(int)(Math.random() * 150),155 - (int)(Math.random() * 155)};
		for (int i = 0; i < 2 ; ++i){
			this.coords[i] = _coords;											//coords of body in world space
			this.velocity[i] = myVector.ZERO.cloneMe();
			this.forces[i] = myVector.ZERO.cloneMe(); 
		}
		this.verts = new myVector[5][12];
		double xVert, yVert, zVert;	
		for(int j = 0; j < this.verts[0].length; ++j){
			zVert = j - 4;		
			double sf = (1 - ((zVert+3)*(zVert+3)*(zVert+3))/(this.verts[0].length * this.verts[0].length * this.verts[0].length * 1.0));
			for(int i = 0; i < this.verts.length; ++i){
				xVert = (1.5*i - 3) * sf;
				yVert = ((-1 * Math.sqrt(9 - ((1.5*i - 3)*(1.5*i - 3))) ) * sf) + (3*(zVert-2)*(zVert-2))/(this.verts[0].length *this.verts[0].length);
				this.verts[i][j] = new myVector(xVert, yVert, zVert);
			}//for i	
		}//for j		
		if(this.ID == mySailBoat.IDcount-1){this.updateGlobSliceVals();}
		this.oldRotAngle = 0;
	}//constructor
	
	public mySailBoat(sqSqErosionSimGlobal _p, myGui _gui,  mySqSqEroder _eng,  mySqSqSubdivider _sub,  myCam _cam){	this(_p,_gui,_eng,_sub, _cam, myVector.ZERO.cloneMe());}	
	/**
	 * update the boat's location based on possibly moving from gui input via the slice coords changing
	 */
	public void updateCoords(){			this.coords[0].set(new myVector(p.dispCoordX, this.coords[0].y, p.dispCoordZ));	}
	/**
	 * update global coords of slice location, so that the gui can move the boat around
	 */
	public void updateGlobSliceVals(){		p.dispCoordX = this.coords[0].x; p.dispCoordZ = this.coords[0].z;}
	/**
	 * evaluate time step for moving boat based on semi-implicit method
	 * @param delT
	 */
	public void evalTimeStep(double delT){
		if((eng.heightMapAra != null) && (eng.heightMapAra.length != 0) && (eng.heightMapAra[0].length != 0)){
			//find difference of forces over last time step
			myVector newForce = eng.getForceAtLocation(this.coords[0], this.mass, this.volume);
			this.forces[1] = myVector._mult(myVector._sub(newForce,oldForce), delT);//change in forces over this past time step
			this.oldForce.set(newForce);
			myVector tmpFrcVec =  myVector._mult(this.forces[1], delT/(1.0 * this.mass));
			this.velocity[1] = myVector._add(this.velocity[0], tmpFrcVec);			//divide by  mass, multiply by delta t
			this.coords[1] = myVector._add(this.coords[0], myVector._mult(this.velocity[1], delT));	
			//evaluate wrap around coords - idx 0 and 2 are new x and z coords
			double [] resAra = eng.findValidBoatLocs(this.coords[1]);
			this.coords[1].set(resAra[0], resAra[1], resAra[2]);
			
			//find new orientation at new coords - boat is oriented in local axes as forward being positive z and up being positive y
			//vectors correspond to columns, x/y/z elements correspond to rows
			this.orientation[1] = eng.getWaterNormAtLocation(this.coords[1]);//up is water normal - already normalized
			if(this.orientation[1]._SqMag() < myConsts.epsValCalc){this.orientation[1] = myVector.UP.cloneMe();}
			myVector forwardHo = this.orientation[2].cloneMe();//copy current "forward" incase of collision			
			this.orientation[2] = eng.getWaterVelAtLocation(this.coords[1]);//forward is water velocity direction
			if(this.orientation[2]._SqMag() < myConsts.epsValCalc){
				this.orientation[2] = myVector.FORWARD.cloneMe();
				myVector tmpWater = eng.getWaterAtLocation(this.coords[1]);
				if(tmpWater._SqMag() < myConsts.epsValCalc){//means we're on dry land
					//if sqmag of water at coords is 0 then we're on terrain, we need to recalc heading, velocity and location - reflect off normal of terrain for forward
					myVector terrNorm = eng.getTerrainNormAtLocation(this.coords[1]);
					forwardHo._mult(-1.0);
					//should return reflected orientation around normal
					myVector rflOrient = myVector._sub(myVector._mult(terrNorm, 2 * forwardHo._dot(terrNorm)), forwardHo);
					this.orientation[2].set(rflOrient.x, forwardHo.y, rflOrient.z);
					//this.velocity[0].set(this.velocity[0].z,this.velocity[0].y, -this.velocity[0].x);
					this.coords[1]._add(myVector._mult(this.orientation[1],new myVector(.01,0,.01)));//should move away from collision in direction of new forward
				}//no water at current location
			}			
			this.orientation[0] = this.orientation[1]._cross(this.orientation[2]); //sideways is cross of up and forward
			this.orientation[0]._normalize();
			//need to recalc forward?  may not be perp to normal
			if(Math.abs(this.orientation[2]._dot(this.orientation[1])) > myConsts.epsValCalc){//if so recalc forward vector
				this.orientation[2] = this.orientation[0]._cross(this.orientation[1]);
			}
			this.orientation[2]._normalize();		
		}
	}//evalTimeStep
	
	public void updateBoat(){		
		//move to use-now idx (0)
		this.velocity[0].set(this.velocity[1]);
		this.coords[0].set(this.coords[1]);
		if(this.ID == mySailBoat.IDcount-1){this.updateGlobSliceVals();}		
	}

	/**
	 * align the boat along the current orientation matrix
	 */
	private void alignBoat(float delT){
		float rotAngle = (float)Math.acos((this.orientation[0].x + this.orientation[1].y + this.orientation[2].z - 1)/2.0);
		if(rotAngle > 0.0){
			double 	diff21 = this.orientation[1].z - this.orientation[2].y,//(m21 - m12)
					diff02 = this.orientation[2].x - this.orientation[0].z,//(m02 - m20)
					diff10 = this.orientation[0].y - this.orientation[1].x,// (m10 - m01)
					denom = Math.sqrt((diff21 * diff21) + (diff02 * diff02) + (diff10 *diff10));
			myVector rotVec = new myVector(diff21/denom, diff02/denom, diff10/denom);
			//axis angle rotation
			p.rotate(oldRotAngle + (rotAngle - oldRotAngle)*delT, (float)rotVec.x, (float)rotVec.y, (float)rotVec.z);
			this.oldRotAngle = rotAngle;
		}//if rotangle >0
	}//alignboat
	
	//draw this body on mesh
	public void drawMe(float delT){
		if(this.ID == mySailBoat.IDcount-1){updateCoords();}//update the boat's coords incase they've changed via gui input
		p.pushMatrix();{
			p.translate((float)this.coords[0].x,(float)this.coords[0].y,(float)this.coords[0].z);		//move to location
			this.alignBoat(delT);
			p.scale(.15f,.15f,.15f);																	//make appropriate size
			p.pushStyle();
			gui.setColorValFill(myGui.gui_boatBody);
			gui.setColorValStroke(myGui.gui_boatBody);
			//draw body
			//p.noStroke();
			drawBody();
			p.pushMatrix();
				p.translate(0, 3.5f, -3);
				p.pushMatrix();
					p.scale(.95f,.85f,1);
					drawMast(false);
				p.popMatrix();
				p.translate(0, -2f, 4);
				p.pushMatrix();
					p.scale(1.3f,1.2f,1);
					drawMast(false);
				p.popMatrix();
				p.translate(0, .8f, 4);
				p.pushMatrix();
					p.scale(1f,.9f,1);
					drawMast(false);
				p.popMatrix();
				p.translate(0, 0, 2);
				p.rotate(PConstants.PI/3.0f, 1, 0,0);
				p.translate(0,.5f,0);
				drawMast(true);
			p.popMatrix();
			p.popStyle();
		p.popMatrix();}		
	}//drawme
	
	private void drawSail(float len, boolean showCross){
		p.pushStyle();
			//gui.setColorValFill(myGui.gui_boatSail);
			p.fill(this.sailColor[0],this.sailColor[1],this.sailColor[2]);
			p.noStroke();
			//gui.setColorValStroke(myGui.gui_boatSail);
			p.beginShape(PConstants.QUAD);	p.vertex(-1.5f,len*.1f,1.5f);p.vertex(-1.5f,len*.9f,1.5f);p.vertex(0,len,.1f);	p.vertex(0,0,.1f);	p.endShape(PConstants.CLOSE);	
			p.beginShape(PConstants.QUAD);  p.vertex(-1.5f,len*.1f,1.5f);p.vertex(-1.5f,len*.9f,1.5f);p.vertex(-3f,len*.9f,1.5f);p.vertex(-3f,len*.1f,1.5f);p.endShape(PConstants.CLOSE);	
			p.beginShape(PConstants.QUAD);	p.vertex(-3f,len*.1f,1.5f);	p.vertex(-3f,len*.9f,1.5f);	p.vertex(-4f,len,0);p.vertex(-4f,0,0);p.endShape(PConstants.CLOSE);	
				//cross
			if(showCross){
				//gui.setColorValFill(myGui.gui_boatEmblem);
				p.fill(this.emblemColor[0],this.emblemColor[1],this.emblemColor[2]);
				p.beginShape(PConstants.QUAD);	p.vertex(-.5f,len*.3f,.6f);	p.vertex(-1.5f,len*4.0f/9.0f,1.52f);p.vertex(-1.5f,len*.5f,1.55f);p.vertex(-1,len*.5f,1.05f);	p.endShape(PConstants.CLOSE);	
				p.beginShape(PConstants.QUAD);	p.vertex(-1,len*.5f,1.05f); p.vertex(-.5f,len*.7f,.6f);p.vertex(-1.5f,len*5.0f/9.0f,1.52f);	p.vertex(-1.5f,len*.5f,1.55f);	p.endShape(PConstants.CLOSE);	
				p.beginShape(PConstants.TRIANGLE); p.vertex(-1.5f,len*4.0f/9.0f,1.52f);p.vertex(-1.5f,len*5.0f/9.0f,1.52f);	p.vertex(-2.25f,len*.5f,1.52f);	p.endShape(PConstants.CLOSE);	

				p.beginShape(PConstants.QUAD);  p.vertex(-2.25f,len*.5f,1.52f); 	p.vertex(-1.75f,len*.85f,1.52f);p.vertex(-2.25f,len*.75f,1.52f);p.vertex(-2.75f,len*.85f,1.52f);p.endShape(PConstants.CLOSE);	
				p.beginShape(PConstants.QUAD);	p.vertex(-2.25f,len*.25f,1.52f);	p.vertex(-1.75f,len*.15f,1.52f);p.vertex(-2.25f,len*.5f,1.52f);p.vertex(-2.75f,len*.15f,1.52f);p.endShape(PConstants.CLOSE);	
				
				p.beginShape(PConstants.TRIANGLE); p.vertex(-3f,len*4.0f/9.0f,1.52f);p.vertex(-3f,len*5.0f/9.0f,1.52f);	p.vertex(-2.25f,len*.5f,1.52f);	p.endShape(PConstants.CLOSE);	
				p.beginShape(PConstants.QUAD);	p.vertex(-3.75f,len*.3f,.6f);	p.vertex(-3f,len*4.0f/9.0f,1.52f);p.vertex(-3f,len*.5f,1.55f);p.vertex(-3.5f,len*.5f,1.05f);	p.endShape(PConstants.CLOSE);	
				p.beginShape(PConstants.QUAD);	p.vertex(-3.5f,len*.5f,1.05f); p.vertex(-3.75f,len*.7f,.6f);p.vertex(-3f,len*5.0f/9.0f,1.52f);	p.vertex(-3f,len*.5f,1.55f);	p.endShape(PConstants.CLOSE);		
			}			
		p.popStyle();	
	}
	
	private void drawMast(boolean halfmast){		
		drawPole(.1f, (halfmast ? 6 : 10), false);
		p.pushMatrix();
			p.translate(0,  4.5f, 0);
			p.rotate(PConstants.HALF_PI, 0,0,1 );
			if(!halfmast){
				p.translate(0,-3.5f,0);
				drawPole(.05f, 7, true);
				drawSail(7, false);
				p.translate(4.5f,1,0);
				drawPole(.05f, 5, true);	
				drawSail(5,true);
			} else {
				p.translate(1, -1.5f,0);
				drawPole(.05f, 3, true);			
				drawSail(3,true);
			}
				
		p.popMatrix();	
	}//drawmast
	
	private void drawPole(float rad, float height, boolean drawBottom){
		double theta, theta2;
		p.pushMatrix();
			for(int i = 0; i <12; ++i){
				theta = (i/12.0) * 2 * Math.PI;
				theta2 = (((i+1)%12)/12.0) * 2 * Math.PI;
				p.beginShape(PConstants.QUAD);
					p.vertex((float)(rad * Math.sin(theta)),  0, (float)(rad * Math.cos(theta)) );
					p.vertex((float)(rad * Math.sin(theta)),  height, (float)(rad * Math.cos(theta)) );
					p.vertex((float)(rad * Math.sin(theta2)), height, (float)(rad * Math.cos(theta2)) );
					p.vertex((float)(rad * Math.sin(theta2)), 0, (float)(rad * Math.cos(theta2)) );
				p.endShape(PConstants.CLOSE);
				p.beginShape(PConstants.TRIANGLE);
					p.vertex((float)(rad * Math.sin(theta)),  height, (float)(rad * Math.cos(theta)) );
					p.vertex(0,  height, 0 );
					p.vertex((float)(rad * Math.sin(theta2)), height, (float)(rad * Math.cos(theta2)) );
				p.endShape(PConstants.CLOSE);
				if(drawBottom){
					p.beginShape(PConstants.TRIANGLE);
						p.vertex((float)(rad * Math.sin(theta)),  0, (float)(rad * Math.cos(theta)) );
						p.vertex(0,  0, 0 );
						p.vertex((float)(rad * Math.sin(theta2)), 0, (float)(rad * Math.cos(theta2)) );
					p.endShape(PConstants.CLOSE);
				}
			}//for i
		p.popMatrix();	
	}//drawPole
	
	private void drawBody(){
		int idx;
		p.pushMatrix();{
			p.translate(0, 1,0);
			int numZ = this.verts[0].length, numX = this.verts.length;
			for(int j = 0; j < numZ-1; ++j){
				p.beginShape(PConstants.QUAD);
				for(int i = 0; i < numX; ++i){
					p.vertex((float)verts[i][j].x, 	(float)verts[i][j].y, 	(float)verts[i][j].z);		p.vertex((float)verts[(i+1)%numX][j].x, 		(float)verts[(i+1)%numX][j].y,			(float)verts[(i+1)%numX][j].z);					p.vertex((float)verts[(i+1)%numX][(j+1)%numZ].x,(float)verts[(i+1)%numX][(j+1)%numZ].y, (float)verts[(i+1)%numX][(j+1)%numZ].z);					p.vertex((float)verts[i][(j+1)%numZ].x,			(float)verts[i][(j+1)%numZ].y, 			(float)verts[i][(j+1)%numZ].z);
				}//for i	
				p.endShape(PConstants.CLOSE);
			}//for j
			for(int i = 0; i < numX; ++i){	finishBodyBottom(i, numZ, numX);	}//for i	
			for(int j = 0; j < numZ-1; ++j){
				p.beginShape(PConstants.QUAD);	p.vertex((float)verts[0][j].x, 			(float)verts[0][j].y, 	 (float)verts[0][j].z);					p.vertex((float)verts[0][j].x, 					(float)verts[0][j].y +.5f,			 (float)verts[0][j].z);					p.vertex((float)verts[0][(j+1)%numZ].x,			(float)verts[0][(j+1)%numZ].y + .5f, (float)verts[0][(j+1)%numZ].z);					p.vertex((float)verts[0][(j+1)%numZ].x,			(float)verts[0][(j+1)%numZ].y, 	 (float)verts[0][(j+1)%numZ].z);				p.endShape(PConstants.CLOSE);
				p.beginShape(PConstants.QUAD);	p.vertex((float)verts[numX-1][j].x,  (float)verts[numX-1][j].y, 	 (float)verts[numX-1][j].z);					p.vertex((float)verts[numX-1][j].x, 		 (float)verts[numX-1][j].y + .5f,			 (float)verts[numX-1][j].z);					p.vertex((float)verts[numX-1][(j+1)%numZ].x, (float)verts[numX-1][(j+1)%numZ].y +.5f,  (float)verts[numX-1][(j+1)%numZ].z);					p.vertex((float)verts[numX-1][(j+1)%numZ].x, (float)verts[numX-1][(j+1)%numZ].y, 	 (float)verts[numX-1][(j+1)%numZ].z);				p.endShape(PConstants.CLOSE);
			}//for j
			p.translate(0,.5f,0);
			//draw rear castle
			for(int j = 0; j < 3; ++j){
				p.beginShape(PConstants.QUAD);	p.vertex((float)verts[0][j].x*.9f, 			(float)verts[0][j].y-.5f, 			 (float)verts[0][j].z);					p.vertex((float)verts[0][j].x*.9f, 					(float)verts[0][j].y+2,			 (float)verts[0][j].z);					p.vertex((float)verts[0][(j+1)%numZ].x*.9f,			(float)verts[0][(j+1)%numZ].y+2, (float)verts[0][(j+1)%numZ].z);					p.vertex((float)verts[0][(j+1)%numZ].x*.9f,			(float)verts[0][(j+1)%numZ].y-.5f, 	 (float)verts[0][(j+1)%numZ].z);				p.endShape(PConstants.CLOSE);
				p.beginShape(PConstants.QUAD);	p.vertex((float)verts[numX-1][j].x*.9f,  (float)verts[numX-1][j].y-.5f,  (float)verts[numX-1][j].z);					p.vertex((float)verts[numX-1][j].x*.9f, 		 (float)verts[numX-1][j].y+2,			 (float)verts[numX-1][j].z);					p.vertex((float)verts[numX-1][(j+1)%numZ].x*.9f, (float)verts[numX-1][(j+1)%numZ].y+2,   (float)verts[numX-1][(j+1)%numZ].z);					p.vertex((float)verts[numX-1][(j+1)%numZ].x*.9f, (float)verts[numX-1][(j+1)%numZ].y-.5f, 	 (float)verts[numX-1][(j+1)%numZ].z);				p.endShape(PConstants.CLOSE);
				p.beginShape(PConstants.QUAD);	p.vertex((float)verts[0][j].x*.9f, 		(float)verts[0][j].y+1.5f,		(float)verts[0][j].z);					p.vertex((float)verts[numX-1][j].x*.9f, 		(float)verts[numX-1][j].y+1.5f,			(float)verts[numX-1][j].z);					p.vertex((float)verts[numX-1][(j+1)%numZ].x*.9f,(float)verts[numX-1][(j+1)%numZ].y+1.5f,  	(float)verts[numX-1][(j+1)%numZ].z);				p.vertex((float)verts[0][(j+1)%numZ].x*.9f,		(float)verts[0][(j+1)%numZ].y+1.5f, 		(float)verts[0][(j+1)%numZ].z);					p.endShape(PConstants.CLOSE);					
			}//for j
			p.beginShape(PConstants.QUAD);	p.vertex((float)verts[0][3].x*.9f, 		(float)verts[0][3].y+2,		(float)verts[0][3].z);	p.vertex((float)verts[numX-1][3].x*.9f, (float)verts[0][3].y+2,(float)verts[numX-1][3].z);	p.vertex((float)verts[numX-1][3].x*.9f, (float)verts[0][3].y-.5f,(float)verts[numX-1][3].z);		p.vertex((float)verts[0][3].x*.9f,		(float)verts[0][3].y-.5f, 	(float)verts[0][3].z);			p.endShape(PConstants.CLOSE);
			p.beginShape(PConstants.QUAD);	p.vertex((float)verts[0][0].x*.9f, 	(float)verts[0][0].y-.5f, 	(float)verts[0][0].z-1);				p.vertex((float)verts[0][0].x*.9f, 	(float)verts[0][0].y+2.5f,	(float)verts[0][0].z-1);				p.vertex((float)verts[0][0].x*.9f,	(float)verts[0][0].y+2,   	(float)verts[0][1].z-1);				p.vertex((float)verts[0][0].x*.9f,	(float)verts[0][0].y-1, 	(float)verts[0][1].z-1);			p.endShape(PConstants.CLOSE);
			p.beginShape(PConstants.QUAD);	p.vertex((float)verts[numX-1][0].x*.9f, (float)verts[numX-1][0].y-.5f, 	(float)verts[numX-1][0].z-1);				p.vertex((float)verts[numX-1][0].x*.9f, (float)verts[numX-1][0].y+2.5f, (float)verts[numX-1][0].z-1);				p.vertex((float)verts[numX-1][0].x*.9f, (float)verts[numX-1][0].y+2, 	(float)verts[numX-1][1].z-1);				p.vertex((float)verts[numX-1][0].x*.9f, (float)verts[numX-1][0].y-1, 	(float)verts[numX-1][1].z-1);			p.endShape(PConstants.CLOSE);
			p.beginShape(PConstants.QUAD);	p.vertex((float)verts[0][0].x*.9f, 		(float)verts[0][0].y+2.5f,		(float)verts[0][0].z - 1);				p.vertex((float)verts[numX-1][0].x*.9f, (float)verts[numX-1][0].y+2.5f,	(float)verts[numX-1][0].z-1);				p.vertex((float)verts[numX-1][0].x*.9f, (float)verts[numX-1][0].y+2f,	(float)verts[numX-1][1].z-1);				p.vertex((float)verts[0][0].x*.9f,		(float)verts[0][0].y+2f, 		(float)verts[0][1].z-1);			p.endShape(PConstants.CLOSE);					
			p.beginShape(PConstants.QUAD);	p.vertex((float)verts[0][0].x*.9f, 		(float)verts[0][0].y-.5f,		(float)verts[0][0].z - 1);				p.vertex((float)verts[numX-1][0].x*.9f, (float)verts[numX-1][0].y-.5f,(float)verts[numX-1][0].z-1);			p.vertex((float)verts[numX-1][0].x*.9f, (float)verts[numX-1][0].y-1,(float)verts[numX-1][1].z-1);				p.vertex((float)verts[0][0].x*.9f,		(float)verts[0][0].y-1, 	(float)verts[0][1].z-1);			p.endShape(PConstants.CLOSE);	
			p.beginShape(PConstants.QUAD);	p.vertex((float)verts[0][0].x*.9f, 		(float)verts[0][0].y+2.5f,		(float)verts[0][0].z - 1);				p.vertex((float)verts[numX-1][0].x*.9f, (float)verts[numX-1][0].y+2.5f,(float)verts[numX-1][0].z-1);			p.vertex((float)verts[numX-1][0].x*.9f, (float)verts[numX-1][0].y-.5f,(float)verts[numX-1][0].z-1);				p.vertex((float)verts[0][0].x*.9f,		(float)verts[0][0].y-.5f, 	(float)verts[0][0].z-1);				p.endShape(PConstants.CLOSE);	
			p.beginShape(PConstants.QUAD);	p.vertex((float)verts[0][0].x*.9f, (float)verts[0][0].y+2,		(float)verts[0][0].z);	p.vertex((float)verts[numX-1][0].x*.9f, (float)verts[0][0].y+2,(float)verts[numX-1][0].z);	p.vertex((float)verts[numX-1][0].x*.9f, (float)verts[0][0].y-.5f,(float)verts[numX-1][0].z);	p.vertex((float)verts[0][0].x*.9f,		(float)verts[0][0].y-.5f, 	(float)verts[0][0].z);	p.endShape(PConstants.CLOSE);	
			//draw front castle
			for(int j = numZ-4; j < numZ-1; ++j){
				p.beginShape(PConstants.QUAD);				p.vertex((float)verts[0][j].x*.9f, 		(float)verts[0][j].y-.5f, 		(float)verts[0][j].z);				p.vertex((float)verts[0][j].x*.9f, 		(float)verts[0][j].y+.5f,		 (float)verts[0][j].z);								p.vertex((float)verts[0][(j+1)%numZ].x*.9f,			(float)verts[0][(j+1)%numZ].y+.5f, (float)verts[0][(j+1)%numZ].z);					p.vertex((float)verts[0][(j+1)%numZ].x*.9f,			(float)verts[0][(j+1)%numZ].y-.5f, 	 (float)verts[0][(j+1)%numZ].z);				p.endShape(PConstants.CLOSE);
				p.beginShape(PConstants.QUAD);				p.vertex((float)verts[numX-1][j].x*.9f, (float)verts[numX-1][j].y-.5f, 	(float)verts[numX-1][j].z);	p.vertex((float)verts[numX-1][j].x*.9f, (float)verts[numX-1][j].y+.5f, (float)verts[numX-1][j].z);				p.vertex((float)verts[numX-1][(j+1)%numZ].x*.9f, (float)verts[numX-1][(j+1)%numZ].y+.5f,   (float)verts[numX-1][(j+1)%numZ].z);					p.vertex((float)verts[numX-1][(j+1)%numZ].x*.9f, (float)verts[numX-1][(j+1)%numZ].y-.5f, 	 (float)verts[numX-1][(j+1)%numZ].z);					p.endShape(PConstants.CLOSE);
				p.beginShape(PConstants.QUAD);				p.vertex((float)verts[0][j].x*.9f, 		(float)verts[0][j].y+.5f,			(float)verts[0][j].z);		p.vertex((float)verts[numX-1][j].x*.9f, (float)verts[numX-1][j].y+.5f, (float)verts[numX-1][j].z);			p.vertex((float)verts[numX-1][(j+1)%numZ].x*.9f,(float)verts[numX-1][(j+1)%numZ].y+.5f,  	(float)verts[numX-1][(j+1)%numZ].z);				p.vertex((float)verts[0][(j+1)%numZ].x*.9f,		(float)verts[0][(j+1)%numZ].y+.5f, 		(float)verts[0][(j+1)%numZ].z);					p.endShape(PConstants.CLOSE);					
			}//for j
			idx = numZ-1;
			p.beginShape(PConstants.QUAD);		p.vertex((float)verts[0][ idx].x*.9f, 		(float)verts[0][ idx].y-.5f,	(float)verts[0][ idx].z);			p.vertex((float)verts[numX-1][ idx].x*.9f, (float)verts[0][ idx].y-.5f,		(float)verts[0][ idx].z);			p.vertex((float)verts[numX-1][ idx].x*.9f, (float)verts[0][ idx].y+.5f,		(float)verts[0][ idx].z);			p.vertex((float)verts[0][ idx].x*.9f,		(float)verts[0][ idx].y+.5f, 	(float)verts[0][ idx].z);			p.endShape(PConstants.CLOSE);	
			idx = numZ-4;
			p.beginShape(PConstants.QUAD);		p.vertex((float)verts[0][ idx].x*.9f, 		(float)verts[0][idx].y-.5f,	(float)verts[0][ idx].z);			p.vertex((float)verts[numX-1][idx].x*.9f, (float)verts[0][idx].y-.5f,		(float)verts[0][ idx].z);			p.vertex((float)verts[numX-1][idx].x*.9f, (float)verts[0][idx].y+.5f,		(float)verts[0][ idx].z);			p.vertex((float)verts[0][idx].x*.9f,		(float)verts[0][idx].y+.5f, 	(float)verts[0][idx].z);			p.endShape(PConstants.CLOSE);	
			
		p.popMatrix();}	}
	private void finishBodyBottom(int i, int numZ, int numX){
		p.beginShape(PConstants.TRIANGLE);	p.vertex((float)verts[i][numZ-1].x, (float)verts[i][numZ-1].y, 	(float)verts[i][numZ-1].z);	p.vertex(0, 1, numZ-2);	p.vertex((float)verts[(i+1)%numX][numZ-1].x, (float)verts[(i+1)%numX][numZ-1].y, 	(float)verts[(i+1)%numX][numZ-1].z);	p.endShape(PConstants.CLOSE);
		p.beginShape(PConstants.QUAD);p.vertex((float)verts[i][0].x, (float)verts[i][0].y, (float)verts[i][0].z);p.vertex((float)verts[i][0].x * .75f, (float)verts[i][0].y * .75f, (float)verts[i][0].z -.5f);	p.vertex((float)verts[(i+1)%numX][0].x * .75f, (float)verts[(i+1)%numX][0].y * .75f, 	(float)verts[(i+1)%numX][0].z -.5f);p.vertex((float)verts[(i+1)%numX][0].x, (float)verts[(i+1)%numX][0].y, 	(float)verts[(i+1)%numX][0].z );p.endShape(PConstants.CLOSE);
		p.beginShape(PConstants.TRIANGLE);	p.vertex((float)verts[i][0].x * .75f, (float)verts[i][0].y * .75f, (float)verts[i][0].z  -.5f);	p.vertex(0, 0, (float)verts[i][0].z - 1);	p.vertex((float)verts[(i+1)%numX][0].x * .75f, (float)verts[(i+1)%numX][0].y * .75f, 	(float)verts[(i+1)%numX][0].z  -.5f);	p.endShape(PConstants.CLOSE);		
	}

	public myVector[] getCoordsAra() {			return coords;}
	public myVector[] getOrientation() {	return orientation;}
	public double getMass() {				return mass;}
	public myVector getNodeLocs() {			return nodeLocs;}

	public void setCoordsAra(myVector[] coords) {				this.coords = coords;	}
	public void setOrientation(myVector[] orientation) {	this.orientation = orientation;	}
	public void setMass(double mass) {						this.mass = mass;	}
	public void setNodeLocs(myVector _nl){					this.nodeLocs = _nl;}
}//mySailBoat
