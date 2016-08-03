package sqSqErosionSimPkg;

import sqSqErosionSimPkg.myParticle;

public class myStamSolver {
	private sqSqErosionSimGlobal p;
//	private myGui gui;
//	private mySqSqEroder eng;
	// boolean flags used to control various elements of the program
	private boolean[] stamFlags;
	// number of flags in the boolean flags array
	private int numFlags = 1;

	// whether to display particles
	private final int dispParticles = 0;
	//number of iterations for the solver to use
	private final int globIter = 20;
	
	//number of cells in the solver grid, currently should be equal
	private int numCells;
	//size of square cell in pixels
	private int cellSize;
	
	//idxs corresponding to current cycle and old cycle
	private final int oldIDX = 0;
	private final int nowIDX = 1;
	//array holding old and current density values
	private double[][] densityAra;
	//array holding old and current x-direction velocities
	private double[][] uAra;
	//array holding old and current y-direction velocities
	private double[][] vAra;
	//array holding old and current z-direction velocities
	private double[][] wAra;
	//treating 1d arrays as 2 d, so this is the total number of cells == max number of array elements
	private int araSize;
	
	public double maxDensity;	//max velocity and density of fluid in sim
	
	//array holding x coord of centers of cells
	private int[] cellCentersX;
	//array holding x coord of centers of cells
	private int[] cellCentersY;
	//array holding z coord of centers of cells
	private int[] cellCentersZ;
	//array holding particles that will be represented in fluid
	public myParticle[] partAra;
	//radius of drawn particles
	public final double particleRadius = 3;
	//how much mass each particle has - relates to how responsive they are to fluid forces
	public float particleMass;
	//diffusion rate for fluid
	public final double diffRate = 0.001;
	//viscosity value for stam solver - use 0 by default
	public double visc = 0;// .00001;
	//number of particles in scene
	public final int numParticles = 500;
	//multipliers to change the color of a particle as it moves
	public final double partVelocityMultX = 5000;
	public final double partVelocityMultY = 5000;
	public final double partVelocityMultZ = 5000;
	//multiplier for density to fill color value
	public double densityColorMult;
	//for density calculation
	public double cellWidth;
	public double cellHeight;
	public double cellDepth;
	//dimensions of solver space, need to be equal
	public int xDim, yDim, zDim, dim;	
	//grid holding voxels
	public myVoxel[] voxelGrid;
	//whether or not this is a 3d solver
	private boolean is3D;

	public myStamSolver(sqSqErosionSimGlobal _p, myGui _gui, mySqSqEroder _eng, int _dim, myVoxel[] _voxelGrid, boolean _is3D) {
		this.p = _p;
//		this.gui = _gui;
//		this.eng = _eng;
		this.cellSize = p.stamCellSize;//how big are the individual cells
		this.xDim = _dim;
		this.yDim = _dim;
		this.zDim = _dim;
		this.dim = _dim;
		this.numCells = _dim - 2;
		this.is3D = _is3D;
		this.araSize = (this.numCells + 2) * (this.numCells + 2) * ((this.is3D) ? (this.numCells + 2) : (1));//number of cells in volume
		this.cellCentersX 	= new int[this.araSize];
		this.cellCentersY 	= new int[this.araSize];
		this.cellCentersZ 	= new int[this.araSize];
		this.voxelGrid = _voxelGrid;
		this.reInitAras();
		//right now use same-size cubical cells
		this.cellWidth = this.cellSize;
		this.cellHeight = this.cellSize;
		this.cellDepth =  this.cellSize;
		
		this.stamFlags = new boolean[this.numFlags];
		this.stamFlags[dispParticles] = true;		
		this.densityColorMult = 40;
	}//solver constructor
	
