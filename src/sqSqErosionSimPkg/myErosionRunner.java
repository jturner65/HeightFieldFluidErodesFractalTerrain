/**
 * 
 */
package sqSqErosionSimPkg;

import java.util.concurrent.Callable;

/**
 * @author john
 *
 */
public class myErosionRunner implements Callable<Boolean> {
	private sqSqErosionSimGlobal p;
	private myGui gui;
	private mySqSqEroder eng;
	private mySqSqSubdivider sub;
	
	private myHeightNode srcHNode;

	/**
	 * 
	 */
	public myErosionRunner(	sqSqErosionSimGlobal _p,	myGui _gui, mySqSqEroder _eng,mySqSqSubdivider _sub, myHeightNode _src) {
		this.p = _p;
		this.gui = _gui;
		this.eng = _eng;
		this.sub = _sub;
		this.srcHNode = _src;
		
		// TODO Auto-generated constructor stub
	}

	/**
	 * this will calculate the lateral sediment capacity for boundary nodes, based on their velocity in the direction of their non-wet neighbors
	 * @param srcHNode src node doing the eroding/depositing
	 * @param dirV_comp direction component of velocity being used for lat sed cap calc
	 * @param srcNodeH2O water volume in original node
	 * @param scaleVol	abrasive action of sediment already in src node - varies between 1 and 2
	 * @return
	 */	
	public double calc2ndOrderLatSedCapacity(double adjNodeLclKc, double dirV_comp, double srcNodeH2O, boolean scaleVol, double abrasionSedMult){	
		double sedCapResult; 				
		//if((dirV_comp == 0) || (!scaleVol && (srcNodeH2O < myConsts.epsValCalc)) || (adjNodeLclKc == 0)){return 0; } 
		if((dirV_comp == 0) || (adjNodeLclKc == 0)){return 0; } 
		sedCapResult = adjNodeLclKc 		//use the adjacent node's lclKc value, in case adj node is a boulder
				* dirV_comp 				//this is directional value based on which component of velocity is incident to wall - may be negative
				* abrasionSedMult			//multiply effect by val between 1, depending on sed content in node
				* (scaleVol ? (srcNodeH2O
				//		/p.globMaxAra[myConsts.H2O]
								) : 1 )	//scale by volume - instead of scaling by max mesh vol, just divide by 10 to keep Kc from needing to get too small
		//		/p.globMaxAra[myConsts.H2O]				
			;
			//record this sediment capacity globally, to keep track of global capacity
		p.globTotValAra[myConsts.SEDCAP] += sedCapResult;
		p.setGlobalVals(myConsts.SEDCAP,sedCapResult);
		return sedCapResult;	
	}//calc2ndOrderLatSedCapacity
	
	/**
	 * calculate sediment capacity for the passed node
	 * @param srcHNode
	 * @param srcNodeH2O
	 * @param scaleVol
	 * @return
	 */	
	public double calc2ndOrderSedCapacity(double srcNodeH2O, boolean scaleVol){	//kc for mississippi ~.35-.4 in tons/day/cfs vs h2o @cfs
		double sedCapResult; 
		myVector srcV = srcHNode.getVelocity();		

		//if((srcV.sqMagn == 0) || (srcNodeH2O < myConsts.epsValCalc)){return 0; } 
		if(srcV.sqMagn == 0) {return 0; } 
		//determine whether or not to use y component of velocity for sedCap calc
		myVector vVec = (!p.allFlagsSet(myConsts.ignoreYVelocity)) ? (srcV.cloneMe()) : (new myVector(srcV.x, 0, srcV.z)) ; //whether or not to use y comp for calculations
		double origVecMag = vVec._mag();
		sedCapResult = srcHNode.lclKc
				* origVecMag 
				* (scaleVol ? (srcNodeH2O
									) : 1 )	//scale by volume - instead of scaling by max mesh vol, just divide by 10 to keep Kc from needing to get too small
			;													//all alone down here to be able to comment out middle line
		
			//determine whether or not to limit h2o capable of having sediment capacity to some capped value by dividing by volume that is greater than 
		if(p.allFlagsSet(myConsts.limitWaterSedCap)){
			sedCapResult /= ((srcNodeH2O <= 1 ) ? (1) : (srcNodeH2O));
			}
			//record this sediment capacity globally, to keep track of global capacity
		p.globTotValAra[myConsts.SEDCAP] += sedCapResult;
		p.setGlobalVals(myConsts.SEDCAP,sedCapResult);
		return sedCapResult;
	}//calcSedCap	
	
	/**
	 * this will convert a passed amount of sediment from altitude at this node
	 * @param convertNewHeight the amount of altitude to convert to sediment - these values vary to facilitate boundaries.
	 * @param convertNewSed amount going to sediment array
	 * @param destsIdx the destination idx in the sediment array that will receive this sediment
	 * need useSedFlux instead of global flag because lat erosion overrides global setting and always acts like flux transport
	 * @param massWasting if masswasting don't use advection for sed erosion
	 */
	public void convertHeightToSed(myHeightNode heightSrc, myHeightNode sedSrc, double convertNewHeight, double convertNewSed, boolean useSedFlux, boolean massWasting){
		int destIdx = ((useSedFlux) ? 2 : 1);
			//take from height of node - using return value is due to possibility of having "steel floor" for erosion to prevent it from eroding any lower than some threshold
		if ((heightSrc != null) && (convertNewHeight != 0)){
			if((p.allFlagsSet(myConsts.advectErosionDep)) 
					//&& (!massWasting)
					){	
				//this.advectSedDep(heightSrc, sedSrc.getVelocity().cloneMe(), -1 * convertNewHeight, p.getDeltaT());
				this.advectSedDep(sedSrc, sedSrc.getVelocity().cloneMe(), -1 * convertNewHeight, p.getDeltaT());
			} else {											
				heightSrc.addHeightByVal( -1 * convertNewHeight);	
			}			//modifying existing height, remove sediment from height or deposit sediment to height			
		}
		//this.sedToDrop = this.sedMult * calcNewSed;						
			//add to sediment at node to idx0 vals, put result in idx1
		if((sedSrc != null) && (convertNewSed != 0)) {
			int idxIncr = (massWasting ? 1 : 0);
			sedSrc.addSediment( convertNewSed,  destIdx-1 + idxIncr, destIdx);								//add sediment to sediment availability from idx srcIDX to idx srcIDX+ 1, or same idx if MassWasting
			p.addToTotalVal(myConsts.SED, sedSrc.getSediment(destIdx + idxIncr));
		}
	}//convertHeightToSed	
	
