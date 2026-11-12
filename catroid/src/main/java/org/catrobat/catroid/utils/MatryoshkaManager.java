package org.catrobat.catroid.utils;

import android.content.Context;
import android.util.Log;

import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.io.StorageOperations;
import org.catrobat.catroid.io.ZipArchiver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MatryoshkaManager {
    private static final String TAG = "MatryoshkaManager";
    private static final String DUMMY_PATH = "Scene/sounds/dummy.mp3";

    public static File packForUpload(Context context, Project realProject) throws Exception {
        /* DELETED */

        return null;
    }

    private static void copyScreenshots(File sourceDir, File destDir) {
        String[] names = { "automatic_screenshot.png", "manual_screenshot.png" };
        for (String name : names) {
            File src = new File(sourceDir, name);
            if (src.exists()) {
                try {
                    StorageOperations.copyFile(src, new File(destDir, name));
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка копирования скриншота: " + name);
                }
            }
        }
    }

    private static File findFileRecursive(File dir, String name) {
        File[] list = dir.listFiles();
        if (list == null) return null;
        for (File f : list) {
            if (f.isDirectory()) {
                File found = findFileRecursive(f, name);
                if (found != null) return found;
            } else if (f.getName().equalsIgnoreCase(name)) {
                return f;
            }
        }
        return null;
    }

    private static void modifyStubXml(File xmlFile, Project realProject) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(xmlFile), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        reader.close();

        String xml = sb.toString()
                .replace("###PROJECT_NAME###", escapeXml(realProject.getName()))
                .replace("###DESCRIPTION###", escapeXml("Made with Ch-engine and some love :3 \n" + realProject.getDescription()));

        try (FileOutputStream fos = new FileOutputStream(xmlFile)) {
            fos.write(xml.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static boolean unpackIfMatryoshka(File projectDir) {
        Log.d(TAG, "Проверка проекта на наличие матрешки в: " + projectDir.getAbsolutePath());

        List<File> wavFiles = new ArrayList<>();
        findWavFilesRecursive(projectDir, wavFiles);

        for (File wavFile : wavFiles) {
            File extractedZip = new File(projectDir.getParent(), "payload_extract_" + UUID.randomUUID() + ".zip");
            try {
                if (ChEnginePayloadHelper.extractPayloadFromAudio(wavFile, extractedZip)) {
                    StorageOperations.deleteDir(projectDir);
                    projectDir.mkdirs();
                    new ZipArchiver().unzip(extractedZip, projectDir);

                    extractedZip.delete();
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при попытке распаковки: " + e.getMessage());
            } finally {
                if (extractedZip.exists()) extractedZip.delete();
            }
        }

        Log.d(TAG, "Это обычный проект.");
        return false;
    }

    private static void findWavFilesRecursive(File dir, List<File> fileList) {
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) {
                findWavFilesRecursive(f, fileList);
            } else if (f.getName().toLowerCase().endsWith(".mp3")) {
                fileList.add(f);
            }
        }
    }
}