	public void reInitAras(){
		this.densityAra 	= new double[2][this.araSize];
		this.uAra 			= new double[2][this.araSize];
		this.vAra 			= new double[2][this.araSize];
		this.wAra 			= new double[2][this.araSize];
		for (int idx = 0; idx < 2; ++idx){
			this.densityAra[idx] 	= new double[this.araSize];
			this.uAra[idx] 			= new double[this.araSize];
			this.vAra[idx] 			= new double[this.araSize];
			this.wAra[idx] 			= new double[this.araSize];
			for (int elemIDX = 0; elemIDX <this.araSize; ++elemIDX){
				this.densityAra[idx][elemIDX] 	= 0;
				this.uAra[idx][elemIDX] 		= 0;
				this.vAra[idx][elemIDX] 		= 0;
				this.wAra[idx][elemIDX] 		= 0;
			}//for elemIDX
		}//init each sub array
		if(this.voxelGrid != null){
			for(int idx = 0; idx < this.voxelGrid.length; ++idx){
				this.voxelGrid[idx].setDensity(0);
			}
		}
	}//reinitAras
	/**
	 * calls a single cycle of the stam fluid solver
	 */
	public void calcStamCycle(){
		//this.queryUI(this.densityAra[oldIDX], this.uAra[oldIDX], this.vAra[oldIDX], this.wAra[oldIDX]);
		this.calcVelocityStep(this.uAra, this.vAra, this.wAra);//passing new and old arrays of all 3 dirs
		this.maxDensity = 0;
		this.calcDensityStep(this.densityAra, this.uAra[nowIDX], this.vAra[nowIDX], this.wAra[nowIDX]);
	}//calcStamCycle
	
	/**
	 * adds a very dense cell at the center of the mesh if 3D
	 */
	private void impulseDensityOrigin3D(int offset){
		this.densityAra[oldIDX][IX((this.numCells/2) + offset, (this.numCells/2) + offset, (this.numCells/2) + offset)] += 50;
	}//adding a density, make sure to track max density

	/**
	 * adds a very dense cell at the center of the mesh if 2D
	 */
	private void impulseDensityOrigin2D(int offset){
		this.densityAra[oldIDX][IX((this.numCells/2) + offset, (this.numCells/2) + offset)] += 10;
	}//add a density in 2d	
	
	/**
	 * adds an initial velocity and density for testing purposes
	 * @param counter the value driving the introduction of values
	 */
	public void addInitVelAndDensity(){
		int counter = this.numCells/5;
		if (this.is3D){
			this.addVelocityOverTime(counter, counter, counter,.0001*Math.random(),  .0001*Math.random(), .0001*Math.random());
			this.impulseDensityOrigin3D((counter/4));
		} else {
			this.addVelocityOverTime(counter, counter, 0,0.000000000000001);
			this.impulseDensityOrigin2D(0);		
		}//if 3d or not
		
	}//addInitVelAndDensity
	
	/**
	 * adds a velocity field of a particular magnitude at the passed idexes location for 3D grid
	 */
	private void addVelocityOverTime(int xIdx, int yIdx, int zIdx, double uVel, double vVel, double wVel){
		int IDX1 = IX(xIdx,     yIdx,     zIdx);
		int IDX2 = IX(xIdx + 1, yIdx - 1, zIdx + 1);
		int IDX3 = IX(xIdx - 1, yIdx + 1, zIdx + 1);
		int IDX4 = IX(xIdx + 1, yIdx - 1, zIdx - 1);
		int IDX5 = IX(xIdx - 1, yIdx + 1, zIdx - 1);
		
		this.uAra[oldIDX][IDX1] = uVel;
		this.uAra[oldIDX][IDX2] = uVel;
		this.uAra[oldIDX][IDX3] = uVel;
		this.uAra[oldIDX][IDX4] = uVel;
		this.uAra[oldIDX][IDX5] = uVel;
		
		this.vAra[oldIDX][IDX1] = vVel;
		this.vAra[oldIDX][IDX2] = vVel;
		this.vAra[oldIDX][IDX3] = vVel;
		this.vAra[oldIDX][IDX4] = vVel;
		this.vAra[oldIDX][IDX5] = vVel;
		
		this.wAra[oldIDX][IDX1] = wVel;
		this.wAra[oldIDX][IDX2] = wVel;
		this.wAra[oldIDX][IDX3] = wVel;
		this.wAra[oldIDX][IDX4] = wVel;
		this.wAra[oldIDX][IDX5] = wVel;
	}//add an impulse velocity at a particular location
	
	/**
	 * adds a velocity field of a particular magnitude at the passed idexes location for 2D grid - needs to be x-z
	 */
	public void addVelocityOverTime(int xIdx, int yIdx, double uVel, double wVel){

		int IDX1 = IX(xIdx,   yIdx);
		int IDX2 = IX(xIdx+1, yIdx+1);
		int IDX3 = IX(xIdx-1, yIdx-1);		
		
		this.uAra[oldIDX][IDX1] = uVel;
		this.uAra[oldIDX][IDX2] = uVel ;
		this.uAra[oldIDX][IDX3] = uVel ;

		this.vAra[oldIDX][IDX1] = wVel;
		this.vAra[oldIDX][IDX2] = wVel;
		this.vAra[oldIDX][IDX3] = wVel;
	}//add an impulse velocity at a particular location
	
