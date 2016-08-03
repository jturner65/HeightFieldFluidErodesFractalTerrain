package sqSqErosionSimPkg;

/*
 * this class will represent a graphical representation of a voxel
 */

public class myVoxel {
	private sqSqErosionSimGlobal p;
	private myStamSolver stamEng;
//	private myGui gui;
//	private mySqSqEroder eng;
	
	private static int voxCount = 0;
	private int voxCountID;
	//used to determine how blue this is
	private double density;
	private float fdensity;
	private double x,y,z,gridDist;
	private float fx, fy, fz;
//	private myVector coords;
	//dimensions - always a cube so always the same for each dir
	private int xIDX, yIDX, zIDX;
	private float fdim;
	private boolean is3D;
	
	
	public myVoxel(sqSqErosionSimGlobal _p, myGui _gui, mySqSqEroder _eng, myStamSolver _stamEng, 
					double _x, double _y, double _z, 
					int _xIDX, int _yIDX, int _zIDX, 
					int _dim, double _density, boolean _is3D) {
		this.p = _p;
//		this.gui =_gui;
//		this.eng = _eng;
		this.stamEng = _stamEng;
		this.voxCountID = voxCount;
		myVoxel.voxCount++;
		this.x = _x; this.y = _y; this.z = _z;
		this.xIDX = _xIDX; this.yIDX = _yIDX; this.zIDX = _zIDX;
		this.fx = (float)_x; this.fy = (float)_y; this.fz = (float)_z;
		//this.dim = _dim;
		this.fdim = (float) _dim;
		this.density = _density;
		this.fdensity =(float) _density;
		this.is3D = _is3D;
		
	}//constructor
	
	public void setStamSolver(myStamSolver _stamEng){	this.stamEng = _stamEng;}

	public void setDensity(double _density){
		this.density = _density;
		this.fdensity = (float)_density;
	}//setdensity

	public void setDensityFromSolver(){
		if (this.is3D){		this.density = this.stamEng.getDensity(this.xIDX,this.yIDX, this.zIDX);}
		else {				this.density = this.stamEng.getDensity(this.xIDX, this.zIDX);	}//2d is on x-z plane, with y == 0			
		this.fdensity = (float)this.density;
		
	}//getDensityFromsolver
	
	public void drawMe(){
		p.pushMatrix();
			//p.stroke(255);
			p.noStroke();
			this.setDensityFromSolver();
			if (this.density < myConsts.epsValDisplay){ 		p.fill(0,0,0,2f); }//effectively empty cell
			else {									p.fill((float) p.mapD(this.xIDX, 0, this.stamEng.getNumCells()+2, 0, 255), 
															(float) p.mapD(this.yIDX, 0, this.stamEng.getNumCells()+2, 0, 255), 
															(float) p.mapD(this.zIDX, 0, this.stamEng.getNumCells()+2, 0, 255),  
															(float) p.mapD(this.density, 0,  this.stamEng.maxDensity, 100, 255));	}

			p.translate(this.fx + this.fdim/2, this.fy + this.fdim/2, this.fz + this.fdim/2);
			p.box(this.fdim);	
//			p.line(0,0,0, (float) (this.p.getDeltaT() * stamEng.getUVal(this.xIDX,this.yIDX, this.zIDX)),
//						  (float) (this.p.getDeltaT() * stamEng.getVVal(this.xIDX,this.yIDX, this.zIDX)),
//						  (float) (this.p.getDeltaT() * stamEng.getWVal(this.xIDX,this.yIDX, this.zIDX)));
			//this.setDensity(0);				//clear out density for this voxel
		p.popMatrix();
		
	}//drawme
	
	public int getID(){return this.voxCountID;}
	public float getFdensity() {return fdensity;}
	public double getX() {return x;}
	public double getY() {return y;}
	public double getZ() {return z;}
	public double getGridDist() {return gridDist;}

	public void setFdensity(float fdensity) {this.fdensity = fdensity;}
	public void setX(double x) {this.x = x;}
	public void setY(double y) {this.y = y;}
	public void setZ(double z) {this.z = z;}
	public void setGridDist(double gridDist) {this.gridDist = gridDist;}
	
}//myvoxel