	/**
	 * if using global lowest k then apply it to all flux vals equally
	 * @param srcHNode
	 * @param pXSection
	 * @param nodeArea
	 * @param delT
	 */
	public void calc2ndOrderPipesFluxValsApplyGlobalK(double pXSection, double nodeArea, double delT){
		double sumFlux = srcHNode.getFluxSum(1);  //this is sum of flux leaving source node
		double ratio1;//, ratio2;
		if(srcHNode.getkVal() >= .99){//only process reasonable k's globally
			if (sumFlux != 0){
				for (int tDir = 0; tDir < myHeightNode.numDir; ++tDir){
					ratio1 = srcHNode.getFluxByDir(1,tDir) / sumFlux;
					srcHNode.setAdjWaterShare(tDir, ratio1);
					if(eng.globalKVal != 1){			srcHNode.scaleFlux(1,tDir,eng.globalKVal);		}
				}//for each direction
				if(eng.globalKVal != 1) {
					if(srcHNode.getkVal() != eng.globalKVal){			
						srcHNode.setkScaledNode (false);			//boolean holding whether k is 1 or not
						srcHNode.setkVal(1);
					} else {	srcHNode.setkScaledNode (true);	 }//only display non-unity k vals for nodes causing k to be lowest value  		//boolean holding whether k is 1 or not					

				}
			}//if sumFlux != 0		
			//set values for debugging
			if ((p.nodeInSliceRange(srcHNode.source.coords.x, srcHNode.source.coords.z))//node is in slice range
			  && (p.allFlagsSet(myConsts.debugModeCnsl)))				  //only save debug data if in debug mode  
			{//we are debugging and these coords are being displayed in a slice
				myDebugObj tmpDebugObj = new myDebugObj(this.p, this.gui, p.simCycles, delT, srcHNode.getHeightWater(1), eng.globalKVal );
				tmpDebugObj.buildDebugData(srcHNode, 1);		//1 is idx of relevant flux data
				Double tmpKey = srcHNode.source.coords.x + (eng.DebugHashOffset * srcHNode.source.coords.z);
				eng.debugFluxVolVals.put(tmpKey, tmpDebugObj);  //holds flux in each of 4 card dirs, plus vol water at node in idx 4 and k at idx 6
			}//if we are debugging and at z = 0 on the object
		}
	}//calc2ndOrderPipesFluxValsApplyGlobalK	
	