	/**
	 * adds sources of density (mouseclicks, for example) to the density array
	 * @param dAra density ara
	 * @param sourceAra locations of mouseclicks
	 */
	public void addSource(double[] dAra, double[] sourceAra){ 
		double deltaT = p.getDeltaT();
		for (int i=0 ; i < this.araSize ; i++ ) {
			dAra[i] += deltaT * sourceAra[i]; 
		}//for each element in dAra
	}//addSource
	
	
	/**
	 * this will draw the particles moving around in the fluid
	 */
	public void drawParticles(){
		p.pushMatrix();
			for(myParticle tmpPart : this.partAra){ 
				tmpPart.moveMe(this.uAra[nowIDX], this.vAra[nowIDX], this.wAra[nowIDX]);
				tmpPart.drawMe();
			}
		p.popMatrix();
	}//drawParticles
	
	/**
	 * this will draw a physical representation of the velocity vector at each cell
	 */
	public void drawFlowLines(){
		p.pushMatrix();
			p.stroke(255);
			p.strokeWeight(1);
			double x0, y0,z0, x1, y1,z1;
			p.fill(255);
			for(int i = 1; i < this.numCells; i+=1){
				for(int j = 1; j < this.numCells; j+=1){
					for(int k = 1; k<this.numCells; k+=1){
						x0 = this.cellCentersX[IX(i,j,k)];
						y0 = this.cellCentersY[IX(i,j,k)];
						z0 = this.cellCentersZ[IX(i,j,k)];
						x1 = x0 + (10 * uAra[nowIDX][IX(i,j,k)]*cellSize/p.getDeltaT());
						y1 = y0 + (10 * vAra[nowIDX][IX(i,j,k)]*cellSize/p.getDeltaT());	
						z1 = z0 + (10 * wAra[nowIDX][IX(i,j,k)]*cellSize/p.getDeltaT());	
						//ellipse(x1,y1,1,1);
						p.line((float)x0, (float)y0, (float)z0, (float)x1, (float)y1, (float)z1);
					}
				}//for j
			}//for i	
			p.noStroke();
		p.popMatrix();
	}//drawFlowLines
	

	//jos stam solver functions	
	/**
	 * calculates the diffusion rate using a implicit integeration with gauss seidel relaxation
	 * @param bound boundary variable
	 * @param xAra array of densities/velocities and old densities/velocities
	 * @param c multiplier for s
	 * @param diff diffusion rate
	 * @param dt
	 */
	public void calcDiffusionJS(int bound, double[][] xAra, double diffRate){
		double a = p.getDeltaT() * diffRate * this.numCells * this.numCells * ( (this.is3D) ? ( this.numCells ) : (1));
		if (this.is3D) { 
			this.linearSolver3D(bound, xAra, a, 1 + (6*a)); 
		} else {
			this.linearSolver2D(bound, xAra, a, 1 + (4*a));
	
		}
	}//calcDiffuse

	
	public void linearSolver2D(int bound, double[][] xAra, double a, double c){
		double cInv = 1.0/c;
		for (int iter=0 ; iter < this.globIter ; iter++ ) {
			for (int j = 1 ; j <= this.numCells; ++j ) {
				for(int i = 1; i <= this.numCells; ++i){
					xAra[nowIDX][IX(i,j)] = (xAra[oldIDX][IX(i,j)] + 
								a * (xAra[nowIDX][IX(i-1,j)] + 
									 xAra[nowIDX][IX(i+1,j)] + 
									 xAra[nowIDX][IX(i,j-1)] + 
									 xAra[nowIDX][IX(i,j+1)] + 
									 xAra[nowIDX][IX(i,j)] + 
									 xAra[nowIDX][IX(i,j)])) * cInv;
				}//for i
			}//for j
			setBoundariesJS2D(bound, xAra[nowIDX] );
		}//for k
	
	}//linearSolver2D
	
