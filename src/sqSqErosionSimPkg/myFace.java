package sqSqErosionSimPkg;
import processing.core.*;

/**
*  a class to define a face object of a polygon
*/
public class myFace extends mySceneObject{
	//id of face in file
	private int faceNum;
	//number of verticies for this face
	private int numVerts;
	//list of verticies making up this face
	private myVertex[] verts;
	//center of this face - avg coords of all verts
	private myVector center; 
	
	/////
	//used for display purposes - declared here to speed process up
	/////
	
	private boolean waterOnPoly, sedOnPoly, sedCapOnPoly, sedConcOnPoly, 						//flags for water on poly, sed on poly, sedcap exist on poly
					h2oVShown, sedSrcVShown, kNotUnity, sedLeftOver;	           				//h2o velocity shown already, sedsrc shown already, k is not 1, sed leftover on a node in this face
	
	private myHeightNode[] heightNodes;			                                            	//the height nodes that correspond with this face's verts, if they exist
	private boolean[] boundaryH2ONodes,				                                            //whether or not any of the nodes of this face are boundary nodes for water
						noH2ONodesByIDX,																//nodes in a poly that have no water
						nonErodingNode,																//nodes in a poly that have no water
						isWrapNode;

	private myVector[] nodeNorms,      			                                            	//normals at each node
						nodeCs,					                                            	//centers of each node 
						origMeshNodeCoords,
						oldMeshNodeCoords,

						h2oNodeVs,				                                            	//water/sediment velocity vectors at each node
						
						h2oNodeCoords,  		                                            	//x,y,z coords of each of 4 node of water polys
						h2oNodeNorms,  			                                            	//x,y,z coords of each of 4 normals for the mode of a water poly
						
						sedNodeCoords,  		                                            	//x,y,z coords of each of 4 node of sed polys
						sedNodeNorms,  			                                            	//x,y,z coords of each of 4 normals for the mode of a sed poly
						
						sedCapNodeCoords,  		                                            	//x,y,z coords of each of 4 node of sedcap polys
						sedCapNodeNorms,  			                                            //x,y,z coords of each of 4 normals for the mode of a sedcap poly

						sedConcNodeCoords,  		                                            //x,y,z coords of each of 4 node of sed concentration polys
						sedConcNodeNorms,  			                                            //x,y,z coords of each of 4 normals for the mode of a sed concentration poly
						
						sedNodeSrc,																//source of nodes' current sediemnt, as per semi-lagrangian back-step	
						
						kNodeLocations, 														//the coords of each node that has a non-unity k
						sedLeftOverLocs,														//coords for each node that has un-transported sediment
						
						wrapToNodeCoords;														//the coordinates that this node wraps around to
	
	private myVector[][] outFluxCoords;															//each vertex has up to 4 outgoing flux values, so use this array as [vert][dir] : dir is defined in myHeightNode
	private double[][] outFluxVals;																//the values for the flux for each vertex in each of 4 directions
			
	private myVector[][] pipeLengthCoords;															//each vertex has up to 4 outgoing pipes, this is the coords of the end of each, for debug display diamonds
	private double[][] pipeLengths;

	private myVector[][] adjNodeHeightDiffsCoords;													//each node has up to 4 neighbors, this is the height difference between each of them displayed at coords, for debug display diamonds
	private double[][] adjNodeHeightDiffs;
	
	private double[] kNodeVals,																			//holds the values of k for each node of this face, 
					numAdjNodes,
//					tiltComponentVals,																	//the calculated sine of the ratio of the normal at a node dotted with the velocity at the node - gets tilt component from paper - varies between ~0 and 1
					sedLeftOverAmt;																		//holds the amount of sediment left over for a node of this face after tansport
	
	private int noH2ONodes, noSEDNodes, noSEDCAPNodes, noSEDCONCNodes;
	

	private myVector h2oFaceNorm, h2oFaceCenter, 					//face normal and center for h2o, sed and sed capacity
					sedFaceNorm, sedFaceCenter, 
					sedCapFaceNorm, sedCapFaceCenter,
					sedConcFaceNorm, sedConcFaceCenter;

	public myFace(sqSqErosionSimGlobal _p, myGui _g, mySqSqEroder _e, mySqSqSubdivider _s, int faceNum, int numVerts, myVertex[] verts){
		super(_p, _g, _e, _s);
		this.faceNum = faceNum;
		this.numVerts = numVerts;
		this.center = new myVector(0,0,0);
		this.verts = new myVertex[numVerts];      //must be in clockwise order and must start at "upper left"
		for (int idx = 0; idx < numVerts ; ++idx ){
			this.verts[idx] = verts[idx];
			if (idx != 0) { //set adjacent vertices
				this.verts[idx].addAdjVertex(this.verts[idx-1]);
				this.verts[idx-1].addAdjVertex(this.verts[idx]);
			}//if idx != 0   
			this.center._add(this.verts[idx].coords);
		}//for idx < numVerts  
		//set adjacencies for verts[0] and verts[numVerts]
		this.center._div(this.numVerts);
		this.verts[0].addAdjVertex(this.verts[numVerts-1]);
		this.verts[numVerts-1].addAdjVertex(this.verts[0]);
		//TODO remove once distance locked to 1
		if(sub.getDeltaNodeDist() == 0){//will be done only once, save time
			sub.setDeltaNodeDist(Math.max(Math.abs(this.verts[0].getCoords().x - this.verts[1].getCoords().x), Math.abs(this.verts[0].getCoords().z - this.verts[1].getCoords().z)));
		}
		this.calcNormal();
		//this.reInitDispVars();
	}//constructor - 3 args

	/**
	 *  calculate normal for this face - use verticies and cross product of first 3 verts
	 */
	public void calcNormal(){
    
		myVector P0P2 = new myVector(0,0,0);
		P0P2.set(this.verts[2].getX() - this.verts[0].getX(),this.verts[2].getY() - this.verts[0].getY(),this.verts[2].getZ() - this.verts[0].getZ());
		P0P2._normalize();

		if (this.numVerts == 3 ){//triangle
			myVector P0P1 = new myVector(0,0,0);
			//set up vectors from each this.vertex
			P0P1.set(this.verts[1].getX() - this.verts[0].getX(),this.verts[1].getY() - this.verts[0].getY(),this.verts[1].getZ() - this.verts[0].getZ());
			P0P1._normalize();
			this.N = P0P2._cross(P0P1);
		} else if (this.numVerts == 4 ) {//quad
			myVector P3P1 = new myVector(0,0,0);
			//set up vectors from each vertex
			P3P1.set(this.verts[1].getX() - this.verts[3].getX(),this.verts[1].getY() - this.verts[3].getY(), this.verts[1].getZ() - this.verts[3].getZ());
			P3P1._normalize();
			this.N = P0P2._cross(P3P1);
		}
    
		this.N._mult(p.getFaceNormalMult());
		this.N._normalize();
    
	}//calcNormal

/**
 *	calculate the normal of a "face" of the water/sediment display - use verts[x].getOwningNode().getCurAltitude() to get y value, x and z are fine as is
 * @param material the material amount being added to the node to calculate the normal
 * @return the normal for the material poly on this face
 */
	public myVector calcNormalMaterialFace(int material){
		myVector materialNorm = new myVector(0,0,0);
		myVector P0P2 = new myVector(0,0,0);
		
		double vert0yVal = (verts[0].getOwningNode() != null)? (verts[0].getY() + verts[0].getOwningNode().getVisualisationVal(material, 0)) : (this.verts[0].getY());
		double vert1yVal = (verts[1].getOwningNode() != null)? (verts[1].getY() + verts[1].getOwningNode().getVisualisationVal(material, 0)) : (this.verts[1].getY());
		double vert2yVal = (verts[2].getOwningNode() != null)? (verts[2].getY() + verts[2].getOwningNode().getVisualisationVal(material, 0)) : (this.verts[2].getY());
		double vert3yVal = (verts[3].getOwningNode() != null)? (verts[3].getY() + verts[3].getOwningNode().getVisualisationVal(material, 0)) : (this.verts[3].getY());
		
		double xVal = verts[2].getX() - verts[0].getX();
		double yVal = vert2yVal - vert0yVal;
		double zVal = verts[2].getZ() - verts[0].getZ();
		P0P2.set(xVal,yVal,zVal);
		P0P2._normalize();

		if (this.numVerts == 3 ){//triangle
			myVector P0P1 = new myVector(0,0,0);
			xVal = verts[1].getX() - verts[0].getX();
			yVal = vert1yVal - vert0yVal;
			zVal = verts[1].getZ() - verts[0].getZ();
			//set up vectors from each vertex
			P0P1.set(xVal,yVal,zVal);
			P0P1._normalize();
			materialNorm = P0P2._cross(P0P1);
		} else if (this.numVerts == 4 ) {//quad
			myVector P3P1 = new myVector(0,0,0);
			xVal = verts[1].getX() - verts[3].getX();
			yVal = vert1yVal - vert3yVal;
			zVal = verts[1].getZ() - verts[3].getZ();
			//set up vectors from each vertex
			P3P1.set(xVal,yVal,zVal);
			P3P1._normalize();
			materialNorm = P0P2._cross(P3P1);
		}
    
		materialNorm._mult(p.getFaceNormalMult());
		materialNorm._normalize();
		return materialNorm;   		
	}//calcNormalWaterFace
	
	public void calcNormal(boolean reCalcCenter){
		this.calcNormal();
		if(reCalcCenter){this.reCalcCenter();}
	}//calcNormal
	
	public void reCalcCenter(){
		this.center = new myVector(0,0,0);
		for (int idx = 0; idx < numVerts ; ++idx ){
			this.center._add(this.verts[idx].coords);
		}//for idx < numVerts  
		this.center._div(this.numVerts);
	}//calculate the center of this face

