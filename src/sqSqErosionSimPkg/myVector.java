package sqSqErosionSimPkg;

/**
*  replacement class for pvector 
*  keeps sqmagnitude, not magnitude - need to calculate magnitude
*/
public class myVector{
	public double x,y,z;
	public double sqMagn;
		//vector constants available to all consumers of myVector
	public static final myVector ZERO = new myVector(0,0,0);
	public static final myVector UP	=	new myVector(0,1,0);
	public static final myVector RIGHT = new myVector(1,0,0);
	public static final myVector FORWARD = new myVector(0,0,1);
	
	myVector(double _x, double _y, double _z){this.x = _x; this.y = _y; this.z = _z;  this._SqMag();}         //constructor 3 args  
	myVector(myVector p){ this(p.x, p.y, p.z); }                                                                                                           	//constructor 1 arg  
	myVector(){ this(0,0,0);}                                                                                                                               //constructor 0 args
	
	public void set(double _x, double _y, double _z){ this.x = _x;  this.y = _y;  this.z = _z; this._mag(); }                                               //set 3 args 
	public void set(myVector p){ this.x = p.x; this.y = p.y; this.z = p.z;  this._mag();}                                                                   //set 1 args
	public void set(double _x, double _y, double _z, double _sqMagn){ this.x = _x;  this.y = _y;  this.z = _z; this.sqMagn = _sqMagn; }                                                                     //set 3 args 
	
	public myVector _mult(double n){ this.x *= n; this.y *= n; this.z *= n; this._mag(); return this; }                                                     //_mult 3 args  
	public static myVector _mult(myVector p, double n){ myVector result = new myVector(p.x * n, p.y * n, p.z * n); return result;}                          //1 vec, 1 double
	public static myVector _mult(myVector p, myVector q){ myVector result = new myVector(p.x *q.x, p.y * q.y, p.z * q.z); return result;}                   //2 vec
	public static void _mult(myVector p, myVector q, myVector r){ myVector result = new myVector(p.x *q.x, p.y * q.y, p.z * q.z); r.set(result);}           //2 vec src, 1 vec dest  
	
	public void _add(double _x, double _y, double _z){ this.x += _x; this.y += _y; this.z += _z;  this._mag(); }                                            //_add 3 args
	public void _add(myVector v){ this.x += v.x; this.y += v.y; this.z += v.z;  this._mag(); this._mag(); }                                                 //_add 1 arg  
	public static myVector _add(myVector p, myVector q){ myVector result = new myVector(p.x + q.x, p.y + q.y, p.z + q.z); return result;}                //2 vec
	public static void _add(myVector p, myVector q, myVector r){ myVector result = new myVector(p.x + q.x, p.y + q.y, p.z + q.z); r.set(result);}       //2 vec src, 1 vec dest  
	
	public void _sub(double _x, double _y, double _z){ this.x -= _x; this.y -= _y; this.z -= _z;  this._mag(); }                                                                   //_sub 3 args
	public void _sub(myVector v){ this.x -= v.x; this.y -= v.y; this.z -= v.z;  this._mag(); }                                                                           //_sub 1 arg 
	public static myVector _sub(myVector p, myVector q){ myVector result = new myVector(p.x - q.x, p.y - q.y, p.z - q.z); return result;}                //2 vec
	public static void _sub(myVector p, myVector q, myVector r){ myVector result = new myVector(p.x - q.x, p.y - q.y, p.z - q.z); r.set(result);}       //2 vec src, 1 vec dest  
	
	public double _mag(){ double magn = Math.sqrt(this._SqMag()); return magn; }  
	public double _SqMag(){ this.sqMagn = (double) ((this.x*this.x) + (this.y*this.y) + (this.z*this.z)); return this.sqMagn; }  							//squared magnitude
	
