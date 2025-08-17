/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 */
package org.catrobat.catroid.content.actions;

import android.util.Log;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;

import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadFileAction extends TemporalAction {
    private static final String TAG = "UploadFileAction";

    public Scope scope;
    public Formula url;
    public Formula filePath;
    public int fileTypeSelection;    // 0: Standard, 1: Binary
    public Formula mimeType;
    public int storageTypeSelection; // 0: Permanent, 1: Temporary

    @Override
    protected void update(float percent) {
        try {
            if (scope == null) return;
            final String urlStr = url.interpretString(scope);
            final String fileStr = filePath.interpretString(scope);
            final String mimeStr = mimeType.interpretString(scope);

            if (scope.getProject() == null) return;
            final File file2 = scope.getProject().getFile(fileStr);
            if (file2 == null) return;


            final String pathStr = file2.getAbsolutePath();

            if (urlStr == null || urlStr.isEmpty() || pathStr.isEmpty()) {
                return;
            }

            // Сетевые операции НЕЛЬЗЯ выполнять в основном потоке.
            // Запускаем их в новом потоке.
            new Thread(() -> {
                HttpURLConnection connection = null;
                try {
                    // Получаем файл. Предполагаем, что путь абсолютный.
                    FileHandle file = Gdx.files.absolute(pathStr);
                    if (!file.exists()) {
                        Log.e(TAG, "File not found: " + pathStr);
                        return;
                    }

                    URL targetUrl = new URL(urlStr);
                    connection = (HttpURLConnection) targetUrl.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setUseCaches(false);

                    // Устанавливаем заголовки
                    connection.setRequestProperty("Content-Type", mimeStr);
                    String storageTypeHeader = (storageTypeSelection == 0) ? "permanent" : "temporary";
                    connection.setRequestProperty("X-Storage-Type", storageTypeHeader);

                    DataOutputStream requestStream = new DataOutputStream(connection.getOutputStream());

                    // Читаем файл и пишем его в тело запроса
                    InputStream fileInputStream = file.read();
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        requestStream.write(buffer, 0, bytesRead);
                    }

                    fileInputStream.close();
                    requestStream.flush();
                    requestStream.close();

                    // Получаем ответ от сервера (важно для завершения запроса)
                    int responseCode = connection.getResponseCode();
                    Log.i(TAG, "Server responded with code: " + responseCode);

                } catch (Exception e) {
                    Log.e(TAG, "Error uploading file", e);
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}