	/**
	 * solves using an implicit integeration with gauss seidel relaxation for 3D - idea from mike ash web page
	 * @param bound boundary variable
	 * @param xAra array of densities/velocities and old densities/velocities
	 * @param a calculated multiplier
	 * @param c divisor for 
	 */
	public void linearSolver3D(int bound, double[][] xAra, double a, double c){
		double cInv = 1.0/c;
		for (int iter=0 ; iter < this.globIter ; iter++ ) {
			for (int k=1 ; k <= this.numCells; ++k ) {
				for (int j = 1 ; j <= this.numCells; ++j ) {
					for(int i = 1; i <= this.numCells; ++i){
						xAra[nowIDX][IX(i,j,k)] = (xAra[oldIDX][IX(i,j,k)] + 
								a * (xAra[nowIDX][IX(i-1,j,k)] + 
									 xAra[nowIDX][IX(i+1,j,k)] + 
									 xAra[nowIDX][IX(i,j-1,k)] + 
									 xAra[nowIDX][IX(i,j+1,k)] + 
									 xAra[nowIDX][IX(i,j,k-1)] + 
									 xAra[nowIDX][IX(i,j,k+1)])) * cInv;
					}//for k
				}//for j
			}//for i
			setBoundariesJS3D(bound, xAra[nowIDX] );
		}//for iter
	}//linSolver3D
	
	/**
	 * handles boundary conditions for values held in an array for 2D solver
	 * @param bound particular boundary we are handling
	 * @param xAra array of values being processed
	 */
	public void setBoundariesJS2D(int bound, double[] xAra){
		for (int  idx=1 ; idx <= this.numCells  ; ++idx ) {
			xAra[IX(0, idx)] 				= ((bound==1) ? (-xAra[IX(1,idx)]) 				: (xAra[IX(1,idx)]));
			xAra[IX(this.numCells+1, idx)]  = ((bound==1) ? (-xAra[IX(this.numCells,idx)])  : (xAra[IX(this.numCells,idx)]));
			xAra[IX(idx, 0 )] 				= ((bound==2) ? (-xAra[IX(idx,1)]) 				: (xAra[IX(idx,1)]));
			xAra[IX(idx, this.numCells+1)]  = ((bound==2) ? (-xAra[IX(idx,this.numCells)])  : (xAra[IX(idx,this.numCells)]));
		}
		xAra[IX(0, 0 )] 								= 0.5f * (xAra[IX(1,0 )] 							+ xAra[IX(0 ,1)]);
		xAra[IX(0, this.numCells + 1)] 					= 0.5f * (xAra[IX(1,this.numCells + 1)] 			+ xAra[IX(0 ,this.numCells )]);
		xAra[IX(this.numCells + 1, 0)] 					= 0.5f * (xAra[IX(this.numCells, 0 )] 				+ xAra[IX(this.numCells + 1, 1)]);
		xAra[IX(this.numCells + 1, this.numCells + 1)]  = 0.5f * (xAra[IX(this.numCells,this.numCells + 1)]	+ xAra[IX(this.numCells + 1, this.numCells )]);
	}//setboundariesJS2D
		
