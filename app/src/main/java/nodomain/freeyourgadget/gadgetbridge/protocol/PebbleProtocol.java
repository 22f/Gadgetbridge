package nodomain.freeyourgadget.gadgetbridge.protocol;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.SimpleTimeZone;

import nodomain.freeyourgadget.gadgetbridge.GBCommand;
import nodomain.freeyourgadget.gadgetbridge.GBDeviceApp;

public class PebbleProtocol {

    static private String TAG = "PebbleProtocol";

    static final short ENDPOINT_FIRMWARE = 1;
    static final short ENDPOINT_TIME = 11;
    static final short ENDPOINT_FIRMWAREVERSION = 16;
    public static final short ENDPOINT_PHONEVERSION = 17;
    static final short ENDPOINT_SYSTEMMESSAGE = 18;
    static final short ENDPOINT_MUSICCONTROL = 32;
    static final short ENDPOINT_PHONECONTROL = 33;
    static final short ENDPOINT_APPLICATIONMESSAGE = 48;
    static final short ENDPOINT_LAUNCHER = 49;
    static final short ENDPOINT_LOGS = 2000;
    static final short ENDPOINT_PING = 2001;
    static final short ENDPOINT_LOGDUMP = 2002;
    static final short ENDPOINT_RESET = 2003;
    static final short ENDPOINT_APP = 2004;
    static final short ENDPOINT_APPLOGS = 2006;
    static final short ENDPOINT_NOTIFICATION = 3000;
    static final short ENDPOINT_RESOURCE = 4000;
    static final short ENDPOINT_SYSREG = 5000;
    static final short ENDPOINT_FCTREG = 5001;
    static final short ENDPOINT_APPMANAGER = 6000;
    public static final short ENDPOINT_DATALOG = 6778;
    static final short ENDPOINT_RUNKEEPER = 7000;
    static final short ENDPOINT_SCREENSHOT = 8000;
    static final short ENDPOINT_PUTBYTES = (short) 48879;

    static final byte NOTIFICATION_EMAIL = 0;
    static final byte NOTIFICATION_SMS = 1;
    static final byte NOTIFICATION_TWITTER = 2;
    static final byte NOTIFICATION_FACEBOOK = 3;

    static final byte PHONECONTROL_ANSWER = 1;
    static final byte PHONECONTROL_HANGUP = 2;
    static final byte PHONECONTROL_GETSTATE = 3;
    static final byte PHONECONTROL_INCOMINGCALL = 4;
    static final byte PHONECONTROL_OUTGOINGCALL = 5;
    static final byte PHONECONTROL_MISSEDCALL = 6;
    static final byte PHONECONTROL_RING = 7;
    static final byte PHONECONTROL_START = 8;
    static final byte PHONECONTROL_END = 9;

    static final byte MUSICCONTROL_SETMUSICINFO = 16;
    static final byte MUSICCONTROL_PLAYPAUSE = 1;
    static final byte MUSICCONTROL_PAUSE = 2;
    static final byte MUSICCONTROL_PLAY = 3;
    static final byte MUSICCONTROL_NEXT = 4;
    static final byte MUSICCONTROL_PREVIOUS = 5;
    static final byte MUSICCONTROL_VOLUMEUP = 6;
    static final byte MUSICCONTROL_VOLUMEDOWN = 7;
    static final byte MUSICCONTROL_GETNOWPLAYING = 7;

    static final byte TIME_GETTIME = 0;
    static final byte TIME_SETTIME = 2;

    static final byte FIRMWAREVERSION_GETVERSION = 0;

    static final byte APPMANAGER_GETAPPBANKSTATUS = 1;
    static final byte APPMANAGER_REMOVEAPP = 2;

    static final int APPMANAGER_RES_SUCCESS = 1;

    static final short LENGTH_PREFIX = 4;
    static final short LENGTH_SETTIME = 5;
    static final short LENGTH_REMOVEAPP = 9;
    static final short LENGTH_PHONEVERSION = 17;


