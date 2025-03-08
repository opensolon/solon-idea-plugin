package org.noear.solon.idea.plugin.initializr;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.util.PathUtil;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.io.ZipUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;

public class InitializerDownloader {

    private static final Logger log = Logger.getInstance(InitializerDownloader.class);

    private final SolonInitializrBuilder builder;

    InitializerDownloader(SolonInitializrBuilder builder) {
        this.builder = builder;
    }

    @NotNull
    private String extractFilenameFromContentDisposition(@Nullable String contentDispositionHeader) {
        String fileName = null;
        if (contentDispositionHeader != null) {
            fileName = contentDispositionHeader
                    .replaceFirst(".*filename=\"?(?<fileName>[^;\"]+);?\"?.*", "${fileName}");
        }
        return !StringUtil.isEmpty(fileName) ? fileName : "unknown";
    }

    void execute(ProgressIndicator indicator) throws IOException {
        File tempFile = FileUtil.createTempFile("solon-initializr-template", ".tmp", true);
        String downloadUrl = builder.getMetadata().buildDownloadUrl();
        debug(() -> log.debug("Downloading project from: " + downloadUrl));
        Download download = HttpRequests.request(downloadUrl).connect(request -> {
            String contentType = request.getConnection().getContentType();
            boolean zip = StringUtil.isNotEmpty(contentType) && contentType.contains("zip");
            String contentDisposition = request.getConnection().getHeaderField("Content-Disposition");
            String fileName = extractFilenameFromContentDisposition(contentDisposition);
            indicator.setText(fileName);
            request.saveToFile(tempFile, indicator);
            return new Download(zip, fileName);
        });
        indicator.setText("Please wait ...");
        File targetExtractionDir = new File(requireNonNull(builder.getContentEntryPath()));
        if (download.zip) {
            ZipUtil.extract(Path.of(tempFile.getAbsolutePath()), Path.of(targetExtractionDir.getAbsolutePath()), null);
        } else {
            FileUtil.copy(tempFile, new File(targetExtractionDir, download.fileName));
        }
        VirtualFile targetFile =
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetExtractionDir);
        RefreshQueue.getInstance().refresh(false, true, null, targetFile);
    }


    private void debug(Runnable doWhenDebug) {
        if (log.isDebugEnabled()) {
            doWhenDebug.run();
        }
    }

    private static class Download {
        private final boolean zip;
        private final String fileName;

        public Download(boolean zip, String fileName) {
            this.zip = zip;
            this.fileName = fileName;
        }
    }

}