	/**
	 * handles boundary conditions for values held in an array for 3D solver
	 * @param bound particular boundary we are handling
	 * @param xAra array of values being processed
	 */
	public void setBoundariesJS3D( int bound, double[] xAra ){	
		for (int idx1 = 1 ; idx1 <= this.numCells; ++idx1) {
			for (int idx2 = 1; idx2 <= this.numCells; ++idx2) {
			xAra[IX(0, idx1, idx2)] 				= ((bound==1) ? (-xAra[IX(1,idx1, idx2)]) 				: (xAra[IX(1,idx1, idx2)]));
			xAra[IX(this.numCells+1, idx1, idx2)] 	= ((bound==1) ? (-xAra[IX(this.numCells,idx1, idx2)]) 	: (xAra[IX(this.numCells,idx1, idx2)]));
			xAra[IX(idx1, 0, idx2 )] 				= ((bound==2) ? (-xAra[IX(idx1,1, idx2)]) 				: (xAra[IX(idx1,1, idx2)]));
			xAra[IX(idx1, this.numCells+1, idx2)] 	= ((bound==2) ? (-xAra[IX(idx1,this.numCells, idx2)]) 	: (xAra[IX(idx1,this.numCells, idx2)]));
			xAra[IX(idx1, idx2, 0 )] 				= ((bound==3) ? (-xAra[IX(idx1, idx2, 1 )] ) 			: (xAra[IX(idx1, idx2, 1 )] ));
			xAra[IX(idx1, idx2, this.numCells+1)] 	= ((bound==3) ? (-xAra[IX(idx1, idx2, this.numCells)]) : (xAra[IX(idx1, idx2, this.numCells)]));
			}//for idx2
		}//for idx1
		//corners
		xAra[IX(0,0,0)] 						= (1/3) * (xAra[IX(1,0,0)] + 
														   xAra[IX(0,1,0)] + 
														   xAra[IX(0,0,1)]);
		xAra[IX(0,0,this.numCells + 1)]			= (1/3) * (xAra[IX(1,0,this.numCells+1)] + 
														   xAra[IX(0,1,this.numCells+1)] + 
														   xAra[IX(0,0,this.numCells)]);		
		
		xAra[IX(0, this.numCells + 1, 0)] 					=  (1/3) * (xAra[IX(1, this.numCells+1,	0)] +
																		xAra[IX(0, this.numCells,		0)] + 
																		xAra[IX(0, this.numCells+1,	1)]);		
		xAra[IX(0, this.numCells + 1, this.numCells + 1)] 	=  (1/3) * (xAra[IX(1, this.numCells+1, 	this.numCells + 1)] +
																		xAra[IX(0, this.numCells, 		this.numCells + 1)] +
																		xAra[IX(0, this.numCells+1, 	this.numCells)]);		
		
		xAra[IX(this.numCells+1,0,0)] 						= (1/3) * (xAra[IX(this.numCells,0,0)] + 
																		xAra[IX(this.numCells+1,1,0)] + 
																		xAra[IX(this.numCells+1,0,1)]);
		xAra[IX(this.numCells+1,0,this.numCells + 1)]		= (1/3) * (xAra[IX(this.numCells,0,this.numCells+1)] + 
														   				xAra[IX(this.numCells+1,1,this.numCells+1)] + 
														   				xAra[IX(this.numCells+1,0,this.numCells)]);		
		
		xAra[IX(this.numCells+1, this.numCells + 1, 0)] 					=  (1/3) * (xAra[IX(this.numCells, this.numCells+1,		0)] +
																						xAra[IX(this.numCells+1, this.numCells,		0)] + 
																						xAra[IX(this.numCells+1, this.numCells+1,	1)]);		
		xAra[IX(this.numCells+1, this.numCells + 1, this.numCells + 1)] 	=  (1/3) * (xAra[IX(this.numCells, this.numCells+1, 	this.numCells + 1)] +
																						xAra[IX(this.numCells+1, this.numCells, 	this.numCells + 1)] +
																						xAra[IX(this.numCells+1, this.numCells+1, 	this.numCells)]);
	

	}//setBoundaries
	
	/**
	 * calculates the advection step first order method for 3D
	 * @param bound boundary conditions
	 * @param densities array of densities at t +dt
	 * @param oldDensities array of densities at t
	 * @param uAra velocity array being applied in x dir
	 * @param vAra velocity array being applied in y dir
	 * 
	 * see also back and forth : bfecc error correction byungmoon kim
	 * see also vorticity confinement : ron fedkin
	 */	
	public void calcAdvectionJS2D(int bound, double[][] densities, double[] uAra, double[] vAra, boolean isDensity){
		int i0, j0, i1, j1;
		double x, y, 
		//z, 
		s0, t0, s1, t1,  deltaT0;
		deltaT0 = p.getDeltaT() *  this.numCells;
		
		for (int j = 1 ; j <= this.numCells ; ++j ) {
			for (int i = 1; i < this.numCells; ++i){
				x = i - (deltaT0 * uAra[IX(i,j)]); 
				y = j - (deltaT0 * vAra[IX(i,j)]);

			
			//center of cells
			if (x < 0.5f) { x = 0.5;} 
			if (x > this.numCells + 0.5) {	x = this.numCells + 0.5f;} 
			i0 = (int)x; i1 = i0 + 1;
			
			if (y<0.5) { y = 0.5;} 
			if (y > this.numCells + 0.5){y = this.numCells + 0.5;}
			j0=(int)y; j1=j0+1;
			
			s1 = x-i0; 
			s0 = 1-s1; 
			t1 = y-j0; 				
			t0 = 1-t1;

			
			densities[nowIDX][IX(i,j)] = (s0 * (t0 * densities[oldIDX][IX(i0,j0)] 
											  + t1 * densities[oldIDX][IX(i0,j1)])) 
										+(s1 * (t0 * densities[oldIDX][IX(i1,j0)] 
											  + t1 * densities[oldIDX][IX(i1,j1)]));
			
			if(isDensity){this.maxDensity = (this.maxDensity > densities[nowIDX][IX(i,j)]) ? (this.maxDensity ) : (densities[nowIDX][IX(i,j)]);}

			}//for i
		}//for j

		this.setBoundariesJS2D(bound, densities[nowIDX]);
	}//calcAdvection	
	
