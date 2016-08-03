package sqSqErosionSimPkg;
import matlabcontrol.*;
import matlabcontrol.extensions.*;


public class myFEMSolver {
	private sqSqErosionSimGlobal p;
	private myGui g;
	private MatlabProxy proxy;
	private MatlabTypeConverter processor;
	private double[][] pts;
	private double [][] tri;		//triangle array of idxs of pts array - 1 3-element array per triangle
	public double[][] ht;			//per triangle "ht" value - avg frc per triangle
	private double[][] bnd;			//idx's of bndry verts
	public double[][] bndHt;		//ht vals of bnd nodes
	private double[][] nBnd;		//neumann bounds - del u = 0 - this should be idx's of neighbor verts to use for bnd vert vals
	private boolean valsSet;
	private double [][] Ures;
	private double [][] ExtrVal;
	private boolean useMatlab;		//whether to solve in java or in matlab
	private double [][] bndFunc;
	
	public myFEMSolver(myGui _g, sqSqErosionSimGlobal _p) throws MatlabConnectionException {
		this.g = _g;
		this.p = _p;
		this.valsSet = false;
		MatlabProxyFactoryOptions options =
           new MatlabProxyFactoryOptions.Builder()
               .setUsePreviouslyControlledSession(true)
               .setMatlabLocation(null)
               .build();
		MatlabProxyFactory factory = new MatlabProxyFactory(options);
		proxy = factory.getProxy();		
		processor = new MatlabTypeConverter(proxy);
		useMatlab = true;
	}//cnstrctr

	public boolean initFEMVals(int hMapAraX, int hMapAraZ, mySqSqEroder eng){
		pts =  new double[hMapAraX * hMapAraZ][2];
		ht =  new double[(hMapAraX-1) * (hMapAraZ-1) * 2][1];	//1 per triangle sent to fem sim
		tri = new double[(hMapAraX-1) * (hMapAraZ-1) * 2][3];
		bnd = new double[(2*hMapAraX) + (2 * (hMapAraZ-2))][1];		
		bndHt = new double[(2*hMapAraX) + (2 * (hMapAraZ-2))][1];
		nBnd = new double[(2*hMapAraX) + (2 * (hMapAraZ-2))][1];			//neumann boundary "neigbor" node idx's (use to get values for bnd nodes)
		bndFunc = new double[1][1];
		bndFunc[0][0] = 1;
		//triangularize mesh, set list of verts in mesh, and boundary and triangle verts
		int matIDX, nMatIDX, triIDX, triM1, triM2, triM3, triM4;
		int bndIDX = 0, znOff = 0, xnOff = 0;
		for(int xIDX = 0; xIDX < hMapAraX; ++xIDX){
			for(int zIDX = 0; zIDX < hMapAraZ; ++zIDX){//copy border x coords
				matIDX = (zIDX*hMapAraX) + xIDX;
				//for neumann boundary nodes, want idx that is 1 place "inside" mesh from bndIDX				
				if((0 == zIDX) || (0 == xIDX) || (hMapAraX-1 == xIDX) || (hMapAraZ-1 == zIDX)){
					znOff = (zIDX == 0) ? 1 : (zIDX == hMapAraZ-1) ? hMapAraZ-2 : zIDX;
					xnOff = (xIDX == 0) ? 1 : (xIDX == hMapAraX-1) ? hMapAraX-2 : xIDX;
					nMatIDX = (znOff * hMapAraX) + xnOff;
					nBnd[bndIDX][0] = nMatIDX + 1;						//idx of neighbor to boundary node, for neumann boundaries
					bnd[bndIDX++][0] = matIDX+1;						//add 1 for matlab 1-idxing - this is the matlab idx of the bnd vertex						
				}
				pts[matIDX][0] = eng.heightMapAra[xIDX][zIDX].source.coords.x;
				pts[matIDX][1] = eng.heightMapAra[xIDX][zIDX].source.coords.z;
				//array of triangles referencing idx's - 1 fewer poly than node in both x and z dir
				if((xIDX < hMapAraX-1) && (zIDX < hMapAraZ-1)){
					triIDX = ((zIDX*(hMapAraX-1)) + xIDX) * 2;
					triM1 = (zIDX*hMapAraX) + xIDX; 
					triM2 = triM1 + 1;
					triM4 = ((zIDX+1)*hMapAraX) + xIDX;
					triM3 = triM4 + 1;
					//add 1 to each value because of matlab 1-major idxing
					tri[triIDX][0] = triM1+1;
					tri[triIDX][1] = triM2+1;
					tri[triIDX][2] = triM3+1;

					tri[triIDX+1][0] = triM1+1;					
					tri[triIDX+1][1] = triM3+1;
					tri[triIDX+1][2] = triM4+1;
				}				
			}//for each z
		}//for each x	
		return true;
	}//initFemVals
	
