package nprime.reg.mocksbi.utility;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Pair;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileUtils {

    public static void SaveFileInAppStorage(final Context context, Uri fileUri, String fileName) throws IOException {
        ParcelFileDescriptor inputPFD = context.getContentResolver().openFileDescriptor(fileUri, "r");
        FileDescriptor fileDescriptor = inputPFD.getFileDescriptor();

        String[] files = context.fileList();

        for (String file : files) {
            if (file.equals(fileName)) {
                //Deleting existing file
                context.deleteFile(fileName);
                break;
            }
        }

        try (FileChannel fileInChannel = new FileInputStream(fileDescriptor).getChannel(); FileChannel fileOutChannel = context.openFileOutput(fileName, Context.MODE_PRIVATE).getChannel()) {
            fileInChannel.transferTo(0, fileInChannel.size(), fileOutChannel);
        }
    }

    public static Pair<String, String> getFileNameAndSize(Context context, final Uri returnUri) {
        Cursor returnCursor =
                context.getContentResolver().query(returnUri, null, null, null, null);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();

        String fileName = returnCursor.getString(nameIndex);
        long fileSize = returnCursor.getLong(sizeIndex);
        returnCursor.close();

        String fileSizeInKb = (fileSize / 1024) + "kb";
        return new Pair<>(fileName, fileSizeInKb);
    }
}
