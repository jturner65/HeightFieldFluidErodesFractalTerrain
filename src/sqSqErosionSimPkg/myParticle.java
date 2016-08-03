package sqSqErosionSimPkg;

import processing.core.PApplet;
import processing.core.PConstants;

/**
 * a particle to display in the fluid simulator
 * @author John
 *
 */
public class myParticle {
	//owning applet
	private sqSqErosionSimGlobal p;
	//gui interface
	//private myGui gui;
	//stam solver
	private myStamSolver stamEng;
	//unique id for this particle
	public int ID;
	//count of particles in system
	public static int partCount = 0;
	//location of the particle
	private double x, y, z;//oldX, oldY, oldZ;
	//radius of the particle
	private double rad;
	//how sensitive the particle is to fluid motion
	private double mass;
	//this particle's velocity
	private myVector U;

	public myParticle(sqSqErosionSimGlobal _p, myGui _gui, myStamSolver _stamEng, double _x, double _y, double _z, double _rad, double _mass) {
		this.ID = partCount;
		myParticle.partCount++;
		this.p = _p;
		//this.gui = _gui;
		this.stamEng = _stamEng;
		this.x = _x;
		this.y = _y;
		this.z = _z;
		this.rad = _rad;		
		this.mass = _mass;
		this.U = new myVector(0,0,0);
	}
	
	public myParticle(sqSqErosionSimGlobal _p, myGui _gui, myStamSolver _stamEng){
		this(_p, _gui, _stamEng, 0,0,0,0,0);
		float randNum1 = p.random(1);
		float randNum2 = p.random(1);
		this.x = (p.width/2.0f) + (randNum2 * (p.width/3.0f) * PApplet.sin(2 * PConstants.PI * randNum1));
		this.y = (p.height/2.0f) + (randNum2 * (p.height/3.0f) * PApplet.cos(2 * PConstants.PI * randNum1));
		this.rad = stamEng.particleRadius;
		this.mass = stamEng.particleMass;
	}
	
	/**
	 * this will handle verifying a particle's behavior at a boundary defined by 0 and w - bind it to boundary coord if it exceeds boundary
	 * @param x coord
	 * @param w large boundary
	 * @return modified coord value based on boundary
	 */
	public double handleBounds(double x, double w){return ((x < 0) || (x > w) ? ((x<0) ? (x = 0) : (x = w)) : x );}
	
	/**
	 * this will subject this particle to fluid forces
	 */
	public void moveMe(double[] uAra, double[] vAra, double[] wAra){
		double tmpX = this.x, tmpY = this.y, tmpZ = this.z;
//		this.oldX = tmpX;
//		this.oldY = tmpY;
//		this.oldZ = tmpZ;
		//cell location holding this particle
		int cellXLoc = (int) ((tmpX-1)/this.stamEng.cellWidth);
		int cellYLoc = (int) ((tmpY-1)/this.stamEng.cellHeight);
		int cellZLoc = (int) ((tmpY-1)/this.stamEng.cellDepth);
		this.U.x = uAra[this.stamEng.IX(cellXLoc, cellYLoc, cellZLoc)];
		this.U.y = vAra[this.stamEng.IX(cellXLoc, cellYLoc, cellZLoc)];
		this.U.z = wAra[this.stamEng.IX(cellXLoc, cellYLoc, cellZLoc)];
		double mult = this.p.getDeltaT()/this.mass;
		
		tmpX += this.U.x * mult;
		tmpY += this.U.y * mult;
		tmpZ += this.U.z * mult;
		
		tmpX = this.handleBounds(tmpX, this.stamEng.xDim);
		tmpY = this.handleBounds(tmpY, this.stamEng.yDim);
		tmpZ = this.handleBounds(tmpY, this.stamEng.zDim);
		
		this.x = tmpX;
		this.y = tmpY;	
		this.z = tmpZ;
	}//moveMe
	
	public void colorMe(){
		int rColorMod = (int) (Math.abs(this.U.x) * this.stamEng.partVelocityMultX); 
		int gColorMod = (int) (Math.abs(this.U.y) * this.stamEng.partVelocityMultY); 
		int bColorMod = (int) (Math.abs(this.U.z) * this.stamEng.partVelocityMultZ);
		
		this.p.fill( rColorMod, gColorMod, bColorMod);		
	}
	
	public void drawMe(){
		this.p.pushMatrix();
			this.colorMe();
			this.p.translate((float)this.x, (float)this.y, (float) this.z);
			this.p.sphere((float) this.rad);
		this.p.popMatrix();
	}

	public String toString(){
		String result = "Part : " + this.ID + " coords (" + this.x + "," + this.y+ "," + this.z +") r:" + this.rad;
		return result;	
	}
	
}//myParticle
