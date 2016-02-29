package ecumene.exo.runtime;

import java.beans.ExceptionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;

import ecumene.exo.sim.galaxy.ExoGalaxyMap;
import ecumene.exo.sim.galaxy.gen.ExoGalaxyMapGen;
import ecumene.exo.sim.planet.ExoPlanet;
import ecumene.exo.sim.planet.ExoPlanetMap;
import ecumene.exo.sim.planet.ExoPlanetMoon;
import ecumene.exo.sim.planet.TrackingParameters;
import ecumene.exo.sim.planet.gen.ExoPlanetMapGen;
import ecumene.exo.sim.solar.ExoSolarObject;
import ecumene.exo.sim.solar.gen.ExoSolarMapGen;
import ecumene.exo.sim.solar.ExoSolarMap;
import ecumene.exo.view.rmap.planet.RMVPlanetMapTag;
import org.apache.commons.cli.ParseException;

import ecumene.exo.analyze.ExoRuntimeAnalyzerTag;
import ecumene.exo.sim.SimContext;
import ecumene.exo.view.IViewerTag;
import ecumene.exo.view.ViewerRunnable;
import ecumene.exo.view.rmap.galaxy.RMVGalaxyMapTag;
import ecumene.exo.view.rmap.solar.RMVSolarMapTag;
import org.joml.Vector2f;

public class ExoRuntime implements Runnable{
	
	public  ExoArgParse          commands;
	private JFrame               frame;
	private List<ViewerRunnable> viewers;
	private IViewerTag[]         viewerDB;
	private ExecutorService      viewerExec;
	private ExceptionListener    exceptionListener;
	private SimContext           context;
	
	public static ExoRuntime INSTANCE;
	
	public ExoRuntime(String[] arguments) throws ParseException, IOException {
		commands  = new ExoArgParse(arguments);
		viewerExec = Executors.newCachedThreadPool();
		viewers = new ArrayList<ViewerRunnable>();
		
		exceptionListener = new ExceptionListener() {
			@Override
			public void exceptionThrown(Exception e) {
				System.out.println(); e.printStackTrace(); //New line + print st
			}
		};

		ExoGalaxyMap galaxy = new ExoGalaxyMapGen(System.currentTimeMillis()).genGalaxy(1, 2, 1, 100, 400).getSource();               // generating galaxy
		ExoSolarMap  solar  = new ExoSolarMapGen(System.currentTimeMillis()).genCentralOrbiters(4, 10,                                //generating the solar system
				                                                                                 new Vector2f(0.001f, 0.01f),
				                                                                                 new Vector2f(-500, 500)).getSource();
		ExoPlanetMap planet = new ExoPlanetMapGen(System.currentTimeMillis()).genExoPlanet(new ExoSolarObject(2),                                        // The solar object representing the planet
				                                                                           2,                                                            // # of moons
				                                                                           new Vector2f(0.5f, 1),                                        // minmax for moon mass
				                                                                           new Vector2f(50, 200),                                        // minmax for moon diameter (r in polar coordinates)
				                                                                           new Vector2f(0, 360),                                         // minmax for moon angle
				                                                                           new Vector2f(-0.15f, 0.15f)).getSource();                     // minmax for moon initial velocity
		planet.getPlanet().setTracking(0, new TrackingParameters("0xFF00FF", 100, false)).setTracking(1, new TrackingParameters("0xFFFF00", 100, false));// tracking data for moons
		context = new SimContext(galaxy, solar, planet);
		
		viewerDB = new IViewerTag[4];
		viewerDB[0] = new ExoRuntimeAnalyzerTag();
		viewerDB[1] = new RMVGalaxyMapTag();
		viewerDB[2] = new RMVSolarMapTag();
		viewerDB[3] = new RMVPlanetMapTag();
	}
	
