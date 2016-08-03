package sqSqErosionSimPkg;
import processing.core.PApplet;

public class sqSqErosionSimMain {

	public static void main(String[] passedArgs) {
	    String[] appletArgs = new String[] {"sqSqErosionSimPkg.sqSqErosionSimGlobal" };
	    if (passedArgs != null) {
	    	PApplet.main(PApplet.concat(appletArgs, passedArgs));
	    } else {
	    	PApplet.main(appletArgs);
	    }
	 }
//	public static void main(String[] args) {
//		PApplet.main(new String[] {   "--present", "sqSqErosionSimPkg.sqSqErosionSimGlobal" });
//	}//main
	
	//getting error because processing is setting fullscreen mode incorrectly

}//class