	/**
	 * calculates the advection step first order method for 3D
	 * @param bound boundary conditions
	 * @param densities array of densities at t +dt
	 * @param oldDensities array of densities at t
	 * @param uAra velocity array being applied in x dir
	 * @param vAra velocity array being applied in y dir
	 * 
	 * see also back and forth : bfecc error correction byungmoon kim
	 * see also vorticity confinement : ron fedkin
	 */	
	public void calcAdvectionJS3D(int bound, double[][] densities, double[] uAra, double[] vAra, double[] wAra, boolean isDensity){
		int i0, j0, k0, i1, j1, k1;
		double x, y, z, s0, t0, q0, s1, t1, q1, deltaT0;
		deltaT0 = p.getDeltaT() *  this.numCells;
		
		for (int k = 1 ; k <= this.numCells ; ++k) {
			for (int j = 1 ; j <= this.numCells ; ++j ) {
				for (int i = 1; i < this.numCells; ++i){
					x = i - (deltaT0 * uAra[IX(i,j,k)]); 
					y = j - (deltaT0 * vAra[IX(i,j,k)]);
					z = k - (deltaT0 * wAra[IX(i,j,k)]);
				
				//center of cells
				if (x < 0.5f) { x = 0.5;} 
				if (x > this.numCells + 0.5) {	x = this.numCells + 0.5f;} 
				i0 = (int)x; i1 = i0 + 1;
				
				if (y<0.5) { y = 0.5;} 
				if (y > this.numCells + 0.5){y = this.numCells + 0.5;}
				j0=(int)y; j1=j0+1;
				
				if (z<0.5) { z = 0.5;} 
				if (z > this.numCells + 0.5){z = this.numCells + 0.5;}
				k0=(int)z; k1=k0+1;
				
				s1 = x-i0; 
				s0 = 1-s1; 
				t1 = y-j0; 				
				t0 = 1-t1;
				q1 = z-k0; 				
				q0 = 1-q1;
				
				densities[nowIDX][IX(i,j,k)] = (s0 * (t0 * (q0 * densities[oldIDX][IX(i0,j0,k0)] 
														  + q1 * densities[oldIDX][IX(i0,j0,k1)])) 
												    +(t1 * (q0 * densities[oldIDX][IX(i0,j1,k0)] 
												    	  + q1 * densities[oldIDX][IX(i0,j1,k1)]))) + 								   
											   (s1 * (t0 * (q0 * densities[oldIDX][IX(i1,j0,k0)] 
													   	  + q1 * densities[oldIDX][IX(i1,j0,k1)]))
												   + (t1 * (q0 * densities[oldIDX][IX(i1,j1,k0)] 
														  + q1 * densities[oldIDX][IX(i1,j1,k1)])));
				
				if(isDensity){this.maxDensity = (this.maxDensity > densities[nowIDX][IX(i,j,k)]) ? (this.maxDensity ) : (densities[nowIDX][IX(i,j,k)]);}
				}//for k
			}//for j
		}//for i
		this.setBoundariesJS3D(bound, densities[nowIDX]);
	}//calcAdvection

	/**
	 * calculates the appropriate pathways for the density through the velocity field
	 * @param xAra densities - old and current
	 * @param uAra velocity ara in x
	 * @param vAra velocity ara in y
	 * @param wAra velocity ara in z
	 */	
	public void calcDensityStep (double[][] xAra, double[] uAra, double[] vAra, double[] wAra){
		this.addSource (xAra[nowIDX], xAra[oldIDX]);//adds source from old into now
		int bound = 0;
		//SWAP ( x0, x1 - diffuse old into new )
			double[] tmpAra = xAra[oldIDX];
			xAra[oldIDX] = xAra[nowIDX];
			xAra[nowIDX] = tmpAra;
		this.calcDiffusionJS(bound, xAra, this.diffRate);
		
		//SWAP ( x0, x - advect new, move into old);
			tmpAra = xAra[oldIDX];
			xAra[oldIDX] = xAra[nowIDX];
			xAra[nowIDX] = tmpAra;
		if (this.is3D){this.calcAdvectionJS3D(bound, xAra, uAra, vAra, wAra, true);}
		else {this.calcAdvectionJS2D(bound, xAra, uAra, vAra, true);}
	}//densityCalculationStep
	