	public void setGui(myGui _g){		this.g = _g;	}
	
	//send currently set values in matlab runner to matlab
	public void setMatlabEnvVals() throws MatlabInvocationException {
		p.resetGMinMaxAra(myConsts.FEMval);	
		processor.setNumericArray("pts", new MatlabNumericArray(this.pts, null));
	    processor.setNumericArray("tri", new MatlabNumericArray(this.tri, null));		
	    processor.setNumericArray("ht", new MatlabNumericArray(this.ht, null));				//per triangle "ht" value - avg frc per triangle
	    processor.setNumericArray("bnd", new MatlabNumericArray(this.bnd, null));			
	    processor.setNumericArray("bndHt", new MatlabNumericArray(this.bndHt, null));			
	    processor.setNumericArray("nBnd", new MatlabNumericArray(this.nBnd, null));			
	    processor.setNumericArray("bndFunc", new MatlabNumericArray(this.bndFunc, null));	
	}//setMatlabEnvVals
	
	//calculate locally TODO:
	public double[][] callFEMLclFunc() {
		if(valsSet){ 
			//call local calculation engine, setvalues
		} else {
			g.print2Cnsl("Vals not set from mesh");          	   
		}	
	   
		p.setGlobalVals(myConsts.FEMval, this.ExtrVal[0][0]);
		p.setGlobalVals(myConsts.FEMval, this.ExtrVal[1][0]);			
		// reinitializes global min/max aras for passed value for index			p.
		return this.Ures;
	}//callFEMFunc
	
	public double[][] callFEMFunc() throws MatlabInvocationException  {
		if(!useMatlab){return callFEMLclFunc();}
		if(valsSet){ 
			//proxy.eval("[Ures,ExtrVal]=femcodeJTJava(pts, tri, bnd, bndHt, ht, nBnd, 0, 0);");			//0 : dirch; variables preset in matlab environment
			proxy.eval("[Ures,ExtrVal]=femcodeJTJava(pts, tri, bnd, bndHt, ht, nBnd, 0, 1);");			//1 : neumann; variables preset in matlab environment
		} else {
			g.print2Cnsl("Vals not set from mesh");          	   
		}	
		this.Ures = processor.getNumericArray("Ures").getRealArray2D();
		this.ExtrVal = processor.getNumericArray("ExtrVal").getRealArray2D();
		p.setGlobalVals(myConsts.FEMval, this.ExtrVal[0][0]);
		p.setGlobalVals(myConsts.FEMval, this.ExtrVal[1][0]);			
		// reinitializes global min/max aras for passed value for index			p.
		return this.Ures;
	}//callFEMFunc

	public double[][] getPts() {return pts;}
	public double[][] getTri() {return tri;}
	public double[][] getHt() {return ht;}
	public double[][] getBnd() {return bnd;}
	public boolean isValsSet() {return valsSet;}
	public double[][] getUres() {return Ures;}
	public double[][] getExtrVal() {return ExtrVal;}
	public boolean isUseMatlab() {return useMatlab;}
	
	public void setPts(double[][] pts) {this.pts = pts;}
	public void setTri(double[][] tri) {this.tri = tri;}
	public void setHt(double[][] ht) {this.ht = ht;}
	public void setBnd(double[][] bnd) {this.bnd = bnd;}
	public void setBndHt(double[][] bndHt) {this.bndHt = bndHt;}
	public void setValsSet(boolean valsSet) {this.valsSet = valsSet;}
	public void setUres(double[][] ures) {Ures = ures;}
	public void setExtrVal(double[][] extrVal) {ExtrVal = extrVal;}
	public void setUseMatlab(boolean useMatlab) {this.useMatlab = useMatlab;}

	

}//class
