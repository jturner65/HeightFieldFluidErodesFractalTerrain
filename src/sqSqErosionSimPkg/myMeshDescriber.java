package sqSqErosionSimPkg;

import java.util.ArrayList;

public class myMeshDescriber {
	//should have one of these per mesh buildable by simulator
	//use to hold simulation values
	public sqSqErosionSimGlobal p;
	public myGui gui;
	public mySqSqEroder eng;
	public mySqSqSubdivider sub;
	
	public ArrayList<String> flagNames;							//list of names of flags that are true, or empty string for flags that are false
	public double[] erosionKConsts;								//erosion const vals for this mesh
	public double[] erosionSimConsts;
	public boolean isWrapX, isWrapZ, isSolid;					//if wraps in x, wraps in z, is solid (i.e. uses normal to determine "down"
	public String meshName, fileName;							//display name of mesh, file name of mesh
	public int meshID;											//mesh id as defined in myconsts
	public int meshType;										//type : noMesh	:-1;  genMesh:0; damMesh:1;fileMesh:2;stamMesh:3;riverMes:4;	
	
	public myMeshDescriber(sqSqErosionSimGlobal _p,	myGui _gui, mySqSqEroder _eng, mySqSqSubdivider _sub ) {
		this.p = _p;
		this.gui = _gui;
		this.eng = _eng;
		this.sub = _sub;
		resetMeshDescriber();
	}//cnstrctr
	
	public void resetMeshDescriber(){
		this.flagNames = new ArrayList<String>();
		this.erosionKConsts = new double[4];
		this.erosionSimConsts = new double[14];
		this.isWrapX = false;
		this.isWrapZ = false;
		this.meshName = "";
		this.fileName = "";
		this.meshID = -1;
		this.meshType= -1; 
	}//resetMeshDescriber
	
	
	public void setErosionKConsts(double[] _k){
		this.erosionKConsts = new double[4];
		for(int i = 0; i < 4; ++i){		this.erosionKConsts[i] = _k[i];	}
	}//setErosionKConsts
	
	public void setErosionSimConsts(double[] _simC){
		this.erosionSimConsts = new double[14];
		for(int i = 0; i < 14; ++i){		this.erosionSimConsts[i] = _simC[i];	}
	}
	
	public void setFlagNames(String _name){this.flagNames.add(_name);}
	
	public void setFlagNames(ArrayList<String> _names){
		for(String _n : _names){	this.flagNames.add(_n);	}
	}
	
	public ArrayList<String> getFlagNames(){return this.flagNames;}
	public double[] getErosionKConsts(){return this.erosionKConsts;}
	public double[] getErosionSimConsts(){return this.erosionSimConsts;}
	
}//myMeshDescriber
