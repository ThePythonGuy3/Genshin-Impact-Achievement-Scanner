package pyguy.util;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.zip.*;

public class CompressionUtils
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static byte[] Compress(byte[] data) throws IOException
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream))
        {
            deflaterOutputStream.write(data);
        }

        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] Decompress(byte[] data) throws IOException
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (InflaterInputStream inflaterInputStream = new InflaterInputStream(new ByteArrayInputStream(data)))
        {
            inflaterInputStream.transferTo(byteArrayOutputStream);
        }

        return byteArrayOutputStream.toByteArray();
    }

    public static void StoreObject(File file, Object object) throws IOException
    {
        try (FileOutputStream fileOutputStream = new FileOutputStream(file))
        {
            fileOutputStream.write(CompressionUtils.Compress(OBJECT_MAPPER.writeValueAsBytes(object)));
        }
    }

    public static <T> T LoadObject(File file, TypeReference<T> typeReference) throws IOException
    {
        try (FileInputStream fileInputStream = new FileInputStream(file))
        {
            return OBJECT_MAPPER.readValue(CompressionUtils.Decompress(fileInputStream.readAllBytes()), typeReference);
        }
    }

    public static <T> T LoadObject(File file, Class<T> type) throws IOException
    {
        try (FileInputStream fileInputStream = new FileInputStream(file))
        {
            return OBJECT_MAPPER.readValue(CompressionUtils.Decompress(fileInputStream.readAllBytes()), type);
        }
    }
}