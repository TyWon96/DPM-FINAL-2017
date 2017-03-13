package finalProject;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;



/**
 * @author ilanahaddad
 * @version 1.0
 *
 */
public class Forward {
	private int corner;	//starting corner
	private int fwdLinePosition;
	private int w1; //defender zone dimension w1
	private int w2; //defender zone dimension w2
	private int disp_x; //ball dispenser position x
	private int disp_y; //ball dispenser position y
	private String omega; //ball dispenser orientation 

	// Left motor connected to output A
	// Right motor connected to output D
	// Ball Launcher Motor connected to output B
	public static final EV3LargeRegulatedMotor leftMotor = WiFiExample.leftMotor;
	public static final EV3LargeRegulatedMotor rightMotor = WiFiExample.rightMotor;
//	public static final EV3LargeRegulatedMotor launcherMotor = WiFiExample.launcherMotor;;
	private static final Port usPort = LocalEV3.get().getPort("S1");

	
	public static Odometer odometer = WiFiExample.odometer;
	public static Navigation navigation = WiFiExample.navigation;
//	public static ballLauncher launch =  WiFiExample.launch;
	
	//Setup ultrasonic sensor
	// 1. Create a port object attached to a physical port (done above)
	// 2. Create a sensor instance and attach to port
	// 3. Create a sample provider instance for the above and initialize operating mode
	// 4. Create a buffer for the sensor data
	@SuppressWarnings("resource")							    	// Because we don't bother to close this resource
	SensorModes usSensor = new EV3UltrasonicSensor(usPort);
	SampleProvider usValue = usSensor.getMode("Distance");			// colorValue provides samples from this instance
	float[] usData = new float[usValue.sampleSize()];				// colorData is the buffer in which data are returned
	
		
	public Forward(int corner, int d1, int w1, int w2, int bx, int by, String omega) {
		this.corner = corner;
		this.fwdLinePosition = d1;
		this.w1 = w1;
		this.w2 = w2;
		this.disp_x = bx;
		this.disp_y = by;
		this.omega = omega;
	}
	
	public void startFWD() {
		//already localized
		//step 1 = travel to ball dispenser at (disp_x,disp_y) 
		//			(while avoiding obstacles)!!
		//step 2 = retrieve ball (make robot wait there for 15 seconds maybe
		//step 3 = travel to shooting zone (under fwdLinePosition)
		//step 4 = shoot ball 
		
		
		//travel to ball dispenser
		navigation.travelTo(disp_x,disp_y);
		
		
	}
	
}