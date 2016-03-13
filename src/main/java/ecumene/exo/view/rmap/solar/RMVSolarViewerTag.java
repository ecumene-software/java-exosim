package ecumene.exo.view.rmap.solar;

import java.beans.ExceptionListener;

import ecumene.exo.view.rmap.RMVTag;

import ecumene.exo.runtime.ExoRuntime;
import org.joml.Vector3f;

public class RMVSolarViewerTag extends RMVTag {

	public RMVSolarViewerTag() {
	}

	@Override
	public String getIdentifier() {
		return "Solar simulation";
	}

	@Override
	public Runnable constructRMV(int id, ExceptionListener listener, String[] args, Vector3f nav) {
		return new RMVSolarMapRenderer(id, listener, ExoRuntime.INSTANCE.getContext().getSolarSystem().getSolarMap(), nav);
	}
}
	
