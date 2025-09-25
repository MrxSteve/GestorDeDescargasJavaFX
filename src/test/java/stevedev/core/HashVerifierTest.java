package stevedev.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import stevedev.core.HashVerifier.HashType;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para HashVerifier
 */
class HashVerifierTest {
    
    private File testFile;
    private static final String TEST_CONTENT = "Hello, World! This is a test file for hash verification.";
    
    @BeforeEach
    void setUp() throws IOException {
        // Crear archivo temporal de prueba
        testFile = File.createTempFile("hashtest", ".txt");
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write(TEST_CONTENT);
        }
    }
    
    @AfterEach
    void tearDown() {
        if (testFile != null && testFile.exists()) {
            testFile.delete();
        }
    }
    
    @Test
    void testCalculateSHA256() throws IOException {
        String hash = HashVerifier.calculateSHA256(testFile.getAbsolutePath());
        
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 tiene 64 caracteres hexadecimales
        assertTrue(hash.matches("[a-f0-9]+"), "Hash debe contener solo caracteres hexadecimales");
    }
    
    @Test
    void testCalculateHash() throws Exception {
        String md5Hash = HashVerifier.calculateHash(testFile.getAbsolutePath(), HashType.MD5);
        String sha1Hash = HashVerifier.calculateHash(testFile.getAbsolutePath(), HashType.SHA1);
        String sha256Hash = HashVerifier.calculateHash(testFile.getAbsolutePath(), HashType.SHA256);
        String sha512Hash = HashVerifier.calculateHash(testFile.getAbsolutePath(), HashType.SHA512);
        
        // Verificar longitudes esperadas
        assertEquals(32, md5Hash.length());
        assertEquals(40, sha1Hash.length());
        assertEquals(64, sha256Hash.length());
        assertEquals(128, sha512Hash.length());
        
        // Todos deben ser hexadecimales
        assertTrue(md5Hash.matches("[a-f0-9]+"));
        assertTrue(sha1Hash.matches("[a-f0-9]+"));
        assertTrue(sha256Hash.matches("[a-f0-9]+"));
        assertTrue(sha512Hash.matches("[a-f0-9]+"));
    }
    
    @Test
    void testVerifyHash() throws Exception {
        String correctHash = HashVerifier.calculateSHA256(testFile.getAbsolutePath());
        String incorrectHash = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
        
        assertTrue(HashVerifier.verifySHA256(testFile.getAbsolutePath(), correctHash));
        assertFalse(HashVerifier.verifySHA256(testFile.getAbsolutePath(), incorrectHash));
    }
    
    @Test
    void testDetectHashType() {
        assertEquals(HashType.MD5, HashVerifier.detectHashType("12345678901234567890123456789012"));
        assertEquals(HashType.SHA1, HashVerifier.detectHashType("1234567890123456789012345678901234567890"));
        assertEquals(HashType.SHA256, HashVerifier.detectHashType("1234567890123456789012345678901234567890123456789012345678901234"));
        assertEquals(HashType.SHA512, HashVerifier.detectHashType("1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"));
        
        assertNull(HashVerifier.detectHashType("invalid"));
        assertNull(HashVerifier.detectHashType(null));
    }
    
    @Test
    void testIsValidHash() {
        // Hashes válidos
        assertTrue(HashVerifier.isValidHash("12345678901234567890123456789012")); // MD5
        assertTrue(HashVerifier.isValidHash("1234567890123456789012345678901234567890")); // SHA1
        assertTrue(HashVerifier.isValidHash("1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef")); // SHA256
        
        // Hashes inválidos
        assertFalse(HashVerifier.isValidHash("invalid"));
        assertFalse(HashVerifier.isValidHash(""));
        assertFalse(HashVerifier.isValidHash(null));
        assertFalse(HashVerifier.isValidHash("123")); // Muy corto
        assertFalse(HashVerifier.isValidHash("1234567890abcdefGHIJ")); // Caracteres inválidos
    }
    
    @Test
    void testCalculateHashWithProgress() throws Exception {
        final boolean[] progressCalled = {false};
        
        String hash = HashVerifier.calculateHashWithProgress(
            testFile.getAbsolutePath(), 
            HashType.SHA256, 
            progress -> {
                progressCalled[0] = true;
                assertTrue(progress >= 0.0 && progress <= 1.0, 
                          "El progreso debe estar entre 0.0 y 1.0");
            }
        );
        
        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertTrue(progressCalled[0], "El callback de progreso debería haber sido llamado");
    }
    
    @Test
    void testConsistentHashes() throws Exception {
        // Calcular el mismo hash múltiples veces
        String hash1 = HashVerifier.calculateSHA256(testFile.getAbsolutePath());
        String hash2 = HashVerifier.calculateSHA256(testFile.getAbsolutePath());
        String hash3 = HashVerifier.calculateHash(testFile.getAbsolutePath(), HashType.SHA256);
        
        assertEquals(hash1, hash2, "Los hashes consecutivos deben ser idénticos");
        assertEquals(hash1, hash3, "Los hashes usando diferentes métodos deben ser idénticos");
    }
    
    @Test
    void testNonExistentFile() {
        assertThrows(IOException.class, () -> {
            HashVerifier.calculateSHA256("/path/to/nonexistent/file.txt");
        });
    }
}