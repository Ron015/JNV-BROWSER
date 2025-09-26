package com.example.advancedbrowser;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.webkit.DownloadListener;
import android.widget.Toast;

public class AdvancedDownloadListener implements DownloadListener {
    private Context context;
    
    public AdvancedDownloadListener(Context context) {
        this.context = context;
    }
    
    @Override
    public void onDownloadStart(String url, String userAgent, 
                               String contentDisposition, String mimetype, 
                               long contentLength) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        
        // Set download details
        request.setMimeType(mimetype);
        request.addRequestHeader("User-Agent", userAgent);
        
        // Set title and description
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
        request.setTitle(fileName);
        request.setDescription("Downloading file...");
        
        // Set destination
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        
        // Get download service and enqueue file
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
        
        Toast.makeText(context, "Download started: " + fileName, Toast.LENGTH_LONG).show();
    }
}