    static final byte PHONEVERSION_APPVERSION_MAGIC = 2; // increase this if pebble complains
    static final byte PHONEVERSION_APPVERSION_MAJOR = 2;
    static final byte PHONEVERSION_APPVERSION_MINOR = 3;
    static final byte PHONEVERSION_APPVERSION_PATCH = 0;


    static final int PHONEVERSION_SESSION_CAPS_GAMMARAY = (int) 0x80000000;

    static final int PHONEVERSION_REMOTE_CAPS_TELEPHONY = 0x00000010;
    static final int PHONEVERSION_REMOTE_CAPS_SMS = 0x00000020;
    static final int PHONEVERSION_REMOTE_CAPS_GPS = 0x00000040;
    static final int PHONEVERSION_REMOTE_CAPS_BTLE = 0x00000080;
    static final int PHONEVERSION_REMOTE_CAPS_REARCAMERA = 0x00000100;
    static final int PHONEVERSION_REMOTE_CAPS_ACCEL = 0x00000200;
    static final int PHONEVERSION_REMOTE_CAPS_GYRO = 0x00000400;
    static final int PHONEVERSION_REMOTE_CAPS_COMPASS = 0x00000800;

    static final byte PHONEVERSION_REMOTE_OS_UNKNOWN = 0;
    static final byte PHONEVERSION_REMOTE_OS_IOS = 1;
    public static final byte PHONEVERSION_REMOTE_OS_ANDROID = 2;
    static final byte PHONEVERSION_REMOTE_OS_OSX = 3;
    static final byte PHONEVERSION_REMOTE_OS_LINUX = 4;
    static final byte PHONEVERSION_REMOTE_OS_WINDOWS = 5;