	/**
	 * display a polygon of water/sediment on this face
	 */
	private void drawWaterPoly(){
		
		p.pushStyle();
			p.shininess(100.0f);
			p.specular(255);
       		//draw water velocity vector
    		if (p.allFlagsSet(myConsts.showH2OVelocity, myConsts.Pipes)){
    		    p.pushMatrix();  	
    			for(int incr = 0; incr < h2oNodeVs.length; ++incr ){ p.dispVectorArrow(h2oNodeVs[incr], h2oNodeCoords[incr],.03, myGui.gui_White, false);}
				this.h2oVShown = true;
				p.popMatrix();
    		}//if drawing water velocity vector
    	
	    	//draw surface normals of water
	    	if (p.allFlagsSet(myConsts.H2OMeshDisplay,myConsts.showH2ONormals)){
	            p.pushMatrix();
	    		if(p.allFlagsSet(myConsts.vertH2ONormals)){ 
	    			for(int incr = 0; incr < h2oNodeNorms.length; ++incr ){
	    														p.dispVectorArrow(h2oNodeNorms[incr], h2oNodeCoords[incr], .01,  myGui.gui_LightBlue, false);}}
	    		else {							    			p.dispVectorArrow(h2oFaceNorm, h2oFaceCenter, .01,  myGui.gui_LightCyan, false);}
	    	   	p.popMatrix();
	    	}//if showing water normals  
	    	
   			if ((!this.sedSrcVShown) && p.allFlagsSet(myConsts.dispSedSrcVecs)) {//if we're showing the source vectors of the sediment,
   				this.sedSrcVShown = true;
   				p.pushMatrix();
   				for(int incr = 0; incr < this.sedNodeSrc.length; ++incr ){
   					if(this.heightNodes[incr].getHeightWater() > 0){
   						p.dispLine(this.sedNodeSrc[incr], h2oNodeCoords[incr], ((this.heightNodes[incr].isBoundaryNode()) ?  myGui.gui_White : myGui.gui_LightMagenta ));
   						//p.dispVectorArrow((myVector._sub(this.sedNodeSrc[incr], this.h2oNodeCoords[incr])), h2oNodeCoords[incr], .01, ((this.heightNodes[incr].isBoundaryNode()) ?  myGui.gui_White : myGui.gui_LightMagenta ), false);
   					}
   				}	   	
   				p.popMatrix();
   			}//if showing sedSrcVecs and sediment exists on this node		    	
 	
		   	//draw water poly  -  if drawing water, heightmap made, and we're currently displaying either water or sediment or sed capacity 	
    		if ((waterOnPoly) && (p.allFlagsSet(myConsts.H2OMeshDisplay)) && (p.globMaxAra[myConsts.H2O] > 0)){ 
    		   	p.pushMatrix();    	
    		      
    			p.beginShape(p.polyType);
	        
    			if (p.allFlagsSet(myConsts.strokeDisplayH2O)){p.stroke(100,100,255);} else {p.noStroke();}
    			for(int idx = 0; idx < this.h2oNodeNorms.length; ++idx){
    				if(!p.anyFlagsSet(myConsts.vertH2ONormals)){p.normal((float)h2oFaceNorm.x, (float)h2oFaceNorm.y, (float)h2oFaceNorm.z);}
    				else { 										p.normal((float)h2oNodeNorms[idx].x, (float)h2oNodeNorms[idx].y, (float)h2oNodeNorms[idx].z);}
    					//if boundary node set color to cyan
    				if(p.allFlagsSet(myConsts.showBoundaryNodes) && boundaryH2ONodes[idx]){ gui.setColorValFill(myGui.gui_LightCyan);}//set water boundary nodes cyan
    				else if(this.noH2ONodesByIDX[idx]) {									gui.setColorValFill(myGui.gui_TransBlack); }
    				else {																	gui.setWaterColorFill(this.heightNodes[idx].getHeightWater()/p.globMaxAra[myConsts.H2O]);}
    				//else {											   						gui.setColorValFill(myGui.gui_WaterColor);}//set water regular color
      				
    				float ySliceDispMod = 0;
    				if(!(p.allFlagsFalse(myConsts.dispCutAwayX, myConsts.dispCutAwayZ))){//if we have slice visualisation enabled
    					if (!(p.nodeInSliceRange(h2oNodeCoords[idx].x, h2oNodeCoords[idx].z))) {   						ySliceDispMod = -200;			}
    				}//if displaying a slice
    				
    				p.vertex((float)h2oNodeCoords[idx].x, (float)h2oNodeCoords[idx].y + ySliceDispMod + .01f, (float)h2oNodeCoords[idx].z); //draw vertex of water}
    			}//for each vertex         
	    	p.endShape(PConstants.CLOSE);//end water poly
	    	p.popMatrix(); 
    		}//if drawing water, heightmap made    	
 
    	p.popStyle();
		
	}//drawWaterPoly
	/**
	 * draw sediment poly
	 */
	private void drawSedPoly(){
	   	//draw sediment poly 
		p.pushStyle();
			p.shininess(100.0f);
			p.specular(255);
       			//draw water velocity vector
    		if ((!this.h2oVShown) && p.allFlagsSet(myConsts.showH2OVelocity, myConsts.Pipes)){
    		    p.pushMatrix();  	
    			for(int incr = 0; incr < h2oNodeVs.length; ++incr ){ p.dispVectorArrow(h2oNodeVs[incr], sedNodeCoords[incr],.03, myGui.gui_White, true);}
				this.h2oVShown = true;
    	    	p.popMatrix();
    		}//if drawing water velocity vector			
			
	    		//draw surface normals of sediment - draw normals even if not showing sediment mesh
	    	if ((p.allFlagsSet(myConsts.showSEDNormals)) && (p.globMaxAra[myConsts.SED] > 0)){
	            p.pushMatrix();
	    		if(p.allFlagsSet(myConsts.vertSEDNormals)){ 
	    			for(int incr = 0; incr < sedNodeNorms.length; ++incr ){
	    														p.dispVectorArrow(sedNodeNorms[incr], sedNodeCoords[incr], .01,  myGui.gui_SedimentColor, false);}}
	    		else {							    			p.dispVectorArrow(sedFaceNorm, sedFaceCenter, .01,  myGui.gui_SedimentColor, false);}
	    	   	p.popMatrix();
	    	}//if showing water normals  			

	   		//draw sediment source vector
   			if ((!this.sedSrcVShown)  && p.allFlagsSet(myConsts.dispSedSrcVecs)) {//if we're showing the source vectors of the sediment,
   				this.sedSrcVShown = true;
   				p.pushMatrix();
   				for(int incr = 0; incr < this.sedNodeSrc.length; ++incr ){
						p.dispLine(this.sedNodeSrc[incr], sedNodeCoords[incr], ((this.heightNodes[incr].isBoundaryNode()) ?  myGui.gui_White : myGui.gui_LightMagenta ));
						//p.dispVectorArrow((myVector._sub(this.sedNodeSrc[incr], this.sedNodeCoords[incr])), sedNodeCoords[incr], .01, ((this.heightNodes[incr].isBoundaryNode()) ?  myGui.gui_White : myGui.gui_LightMagenta ), false);
   				}	   	
   				p.popMatrix();
   			}//if showing sedSrcVecs and sediment exists on this node	    	
	    	
			if ((this.sedOnPoly) && (p.allFlagsSet(myConsts.sedMeshDisplay)) && (p.globMaxAra[myConsts.SED] > 0)){ 
			   	p.pushMatrix();    	
				p.beginShape(p.polyType);
	        
				if (p.allFlagsSet(myConsts.strokeDisplaySED)){gui.setColorValStroke(myGui.gui_SedimentColor);} else {p.noStroke();}
				for(int idx = 0; idx < sedNodeNorms.length; ++idx){
					if(!p.anyFlagsSet(myConsts.vertSEDNormals)){p.normal((float)sedFaceNorm.x, (float)sedFaceNorm.y, (float)sedFaceNorm.z);}
					else { 										p.normal((float)sedNodeNorms[idx].x, (float)sedNodeNorms[idx].y, (float)sedNodeNorms[idx].z);}
						//if boundary node set color to cyan
					gui.setColorValFill(myGui.gui_SedimentColor);//set sediment boundary nodes magenta 
							
					float ySliceDispMod = 0;
					if(!(p.allFlagsFalse(myConsts.dispCutAwayX, myConsts.dispCutAwayZ))){//if we have slice visualisation enabled
						if (!(p.nodeInSliceRange(sedNodeCoords[idx].x, sedNodeCoords[idx].z))) {   						ySliceDispMod = -200;			}
					}//if displaying a slice
					
					p.vertex((float)sedNodeCoords[idx].x, (float)sedNodeCoords[idx].y + ySliceDispMod, (float)sedNodeCoords[idx].z); //draw vertex of water}
				}//for each vertex         
				p.endShape(PConstants.CLOSE);//end water poly
				p.popMatrix(); 
			}//if drawing sediment, heightmap made    	
		p.popStyle();
	}//drawSedPoly
	
	/**
	 * draw poly representing sediment capacity
	 */
	private void drawSedCapPoly(){
	   	//draw sediment poly 
		p.pushStyle();
			p.shininess(100.0f);
			p.specular(255);
	   			//draw water velocity vector
			if (p.allFlagsSet(myConsts.showH2OVelocity, myConsts.Pipes) && (!this.h2oVShown)){
			    p.pushMatrix();  	
				for(int incr = 0; incr < h2oNodeVs.length; ++incr ){ p.dispVectorArrow(h2oNodeVs[incr], sedCapNodeCoords[incr],.03, myGui.gui_White, false);}
				this.h2oVShown = true;
		    	p.popMatrix();
			}//if drawing water velocity vector					
				//draw surface normals of sediment - draw normals even if not showing sediment mesh
	    	if ((this.sedCapOnPoly) && (p.allFlagsSet(myConsts.showSedCapNormals)) && (p.globMaxAra[myConsts.SEDCAP] > 0)){
	            p.pushMatrix();
	    		if(p.allFlagsSet(myConsts.vertSedCapNormals)){ 
	    			for(int incr = 0; incr < sedCapNodeNorms.length; ++incr ){
	    														p.dispVectorArrow(sedCapNodeNorms[incr], sedCapNodeCoords[incr], .01,  myGui.gui_SedCapColor, false);}}
	    		else {							    			p.dispVectorArrow(sedCapFaceNorm, sedCapFaceCenter, .01,  myGui.gui_SedCapColor, false);}
	    	   	p.popMatrix();
	    	}//if showing sedcap normals  				

	    	//draw sediment source vector
   			if ((!this.sedSrcVShown)  && p.allFlagsSet(myConsts.dispSedSrcVecs)) {//if we're showing the source vectors of the sediment,
   				this.sedSrcVShown = true;
   				p.pushMatrix();
   				for(int incr = 0; incr < this.sedNodeSrc.length; ++incr ){
					p.dispLine(this.sedNodeSrc[incr], sedCapNodeCoords[incr], ((this.heightNodes[incr].isBoundaryNode()) ?  myGui.gui_White : myGui.gui_LightMagenta ));
   					//p.dispVectorArrow((myVector._sub(this.sedNodeSrc[incr], this.sedCapNodeCoords[incr])), sedCapNodeCoords[incr], .01, myGui.gui_LightMagenta, false);
   				}	   	
   				p.popMatrix();
   			}//if showing sedSrcVecs and sediment exists on this node	 	
			if ((this.sedCapOnPoly) && (p.allFlagsSet(myConsts.sedCapMeshDisplay)) && (p.globMaxAra[myConsts.SEDCAP] > 0)){ 
			   	p.pushMatrix();    	
				p.beginShape(p.polyType);
	        
				if (p.allFlagsSet(myConsts.strokeDisplaySedCap)){gui.setColorValStroke(myGui.gui_SedCapColor);} else {p.noStroke();}
				for(int idx = 0; idx < sedCapNodeNorms.length; ++idx){
					if(!p.anyFlagsSet(myConsts.vertSedCapNormals)){p.normal((float)sedCapFaceNorm.x, (float)sedCapFaceNorm.y, (float)sedCapFaceNorm.z);}
					else { 											p.normal((float)sedCapNodeNorms[idx].x, (float)sedCapNodeNorms[idx].y, (float)sedCapNodeNorms[idx].z);}
						//if boundary node set color to cyan
					gui.setColorValFill(myGui.gui_SedCapColor);//set sediment boundary nodes magenta 
							
					float ySliceDispMod = 0;
					if(!(p.allFlagsFalse(myConsts.dispCutAwayX, myConsts.dispCutAwayZ))){//if we have slice visualisation enabled
						if (!(p.nodeInSliceRange(sedCapNodeCoords[idx].x, sedCapNodeCoords[idx].z))) {   						ySliceDispMod = -200;			}
					}//if displaying a slice
					
					p.vertex((float)sedCapNodeCoords[idx].x, (float)sedCapNodeCoords[idx].y + ySliceDispMod, (float)sedCapNodeCoords[idx].z); //draw vertex of water}
				}//for each vertex         
				p.endShape(PConstants.CLOSE);//end water poly
				p.popMatrix(); 
			}//if drawing sediment, heightmap made
		p.popStyle();
	}//drawSedCapPoly	

