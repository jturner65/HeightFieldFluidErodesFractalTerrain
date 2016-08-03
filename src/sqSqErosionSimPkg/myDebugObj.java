package sqSqErosionSimPkg;

import java.util.HashMap;

public class myDebugObj {
	public sqSqErosionSimGlobal p;
	public myGui gui;
	
	public double x,y,z;
	public double delT;
	public double calcK;
	public long simCycle;
	public double volH2O;
	public double nodeHeight;
	public double sedimentVals;
	//pipes values
	public final int N = 0, E = 1, S = 2, W = 3;        //for use in flux array 
	public final String[] dirValAra = {"N", "E", "S", "W"};
	public int[] adjNodeDirIdx;                         //the idx of the n,e,s,and w neighbors of this node
	public double[] oldFluxVals;
	public double[] fluxVals;
	public double[] adjWaterShare;     					//ratio of total current water allocated via outflow to lower neighbors, idxed by neighbor number
	public double[] adjNodeH2O;							//current water at adj node, as of sampling
	public double[] adjNodeHeight;						//current height of adj node, as of sampling

	
	public myVector velocity;        					//velocity in 2D along surface of terrain of shallow water model - negative gradient 
	public HashMap<Integer, myHeightNode> adjNodes;
	public HashMap<Integer, Integer> adjNodesDir;  		//direction of each adj node 0- 3	
	public HashMap<Integer, Double> pipeLengths;  		//lengths of virtual pipes to each neighbor, idxed by neighbor's id
	  
	
	public myDebugObj(sqSqErosionSimGlobal _p, myGui _gui, long _simCycle, double _delT, double _volH2O, double _calcK) {
		this.p = _p;
		this.gui = _gui;
		this.simCycle = _simCycle;
		this.delT = _delT;
		this.calcK = _calcK;
		this.adjNodeDirIdx = new int[4];                         //the idx of the n,e,s,and w neighbors of this node
		this.volH2O = _volH2O;
		this.oldFluxVals = new double[4];
		this.fluxVals = new double[4];
		this.adjWaterShare = new double[4];			//ratio of total current water allocated via outflow to lower neighbors, idxed by neighbor number
		this.adjNodeH2O = new double[4];
		this.adjNodeHeight = new double[4];
		this.velocity = new myVector();        					//velocity in 2D along surface of terrain of shallow water model - negative gradient, of either water or sediment
		
		this.adjNodes = new  HashMap<Integer, myHeightNode>();
		this.adjNodesDir = new  HashMap<Integer, Integer>();  		//direction of each adj node 0- 3	
		this.pipeLengths = new HashMap<Integer,Double>();  		//lengths of virtual pipes to each neighbor, idxed by neighbor's id	
	}//
	
	/**
	 * fills in the relevant debug data from the passed src height node
	 * @param srcHNode
	 */	
	public void buildDebugData(myHeightNode srcHNode, int step){
		HashMap<Integer, myHeightNode> adjNodeMap = srcHNode.getAdjNodes();
		int adjNodeDirIDX;
		for (int dir = 0; dir < 4; ++dir){
			adjNodeDirIDX = srcHNode.getAdjNodeIDByPassedDir(dir);
				if(adjNodeMap.get(adjNodeDirIDX) != null){
				this.oldFluxVals[dir] = srcHNode.getFluxByDir(0, dir);
				this.fluxVals[dir] = srcHNode.getFluxByDir(step,dir);
				this.adjWaterShare[dir] = srcHNode.getAdjWaterShare(dir);
				this.adjNodeDirIdx[dir] = adjNodeDirIDX;
				this.adjNodeH2O[dir] = adjNodeMap.get(adjNodeDirIDX).getHeightWater();
				this.adjNodeHeight[dir] = adjNodeMap.get(adjNodeDirIDX).source.coords.y;
			}
		}
		this.x = srcHNode.source.coords.x;
		this.y = srcHNode.source.coords.y;
		this.z = srcHNode.source.coords.z;
		this.velocity = srcHNode.getVelocity().cloneMe(); // this.velocityIn[idx] = srcHNode.getVelocityIn(idx).cloneMe();}
		this.adjNodes = (HashMap<Integer, myHeightNode>)adjNodeMap.clone();
		this.adjNodesDir = (HashMap<Integer, Integer>) srcHNode.getAdjNodeDirByID().clone();
		this.pipeLengths = (HashMap<Integer, Double>) srcHNode.getPipeLengths().clone();
	}
	
	public String toString(){
		String result = "Debug info node at ("+ this.gui.df1.format(this.x) +"," + this.gui.df1.format(this.y) + "," + this.gui.df1.format(this.z) 
							+") for sim cycle : " + this.simCycle + ":\n";
		for (int dir = 0; dir < 4; ++dir){
			result +="\tDir : " + dirValAra[dir] + " | Flux value : " + this.fluxVals[dir] + " | Pipe length to adjacent node : " 
						+ this.pipeLengths.get(adjNodeDirIdx[dir]) + " | adj node's share of this node's h2o : " + this.adjWaterShare[dir] + "\n";
			if( this.adjNodes.get(adjNodeDirIdx[dir]) != null){
			result += "\t\tThis is adjacent node in dir : " + dirValAra[dir] + " ID : "+ this.adjNodes.get(adjNodeDirIdx[dir]).ID + " at " 
						+ this.adjNodes.get(adjNodeDirIdx[dir]).source.coords.toString() + " | Node h2o = " + this.adjNodeH2O[dir] + " | Height : " + this.adjNodeHeight[dir] + "\n";
			} else {
				result += "\t\tThe adjacent node in dir : " + dirValAra[dir] + " does not exist";
			}
		}
		result +="\n";
		result += "\tH2O at node : " + this.volH2O + "\n";
		result += "\tNode height : " + this.nodeHeight + "\n";
		result += "\tDebug delta t : " + this.delT + "\n";
		result += "\tCalculated K : " + this.calcK + "\n";
		result += "\tCalculated velocity of changes in H2O : " + this.velocity.toString();
		result += "\n\n";
		return result;
	}
	
}
