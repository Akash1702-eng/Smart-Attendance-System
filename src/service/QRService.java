package service;

// Abstract class (Unit 4: Abstraction)
public abstract class QRService {
    // Abstract method
    public abstract String generateQRData(String studentId);
    
    // Concrete method
    public boolean validateQRData(String qrData) {
        return qrData != null && qrData.startsWith("STUDENT:") && qrData.split(":").length >= 2;
    }
}