	/**
	 * draw poly representing sediment concentration 
	 */
	private void drawSedConcPoly(){
	   	//draw sediment poly 
		p.pushStyle();
			p.shininess(100.0f);
			p.specular(255);
	   			//draw water velocity vector
			if (p.allFlagsSet(myConsts.showH2OVelocity, myConsts.Pipes) && (!this.h2oVShown)){
			    p.pushMatrix();  	
				for(int incr = 0; incr < h2oNodeVs.length; ++incr ){ p.dispVectorArrow(h2oNodeVs[incr], sedConcNodeCoords[incr],.03, myGui.gui_White, false);}
				this.h2oVShown = true;
		    	p.popMatrix();
			}//if drawing water velocity vector					
				//draw surface normals of sediment - draw normals even if not showing sediment mesh
	    	if ((this.sedConcOnPoly) && (p.allFlagsSet(myConsts.showSedConcNormals)) && (p.globMaxAra[myConsts.SEDCONC] > 0)){
	            p.pushMatrix();
	    		if(p.allFlagsSet(myConsts.vertSedConcNormals)){ 
	    			for(int incr = 0; incr < sedConcNodeNorms.length; ++incr ){
	    														p.dispVectorArrow(sedConcNodeNorms[incr], sedConcNodeCoords[incr], .01,  myGui.gui_SedConcColor, false);}}
	    		else {							    			p.dispVectorArrow(sedConcFaceNorm, sedConcFaceCenter, .01,  myGui.gui_SedConcColor, false);}
	    	   	p.popMatrix();
	    	}//if showing sedConc normals  				

	    	//draw sediment source vector
   			if ((!this.sedSrcVShown)  && p.allFlagsSet(myConsts.dispSedSrcVecs)) {//if we're showing the source vectors of the sediment,
   				this.sedSrcVShown = true;
   				p.pushMatrix();
   				for(int incr = 0; incr < this.sedNodeSrc.length; ++incr ){
					p.dispLine(this.sedNodeSrc[incr], sedConcNodeCoords[incr], ((this.heightNodes[incr].isBoundaryNode()) ?  myGui.gui_White : myGui.gui_LightMagenta ));
   					//p.dispVectorArrow((myVector._sub(this.sedNodeSrc[incr], this.sedConcNodeCoords[incr])), sedConcNodeCoords[incr], .01, myGui.gui_LightMagenta, false);
   				}	   	
   				p.popMatrix();
   			}//if showing sedSrcVecs and sediment exists on this node	 	
			if ((this.sedConcOnPoly) && (p.allFlagsSet(myConsts.sedConcMeshDisplay)) && (p.globMaxAra[myConsts.SEDCONC] > 0)){ 
			   	p.pushMatrix();    	
				p.beginShape(p.polyType);
	        
				if (p.allFlagsSet(myConsts.strokeDisplaySedConc)){gui.setColorValStroke(myGui.gui_SedConcColor);} else {p.noStroke();}
				for(int idx = 0; idx < sedConcNodeNorms.length; ++idx){
					if(!p.anyFlagsSet(myConsts.vertSedConcNormals)){p.normal((float)sedConcFaceNorm.x, (float)sedConcFaceNorm.y, (float)sedConcFaceNorm.z);}
					else { 											p.normal((float)sedConcNodeNorms[idx].x, (float)sedConcNodeNorms[idx].y, (float)sedConcNodeNorms[idx].z);}
						//if boundary node set color to cyan
					gui.setColorValFill(myGui.gui_SedConcColor);//set sediment boundary nodes magenta 
							
					float ySliceDispMod = 0;
					if(!(p.allFlagsFalse(myConsts.dispCutAwayX, myConsts.dispCutAwayZ))){//if we have slice visualisation enabled
						if (!(p.nodeInSliceRange(sedConcNodeCoords[idx].x, sedConcNodeCoords[idx].z))) {   						ySliceDispMod = -200;			}
					}//if displaying a slice
					
					p.vertex((float)sedConcNodeCoords[idx].x, (float)sedConcNodeCoords[idx].y + ySliceDispMod, (float)sedConcNodeCoords[idx].z); //draw vertex of water}
				}//for each vertex         
				p.endShape(PConstants.CLOSE);//end water poly
				p.popMatrix(); 
			}//if drawing sediment, heightmap made
		p.popStyle();
	}//drawSedConcPoly		
	
	//draw mesh poly representing debug values
	private void drawDebugNode(double[] yVal, int guiColor){
		p.pushStyle();
		p.pushMatrix();
			gui.setColorValStroke(guiColor);
			p.beginShape(p.polyType);
			for(int idx = 0; idx < this.numVerts; ++idx){
				myVertex tmpVert = this.verts[idx];
				p.vertex((float)tmpVert.getX(), (float)yVal[idx], (float)tmpVert.getZ());					
			}		
			p.endShape(PConstants.CLOSE);
		p.popMatrix();
		p.popStyle();
	}
	
	//draw line linking wrap nodes - source is high, wrap node is low
	private void drawDebugVectors(myVector[] drawToCoords, int guiColor){
		p.pushStyle();
		p.pushMatrix();
			//gui.setColorValStroke(guiColor);
			for(int idx = 0; idx < this.numVerts; ++idx){
				//myHeightNode tmpNode = this.heightNodes[idx];
				myVertex tmpVert = this.heightNodes[idx].source;
				if ((tmpVert.isRainOnMe()) && (!tmpVert.getCoords().equals(drawToCoords[idx]))){
					gui.setColorValStroke(myGui.gui_Blue);
					p.line((float) tmpVert.getX(), 15,(float) tmpVert.getZ(), (float) tmpVert.getX(), (float)tmpVert.getY() ,(float) tmpVert.getZ());
					gui.setColorValStroke(myGui.gui_Green);						
					p.line((float) tmpVert.getX(), 15,(float) tmpVert.getZ(), (float) drawToCoords[idx].x, - 15, (float) drawToCoords[idx].z);
					gui.setColorValStroke(myGui.gui_Red);
					p.line((float) drawToCoords[idx].x, (float) drawToCoords[idx].y, (float) drawToCoords[idx].z, (float) drawToCoords[idx].x, - 15, (float) drawToCoords[idx].z);
				}
			}		
		p.popMatrix();
		p.popStyle();
	}//drawDebugVectors
	
