package ioio.lib.pic;

import android.util.Log;

import ioio.lib.IOIOException.ConnectionLostException;
import ioio.lib.IOIOException.InvalidStateException;
import ioio.lib.Input;

/**
 * Represent and manage digital input pins on the IOIO.
 *
 * @author arshan
 */
public class DigitalInput extends IOIOPin implements IOIOPacketListener, Input<Boolean> {

	public static final int FLOATING = 0;
	public static final int PULL_UP = 1;
	public static final int PULL_DOWN = 2;

	private boolean active = false;
	private boolean state = false;

	IOIOImpl ioio;

	DigitalInput(IOIOImpl ioio, PacketFramerRegistry registry, int pin, int mode) throws ConnectionLostException {
		super(pin);
		this.ioio = ioio;
		ioio.registerListener(this);
		registry.registerFramer(Constants.SET_INPUT, SET_DIGITAL_INPUT_PACKET_FRAMER);
		registry.registerFramer(Constants.REPORT_DIGITAL_STATUS, REPORT_DIGITAL_STATUS_PACKET_FRAMER);
		registry.registerFramer(Constants.SET_CHANGE_NOTIFY, CHANGE_NOTIFY_HANDLER);
		init(mode);
	}

	private void init(int mode) throws ConnectionLostException {
		ioio.queuePacket(new IOIOPacket(
			Constants.SET_INPUT,
			new byte[]{ (byte)(pin << 2 | mode) }
			));
		ioio.queuePacket(new IOIOPacket(
			Constants.SET_CHANGE_NOTIFY,
			new byte[]{(byte)(pin<<2 | 1)}
		));
	}

	@Override
    public Boolean read() throws InvalidStateException {
       if (isInvalid()) {
            throw Constants.INVALID_STATE_EXCEPTION;
        }
		return state;
	}

	@Override
    public void handlePacket(IOIOPacket packet) {
		// TODO(arshan): is it active before the first report?
		switch(packet.message) {
		case Constants.SET_INPUT:
			active = true;
			Log.i("IOIO","pin " + pin + " set as input");
			break;
		case Constants.REPORT_DIGITAL_STATUS:
			if (active && packet.payload[0] >> 2 == pin) {
				state = ((packet.payload[0] & 0x1) == 0)? false : true;
				Log.i("IOIO", "pin " + pin + " status is here : " + (state?"Hi":"Low"));
			}
			break;
		}

	}

    @Override
    public void close() {
        // TODO(TF)
    }

    private static final PacketFramer CHANGE_NOTIFY_HANDLER = PacketFramers.getNBytePacketFramerFor(Constants.SET_CHANGE_NOTIFY, 1);

    private static final PacketFramer SET_DIGITAL_INPUT_PACKET_FRAMER =
        PacketFramers.getNBytePacketFramerFor(Constants.SET_INPUT, 1);

    private static final PacketFramer REPORT_DIGITAL_STATUS_PACKET_FRAMER =
        PacketFramers.getNBytePacketFramerFor(Constants.REPORT_DIGITAL_STATUS, 1);
}