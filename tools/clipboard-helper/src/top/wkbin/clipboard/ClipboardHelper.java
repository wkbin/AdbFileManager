package top.wkbin.clipboard;

import android.content.ClipData;
import android.os.IBinder;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

/** Runs under Android's shell UID via app_process and writes the system clipboard. */
public final class ClipboardHelper {
    private ClipboardHelper() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: ClipboardHelper <utf8-file> <user-id>");
        }

        String text = readUtf8(args[0]);
        int userId = Integer.parseInt(args[1]);
        Object clipboard = getClipboardService();
        Method setter = findSetPrimaryClip(clipboard.getClass());
        Object[] invocationArgs = buildInvocationArgs(setter.getParameterTypes(), text, userId);

        try {
            setter.invoke(clipboard, invocationArgs);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            throw exception;
        }
        System.out.println("CLIPBOARD_SET_OK");
    }

    private static Object getClipboardService() throws Exception {
        Class<?> serviceManager = Class.forName("android.os.ServiceManager");
        IBinder binder = (IBinder) serviceManager
                .getMethod("getService", String.class)
                .invoke(null, "clipboard");
        if (binder == null) throw new IllegalStateException("Clipboard service is unavailable");

        Class<?> stub = Class.forName("android.content.IClipboard$Stub");
        return stub.getMethod("asInterface", IBinder.class).invoke(null, binder);
    }

    private static Method findSetPrimaryClip(Class<?> type) {
        for (Method method : type.getMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (method.getName().equals("setPrimaryClip")
                    && parameters.length > 0
                    && parameters[0] == ClipData.class) {
                return method;
            }
        }
        throw new IllegalStateException("Android clipboard API is unsupported on this device");
    }

    private static Object[] buildInvocationArgs(Class<?>[] types, String text, int userId) {
        Object[] values = new Object[types.length];
        int stringIndex = 0;
        int intIndex = 0;
        for (int index = 0; index < types.length; index++) {
            Class<?> type = types[index];
            if (type == ClipData.class) {
                values[index] = ClipData.newPlainText("AdbFileManager", text);
            } else if (type == String.class) {
                values[index] = stringIndex++ == 0 ? "com.android.shell" : null;
            } else if (type == int.class || type == Integer.class) {
                values[index] = intIndex++ == 0 ? userId : 0;
            } else if (type == boolean.class || type == Boolean.class) {
                values[index] = false;
            } else {
                values[index] = null;
            }
        }
        return values;
    }

    private static String readUtf8(String path) throws Exception {
        try (FileInputStream input = new FileInputStream(path);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