	/**
	 * draw terrain polygon corresponding to this face
	 */
	private void drawTerrainPoly(){
		myVertex tmpVert; 
		p.pushStyle();
    	p.pushMatrix();

		if(p.allFlagsSet(myConsts.useMatlabFESolve) && p.allFlagsSet(myConsts.dispMatLabFEResults) && (null != this.p.mat)){ 	//display results of finite element calculation	vecFEMColorFill
			p.pushMatrix();
			p.pushStyle();
				p.stroke(120,120,120);
				p.beginShape(p.polyType);
				double terrVal;
			    for(int idx = 0; idx < this.numVerts; ++idx){
			    	tmpVert = this.verts[idx];			    	
			    	terrVal = ((p.globMaxAra[myConsts.FEMval] - p.globMinAra[myConsts.FEMval]) == 0 ?  1 : 
							(this.heightNodes[idx].getMatFEMval() - p.globMinAra[myConsts.FEMval])/(p.globMaxAra[myConsts.FEMval] - p.globMinAra[myConsts.FEMval]) ); 
			    	gui.setFemValFill(terrVal, (p.allFlagsSet(myConsts.terrainMeshDisplay) ? 255 : 180));
					float ySliceDispMod = 0;
	    			if(!(p.allFlagsFalse(myConsts.dispCutAwayX, myConsts.dispCutAwayZ))){//if we have slice visualisation enabled
	    				if (!(p.nodeInSliceRange(this.heightNodes[idx].source.coords.x, this.heightNodes[idx].source.coords.z))) {  ySliceDispMod = -200;}
	    			}//if displaying a slice;
			    	p.vertex((float)tmpVert.coords.x, (float)(0.1f + (p.allFlagsSet(myConsts.terrainMeshDisplay) ? tmpVert.coords.y : 0 ) + ySliceDispMod) , (float)tmpVert.coords.z);
			    }//for each vert		
			    p.endShape(PConstants.CLOSE);
			p.popStyle();
			p.popMatrix();
		}//for fem code   	
    	
	    	if(p.allFlagsSet(myConsts.strokeDisplayTerrain)) {p.stroke(120,120,120); } else {p.noStroke();};//draw lines around terrain poly
	    	if(p.allFlagsSet(myConsts.terrainMeshDisplay)){p.beginShape(p.polyType);}//draw terrain poly 
	    		//if draw not using vertex normals, set the normal to be the face normal
			if (!p.anyFlagsSet(myConsts.vertTerrainNormals)){ this.calcNormal(true); p.normal((float)this.N.x, (float)this.N.y, (float)this.N.z);} 
	
		    for(int idx = 0; idx < this.numVerts; ++idx){
		    	tmpVert = this.verts[idx];
		    		//if using vertex normal, set normal for every vertex as vert list being listed
	    		if (p.allFlagsSet(myConsts.vertTerrainNormals)){tmpVert.calcNormal(); p.normal((float)tmpVert.N.x, (float)tmpVert.N.y, (float)tmpVert.N.z);}
	    		
    			if((p.allFlagsSet(myConsts.showWrapNodes)) && (this.isWrapNode[idx])){gui.setColorValFill(myGui.gui_White);} 
    			else {
    				double terrVal = ((p.globMaxAra[myConsts.COORD_Y] - p.globMinAra[myConsts.COORD_Y]) == 0 ?  1 : (tmpVert.coords.y - p.globMinAra[myConsts.COORD_Y])/(p.globMaxAra[myConsts.COORD_Y] - p.globMinAra[myConsts.COORD_Y]) ); 
    				gui.setTerrainColorFill(terrVal,(p.allFlagsSet(myConsts.dispMatLabFEResults) ? 50 : 255));
    			}
    			//else {gui.setColorValFill(myGui.gui_TerrainColor);}	    
    				//display colorizations of wrap around and x/z coords for debugging, if not using slice display
	    		if(p.allFlagsSet(myConsts.debugModeCnsl) && p.allFlagsFalse(myConsts.dispCutAwayX, myConsts.dispCutAwayZ)){//show wrap-around edges on graphs that have them, leaving edge in green, receiving edge in red
	    			if (this.heightNodes[idx] == null) {
	    				double terrVal = ((p.globMaxAra[myConsts.COORD_Y] - p.globMinAra[myConsts.COORD_Y]) == 0 ?  1 : (tmpVert.coords.y - p.globMinAra[myConsts.COORD_Y])/(p.globMaxAra[myConsts.COORD_Y] - p.globMinAra[myConsts.COORD_Y]) ); 
	    				gui.setTerrainColorFill(terrVal,(p.allFlagsSet(myConsts.dispMatLabFEResults) ? 50 : 255));
	    				//gui.setColorValFill(myGui.gui_TerrainVertColor);
	    				}			//no node, only displaying vert mesh    			
	    			else {
    					if (this.heightNodes[idx].isMirrorNode(0)){//e-w mirror node
    						if (this.heightNodes[idx].getMirrorNodeDir(0) == myHeightNode.W){	gui.setColorValFill(myGui.gui_Magenta);		} //all wrapAroundPipes nodes will be mirror nodes and have mirror nodes, so only 1 check necessary
    						else if (this.heightNodes[idx].getMirrorNodeDir(0) == myHeightNode.E){	gui.setColorValFill(myGui.gui_Green);		} //all wrapAroundPipes nodes will be mirror nodes and have mirror nodes, so only 1 check necessary
    					}
    					else if(this.heightNodes[idx].isMirrorNode(1)){gui.setColorValFill(myGui.gui_Cyan);		}
    					else {
    						p.fill((float)((tmpVert.xIDX/(1.0 * eng.heightMapAra.length-1)) * 255), (float)((tmpVert.zIDX/(1.0 * eng.heightMapAra[0].length-1)) * 200),0.0f);		//low x vals black, high x vals red
    						p.ambient((float)((tmpVert.xIDX/(1.0 * eng.heightMapAra.length-1))* 255), (float)((tmpVert.zIDX/(1.0 *eng.heightMapAra[0].length-1))*200),0.0f);	//low z vals black, high z vals green					
    					}			//if WrapAroundPipesNeighbor
	    			}//if node null else
	    		}//if debugModeCnsl
	    		
	    		if(p.allFlagsSet(myConsts.showBoundaryNodes) && (this.heightNodes[idx].boundaryDryNode)){gui.setColorValFill(myGui.gui_Green); 	}
	    			//draw vertex of this terrain face at appropriate position, if we have terrain polys enabled
    			if(this.nonErodingNode[idx]){gui.setColorValFill(myGui.gui_BoulderColor); gui.setColorValFill(myGui.gui_BoulderColor);}

    			if(p.allFlagsSet(myConsts.terrainMeshDisplay)){  
	    			float ySliceDispMod = 0;
	    			if(!(p.allFlagsFalse(myConsts.dispCutAwayX, myConsts.dispCutAwayZ))){//if we have slice visualisation enabled
	    				if (!(p.nodeInSliceRange(this.heightNodes[idx].source.coords.x, this.heightNodes[idx].source.coords.z))) {  ySliceDispMod = -200;}
	    			}//if displaying a slice;
	    			p.vertex((float)tmpVert.coords.x, (float)tmpVert.coords.y + ySliceDispMod, (float)tmpVert.coords.z);
	    		}
			//print2Cnsl(this.toString());
	    	}//for each vert
		    	    	
	    	if(p.allFlagsSet(myConsts.terrainMeshDisplay)){p.endShape(PConstants.CLOSE);}//draw terrain poly	    		    		
    	p.popMatrix();
    	p.popStyle();
    	p.pushMatrix();
	    	//draw surface normals of terrain
	    	if (p.allFlagsSet(myConsts.terrainMeshDisplay,myConsts.showTerrainNormals)){
	    		if(p.allFlagsSet(myConsts.vertTerrainNormals)){ for(int incr = 0; incr < h2oNodeVs.length; ++incr ){
	    														p.dispVectorArrow(this.nodeNorms[incr], nodeCs[incr], .01, myGui.gui_Yellow, false);}}
	    		else {							    			p.dispVectorArrow(this.N, this.center, .01, myGui.gui_Yellow, false);}
	    	}//if showing terrain normals    	
    	p.popMatrix();
	}//drawTerrainPoly

	/**
	 * will display a poly displaced around terrain by an amount equivalent to the size of k or the leftover sediment at a particular node from the pipes implementation of flux calclulations
	 * @param nodeVals
	 * @param nodeLocs
	 * @param colorDisp
	 * @param use1high
	 * @param changeColor whether the color changes for each idx - used when displaying polys for each vert (such as pipe length) where each corner of the poly corresponds to a different node value
	 * @param usePassedHeight
	 * @param useScaledColor whether or not to show different colors based on positive or negative values
	 */
	private void dispDebugValPoly(double[] nodeVals, myVector[] nodeLocs, int colorDisp, boolean use1high, boolean changeColor, boolean usePassedHeight, boolean useScaledColor ){
		double ySliceDispMod = 0;
		double yMult =  p.globMaxAra[myConsts.H2O] *  ((useScaledColor) ? p.vVecScaleFact : 1);

		p.pushStyle();
		p.pushMatrix();
			p.noStroke();
			p.beginShape(p.polyType);
			if(!changeColor){
			
				//gui.setColorValStroke(colorDisp);
				gui.setColorValFill(colorDisp);
			}
			for(int idx = 0; idx < nodeVals.length; ++idx){	
				if (nodeLocs[idx] == null) {
					gui.print2Cnsl("Display debug val : " + use1high +"|"+changeColor +"|"+ usePassedHeight +"|"+ useScaledColor );
					p.popMatrix();
					p.popStyle();	
					return;
				}
				if(changeColor){
				//	gui.setColorValStroke(colorDisp + idx);
					gui.setColorValFill(colorDisp + idx);
				}
						//make node with 0 value transparent black
				if(useScaledColor) {
					if(nodeVals[idx] == 0){				gui.setColorValFill(myGui.gui_Black);}
					else if (nodeVals[idx] < 0) {		gui.setColorValFill(myGui.gui_TransMagenta);} //more sediment is pulled than is at nod
					else {								gui.setColorValFill(myGui.gui_TransYellow);}//sediment left over						
				}
			
				if(!(p.allFlagsFalse(myConsts.dispCutAwayX, myConsts.dispCutAwayZ))){//if we have slice visualisation enabled
					if (!(p.nodeInSliceRange(nodeLocs[idx].x, nodeLocs[idx].z))) {  ySliceDispMod = -200;}
					else {ySliceDispMod = 0;}
				}
				if (usePassedHeight){ 
					float yVal = (float) (ySliceDispMod +  nodeLocs[idx].y);
					if (yVal == 0) { 
					//	gui.setColorValStroke(myGui.gui_TransBlack );
						gui.setColorValFill(myGui.gui_TransBlack);				
					}
					p.vertex((float) nodeLocs[idx].x, yVal, (float) nodeLocs[idx].z);
				} else {
					if (use1high){	p.vertex((float) nodeLocs[idx].x, (float) (ySliceDispMod + ( (nodeVals[idx] == 0) ? 0 : (1-nodeVals[idx]) * p.globMaxAra[myConsts.H2O])), (float) nodeLocs[idx].z);}
					else {			p.vertex((float) nodeLocs[idx].x, (float) (ySliceDispMod + nodeVals[idx] * yMult), (float) nodeLocs[idx].z);}
				}
			}//for
			p.endShape(PConstants.CLOSE);
		p.popMatrix();
		p.popStyle();	
	}//dispKNode
	
	
	/**
	 * display a polygon for each vertex/node showing debug data for particular vector quantity at each of poly's verts
	 * mainly used to show adjacent node debug data for each vertex/node
	 */
	private void dispVecValPoly(double[] nodeVals, myVector[] nodeLocs, int colorDisp, boolean use1high, boolean changeColor){
		double ySliceDispMod = 0;

		p.pushStyle();
		p.pushMatrix();
			p.noStroke();
			p.beginShape(p.polyType);
			if(!changeColor){			
				//gui.setColorValStroke(colorDisp);
				gui.setColorValFill(colorDisp);
			}
			for(int idx = 0; idx < nodeVals.length; ++idx){	
				if(changeColor){
				//	gui.setColorValStroke(colorDisp + idx);
					gui.setColorValFill(colorDisp + idx);
				}
				if(!(p.allFlagsFalse(myConsts.dispCutAwayX, myConsts.dispCutAwayZ))){//if we have slice visualisation enabled
					if (!(p.nodeInSliceRange(nodeLocs[idx].x, nodeLocs[idx].z))) {  ySliceDispMod = -200;}
					else {ySliceDispMod = 0;}
				}
				float yVal = (float) (ySliceDispMod +  nodeLocs[idx].y);
				if (yVal == 0) { 
				//	gui.setColorValStroke(myGui.gui_TransBlack );
					gui.setColorValFill(myGui.gui_TransBlack);				
				}
				p.vertex((float) nodeLocs[idx].x, yVal, (float) nodeLocs[idx].z);
			}//for
			p.endShape(PConstants.CLOSE);
		p.popMatrix();
		p.popStyle();	
	}//dispKNode	
		