	/**
	 * calculate flux values to go in each pipe - flux idxs go from 0 to 1
	 * @param srcHNode the source height node at t+delT (end result node)
	 * @param pXSection the cross section of the pipe for the flux model
	 * @param deltaNodeDist the distance between adjacent nodes/vertexes in mesh
	 * @param epsVal the current global epsilon value
	 * @param delT the current global delta t
	 */
	public void calc2ndOrderPipesFluxVals(double pXSection, double nodeArea, double delT){
		double heightDiff;
		myHeightNode adjHNode;
		double K, tmpVal;
		int dir;     //0-3 for n,e,s,w
		double srcNodeH2O_1 = srcHNode.getHeightWater(1);
		double calcFluxAmt; //amount of calculated flux

		for (Integer IDX : srcHNode.getAdjNodes().keySet()){//idx's are obj id's - not sequential - this gives each adjHNode to src hnode
			//tmpVal = 0;
			adjHNode = srcHNode.getAdjNodes().get(IDX);
			heightDiff = srcHNode.getAdjHeightDiffsByIDX(adjHNode.ID);

			dir = srcHNode.getAdjNodeDirByID().get(IDX);      //dir is 0-3 for n,e,s,w
				//flux always positive - only outgoing flux.  incoming flux covered by other nodes' outgoing flux
			calcFluxAmt = srcHNode.getFluxByDir(0,dir) + (delT * (pXSection * myConsts.grav * eng.gravMult * heightDiff)) / (srcHNode.getPipeLength(IDX));//using pythagorean len
			if (calcFluxAmt > 0){ tmpVal = calcFluxAmt; }
			else { 			      tmpVal = 0;}

				//puts flux in ara idx 1
			srcHNode.setFlux(1, dir, tmpVal);
		}//for each index in neighbors keyset                          
//now calculate ratio of node's current water allocated to each pipe based on velocity
		//want sum of flux after round 1
		double sumFlux = srcHNode.getFluxSum(1);  //this is sum of flux leaving source node
		double ratio1;//, ratio2;
		//if the flux leaving times the timestep delT is greater than the vol of water - srcNodeH2O_1 is height of water at node, deltaNodeDist * deltaNodeDist is area of node
		double volAtNode = srcNodeH2O_1 * nodeArea;	//this is water in volume of source node
		if ((sumFlux * delT) >= volAtNode){   K = volAtNode / (sumFlux * delT); K *= .99999999; }//needed for flat surfaces to eventually stop water from moving
		else {		 					      K = 1; }		
		if((p.allFlagsSet(myConsts.useLowestKGlobally)) && (K >=.99)){
			eng.globalKVal = Math.min(eng.globalKVal, K);//ignore tiny values
			srcHNode.setkScaledNode (K!=1);			//boolean holding whether k is 1 or not - used only to tell where lowest k originated from
			srcHNode.setkVal(K); 
		} else {//not using global lowest k val		
			if (sumFlux != 0){
				for (int tDir = 0; tDir < myHeightNode.numDir; ++tDir){
					ratio1 = srcHNode.getFluxByDir(1,tDir) / sumFlux;
					srcHNode.setAdjWaterShare(tDir, ratio1);
					if(K != 1){
						srcHNode.scaleFlux(1,tDir, K);
					}
				}//for each direction
				srcHNode.setkScaledNode (K!=1);			//boolean holding whether k is 1 or not
				srcHNode.setkVal(K);			
			}//if sumFlux != 0	
			//set values for debugging
			if ((p.allFlagsSet(myConsts.debugModeCnsl)) && (p.nodeInSliceRange(srcHNode.source.coords.x, srcHNode.source.coords.z)))//only save debug data if in debug mode and node is in slice range			  				   
			{//we are debugging and these coords are being displayed in a slice
				myDebugObj tmpDebugObj = new myDebugObj(this.p, this.gui, p.simCycles, delT, srcHNode.getHeightWater(1), K );
				tmpDebugObj.buildDebugData(srcHNode, 1);		//1 is idx of relevant flux data
				Double tmpKey = srcHNode.source.coords.x + (eng.DebugHashOffset * srcHNode.source.coords.z);
				eng.debugFluxVolVals.put(tmpKey, tmpDebugObj);  //holds flux in each of 4 card dirs, plus vol water at node in idx 4 and k at idx 6
			}//if we are debugging and at z = 0 on the object
		}//if not using lowest k globally
	}//procCalcPipeFluxVals function

	
	/**
	 * calculate water values for midpoint method - this only calculates values, doesnt actually move water to new idxs
	 *  calculate water values and velocity fields - get outflow flux of neighbors into this src, sum and subtract this node's outflow
	 *  to get h2o vol change - vol h2o idx's go from 1 to 2
	 * @param srcHNode the source height node at t+delT (end result node)
	 * @param srcHNodeOld the source height node at t (beginning node)
	 * @param deltaNodeDist the distance between adjacent nodes/vertexes in mesh
	 * @param delT the current global delta t	*  
	*/
	public void calc2ndOrderPipesWaterValsMidPoint(double nodeArea, double delT){
		double delV;  
		final int dirN = 0, dirE = 1, dirS = 2, dirW = 3;  
		double inFluxSum = 0, 
				inFluxAdj,
				srcH2OAmt = srcHNode.getHeightWater(1),
				outFluxSum = srcHNode.getFluxSum(1);	//sum of outgoing flux values
		myHeightNode[] tmpAdjHNode = new myHeightNode[myHeightNode.numDir];								//adjheight nodes by direction
		double[] fluxIns = new double[myHeightNode.numDir];												//calculate in-fluxes based on each neighbor node, some of which may be null
	  
		for (int dir = 0; dir < myHeightNode.numDir; ++dir){      											//dir is n,e,s,w, this is summing all influxes
			tmpAdjHNode[dir] = srcHNode.getAdjNodesByPassedDir(dir);						//get adj node at particular direction fromm source
			if (tmpAdjHNode[dir] != null) { 
				inFluxAdj = tmpAdjHNode[dir].getFluxByDir(1,((dir+2) % 4)); 				//get the adj node to the dir direction's (dir+2)%4 direction flux - that's his flux back into me			
				fluxIns[dir] = inFluxAdj;													//flux into this node from neighbor to the dir direction
				inFluxSum += inFluxAdj;
//		 	     if ((inFluxAdj != 0) || (outFluxSum != 0)) { gui.print2Cnsl("adj node : " + tmpAdjHNodeOld.ID + " src node " + srcHNodeOld.ID +  " : inFlux : " + inFluxAdj + " outfluxsum : " + outFluxSum);      }
			}//if not null node
			else { 	fluxIns[dir] = 0; }
		}//for each direction                          
		delV = delT * (inFluxSum - outFluxSum);
		double newWaterHeight = srcH2OAmt + (delV/(nodeArea)); 
		srcHNode.setHeightWater(newWaterHeight, 4, false);		
	}//calc2ndOrderPipesWaterValsMidPoint function		
	
	
	/**
	*  calculate water values and velocity fields - get outflow flux of neighbors into this src, sum and subtract this node's outflow
	*  to get h2o vol change - vol h2o idx's go from 1 to 2
	 * @param srcHNode the source height node at t+delT (end result node)
	 * @param srcHNodeOld the source height node at t (beginning node)
	 * @param deltaNodeDist the distance between adjacent nodes/vertexes in mesh
	 * @param delT the current global delta t	*  
	*/
	public void calc2ndOrderPipesWaterVals(double nodeArea, double delT){
		double delV, delSed;  
		final int dirN = 0, dirE = 1, dirS = 2, dirW = 3;  
		double inFluxSum = 0, 
				inFluxAdj,
				srcH2OAmt = srcHNode.getHeightWater(1),
				outFluxSum = srcHNode.getFluxSum(1);	//sum of outgoing flux values
				//using this method to move sediment too
		double inH2OAdj, inSedSum = 0,	inSedAdj,
				srcSedAmt = 0,// = getSedTransportVal(srcHNode, 0, 1),	
				outSedSum = 0;
		if (p.allFlagsSet(myConsts.useSedFlux) ){
			srcSedAmt = getSedTransportVal(srcHNode, 0, 1);	
			if(srcH2OAmt >0){			outSedSum = outFluxSum * srcSedAmt/srcH2OAmt; 	} 			//amount of sediment in this node that is leaving - determined by the ratio of water leaving to total water
		}
		myHeightNode[] tmpAdjHNode = new myHeightNode[myHeightNode.numDir];								//adjheight nodes by direction
		double[] fluxIns = new double[myHeightNode.numDir];												//calculate in-fluxes based on each neighbor node, some of which may be null
	  
		for (int dir = 0; dir < myHeightNode.numDir; ++dir){      											//dir is n,e,s,w, this is summing all influxes
			tmpAdjHNode[dir] = srcHNode.getAdjNodesByPassedDir(dir);						//get adj node at particular direction fromm source
			if (tmpAdjHNode[dir] != null) { 
				inFluxAdj = tmpAdjHNode[dir].getFluxByDir(1,((dir+2) % 4)); 				//get the adj node to the dir direction's (dir+2)%4 direction flux - that's his flux back into me			
				fluxIns[dir] = inFluxAdj;													//flux into this node from neighbor to the dir direction
				inFluxSum += inFluxAdj;
					//if no water, then no sediment is coming in either
				if (p.allFlagsSet(myConsts.useSedFlux) ){
					inH2OAdj = tmpAdjHNode[dir].getHeightWater(1);
					inSedAdj = ((inH2OAdj == 0) ? 0 : ((inFluxAdj/inH2OAdj) * (getSedTransportVal(tmpAdjHNode[dir],0,1))));
					inSedSum += inSedAdj;
				}
//		 	     if ((inFluxAdj != 0) || (outFluxSum != 0)) { gui.print2Cnsl("adj node : " + tmpAdjHNodeOld.ID + " src node " + srcHNodeOld.ID +  " : inFlux : " + inFluxAdj + " outfluxsum : " + outFluxSum);      }
			}//if not null node
			else { 	fluxIns[dir] = 0; }
		}//for each direction                          
		delV = delT * (inFluxSum - outFluxSum);
		double newWaterHeight = srcH2OAmt + (delV/(nodeArea)); 
		srcHNode.setHeightWater(newWaterHeight,2, true);
		
		if (p.allFlagsSet(myConsts.useSedFlux) ){
			delSed = delT * (inSedSum - outSedSum);	//delta t already accounted for in sed calculations
			double newSedVal = srcSedAmt + (delSed /(nodeArea));	
			if(srcHNode.lclKc != 0) {			setSedTransportVal(srcHNode,1,2,newSedVal);	}//sed transport either sed or conc
		}
			// now set source heightnode's velocity vector
		srcHNode.calcAndSetVelocity(fluxIns[dirW], fluxIns[dirE],fluxIns[dirN],fluxIns[dirS], 1, srcHNode.getVolWaterAra(), 2, delV/delT);
		if(p.allFlagsSet(myConsts.useSedFlux)){
			myVector sedVelocity = srcHNode.getVelocity().cloneMe(), startLoc = srcHNode.source.coords.cloneMe();
			sedVelocity._mult(-1 * delT);
			startLoc._add(sedVelocity);
			srcHNode.setSedSource(startLoc);
		}
		srcHNode.boundaryDryNode = false;
		srcHNode.setBoundaryNode(false);										//set true if any neighbor has no water and this has water
		
	}//procCalcPipeFluxVals function		
	/**
	 * this method determines the 4 surrounding nodes for a particular x and z value for location, account for wrap around, putting them in resAra
	 * the nodes will be idx 0 - lowXlowZ; 1 - lowXhighZ; 2 - highXlowZ; 3 - highXhighZ
	 * @param newXIDX the location for x direction on the mesh
	 * @param newZIDX the location for z direction on the mesh
	 * @return the array containing the 4 nodes that surround the xVal,zVal location
	 */		
	public void linInterpNodes(double newX, double newZ, boolean betweenWrapNodesX, myHeightNode[] resAra){
		//idx in result ara
		//final int lowXlowZ = 0, lowXhighZ = 1, highXlowZ = 2, highXhighZ = 3;
		
		int xLowCoordIDX, xHighCoordIDX, zLowCoordIDX, zHighCoordIDX, 
		//intActXLow = -1, intActXHigh = -1, intActZLow = -1, intActZHigh = -1,			//actual coordinates used in array lookup, set when lookup completed Debug use
		xHNodeIDXMod = 0, 
		zHNodeIDXMod = 0;		
		
			//find virtual index in ara of sediment to pull - actual idx's will will be ints on either side of these doubles, barring boundry conditions
		double newXIDXLoc =  newX - p.globMinAra[myConsts.COORD_X]; 
		double newZIDXLoc =  newZ - p.globMinAra[myConsts.COORD_Z];	
		
			//new index location corresponding to the greatest value less than newXIDX/newZIDX - floor is low idx, ceil is high idx, float part is interpolant
		xLowCoordIDX = ((gui.inRange((float)newXIDXLoc, 0, eng.heightMapAra.length - 1 )) ?  (int)newXIDXLoc :			//if in valid range, then use int rounding of val, otherwise use boundary vals, based on value 
						(newXIDXLoc < 0) ? 0 : eng.heightMapAra.length - 1) ;
		xHighCoordIDX = xLowCoordIDX + ((xLowCoordIDX >= (eng.heightMapAra.length - 1 ) /* || (newXIDXLoc < 0)*/) ? 0 : 1);
		if (newX < p.globMinAra[myConsts.COORD_X]){ 	xHNodeIDXMod = -1;}//means the value of x is lower than the lowest value
		if (newX > p.globMaxAra[myConsts.COORD_X]){		xHNodeIDXMod = 1;}

		zLowCoordIDX = ( (gui.inRange((float)newZIDXLoc, 0, eng.heightMapAra[0].length - 1 ) ) ?  (int)newZIDXLoc :			//if in valid range, then use int rounding of val, otherwise use boundary vals, based on value 
			(newZIDXLoc < 0) ? 0 : eng.heightMapAra[0].length - 1) ;				
				
		zHighCoordIDX = zLowCoordIDX + ((zLowCoordIDX >= (eng.heightMapAra[0].length - 1)/* || (newZIDXLoc < 0)*/ ) ? 0 : 1);
		if (newZ < p.globMinAra[myConsts.COORD_Z]){			zHNodeIDXMod = -1;}		//means use low coord as high coord - value is lower than lowest possible coord - and 0 for low coord sediment
		if (newZ > p.globMaxAra[myConsts.COORD_Z]){			zHNodeIDXMod = 1;}		//means use 0 for high sediment
		
		//make sure low and high coords are legal

		if(betweenWrapNodesX){
			xLowCoordIDX = eng.heightMapAra.length - 1;
			xHighCoordIDX = 0;
			zHighCoordIDX = zLowCoordIDX + 1;
			if(zLowCoordIDX >= 0) {
				resAra[eng.lowXlowZ] 		= (eng.heightMapAra[xLowCoordIDX][zLowCoordIDX]);
				resAra[eng.highXlowZ]	 	= (eng.heightMapAra[xHighCoordIDX][zLowCoordIDX]);
			} else {
				resAra[eng.lowXlowZ] 		= null;
				resAra[eng.highXlowZ]	 	= null;
			}
			
			if(zLowCoordIDX < eng.heightMapAra[0].length - 1) {
				resAra[eng.lowXhighZ] 		= (eng.heightMapAra[xLowCoordIDX][zHighCoordIDX]);		
				resAra[eng.highXhighZ]  	= (eng.heightMapAra[xHighCoordIDX][zHighCoordIDX]);	
	
			} else {
				resAra[eng.lowXhighZ] 		= null;		
				resAra[eng.highXhighZ]  	= null;	
			}
				//debug values
//			intActXLow	= eng.heightMapAra.length - 1;
//			intActXHigh = 0;
//			intActZLow 	= zLowCoordIDX;
//			intActZHigh	= zLowCoordIDX+1;
		
		} else {
			if((xHNodeIDXMod != -1) && (zHNodeIDXMod != -1)){//means both low coords are valid
				resAra[eng.lowXlowZ] = (eng.heightMapAra[xLowCoordIDX][zLowCoordIDX]);
//				intActXLow	= xLowCoordIDX;
//				intActZLow 	= zLowCoordIDX;
				}
			
			if((xHNodeIDXMod != 1) && (zHNodeIDXMod != 1)){//means both high coords coords are valid
				resAra[eng.highXhighZ] = (eng.heightMapAra[xHighCoordIDX][zHighCoordIDX]);		
//				intActXHigh = xHighCoordIDX;
//				intActZHigh	= zHighCoordIDX;
			}
			
			if((xHNodeIDXMod != 1) && (zHNodeIDXMod != -1)){//means high x and low z coords are valid
				resAra[eng.highXlowZ] = (eng.heightMapAra[xHighCoordIDX][zLowCoordIDX]);
//				intActXHigh = xHighCoordIDX;
//				intActZLow 	= zLowCoordIDX;
			}
			
			if((xHNodeIDXMod != -1) && (zHNodeIDXMod != 1)){//means lowx and high z are valid
				resAra[eng.lowXhighZ] = (eng.heightMapAra[xLowCoordIDX][zHighCoordIDX]);		
//				intActXLow	= xLowCoordIDX;
//				intActZHigh	= zHighCoordIDX;
			}						
		}//if wrap in x, else if not		
	}//linInterpNodes
	
