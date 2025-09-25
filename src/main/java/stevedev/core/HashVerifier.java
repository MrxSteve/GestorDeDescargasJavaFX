package stevedev.core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilidad para verificar la integridad de archivos mediante hashes
 */
public class HashVerifier {
    
    public enum HashType {
        MD5("MD5"),
        SHA1("SHA-1"),
        SHA256("SHA-256"),
        SHA512("SHA-512");
        
        private final String algorithm;
        
        HashType(String algorithm) {
            this.algorithm = algorithm;
        }
        
        public String getAlgorithm() {
            return algorithm;
        }
    }

    /**
     * Calcula el hash de un archivo usando el algoritmo especificado
     * @param filePath Ruta del archivo
     * @param hashType Tipo de hash a calcular
     * @return Hash en formato hexadecimal
     * @throws IOException Si hay error leyendo el archivo
     * @throws NoSuchAlgorithmException Si el algoritmo no es compatible
     */
    public static String calculateHash(String filePath, HashType hashType) 
            throws IOException, NoSuchAlgorithmException {
        
        MessageDigest digest = MessageDigest.getInstance(hashType.getAlgorithm());
        
        try (InputStream fis = Files.newInputStream(Paths.get(filePath));
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = bis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        
        return bytesToHex(digest.digest());
    }

    /**
     * Calcula el hash SHA-256 de un archivo (método por defecto)
     */
    public static String calculateSHA256(String filePath) throws IOException {
        try {
            return calculateHash(filePath, HashType.SHA256);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 siempre debería estar disponible
            throw new RuntimeException("SHA-256 no disponible", e);
        }
    }

    /**
     * Verifica si el hash calculado coincide con el esperado
     * @param filePath Ruta del archivo
     * @param expectedHash Hash esperado
     * @param hashType Tipo de hash
     * @return true si los hashes coinciden
     */
    public static boolean verifyHash(String filePath, String expectedHash, HashType hashType) {
        try {
            String calculatedHash = calculateHash(filePath, hashType);
            return calculatedHash.equalsIgnoreCase(expectedHash);
        } catch (Exception e) {
            System.err.println("Error verificando hash: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica hash SHA-256 (método por defecto)
     */
    public static boolean verifySHA256(String filePath, String expectedHash) {
        return verifyHash(filePath, expectedHash, HashType.SHA256);
    }

    /**
     * Detecta el tipo de hash basado en la longitud
     * @param hash Hash a analizar
     * @return Tipo de hash detectado o null si no se puede determinar
     */
    public static HashType detectHashType(String hash) {
        if (hash == null) return null;
        
        switch (hash.length()) {
            case 32:
                return HashType.MD5;
            case 40:
                return HashType.SHA1;
            case 64:
                return HashType.SHA256;
            case 128:
                return HashType.SHA512;
            default:
                return null;
        }
    }

    /**
     * Calcula hash con progreso para archivos grandes
     * @param filePath Ruta del archivo
     * @param hashType Tipo de hash
     * @param progressCallback Callback para reportar progreso (0.0 - 1.0)
     * @return Hash calculado
     */
    public static String calculateHashWithProgress(String filePath, HashType hashType, 
            java.util.function.Consumer<Double> progressCallback) 
            throws IOException, NoSuchAlgorithmException {
        
        MessageDigest digest = MessageDigest.getInstance(hashType.getAlgorithm());
        long fileSize = Files.size(Paths.get(filePath));
        long bytesProcessed = 0;
        
        try (InputStream fis = Files.newInputStream(Paths.get(filePath));
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = bis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
                bytesProcessed += bytesRead;
                
                if (progressCallback != null && fileSize > 0) {
                    double progress = (double) bytesProcessed / fileSize;
                    progressCallback.accept(progress);
                }
            }
        }
        
        return bytesToHex(digest.digest());
    }

    /**
     * Convierte bytes a representación hexadecimal
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Valida si una cadena es un hash válido
     * @param hash Cadena a validar
     * @return true si es un hash hexadecimal válido
     */
    public static boolean isValidHash(String hash) {
        if (hash == null || hash.isEmpty()) {
            return false;
        }
        
        // Verificar que solo contenga caracteres hexadecimales
        return hash.matches("^[a-fA-F0-9]+$") && 
               (hash.length() == 32 || hash.length() == 40 || 
                hash.length() == 64 || hash.length() == 128);
    }
}