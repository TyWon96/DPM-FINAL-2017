package finalProject;

import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;


/**
 * The forward class is a general class that is 
 * used to start up the necessary sensors and other 
 * classes (navigation and odometer). Forward should 
 * be called directly after localization if the robot is 
 * playing the forward role and it tells the robot to move 
 * to the position of the ball dispenser.
 * 
 * @author Ilana Haddad
 * @version 1.0
 *
 */
public class Forward {
	private int corner;	//starting corner
	private int fwdLinePosition;
	private int w1; //defender zone dimension w1
	private int w2; //defender zone dimension w2
	private int bx; //ball dispenser position x
	private int by; //ball dispenser position y
	private String omega; //ball dispenser orientation 
	private final double TILE_LENGTH = 30.48;
	private final int CENTER_X_COORD = 10; //x coordinate of center of field we will shoot from
	private final double ROBOT_FRONT_TOCENTER_DIST = 8; //distance from front of robot to center of rotation
	private final int FIELD_DIST = 8; //12
	private final int OUTER_TILES = 2;

	/** The left motor, which is connected to output A */
	public static final EV3LargeRegulatedMotor leftMotor = WiFiExample.leftMotor;
	/**The right motor, which is connected to output D */
	public static final EV3LargeRegulatedMotor rightMotor = WiFiExample.rightMotor;
	public static final EV3LargeRegulatedMotor launcherMotor = WiFiExample.launcherMotor;
	/**The navigation program for the robot */
	public static Navigation nav = WiFiExample.navigation;
	/** Instantiating odometer of robopt*/
	public static Odometer odo = WiFiExample.odometer;
	/** The Ultrasonic Sensor */
	private static final Port usPort = LocalEV3.get().getPort("S1");
	Launcher launcher = new Launcher(launcherMotor);
	
	public static Correction correction = WiFiExample.correction;
//	/** The motor for the ball launcher */
//	public static Launcher launch =  WiFiExample.launch;
	/** 
	 * The Forward constructor
	 * @param corner the corner which the robot starts in
	 * @param d1 the position of the line separating the forward and defensive zones
	 * @param w1 one of the two coordinates of the bounce zone
	 * @param w2 one of the two coordinates of the bounce zone
	 * @param bx the x coordinate of the ball dispenser
	 * @param by the y coordinate of the ball dispenser
	 * @param omega the orientation of the robot
	 */
	public Forward(Navigation navigation, Odometer odometer, int corner, int d1, int w1, int w2, int bx, int by, String omega) {
		this.corner = corner;
		this.fwdLinePosition = d1;
		this.w1 = w1;
		this.w2 = w2;
		this.bx = bx;
		this.by = by;
		this.omega = omega;
		this.nav = navigation;
		this.odo = odometer;
	}
	/**
	 * The start Forward method should begin directly after 
	 * localization and move the robot to the position of the ball dispenser.
	 */
	public void startFWD() {
		
		int[] field_coord = new int[3]; 	//array that stores field coordinates of the robot's position
		if(corner==1){
			field_coord[0] =0;
			field_coord[1] =0;
			field_coord[2] = 0;
		}
		if(corner==2){
			field_coord[0] =10;
			field_coord[1] =0;
			field_coord[2] = 0;
		}
		if(corner==3){
			field_coord[0] =10;
			field_coord[1] =10;
			field_coord[2] = 180;
		}
		if(corner==4){
			field_coord[0] =0;
			field_coord[1] =10;
			field_coord[2] = 180;
		}
		//update odometer to correct position on field using the field_coord array
		double[] position = {TILE_LENGTH*field_coord[0], TILE_LENGTH*field_coord[1], field_coord[2]};
		odo.setPosition(position, new boolean[]{true,true,true});
		
		//convert bx,by to cm:
		double bx_cm, by_cm;
		bx_cm = bx*TILE_LENGTH;
		by_cm = by*TILE_LENGTH;
		

		
		int dispToCorner_x = Math.abs(bx-field_coord[0]); //tiles left to travel to dispenser (if disp is on south wall)
		int dispToCorner_y = Math.abs(by-field_coord[1]); //tiles left to travel to dispenser (if disp is on east/west wall)
//		if((dispToCorner_x>4 || dispToCorner_y>4) && (dispToCorner_x<=8|| dispToCorner_y<=8)){ 
//			//if distance from starting corner to ball dispenser is more than 4 tiles but less than 8, localize after 4
//			//if it is 8 away, we only need to localize at 4 because we localize once dispenser is reached
//			//so, localize after traveling 4 tiles, and then travel what's left:
//			if(bx == 0){ //west wall:
//				nav.travelTo(0, 4*TILE_LENGTH); 
//				correction.localize();
//				nav.travelTo(0, (by-4)*TILE_LENGTH);
//				
//			}
//			
//		}
//		else if(dispToCorner_x>8 || dispToCorner_y>8){ //if distance is 9 or 10 however, we need to localize at 4, and 8. 
//			
//		}
//		else if(dispToCorner_x<=4 || dispToCorner_y<=4){ //just travel to ball dispenser without localizing
//			
//		}
//		nav.travelTo(bx_cm, by_cm-(2*30.48));
//		correction.localize();
		
		//travel to ball dispenser cm coordinates:
		nav.travelTo(bx_cm, by_cm); 
		nav.finishTravel = false;
		nav.turnToSmart(90); //facing away from disp

		//localize forward
		correction.localizeFWD();
		//drive forward a little to correct angle:
		nav.driveWCorrection(14);
		nav.driveWCorrection(-15.5); //drive back to intersection
		launcher.Enter_Launch_Position();//pulls the arm down
		
		//beep to indicate robot is ready to receive ball:
		Sound.beep();
		
		//wait a few seconds at the dispenser
		try { Thread.sleep(10000); } catch (InterruptedException e) {}
		
		//BALL RECEIVED: turn off US sensor
		
		
		//travel one tile behind forward line IN Y FIRST, localize
		int fwdLine_coord = 10 - fwdLinePosition;
		nav.travelToYFIRST(5*TILE_LENGTH, ((fwdLine_coord-1)*TILE_LENGTH) - ROBOT_FRONT_TOCENTER_DIST);
		nav.finishTravel = false;
		nav.turnToSmart(0); //face target 
		correction.localize(); 
		nav.travelToYFIRST(5*TILE_LENGTH, (fwdLine_coord*TILE_LENGTH) - ROBOT_FRONT_TOCENTER_DIST); //go to forward line
		nav.finishTravel = false;
		launcher.Fire(fwdLinePosition);
		
		
		
	
		
	}
	
}