	/**
	 * this function will move an amount of material chosen to be deposited forward some amount based on the velocity at the node, some random amount between 0 and 1 
	 * representing where in the column of fluid the particle is depositing from, and some constant negative velocity based on the amount of the material being 
	 * deposited and gravity.  once the destination is calculated, deposited amount will be shared amongst the 4 nodes it is found to land in the midst of.
	 * @param srcHNode the source of the velocity.  general algorithm is similar to the transport of sediment in suspension in the water.
	 * @param sedToDrop amount of sediment to lift up
	 * @param delT time step
	 */
	public void advectSedDep(myHeightNode src, myVector depDir, double sedToDrop, double delT){
		//idx in result ara
		myHeightNode[] cornerNodesAra = new myHeightNode[4];
		double //newX, newZ, 
		diffx, diffz;//values of 4 neighbors to newXIDX/newZIDX coord
		//4 nodes surrounding source of sediment
			//find multiplier for height - is going to be used to "smear" deposit based on random height in column of water where deposit will be coming from
			//sed particle size affects surface area to mass/vol ratio, causing them to drop slower or faster
		//sedDepAdvectAngle
		if(depDir.sqMagn != 0){	
			depDir._div(depDir._mag());
			if(Math.abs(depDir.sqMagn - 1) > myConsts.epsValCalc ) {gui.print2Cnsl("error in advect sed dep - deposit dir vector not magnitude 1 : " + depDir.sqMagn );}
		//	gui.print2Cnsl("non-0 vel vector in advect sed dep");			
		}//retain direction, but should make unit length
		//else {gui.print2Cnsl("\t0 mag vel vector in advect sed dep");}
		
		double randVal = Math.random();
		double waterHeight = src.getHeightWater(2);
		double weightedHeight = ((1-(Math.log((randVal * waterHeight)+1) / Math.log(waterHeight+1)))/2.0) * waterHeight;
		depDir._mult(weightedHeight);
		double[] diffVals = findDiffValsForNodeAra(src.source.coords, depDir, cornerNodesAra, delT, 1);

		diffx = diffVals[0];
		diffz = diffVals[1];
//		newX  = diffVals[2];
//		newZ  = diffVals[3];				
		
		this.calc2ndOrderSpreadDeposit(diffx, diffz, sedToDrop, cornerNodesAra);
	}//calc2ndOrderAdvectSedDeposition 
	
