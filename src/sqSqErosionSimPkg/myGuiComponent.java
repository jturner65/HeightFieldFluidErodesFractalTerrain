package sqSqErosionSimPkg;

public abstract class myGuiComponent {
	public int ID;
	public static int IDcnt = 0;
	
	public String label;				//display text	
	public String mouseOverText;		//string of text when mouse-over on this object

	public myVector dispCoords;			//location of upper left corner for display purposes
	public myVector dispDimension;			//dimensions of this object in x,y,z lengths from upper left corner for display purposes
	public myVector clickCoords;			//upper left corner of clickable zone for this object
	public myVector clickDimension;		//size of clickable hotspot for this object

	public boolean usePopUpEntry;			//whether or not this object uses a pop-up window to allow for data entry/modification
	public int typePopUp;					//if this uses a pop up, what kind does it use

	public boolean enabled;		//whether this is enabled or not (i.e. accepts input)
	public int disabledColor;		//myGui const for disp color when not enabled (does not accept input)
	public int onColor;			//myGui const for on color
	public int offColor;			//myGui const referring to this object's color when "off" - value-driven

	public boolean clickDownChange;	//change display quantities when object click pressed

	
	public myGuiComponent( String _label, String _mouseOverText,					                                              
							myVector _dispCoords, myVector _dispDimension, myVector _clickCoords, myVector _clickDimension,
			                boolean _usePopUpEntry, boolean _enabled, boolean _clickDownChange,		
			                int _typePopUp, int _disabledColor, int _onColor, int _offColor) {
		this.ID = IDcnt++;
		this.label			 	=  _label;			 
		this.mouseOverText		=  _mouseOverText;		 
		this.dispCoords	 		=  _dispCoords;	 
		this.dispDimension	 	=  _dispDimension;	 
		this.clickCoords	 	=  _clickCoords;	 
		this.clickDimension 	=  _clickDimension; 
		this.usePopUpEntry	 	=  _usePopUpEntry;	 
		this.typePopUp		 	=  _typePopUp;		 
		this.enabled		 	=  _enabled;		 
		this.disabledColor	 	=  _disabledColor;	 
		this.onColor		 	=  _onColor;		 
		this.offColor		 	=  _offColor;		 
		this.clickDownChange 	=  _clickDownChange; 
	}//constructor
		//empty constructor
	public myGuiComponent(){this("tmpDummyLabel","this is a tmp label", myVector.ZERO.cloneMe(), myVector.ZERO.cloneMe(), myVector.ZERO.cloneMe(), myVector.ZERO.cloneMe(), false, false, false, -1, -1, -1, -1);}

	public abstract void drawMe();
	
	public String toString(){
		String resStr = "ID : " + this.ID + " |Label : " + this.label + " |Display Coords : "+this.dispCoords.toString() + "|Display Dimensions : " + this.dispDimension.toString() +"\n" ;
		resStr += "\t|Click Coords : "+ this.clickCoords.toString() + " |Click Dimension : " + this.clickDimension.toString() + "\n";
		
		
		return resStr;	
	}//toString

}
