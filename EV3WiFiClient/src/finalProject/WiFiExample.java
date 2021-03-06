package finalProject;

import java.util.Map;


import finalProject.Defense;
import finalProject.Forward;
import finalProject.Navigation;
import finalProject.Odometer;
import finalProject.Launcher;
import finalProject.UltrasonicPoller;

import wifi.WifiConnection;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;


/**
 * Example class using WifiConnection to communicate with a server and receive
 * data concerning the competition such as the starting corner the robot is
 * placed in.
 * 
 * @author Michael Smith
 * @author Ilana Haddad
 * @version 2.0 
 */
public class WiFiExample {
	public static final double WHEEL_RADIUS = 2.0768;
	//	public static final double TRACK = 11.4; 
	public static double TRACK = 10.9; 
	public static final int FORWARD_SPEED = 250;
	public static final int ROTATE_SPEED = 150;

	// Left motor connected to output A
	// Right motor connected to output D
	// Ball Launcher Motor connected to output B
	public static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
	public static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));
	public static final EV3LargeRegulatedMotor launcherMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("B"));
	public static final EV3MediumRegulatedMotor barMotor = new EV3MediumRegulatedMotor(LocalEV3.get().getPort("C"));

	public static Launcher launcher = new Launcher(launcherMotor, barMotor);
	//colorPorts front, left side and right side of the EV3
	private static final Port colorPortF = LocalEV3.get().getPort("S2");	
	private static final Port colorPortL = LocalEV3.get().getPort("S3");	
	private static final Port colorPortR = LocalEV3.get().getPort("S4");	

	public static final Port usPort = LocalEV3.get().getPort("S1");

	//Initialization of odometer and navigation objects.
	public static EV3ColorSensor colorSensorF = new EV3ColorSensor(colorPortF);
	public static EV3ColorSensor colorSensorL = new EV3ColorSensor(colorPortL);
	public static EV3ColorSensor colorSensorR = new EV3ColorSensor(colorPortR);
	public static Odometer odometer = new Odometer(leftMotor, rightMotor);
	public static Navigation navigation;
	public static Correction correction;
	//Setup ultrasonic sensor
	// 1. Create a port object attached to a physical port (done above)
	// 2. Create a sensor instance and attach to port
	// 3. Create a sample provider instance for the above and initialize operating mode
	// 4. Create a buffer for the sensor data
	@SuppressWarnings("resource")							    	// Because we don't bother to close this resource
	public static SensorModes usSensor = new EV3UltrasonicSensor(usPort);
	public static SampleProvider usValue = usSensor.getMode("Distance");			// colorValue provides samples from this instance
	public static float[] usData = new float[usValue.sampleSize()];				// colorData is the buffer in which data are returned

	public static UltrasonicPoller usPoller = new UltrasonicPoller(usValue, usData);
	public static PController cont;
	//	public static ballLauncher launch = new ballLauncher(launcherMotor,odometer,navigation);


	/*
	 * We use System.out.println() instead of LCD printing so that full debug
	 * output (e.g. the very long string containing the transmission) can be
	 * read on the screen OR a remote console such as the EV3Control program via
	 * Bluetooth or WiFi
	 * 
	 * 
	 * 					****
	 *** INSTRUCTIONS ***
	 ****
	 * There are two variables each team MUST set manually below:
	 *  
	 * 1. SERVER_IP: the IP address of the computer running the server
	 * application. This will be your own laptop, until the beta beta demo or
	 * competition where this is the TA or professor's laptop. In that case, set
	 * the IP to 192.168.2.3. 
	 * 
	 * 2. TEAM_NUMBER: your project team number
	 */
	private static final String SERVER_IP = "192.168.2.3";
	private static final int TEAM_NUMBER = 3;

	// Enable/disable printing of debug info from the WiFi class
	private static final boolean ENABLE_DEBUG_WIFI_PRINT = true;

	@SuppressWarnings("rawtypes")
	/**
	 * Main method that will be entered at the beginning of each round.
	 * The method will begin the running of the odometer and the display
	 * and then it will have the robot do its initial localization to find
	 * the exact position and heading of the robot. Then, the robot should
	 * start the running of the obstacle avoidance and navigation systems and, 
	 * depending on whether the robot is playing forward or defense should begin
	 * the robot's defense or forward system.
	 *
	 */
	public static void main(String[] args) {

		System.out.println("Running..");

		//Setup color sensor
		// 1. Create a port object attached to a physical port (done above)
		// 2. Create a sensor instance and attach to port
		// 3. Create a sample provider instance for the above and initialize operating mode
		// 4. Create a buffer for the sensor data
		@SuppressWarnings("resource")

		SampleProvider colorValueF = colorSensorF.getMode("Red");			// colorValue provides samples from this instance
		SampleProvider colorValueR = colorSensorR.getMode("Red");
		SampleProvider colorValueL = colorSensorL.getMode("Red");

		float[] colorData = new float[100];			// colorData is the buffer in which data are returned
		float[] colorData2 = new float[100];


		// Initialize WifiConnection class
		WifiConnection conn = new WifiConnection(SERVER_IP, TEAM_NUMBER, ENABLE_DEBUG_WIFI_PRINT);
		Sound.beep();
		// Connect to server and get the data, catching any errors that might occur
		try {
			/*
			 * getData() will connect to the server and wait until the user/TA
			 * presses the "Start" button in the GUI on their laptop with the
			 * data filled in. Once it's waiting, you can kill it by
			 * pressing the upper left hand corner button (back/escape) on the EV3.
			 * getData() will throw exceptions if it can't connect to the server
			 * (e.g. wrong IP address, server not running on laptop, not connected
			 * to WiFi router, etc.). It will also throw an exception if it connects 
			 * but receives corrupted data or a message from the server saying something 
			 * went wrong. For example, if TEAM_NUMBER is set to 1 above but the server expects
			 * teams 17 and 5, this robot will receive a message saying an invalid team number 
			 * was specified and getData() will throw an exception letting you know.
			 */
			Map data = conn.getData();
			int fwdTeam = ((Long) data.get("FWD_TEAM")).intValue();
			int defTeam = ((Long) data.get("DEF_TEAM")).intValue();
			int fwdCorner = ((Long) data.get("FWD_CORNER")).intValue();
			int defCorner = ((Long) data.get("DEF_CORNER")).intValue();
			int w1 = ((Long) data.get("w1")).intValue();
			int w2 = ((Long) data.get("w2")).intValue();
			int d1 = ((Long) data.get("d1")).intValue();
			int bx = ((Long) data.get("bx")).intValue();
			int by = ((Long) data.get("by")).intValue();
			String orientation = (String) data.get("omega");

			navigation = new Navigation(odometer);
			cont = new PController(leftMotor, rightMotor, usPoller, usValue, usSensor, usData);
			Localization lsl = new Localization(odometer,navigation, colorValueF, colorData, 
					colorData2, leftMotor, rightMotor, usValue, usSensor, usData);
			final TextLCD t = LocalEV3.get().getTextLCD(); 
			t.clear();
			OdometryDisplay odometryDisplay = new OdometryDisplay(odometer,t);

			if(fwdTeam == 3){ //play forward:
				System.out.println();
				System.out.println();
				System.out.println();
				System.out.println();
				System.out.println();
				System.out.println();
				System.out.println();
				System.out.println();

				odometer.start();
				odometryDisplay.start();
				Sound.setVolume(0);
				lsl.doLocalization();
				colorSensorF.close();
				correction = new Correction(odometer, navigation, colorValueR, colorValueL, colorValueF, leftMotor, rightMotor);
				usPoller.start();
				cont.start();
				Forward forward = new Forward(navigation,odometer,fwdCorner,d1,w1,w2,bx,by,orientation);
				forward.startFWD();
			}



			if(defTeam == 3){//play defense:
				System.out.println();
				System.out.println();
				System.out.println();
				System.out.println();
				System.out.println();
				System.out.println();
				System.out.println();
				System.out.println();
				odometer.start();
				odometryDisplay.start();
				Sound.setVolume(0);
				lsl.doLocalization();
				colorSensorF.close();
				correction = new Correction(odometer, navigation, colorValueR, colorValueL, colorValueF, leftMotor, rightMotor);
				usPoller.start();
				cont.start();
				Defense defense = new Defense(defCorner, w1, w2);
				defense.startDEF();
			}
		}
		catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}

		// Wait until user decides to end program
		Button.waitForAnyPress();
	}
}