	/**
	 * this will find, from a given x,z coordinate, the velocity at that coordinate by looking at its 3 neighbors
	 * @param posX the delta dist in x dir
	 * @param posZ the delta dist in z dir
	 * @param srcHNode the node we use as the base coordinate
	 * @return an array holding the x and z components of the velocity at the passed location
	 */
	public myVector calc2ndOrderGetVelocityAtPos(double diffx, double diffz, myHeightNode[] cornerNodesAra){
		myVector lxlzVal = cornerNodesAra[eng.lowXlowZ].getVelocity(), 
				lxhzVal = cornerNodesAra[eng.lowXhighZ].getVelocity(), 
				hxlzVal = cornerNodesAra[eng.highXlowZ].getVelocity(), 
				hxhzVal = cornerNodesAra[eng.highXhighZ].getVelocity();
		return this.GetInterpVal(diffx, diffz, lxlzVal, lxhzVal, hxlzVal, hxhzVal, false);
	}//calc2ndOrderGetVelocityAtPos
	
	/**
	 * this will spread deposit depVal among 4 nodes held in cornerNodesAra based on the passed interpolants
	 * @param diffxIDX
	 * @param diffzIDX
	 * @param depVal
	 * @param cornerNodesAra
	 */
	public void calc2ndOrderSpreadDeposit(double diffx, double diffz, double depVal, myHeightNode[] cornerNodesAra){
		cornerNodesAra[eng.lowXlowZ].addHeightByVal(((1-diffx) * (1-diffz) * depVal));
		cornerNodesAra[eng.lowXhighZ].addHeightByVal(((1-diffx) * (diffz) * depVal));
		cornerNodesAra[eng.highXlowZ].addHeightByVal(((diffx) * (1-diffz) * depVal));
		cornerNodesAra[eng.highXhighZ].addHeightByVal(((diffx) * (diffz) * depVal));		
	}
	
	/**
	 * checks if a particular new coordinate on the grid surpasses a wrap-around boundary, and if so deals with it
	 * @param wrapAra boolean array used to pass a needed value by reference
	 * @param newXIDX the coordinate - a single x, y or z value
	 * @param araConst which coord constant to use as a source for comparison to min and max
	 * @return the newly modified x (if it needs to be modified) and whether or not this is a wrap around array in wrapAra
	 */	
	public double checkIfWrap(boolean[] wrapAra, double newX, int araConst){
		wrapAra[0] = false;
		if (p.allFlagsSet(myConsts.wrapAroundMesh)){
			if(newX > (p.globMaxAra[araConst])) {//calculated x location is greater than max coord - will either wrap around mesh or pull from between highet and lowest node in wrap 		
				newX = (p.globMinAra[araConst] - ((p.globMaxAra[araConst] + 1) - newX));
				wrapAra[0] = true;				
			}	
			else if(newX < (p.globMinAra[araConst] )) {//wrap	
				newX = (p.globMaxAra[araConst] + (newX - (p.globMinAra[araConst] - 1)));	
				wrapAra[0] = true;				
			}
		}//if wrap around mesh		
		return newX;		
	}//checkIfWrap

	/**
	 * determine the appropriate interpolants for x and z given a particular node, velocity and velocity direction (+/-1 for forward/back step), for calculating the correct location of the resultant
	 * vector between 4 adjacent nodes.  this also will return the correct nodes in the cornernodesara structure
	 * @param srcHNode
	 * @param srcVel
	 * @param cornerNodesAra 
	 * @param delT
	 * @param velDir
	 * @return
	 */
	public double[] findValidBoatLocs(myVector srcHNodeCoords){
		double[] resAra = new double[4];		//idx 0 : diffxIDX, 1:diffzIDX, 2:newx, 3:newz

		resAra[0] = srcHNodeCoords.x;		//location in the mesh in x and z where sediment is coming from
		resAra[1] = srcHNodeCoords.y;
		resAra[2] = srcHNodeCoords.z;

		boolean[] betweenWrapNodesX = new boolean[1];	//using an array to pass by reference		
		resAra[0] = checkIfWrap(betweenWrapNodesX, resAra[0], myConsts.COORD_X);	
		resAra[2] = checkIfWrap(new boolean[]{false}, resAra[2], myConsts.COORD_Z);	
		
		if (betweenWrapNodesX[0]){//wrap around in x, so recalculate y value
			resAra[0] += 1;
			resAra[1] += eng.heightMapAra[eng.heightMapAra.length-1][0].getAvgNeighborHeightDiff(0);
		}
		
		return resAra;
	}//findDiffValsForNodeAra	
	
	/**
	 * determine the appropriate interpolants for x and z given a particular node, velocity and velocity direction (+/-1 for forward/back step), for calculating the correct location of the resultant
	 * vector between 4 adjacent nodes.  this also will return the correct nodes in the cornernodesara structure
	 * @param srcHNode
	 * @param srcVel
	 * @param cornerNodesAra 
	 * @param delT
	 * @param velDir
	 * @return
	 */
	public double[] findDiffValsForNodeAra(myVector srcHNodeCoords, myVector srcVel, myHeightNode[] cornerNodesAra, double delT, int velDir){
		double[] resAra = new double[4];		//idx 0 : diffxIDX, 1:diffzIDX, 2:newx, 3:newz
		double 	delVx = (srcVel.x * delT * velDir),	//velDir is negative for sed transport, positive for sed deposit
				delVz = (srcVel.z * delT * velDir);
		
		resAra[eng.newXIDX] = srcHNodeCoords.x + delVx;		//location in the mesh in x and z where sediment is coming from
		resAra[eng.newZIDX] = srcHNodeCoords.z + delVz;

		boolean[] betweenWrapNodesX = new boolean[1];	//using an array to pass by reference		
		resAra[eng.newXIDX] = checkIfWrap(betweenWrapNodesX, resAra[eng.newXIDX], myConsts.COORD_X);	
				
		this.linInterpNodes(resAra[eng.newXIDX], resAra[eng.newZIDX], betweenWrapNodesX[0], cornerNodesAra);
		//by here we have the 4 nodes around the location newXIDX,newZIDX
	
		//interpolate velocity from resAra nodes
			//get decimal part for interpolant
		resAra[eng.diffxIDX] = resAra[eng.newXIDX] % 1;
		resAra[eng.diffzIDX] = resAra[eng.newZIDX] % 1;		
		
		//make sure low and high coords are legal		
		if (resAra[eng.diffxIDX] < 0){	resAra[eng.diffxIDX] +=1;}// gui.print2Cnsl("diffxIDX less than 0 : "+ diffxIDX + " newx : " + newXIDX); //handles wrap-around 
		if (resAra[eng.diffzIDX] < 0){  resAra[eng.diffzIDX] +=1;}// gui.print2Cnsl("diffzIDX less than 0 : "+ diffzIDX + " newz : " + newZIDX);

		if((resAra[eng.diffzIDX] > 1) || (resAra[eng.diffxIDX] > 1)){			gui.print2Cnsl("error - diff too big " + " diffzIDX : " + resAra[eng.diffzIDX] + " | diffxIDX : " + resAra[eng.diffxIDX]);	} 
		else if((resAra[eng.diffzIDX] < 0) || (resAra[eng.diffxIDX] < 0)){	gui.print2Cnsl("error - diff too small " + " diffzIDX : " + resAra[eng.diffzIDX] + " | diffxIDX : " + resAra[eng.diffxIDX]);	}			
		
		return resAra;
	}//findDiffValsForNodeAra
	
