
package com.mclear.classdroid.threads;

import org.xmlrpc.android.WebUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.mclear.classdroid.R;
import com.mclear.classdroid.bo.Post;
import com.mclear.classdroid.bo.Pupil;
import com.mclear.classdroid.bo.PupilServices;
import com.mclear.classdroid.db.DBAdapter;
import com.mclear.classdroid.services.ClassdroidService;

public class UploaderThread extends Thread {
    private long postId;

    private Context context;

    public UploaderThread(long postId, Context context) {
        this.postId = postId;
        this.context = context;
    }

    @Override
    public void run() {
        DBAdapter dbAdapter = new DBAdapter(context);
        try {
            NotificationManager nManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            nManager.cancel((int) (R.string.app_name + postId));

            dbAdapter.open();
            Post post = dbAdapter.getPostById(postId);
            Pupil pupil = dbAdapter.getPupilById(post.getPupilId());
            PupilServices service = dbAdapter.getPupilServiceByPupilId(post.getPupilId(),
                    PupilServices.TYPE_PRIMARYBLOGGER);
            dbAdapter.close();
            if (service.getIsEnabled() == PupilServices.SERVICE_ENABLED) {
                dbAdapter.open();
                String url = WebUtils.uploadPostToWordpress(pupil, post.getLocalImagePath(), ""
                        + post.getGrade(), service, context);
                post.setIsPosted(1);
                post.setReturnedString(url);
                dbAdapter.updatePost(post);
                dbAdapter.close();
            } else {
                post.setReturnedString("");
            }
            dbAdapter.open();
            service = dbAdapter.getPupilServiceByPupilId(post.getPupilId(),
                    PupilServices.TYPE_XPARENA);
            dbAdapter.close();
            if (service.getIsEnabled() == PupilServices.SERVICE_ENABLED) {
                WebUtils.uploadPostToXPArena(service, pupil, post.getReturnedString(), "500");
            }
        } catch (Exception e) {
            e.printStackTrace();
            NotificationManager nManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            nManager.cancel(R.string.app_name);
            Intent intent = new Intent(context, ClassdroidService.class);
            intent.putExtra("id", postId);
            PendingIntent pendingIntent = PendingIntent
                    .getService(context, (int) postId, intent, 0);
            Notification notification = new Notification(R.drawable.icon, context
                    .getString(R.string.lab_classdroid_error), System.currentTimeMillis());
            notification.setLatestEventInfo(context, context.getString(R.string.lab_upload_failed),
                    context.getString(R.string.lab_try_again), pendingIntent);
            nManager.notify((int) (R.string.app_name + postId), notification);
        } finally {
            dbAdapter.close();
        }
    }
}