    private static byte[] encodeMessage(short endpoint, byte type, int cookie, String[] parts) {
        // Calculate length first
        int length = LENGTH_PREFIX + 1;
        if (parts != null) {
            for (String s : parts) {
                if (s == null || s.equals("")) {
                    length++; // encode null or empty strings as 0x00 later
                    continue;
                }
                length += (1 + s.getBytes().length);
            }
        }
        if (endpoint == ENDPOINT_PHONECONTROL) {
            length += 4; //for cookie;
        }

        // Encode Prefix
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) (length - LENGTH_PREFIX));
        buf.putShort(endpoint);
        buf.put(type);

        if (endpoint == ENDPOINT_PHONECONTROL) {
            buf.putInt(cookie);
        }
        // Encode Pascal-Style Strings
        if (parts != null) {
            for (String s : parts) {
                if (s == null || s.equals("")) {
                    //buf.put((byte)0x01);
                    buf.put((byte) 0x00);
                    continue;
                }

                int partlength = s.getBytes().length;
                if (partlength > 255) partlength = 255;
                buf.put((byte) partlength);
                buf.put(s.getBytes(), 0, partlength);
            }
        }
        return buf.array();
    }

    public static byte[] encodeSMS(String from, String body) {
        Long ts = System.currentTimeMillis() / 1000;
        ts += SimpleTimeZone.getDefault().getOffset(ts) / 1000;
        String tsstring = ts.toString();  // SIC
        String[] parts = {from, body, tsstring};

        return encodeMessage(ENDPOINT_NOTIFICATION, NOTIFICATION_SMS, 0, parts);
    }

    public static byte[] encodeEmail(String from, String subject, String body) {
        Long ts = System.currentTimeMillis() / 1000;
        ts += SimpleTimeZone.getDefault().getOffset(ts) / 1000;
        String tsstring = ts.toString(); // SIC
        String[] parts = {from, body, tsstring, subject};

        return encodeMessage(ENDPOINT_NOTIFICATION, NOTIFICATION_EMAIL, 0, parts);
    }

    public static byte[] encodeSetTime(long ts) {
        if (ts == -1) {
            ts = System.currentTimeMillis() / 1000;
            ts += SimpleTimeZone.getDefault().getOffset(ts) / 1000;
        }
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_SETTIME);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_SETTIME);
        buf.putShort(ENDPOINT_TIME);
        buf.put(TIME_SETTIME);
        buf.putInt((int) ts);

        return buf.array();
    }

    public static byte[] encodeSetCallState(String number, String name, GBCommand command) {
        String[] parts = {number, name};
        byte pebbleCmd;
        switch (command) {
            case CALL_START:
                pebbleCmd = PHONECONTROL_START;
                break;
            case CALL_END:
                pebbleCmd = PHONECONTROL_END;
                break;
            case CALL_INCOMING:
                pebbleCmd = PHONECONTROL_INCOMINGCALL;
                break;
            case CALL_OUTGOING:
                // pebbleCmd = PHONECONTROL_OUTGOINGCALL;
                /*
                 *  HACK/WORKAROUND for non-working outgoing call display.
                 *  Just send a incoming call command immediately followed by a start call command
                 *  This prevents vibration of the Pebble.
                 */
                byte[] callmsg = encodeMessage(ENDPOINT_PHONECONTROL, PHONECONTROL_INCOMINGCALL, 0, parts);
                byte[] startmsg = encodeMessage(ENDPOINT_PHONECONTROL, PHONECONTROL_START, 0, parts);
                byte[] msg = new byte[callmsg.length + startmsg.length];
                System.arraycopy(callmsg, 0, msg, 0, callmsg.length);
                System.arraycopy(startmsg, 0, msg, startmsg.length, startmsg.length);
                return msg;
            // END HACK
            default:
                return null;
        }
        return encodeMessage(ENDPOINT_PHONECONTROL, pebbleCmd, 0, parts);
    }

    public static byte[] encodeSetMusicInfo(String artist, String album, String track) {
        String[] parts = {artist, album, track};
        return encodeMessage(ENDPOINT_MUSICCONTROL, MUSICCONTROL_SETMUSICINFO, 0, parts);
    }

    public static byte[] encodeFirmwareVersionReq() {
        return encodeMessage(ENDPOINT_FIRMWAREVERSION, FIRMWAREVERSION_GETVERSION, 0, null);
    }

    public static byte[] encodeAppInfoReq() {
        return encodeMessage(ENDPOINT_APPMANAGER, APPMANAGER_GETAPPBANKSTATUS, 0, null);
    }

    public static byte[] encodeAppDelete(int id, int index) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_REMOVEAPP);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_REMOVEAPP);
        buf.putShort(ENDPOINT_APPMANAGER);
        buf.put(APPMANAGER_REMOVEAPP);
        buf.putInt(id);
        buf.putInt(index);

        return buf.array();
    }

    public static byte[] encodePhoneVersion(byte os) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_PHONEVERSION);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_PHONEVERSION);
        buf.putShort(ENDPOINT_PHONEVERSION);
        buf.put((byte) 0x01);
        buf.putInt(-1); //0xffffffff

        if (os == PHONEVERSION_REMOTE_OS_ANDROID) {
            buf.putInt(PHONEVERSION_SESSION_CAPS_GAMMARAY);
        } else {
            buf.putInt(0);
        }
        buf.putInt(PHONEVERSION_REMOTE_CAPS_SMS | PHONEVERSION_REMOTE_CAPS_TELEPHONY | os);

        buf.put(PHONEVERSION_APPVERSION_MAGIC);
        buf.put(PHONEVERSION_APPVERSION_MAJOR);
        buf.put(PHONEVERSION_APPVERSION_MINOR);
        buf.put(PHONEVERSION_APPVERSION_PATCH);

        return buf.array();
    }

    public static GBDeviceCommand decodeResponse(byte[] responseData) {
        ByteBuffer buf = ByteBuffer.wrap(responseData);
        buf.order(ByteOrder.BIG_ENDIAN);
        short length = buf.getShort();
        short endpoint = buf.getShort();
        byte pebbleCmd = buf.get();
        GBDeviceCommand cmd = null;
        switch (endpoint) {
            case ENDPOINT_MUSICCONTROL:
                GBDeviceCommandMusicControl musicCmd = new GBDeviceCommandMusicControl();
                switch (pebbleCmd) {
                    case MUSICCONTROL_NEXT:
                        musicCmd.command = GBDeviceCommandMusicControl.Command.NEXT;
                        break;
                    case MUSICCONTROL_PREVIOUS:
                        musicCmd.command = GBDeviceCommandMusicControl.Command.PREVIOUS;
                        break;
                    case MUSICCONTROL_PLAY:
                        musicCmd.command = GBDeviceCommandMusicControl.Command.PLAY;
                        break;
                    case MUSICCONTROL_PAUSE:
                        musicCmd.command = GBDeviceCommandMusicControl.Command.PAUSE;
                        break;
                    case MUSICCONTROL_PLAYPAUSE:
                        musicCmd.command = GBDeviceCommandMusicControl.Command.PLAYPAUSE;
                        break;
                    default:
                        break;
                }
                cmd = musicCmd;
                break;
            case ENDPOINT_PHONECONTROL:
                GBDeviceCommandCallControl callCmd = new GBDeviceCommandCallControl();
                switch (pebbleCmd) {
                    case PHONECONTROL_HANGUP:
                        callCmd.command = GBDeviceCommandCallControl.Command.END;
                        break;
                    default:
                        Log.i(TAG, "Unknown PHONECONTROL command" + pebbleCmd);
                        break;
                }
                cmd = callCmd;
                break;
            case ENDPOINT_FIRMWAREVERSION:
                GBDeviceCommandVersionInfo versionCmd = new GBDeviceCommandVersionInfo();

                int version = buf.getInt();
                byte[] versionString = new byte[32];
                buf.get(versionString, 0, 32);

                versionCmd.fwVersion = new String(versionString).trim();
                cmd = versionCmd;
                break;
            case ENDPOINT_APPMANAGER:
                switch (pebbleCmd) {
                    case APPMANAGER_GETAPPBANKSTATUS:
                        GBDeviceCommandAppInfo appInfoCmd = new GBDeviceCommandAppInfo();
                        int banks = buf.getInt();
                        int banksUsed = buf.getInt();
                        byte[] appName = new byte[32];
                        byte[] creatorName = new byte[32];
                        appInfoCmd.apps = new GBDeviceApp[banksUsed];

                        for (int i = 0; i < banksUsed; i++) {
                            int id = buf.getInt();
                            int index = buf.getInt();
                            buf.get(appName, 0, 32);
                            buf.get(creatorName, 0, 32);
                            int flags = buf.getInt();
                            Short appVersion = buf.getShort();
                            appInfoCmd.apps[i] = new GBDeviceApp(id, index, new String(appName).trim(), new String(creatorName).trim(), appVersion.toString());
                        }
                        cmd = appInfoCmd;
                        break;
                    case APPMANAGER_REMOVEAPP:
                        GBDeviceCommandAppManagementResult deleteRes = new GBDeviceCommandAppManagementResult();
                        deleteRes.type = GBDeviceCommandAppManagementResult.CommandType.DELETE;

                        int result = buf.getInt();
                        switch (result) {
                            case APPMANAGER_RES_SUCCESS:
                                deleteRes.result = GBDeviceCommandAppManagementResult.Result.SUCCESS;
                                break;
                            default:
                                deleteRes.result = GBDeviceCommandAppManagementResult.Result.FAILURE;
                                break;
                        }
                        cmd = deleteRes;
                        break;
                    default:
                        Log.i(TAG, "Unknown APPMANAGER command" + pebbleCmd);
                        break;
                }
                break;
            default:
                break;
        }

        return cmd;
    }
}