	/**
	 * reinitialize all relevant arrays and values for displaying various quantities
	 */
	private void reInitDispVars(){
		this.heightNodes 		            = new myHeightNode[this.numVerts];						//the height nodes that correspond with this face's verts, if they exist
		this.boundaryH2ONodes 	            = new boolean[this.numVerts];							//whether or not any of the nodes of this face are boundary nodes 	
		this.isWrapNode						= new boolean[this.numVerts];
		this.nonErodingNode					= new boolean[this.numVerts];
		this.noH2ONodesByIDX	            = new boolean[this.numVerts];							//whether a particular node in a poly has water or not	
		this.nodeNorms 			            = new myVector[this.numVerts];      					//normals at each node
		this.nodeCs 			            = new myVector[this.numVerts];							//centers of each node 
				                                    
		this.origMeshNodeCoords 	        = new myVector[this.numVerts];  						//x,y,z coords of original mesh, before erosion
		this.oldMeshNodeCoords  	        = new myVector[this.numVerts];  						//x,y,z coords of original mesh, before erosion
		this.h2oNodeCoords 		            = new myVector[this.numVerts];  						//x,y,z coords of each of 4 node of water polys
		this.h2oNodeNorms 		            = new myVector[this.numVerts];  						//x,y,z coords of each of 4 normals for the mode of a water poly
		this.h2oNodeVs 			            = new myVector[this.numVerts];							//water/sediment velocity vectors at each node
		                                    
		this.sedNodeCoords		            = new myVector[this.numVerts];  	
    	this.sedNodeNorms		            = new myVector[this.numVerts];	
    	                                    
    	this.sedCapNodeCoords	            = new myVector[this.numVerts];
    	this.sedCapNodeNorms	            = new myVector[this.numVerts];		
                                                                             
    	this.sedConcNodeCoords              = new myVector[this.numVerts];       
    	this.sedConcNodeNorms               = new myVector[this.numVerts];		 
     	
		this.kNodeLocations		            = new myVector[this.numVerts];
    	this.kNodeVals 			            = new double[this.numVerts];							//holds the values of k for each node of this face
    	this.kNotUnity 			            = false;	
    	
    	this.sedLeftOver					= false;												//whether sediment is not transported from any node in this face
    	this.sedLeftOverLocs				= new myVector[this.numVerts];							//coords of node that has sediment left over after transport
    	this.sedLeftOverAmt					= new double[this.numVerts];
                                            
    	this.sedNodeSrc			            = new myVector[this.numVerts];							//coords of source of sediment
    	    	
    	this.outFluxCoords                  = new myVector[this.numVerts][4];						//each of 4 directions for each of the vertexes of this face
    	this.outFluxVals		            = new double[this.numVerts][4];	
    	                                    
    	this.pipeLengthCoords  	            = new myVector[this.numVerts][4];						//each of 4 directions for each of the vertexes of this face
    	this.pipeLengths	 	            = new double[this.numVerts][4];	
    	
    	this.adjNodeHeightDiffsCoords  		= new myVector[this.numVerts][4];						//each of 4 directions for each of the vertexes of this face
    	this.adjNodeHeightDiffs	 			= new double[this.numVerts][4];	

    	this.wrapToNodeCoords  				= new myVector[this.numVerts];

//    	this.tiltComponentVals  			= new double[this.numVerts];
       	this.numAdjNodes 					= new double[this.numVerts];

    	this.waterOnPoly 					= false;
    	this.sedOnPoly						= false;
    	this.sedCapOnPoly					= false;
    	this.sedConcOnPoly					= false;
    	this.h2oVShown						= false;
    	this.sedSrcVShown					= false;
    	
     	this.noH2ONodes 					= 0;
		this.noSEDNodes 					= 0;
		this.noSEDCAPNodes					= 0;     	
       	this.noSEDCONCNodes					= 0;
       	
		this.h2oFaceCenter 					= new myVector(0,0,0);
		this.sedFaceCenter					= new myVector(0,0,0);
		this.sedCapFaceCenter				= new myVector(0,0,0);		
		this.sedConcFaceCenter				= new myVector(0,0,0);
	
	}//reInitDispVars
	
	/**
	 * returns whether we should draw vertex/node data for debugging depending on which vert the current vert is in the face ara, and where it lies
	 * on the mesh based on its location in a particular face.  this is we we draw each vert/node once and only once, for the entire mesh
	 * @param idx the index of the vert/node in the current face
	 * @param nodeCoords the coordinates of the vert
	 * @return whether or not to calculate/draw a particular node's debug data based on its location
	 */
	private boolean checkValidAdjNodeByIdx(int idx, myVector nodeCoords){
		return ( 
				(((idx == 0) ||
				((idx == 1) && (nodeCoords.x == p.globMaxAra[myConsts.COORD_X]))		||
				((idx == 3) && (nodeCoords.z == p.globMaxAra[myConsts.COORD_Z])) 		||						
				((idx == 2) && (nodeCoords.x == p.globMaxAra[myConsts.COORD_X]) && (nodeCoords.z == p.globMaxAra[myConsts.COORD_Z])) ))	
			);
	}//checkValidAdjNodeByIdx
	
	/**
	 * this will return a particular debug quantity from a vert/node based on the type int passed
	 * @param idx the index in the current heightNodes ara
	 * @param adjNodeID the id of the adjacent node to this node
	 * @param type the type of debug data being requested
	 * @return the debug data
	 */
	private double getAdjValByType(int idx, int adjNodeID, int type){
		switch (type) {
		case 0 : { return this.heightNodes[idx].getPipeLength(adjNodeID);}
		case 1 : { return this.heightNodes[idx].getAdjHeightDiffsByIDX(adjNodeID);}
		default : {return 0;}
		}//switch
	}//getAdjValByType
	
	/**
	 * for debug purposes, find every adj node to particular node and record specific values to display
	 * @param idx index in vert list of current vert/node
	 * @param nodeCoords coords of current vert/node
	 * @param globVecAra global array holding vector data (coords) of particular debug values for each adj node to current node 
	 * @param globValAra global array holding scalar debug data for each adj node to current node
	 * @param type of vector value being recorded : 0 = pipe lengths, 1 = height differences
	 */
	private void buildAdjNodeVecVals(int idx, myVector nodeCoords, myVector[][] globVecAra, double[][] globValAra, int type){
		myHeightNode adjHNodeE = this.heightNodes[idx].getAdjNodesByPassedDir(myHeightNode.E),
				adjHNodeW = this.heightNodes[idx].getAdjNodesByPassedDir(myHeightNode.W),
				adjHNodeN = this.heightNodes[idx].getAdjNodesByPassedDir(myHeightNode.N),
				adjHNodeS = this.heightNodes[idx].getAdjNodesByPassedDir(myHeightNode.S);	
		double deltaNodeDist = 1;//sub.getDeltaNodeDist();			//: eventually remove once deltaNodeDist locked to 1
		
		if(adjHNodeE != null){
//			dispDebugValsForNode(this.heightNodes[idx], adjHNodeE, myHeightNode.E, "East");
			globValAra[idx][myHeightNode.E] = getAdjValByType(idx,adjHNodeE.ID,type);
			globVecAra[idx][myHeightNode.E] = new myVector(nodeCoords.x + (.5f * deltaNodeDist), globValAra[idx][myHeightNode.E] * .5 * p.vVecScaleFact, nodeCoords.z);
		} else {
			globValAra[idx][myHeightNode.E] = 0;
			globVecAra[idx][myHeightNode.E] = new myVector(nodeCoords.x + (.5f * deltaNodeDist), nodeCoords.y, nodeCoords.z);							
		}
		if(adjHNodeW != null){
//			dispDebugValsForNode(this.heightNodes[idx], adjHNodeW, myHeightNode.W, "West");
			globValAra[idx][myHeightNode.W] = getAdjValByType(idx,adjHNodeW.ID,type); 
			globVecAra[idx][myHeightNode.W] = new myVector(nodeCoords.x - (.5f * deltaNodeDist), globValAra[idx][myHeightNode.W] * .5 * p.vVecScaleFact, nodeCoords.z);
		} else {
			globValAra[idx][myHeightNode.W] = 0;
			globVecAra[idx][myHeightNode.W] = new myVector(nodeCoords.x - (.5f * deltaNodeDist), nodeCoords.y, nodeCoords.z);							
		}
		
		if(adjHNodeN != null){
//			dispDebugValsForNode(this.heightNodes[idx], adjHNodeN, myHeightNode.N, "North");
			globValAra[idx][myHeightNode.N] = getAdjValByType(idx,adjHNodeN.ID,type); 
			globVecAra[idx][myHeightNode.N] = new myVector(nodeCoords.x, 							globValAra[idx][myHeightNode.N] * .5 * p.vVecScaleFact, nodeCoords.z + (.5f * deltaNodeDist));
		} else {
			globValAra[idx][myHeightNode.N] = 0;
			globVecAra[idx][myHeightNode.N] = new myVector(nodeCoords.x, 							nodeCoords.y, nodeCoords.z + (.5f * deltaNodeDist));							
		}
		
		if(adjHNodeS != null){
//			dispDebugValsForNode(this.heightNodes[idx], adjHNodeS, myHeightNode.S, "South");
			globValAra[idx][myHeightNode.S] = getAdjValByType(idx,adjHNodeS.ID,type); 
			globVecAra[idx][myHeightNode.S] = new myVector(nodeCoords.x, 							globValAra[idx][myHeightNode.S] * .5 * p.vVecScaleFact, nodeCoords.z - (.5f * deltaNodeDist));
		} else {
			globValAra[idx][myHeightNode.S] = 0;
			globVecAra[idx][myHeightNode.S] = new myVector(nodeCoords.x, 							nodeCoords.y, nodeCoords.z - (.5f * deltaNodeDist));							
		}		
	}//buildAdjNodeVecVals
	
//	private void dispDebugValsForNode(myHeightNode srcNode, myHeightNode adjNode, int dir, String dirStr){
//		if((srcNode.ID == 1579) || (srcNode.ID == 19) || (srcNode.ID == 59) ){
//			gui.print2Cnsl(""+srcNode.ID + "'s " + dirStr + " node : " + adjNode.ID + " size of adj nodes : " + srcNode.getAdjNodes().size());
//			gui.print2Cnsl("length of pipe to " + dirStr.charAt(0) + " neighbor : " + srcNode.getPipeLength(adjNode.ID ));
//			gui.print2Cnsl("height diff to " + dirStr.charAt(0) + " neighbor : " + srcNode.getAdjHeightDiffsByDir(dir));
//		}
//	}//dispDebugVals