	/**
	 * this will return an interpolated value from 4 vectors, interpolated via x and z interpolants
	 * @param posX the delta dist in x dir
	 * @param posZ the delta dist in z dir
	 * @param srcHNode the node we use as the base coordinate
	 * @return an array holding the x and z components of the velocity at the passed location
	 */
	private myVector GetInterpVal(double diffx, double diffz, myVector lxlzVal,myVector lxhzVal, myVector hxlzVal, myVector hxhzVal, boolean interpY ){
		myVector result = new myVector(), valXlowZ = new myVector(), valXHighZ = new myVector();
		
		valXlowZ.x 	= ((1-diffx) * lxlzVal.x)	+ (diffx * hxlzVal.x);
		valXHighZ.x	= ((1-diffx) * lxhzVal.x)	+ (diffx * hxhzVal.x);		
		result.x = ((1-diffz) * valXlowZ.x) + (diffz * valXHighZ.x);		
		
		if (interpY){
			valXlowZ.y 	= ((1-diffx) * lxlzVal.y)	+ (diffx * hxlzVal.y);
			valXHighZ.y	= ((1-diffx) * lxhzVal.y)	+ (diffx * hxhzVal.y);		
			result.y    = ((1-diffz) * valXlowZ.y) + (diffz * valXHighZ.y);			
		}//if interpolating y value too
		
		valXlowZ.z 	= ((1-diffx) * lxlzVal.z)	+ (diffx * hxlzVal.z);
		valXHighZ.z	= ((1-diffx) * lxhzVal.z)	+ (diffx * hxhzVal.z);
		result.z = ((1-diffz) * valXlowZ.z) + (diffz * valXHighZ.z);			
		return result;
	}//calc2ndOrderGetVelocityAtPos	
	/**
	 * depending on whether we are transporting sediment concentration or sediment itself, this will return the correct value
	 * @param node node pulling from
	 * @param sedIDX idx in sed ara for sediment
	 * @param h2oIDX idx in h2o ara for water, if used, otherwise ignored
	 * @return either the sediment or sediment concentration existing at this node
	 */
	private double getSedTransportVal(myHeightNode node, int sedIDX, int h2oIDX){
		if(p.allFlagsSet(myConsts.useSedConc)){			return node.getSedimentConcentration(sedIDX, h2oIDX);} 
		else {											return node.getSediment(sedIDX);}
	}//getSedTransportVal
	
	private void setSedTransportVal(myHeightNode node, int sedIDX, int h2oIDX, double conc){
		if(p.allFlagsSet(myConsts.useSedConc)){			node.setSedimentFromConcentration(sedIDX, h2oIDX, conc);}
		else {											node.setSedimentPipes(conc, sedIDX);}
	}//getSedTransportVal

	
	/**		RETURN EITHER SED OR SED CONC IN IDX 0, H2O IN IDX 1
	 * will return with "dry" node's contribution to sediment, determined by weighted interpolation of dry node's wet neighbors, if any exist
	 * --weight cardinal-dir nodes by 2, diags by 1, sum all weights and normalize
	 * @param dryNode
	 * @return
	 */
	private double[] getSedForDryNode(myHeightNode dryNode, int recLayer, myHeightNode callNode){
		myHeightNode compNode;
		int numRecLayers = 3;
		//double[] result = new double[2];
		int x = dryNode.source.xIDX, z = dryNode.source.zIDX, xIdx, zIdx, multiplier, totDivisor = 0;
		double sedAmt, h2oAmt, totSed = 0,	totH2O = 0;

		for(int xMod = -1; xMod < 2; ++xMod){
			xIdx = x + xMod;		//x is location of drynode, xmod is location of neighbor in x dir
			//for each potential neighbor in the x direction - check if drynode is a wrap around node to the other side of the mesh
			if ((!(dryNode.isMirrorNode(0))) && ((xIdx < 0) || (xIdx >= eng.heightMapAra.length))){				continue; }//at edge for this x and not wrap around, so leave the loop
			if(dryNode.isMirrorNode(0)){//wrap around node	- adjust xIdx	
				if(xIdx < 0) 							{	xIdx = eng.heightMapAra.length-1; }						
				else if (xIdx >= eng.heightMapAra.length) 	{	xIdx = 0; }//wrapping to xidx = 0
			}
			
			for(int zMod = -1; zMod < 2; ++zMod){			//need to check boundaries of mesh for wrap around, or edges without neighbors
				zIdx = z + zMod;
				if ((xMod == 0) && (zMod == 0)){							continue;	}//don't process center node
				if ((zIdx < 0) || (zIdx >= eng.heightMapAra[0].length)){		continue;	}//at edge for this z  leave the loop - no wrap on z				
				compNode = eng.heightMapAra[xIdx][zIdx];
				h2oAmt = compNode.getHeightWater(2);
				sedAmt = this.getSedTransportVal(compNode, 1, 2); //compNode.getSediment(1);  //either sediment or sed conc
//				if((h2oAmt > 0) && (sedAmt > 0)){//wet node with sediment, use to further calculations					
				if(h2oAmt > 0){//wet node, use to further calculations					
					multiplier = (((xMod == 0) || (zMod == 0)) ? 2 : 1); //weight each node's contribution by either 2 or 1 based on location
					totDivisor += multiplier;			//keep total of multipliers, for weighting
					totSed +=  (multiplier * sedAmt);	//keep total of sediment copied from surrounding nodes
					totH2O += (multiplier * h2oAmt);
				}//if not a dry node	
			}//for z
		}//for x
		
		//perform calculation
		if(totDivisor == 0){//means a dry node surrounded by dry nodes - tot sed and toth2o should also be 0
			//recursively call this routine with this node's adjacent nodes, and then have their results contribute to build this node's results
			totSed = 0;
			totH2O = 0;
			if(recLayer < numRecLayers){									//set max # of recursive layers for any node to be less than 3
				for(int xMod = -1; xMod < 2; ++xMod){
					xIdx = x + xMod;
					if ((!(dryNode.isMirrorNode(0))) && ((xIdx < 0) || (xIdx >= eng.heightMapAra.length))){				continue; }//at edge for this x  leave the loop
					if(dryNode.isMirrorNode(0)){//wrap around node	- adjust xIdx	
						if(xIdx < 0) 							{	xIdx = eng.heightMapAra.length-1; }						
						else if (xIdx >= eng.heightMapAra.length) 	{	xIdx = 0; }//wrapping to xidx = 0
					}
					for(int zMod = -1; zMod < 2; ++zMod){			//need to check boundaries of mesh for wrap around, or edges without neighbors
						zIdx = z + zMod;
						if (((xMod == 0) && (zMod == 0)) || ((zIdx < 0) || (zIdx >= eng.heightMapAra[0].length))){		continue;	}//at edge for this z  leave the loop - no wrap on z, also don't process center node					
						compNode = eng.heightMapAra[xIdx][zIdx];  									//dry neighbor of current dry node - recurse through all neighbors to find a wet node to contribute to this node's amounts
						if (compNode.ID == dryNode.ID) { continue;}								//don't check out calling node, we know this one is empty
						double[] results = new double[2];
						results = getSedForDryNode(compNode, recLayer + 1, dryNode);				
						sedAmt = results[0];
						h2oAmt = results[1];
						//if((h2oAmt > 0) && (sedAmt > 0)){										//wet node with sediment, use to further calculations					
						if(h2oAmt > 0){										//wet node with sediment, use to further calculations					
							multiplier = (((xMod == 0) || (zMod == 0)) ? 2 : 1); 				//weight each node's contribution by either 2 or 1 based on location
							totDivisor += multiplier;											//keep total of multipliers, for weighting
							totSed += (multiplier * sedAmt);									//keep total of sediment copied from surrounding nodes
							totH2O += (multiplier * h2oAmt);
						}//if not a dry node	
					}//for z
				}//for x
			} //else {}//only search
		}//if totDivisor == 0
		double[] results = new double[2];
		results[0] = (totSed/totDivisor);			//totals for this particular node - if called in recursive loop, this node's contribution will be lessened again
		results[1] = (totH2O/totDivisor);		
		return results;
	}//getSedForDryNode
	
