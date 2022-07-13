package nprime.reg.mocksbi.workmanager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;

public class FileUriWorker extends Worker {
    public FileUriWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String fileName = getInputData().getString("FileName");
        String strUri = getInputData().getString("Uri");

        Uri uri = Uri.parse(strUri);
        getApplicationContext().revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

        //Log.d("FUWorker", "Worker Method invoked");
        File file = new File(fileName);
        if(file.exists()){
            if(file.delete()){
                Log.d("FUWorker", "File deleted");
                return Result.success();
            }
        }else{
            return Result.success();
        }
        return Result.retry();
    }
}
