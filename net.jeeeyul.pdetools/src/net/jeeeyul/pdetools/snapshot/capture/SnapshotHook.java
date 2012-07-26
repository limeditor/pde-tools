package net.jeeeyul.pdetools.snapshot.capture;

import java.util.HashSet;
import java.util.Set;

import net.jeeeyul.pdetools.shared.SharedImages;
import net.jeeeyul.pdetools.snapshot.NewSnapshotEntryJob;
import net.jeeeyul.pdetools.snapshot.model.snapshot.ShellInfo;
import net.jeeeyul.pdetools.snapshot.model.snapshot.SnapshotFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class SnapshotHook {
	private Set<Integer> hookingTypes = new HashSet<Integer>();
	private HookingState state = HookingState.NONE;

	private Listener dispatcher = new Listener() {
		@Override
		public void handleEvent(Event event) {
			SnapshotHook.this.handleEvent(event);
		}
	};

	private Display display;
	private CaptureBoundsShell captureBoundsShell;
	private ControlCapture controlCapture;
	private Control controlUnderMouse;
	private Cursor captureCursor;

	public SnapshotHook(Display display) {
		this.display = display;
	}

	private void capture() {
		if (state == HookingState.TRACK_CONTROL) {
			transite(HookingState.CAPTURING_CONTROL);
		} else {
			transite(HookingState.CAPTURING_SHELL);
		}

		Image image = null;
		if (state == HookingState.CAPTURING_SHELL) {
			image = getControlCapture().capture(controlUnderMouse.getShell());
		} else {
			image = getControlCapture().capture(controlUnderMouse);
		}

		ImageData imageData = image.getImageData();
		image.dispose();

		NewSnapshotEntryJob job = new NewSnapshotEntryJob(imageData);
		if (state == HookingState.CAPTURING_SHELL) {
			Shell shell = controlUnderMouse.getShell();
			ShellInfo shellInfo = SnapshotFactory.eINSTANCE.createShellInfo();
			if (shell.getImage() != null) {
				shellInfo.setIcon(shell.getImage().getImageData());
			}
			shellInfo.setShellStyle(shell.getStyle());
			shellInfo.setShellTitle(shell.getText());
			job.setShellInfo(shellInfo);
			job.setControlType(shell.getClass().getCanonicalName());
		} else {
			job.setControlType(controlUnderMouse.getClass().getCanonicalName());
		}
		job.schedule();

		transite(HookingState.NONE);

	}

	private void dispose() {
		if (controlUnderMouse != null) {
			restoreCursor(controlUnderMouse);
		}
		unhookAll();
		controlUnderMouse = null;
		getCaptureBoundsShell().dispose();
		getCaptureCursor().dispose();
	}

	private CaptureBoundsShell getCaptureBoundsShell() {
		if (captureBoundsShell == null) {
			captureBoundsShell = new CaptureBoundsShell(Display.getCurrent());
		}
		return captureBoundsShell;
	}

	private Cursor getCaptureCursor() {
		if (captureCursor == null || captureCursor.isDisposed()) {
			ImageData imageData = SharedImages.getImageDescriptor(SharedImages.CAMERA).getImageData();
			captureCursor = new Cursor(display, imageData, 8, 8);
		}
		return captureCursor;
	}

	private ControlCapture getControlCapture() {
		if (controlCapture == null) {
			controlCapture = new ControlCapture();
		}
		return controlCapture;
	}

	private void handleEvent(Event event) {
		switch (event.type) {
			case SWT.KeyDown:
			case 3005:
				onKeyDown(event);
				break;

			case SWT.KeyUp:
				onKeyUp(event);
				break;

			case SWT.MouseMove:
				onMouseMove(event);
				break;

			case SWT.MouseDown:
				onMouseDown(event);
				break;

			default:
				break;
		}

		event.doit = false;
	}

	private void hook(int... eventTypes) {
		for (int each : eventTypes) {
			if (hookingTypes.contains(each)) {
				continue;
			}
			hookingTypes.add(each);
			display.addFilter(each, dispatcher);
		}
	}

	private void onKeyDown(Event event) {
		if (event.keyCode == SWT.ESC) {
			transite(HookingState.NONE);
			return;
		}

		switch (state) {
			case TRACK_CONTROL:
				if (event.keyCode == SWT.MOD1) {
					transite(HookingState.TRACK_SHELL);
					return;
				}

				switch (event.keyCode) {
					case SWT.ARROW_UP:
						Composite parent = controlUnderMouse.getParent();
						if (parent != null) {
							controlUnderMouse = parent;
							getCaptureBoundsShell().setTarget(controlUnderMouse);
						}
						break;
				}

			default:
				break;
		}

	}

	private void onKeyUp(Event event) {
		switch (state) {
			case TRACK_SHELL:
				if (event.keyCode == SWT.MOD1) {
					transite(HookingState.TRACK_CONTROL);
				}
				break;

			default:
				break;
		}
	}

	private void onMouseDown(Event event) {
		switch (state) {
			case TRACK_CONTROL:
			case TRACK_SHELL:
				capture();
				break;

			default:
				break;
		}
	}

	private void onMouseMove(Event event) {
		switch (state) {
			case TRACK_CONTROL:
				updateControlUnderMouse(event);
				getCaptureBoundsShell().setTarget(controlUnderMouse);
				break;

			case TRACK_SHELL:
				updateControlUnderMouse(event);
				getCaptureBoundsShell().setTarget(controlUnderMouse.getShell());
				break;

			default:
				break;
		}

	}

	/**
	 * 
	 * 3005 is used for comparability to support under version of 3.8,
	 * {@link ST#VerifyKey}
	 */
	public void start() {
		if (state != HookingState.NONE) {
			return;
		}

		hook(SWT.MouseMove, SWT.MouseDown, SWT.MouseUp, SWT.KeyDown, SWT.KeyUp, 3005);
		controlUnderMouse = display.getCursorControl();
		transite(HookingState.TRACK_CONTROL);
	}

	private void transite(HookingState nextState) {
		if (this.state == nextState) {
			return;
		}
		this.state = nextState;
		switch (state) {
			case TRACK_CONTROL:
				getCaptureBoundsShell().setTarget(controlUnderMouse);
				getCaptureBoundsShell().show();
				break;

			case TRACK_SHELL:
				getCaptureBoundsShell().setTarget(controlUnderMouse.getShell());
				getCaptureBoundsShell().show();
				break;

			case CAPTURING_CONTROL:
			case CAPTURING_SHELL:
				getCaptureBoundsShell().hide();
				break;

			case NONE:
				dispose();

				break;
		}
	}

	private void unhookAll() {
		for (Integer each : hookingTypes) {
			display.removeFilter(each, dispatcher);
		}
		hookingTypes.clear();
	}

	private void updateControlUnderMouse(Event event) {
		Control newControl = null;
		if (event.widget instanceof Control) {
			newControl = (Control) event.widget;
		}

		Control oldControl = this.controlUnderMouse;
		if (oldControl == newControl) {
			return;
		}

		this.controlUnderMouse = newControl;

		if (oldControl != null) {
			restoreCursor(oldControl);
		}

		if (newControl != null) {
			applyCaptureCursor(newControl);
		}
	}

	private void applyCaptureCursor(Control newControl) {
		Cursor oldCursor = newControl.getCursor();
		newControl.setData("pdetools-old-cursor", oldCursor);
		newControl.setCursor(getCaptureCursor());
	}

	private void restoreCursor(Control control) {
		Cursor oldCursor = (Cursor) control.getData("pdetools-old-cursor");
		if (oldCursor != null && oldCursor.isDisposed()) {
			oldCursor = null;
		}
		control.setCursor(oldCursor);
	}
}