	public void _normalize(){double magn = this._mag();this.x = (magn == 0) ? 0 : this.x/magn; this.y = (magn == 0) ? 0 : this.y / magn; this.z = (magn == 0) ? 0 : this.z / magn; magn = this._mag(); this.sqMagn = magn * magn; }
	public static myVector _normalize(myVector v){double magn = v._mag(); myVector newVec = (magn == 0) ? (new myVector(0,0,0)) : (new myVector( v.x /= magn, v.y /= magn, v.z /= magn)); newVec._mag(); return newVec;}
	
	public myVector _normalized(){double magn = this._mag(); myVector newVec = (magn == 0) ? (new myVector(0,0,0)) : (new myVector( this.x /= magn, this.y /= magn, this.z /= magn)); newVec._mag(); return newVec;}

	public myVector cloneMe(){myVector retVal = new myVector(this.x, this.y, this.z); retVal._mag(); return retVal;}  
	
	public double _L1Dist(myVector q){return Math.abs((this.x - q.x) + (this.y - q.y) + (this.z - q.z)); }
	public static double _L1Dist(myVector q, myVector r){ return Math.abs((r.x - q.x) + (r.y - q.y) + (r.z - q.z));}
	
	public double _SqrDist(myVector q){ return (double)(((this.x - q.x)*(this.x - q.x)) + ((this.y - q.y)*(this.y - q.y)) + ((this.z - q.z)*(this.z - q.z))); }
	public static double _SqrDist(myVector q, myVector r){  return (double)(((r.x - q.x) *(r.x - q.x)) + ((r.y - q.y) *(r.y - q.y)) + ((r.z - q.z) *(r.z - q.z)));}
	
	public double _dist(myVector q){ return (double)Math.sqrt( ((this.x - q.x)*(this.x - q.x)) + ((this.y - q.y)*(this.y - q.y)) + ((this.z - q.z)*(this.z - q.z)) ); }
	public static double _dist(myVector q, myVector r){  return (double)Math.sqrt(((r.x - q.x) *(r.x - q.x)) + ((r.y - q.y) *(r.y - q.y)) + ((r.z - q.z) *(r.z - q.z)));}
	
	public double _dist(double qx, double qy, double qz){ return (double)Math.sqrt( ((this.x - qx)*(this.x - qx)) + ((this.y - qy)*(this.y - qy)) + ((this.z - qz)*(this.z - qz)) ); }
	public static double _dist(myVector r, double qx, double qy, double qz){  return (double)Math.sqrt(((r.x - qx) *(r.x - qx)) + ((r.y - qy) *(r.y - qy)) + ((r.z - qz) *(r.z - qz)));}
	
	public void _div(double q){this.x /= q; this.y /= q; this.z /= q; this._mag();}  
	
	public myVector _cross(myVector b){ return new myVector((this.y * b.z) - (this.z*b.y), (this.z * b.x) - (this.x*b.z), (this.x * b.y) - (this.y*b.x));}		//cross product 
	public static myVector _cross(double ax, double ay, double az, double bx, double by, double bz){		return new myVector((ay*bz)-(az*by), (az*bx)-(ax*bz), (ax*by)-(ay*bx));}
	
	
	public double _dot(myVector b){return ((this.x * b.x) + (this.y * b.y) + (this.z * b.z));}																	//dot product
	
	public static double _angleBetween(myVector v1, myVector v2) {
		double 	_v1Mag = v1._mag(), 
				_v2Mag = v2._mag(), 
				dotProd = v1._dot(v2),
				cosAngle = dotProd/(_v1Mag * _v2Mag),
				angle = Math.acos(cosAngle);
		return angle;
	}//_angleBetween
	
	/**
	 * returns if this vector is equal to passed vector
	 * @param b vector to check
	 * @return whether they are equal
	 */
	public boolean equals(Object b){
		if (this == b) return true;
		if (!(b instanceof myVector)) return false;
		myVector v = (myVector)b;
		return ((this.x == v.x) && (this.y == v.y) && (this.z == v.z));		
	}				
	
	public String toString(){return "|(" + this.x + ", " + this.y + ", " + this.z+")| sqMag:" + this.sqMagn;}

}//myVector