	/**
	 * build arrays holding debug-related values
	 * @param idx index of current vertex
	 * @param tmpVert current vertex
	 */
	private void buildDebugAraVals(int idx, myVertex tmpVert){
		//debug-related values
		myVector nodeCoords = new myVector(tmpVert.getX(),tmpVert.getY(),tmpVert.getZ());
			//visualizing k in pipes sim
		if (p.allFlagsSet(myConsts.dispNon1KVals)) {
			this.kNodeLocations[idx] = new myVector(nodeCoords);		//get a copy of this vert, always put in array in case a later node on this face has a non-zero k			
			if (this.heightNodes[idx].iskScaledNode()){
				this.kNotUnity = true;
				this.kNodeVals[idx] = this.heightNodes[idx].getkVal();
			}//if isscalednode
		}//if showing k

			//visualizing nodes with left over sediment after sediment transport stage
		if (p.allFlagsSet(myConsts.dispSedTransLeftoverVals)){
			this.sedLeftOverLocs[idx] = new myVector(nodeCoords);	
			if (this.heightNodes[idx].getSedLeftOver() != 0){
				this.sedLeftOver = true;
				this.sedLeftOverAmt[idx] = this.heightNodes[idx].getSedLeftOver();
			}
		}
		
		if ((p.allFlagsSet(myConsts.dispFluxVals)) && checkValidAdjNodeByIdx(idx, nodeCoords)){		
			//build 4-element array for each vertex representing the flux in each of 4 directions - only build 1 element so we don't rebuild verts over and over
				//x decreases to west, z decreases to south
			this.outFluxVals[idx] = this.heightNodes[idx].getFlux()[0];
			double deltaNodeDist = 1;//sub.getDeltaNodeDist();			// eventually remove once deltaNodeDist locked to 1
			
			this.outFluxCoords[idx][myHeightNode.E] = new myVector(nodeCoords.x + (.5f * deltaNodeDist), outFluxVals[idx][myHeightNode.E] * .5 * p.vVecScaleFact, nodeCoords.z);
			this.outFluxCoords[idx][myHeightNode.W] = new myVector(nodeCoords.x - (.5f * deltaNodeDist), outFluxVals[idx][myHeightNode.W] * .5 * p.vVecScaleFact, nodeCoords.z);
			this.outFluxCoords[idx][myHeightNode.N] = new myVector(nodeCoords.x, 						 outFluxVals[idx][myHeightNode.N] * .5 * p.vVecScaleFact, nodeCoords.z + (.5f * deltaNodeDist));
			this.outFluxCoords[idx][myHeightNode.S] = new myVector(nodeCoords.x, 						 outFluxVals[idx][myHeightNode.S] * .5 * p.vVecScaleFact, nodeCoords.z - (.5f * deltaNodeDist));

		}//debugging, pipes mode, and we wish to display the flux values for each direction from each vertex

		if ((p.allFlagsSet(myConsts.dispPipeLengths)) && checkValidAdjNodeByIdx(idx, nodeCoords)){
			this.buildAdjNodeVecVals(idx, nodeCoords, this.pipeLengthCoords, this.pipeLengths,0);
		}//debugging, pipes mode, and we wish to display the pipe lengths for each direction from each vertex
		
		if ((p.allFlagsSet(myConsts.dispHeightDiffs)) && checkValidAdjNodeByIdx(idx, nodeCoords)){
			this.buildAdjNodeVecVals(idx, nodeCoords, this.adjNodeHeightDiffsCoords, this.adjNodeHeightDiffs,1);
		}//debugging, pipes mode, and we wish to display the pipe lengths for each direction from each vertex
					
		if (p.allFlagsSet(myConsts.dispOriginalMesh))	{}
//		{ this.tiltComponentVals[idx] = this.heightNodes[idx].getTiltComponent(); }
		if (p.allFlagsSet(myConsts.dispAdjNodes)){				this.numAdjNodes[idx] = this.heightNodes[idx].getAdjNodes().size();}
		if (p.allFlagsSet(myConsts.dispWrapToNodes)){
			if(this.heightNodes[idx].isMirrorNode(0)){
				if((eng.heightMapAra != null) && (eng.heightMapAra.length != 0) && (eng.heightMapAra[0].length !=0 )){ 
					this.wrapToNodeCoords[idx] = (this.heightNodes[idx].source.xIDX == 0) ? 
							(eng.heightMapAra[eng.heightMapAra.length-1][this.heightNodes[idx].source.zIDX].source.coords) :
							(eng.heightMapAra[0][this.heightNodes[idx].source.zIDX].source.coords);
				}
				else {   							this.wrapToNodeCoords[idx] = eng.getHNode(eng.heightMap.get(this.heightNodes[idx].getMirrorNodeID(0))).getSource().getCoords();}}
			else {									this.wrapToNodeCoords[idx] = this.heightNodes[idx].source.coords;}
		}
	}//buildDebugAraVals
	
