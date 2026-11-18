package com.smartwater.backup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Backup and restore service for database and data.
 * Supports automatic backups and manual restore operations.
 */
public class BackupService {
    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);
    private static BackupService instance;
    private static final String BACKUP_DIR = "backups";
    private static final String DATABASE_FILE = "data/smartwater.db";
    private static final String BACKUP_FORMAT = "yyyy-MM-dd_HH-mm-ss";

    /**
     * Private constructor for singleton pattern.
     */
    private BackupService() {
        try {
            Files.createDirectories(Paths.get(BACKUP_DIR));
            logger.info("BackupService initialized");
        } catch (IOException e) {
            logger.error("Failed to create backup directory: {}", e.getMessage(), e);
        }
    }

    /**
     * Gets the singleton instance.
     *
     * @return the BackupService instance
     */
    public static synchronized BackupService getInstance() {
        if (instance == null) {
            instance = new BackupService();
        }
        return instance;
    }

    /**
     * Creates a backup of the database.
     *
     * @return backup file path
     * @throws IOException if backup fails
     */
    public String createBackup() throws IOException {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(BACKUP_FORMAT));
            String backupFileName = String.format("smartwater_backup_%s.db", timestamp);
            Path backupPath = Paths.get(BACKUP_DIR, backupFileName);

            Path sourceFile = Paths.get(DATABASE_FILE);

            // Check if database file exists
            if (!Files.exists(sourceFile)) {
                throw new FileNotFoundException("Database file not found: " + DATABASE_FILE);
            }

            // Copy database file to backup location
            Files.copy(sourceFile, backupPath, StandardCopyOption.REPLACE_EXISTING);

            logger.info("Database backup created successfully: {}", backupPath);
            return backupPath.toString();

        } catch (IOException e) {
            logger.error("Failed to create backup: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Creates an automatic backup if not done recently.
     *
     * @param intervalMinutes minimum minutes between backups
     * @return backup file path if created, null otherwise
     * @throws IOException if backup fails
     */
    public String createAutomaticBackup(int intervalMinutes) throws IOException {
        try {
            String lastBackupFile = getLastBackupFile();

            if (lastBackupFile != null) {
                LocalDateTime lastBackupTime = extractTimestampFromBackup(lastBackupFile);
                LocalDateTime now = LocalDateTime.now();

                if (lastBackupTime != null &&
                        lastBackupTime.plusMinutes(intervalMinutes).isAfter(now)) {
                    logger.info("Automatic backup skipped: recent backup exists");
                    return null;
                }
            }

            return createBackup();

        } catch (Exception e) {
            logger.error("Failed to create automatic backup: {}", e.getMessage(), e);
            throw new IOException("Automatic backup failed", e);
        }
    }

    /**
     * Lists all available backups.
     *
     * @return list of backup file names
     */
    public List<String> listBackups() {
        List<String> backups = new ArrayList<>();

        try (Stream<Path> paths = Files.list(Paths.get(BACKUP_DIR))) {
            paths.filter(path -> path.toString().endsWith(".db"))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .sorted()
                    .forEach(backups::add);

            logger.info("Found {} backups", backups.size());

        } catch (IOException e) {
            logger.error("Failed to list backups: {}", e.getMessage(), e);
        }

        return backups;
    }

    /**
     * Restores database from a backup.
     *
     * @param backupFileName the backup file name to restore from
     * @return true if restore succeeds
     * @throws IOException if restore fails
     */
    public boolean restoreFromBackup(String backupFileName) throws IOException {
        try {
            Path backupFile = Paths.get(BACKUP_DIR, backupFileName);

            if (!Files.exists(backupFile)) {
                throw new FileNotFoundException("Backup file not found: " + backupFile);
            }

            Path targetFile = Paths.get(DATABASE_FILE);

            // Create backup of current database before restoring
            if (Files.exists(targetFile)) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(BACKUP_FORMAT));
                Path currentBackup = Paths.get(BACKUP_DIR, String.format("pre_restore_backup_%s.db", timestamp));
                Files.copy(targetFile, currentBackup, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Current database backed up to: {}", currentBackup);
            }

            // Restore from backup
            Files.copy(backupFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

            logger.info("Database restored successfully from: {}", backupFile);
            return true;

        } catch (IOException e) {
            logger.error("Failed to restore backup: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Deletes an old backup file.
     *
     * @param backupFileName the backup file name to delete
     * @return true if deletion succeeds
     */
    public boolean deleteBackup(String backupFileName) {
        try {
            Path backupFile = Paths.get(BACKUP_DIR, backupFileName);

            if (!Files.exists(backupFile)) {
                logger.warn("Backup file not found: {}", backupFile);
                return false;
            }

            Files.delete(backupFile);
            logger.info("Backup deleted: {}", backupFile);
            return true;

        } catch (IOException e) {
            logger.error("Failed to delete backup: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Cleans up old backups, keeping only the most recent ones.
     *
     * @param keepCount number of backups to keep
     * @return number of backups deleted
     */
    public int cleanupOldBackups(int keepCount) {
        try {
            List<String> backups = listBackups();

            if (backups.size() <= keepCount) {
                logger.info("No old backups to clean up");
                return 0;
            }

            int deletedCount = 0;
            for (int i = 0; i < backups.size() - keepCount; i++) {
                if (deleteBackup(backups.get(i))) {
                    deletedCount++;
                }
            }

            logger.info("Cleaned up {} old backups, keeping {}", deletedCount, keepCount);
            return deletedCount;

        } catch (Exception e) {
            logger.error("Error during backup cleanup: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Gets size of a backup file in bytes.
     *
     * @param backupFileName the backup file name
     * @return size in bytes
     */
    public long getBackupSize(String backupFileName) {
        try {
            Path backupFile = Paths.get(BACKUP_DIR, backupFileName);
            return Files.size(backupFile);
        } catch (IOException e) {
            logger.error("Failed to get backup size: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Gets the last backup file.
     *
     * @return backup file name, or null if none exist
     */
    private String getLastBackupFile() {
        List<String> backups = listBackups();
        return backups.isEmpty() ? null : backups.get(backups.size() - 1);
    }

    /**
     * Extracts timestamp from backup file name.
     *
     * @param backupFileName the backup file name
     * @return LocalDateTime or null if extraction fails
     */
    private LocalDateTime extractTimestampFromBackup(String backupFileName) {
        try {
            // Format: smartwater_backup_yyyy-MM-dd_HH-mm-ss.db
            String timestampStr = backupFileName
                    .replace("smartwater_backup_", "")
                    .replace(".db", "");

            return LocalDateTime.parse(timestampStr,
                    DateTimeFormatter.ofPattern(BACKUP_FORMAT));

        } catch (Exception e) {
            logger.error("Failed to parse backup timestamp: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Creates a backup info string.
     *
     * @param backupFileName the backup file name
     * @return info string
     */
    public String getBackupInfo(String backupFileName) {
        try {
            long size = getBackupSize(backupFileName);
            LocalDateTime timestamp = extractTimestampFromBackup(backupFileName);

            return String.format("Backup: %s | Size: %d bytes | Date: %s",
                    backupFileName, size, timestamp);

        } catch (Exception e) {
            logger.error("Failed to get backup info: {}", e.getMessage(), e);
            return "Unknown backup: " + backupFileName;
        }
    }
}