	/**
	 * velocity solver
	 * @param uAra x dir velocity
	 * @param vAra y dir velocity
	 * @param uOldAra old velocity x
	 * @param vOldAra old velocity y
	 * @param visc viscosity
	 */
	public void calcVelocityStep(double[][] uAra, double[][] vAra, double[][] wAra){
		this.addSource( uAra[nowIDX],uAra[oldIDX]); 
		this.addSource( vAra[nowIDX],vAra[oldIDX]); 
		this.addSource( wAra[nowIDX],wAra[oldIDX]); 
		
		//SWAP ( uOldAra, uAra ); 
			double[] tmp = uAra[oldIDX];
			uAra[oldIDX] = uAra[nowIDX];
			uAra[nowIDX] = tmp;			
		calcDiffusionJS(1, uAra, this.visc);
		
		//swap (vAra, vOldAra)
			tmp = vAra[oldIDX];
			vAra[oldIDX] = vAra[nowIDX];
			vAra[nowIDX] = tmp;			
		calcDiffusionJS(2, vAra, this.visc);
		
		if (this.is3D){
			//SWAP ( wOldAra, wAra ); only if 3d 
				tmp = wAra[oldIDX];
				wAra[oldIDX] = wAra[nowIDX];
				wAra[nowIDX] = tmp;			
			calcDiffusionJS(3, wAra, this.visc);
		}
		
		if(this.is3D) {this.project3D(uAra[nowIDX], vAra[nowIDX], wAra[nowIDX], uAra[oldIDX], vAra[oldIDX]);}
		else {		   this.project2D(uAra[nowIDX], vAra[nowIDX], uAra[oldIDX], vAra[oldIDX]);}
		//swap again
		tmp = uAra[oldIDX];
		uAra[oldIDX] = uAra[nowIDX];
		uAra[nowIDX] = tmp;			

		tmp = vAra[oldIDX];
		vAra[oldIDX] = vAra[nowIDX];
		vAra[nowIDX] = tmp;			

		if (this.is3D){
			//SWAP ( wOldAra, wAra ); only if 3d 
				tmp = wAra[oldIDX];
				wAra[oldIDX] = wAra[nowIDX];
				wAra[nowIDX] = tmp;			
		}

		if (this.is3D){
			this.calcAdvectionJS3D(1, uAra, uAra[oldIDX], vAra[oldIDX], wAra[oldIDX], false); 
			this.calcAdvectionJS3D(2, vAra, uAra[oldIDX], vAra[oldIDX], wAra[oldIDX], false); 
			this.calcAdvectionJS3D(3, wAra, uAra[oldIDX], vAra[oldIDX], wAra[oldIDX], false); 
		} else {
			this.calcAdvectionJS2D(1, uAra, uAra[oldIDX], vAra[oldIDX], false); 
			this.calcAdvectionJS2D(2, vAra, uAra[oldIDX], vAra[oldIDX], false); 			
		}
		//call project again to calculate pressures properly		
		if(this.is3D) {	this.project3D(uAra[nowIDX], vAra[nowIDX], wAra[nowIDX], uAra[oldIDX], vAra[oldIDX]);}
		else {			this.project2D(uAra[nowIDX], vAra[nowIDX], uAra[oldIDX], vAra[oldIDX]);}
	}//velocityCalcStep

	/**
	 * process poisson calculation for preserving mass pressure projection
	 * @param uAra velocity field in x
	 * @param vAra velocity field in y
	 * @param wAra velocity field in w
	 * @param pressure mass conserving field
	 * @param div gradient
	 * 
	 * better to use conjugate gradient
	 */	
	public void project2D(double[] uAra, double[] vAra, double[] pressure, double[] div){
		double height = 1.0 / this.numCells;
		for (int j = 1; j <= this.numCells; ++j) {
			for (int i = 1; i < this.numCells; ++i){
				div[IX(i,j)] = -0.5 * height * (uAra[IX(i+1,j)]
												 -uAra[IX(i-1,j)] 
											 	 +vAra[IX(i,j+1)] 
											     -vAra[IX(i,j-1)]); 
				pressure[IX(i,j)] = 0;
			}//for i
		}//for j
		this.setBoundariesJS2D( 0, div ); 
		this.setBoundariesJS2D( 0, pressure );
		double[][] tmpDoubleAra = {div, pressure};
		this.linearSolver2D(0, tmpDoubleAra, 1.0, 4.0);
		

		for (int j = 1 ; j <= this.numCells ; ++j ) {
			for (int i = 1 ; i <= this.numCells ; ++i ) {
				uAra[IX(i,j)] -= 0.5 * (pressure[IX(i+1,j)] - pressure[IX(i-1,j)])/height;
				vAra[IX(i,j)] -= 0.5 * (pressure[IX(i,j+1)] - pressure[IX(i,j-1)])/height;
			}//for i
		}//for j
		this.setBoundariesJS2D(1, uAra); 
		this.setBoundariesJS2D(2, vAra);
	}//project	2D	 
	 
