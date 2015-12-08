package ecumene.exo.sim.solar;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import org.joml.Vector2f;
import org.joml.Vector3f;

import ecumene.exo.ExoRuntime;
import ecumene.exo.sim.ESContext;
import ecumene.exo.sim.IESContextListener;
import ecumene.exo.sim.map.real.JRMapRenderer;
import ecumene.exo.sim.map.real.RMap;
import ecumene.exo.sim.map.real.RPoint;

public class JExoSolarRenderer extends JRMapRenderer implements IESContextListener {

	private ExoSolarMap map;
	private int follow;
	
	public JExoSolarRenderer(ExoSolarMap map) {
		super(new RMap());
		this.rendererList.add(new ExoSolarObjectRenderer(this));
		this.rendererList.remove(getDefaultRPointRenderer());
		
		this.map = map;
		
		addKeyListener(new KeyListener() {
			@Override public void keyTyped(KeyEvent e)    {}
			@Override public void keyReleased(KeyEvent e) {}
			@Override public void keyPressed(KeyEvent e)  {
				if(e.getKeyCode() == KeyEvent.VK_V){
					useNames = !useNames; // Use names in solar renderer means to toggle disp. vector rendering
				}
			}
		});
	}
	
	private IExoSolarObject followObject;
	private int lastFollow; // To check if it was changed since the last frame
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		if(ExoRuntime.INSTANCE.getContext().getSolarFollow() > -1) this.follow = ExoRuntime.INSTANCE.getContext().getSolarFollow();
		if(lastFollow != follow) this.followObject = map.getObjects().get(follow);
		
		if(followObject != null){
			Vector2f navPos = new Vector2f(followObject.getPosition());
			navPos.add(followObject.getVelocity());
			navPos.x *= -Math.abs(navigation.z);
			navPos.y *= Math.abs(navigation.z);
			this.navigation = new Vector3f(navPos.x, navPos.y, this.navigation.z);
		}
		
		Graphics2D graphics = (Graphics2D) g;
		graphics.drawString("Solar Abstraction", 0, 10);
		graphics.drawString("Press V to toggle vectors", 0, 20);
		graphics.drawString("Artifact Count (+ sun): " + pMap.getMap().length, 0, 30);
		
		lastFollow = follow; // Update last frame follow index
	}
	
	public void onStep(float interp){
		pMap = map.step(interp);
	}

	@Override
	public void onContextChanged(ESContext context) {
		repaint();
	}

	public void setSolarMap(ExoSolarMap solarMap) {
		this.map = solarMap;
	}
	
}
