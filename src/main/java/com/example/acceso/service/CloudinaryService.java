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

    /**
     * Elimina una imagen de Cloudinary usando su URL completa.
     * Extrae el public_id de la URL y luego llama al método de eliminación.
     * @param imageUrl La URL completa (secure_url) de la imagen a eliminar.
     * @throws IOException Si ocurre un error durante la eliminación.
     */
    public void deleteImageByUrl(String imageUrl) throws IOException {
        if (imageUrl == null || imageUrl.isEmpty() || !imageUrl.contains(cloudinaryEnvFolder)) {
            System.err.println("URL de Cloudinary inválida o no contiene la carpeta de entorno: " + imageUrl);
            return; // No se puede procesar si la URL no es válida
        }

        try {
            // Extraer el public_id de la URL.
            // Esto asume que la URL contiene la carpeta de entorno (ej: "desarrollo/vouchers/...")
            // y que el public_id es la parte de la ruta desde la carpeta de entorno hasta antes de la extensión.
            int startIndex = imageUrl.lastIndexOf(cloudinaryEnvFolder);
            String publicIdWithExtension = imageUrl.substring(startIndex);
            String publicId = publicIdWithExtension.substring(0, publicIdWithExtension.lastIndexOf('.'));

            System.out.println("Extrayendo public_id de la URL: " + imageUrl);
            System.out.println("Public ID extraído para eliminación: " + publicId);

            this.deleteFile(publicId);
        } catch (Exception e) {
            // Lanza una nueva excepción para notificar al servicio que la llamó.
            throw new IOException("Error al intentar eliminar imagen de Cloudinary por URL: " + imageUrl, e);
        }
    }
}