	/**
	 * process poisson calculation for preserving mass pressure projection for 3D solver
	 * @param uAra velocity field in x
	 * @param vAra velocity field in y
	 * @param wAra velocity field in w
	 * @param pressure mass conserving field
	 * @param div gradient
	 * 
	 * better to use conjugate gradient
	 */	
	public void project3D(double[] uAra, double[] vAra, double[] wAra, double[] pressure, double[] div){
		double height = 1.0 / this.numCells;
		for (int k = 1; k <= this.numCells; ++k ) {
			for (int j = 1; j <= this.numCells; ++j) {
				for (int i = 1; i < this.numCells; ++i){
					div[IX(i,j,k)] = -0.5 * height * (uAra[IX(i+1,j,k)]
													 -uAra[IX(i-1,j,k)] 
												 	 +vAra[IX(i,j+1,k)] 
												     -vAra[IX(i,j-1,k)] 
												     +wAra[IX(i,j,k+1)] 
												     -wAra[IX(i,j,k-1)]);
					pressure[IX(i,j,k)] = 0;
				}//for i
			}//for j
		}//for k
		this.setBoundariesJS3D( 0, div ); 
		this.setBoundariesJS3D( 0, pressure );
		double[][] tmpDoubleAra = {div, pressure};
		this.linearSolver3D(0, tmpDoubleAra, 1.0, 6.0);
		

		for (int k = 1; k <= this.numCells ; ++k) {		
			for (int j = 1 ; j <= this.numCells ; ++j ) {
				for (int i = 1 ; i <= this.numCells ; ++i ) {
					uAra[IX(i,j,k)] -= 0.5 * (pressure[IX(i+1,j,k)] - pressure[IX(i-1,j,k)])/height;
					vAra[IX(i,j,k)] -= 0.5 * (pressure[IX(i,j+1,k)] - pressure[IX(i,j-1,k)])/height;
					wAra[IX(i,j,k)] -= 0.5 * (pressure[IX(i,j,k+1)] - pressure[IX(i,j,k-1)])/height;
				}//for k
			}//for i
		}//for j
		this.setBoundariesJS3D(1, uAra); 
		this.setBoundariesJS3D(2, vAra);
		this.setBoundariesJS3D(3, wAra);
	}//project	3D
	
	
	/**
	 * stam solver index interpreter for 3 d - takes x, y, z indices for 3 d array, returns index into 1 d array
	 * @param i x coord
	 * @param j y coord
	 * @param k z coord
	 * @return index into 1 d array
	 */
	public int IX(int i, int j, int k){ return  i + (( this.numCells + 2 ) * j) + (((this.numCells + 2) * (this.numCells + 2)) * k);}
	
	/**
	 * stam solver index interpreter for 2 d - takes x, y indices for 2 d array, returns index into 1 d array
	 * @param i x coord
	 * @param j y coord
	 * @return index into 1 d array
	 */
	public int IX(int i, int j){ return  i + (( this.numCells + 2 ) * j) ;}

	public double getDensity(int x, int y, int z){ return this.densityAra[nowIDX][IX(x,y,z)] / this.densityColorMult;}
	public double getDensity(int x, int y){ return this.densityAra[nowIDX][IX(x,y)] / this.densityColorMult;}
	public double getUVal(int x, int y, int z){ return this.uAra[nowIDX][IX(x,y,z)];}
	public double getVVal(int x, int y, int z){ return this.vAra[nowIDX][IX(x,y,z)];}
	public double getWVal(int x, int y, int z){ return this.wAra[nowIDX][IX(x,y,z)];}
	public int getAraSize(){return this.araSize;}
	
	public boolean getIs3D() {return this.is3D;	}
	public int getDim() {return this.dim;}
	public void setIs3D(boolean is3d) {is3D = is3d;	}
	public int getNumCells() {return numCells;}
	public void setNumCells(int numCells) {	this.numCells = numCells;}

	public myVoxel[] getVoxelGrid(){return this.voxelGrid;}
	public void setVoxelGrid(myVoxel[] _voxelGrid){this.voxelGrid = _voxelGrid;}
	
	
}//stam GDC talk-based fluid solver