	/**
	 * this will build and initialize the arrays of values used to display the data corresponding to this poly - terrain, water, sediment, velocity, normals, poly boundaries, etc.
	 */
	private void buildDisplayArrays(){
		double tmpYVal =  0,
				avgYValH2O = 0,
				avgYValSed = 0,
				avgYValSedCap = 0,
				avgYValSedConc = 0;
		double[] yValH2O 	= new double[this.numVerts], 
				yValSED 	= new double[this.numVerts], 
				yValSEDCAP 	= new double[this.numVerts],							
				yValSEDCONC	= new double[this.numVerts];								//amount to modify height to display various polys
		
		this.reInitDispVars();
		
    	for(int idx = 0; idx < this.numVerts; ++idx){//for each vertex in this face
    		myVertex tmpVert = this.verts[idx];
    			//get owning height node for this vertex, if exists
			this.heightNodes[idx] = tmpVert.getOwningNode();
			if((this.heightNodes[idx] != null) && (p.allFlagsSet(myConsts.heightMapMade))){//if vert has owning node and height map exists, get the nodes that correspond to the verts in this face and all other relevant node-related data
				
				if((this.heightNodes[idx].isUseLocalErosionConstants(eng.Kc)) && (this.heightNodes[idx].lclKc == 0)){ this.nonErodingNode[idx] = true;}
					//if displaying sediment or water and showing velocity, get velocity vector values for display of velocity vector lines
    			if (p.allFlagsSet(myConsts.showH2OVelocity))	{h2oNodeVs[idx] = new myVector(this.heightNodes[idx].getVelocity());	}
    				//if displaying water normals
    			if (p.allFlagsSet(myConsts.vertH2ONormals)) 	{h2oNodeNorms[idx] = this.heightNodes[idx].H2O_Norm;}
    				//if displaying sed normals    			
       			if (p.allFlagsSet(myConsts.vertSEDNormals)) 	{sedNodeNorms[idx] = this.heightNodes[idx].Sed_Norm;}
					//if displaying sed cap normals    			
				if (p.allFlagsSet(myConsts.vertSedCapNormals)) 	{sedCapNodeNorms[idx] = this.heightNodes[idx].SedCap_Norm;}
					//if displaying sed concentration normals    			
				if (p.allFlagsSet(myConsts.vertSedConcNormals)) 	{sedConcNodeNorms[idx] = this.heightNodes[idx].SedConc_Norm;}
     				//if we're showing the source vectors of the sediment, and sediment exists at this node
    			if(p.allFlagsSet(myConsts.dispSedSrcVecs))		{this.sedNodeSrc[idx] = this.heightNodes[idx].getSedSource();}
    				//if we're showing the original mesh, before erosion
    			if(p.allFlagsSet(myConsts.dispOriginalMesh))	{this.origMeshNodeCoords[idx] = this.heightNodes[idx].originalCoords;}
    				//if we're showing a previously-captured mesh's data overlayed upon current mesh
    			if((p.allFlagsSet(myConsts.dispOldMeshVals)) && (eng.compareHeightMapAraData != null) && (eng.compareHeightMapAraData.length > 0) && (eng.compareHeightMapAraData[0].length > 0)){
    				int xIdx = this.heightNodes[idx].source.xIDX,
    					zIdx = this.heightNodes[idx].source.zIDX;
     				this.oldMeshNodeCoords[idx] = new myVector(eng.compareHeightMapAraData[xIdx][zIdx]);
    			} else 
    			if((p.allFlagsSet(myConsts.dispOldMeshVals)) && (eng.oldHeightMapAraData != null) && (eng.oldHeightMapAraData.length > 0) && (eng.oldHeightMapAraData[0].length > 0)){
    				int xIdx = this.heightNodes[idx].source.xIDX,
    					zIdx = this.heightNodes[idx].source.zIDX;
    				this.oldMeshNodeCoords[idx] = new myVector(eng.oldHeightMapAraData[xIdx][zIdx]);
    			}
    		
    			if((p.allFlagsSet(myConsts.showWrapNodes)) && (this.heightNodes[idx].isMirrorNode(0))){this.isWrapNode[idx] = true;}
    				//water is not zero on mesh and we're displaying water
    			if((p.allFlagsSet(myConsts.H2OMeshDisplay)) && (p.globMaxAra[myConsts.H2O] > 0)){
					if (this.heightNodes[idx].getVisualisationVal(myConsts.H2O,0) < myConsts.epsValDisplay){//no water
						yValH2O[idx] = 0; 
						this.noH2ONodesByIDX[idx] = true;
						this.noH2ONodes++;
					} else {
							//get displacement value appropriate for amount of water/sediment at node
						yValH2O[idx] = this.heightNodes[idx].getVisualisationVal(myConsts.H2O,0);															//eventually change yval to be in direction of normal for torus
						this.waterOnPoly = true; //if no node is 0 then water on this face
						this.noH2ONodesByIDX[idx] = false;
						avgYValH2O += tmpVert.getY() + yValH2O[idx] - (10*myConsts.epsValDisplay);
					}//if no water on node, else if water on node    
					this.boundaryH2ONodes[idx] = this.heightNodes[idx].isBoundaryNode();	              				
				}//if showing water mesh and water exists on entire mesh
    			
					//sediment is not zero and we're displaying sediment
				if((p.allFlagsSet(myConsts.sedMeshDisplay)) && (p.globMaxAra[myConsts.SED] > 0)){
					if (this.heightNodes[idx].getVisualisationVal(myConsts.SED,0)  < myConsts.epsValDisplay){//no sediment at this node
						yValSED[idx] = 0; 
						this.noSEDNodes++;
					} else {
							//get displacement value appropriate for amount of water/sediment at node
						tmpYVal = this.heightNodes[idx].getVisualisationVal(myConsts.SED,0);															//eventually change yval to be in direction of normal for torus
						yValSED[idx] = p.mapD(tmpYVal, p.globMinAra[myConsts.SED], p.globMaxAra[myConsts.SED], p.globMinAra[myConsts.H2O], p.globMaxAra[myConsts.H2O]/2);
						this.sedOnPoly = true; //if no node is 0 then sed on this face
					avgYValSed +=  (tmpVert.getY() + yValSED[idx] - (10*myConsts.epsValDisplay));
					}									
				}
					//sediment capacity is not zero and we're displaying sediment capacity
				if((p.allFlagsSet(myConsts.sedCapMeshDisplay)) && (p.globMaxAra[myConsts.SEDCAP] > 0)){
					if (this.heightNodes[idx].getVisualisationVal(myConsts.SEDCAP,0)  < myConsts.epsValDisplay){//no sediment at this node
						yValSEDCAP[idx] = 0; 
						this.noSEDCAPNodes++;
					} else {
							//get displacement value appropriate for amount of water/sediment at node
						tmpYVal = this.heightNodes[idx].getVisualisationVal(myConsts.SEDCAP,0);															//eventually change yval to be in direction of normal for torus
						yValSEDCAP[idx] = p.mapD(tmpYVal, p.globMinAra[myConsts.SEDCAP], p.globMaxAra[myConsts.SEDCAP], p.globMinAra[myConsts.H2O], p.globMaxAra[myConsts.H2O]/2);
						this.sedCapOnPoly = true; //if no node is 0 then water on this face
						avgYValSedCap +=  tmpVert.getY() + yValSEDCAP[idx] - (10*myConsts.epsValDisplay);
					}					
				}
					//sediment capacity is not zero and we're displaying sediment capacity
				if((p.allFlagsSet(myConsts.sedConcMeshDisplay)) && (p.globMaxAra[myConsts.SEDCONC] > 0)){
					if (this.heightNodes[idx].getVisualisationVal(myConsts.SEDCONC,0)  < myConsts.epsValDisplay){//no sediment at this node
						yValSEDCONC[idx] = 0; 
						this.noSEDCONCNodes++;
					} else {
							//get displacement value appropriate for amount of water/sediment at node
						tmpYVal = this.heightNodes[idx].getVisualisationVal(myConsts.SEDCONC,0);															//eventually change yval to be in direction of normal for torus
						yValSEDCONC[idx] = p.mapD(tmpYVal, p.globMinAra[myConsts.SEDCONC], p.globMaxAra[myConsts.SEDCONC], p.globMinAra[myConsts.H2O], p.globMaxAra[myConsts.H2O]/2);
						this.sedConcOnPoly = true; //if no node is 0 then water on this face
						avgYValSedConc +=  tmpVert.getY() + yValSEDCONC[idx] - (10*myConsts.epsValDisplay);
					}					
				}
					//build debug-related value arrays
				if(p.allFlagsSet(myConsts.debugMode, myConsts.Pipes)){ this.buildDebugAraVals(idx, tmpVert);}

			}//if heightmap made and this vert's heightNode not null
			//now build arrays of terrain and terrain-related data
				//get normal for this vertex
			this.nodeNorms[idx] = new myVector(tmpVert.N);//make copy of normals, so we don't accidentally change them
			this.nodeCs[idx] = new myVector(tmpVert.getX(),tmpVert.getY(),tmpVert.getZ());
					
    	}//for each vertex
    	
		//calc avg y values to add to 
		if (this.noH2ONodes != 0)		{	avgYValH2O /= (1.0 * (this.verts.length - this.noH2ONodes));		}
		if (this.noSEDNodes != 0)		{	avgYValSed /= (1.0 * (this.verts.length - this.noSEDNodes));		}
		if (this.noSEDCAPNodes != 0)	{	avgYValSedCap /= (1.0 * (this.verts.length - this.noSEDCAPNodes));}    	
		if (this.noSEDCONCNodes != 0)	{	avgYValSedConc /= (1.0 * (this.verts.length - this.noSEDCONCNodes));}    	
		
		//if not displaying terrain, don't add y value to sed, sedcap or sedConc
		if(p.allFlagsFalse(myConsts.terrainMeshDisplay)){
			avgYValSed = 0;
			avgYValSedCap = 0;
			avgYValSedConc = 0;
		}
    		
    		//address nodes that have 0 water/sediment/sedcap - need to do after every vert is made
    	for(int idx = 0; idx < this.numVerts; ++idx){//for each vertex in this face    		
    		if (this.waterOnPoly){//set values for water poly face normal and center
					//the coordinate of the corresponding water/sed surface vertex if displaying h2o/sed mesh
    			if(yValH2O[idx] != 0) { 
    				this.h2oNodeCoords[idx] = new myVector(this.nodeCs[idx].x, this.nodeCs[idx].y + yValH2O[idx] - (10*myConsts.epsValDisplay), this.nodeCs[idx].z);
    			} else {
    				this.h2oNodeCoords[idx] = new myVector(this.nodeCs[idx].x, Math.min(this.nodeCs[idx].y + yValH2O[idx] - (10*myConsts.epsValDisplay), avgYValH2O), this.nodeCs[idx].z);
    			}
					//add current values to center vector, then div by num verts to find center coord of h2o/sed mesh face
				this.h2oFaceCenter._add(h2oNodeCoords[idx]);	
    		}	
    		
    		if(this.sedOnPoly){
    			double sedAmt;
					//the coordinate of the corresponding water/sed surface vertex if displaying h2o/sed mesh
    			if(yValSED[idx] != 0) { 
    				sedAmt = this.nodeCs[idx].y + yValSED[idx] - (10*myConsts.epsValDisplay);
    			} else {
    				sedAmt = Math.min(this.nodeCs[idx].y + yValSED[idx] - (10*myConsts.epsValDisplay), avgYValSed);
    			}
    			//p.allFlagsSet(myConsts.terrainMeshDisplay)
  				this.sedNodeCoords[idx] = new myVector(this.nodeCs[idx].x, (p.allFlagsSet(myConsts.terrainMeshDisplay) ? sedAmt : sedAmt -  this.nodeCs[idx].y), this.nodeCs[idx].z);
    			
					//add current values to center vector, then div by num verts to find center coord of h2o/sed mesh face
				this.sedFaceCenter._add(sedNodeCoords[idx]);						
    		}
    		
    		if(this.sedCapOnPoly){
					//the coordinate of the corresponding water/sed surface vertex if displaying h2o/sed mesh
    			if(yValSEDCAP[idx] != 0) { 
    				this.sedCapNodeCoords[idx] = new myVector( this.nodeCs[idx].x, this.nodeCs[idx].y + yValSEDCAP[idx] - (10*myConsts.epsValDisplay), this.nodeCs[idx].z);
    			} else {
    				this.sedCapNodeCoords[idx] = new myVector( this.nodeCs[idx].x, Math.min(this.nodeCs[idx].y + yValSEDCAP[idx] - (10*myConsts.epsValDisplay), avgYValSedCap), this.nodeCs[idx].z); 				
    			}
					//add current values to center vector, then div by num verts to find center coord of h2o/sed mesh face
				this.sedCapFaceCenter._add(sedCapNodeCoords[idx]);						
    		}

    		if(this.sedConcOnPoly){
					//the coordinate of the corresponding water/sed surface vertex if displaying h2o/sed mesh
    			if(yValSEDCONC[idx] != 0) { 
    				this.sedConcNodeCoords[idx] = new myVector( this.nodeCs[idx].x, this.nodeCs[idx].y + yValSEDCONC[idx] - (10*myConsts.epsValDisplay), this.nodeCs[idx].z);
    			} else {
    				this.sedConcNodeCoords[idx] = new myVector( this.nodeCs[idx].x, Math.min(this.nodeCs[idx].y + yValSEDCONC[idx] - (10*myConsts.epsValDisplay), avgYValSedConc), this.nodeCs[idx].z); 				
    			}
					//add current values to center vector, then div by num verts to find center coord of h2o/sed mesh face
				this.sedConcFaceCenter._add(sedConcNodeCoords[idx]);						
    		}
    	}//for each idx corresponding to each vertex
    	
		if (this.waterOnPoly){//set values for water poly face normal and center
			this.h2oFaceNorm = new myVector(this.calcNormalMaterialFace(myConsts.H2O));
			this.h2oFaceCenter._div(verts.length);			//set center coord for water poly face
		}	
		
		if(this.sedOnPoly){
			this.sedFaceNorm = new myVector(this.calcNormalMaterialFace(myConsts.SED));
			this.sedFaceCenter._div(verts.length);
		}
		
		if(this.sedCapOnPoly){
			this.sedCapFaceNorm = new myVector(this.calcNormalMaterialFace(myConsts.SEDCAP));
			this.sedCapFaceCenter._div(verts.length);			
		}
		if(this.sedConcOnPoly){
			this.sedConcFaceNorm = new myVector(this.calcNormalMaterialFace(myConsts.SEDCONC));
			this.sedConcFaceCenter._div(verts.length);			
		}
	}//buildDisplayArrays	

	/**
	 * display vector quantities for each node as 4-vert poly, corresponding to each of 4 potential adj nodes
	 * @param globAdjNodeVecAra
	 * @param globAdjNodeValAra
	 */
	public void dispAdjNodeDebugVecVals(myVector [][] globAdjNodeVecAra, double [][] globAdjNodeValAra){
		this.dispVecValPoly(globAdjNodeValAra[0], globAdjNodeVecAra[0], myGui.gui_Gray, false, true);
		if (this.heightNodes[1].source.coords.x == p.globMaxAra[myConsts.COORD_X]){
			this.dispVecValPoly(globAdjNodeValAra[1], globAdjNodeVecAra[1], myGui.gui_Gray, false, true);				
		}
		if (this.heightNodes[3].source.coords.z == p.globMaxAra[myConsts.COORD_Z]){
			this.dispVecValPoly(globAdjNodeValAra[3], globAdjNodeVecAra[3], myGui.gui_Gray, false, true);				
		}
		if ((this.heightNodes[2].source.coords.x == p.globMaxAra[myConsts.COORD_X]) && (this.heightNodes[2].source.coords.z == p.globMaxAra[myConsts.COORD_Z])){
			this.dispVecValPoly(globAdjNodeValAra[2], globAdjNodeVecAra[2], myGui.gui_Gray, false, true);				
		}
	}//dispPipeLengths
	
//	//dispFluxVals
//	public void dispFluxVals(){
//		this.dispDebugValPoly(this.outFluxVals[0], this.outFluxCoords[0], myGui.gui_Gray, false, true, true, false);
//		if (this.heightNodes[1].source.coords.x == p.globMaxAra[myConsts.COORD_X]){
//			this.dispDebugValPoly(this.outFluxVals[1], this.outFluxCoords[1], myGui.gui_Gray, false, true, true, false);				
//		}
//		if (this.heightNodes[3].source.coords.z == p.globMaxAra[myConsts.COORD_Z]){
//			this.dispDebugValPoly(this.outFluxVals[3], this.outFluxCoords[3], myGui.gui_Gray, false, true, true, false);				
//		}
//		if ((this.heightNodes[2].source.coords.x == p.globMaxAra[myConsts.COORD_X]) && (this.heightNodes[2].source.coords.z == p.globMaxAra[myConsts.COORD_Z])){
//			this.dispDebugValPoly(this.outFluxVals[2], this.outFluxCoords[2], myGui.gui_Gray, false, true, true, false);				
//		}
//	}//dispFluxVals
	