	/**
	 * --uses 2d ara--
	 * fast method for pipes transport of sediment, using stam-y lagrangian method - put in each node at srcIDX + 1
	 * calculate by finding the sediment at t + delt that came to this node during t time step from a node at x - u*delT, z - v*delT
	 * @param srcHNode the source node to draw information from about sediment
	 * @param srcHNodeOld the old source node - gets copy of result
	 * @param xCoords array of all x coords of this mesh - in order
	 * @param zCoords array of all z coords of this mesh - in order
	 * @param delT time step	
	 * @param srcIDX index to pull sed from
	 */
	public void calc2ndOrderPipesSedTransportAra(double delT){
		//idx in result ara
		myHeightNode[] resAra = new myHeightNode[4];
		for(int i = 0; i<4; ++i){resAra[i] = null;}
		double newSedXLowZ, newSedXHighZ, newX, newZ,diffx, diffz,
		srcH2O = srcHNode.getHeightWater(2),
				LLh2o,LHh2o,HLh2o,HHh2o,
		srcSed = srcHNode.getSediment(1),
		newSedlowXlowZ = 0,newSedhighXlowZ = 0, newSedlowXhighZ = 0,newSedhighXhighZ = 0,newSedVal = 0;//values of 4 neighbors to newXIDX/newZIDX coord
		
		//if(srcSed > srcH2O){	gui.print2Cnsl("2) : more sed than water in transport : "  + srcSed + " > " + srcH2O + " on node : " + srcHNode.ID + " at x,z idx : " + srcHNode.source.xIDX + "," + srcHNode.source.zIDX);	}
			//only used by boundary nodes
		boolean boundLLConv = false, boundLHConv = false,	boundHLConv = false,	boundHHConv = false;
		
		//calculates newXIDX,Z and diffxIDX,z for this srcHNode and its velocity, also populates resAra with 4 appropriate nodes that surround the back-step location 
		//idx in diff vals : 0 = diffxIDX, 1 = diffzIDX, 2 = newXIDX, 3 = newZIDX
		myVector srcVel =  srcHNode.getVelocity();
		double[] diffVals = findDiffValsForNodeAra(srcHNode.source.coords, srcVel, resAra, delT, -1);//-1 means backwards step

		diffx = diffVals[0]; 	diffz = diffVals[1]; 	newX  = diffVals[2]; 	newZ  = diffVals[3];
			//sed values are all inited to 0

		LLh2o = 1;		HHh2o = 1;	HLh2o = 1;	LHh2o = 1;

		if(resAra[eng.lowXlowZ] 	!= null){		newSedlowXlowZ  =  getSedTransportVal(resAra[eng.lowXlowZ], 1, 2); 		LLh2o = resAra[eng.lowXlowZ].getHeightWater(2);}				
		if(resAra[eng.highXhighZ] 	!= null){		newSedhighXhighZ = getSedTransportVal(resAra[eng.highXhighZ], 1, 2);	HHh2o = resAra[eng.highXhighZ].getHeightWater(2);}	
		if(resAra[eng.highXlowZ] 	!= null){		newSedhighXlowZ = getSedTransportVal(resAra[eng.highXlowZ], 1, 2);		HLh2o = resAra[eng.highXlowZ].getHeightWater(2);}			
		if(resAra[eng.lowXhighZ] 	!= null){		newSedlowXhighZ  = getSedTransportVal(resAra[eng.lowXhighZ], 1, 2);		LHh2o = resAra[eng.lowXhighZ].getHeightWater(2);}						

		//if nodes to pull from are dry and not outside bounds of mesh
		if(p.allFlagsSet(myConsts.pullSedFromDryNodes)){
			if((resAra[eng.lowXlowZ] != null)  && (newSedlowXlowZ == 0) && (resAra[eng.lowXlowZ].getHeightWater(2) == 0)){ 		//sanity check - this node has no water and no sed-
				double[] dryTransRes = calc2ndOrderSedTransDryNode(eng.lowXlowZ, resAra, srcH2O, srcSed);
				newSedlowXlowZ 	= dryTransRes[0];			//sed or sedConc
				LLh2o 			= dryTransRes[1];			
				boundLLConv = p.allFlagsSet(myConsts.erodeDryNodes);//true;
			}
			if((resAra[eng.lowXhighZ] != null) && (newSedlowXhighZ == 0) && (resAra[eng.lowXhighZ].getHeightWater(2) == 0)){//* 1000 < srcH2O)){ 		//sanity check - this node has no water
				double[] dryTransRes = calc2ndOrderSedTransDryNode(eng.highXhighZ, resAra, srcH2O, srcSed);
				newSedlowXhighZ = dryTransRes[0];			//sed or sed conc
				LHh2o 			= dryTransRes[1];
				
				boundLHConv = p.allFlagsSet(myConsts.erodeDryNodes);//true;
			}
			if((resAra[eng.highXlowZ] != null) && (newSedhighXlowZ == 0) && (resAra[eng.highXlowZ].getHeightWater(2) == 0)){ 		//sanity check - this node has no water
				double[] dryTransRes = calc2ndOrderSedTransDryNode(eng.highXlowZ, resAra, srcH2O, srcSed);
				newSedhighXlowZ = dryTransRes[0];			//sed or sed conc
				HLh2o 			= dryTransRes[1];
				boundHLConv = p.allFlagsSet(myConsts.erodeDryNodes);//true;			
			}
			if((resAra[eng.highXhighZ] != null) && (newSedhighXhighZ == 0) && (resAra[eng.highXhighZ].getHeightWater(2) == 0)){ 		//sanity check - this node has no water
				double[] dryTransRes = calc2ndOrderSedTransDryNode(eng.highXhighZ, resAra, srcH2O, srcSed);
				newSedhighXhighZ = dryTransRes[0];			//sed or sed conc
				HHh2o 			 = dryTransRes[1];		
				boundHHConv = p.allFlagsSet(myConsts.erodeDryNodes);//true;
			}		
		}


			//record amount of sediment/sed concentration moved and also redeposit unused sediment from dry nodes - converts back to sediment in this method if sed conc
		if(resAra[eng.lowXlowZ] != null) { 		this.calc2ndOrderSedTransAmt((1-diffx),  (1-diffz), newSedlowXlowZ, resAra[eng.lowXlowZ], LLh2o, boundLLConv);}//			sedInterpMult = (1-diffxIDX) * (1-diffzIDX);
		if(resAra[eng.lowXhighZ] != null) {		this.calc2ndOrderSedTransAmt((1-diffx), diffz, newSedlowXhighZ, resAra[eng.lowXhighZ], LHh2o,  boundLHConv);}		
		if(resAra[eng.highXlowZ] != null) {		this.calc2ndOrderSedTransAmt(diffx, (1-diffz), newSedhighXlowZ, resAra[eng.highXlowZ], HLh2o, boundHLConv);}
		if(resAra[eng.highXhighZ] != null) { 	this.calc2ndOrderSedTransAmt(diffx, diffz, newSedhighXhighZ, resAra[eng.highXhighZ], HHh2o, boundHHConv);}

		srcHNode.setSedSource(new myVector(newX, srcHNode.source.coords.y, newZ));		

		//interpolation for low z and high z first
		newSedXLowZ 	= ((1-diffx) * newSedlowXlowZ)	+ (diffx * newSedhighXlowZ);
		newSedXHighZ 	= ((1-diffx) * newSedlowXhighZ)	+ (diffx * newSedhighXhighZ); 
		
		newSedVal = ((1-diffz) * newSedXLowZ) + (diffz * newSedXHighZ);
		
		setSedTransportVal(srcHNode, 2, 2, newSedVal);			//set either sediment transported or sediment concentration transported as sediment on destination node
	}//calc2ndOrderPipesSedTransport		
	
	 
	private double[] calc2ndOrderSedTransDryNode(int dryNodeIDX, myHeightNode[] nodeAra, double srcH2O, double srcSed){
		double[] result = new double[2]; 
		double[] tmpVal;
		double interpSed, waterVal;
		tmpVal =  getSedForDryNode(nodeAra[dryNodeIDX], 0, null);
		//interpSed = ((tmpVal[0] <= 0) || (tmpVal[1] <= myConsts.epsValCalc ) ? srcSed : tmpVal[0]);// /(((p.allFlagsSet(myConsts.useSedConc))) ? (tmpVal[1]) : 1));
		interpSed = tmpVal[0];// /(((p.allFlagsSet(myConsts.useSedConc))) ? (tmpVal[1]) : 1));
		//LLh2o = 1;
		//waterVal = (((p.allFlagsSet(myConsts.useSedConc))) ? ((tmpVal[0] <= 0) || (tmpVal[1] <= myConsts.epsValCalc )  ? srcH2O : tmpVal[1]) : 1);
		waterVal = (((p.allFlagsSet(myConsts.useSedConc))) ? (tmpVal[1]) : 1);//either amt of water here or 1, depending on concentration calculation or pure sediment calculation
		if((interpSed < 1000) && (interpSed >= 0)){
			result[0] = interpSed;		
			result[1] = waterVal;
			convertHeightToSed(nodeAra[dryNodeIDX], nodeAra[dryNodeIDX], p.allFlagsSet(myConsts.erodeDryNodes) ? interpSed : 0, interpSed, false, false);
		} else {
			//p.setFlags(myConsts.simulate, false);
			//if(p.allFlagsSet(myConsts.debugMode)){	gui.print2Cnsl("3):srcSed/conc and h2o : " + gui.df7.format(srcSed)+" & "+ gui.df7.format(srcH2O)	+ " sed/conc !trans : " + interpSed + " water !trans : " + result[1]);}
			result[0] = 0;
			result[1] = 1;
		}
		return result;		
	}//calc2ndOrderSedTransDryNode
	
