package com.example.acceso.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder.env}")
    private String cloudinaryEnvFolder;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public Map<String, String> uploadFile(MultipartFile file, String subFolder) throws IOException {
        // Construir la ruta de la carpeta: ej: "desarrollo/logos" o "produccion/vouchers"
        String fullFolderPath = cloudinaryEnvFolder + "/" + subFolder;

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                // Usar el parámetro 'folder' para especificar el directorio
                "folder", fullFolderPath,
                // Pedir a Cloudinary que genere un nombre de archivo único
                "use_filename", true,
                "unique_filename", true,
                "overwrite", true,
                "resource_type", "auto"
        ));

        return Map.of(
                "secure_url", (String) uploadResult.get("secure_url"),
                "public_id", (String) uploadResult.get("public_id")
        );
    }

    public void deleteFile(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}