	/**
	*  draw this terrain face - also draws water/sediment polys on top of terrain
	*/
	public void drawMe(){
       	this.buildDisplayArrays();	
       	this.drawTerrainPoly();
		if(p.allFlagsSet(myConsts.heightMapMade)){//only draw water/sed/sedcap polys if height map already made
			if(p.allFlagsSet(myConsts.debugMode, myConsts.Pipes)){
		       	if (p.allFlagsSet(myConsts.dispAdjNodes))        {this.drawDebugNode(this.numAdjNodes, myGui.gui_White);}//debug - used to display number of adjacent nodes
	   			//if (p.allFlagsSet(myConsts.dispOriginalMesh))	{this.drawDebugNode(this.tiltComponentVals, myGui.gui_White);}
	   			if (p.allFlagsSet(myConsts.dispWrapToNodes))     {this.drawDebugVectors(this.wrapToNodeCoords, myGui.gui_White);}
		    	if (kNotUnity) {	this.dispDebugValPoly(kNodeVals, kNodeLocations, myGui.gui_KDispColor, true, false, true, false);} //display poly face representing k value on mesh
		    	if (sedLeftOver){	this.dispDebugValPoly(this.sedLeftOverAmt, this.sedLeftOverLocs, myGui.gui_Sed1DispColor, false, false, false, true);}
		    	
		    	//if (p.allFlagsSet(myConsts.dispFluxVals)){this.dispFluxVals();}
		    	if (p.allFlagsSet(myConsts.dispFluxVals)){this.dispAdjNodeDebugVecVals(this.outFluxCoords, this.outFluxVals);}
		    	if (p.allFlagsSet(myConsts.dispPipeLengths)){this.dispAdjNodeDebugVecVals(this.pipeLengthCoords, this.pipeLengths);}
		    	if (p.allFlagsSet(myConsts.dispHeightDiffs)){this.dispAdjNodeDebugVecVals(this.adjNodeHeightDiffsCoords, this.adjNodeHeightDiffs);}
		   		if (p.allFlagsSet(myConsts.dispOriginalMesh)) {this.dispDebugValPoly(new double[4], this.origMeshNodeCoords, myGui.gui_TransGray, false, false, true, false);}
		   		if (p.allFlagsSet(myConsts.dispOldMeshVals))  {	this.dispDebugValPoly(new double[4], this.oldMeshNodeCoords, myGui.gui_TransCyan, false, false, true, false);}
			}
			//display matlab FEM results
			if (this.waterOnPoly){		this.drawWaterPoly();}
			if (this.sedOnPoly){		this.drawSedPoly();}
			if (this.sedCapOnPoly){ 	this.drawSedCapPoly();}
			if (this.sedConcOnPoly){ 	this.drawSedConcPoly();}
		}
	}//drawMe method	
		

	/**
	 *  finds the adjacent faces of this face based on its verticies, and populates the 
	 *  adjFaces structure with them. needs each vertex's adjacent face list to be populated
	 */
	public void findAndSetAdjFaces(){
		boolean checkAdjFaces;
		boolean checkThisAdjFace;
		int numVertMatches;
		//for each vertex of this face, find all its adjacent faces and put in 3 sets
		//then, ignoring this face, find the intersection of every pair of sets
		for (int idx = 0; idx < this.verts.length; ++idx){//for every poly vertex
			for (myFace adjFace : this.verts[idx].getAdjFaces()){//for every face adjacent to adjacent vertex
				checkAdjFaces = false;  //set to true if adjface is adjacent to this face - shares 2 verticies
				checkThisAdjFace = false;  //set to true if adjface = this
				numVertMatches = 1;
				for (myVertex vert : this.verts){//for each vertex in this poly's vertex list
					if ((vert != this.verts[idx]) && (adjFace.containsVertex(vert) != -1)) {
						checkAdjFaces = true;        
						numVertMatches++; 
					}//if contains vert
				}//for each vertex -
				if (numVertMatches == this.numVerts){ checkThisAdjFace = true;  } //verify not this face - only true if num matches == all verts
				if ((!adjFace.equals(this)) && (checkAdjFaces) && (!checkThisAdjFace)){//don't include this face 
					boolean addAdjFaceTest = addAdjFace(adjFace); 
					if (!addAdjFaceTest) {gui.print2Cnsl("Error adding adjacent face : " + adjFace.faceNum);}
				}//if not this face
			}//for each face adjacent to adjacent verticies
		}//for each vertex
	}//findAndsetAdjFaces method
  
  	/**
  	 *  method returns the index of the 3rd vertex of this face given two others
  	 */
  	public int findThirdVertIDX(myVertex v1, myVertex v2){
  		for (int i = 0; i < this.verts.length; i++){	if ((!verts[i].equals(v1)) && (!verts[i].equals(v2))){ return i; }}//for each vertex in this face
  		return -1;
  	}//findthirdVertIDX method
  
  	/**
  	 *  finds and returns the adjacent face to this face in this obj's adjacent 
  	 *  face list that shares the passed verticies, or null if none
  	 *  REQUIRES THIS FACE'S ADJ LIST BE ALREADY APPROPRIATELY CALCULATED
  	 */
  	public myFace findAdjFaceFromVerts(myVertex v1, myVertex v2){   
  		for (myFace adjFace : this.adjFaces){
  			if ((!adjFace.equals(this)) && (adjFace.containsVertex(v1) != -1) && (adjFace.containsVertex(v2) != -1)){return adjFace;}//if check
  		}//for each face
  		return null;
  	}//findAdjface method
 
  	/**
  	 *  returns index of passed vertex, or -1 if not present
  	 */  
  	public int containsVertex(myVertex passedVert){ 
  		int result = -1;
  		for (int i = 0; i < this.verts.length; i++){
  			if (this.verts[i].equals(passedVert)){ result = i; } 
  		}//for each vertex in this face
  		return result;
  	}//containsVertex
  
  	/**
  	 *  this method will subdivide this face into 4 new faces for loop subdivision.
  	 *  first, will retrieve the 3 verts that make up this face and calculate correct midpoints (using existing ones if they do exist)
  	 *  then will construct 4 new faces from these midpoints and return them
  	 */  
  	public myFace[] subdivideLoop(){
  		myFace[] resAra = new myFace[4];
  		int vert1IDX, vert2IDX, tmpThirdIDX;
  		myFace adjFace;
  		//halfEdge verticies : 
  		myVertex newVert01, newVert12, newVert20;
  		//opposite verticies in adjacent polys across the edge from each vertex of this poly
  		myVertex[] oppVertAra = new myVertex[3];
  		
  		for (int vertIDX = 0; vertIDX < 3; ++vertIDX){
  			//find adjacent face sharing 2 verts of this face
  			vert1IDX = (vertIDX + 1)%3;
  			vert2IDX = (vertIDX + 2)%3;

  			adjFace = findAdjFaceFromVerts(this.verts[vert1IDX],this.verts[vert2IDX]);
  			//      print2Cnsl("opp face to vertex #:" + vertIDX + " : " + adjFace.toString());
  			tmpThirdIDX = adjFace.findThirdVertIDX(this.verts[ vert1IDX ],this.verts[vert2IDX]);
  			//      print2Cnsl("opp vertex to vertex#:" + vertIDX + " : " + adjFace.getVerts()[tmpThirdIDX]);
  			oppVertAra[vertIDX] = adjFace.getVerts()[tmpThirdIDX];
  		}
   
  		//set new vertex values based on equation given in class
  		//new vert on edge between 0 and 1 vertices
  		newVert01 = sub.calcHalfVertex(this.verts[0], this.verts[1], this.verts[2], oppVertAra[2]);
  		//new vert on edge between 1 and 2 vertices
  		newVert12 = sub.calcHalfVertex(this.verts[1], this.verts[2], this.verts[0], oppVertAra[0]);
  		//new vert on edge between 2 and 0 vertices
  		newVert20 = sub.calcHalfVertex(this.verts[2], this.verts[0], this.verts[1], oppVertAra[1]);
  		
  		//to make new faces, MUST MAINTAIN ORIENTATION! - if subdivision results in face going black, then orientation is wrong
  		myVertex[] tmpVertAra0 = {this.verts[0],newVert01,newVert20};
  		myVertex[] tmpVertAra1 = {newVert01, this.verts[1], newVert12};  
  		myVertex[] tmpVertAra2 = {newVert20, newVert12, this.verts[2]};
  		myVertex[] tmpVertAra3 = {newVert01, newVert12, newVert20};

  		resAra[0] = new myFace(p, gui, eng, sub,  sub.calcFaceID(this.faceNum,0), 3, tmpVertAra0);    
  		//add face to each new vertex's adjacent lists
  		boolean calcAdjacency = sub.addFaceToVertAdjList(resAra[0], tmpVertAra0);
    
  		resAra[1] = new myFace(p, gui, eng, sub, sub.calcFaceID(this.faceNum,1), 3, tmpVertAra1);  
  		//add face to each new vertex's adjacent list
  		calcAdjacency = sub.addFaceToVertAdjList(resAra[1], tmpVertAra1);
    
  		resAra[2] = new myFace(p, gui, eng, sub, sub.calcFaceID(this.faceNum,2), 3, tmpVertAra2);
  		//add face to each new vertex's adjacent list
  		calcAdjacency = sub.addFaceToVertAdjList(resAra[2], tmpVertAra2);
    
  		resAra[3] = new myFace(p, gui, eng, sub,  sub.calcFaceID(this.faceNum,3), 3, tmpVertAra3);
  		//add face to each new vertex's adjacent list
  		calcAdjacency = sub.addFaceToVertAdjList(resAra[3], tmpVertAra3);
  		if (!calcAdjacency){gui.print2Cnsl("Error adding face in subdivideLoop ");}
  		return resAra;
  	}//subdivideLoop

  	public myVertex[] getVerts(){ return this.verts; }
  	
	public int getFaceNum(){return this.faceNum;}
	public int getNumVerts(){return this.numVerts;}
	public myVertex getVertByIDX(int idx) { return this.verts[idx];}

  	public String toString(){ 
  		String result = "\nFace ID : " + this.faceNum + " Object ID : " + this.ID + " NumVerts : " + this.numVerts;
  		for (myVertex vert : this.verts){
  			result += "\n\t vert : " + vert.toString();
  		}//for each vert
  		return result;
  	}//tostring
  
  	public String toStringWithoutVerts(){ return  "Face Num : " + this.faceNum + " Object ID : " + this.ID;}//toStringWithoutVerts
}//myFace