	/**
	 * set amount of either sediment or sediment concentration being transported
	 * 
	 * @param diffx
	 * @param diffz
	 * @param newSedAmt
	 * @param interpNode
	 * @param boundaryConv
	 */	
	private void calc2ndOrderSedTransAmt(double diffx, double diffz, double origSedAmt, myHeightNode interpNode, double interpH2O, boolean boundaryConv){
		double sedInterpMult, transAmt, reDepAmt;
		sedInterpMult = diffx * diffz;
		transAmt = sedInterpMult * origSedAmt * (((p.allFlagsSet(myConsts.useSedConc))) ? interpH2O : 1);//multiply by water at dest node to convert back to sediment
		interpNode.addSedTransported(transAmt);			//sedTransported is not used for any primary sediment calculations, only transportation and sed debugging/display
		if(boundaryConv){								//if we are depositing in the transport step to account for eroding the dry land we get the sediment from					
			reDepAmt = -1 * (1-sedInterpMult) * origSedAmt * (((p.allFlagsSet(myConsts.useSedConc))) ? interpH2O : 1);//multiply by amt of water if using sed conc
			convertHeightToSed(interpNode, interpNode, p.allFlagsSet(myConsts.erodeDryNodes) ? reDepAmt : 0 , reDepAmt, false, false);		//deposit either dep amt or 0 to height of node
		}//means some amount of this node was converted to sediment to act as a transportation source	
	}//calc2ndOrderSetTransAmt	

	@Override
	public Boolean call() throws Exception {
		
		
		return true;
	}
	
	
}