	private boolean scanLine = false;
	private void parseCommand(String input) throws Throwable{
		String upperCommand = input.toUpperCase();
		String[] upperLineWords = upperCommand.split(" ");
		if(upperCommand.equals("WHATDO")){
			System.out.println("---------WHATDO-EXOSIM----------");
			System.out.println("exit             -> Exit");
			System.out.println("run  [id] [args] -> Runs runnable (id)");
			System.out.println("ran              -> Lists running");
			System.out.println("stop [id]        -> Stops runnable (id)");
			System.out.println("--------------------------------");
		} else if(upperLineWords[0].equals("RUN")){                 // Run
			if(viewerDB.length == 0) System.out.println("Runables Empty...");
			else {
				if(upperLineWords.length == 1){                     // Run a runnable (upperLineWords[1] = runnable ID)
					System.out.println("Use: run [runnable] [args]");
					System.out.println("---------RUNNABLES----------");
					for(int i = 0; i < viewerDB.length; i++){      // Iterate all runnables
						System.out.println(i + ": " + viewerDB[i].getIdentifier());
					}
					System.out.println("----------------------------");
				} else {
					int chosen = Integer.parseInt(upperLineWords[1]);                                                                                                               // Parse second num as int
					if(chosen >= viewerDB.length) throw new IllegalArgumentException("Chosen int larger than runnables");                                                           // Is that num illegal?
					ViewerRunnable runnable = (ViewerRunnable) viewerDB[chosen].construct(chosen, exceptionListener, Arrays.copyOfRange(upperLineWords, 1, upperLineWords.length)); // Choose a runnable from num
					System.out.println("Running " + chosen);                                                                                                                        // We're running that now!
					viewerExec.submit(runnable);                                                                                                                                    // Submit runnable to exec. service
					viewers.add(runnable);                                                                                                                                          // Add runnable
				}
			}
		} else if(upperLineWords[0].equals("EXIT")){
			System.out.println("Exiting Exo Sim...");                                                                      // Exiting!
			scanLine = false;                                                                                              // Stop scanning!
			System.exit(0);
		} else if(upperLineWords[0].equals("RAN")) {
			System.out.println("-----------RUNNING----------");                                                            // List running
			if(viewers.size() == 0) System.out.println("None running...");
			else for(int i = 0; i < viewers.size(); i++){                                                                  // Iterate all running
				System.out.println("Runnable ID: " + i + " Running ID: " + viewers.get(i).getID());                        // Runnable ID: X Running ID: X 
			}
			System.out.println("----------------------------");
	    } else if(upperLineWords[0].equals("STOP")){                                                                       // Stop a runnable
			int chosen = Integer.parseInt(upperLineWords[1]);                                                              // Parse chosen runnable
			if(chosen >= viewers.size()) throw new IllegalArgumentException("Chosen int larger than runnables");	   	   // Report if chosen int is too big
			if(viewers.get(chosen) == null) throw new IllegalArgumentException("Thread " + chosen + " is already kill!");  // Report if chosen isn't running
			viewers.get(chosen).kill(chosen);                                                                                                               
			viewers.remove(chosen);
		} else {
			if(upperCommand.trim().length() > 0) System.out.println("Unknown command '" + upperLineWords[0] + "'");
		}
		System.out.print("->");
	}
	
	@Override
	public void run(){
		System.out.println("Current Working Directory: \"" + commands.getPWD() + "\"");
		System.out.println("For what do? type 'whatdo'");
		Scanner scanner = new Scanner(new UnClosableDecorator(System.in));
		System.out.print("->");
		boolean parsingRTCommand = commands.getRTCommands() == null ? false : (commands.getRTCommands().length == 0 ? false : true);
		scanLine                 = true;
		
		if(parsingRTCommand)
			for(int i = 0; i < commands.getRTCommands().length; i++){
				try{
					parseCommand(commands.getRTCommands()[i]);
				}catch(Throwable e){
					e.printStackTrace();
					System.out.println("Skipping line " + commands.getRTCommands()[i]);
					continue;
				}		
			}
		while(scanner.hasNextLine() || !scanLine){
			String nextLine = scanner.nextLine();
			try{
				parseCommand(nextLine);
			}catch(Throwable e){
				e.printStackTrace();
				System.out.println("Skipping line " + nextLine);
				continue;
			}		
		}
		scanner.close();
		viewerExec.shutdown();
	}
	
	public SimContext getContext(){
		return context;
	}
	
	public void setContext(SimContext context){
		for(int i = 0; i < viewers.size(); i++)
			viewers.get(i).onContextChanged(context);
	}
	
	public void step(){
		for(int i = 0; i < viewers.size(); i++)
			viewers.get(i)
			.onStep(context, 
					context.getSteps());
	}
	
	public IViewerTag[] getRunnables(){
		return viewerDB;
	}

	public static void main(String[] args) throws ParseException, IOException {
		INSTANCE = new ExoRuntime(args);
		INSTANCE.run();
		System.exit(0);
	}
}
